package rich.util.inventory.impl;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import rich.IMinecraft;
import rich.manager.Manager;
import rich.util.network.NetworkUtility;

import java.util.List;

@UtilityClass
public class InventoryUtility implements IMinecraft {
    public boolean skipClient = false;
    private boolean isWriting = false;

    public void startWrite() {
        isWriting = true;
    }

    public void stopWrite() {
        isWriting = false;
    }

    public boolean isWriting() {
        return isWriting;
    }

    public void swap(int from, int to) {
        NetworkUtility.send(InventoryUtility.click(0, from, 0, SlotActionType.PICKUP));
        NetworkUtility.send(InventoryUtility.click(0, to, 0, SlotActionType.PICKUP));
        NetworkUtility.send(InventoryUtility.click(0, from, 0, SlotActionType.PICKUP));
    }

    public int find(Item item) {
        for (int i = 0; i < 44; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item)
                return i;
        }
        return -1;
    }

    public int find(Item item, boolean enchanted) {
        for (int i = 0; i < 44; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item && (!stack.hasEnchantments() || !enchanted))
                return i;
        }
        return find(item);
    }

    public int wrapHotbar(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public void useItem(int slot, boolean swing) {
        useItem(slot, swing, false, false);
    }

    public void useItem(int slot, boolean swing, boolean cooldown, boolean delay) {
        if (slot == -1) return;

        startWrite();

        if (slot < 9) {
            if (mc.player.getInventory().getSelectedSlot() != slot)
                NetworkUtility.send(new UpdateSelectedSlotC2SPacket(slot));

            NetworkUtility.sendUse(Hand.MAIN_HAND);
            if (swing) mc.player.swingHand(Hand.MAIN_HAND);

            if (delay) {
                Manager.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player.getInventory().getSelectedSlot() != slot)
                        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
                }, 3);
            } else {
                if (mc.player.getInventory().getSelectedSlot() != slot)
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
            }
        } else {
            int s = mc.player.getInventory().getSelectedSlot();
            if (cooldown) s = (s + 1) % 8;

            InventoryUtility.swap(
                    InventoryUtility.wrapHotbar(slot),
                    InventoryUtility.wrapHotbar(s)
            );

            if (cooldown) {
                if (mc.player.getInventory().getSelectedSlot() != s)
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(s));

                NetworkUtility.sendUse(Hand.MAIN_HAND);
                if (swing) mc.player.swingHand(Hand.MAIN_HAND);

                if (mc.player.getInventory().getSelectedSlot() != s)
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
            } else {
                NetworkUtility.sendUse(Hand.MAIN_HAND);
                if (swing) mc.player.swingHand(Hand.MAIN_HAND);
            }

            int finalS = s;
            Manager.SCHEDULER.scheduleOnce(() -> {
                InventoryUtility.swap(
                        InventoryUtility.wrapHotbar(slot),
                        InventoryUtility.wrapHotbar(finalS)
                );
                NetworkUtility.send(new CloseHandledScreenC2SPacket(finalS));
            }, 3);
        }

        stopWrite();
    }

    public int findHotbar(Item item) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                slot = i;
                break;
            }
        }

        return slot;
    }

    public ClickSlotC2SPacket click(int syncId, int slotId, int button, SlotActionType actionType) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        List<ItemStack> list = Lists.<ItemStack>newArrayListWithCapacity(i);

        for (Slot slot : defaultedList) {
            list.add(slot.getStack().copy());
        }
        Int2ObjectMap<ItemStackHash> int2ObjectMap = new Int2ObjectOpenHashMap<>();

        for (int j = 0; j < i; j++) {
            ItemStack itemStack = (ItemStack)list.get(j);
            ItemStack itemStack2 = defaultedList.get(j).getStack();
            if (!ItemStack.areEqual(itemStack, itemStack2)) {
                int2ObjectMap.put(j, ItemStackHash.fromItemStack(itemStack2, mc.getNetworkHandler().getComponentHasher()));
            }
        }

        ItemStackHash itemStackHash = ItemStackHash.fromItemStack(
                slotId == -999 ? screenHandler.getCursorStack() : mc.player.getInventory().getStack(slotId),
                mc.getNetworkHandler().getComponentHasher()
        );

        return new ClickSlotC2SPacket(
                syncId,
                screenHandler.getRevision(),
                Shorts.checkedCast(slotId),
                SignedBytes.checkedCast(button),
                actionType,
                int2ObjectMap,
                itemStackHash
        );
    }

    public void selectSlot(int slot) {
        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(slot));
    }
}