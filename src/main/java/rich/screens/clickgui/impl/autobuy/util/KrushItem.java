package rich.screens.clickgui.impl.autobuy.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuyItemSettings;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuySettingsManager;

public class KrushItem implements AutoBuyableItem {
    private final String displayName;
    private final Item material;
    private final ItemStack displayStack;
    private final int defaultPrice;
    private final AutoBuyItemSettings settings;
    private boolean enabled;

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice) {
        this.displayName = displayName;
        this.material = material;
        this.displayStack = displayStack;
        this.defaultPrice = defaultPrice;
        this.enabled = true;
        this.settings = new AutoBuyItemSettings(defaultPrice, material, displayName, true);
        AutoBuySettingsManager.getInstance().loadSettings(displayName, this.settings);
    }

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice, boolean canHaveQuantity) {
        this.displayName = displayName;
        this.material = material;
        this.displayStack = displayStack;
        this.defaultPrice = defaultPrice;
        this.enabled = true;
        this.settings = new AutoBuyItemSettings(defaultPrice, material, displayName, canHaveQuantity);
        AutoBuySettingsManager.getInstance().loadSettings(displayName, this.settings);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public ItemStack createItemStack() {
        return displayStack.copy();
    }

    @Override
    public int getPrice() {
        return settings.getBuyBelow();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public AutoBuyItemSettings getSettings() {
        return settings;
    }
}