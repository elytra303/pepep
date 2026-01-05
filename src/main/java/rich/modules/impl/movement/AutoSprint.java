package rich.modules.impl.movement;

import lombok.Getter;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.Instance;

public class AutoSprint extends ModuleStructure {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    public static volatile boolean sprintBlocked = false;
    public static volatile int sprintBlockTicks = 0;
    private static final int MAX_BLOCK_TICKS = 10;

    @Getter
    private final BooleanSetting noReset = new BooleanSetting("Не сбрасывать спринт", "Don't reset sprint for crits")
            .setValue(false);

    public AutoSprint() {
        super("AutoSprint", null, ModuleCategory.MOVEMENT);
        setup(noReset);
    }

    public static void blockSprint() {
        if (mc.player == null) return;
        sprintBlocked = true;
        sprintBlockTicks = 0;
        mc.player.setSprinting(false);
    }

    public static void unblockSprint() {
        sprintBlocked = false;
        sprintBlockTicks = 0;
    }

    public static boolean isBlocked() {
        return sprintBlocked;
    }

    public static void tickBlockTimeout() {
        if (!sprintBlocked) return;

        sprintBlockTicks++;
        if (sprintBlockTicks > MAX_BLOCK_TICKS) {
            unblockSprint();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        tickBlockTimeout();

        if (sprintBlocked) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            return;
        }

        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();
        boolean canSprint = !horizontal && mc.player.forwardSpeed > 0;

        if (sneaking) {
            return;
        }

        if (canSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void deactivate() {
        unblockSprint();
    }
}