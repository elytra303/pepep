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

    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        boolean isMace = isHoldingMace();

        float cooldownProgress = mc.player.getAttackCooldownProgress(ticks);

        return isMace || cooldownProgress > 0.9F;
    }


//    public boolean hasTicksElapsedSinceLastClick(int ticks) {eeee
//        return lastClickPassed() >= (ticks * 50L * (20F / Network.TPS));
//    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate() {
        lastClickTime = System.currentTimeMillis();
    }

    private boolean isHoldingMace() {
        ItemStack mainHand = mc.player.getMainHandStack();
        return mainHand.getItem().getTranslationKey().toLowerCase().contains("mace");
    }
}