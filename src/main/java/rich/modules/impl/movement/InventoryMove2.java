package rich.modules.impl.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.implement.*;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryMove2 extends ModuleStructure {
    private final List<Packet<?>> packets = new ArrayList<>();
    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Vanilla", "Legit")
            .selected("Legit");

    enum MovePhase { READY, SLOWING_DOWN, ALLOW_MOVEMENT, SEND_PACKETS, SPEEDING_UP, FINISHED }
    MovePhase movePhase = MovePhase.READY;
    long actionStartTime = 0L;
    boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed, wasJumpPressed;
    boolean keysOverridden = false;
    boolean inventoryOpened = false;
    boolean packetsHeld = false;

    public InventoryMove2() {
        super("InventoryMove2", "Inventory Move2", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mode.isSelected("Legit")) {
            switch (e.getPacket()) {
//                case ClickSlotC2SPacket slot when (packetsHeld || MoveUtil.hasPlayerMovement() || InventoryUtility.isWriting())
//                        && InventoryFlowManager.shouldSkipExecution() -> {
//                    packets.add(slot);
//                    e.cancel();
//                    packetsHeld = true;
//                }
//                case CloseScreenS2CPacket screen when screen.getSyncId() == 0 -> e.cancel();
                default -> {
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mode.isSelected("Legit")) {
            processLegitMovement();
        } else {
//            if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution()) {
//                InventoryFlowManager.updateMoveKeys();
//            }
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (!mode.isSelected("Legit")) {
            Screen currentScreen = mc.currentScreen;
            if (currentScreen instanceof ChatScreen) return;
//            InventoryFlowManager.updateMoveKeys();
            return;
        }
        if (movePhase == MovePhase.SLOWING_DOWN || movePhase == MovePhase.SEND_PACKETS) {
            e.inputNone();
        } else {
            Screen currentScreen = mc.currentScreen;
            if (currentScreen instanceof ChatScreen) return;
//            InventoryFlowManager.updateMoveKeys();
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
        wasForwardPressed = mc.options.forwardKey.isPressed();
        wasBackPressed = mc.options.backKey.isPressed();
        wasLeftPressed = mc.options.leftKey.isPressed();
        wasRightPressed = mc.options.rightKey.isPressed();
        wasJumpPressed = mc.options.jumpKey.isPressed();

        movePhase = MovePhase.ALLOW_MOVEMENT;
        keysOverridden = false;
        packetsHeld = false;
    }

    private void handleMovementStates() {
        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (movePhase) {
            case SLOWING_DOWN -> {
                if (!keysOverridden) {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.options.jumpKey.setPressed(false);
                    keysOverridden = true;
                }

                if (elapsed > 0) {
                    movePhase = MovePhase.SEND_PACKETS;
                    actionStartTime = System.currentTimeMillis();
                }
            }

            case ALLOW_MOVEMENT -> {
//                if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution()) {
//                    InventoryFlowManager.updateMoveKeys();
//                }
            }

            case SEND_PACKETS -> {
                if (!packets.isEmpty()) {
//                    packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                    packets.clear();
                }
                packetsHeld = false;
                movePhase = MovePhase.SPEEDING_UP;
                actionStartTime = System.currentTimeMillis();
            }

            case SPEEDING_UP -> {
                if (keysOverridden) {
                    restoreKeyStates();
                }
                if (System.currentTimeMillis() - actionStartTime > 0) {
                    movePhase = MovePhase.FINISHED;
                }
            }

            case FINISHED -> resetState();
        }
    }

    private void restoreKeyStates() {
        mc.options.forwardKey.setPressed(wasForwardPressed);
        mc.options.backKey.setPressed(wasBackPressed);
        mc.options.leftKey.setPressed(wasLeftPressed);
        mc.options.rightKey.setPressed(wasRightPressed);
        mc.options.jumpKey.setPressed(wasJumpPressed);
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

//    @EventHandler
//    public void onClickSlot(ClickSlotEvent e) {
//        if (mode.isSelected("Legit")) {
//            SlotActionType actionType = e.getActionType();
//            if ((packetsHeld || MoveUtil.hasPlayerMovement() || InventoryUtility.isWriting()) &&
//                    ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW))
//                            || actionType.equals(SlotActionType.PICKUP_ALL))) {
//                e.cancel();
//            }
//        }
//    }

//    @EventHandler
//    public void onCloseScreen(CloseScreenEvent e) {
//        if (mode.isSelected("Legit") && packetsHeld && movePhase == MovePhase.ALLOW_MOVEMENT) {
//            movePhase = MovePhase.SLOWING_DOWN;
//            actionStartTime = System.currentTimeMillis();
//        }
//    }
}