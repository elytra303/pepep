package rich.modules.impl.misc;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.KeyEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.misc.elytrahelper.ElytraSwapper;
import rich.modules.impl.misc.elytrahelper.FireworkUser;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.inventory.InventoryUtils;
import rich.util.string.chat.ChatMessage;
import rich.util.timer.StopWatch;

public class ElytraHelper extends ModuleStructure {

    private final BindSetting swapBind = new BindSetting("Кнопка свапа", "Кнопка для смены элитры/нагрудника");
    private final BindSetting fireworkBind = new BindSetting("Кнопка фейерверка", "Кнопка для использования фейерверка");
    private final BooleanSetting autoTakeoff = new BooleanSetting("Авто взлёт", "Автоматический взлёт при прыжке").setValue(false);
    private final BooleanSetting autoFirework = new BooleanSetting("Авто фейерверк", "Автоматическое использование фейерверка при взлёте").setValue(false);

    private final ElytraSwapper swapper = new ElytraSwapper();
    private final FireworkUser fireworkUser = new FireworkUser();
    private final StopWatch fireworkTimer = new StopWatch();
    private final StopWatch jumpTimer = new StopWatch();

    private boolean wasOnGround = true;
    private boolean jumpPressed = false;

    public ElytraHelper() {
        super("ElytraHelper", "Elytra Helper", ModuleCategory.MISC);
        setup(swapBind, fireworkBind, autoTakeoff, autoFirework);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.currentScreen != null) return;

        if (e.isKeyDown(swapBind.getKey())) {
            boolean isElytraEquipped = mc.player.getInventory().getStack(38).getItem().equals(Items.ELYTRA);
            String itemName = isElytraEquipped ? "Нагрудник" : "Элитру";
            ChatMessage.brandmessage("Свапнул на " + itemName);
            swapper.swap();
            InventoryUtils.closeScreen();
        } else if (e.isKeyDown(fireworkBind.getKey())) {
            fireworkUser.useItemOnHotbar(Items.FIREWORK_ROCKET);
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean hasElytra = chestStack.get(DataComponentTypes.GLIDER) != null;

        if (autoTakeoff.isValue() && hasElytra) {
            boolean isOnGround = mc.player.isOnGround();

            if (isOnGround && !wasOnGround) {
                jumpPressed = false;
            }

            if (isOnGround && mc.options.jumpKey.isPressed() && jumpTimer.finished(300)) {
                mc.player.jump();
                jumpPressed = true;
                jumpTimer.reset();
            }

            if (!isOnGround && jumpPressed && !mc.player.isGliding() && !mc.player.getAbilities().flying) {
                if (chestStack.getMaxDamage() > 0 && chestStack.getDamage() < chestStack.getMaxDamage() - 1) {
                    mc.getNetworkHandler().sendPacket(
                            new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                    );
                    jumpPressed = false;
                }
            }

            wasOnGround = isOnGround;
        }

        if (autoFirework.isValue() && hasElytra && mc.player.isGliding()) {
            if (fireworkTimer.finished(1000)) {
                fireworkUser.useItemOnHotbar(Items.FIREWORK_ROCKET);
                fireworkTimer.reset();
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null) return;

        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean hasElytra = chestStack.get(DataComponentTypes.GLIDER) != null;

        if (autoTakeoff.isValue() && hasElytra && !mc.player.isOnGround() && jumpPressed && !mc.player.isGliding()) {
            event.setJumping(true);
        }
    }
}