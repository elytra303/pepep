package rich.util.modules.autobuy;

import net.minecraft.client.MinecraftClient;
import rich.util.string.chat.ChatMessage;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private ExecutorService executor;
    private final ConcurrentLinkedQueue<BuyRequest> buyQueue = new ConcurrentLinkedQueue<>();

    public void startAsServer() {
        stop();
        running.set(true);
        executor = Executors.newCachedThreadPool();

        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                serverSocket.setReuseAddress(true);
                serverSocket.setSoTimeout(500);
                msg("§a[ПОКУПАТЕЛЬ] Жду проверяющего на порту " + PORT);

                while (running.get()) {
                    try {
                        Socket client = serverSocket.accept();
                        client.setTcpNoDelay(true);
                        client.setSoTimeout(500);

                        acceptedClient = client;
                        out = new PrintWriter(client.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        connected.set(true);
                        clientInAuction.set(false);
                        msg("§a[ПОКУПАТЕЛЬ] Проверяющий подключился!");

                        serverReadLoop();
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        if (running.get()) {
                            connected.set(false);
                        }
                    }
                }
            } catch (IOException e) {
                if (running.get()) msg("§c[ПОКУПАТЕЛЬ] Ошибка: " + e.getMessage());
            }
        });
    }

    public void startAsClient() {
        stop();
        running.set(true);
        executor = Executors.newCachedThreadPool();

        executor.execute(() -> {
            while (running.get()) {
                if (!connected.get()) {
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress("localhost", PORT), 1000);
                        socket.setTcpNoDelay(true);
                        socket.setSoTimeout(500);

                        clientSocket = socket;
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        connected.set(true);
                        msg("§a[ПРОВЕРЯЮЩИЙ] Подключился к покупателю!");

                        clientReadLoop();
                    } catch (IOException e) {
                        connected.set(false);
                    }
                }
                sleep(1000);
            }
        });
    }

    private void serverReadLoop() {
        try {
            while (running.get() && connected.get()) {
                try {
                    if (in != null && in.ready()) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }
                        processServerMessage(line);
                    } else {
                        sleep(50);
                    }
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException ignored) {
        } finally {
            connected.set(false);
            clientInAuction.set(false);
            if (running.get()) msg("§c[ПОКУПАТЕЛЬ] Проверяющий отключился");
            closeClient();
        }
    }

    private void clientReadLoop() {
        try {
            while (running.get() && connected.get()) {
                try {
                    if (in != null && in.ready()) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }
                    } else {
                        sleep(50);
                    }
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException ignored) {
        } finally {
            connected.set(false);
            if (running.get()) msg("§c[ПРОВЕРЯЮЩИЙ] Соединение потеряно");
            closeClient();
        }
    }

    private void processServerMessage(String line) {
        if (line.startsWith("BUY:")) {
            try {
                String[] parts = line.substring(4).split("\\|");
                if (parts.length == 2) {
                    String name = parts[0];
                    int price = Integer.parseInt(parts[1]);
                    buyQueue.add(new BuyRequest(name, price));
                    msg("§a[ПОКУПАТЕЛЬ] Получил: §f" + name + " §aза " + price + "$");
                }
            } catch (Exception ignored) {}
        } else if (line.equals("ENTER_AUCTION")) {
            clientInAuction.set(true);
            msg("§a[ПОКУПАТЕЛЬ] Проверяющий в аукционе");
        } else if (line.equals("LEAVE_AUCTION")) {
            clientInAuction.set(false);
            msg("§e[ПОКУПАТЕЛЬ] Проверяющий вышел из аукциона");
        }
    }

    public void sendBuyCommand(String itemName, int price) {
        if (connected.get() && out != null) {
            out.println("BUY:" + itemName + "|" + price);
            msg("§b[ПРОВЕРЯЮЩИЙ] Отправил: §f" + itemName + " §bза " + price + "$");
        }
    }

    public void sendEnterAuction() {
        if (connected.get() && out != null) {
            out.println("ENTER_AUCTION");
        }
    }

    public void sendLeaveAuction() {
        if (connected.get() && out != null) {
            out.println("LEAVE_AUCTION");
        }
    }

    public BuyRequest pollBuyRequest() {
        return buyQueue.poll();
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

    private void closeClient() {
        try { if (acceptedClient != null) acceptedClient.close(); } catch (Exception ignored) {}
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception ignored) {}
        acceptedClient = null;
        clientSocket = null;
        out = null;
        in = null;
    }

    public void stop() {
        running.set(false);
        connected.set(false);
        clientInAuction.set(false);
        buyQueue.clear();

        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        serverSocket = null;

        closeClient();

        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}
            executor = null;
        }
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