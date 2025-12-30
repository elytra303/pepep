package rich.modules.impl.misc;

import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.AuctionUtils;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.util.modules.autobuy.BuyRequest;
import rich.util.modules.autobuy.NetworkManager;
import rich.util.string.chat.ChatMessage;

import java.util.*;

@Getter
public class AutoBuy extends ModuleStructure {
    private static AutoBuy instance;

    private final SelectSetting mode = new SelectSetting("Режим", "Проверяющий").value("Проверяющий", "Покупающий");
    private final SliderSettings updateDelay = new SliderSettings("Задержка обновления", "").range(300, 1000).setValue(400);
    private final BooleanSetting notifications = new BooleanSetting("Уведомления", "").setValue(true);

    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();
    private final NetworkManager network = new NetworkManager();

    private long lastUpdate = 0;
    private long lastAhOpen = 0;
    private boolean inAuction = false;
    private boolean notifiedEnter = false;
    private Set<String> sentItems = new HashSet<>();

    public AutoBuy() {
        super("Auto Buy", "Автоматическая покупка на аукционе", ModuleCategory.MISC);
        instance = this;
        setup(mode, updateDelay, notifications);
    }

    public static AutoBuy getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        super.activate();
        autoBuyManager.setEnabled(true);
        reset();

        if (mode.isSelected("Покупающий")) {
            network.startAsServer();
        } else {
            network.startAsClient();
        }

        msg("§aМодуль включён. Режим: §b" + mode.getSelected());
    }

    @Override
    public void deactivate() {
        super.deactivate();
        autoBuyManager.setEnabled(false);
        network.stop();
        reset();
        msg("§cМодуль выключен");
    }

    private void reset() {
        inAuction = false;
        notifiedEnter = false;
        sentItems.clear();
        lastUpdate = 0;
        lastAhOpen = 0;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!autoBuyManager.isEnabled()) return;

        long now = System.currentTimeMillis();

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            if (inAuction) {
                inAuction = false;
                if (mode.isSelected("Проверяющий") && notifiedEnter) {
                    network.sendLeaveAuction();
                    notifiedEnter = false;
                }
            }

            if (now - lastAhOpen > 3000) {
                mc.player.networkHandler.sendChatCommand("ah");
                lastAhOpen = now;
            }
            return;
        }

        String title = screen.getTitle().getString();

        if (title.contains("Подозрительная цена")) {
            confirmSuspiciousPrice(screen);
            return;
        }

        if (!title.contains("Аукцион") && !title.contains("Поиск")) {
            if (inAuction) {
                inAuction = false;
                if (mode.isSelected("Проверяющий") && notifiedEnter) {
                    network.sendLeaveAuction();
                    notifiedEnter = false;
                }
            }
            return;
        }

        if (!inAuction) {
            inAuction = true;
            sentItems.clear();
            msg("§aВ аукционе");

            if (mode.isSelected("Проверяющий") && !notifiedEnter) {
                network.sendEnterAuction();
                notifiedEnter = true;
            }
        }

        if (now - lastUpdate > (long) updateDelay.getValue()) {
            updateAuction(screen);
            lastUpdate = now;
        }

        if (mode.isSelected("Покупающий")) {
            processBuyRequests(screen);
        } else {
            scanAndSend(screen);
        }
    }

    private void updateAuction(GenericContainerScreen screen) {
        int syncId = screen.getScreenHandler().syncId;
        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    private void confirmSuspiciousPrice(GenericContainerScreen screen) {
        int syncId = screen.getScreenHandler().syncId;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.getStack().getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                msg("§aПодтвердил покупку");
                return;
            }
        }
    }

    private void scanAndSend(GenericContainerScreen screen) {
        if (!network.isConnected()) return;

        List<AutoBuyableItem> items = autoBuyManager.getEnabledItems();
        if (items.isEmpty()) return;

        for (int i = 0; i < 45 && i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) continue;

            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) continue;

            String name = AuctionUtils.funTimePricePattern.matcher(stack.getName().getString()).replaceAll("").trim();
            String key = name + "|" + price;

            if (sentItems.contains(key)) continue;

            for (AutoBuyableItem item : items) {
                if (price > item.getSettings().getBuyBelow()) continue;

                if (item.getSettings().isCanHaveQuantity()) {
                    if (stack.getCount() < item.getSettings().getMinQuantity()) continue;
                }

                if (AuctionUtils.compareItem(stack, item.createItemStack())) {
                    sentItems.add(key);
                    network.sendBuyCommand(name, price);
                    msg("§bНашёл: §f" + name + " §bза " + price + "$");
                    break;
                }
            }
        }
    }

    private void processBuyRequests(GenericContainerScreen screen) {
        BuyRequest request;
        while ((request = network.pollBuyRequest()) != null) {
            buyItem(screen, request.itemName, request.price);
        }
    }

    private void buyItem(GenericContainerScreen screen, String itemName, int price) {
        int syncId = screen.getScreenHandler().syncId;

        for (int i = 0; i < 45 && i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String name = AuctionUtils.funTimePricePattern.matcher(stack.getName().getString()).replaceAll("").trim();
            int stackPrice = AuctionUtils.getPrice(stack);

            if (name.equals(itemName) && stackPrice == price) {
                mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                msg("§a✓ КУПИЛ: §f" + itemName + " §aза " + price + "$");
                return;
            }
        }

        msg("§c✗ Не нашёл: §f" + itemName + " §cза " + price + "$");
    }

    private void msg(String text) {
        if (notifications.isValue() && mc.player != null) {
            ChatMessage.autobuymessage(text);
        }
    }

    public NetworkManager getNetworkManager() {
        return network;
    }

    public boolean isFullyEnabled() {
        return isState() && autoBuyManager.isEnabled();
    }
}