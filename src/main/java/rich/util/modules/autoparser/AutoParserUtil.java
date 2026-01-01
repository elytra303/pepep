package rich.util.modules.autoparser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.AuctionUtils;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.util.string.chat.ChatMessage;
import rich.util.timer.StopWatch;
import rich.util.timer.TimerUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoParserUtil {
    private static AutoParserUtil instance;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final AutoParserConfig config = AutoParserConfig.getInstance();
    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    private final TimerUtil actionTimer = TimerUtil.create();
    private final TimerUtil commandTimer = TimerUtil.create();
    private final StopWatch antiAfkWatch = new StopWatch();

    private ParserState state = ParserState.IDLE;

    private int currentItemIndex = 0;
    private int currentPage = 1;
    private int totalPages = 1;
    private String currentSearchItem = "";
    private String[] currentAutoBuyNames = new String[0];
    private final Map<String, Integer> lowestPricesFound = new HashMap<>();

    private final Map<String, Integer> parsedPrices = new HashMap<>();
    private int updatedCount = 0;
    private int skippedCount = 0;

    private static final Pattern PAGE_PATTERN = Pattern.compile("\\[(\\d+)/(\\d+)]");

    private static final int MAX_PAGES_TO_SCAN = 2;
    private static final long ANTI_AFK_INTERVAL = 20000;
    private static final long PAGE_CLICK_DELAY = 200;
    private static final long CHECK_INTERVAL = 500;
    private static final long COMMAND_DELAY = 300;
    private static final long COMMAND_RETRY_DELAY = 1500;
    private static final int MAX_WAIT_ATTEMPTS = 60;
    private static final int MAX_RETRIES = 3;

    private int waitAttempts = 0;
    private int antiAfkAction = 0;
    private int retryCount = 0;
    private String lastFoundTitle = "";
    private int pageChangeAttempts = 0;
    private String titleBeforePageClick = "";
    private boolean commandSent = false;

    private enum ParserState {
        IDLE,
        PREPARING_COMMAND,
        SENDING_COMMAND,
        WAITING_FOR_AUCTION,
        SCANNING_PAGE,
        CLICKING_NEXT_PAGE,
        WAITING_PAGE_CHANGE,
        FINISHING_ITEM,
        FINISHED
    }

    private AutoParserUtil() {}

    public static AutoParserUtil getInstance() {
        if (instance == null) {
            instance = new AutoParserUtil();
        }
        return instance;
    }

    public void onTick() {
        if (!config.isRunning()) return;
        if (mc.player == null || mc.world == null) return;

        if (antiAfkWatch.finished(ANTI_AFK_INTERVAL)) {
            performAntiAfk();
            antiAfkWatch.reset();
        }

        switch (state) {
            case PREPARING_COMMAND -> handlePreparingCommand();
            case SENDING_COMMAND -> handleSendingCommand();
            case WAITING_FOR_AUCTION -> handleWaitingForAuction();
            case SCANNING_PAGE -> handleScanningPage();
            case CLICKING_NEXT_PAGE -> handleClickingNextPage();
            case WAITING_PAGE_CHANGE -> handleWaitingPageChange();
            case FINISHING_ITEM -> handleFinishingItem();
            case FINISHED -> handleFinished();
        }
    }

    private void performAntiAfk() {
        if (mc.player == null) return;

        antiAfkAction++;
        switch (antiAfkAction % 4) {
            case 0 -> {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.setYaw(mc.player.getYaw() + 5);
            }
            case 1 -> {
                mc.player.swingHand(Hand.OFF_HAND);
                mc.player.setYaw(mc.player.getYaw() - 5);
            }
            case 2 -> {
                mc.player.setPitch(mc.player.getPitch() + 3);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            case 3 -> {
                mc.player.setPitch(mc.player.getPitch() - 3);
                mc.player.swingHand(Hand.OFF_HAND);
            }
        }
    }

    private void handlePreparingCommand() {
        if (!actionTimer.hasTimeElapsed(COMMAND_DELAY)) return;

        if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
            actionTimer.resetCounter();
            return;
        }

        commandSent = false;
        state = ParserState.SENDING_COMMAND;
        commandTimer.resetCounter();
        actionTimer.resetCounter();
    }

    private void handleSendingCommand() {
        if (mc.player == null || mc.player.networkHandler == null) {
            debug("§c[Error] Player or networkHandler is null!");
            actionTimer.resetCounter();
            return;
        }

        if (!commandSent) {
            try {
                String command = "ah search " + currentSearchItem;
                mc.player.networkHandler.sendChatCommand(command);
                debug("§7[Command] /" + command);
                commandSent = true;
                commandTimer.resetCounter();
            } catch (Exception e) {
                debug("§c[Error] Failed to send command: " + e.getMessage());
                try {
                    mc.player.networkHandler.sendChatMessage("/ah search " + currentSearchItem);
                    commandSent = true;
                    commandTimer.resetCounter();
                } catch (Exception ex) {
                    debug("§c[Error] Fallback also failed");
                }
            }
        }

        if (commandTimer.hasTimeElapsed(COMMAND_RETRY_DELAY)) {
            if (!commandSent) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    debug("§c[Skip] Cannot send command for: " + currentSearchItem);
                    skippedCount++;
                    goToNextItem();
                    return;
                }
                commandSent = false;
                commandTimer.resetCounter();
                return;
            }

            state = ParserState.WAITING_FOR_AUCTION;
            waitAttempts = 0;
            actionTimer.resetCounter();
        }
    }

    private void handleWaitingForAuction() {
        if (!actionTimer.hasTimeElapsed(CHECK_INTERVAL)) return;
        actionTimer.resetCounter();

        waitAttempts++;

        if (waitAttempts > MAX_WAIT_ATTEMPTS) {
            retryCount++;
            if (retryCount < MAX_RETRIES) {
                debug("§e[Retry " + retryCount + "/" + MAX_RETRIES + "] " + currentSearchItem);
                state = ParserState.PREPARING_COMMAND;
                waitAttempts = 0;
                lastFoundTitle = "";
                commandSent = false;
                actionTimer.resetCounter();
                return;
            }

            debug("§c[Skip] " + currentSearchItem);
            skippedCount++;
            goToNextItem();
            return;
        }

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            return;
        }

        String title = screen.getTitle().getString();
        String titleLower = title.toLowerCase();

        if (title.equals(lastFoundTitle)) {
            return;
        }

        if (titleLower.contains("не найден") || titleLower.contains("ничего") ||
                titleLower.contains("пусто") || titleLower.contains("нет результатов") ||
                titleLower.contains("товары не найдены") || titleLower.contains("not found")) {
            debug("§c[Not Found] " + currentSearchItem);
            skippedCount++;
            goToNextItem();
            return;
        }

        Matcher matcher = PAGE_PATTERN.matcher(title);
        if (matcher.find()) {
            currentPage = Integer.parseInt(matcher.group(1));
            int realTotalPages = Integer.parseInt(matcher.group(2));
            totalPages = Math.min(realTotalPages, MAX_PAGES_TO_SCAN);
            lastFoundTitle = title;
            state = ParserState.SCANNING_PAGE;
            return;
        }

        String searchLower = currentSearchItem.toLowerCase();
        boolean titleMatchesSearch = titleLower.contains(searchLower) || containsAnyWord(titleLower, searchLower);

        if (titleLower.contains("поиск") || titleLower.contains("search") ||
                titleLower.contains("аукцион") || titleLower.contains("auction") ||
                titleLower.contains("ah") || titleMatchesSearch) {

            boolean hasItems = false;
            int slotsToCheck = Math.min(45, screen.getScreenHandler().slots.size());
            for (int i = 0; i < slotsToCheck; i++) {
                Slot slot = screen.getScreenHandler().slots.get(i);
                if (!slot.getStack().isEmpty()) {
                    int price = AuctionUtils.getPrice(slot.getStack());
                    if (price > 0) {
                        hasItems = true;
                        break;
                    }
                }
            }

            if (!hasItems) {
                return;
            }

            currentPage = 1;
            totalPages = 1;
            lastFoundTitle = title;
            state = ParserState.SCANNING_PAGE;
        }
    }

    private void handleScanningPage() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            state = ParserState.FINISHING_ITEM;
            return;
        }

        String currentTitle = screen.getTitle().getString();
        Matcher matcher = PAGE_PATTERN.matcher(currentTitle);
        if (matcher.find()) {
            int actualPage = Integer.parseInt(matcher.group(1));
            if (actualPage > MAX_PAGES_TO_SCAN) {
                state = ParserState.FINISHING_ITEM;
                return;
            }
            currentPage = actualPage;
        }

        int slotsToScan = Math.min(45, screen.getScreenHandler().slots.size());

        for (int i = 0; i < slotsToScan; i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) continue;

            String itemName = stack.getName().getString();

            for (String autoBuyName : currentAutoBuyNames) {
                if (matchesItem(stack, itemName, autoBuyName)) {
                    int currentLowest = lowestPricesFound.getOrDefault(autoBuyName, Integer.MAX_VALUE);
                    if (price < currentLowest) {
                        lowestPricesFound.put(autoBuyName, price);
                    }
                }
            }
        }

        if (currentPage < totalPages && currentPage < MAX_PAGES_TO_SCAN) {
            state = ParserState.CLICKING_NEXT_PAGE;
            titleBeforePageClick = currentTitle;
            actionTimer.resetCounter();
        } else {
            state = ParserState.FINISHING_ITEM;
        }
    }

    private void handleClickingNextPage() {
        if (!actionTimer.hasTimeElapsed(PAGE_CLICK_DELAY)) return;

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            state = ParserState.FINISHING_ITEM;
            return;
        }

        try {
            int syncId = screen.getScreenHandler().syncId;
            mc.interactionManager.clickSlot(syncId, 50, 0, SlotActionType.PICKUP, mc.player);
        } catch (Exception ignored) {}

        state = ParserState.WAITING_PAGE_CHANGE;
        pageChangeAttempts = 0;
        actionTimer.resetCounter();
    }

    private void handleWaitingPageChange() {
        if (!actionTimer.hasTimeElapsed(100)) return;
        actionTimer.resetCounter();

        pageChangeAttempts++;

        if (pageChangeAttempts > 30) {
            state = ParserState.FINISHING_ITEM;
            return;
        }

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            state = ParserState.FINISHING_ITEM;
            return;
        }

        String newTitle = screen.getTitle().getString();
        if (!newTitle.equals(titleBeforePageClick)) {
            Matcher matcher = PAGE_PATTERN.matcher(newTitle);
            if (matcher.find()) {
                int newPage = Integer.parseInt(matcher.group(1));
                if (newPage > MAX_PAGES_TO_SCAN) {
                    state = ParserState.FINISHING_ITEM;
                    return;
                }
            }
            state = ParserState.SCANNING_PAGE;
        }
    }

    private void handleFinishingItem() {
        for (Map.Entry<String, Integer> entry : lowestPricesFound.entrySet()) {
            String autoBuyName = entry.getKey();
            int lowestPrice = entry.getValue();

            if (lowestPrice < Integer.MAX_VALUE) {
                int discountedPrice = calculateDiscountedPrice(lowestPrice);
                parsedPrices.put(autoBuyName, discountedPrice);

                boolean updated = updateAutoBuyPrice(autoBuyName, discountedPrice);

                if (updated) {
                    ChatMessage.autobuymessageSuccess("§a✓ §f" + autoBuyName + "§7: " + formatPrice(lowestPrice) + " → §b" + formatPrice(discountedPrice));
                    updatedCount++;
                }
            }
        }

        goToNextItem();
    }

    private void handleFinished() {
        try {
            if (mc.player != null && mc.currentScreen != null) {
                mc.player.closeHandledScreen();
            }
        } catch (Exception ignored) {}

        ChatMessage.autobuymessageSuccess("§a══════════════════════════════");
        ChatMessage.autobuymessageSuccess("§a✓ AutoParser завершён!");
        ChatMessage.autobuymessage("§7Обновлено: §a" + updatedCount);
        ChatMessage.autobuymessageSuccess("§a══════════════════════════════");

        fullReset();
    }

    private void goToNextItem() {
        currentItemIndex++;
        lastFoundTitle = "";
        commandSent = false;

        List<AutoParserItems.ParserItemEntry> items = AutoParserItems.getItems();

        if (currentItemIndex >= items.size()) {
            state = ParserState.FINISHED;
            return;
        }

        prepareNextItem();
    }

    private void prepareNextItem() {
        List<AutoParserItems.ParserItemEntry> items = AutoParserItems.getItems();
        AutoParserItems.ParserItemEntry entry = items.get(currentItemIndex);

        currentSearchItem = entry.getSearchQuery();
        currentAutoBuyNames = entry.getAutoBuyNames();
        currentPage = 1;
        totalPages = 1;
        lowestPricesFound.clear();
        waitAttempts = 0;
        retryCount = 0;
        commandSent = false;

        for (String name : currentAutoBuyNames) {
            lowestPricesFound.put(name, Integer.MAX_VALUE);
        }

        debug("§7[" + (currentItemIndex + 1) + "/" + items.size() + "] §b" + currentSearchItem);

        state = ParserState.PREPARING_COMMAND;
        actionTimer.resetCounter();
    }

    private void fullReset() {
        currentItemIndex = 0;
        currentPage = 1;
        totalPages = 1;
        currentSearchItem = "";
        currentAutoBuyNames = new String[0];
        lowestPricesFound.clear();
        parsedPrices.clear();
        updatedCount = 0;
        skippedCount = 0;
        waitAttempts = 0;
        antiAfkAction = 0;
        retryCount = 0;
        lastFoundTitle = "";
        pageChangeAttempts = 0;
        titleBeforePageClick = "";
        commandSent = false;
        state = ParserState.IDLE;

        config.reset();
    }

    public void start() {
        if (config.isRunning()) {
            ChatMessage.autobuymessageWarning("AutoParser уже запущен!");
            return;
        }

        if (mc.player == null || mc.world == null) {
            ChatMessage.autobuymessageError("Игрок не в мире!");
            return;
        }

        fullReset();

        List<AutoParserItems.ParserItemEntry> items = AutoParserItems.getItems();
        if (items.isEmpty()) {
            ChatMessage.autobuymessageError("Список предметов для парсинга пуст!");
            return;
        }

        config.setRunning(true);
        config.setEnabled(true);

        ChatMessage.autobuymessageSuccess("§a══════ AutoParser ══════");
        ChatMessage.autobuymessage("§7Предметов: §b" + items.size() + " §7| Скидка: §b" + config.getDiscountPercent() + "%");
        ChatMessage.autobuymessageSuccess("§a═════════════════════════");

        antiAfkWatch.reset();
        actionTimer.resetCounter();
        commandTimer.resetCounter();

        prepareNextItem();
    }

    public void stop() {
        if (!config.isRunning()) return;

        ChatMessage.autobuymessageWarning("AutoParser остановлен");
        if (updatedCount > 0 || skippedCount > 0) {
            ChatMessage.autobuymessage("§7Обновлено: §a" + updatedCount);
        }
        fullReset();
    }

    private boolean containsAnyWord(String title, String search) {
        String[] words = search.split("\\s+");
        for (String word : words) {
            if (word.length() >= 3 && title.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesItem(ItemStack stack, String itemName, String autoBuyName) {
        String cleanItemName = itemName.toLowerCase()
                .replaceAll("§.", "")
                .trim();
        String cleanAutoBuyName = autoBuyName.toLowerCase()
                .replace("[★] ", "")
                .replace("[⚒] ", "")
                .replace("[❄] ", "")
                .replace("[🍹] ", "")
                .trim();

        if (cleanItemName.contains(cleanAutoBuyName)) {
            return true;
        }

        try {
            for (AutoBuyableItem item : autoBuyManager.getAllItems()) {
                if (item.getDisplayName().equals(autoBuyName)) {
                    if (AuctionUtils.compareItem(stack, item.createItemStack())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    private int calculateDiscountedPrice(int originalPrice) {
        double discount = config.getDiscountPercent() / 100.0;
        return (int) (originalPrice * (1 - discount));
    }

    private boolean updateAutoBuyPrice(String itemName, int newPrice) {
        try {
            for (AutoBuyableItem item : autoBuyManager.getAllItems()) {
                String displayName = item.getDisplayName();

                if (displayName.equals(itemName)) {
                    item.getSettings().setBuyBelow(newPrice);
                    item.getSettings().saveToConfig();
                    return true;
                }

                String cleanDisplayName = displayName
                        .replace("[★] ", "")
                        .replace("[⚒] ", "")
                        .replace("[❄] ", "")
                        .replace("[🍹] ", "")
                        .trim();
                String cleanItemName = itemName
                        .replace("[★] ", "")
                        .replace("[⚒] ", "")
                        .replace("[❄] ", "")
                        .replace("[🍹] ", "")
                        .trim();

                if (cleanDisplayName.equalsIgnoreCase(cleanItemName)) {
                    item.getSettings().setBuyBelow(newPrice);
                    item.getSettings().saveToConfig();
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void debug(String message) {
        if (config.isDebugMode()) {
            ChatMessage.autobuymessage(message);
        }
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.2fM$", price / 1000000.0);
        } else if (price >= 1000) {
            return String.format("%.1fK$", price / 1000.0);
        }
        return price + "$";
    }

    public boolean isRunning() {
        return config.isRunning();
    }

    public int getCurrentProgress() {
        return currentItemIndex;
    }

    public int getTotalItems() {
        return AutoParserItems.getItems().size();
    }

    public String getCurrentItem() {
        return currentSearchItem;
    }
}