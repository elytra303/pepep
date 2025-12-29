package rich.screens.clickgui.impl.autobuy.manager;

import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.ItemRegistry;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;
import rich.util.string.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class AutoBuyManager {
    private static AutoBuyManager instance;

    private AutoBuyManager() {}

    public static AutoBuyManager getInstance() {
        if (instance == null) {
            instance = new AutoBuyManager();
        }
        return instance;
    }

    public void setEnabled(boolean enabled) {
        boolean wasEnabled = AutoBuyConfig.getInstance().isGlobalEnabled();
        AutoBuyConfig.getInstance().setGlobalEnabled(enabled);

        if (wasEnabled != enabled) {
            if (enabled) {
                ChatMessage.autobuymessageSuccess("Глобальный автобай включён");
            } else {
                ChatMessage.autobuymessageWarning("Глобальный автобай выключен");
            }
        }
    }

    public boolean isEnabled() {
        return AutoBuyConfig.getInstance().isGlobalEnabled();
    }

    public List<AutoBuyableItem> getAllItems() {
        return ItemRegistry.getAllItems();
    }

    public List<AutoBuyableItem> getEnabledItems() {
        List<AutoBuyableItem> enabled = new ArrayList<>();
        for (AutoBuyableItem item : getAllItems()) {
            if (item.isEnabled()) {
                enabled.add(item);
            }
        }
        return enabled;
    }

    public void toggleItem(AutoBuyableItem item) {
        item.setEnabled(!item.isEnabled());
        ItemRegistry.saveItemSettings(item);
    }
}