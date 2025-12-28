package rich.screens.clickgui.impl.autobuy.manager;

import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

public class AutoBuyManager {
    private static AutoBuyManager instance;
    private boolean enabled = false;

    private AutoBuyManager() {}

    public static AutoBuyManager getInstance() {
        if (instance == null) {
            instance = new AutoBuyManager();
        }
        return instance;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
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