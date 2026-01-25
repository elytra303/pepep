package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.Instance;

public class AutoSprint extends ModuleStructure {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    private static volatile boolean serverSprintState = false;
    private static volatile boolean sprintBlocked = false;
    private static volatile int blockTicksElapsed = 0;
    private static volatile boolean attackPending = false;
    private static final int MIN_BLOCK_TICKS = 1;
    private static final int MAX_BLOCK_TICKS = 8;

    @Getter
    private final BooleanSetting noReset = new BooleanSetting("Не сбрасывать спринт", "Don't reset sprint for crits")
            .setValue(false);

    public AutoSprint() {
        super("AutoSprint", ModuleCategory.MOVEMENT);
        settings(noReset);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.SEND) return;
        if (!(event.getPacket() instanceof ClientCommandC2SPacket packet)) return;

        if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            if (serverSprintState) {
                event.cancel();
                return;
            }
            serverSprintState = true;
        } else if (packet.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            if (!serverSprintState) {
                event.cancel();
                return;
            }
            serverSprintState = false;
        }
    }

    public static boolean isServerSprinting() {
        return serverSprintState;
    }

    public static void resetServerState() {
        serverSprintState = false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public static void blockSprint() {
        if (mc.player == null) return;

        sprintBlocked = true;
        blockTicksElapsed = 0;
        attackPending = true;

        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public static void unblockSprint() {
        sprintBlocked = false;
        blockTicksElapsed = 0;
        attackPending = false;
    }

    public static boolean isBlocked() {
        return sprintBlocked;
    }

    public static boolean isReadyForAttack() {
        return sprintBlocked && blockTicksElapsed >= MIN_BLOCK_TICKS;
    }

    public static boolean isAttackPending() {
        return attackPending;
    }

    public static void cancelPending() {
        attackPending = false;
        unblockSprint();
    }

    public static boolean hasAnyMovementInput() {
        if (mc.player == null) return false;

        return mc.player.input.playerInput.forward() ||
                mc.player.input.playerInput.backward() ||
                mc.player.input.playerInput.left() ||
                mc.player.input.playerInput.right();
    }

    public static boolean hasStrafingInput() {
        if (mc.player == null) return false;
        return mc.player.input.playerInput.left() || mc.player.input.playerInput.right();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        if (sprintBlocked) {
            blockTicksElapsed++;

            if (blockTicksElapsed > MAX_BLOCK_TICKS) {
                unblockSprint();
            }

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

        if (sneaking) return;

        if (canSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        unblockSprint();
        resetServerState();
    }
}