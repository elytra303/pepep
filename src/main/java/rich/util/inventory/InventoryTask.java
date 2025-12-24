package rich.util.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import rich.IMinecraft;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InventoryTask implements IMinecraft {

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, type, mc.player);
    }

    public static void closeScreen(boolean packet) {
        if (packet) mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        else mc.player.closeHandledScreen();
    }

    public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
        if (slot != null) clickSlot(slot.id, button, clickType, silent);
    }

    public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent);
    }

    public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, mc.player);
        if (silent) mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, mc.player);
    }

    public static Slot getSlot(Item item) {
        return getSlot(item, s -> true);
    }

    public static Slot getSlot(Item item, Predicate<Slot> filter) {
        return getSlot(item, Comparator.comparingInt(s -> 0), filter);
    }

    public static Slot getSlot(Predicate<Slot> filter) {
        return slots().filter(filter).findFirst().orElse(null);
    }

    public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        return slots().filter(filter).max(comparator).orElse(null);
    }

    public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        return slots().filter(s -> s.getStack().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
    }

    public static Slot getFoodMaxSaturationSlot() {
        return slots().filter(s -> s.getStack().get(DataComponentTypes.FOOD) != null && !s.getStack().get(DataComponentTypes.FOOD).canAlwaysEat())
                .max(Comparator.comparingDouble(s -> s.getStack().get(DataComponentTypes.FOOD).saturation())).orElse(null);
    }

    public static Slot getSlot(List<Item> item) {
        return slots().filter(s -> item.contains(s.getStack().getItem())).findFirst().orElse(null);
    }

    public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
        return slots().filter(s -> {
            PotionContentsComponent component = s.getStack().get(DataComponentTypes.POTION_CONTENTS);
            if (component == null) return false;
            return StreamSupport.stream(component.getEffects().spliterator(), false).anyMatch(e -> e.getEffectType().equals(effect));
        }).findFirst().orElse(null);
    }

    public static Slot getPotionFromCategory(StatusEffectCategory category) {
        return slots().filter(s -> {
            ItemStack stack = s.getStack();
            PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (!stack.getItem().equals(Items.SPLASH_POTION) || component == null) return false;
            StatusEffectCategory category2 = category.equals(StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> e.getEffectType().value().getCategory().equals(category)).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> e.getEffectType().value().getCategory().equals(category2)).count();
            return effects >= effects2;
        }).findFirst().orElse(null);
    }

    public static int getInventoryCount(Item item) {
        return IntStream.range(0, 45).filter(i -> Objects.requireNonNull(mc.player).getInventory().getStack(i).getItem().equals(item)).map(i -> mc.player.getInventory().getStack(i).getCount()).sum();
    }

    public static int getHotbarItems(List<Item> items) {
        return IntStream.range(0, 9).filter(i -> items.contains(mc.player.getInventory().getStack(i).getItem())).findFirst().orElse(-1);
    }

    public static int getHotbarSlotId(IntPredicate filter) {
        return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
    }

    public static int getCount(Predicate<Slot> filter) {
        return slots().filter(filter).mapToInt(s -> s.getStack().getCount()).sum();
    }

    public static Slot mainHandSlot() {
        long count = slots().count();
        int i = count == 46 ? 10 : 9;
        return slots().toList().get(Math.toIntExact(count - i + mc.player.getInventory().getSelectedSlot()));
    }

    public static boolean isServerScreen() {
        return slots().toList().size() != 46;
    }

    public static Stream<Slot> slots() {
        return mc.player.currentScreenHandler.slots.stream();
    }


    public static String getCleanName(Text text) {
        if (text == null) return "";
        String name = text.getString();
        if (name == null) return "";
        return name.replaceAll("§[0-9a-fk-or]", "").toLowerCase();
    }
}