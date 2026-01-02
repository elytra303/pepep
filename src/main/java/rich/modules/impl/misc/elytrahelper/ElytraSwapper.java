package rich.modules.impl.misc.elytrahelper;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.IMinecraft;
import rich.util.inventory.InventoryUtility;

public class ElytraSwapper implements IMinecraft {

    public void swap() {
        boolean isElytraEquipped = mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA;

        for (int i = 0; i < 46; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
            if (component == null || component.slot() != EquipmentSlot.CHEST) continue;

            if ((stack.getItem() == Items.ELYTRA && !isElytraEquipped)
                    || (isElytraEquipped && stack.getItem() != Items.ELYTRA)) {

                InventoryUtility.swap(
                        InventoryUtility.wrapHotbar(i),
                        6
                );
                return;
            }
        }
    }
}