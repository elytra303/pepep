package rich.screens.clickgui.impl.autobuy.settings;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;

@Getter
@Setter
public class AutoBuyItemSettings {
    private int buyBelow;
    private int minQuantity = 1;
    private boolean canHaveQuantity = false;
    private Item material;
    private String displayName;

    public AutoBuyItemSettings(int defaultPrice, Item material, String displayName) {
        this.buyBelow = defaultPrice;
        this.material = material;
        this.displayName = displayName;
    }

    public AutoBuyItemSettings(int defaultPrice, Item material, String displayName, boolean canHaveQuantity) {
        this.buyBelow = defaultPrice;
        this.material = material;
        this.displayName = displayName;
        this.canHaveQuantity = canHaveQuantity;
    }
}