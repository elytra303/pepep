package rich.modules.impl.misc;

import lombok.Getter;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.util.modules.autobuy.*;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import rich.util.timer.TimerUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AutoBuy extends ModuleStructure {
    private final SelectSetting leaveType = new SelectSetting("Тип обхода", "Проверяющий").value("Проверяющий", "Покупающий");
    private final SliderSettings timer2 = new SliderSettings("Таймер обновления аукциона", "").range(350, 750).setValue(350);
    private final BooleanSetting bypassDelay = new BooleanSetting("Обход задержки 1.16.5 анках", "").setValue(false);
    private final BooleanSetting bypassDelay1214 = new BooleanSetting("Обход задержки 1.21.4 анках", "").setValue(false);

    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();
    private final NetworkManager networkManager;
    private final ServerManager serverManager;
    private final AuctionHandler auctionHandler;
    private final AfkHandler afkHandler;

    private final TimerUtil openTimer = TimerUtil.create();
    private final TimerUtil updateTimer = TimerUtil.create();
    private final TimerUtil buyTimer = TimerUtil.create();
    private final TimerUtil switchTimer = TimerUtil.create();
    private final TimerUtil enterDelayTimer = TimerUtil.create();
    private final TimerUtil ahSpamTimer = TimerUtil.create();
    private final TimerUtil connectionCheckTimer = TimerUtil.create();
    private final TimerUtil auctionRequestTimer = TimerUtil.create();

    private boolean open = false;
    private boolean serverInAuction = false;
    private boolean justEntered = false;
    private boolean spammingAh = false;
    private boolean waitingForAuctionOpen = false;
    private List<AutoBuyableItem> cachedEnabledItems = new ArrayList<>();

    public AutoBuy() {
        super("Auto Buy", "Автоматическая покупка предметов на аукционе", ModuleCategory.MISC);

        timer2.setVisible(() -> leaveType.isSelected("Покупающий"));
        bypassDelay.setVisible(() -> leaveType.isSelected("Покупающий"));
        bypassDelay1214.setVisible(() -> leaveType.isSelected("Покупающий"));

        setup(leaveType, timer2, bypassDelay, bypassDelay1214);

        networkManager = new NetworkManager();
        serverManager = new ServerManager(bypassDelay, bypassDelay1214);
        auctionHandler = new AuctionHandler(autoBuyManager);
        afkHandler = new AfkHandler();
    }

    @Override
    public void activate() {
        super.activate();
        autoBuyManager.setEnabled(true);
        resetTimers();
        resetState();

        if (leaveType.isSelected("Покупающий") && (bypassDelay.isValue() || bypassDelay1214.isValue())) {
            mc.options.pauseOnLostFocus = false;
        }

        cacheEnabledItems();
        networkManager.start(leaveType.getSelected());
    }

    @Override
    public void deactivate() {
        super.deactivate();
        autoBuyManager.setEnabled(false);
        networkManager.stop();
        serverManager.reset();
        afkHandler.resetMovementKeys(mc.options);
    }

    private void resetTimers() {
        openTimer.resetCounter();
        updateTimer.resetCounter();
        buyTimer.resetCounter();
        switchTimer.resetCounter();
        enterDelayTimer.resetCounter();
        ahSpamTimer.resetCounter();
        connectionCheckTimer.resetCounter();
        auctionRequestTimer.resetCounter();
        serverManager.resetTimers();
        afkHandler.resetTimers();
    }

    private void resetState() {
        open = false;
        serverInAuction = false;
        justEntered = false;
        spammingAh = false;
        waitingForAuctionOpen = false;
        cachedEnabledItems.clear();
        networkManager.clearQueues();
        auctionHandler.clear();
    }

    private void cacheEnabledItems() {
        cachedEnabledItems.clear();
        cachedEnabledItems.addAll(autoBuyManager.getEnabledItems());
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof GameMessageS2CPacket gameMessage) {
            Text content = gameMessage.content();
            String message = content.getString();

            if (message.contains("Вы уже подключены к этому серверу!")) {
                serverManager.switchToNextServer(mc.player, networkManager, leaveType.isSelected("Покупающий"));
                return;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!autoBuyManager.isEnabled()) return;

        handleConnectionStatus();
        afkHandler.handle(mc);

        boolean wasInHub = serverManager.isInHub();
        serverManager.updateHubStatus(mc.world);

        if (serverManager.shouldJoinAnarchy(bypassDelay.isValue(), bypassDelay1214.isValue())) {
            serverManager.joinAnarchyFromHub(mc.player);
        }

        if (wasInHub && !serverManager.isInHub()) {
            handleServerSwitch();
        }

        if (serverManager.isWaitingForServerLoad() || ServerSwitchHandler.isWaitingForServerLoad()) {
            if (ServerSwitchHandler.hasTimedOut() || (!wasInHub && !serverManager.isInHub())) {
                serverManager.setWaitingForServerLoad(false);
                ServerSwitchHandler.setWaitingForServerLoad(false);
                handleServerSwitch();
            }
        }

        handleAhSpam();
        handleAuction();
        handleServerAutoSwitch();
        handleCheckerAuctionRequest();
    }

    private void handleConnectionStatus() {
        if (leaveType.isSelected("Проверяющий")) {
            if (connectionCheckTimer.hasTimeElapsed(5000)) {
                if (!networkManager.isConnectedToServer()) {
                    networkManager.start(leaveType.getSelected());
                }
                connectionCheckTimer.resetCounter();
            }
        }
    }

    private void handleServerSwitch() {
        justEntered = true;
        enterDelayTimer.resetCounter();
        switchTimer.resetCounter();
        waitingForAuctionOpen = false;
        auctionRequestTimer.resetCounter();
    }

    private void handleAhSpam() {
        if (bypassDelay.isValue() || bypassDelay1214.isValue()) {
            if (justEntered && enterDelayTimer.hasTimeElapsed(2000)) {
                if (!spammingAh) {
                    spammingAh = true;
                    ahSpamTimer.resetCounter();
                }
            }
            if (spammingAh && !afkHandler.isPerformingAction()) {
                if (ahSpamTimer.hasTimeElapsed(1250)) {
                    if (mc.player.networkHandler != null) {
                        CommandSender.sendCommand(mc.player, "/ah");
                    }
                    ahSpamTimer.resetCounter();
                }
            }
        }
    }

    private void handleCheckerAuctionRequest() {
        if (leaveType.isSelected("Проверяющий")) {
            if (!open && !waitingForAuctionOpen) {
                if (auctionRequestTimer.hasTimeElapsed(3000)) {
                    if (networkManager.isConnectedToServer()) {
                        CommandSender.openAuction();
                        waitingForAuctionOpen = true;
                        auctionRequestTimer.resetCounter();
                    }
                }
            }
            if (waitingForAuctionOpen && auctionRequestTimer.hasTimeElapsed(5000)) {
                waitingForAuctionOpen = false;
                auctionRequestTimer.resetCounter();
            }
        }
    }

    private void handleAuction() {
        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            int syncId = screen.getScreenHandler().syncId;
            List<Slot> slots = screen.getScreenHandler().slots;

            if (title.contains("Аукцион") || title.contains("Аукционы") || title.contains("Поиск")) {
                if (!open) {
                    enterAuction();
                    return;
                }

                if (leaveType.isSelected("Покупающий")) {
                    handleBuyerMode(screen, syncId, slots);
                } else if (leaveType.isSelected("Проверяющий")) {
                    handleCheckerMode(slots);
                }
            } else if (title.contains("Подозрительная цена")) {
                auctionHandler.handleSuspiciousPrice(mc, syncId, slots);
                openTimer.resetCounter();
                buyTimer.resetCounter();
            } else {
                exitAuction();
            }
        } else {
            exitAuction();
        }
    }

    private void enterAuction() {
        open = true;
        openTimer.resetCounter();
        updateTimer.resetCounter();
        buyTimer.resetCounter();
        serverInAuction = true;
        auctionHandler.clear();
        justEntered = false;
        spammingAh = false;
        waitingForAuctionOpen = false;
        cacheEnabledItems();
        if (leaveType.isSelected("Проверяющий")) {
            networkManager.notifyAuctionEnter();
        }
        if (leaveType.isSelected("Покупающий")) {
            networkManager.requestAuctionOpen();
        }
    }

    private void exitAuction() {
        if (open) {
            open = false;
            serverInAuction = false;
            auctionHandler.clear();
            if (leaveType.isSelected("Проверяющий")) {
                networkManager.notifyAuctionLeave();
            }
        }
    }

    private void handleBuyerMode(GenericContainerScreen screen, int syncId, List<Slot> slots) {
        long clientCount = networkManager.getClientInAuctionCount();

        if (networkManager.getQueueSize() > 30) {
            auctionHandler.updateAuction(mc, syncId);
            networkManager.sendUpdateToClients();
            updateTimer.resetCounter();
            networkManager.clearQueues();
            return;
        }

        BuyRequest request = networkManager.pollRequest();
        if (request != null) {
            auctionHandler.handleBuyRequest(mc, syncId, slots, request, networkManager);
        }

        if (auctionHandler.shouldUpdate()) {
            auctionHandler.updateAuction(mc, syncId);
            networkManager.sendUpdateToClients();
            updateTimer.resetCounter();
            networkManager.clearQueues();
        }

        if (updateTimer.hasTimeElapsed((long) timer2.getValue()) && serverInAuction &&
                clientCount > 0 && networkManager.isQueuesEmpty()) {
            auctionHandler.updateAuction(mc, syncId);
            networkManager.sendUpdateToClients();
            updateTimer.resetCounter();
        }
    }

    private void handleCheckerMode(List<Slot> slots) {
        List<Slot> bestSlots = auctionHandler.findMatchingSlots(slots, cachedEnabledItems);
        if (!bestSlots.isEmpty()) {
            auctionHandler.processBestSlots(bestSlots, networkManager);
            buyTimer.resetCounter();
        }
    }

    private void handleServerAutoSwitch() {
        if (leaveType.isSelected("Покупающий") && (bypassDelay.isValue() || bypassDelay1214.isValue())) {
            if (!serverManager.isInHub() && switchTimer.hasTimeElapsed(60000)) {
                serverManager.switchToNextServer(mc.player, networkManager, true);
            }
        }
    }
}