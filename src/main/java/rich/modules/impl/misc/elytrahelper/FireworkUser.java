package rich.modules.impl.misc.elytrahelper;

import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import rich.IMinecraft;
import rich.util.inventory.InventoryResult;
import rich.util.inventory.InventoryUtils;

public class FireworkUser implements IMinecraft {

    public void useItemOnHotbar(Item item) {
        if (mc.player == null || mc.interactionManager == null) return;

        int slot = getItemOnHotbar(item);

        if (slot == -1) {
            InventoryResult result = InventoryUtils.find(item);
            if (result.found()) {
                int freeHotbarSlot = findFreeHotbarSlot();
                if (freeHotbarSlot != -1) {
                    InventoryUtils.swap(result.slot(), InventoryUtils.wrapSlot(freeHotbarSlot));
                    slot = freeHotbarSlot;
                }
            }
        }

        if (slot != -1 && !mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
            useItem(slot, false);
        }
    }

    private void useItem(int slot, boolean swing) {
        if (slot == -1 || mc.player == null || mc.getNetworkHandler() == null) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (currentSlot != slot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

        if (swing) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (currentSlot != slot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
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

    public int getItemOnHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}