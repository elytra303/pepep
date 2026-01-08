package rich.modules.impl.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import rich.events.api.EventHandler;
import rich.events.impl.ClickSlotEvent;
import rich.events.impl.CloseScreenEvent;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.inventory.InventoryUtils;
import rich.util.move.MoveUtil;
import rich.util.string.PlayerInteractionHelper;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryMove extends ModuleStructure {

    private final List<Packet<?>> packets = new ArrayList<>();
    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Normal", "Legit")
            .selected("Legit");

    enum MovePhase { READY, SLOWING_DOWN, ALLOW_MOVEMENT, SPEEDING_UP, SEND_PACKETS, FINISHED }

    MovePhase movePhase = MovePhase.READY;
    long actionStartTime = 0L;
    boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed, wasJumpPressed;
    boolean keysOverridden = false;
    boolean inventoryOpened = false;
    boolean packetsHeld = false;

    public InventoryMove() {
        super("InventoryMove", "Inventory Move", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @Override
    public void deactivate() {
        resetState();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mode.isSelected("Legit")) {
            if (e.getPacket() instanceof ClickSlotC2SPacket slot) {
                if ((packetsHeld || MoveUtil.hasPlayerMovement()) && shouldSkipExecution()) {
                    packets.add(slot);
                    e.cancel();
                    packetsHeld = true;
                }
            } else if (e.getPacket() instanceof CloseScreenS2CPacket screen) {
                if (screen.getSyncId() == 0) {
                    e.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        if (mode.isSelected("Legit")) {
            processLegitMovement();
        } else {
            if (!isServerScreen() && shouldSkipExecution()) {
                updateMoveKeys();
            }
        }
    }

    private void processLegitMovement() {
        boolean hasOpenScreen = mc.currentScreen != null;

        if (hasOpenScreen && !inventoryOpened && movePhase == MovePhase.READY) {
            startLegitMovement();
            inventoryOpened = true;
        }

        if (!hasOpenScreen && inventoryOpened) {
            if (packetsHeld && movePhase == MovePhase.ALLOW_MOVEMENT) {
                movePhase = MovePhase.SLOWING_DOWN;
                actionStartTime = System.currentTimeMillis();
            } else if (!packetsHeld) {
                resetState();
            }
            inventoryOpened = false;
            return;
        }

        if (movePhase != MovePhase.READY) {
            handleMovementStates();
        }
    }

    private void startLegitMovement() {
        wasForwardPressed = isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode());
        wasBackPressed = isKeyPressed(mc.options.backKey.getDefaultKey().getCode());
        wasLeftPressed = isKeyPressed(mc.options.leftKey.getDefaultKey().getCode());
        wasRightPressed = isKeyPressed(mc.options.rightKey.getDefaultKey().getCode());
        wasJumpPressed = isKeyPressed(mc.options.jumpKey.getDefaultKey().getCode());

        movePhase = MovePhase.ALLOW_MOVEMENT;
        keysOverridden = false;
        packetsHeld = false;
    }

    private void handleMovementStates() {
        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (movePhase) {
            case SLOWING_DOWN -> {
                if (mc.player != null && mc.player.input != null) {
                    mc.player.input.playerInput = new PlayerInput(
                            false, false, false, false,
                            mc.player.input.playerInput.jump(),
                            mc.player.input.playerInput.sneak(),
                            mc.player.input.playerInput.sprint()
                    );
                }

                if (!keysOverridden) {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.options.jumpKey.setPressed(false);
                    keysOverridden = true;
                }

                if (elapsed > 1) {
                    movePhase = MovePhase.SEND_PACKETS;
                    actionStartTime = System.currentTimeMillis();
                }
            }

            case ALLOW_MOVEMENT -> {
                if (!isServerScreen() && shouldSkipExecution()) {
                    updateMoveKeys();
                }
            }

            case SEND_PACKETS -> {
                if (!packets.isEmpty()) {
                    packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                    packets.clear();
                    updateSlots();
                }
                packetsHeld = false;
                movePhase = MovePhase.SPEEDING_UP;
                actionStartTime = System.currentTimeMillis();
            }

            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - actionStartTime;
                float speedupProgress = Math.min(1.0f, speedupElapsed / 1.0f);

                if (keysOverridden) {
                    restoreKeyStates();
                }

                if (mc.player != null && mc.player.input != null) {
                    boolean forward = isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode());

                    if (speedupProgress > 0.5f && forward && !mc.player.isSprinting()) {
                        mc.player.setSprinting(true);
                    }
                }

                if (speedupElapsed > 1) {
                    movePhase = MovePhase.FINISHED;
                }
            }

            case FINISHED -> resetState();
        }
    }

    private void restoreKeyStates() {
        boolean currentForward = isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode());
        boolean currentBack = isKeyPressed(mc.options.backKey.getDefaultKey().getCode());
        boolean currentLeft = isKeyPressed(mc.options.leftKey.getDefaultKey().getCode());
        boolean currentRight = isKeyPressed(mc.options.rightKey.getDefaultKey().getCode());
        boolean currentJump = isKeyPressed(mc.options.jumpKey.getDefaultKey().getCode());

        mc.options.forwardKey.setPressed(wasForwardPressed && currentForward);
        mc.options.backKey.setPressed(wasBackPressed && currentBack);
        mc.options.leftKey.setPressed(wasLeftPressed && currentLeft);
        mc.options.rightKey.setPressed(wasRightPressed && currentRight);
        mc.options.jumpKey.setPressed(wasJumpPressed && currentJump);
        keysOverridden = false;
    }

    private void resetState() {
        if (keysOverridden) {
            restoreKeyStates();
        }
        movePhase = MovePhase.READY;
        inventoryOpened = false;
        packetsHeld = false;
        packets.clear();
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        if (mode.isSelected("Legit")) {
            SlotActionType actionType = e.getActionType();
            if ((packetsHeld || MoveUtil.hasPlayerMovement()) &&
                    ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW))
                            || actionType.equals(SlotActionType.PICKUP_ALL))) {
                e.cancel();
            }
        }
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent e) {
        if (mode.isSelected("Legit") && packetsHeld && movePhase == MovePhase.ALLOW_MOVEMENT) {
            movePhase = MovePhase.SLOWING_DOWN;
            actionStartTime = System.currentTimeMillis();
        }
    }

    private boolean isKeyPressed(int keyCode) {
        return InputUtil.isKeyPressed(mc.getWindow(), keyCode);
    }

    private void updateMoveKeys() {
        mc.options.forwardKey.setPressed(isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(isKeyPressed(mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(isKeyPressed(mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(isKeyPressed(mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(isKeyPressed(mc.options.jumpKey.getDefaultKey().getCode()));
    }

    private boolean shouldSkipExecution() {
        return mc.currentScreen != null
                && !(mc.currentScreen instanceof ChatScreen)
                && !(mc.currentScreen instanceof SignEditScreen)
                && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen)
                && !(mc.currentScreen instanceof StructureBlockScreen);
    }

    private boolean isServerScreen() {
        if (mc.player == null) return false;
        return mc.player.currentScreenHandler.slots.size() != 46;
    }

    private void updateSlots() {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                0, 0,
                SlotActionType.PICKUP_ALL,
                mc.player
        );
    }
}