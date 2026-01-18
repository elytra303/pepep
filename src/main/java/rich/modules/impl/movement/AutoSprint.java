package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
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
        super("AutoSprint", ModuleCategory.MOVEMENT);
        setup(noReset);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public static void blockSprint() {
        if (mc.player == null) return;
        sprintBlocked = true;
        sprintBlockTicks = 0;
        mc.player.setSprinting(false);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public static void unblockSprint() {
        sprintBlocked = false;
        sprintBlockTicks = 0;
    }

    public static boolean isBlocked() {
        return sprintBlocked;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public static void tickBlockTimeout() {
        if (!sprintBlocked) return;

        sprintBlockTicks++;
        if (sprintBlockTicks > MAX_BLOCK_TICKS) {
            unblockSprint();
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        tickBlockTimeout();

        if (sprintBlocked) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            return;
        }

        processSprint();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processSprint() {
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
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        unblockSprint();
    }
}