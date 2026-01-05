package rich.modules.impl.misc.elytrahelper;

import net.minecraft.item.Item;
import rich.IMinecraft;
import rich.util.inventory.impl.InventoryUtility;

public class FireworkUser implements IMinecraft {

    public void useItemOnHotbar(Item item) {
        int slot = getItemOnHotbar(item);

        if (slot == -1) {
            int fireworkSlot = InventoryUtility.find(item);
            if (fireworkSlot != -1) {
                int freeHotbarSlot = findFreeHotbarSlot();
                if (freeHotbarSlot != -1) {
                    InventoryUtility.swap(
                        InventoryUtility.wrapHotbar(fireworkSlot),
                        InventoryUtility.wrapHotbar(freeHotbarSlot)
                    );
                    slot = freeHotbarSlot;
                }
            }
        }

        if (slot != -1 && !mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
            InventoryUtility.useItem(slot, false);
        }
    }

    private int findFreeHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int getItemOnHotbar(Item items) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == items) {
                slot = i;
                break;
            }
        }
        return slot;
    }
}