package rich.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import rich.IMinecraft;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pressing implements IMinecraft {

    long lastClickTime = System.currentTimeMillis();

    private static final long MINIMUM_COOLDOWN_MS = 550;
    private static final long MACE_COOLDOWN_MS = 50;

    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        if (mc.player == null) return false;

        if (isHoldingMace()) {
            return lastClickPassed() >= MACE_COOLDOWN_MS;
        }

        float cooldownProgress = mc.player.getAttackCooldownProgress(ticks);
        boolean minimumDelayPassed = lastClickPassed() >= MINIMUM_COOLDOWN_MS;

        if (isWeapon()) {
            return cooldownProgress >= 0.9F && minimumDelayPassed;
        }

        return minimumDelayPassed;
    }

    public boolean isMaceFastAttack() {
        return isHoldingMace() && lastClickPassed() >= MACE_COOLDOWN_MS;
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate() {
        lastClickTime = System.currentTimeMillis();
    }

    public boolean isHoldingMace() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandStack();
        return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
    }

    public boolean isWeapon() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand.isEmpty()) return false;
        String itemName = mainHand.getItem().getTranslationKey().toLowerCase();
        return itemName.contains("sword") || itemName.contains("axe") || itemName.contains("trident");
    }
}