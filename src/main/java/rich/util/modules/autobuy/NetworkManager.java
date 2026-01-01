package rich.util.modules.autobuy;

import net.minecraft.client.MinecraftClient;
import rich.util.string.chat.ChatMessage;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkManager {
    private static final int PORT = 25566;

    private volatile ServerSocket serverSocket;
    private volatile Socket clientSocket;
    private volatile Socket acceptedClient;
    private volatile PrintWriter out;
    private volatile BufferedReader in;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean clientInAuction = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final AtomicBoolean syncReady = new AtomicBoolean(false);
    private final AtomicBoolean remoteReady = new AtomicBoolean(false);
    private final AtomicBoolean syncStarted = new AtomicBoolean(false);
    private final AtomicLong syncStartTime = new AtomicLong(0);
    private volatile ExecutorService executor;
    private final ConcurrentLinkedQueue<BuyRequest> buyQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> serverSwitchQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Boolean> pauseQueue = new ConcurrentLinkedQueue<>();

    public void startAsServer() {
        stopAndWait();

        running.set(true);
        stopping.set(false);
        resetSync();
        executor = Executors.newCachedThreadPool();

        executor.execute(() -> {
            int attempts = 0;
            while (running.get() && serverSocket == null && attempts < 5) {
                try {
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(PORT));
                    serverSocket.setSoTimeout(1000);
                    msg("§a[ПОКУПАТЕЛЬ] Сервер запущен на порту " + PORT);
                } catch (IOException e) {
                    serverSocket = null;
                    attempts++;
                    if (attempts < 5) {
                        msg("§e[ПОКУПАТЕЛЬ] Порт занят, попытка " + attempts + "/5...");
                        sleep(1000);
                    } else {
                        msg("§c[ПОКУПАТЕЛЬ] Не удалось запустить сервер");
                        return;
                    }
                }
            }

            while (running.get() && serverSocket != null && !serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    client.setTcpNoDelay(true);
                    client.setKeepAlive(true);

                    closeClientSockets();
                    resetSync();

                    acceptedClient = client;
                    out = new PrintWriter(client.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    connected.set(true);
                    clientInAuction.set(false);
                    msg("§a[ПОКУПАТЕЛЬ] Проверяющий подключился!");

                    executor.execute(this::serverReadLoop);
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    if (running.get() && !stopping.get()) {
                        connected.set(false);
                    }
                }
            }
        });
    }

    public void startAsClient() {
        stopAndWait();

        running.set(true);
        stopping.set(false);
        resetSync();
        executor = Executors.newCachedThreadPool();

        executor.execute(() -> {
            while (running.get() && !stopping.get()) {
                if (!connected.get()) {
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress("localhost", PORT), 2000);
                        socket.setTcpNoDelay(true);
                        socket.setKeepAlive(true);

                        clientSocket = socket;
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        connected.set(true);
                        resetSync();
                        msg("§a[ПРОВЕРЯЮЩИЙ] Подключился к покупателю!");

                        clientReadLoop();
                    } catch (IOException e) {
                        connected.set(false);
                    }
                }
                sleep(2000);
            }
        });
    }

    private void serverReadLoop() {
        try {
            String line;
            while (running.get() && connected.get() && in != null && !stopping.get()) {
                line = in.readLine();
                if (line == null) break;
                processServerMessage(line);
            }
        } catch (IOException ignored) {
        } finally {
            connected.set(false);
            clientInAuction.set(false);
            resetSync();
            if (running.get() && !stopping.get()) {
                msg("§c[ПОКУПАТЕЛЬ] Проверяющий отключился");
            }
        }
    }

    private void clientReadLoop() {
        try {
            String line;
            while (running.get() && connected.get() && in != null && !stopping.get()) {
                line = in.readLine();
                if (line == null) break;
                processClientMessage(line);
            }
        } catch (IOException ignored) {
        } finally {
            connected.set(false);
            resetSync();
            if (running.get() && !stopping.get()) {
                msg("§c[ПРОВЕРЯЮЩИЙ] Соединение потеряно");
            }
            closeClientSockets();
        }
    }

    private void processServerMessage(String line) {
        if (line.startsWith("BUY:")) {
            try {
                String data = line.substring(4);
                String[] parts = data.split("\\|\\|\\|");
                if (parts.length == 3) {
                    int price = Integer.parseInt(parts[0]);
                    String itemId = parts[1];
                    String displayName = parts[2];
                    buyQueue.add(new BuyRequest(price, itemId, displayName));
                    msg("§a[ПОКУПАТЕЛЬ] Получил: §f" + displayName + " §aза " + price + "$");
                }
            } catch (Exception ignored) {}
        } else if (line.equals("ENTER_AUCTION")) {
            clientInAuction.set(true);
            msg("§a[ПОКУПАТЕЛЬ] Проверяющий в аукционе");
        } else if (line.equals("LEAVE_AUCTION")) {
            clientInAuction.set(false);
            resetSync();
            msg("§e[ПОКУПАТЕЛЬ] Проверяющий вышел из аукциона");
        } else if (line.equals("PAUSE:true")) {
            pauseQueue.add(true);
        } else if (line.equals("PAUSE:false")) {
            pauseQueue.add(false);
        } else if (line.equals("SYNC_READY")) {
            remoteReady.set(true);
            checkAndStartSync();
        } else if (line.equals("SYNC_START")) {
            syncStarted.set(true);
            syncStartTime.set(System.currentTimeMillis());
            msg("§a[СИНХРО] Начинаем синхронное обновление!");
        }
    }

    private void processClientMessage(String line) {
        if (line.startsWith("SWITCH:")) {
            String server = line.substring(7);
            serverSwitchQueue.add(server);
            msg("§e[ПРОВЕРЯЮЩИЙ] Смена сервера: " + server);
        } else if (line.equals("PAUSE:true")) {
            pauseQueue.add(true);
        } else if (line.equals("PAUSE:false")) {
            pauseQueue.add(false);
        } else if (line.equals("SYNC_READY")) {
            remoteReady.set(true);
            msg("§e[СИНХРО] Покупатель готов");
        } else if (line.equals("SYNC_START")) {
            syncStarted.set(true);
            syncStartTime.set(System.currentTimeMillis());
            msg("§a[СИНХРО] Начинаем синхронное обновление!");
        }
    }

    public void sendReady() {
        if (connected.get() && out != null && !syncReady.get()) {
            syncReady.set(true);
            out.println("SYNC_READY");
            out.flush();
            msg("§e[СИНХРО] Отправил готовность");
            checkAndStartSync();
        }
    }

    private void checkAndStartSync() {
        if (syncReady.get() && remoteReady.get() && !syncStarted.get()) {
            msg("§e[СИНХРО] Оба готовы, ждём 2 секунды...");

            new Thread(() -> {
                sleep(2000);
                if (connected.get() && out != null && !syncStarted.get()) {
                    syncStarted.set(true);
                    syncStartTime.set(System.currentTimeMillis());
                    out.println("SYNC_START");
                    out.flush();
                    msg("§a[СИНХРО] СТАРТ!");
                }
            }).start();
        }
    }

    public void resetSync() {
        syncReady.set(false);
        remoteReady.set(false);
        syncStarted.set(false);
        syncStartTime.set(0);
    }

    public boolean isSyncStarted() {
        return syncStarted.get();
    }

    public long getSyncStartTime() {
        return syncStartTime.get();
    }

    public void sendBuyCommand(int price, String itemId, String displayName) {
        if (connected.get() && out != null) {
            out.println("BUY:" + price + "|||" + itemId + "|||" + displayName);
            out.flush();
        }
    }

    public void sendServerSwitch(String server) {
        if (connected.get() && out != null) {
            out.println("SWITCH:" + server);
            out.flush();
        }
    }

    public void sendPauseState(boolean paused) {
        if (connected.get() && out != null) {
            out.println("PAUSE:" + paused);
            out.flush();
        }
    }

    public void sendEnterAuction() {
        if (connected.get() && out != null) {
            out.println("ENTER_AUCTION");
            out.flush();
        }
    }

    public void sendLeaveAuction() {
        if (connected.get() && out != null) {
            out.println("LEAVE_AUCTION");
            out.flush();
            resetSync();
        }
    }

    public BuyRequest pollBuyRequest() {
        return buyQueue.poll();
    }

    public String pollServerSwitch() {
        return serverSwitchQueue.poll();
    }

    public Boolean pollPauseState() {
        return pauseQueue.poll();
    }

    public boolean isConnected() {
        return connected.get();
    }

    public int getConnectedClientCount() {
        return connected.get() ? 1 : 0;
    }

    public long getClientInAuctionCount() {
        return clientInAuction.get() ? 1 : 0;
    }

    public boolean isConnectedToServer() {
        return connected.get() && clientSocket != null;
    }

    public boolean isServerRunning() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    private void closeClientSockets() {
        PrintWriter tempOut = out;
        BufferedReader tempIn = in;
        Socket tempAccepted = acceptedClient;
        Socket tempClient = clientSocket;

        out = null;
        in = null;
        acceptedClient = null;
        clientSocket = null;

        try { if (tempIn != null) tempIn.close(); } catch (Exception ignored) {}
        try { if (tempOut != null) tempOut.close(); } catch (Exception ignored) {}
        try { if (tempAccepted != null) tempAccepted.close(); } catch (Exception ignored) {}
        try { if (tempClient != null) tempClient.close(); } catch (Exception ignored) {}
    }

    private void closeServerSocket() {
        ServerSocket temp = serverSocket;
        serverSocket = null;
        try { if (temp != null) temp.close(); } catch (Exception ignored) {}
    }

    public void stop() {
        stopping.set(true);
        running.set(false);
        connected.set(false);
        clientInAuction.set(false);
        resetSync();

        buyQueue.clear();
        serverSwitchQueue.clear();
        pauseQueue.clear();

        closeClientSockets();
        closeServerSocket();

        ExecutorService temp = executor;
        executor = null;
        if (temp != null) {
            temp.shutdownNow();
        }
    }

    public void stopAndWait() {
        stop();
        sleep(500);
    }

    private void msg(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.execute(() -> ChatMessage.autobuymessage(text));
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}