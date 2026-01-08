package rich.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import rich.IMinecraft;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pressing implements IMinecraft {

    private final int[] funTimeTicks = new int[]{10, 11, 10, 13}, spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12}, defaultTicks = new int[]{10, 11};

    long lastClickTime = System.currentTimeMillis();

    private static final long MINIMUM_COOLDOWN_MS = 500;
    private static final long MACE_COOLDOWN_MS = 50;

    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        boolean isMace = isHoldingMace();

        assert mc.player != null;

        if (isMace) {
            return lastClickPassed() >= MACE_COOLDOWN_MS;
        }

        boolean cooldownReady = mc.player.getAttackCooldownProgress(ticks) > 0.9F;
        boolean minimumDelayPassed = lastClickPassed() >= MINIMUM_COOLDOWN_MS;

        return cooldownReady && minimumDelayPassed;
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
}