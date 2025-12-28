package rich.screens.clickgui.impl.autobuy.window;

import net.minecraft.item.ItemStack;

public interface AutoBuyableItem {
    String getDisplayName();
    ItemStack createItemStack();
    int getPrice();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    AutoBuyItemSettings getSettings();
}