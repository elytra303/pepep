package rich.util.modules.autobuy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import rich.screens.clickgui.impl.autobuy.AuctionUtils;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.util.string.chat.ChatMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionHandler {
    private Set<String> notFoundItems = ConcurrentHashMap.newKeySet();
    private Set<String> processedItems = ConcurrentHashMap.newKeySet();
    private Set<String> sentItems = ConcurrentHashMap.newKeySet();
    private Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    private int failedCount = 0;
    private int updateCount = 0;

    private AutoBuyManager autoBuyManager;

    public AuctionHandler(AutoBuyManager autoBuyManager) {
        this.autoBuyManager = autoBuyManager;
    }

    public void clear() {
        notFoundItems.clear();
        processedItems.clear();
        sentItems.clear();
        lastMessageTime.clear();
        failedCount = 0;
        updateCount = 0;
    }

    public void handleBuyRequest(MinecraftClient mc, int syncId, List<Slot> slots, BuyRequest request, NetworkManager networkManager) {
        ChatMessage.autobuymessage("§7Получен запрос: §f" + request.itemName + " §7за §a" + request.price + "$");

        Slot targetSlot = findSlotByItemAndPrice(slots, request.itemName, request.price);
        if (targetSlot != null) {
            mc.interactionManager.clickSlot(syncId, targetSlot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            ChatMessage.autobuymessageSuccess("§a✓ Куплено: §f" + request.itemName + " §7за §a" + request.price + "$");
            failedCount = 0;
        } else {
            String itemKey = request.itemName + "|" + request.price;
            if (!notFoundItems.contains(itemKey)) {
                notFoundItems.add(itemKey);
                ChatMessage.autobuymessageWarning("§c✗ Предмет не найден: §f" + request.itemName + " §7за §a" + request.price + "$");
            }
            failedCount++;
        }
    }

    public boolean shouldUpdate() {
        return failedCount > 3;
    }

    public void updateAuction(MinecraftClient mc, int syncId) {
        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
        notFoundItems.clear();
        failedCount = 0;
        updateCount++;
        ChatMessage.autobuymessage("§b⟳ Обновление аукциона #" + updateCount);
    }

    public void handleSuspiciousPrice(MinecraftClient mc, int syncId, List<Slot> slots) {
        Slot confirmSlot = slots.stream()
                .filter(slot -> !slot.getStack().isEmpty())
                .filter(slot -> slot.getStack().getItem() == Items.GREEN_STAINED_GLASS_PANE)
                .findFirst()
                .orElse(null);
        if (confirmSlot != null) {
            mc.interactionManager.clickSlot(syncId, confirmSlot.id, 0, SlotActionType.PICKUP, mc.player);
            ChatMessage.autobuymessageSuccess("§a✓ Подтверждена подозрительная цена");
        }
    }

    public List<Slot> findMatchingSlots(List<Slot> slots, List<AutoBuyableItem> cachedEnabledItems) {
        List<Slot> matching = new ArrayList<>();
        int checkedCount = 0;
        int skippedThorns = 0;
        int skippedPrice = 0;
        int skippedQuantity = 0;

        ChatMessage.autobuymessage("§7Начинаю поиск предметов. Активных в списке: §b" + cachedEnabledItems.size());

        for (int i = 0; i <= 44; i++) {
            if (i >= slots.size()) break;
            Slot slot = slots.get(i);
            if (slot.getStack().isEmpty()) continue;
            ItemStack stack = slot.getStack();
            checkedCount++;

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) {
                skippedThorns++;
                continue;
            }

            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) {
                skippedPrice++;
                continue;
            }

            String stackName = stack.getName().getString();
            String cleanStackName = AuctionUtils.funTimePricePattern.matcher(stackName).replaceAll("").trim();

            for (AutoBuyableItem item : cachedEnabledItems) {
                if (!item.isEnabled()) continue;

                int maxPrice = item.getSettings().getBuyBelow();
                if (price > maxPrice) continue;

                if (item.getSettings().isCanHaveQuantity()) {
                    int stackCount = stack.getCount();
                    if (stackCount < item.getSettings().getMinQuantity()) {
                        skippedQuantity++;
                        continue;
                    }
                }

                if (AuctionUtils.compareItem(stack, item.createItemStack())) {
                    matching.add(slot);
                    ChatMessage.autobuymessageSuccess("§a★ Вижу предмет: §f" + cleanStackName + " §7за §a" + price + "$ §7(лимит: §a" + maxPrice + "$§7)");
                    break;
                }
            }
        }

        if (matching.isEmpty()) {
            ChatMessage.autobuymessage("§7Проверено слотов: §b" + checkedCount + " §7| Ничего не найдено");
            if (skippedThorns > 0) ChatMessage.autobuymessage("§7Пропущено с шипами: §b" + skippedThorns);
            if (skippedPrice > 0) ChatMessage.autobuymessage("§7Без цены: §b" + skippedPrice);
            if (skippedQuantity > 0) ChatMessage.autobuymessage("§7Мало количество: §b" + skippedQuantity);
        } else {
            ChatMessage.autobuymessageSuccess("§a✓ Найдено предметов для покупки: §b" + matching.size());
        }

        matching.sort(Comparator.comparingInt(slot -> AuctionUtils.getPrice(slot.getStack())));
        return matching;
    }

    public void processBestSlots(List<Slot> bestSlots, NetworkManager networkManager) {
        Map<String, Integer> itemCounts = new HashMap<>();
        int sentCount = 0;

        for (Slot bestSlot : bestSlots) {
            ItemStack stack = bestSlot.getStack();
            String itemName = stack.getName().getString();
            String cleanName = AuctionUtils.funTimePricePattern.matcher(itemName).replaceAll("").trim();
            int price = AuctionUtils.getPrice(stack);
            String itemKey = cleanName + "|" + price;

            itemCounts.put(cleanName, itemCounts.getOrDefault(cleanName, 0) + 1);

            if (!sentItems.contains(itemKey)) {
                sentItems.add(itemKey);
                networkManager.sendBuy(cleanName, price);
                sentCount++;
                ChatMessage.autobuymessage("§7→ Отправлен запрос на покупку: §f" + cleanName + " §7за §a" + price + "$");
            }
        }

        if (sentCount > 0) {
            ChatMessage.autobuymessageSuccess("§a✓ Отправлено запросов: §b" + sentCount);
        }

        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String itemName = entry.getKey();
            Long lastTime = lastMessageTime.get(itemName);
            if (lastTime == null || currentTime - lastTime > 2000) {
                lastMessageTime.put(itemName, currentTime);
            }
        }
    }

    private Slot findSlotByItemAndPrice(List<Slot> slots, String itemName, int expectedPrice) {
        for (int i = 0; i <= 44; i++) {
            if (i >= slots.size()) break;
            Slot slot = slots.get(i);
            if (slot.getStack().isEmpty()) continue;
            ItemStack stack = slot.getStack();

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) {
                continue;
            }

            String stackName = stack.getName().getString();
            stackName = AuctionUtils.funTimePricePattern.matcher(stackName).replaceAll("").trim();
            int price = AuctionUtils.getPrice(stack);

            if (stackName.equals(itemName) && price == expectedPrice) {
                return slot;
            }
        }
        return null;
    }
}