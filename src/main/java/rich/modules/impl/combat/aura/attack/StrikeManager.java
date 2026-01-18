package rich.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;
import rich.events.api.types.EventType;
import rich.events.impl.PacketEvent;
import rich.events.impl.UsingItemEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.TriggerBot;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.movement.AutoSprint;
import rich.modules.impl.movement.ElytraTarget;
import rich.util.player.PlayerSimulation;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StrikeManager implements IMinecraft {
    private final Pressing clickScheduler = new Pressing();
    private final StopWatch attackTimer = new StopWatch();
    private final StopWatch shieldWatch = new StopWatch();

    private int count = 0;
    private int ticksOnBlock = 0;
    private boolean pendingAttack = false;

    void tick() {
        if (mc.player != null && mc.player.isOnGround()) {
            ticksOnBlock++;
        } else {
            ticksOnBlock = 0;
        }
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
    }

    public void resetPendingState() {
        if (pendingAttack || AutoSprint.isBlocked()) {
            pendingAttack = false;
            AutoSprint.unblockSprint();
        }
    }

    private void cancelPendingAttack() {
        pendingAttack = false;
        AutoSprint.unblockSprint();
    }

    private boolean isHoldingMace() {
        return clickScheduler.isHoldingMace();
    }

    private boolean isEmptyHand() {
        return mc.player != null && mc.player.getMainHandStack().isEmpty();
    }

    private boolean isPlayerEating() {
        if (mc.player == null) return false;
        if (!mc.player.isUsingItem()) return false;
        var activeItem = mc.player.getActiveItem();
        if (activeItem.isEmpty()) return false;
        var useAction = activeItem.getUseAction();
        return useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }

    private boolean shouldWaitForEating() {
        Aura aura = Aura.getInstance();
        return aura.options.isSelected("Не бить если ешь") && isPlayerEating();
    }

    private boolean isInWater() {
        return mc.player != null && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isSwimming());
    }

    private boolean isFullyUnderwater() {
        return mc.player != null && (mc.player.isSubmergedInWater() || mc.player.isSwimming());
    }

    private boolean isOnWaterSurface() {
        if (mc.player == null) return false;
        return mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.isSwimming();
    }

    private boolean canCritOnWaterSurface() {
        if (mc.player == null) return false;
        if (!isOnWaterSurface()) return false;

        double velocityY = mc.player.getVelocity().y;
        double fallDistance = mc.player.fallDistance;
        double playerY = mc.player.getY();
        double lastY = mc.player.lastRenderY;

        boolean isFalling = velocityY < -0.08D;
        boolean hasFallDistance = fallDistance > 0.0D;
        boolean isDescending = playerY < lastY;

        return isFalling || hasFallDistance || isDescending;
    }

    private boolean isAboveWaterAndFalling() {
        if (mc.player == null) return false;
        if (!mc.player.isTouchingWater()) return false;

        double velocityY = mc.player.getVelocity().y;
        double fallDistance = mc.player.fallDistance;

        boolean notFullySubmerged = !mc.player.isSubmergedInWater();
        boolean isFallingDown = velocityY < -0.05D || fallDistance > 0.0D;

        return notFullySubmerged && isFallingDown;
    }

    private boolean hasLowCeiling() {
        if (mc.player == null || mc.world == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos above1 = playerPos.up(2);
        BlockPos above2 = playerPos.up(3);

        BlockState state1 = mc.world.getBlockState(above1);
        BlockState state2 = mc.world.getBlockState(above2);

        boolean blocked1 = !state1.isAir() && !state1.getCollisionShape(mc.world, above1).isEmpty();
        boolean blocked2 = !state2.isAir() && !state2.getCollisionShape(mc.world, above2).isEmpty();

        return blocked1 || blocked2;
    }

    private long getMinAttackDelay() {
        if (isHoldingMace()) {
            return 50L;
        }
        return 550L;
    }

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.getTarget() == null || !config.getTarget().isAlive()) {
            resetPendingState();
            return;
        }

        if (shouldWaitForEating()) {
            if (pendingAttack) cancelPendingAttack();
            return;
        }

        if (isHoldingMace()) {
            handleMaceAttack(config);
            return;
        }

        boolean noResetSprint = AutoSprint.getInstance() != null &&
                AutoSprint.getInstance().isState() &&
                AutoSprint.getInstance().getNoReset().isValue();

        boolean inWater = isInWater();
        boolean needSprintReset = !noResetSprint && !inWater && mc.player.isSprinting() && !mc.player.isGliding();

        if (pendingAttack && AutoSprint.isBlocked() && AutoSprint.sprintBlockTicks >= 1) {
            if (shouldWaitForEating()) {
                cancelPendingAttack();
                return;
            }

            boolean elytraMode = checkElytraMode(config);
            if (elytraMode && !checkElytraRaycast(config)) {
                cancelPendingAttack();
                return;
            }

            if (!RaycastAngle.rayTrace(config)) {
                cancelPendingAttack();
                return;
            }

            if (!canAttack(config, 0)) {
                if (AutoSprint.sprintBlockTicks > 8) {
                    cancelPendingAttack();
                }
                return;
            }

            executeAttack(config);
            cancelPendingAttack();
            return;
        }

        if (canAttack(config, 0)) {
            preAttackEntity(config);
        }

        boolean elytraMode = checkElytraMode(config);
        if (elytraMode) {
            if (!checkElytraRaycast(config)) {
                if (pendingAttack) cancelPendingAttack();
                return;
            }
        }

        if (!RaycastAngle.rayTrace(config)) {
            if (pendingAttack) cancelPendingAttack();
            return;
        }

        if (canAttack(config, 0)) {
            if (needSprintReset && !AutoSprint.isBlocked()) {
                AutoSprint.blockSprint();
                pendingAttack = true;
                return;
            }

            executeAttack(config);
            return;
        }

        if (canAttack(config, 2) && needSprintReset && !AutoSprint.isBlocked()) {
            preAttackEntity(config);
            AutoSprint.blockSprint();
            pendingAttack = true;
        }
    }

    private void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() &&
                mc.player.isUsingItem() &&
                mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
    }

    private void handleMaceAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (shouldWaitForEating()) return;
        if (mc.player.distanceTo(config.getTarget()) > Aura.getInstance().getAttackrange().getValue()) return;
        if (!RaycastAngle.rayTrace(config)) return;
        if (!clickScheduler.isMaceFastAttack()) return;
        if (!attackTimer.finished(25)) return;

        boolean noResetSprint = AutoSprint.getInstance() != null &&
                AutoSprint.getInstance().isState() &&
                AutoSprint.getInstance().getNoReset().isValue();

        if (!noResetSprint && !isInWater() && mc.player.isSprinting() && !mc.player.isGliding()) {
            AutoSprint.blockSprint();
            if (AutoSprint.sprintBlockTicks < 1) {
                pendingAttack = true;
                return;
            }
        }

        executeAttack(config);
        if (AutoSprint.isBlocked()) AutoSprint.unblockSprint();
        pendingAttack = false;
    }

    private boolean checkElytraMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
        return Aura.target != null &&
                Aura.target.isGliding() &&
                mc.player.isGliding() &&
                ElytraTarget.getInstance() != null &&
                ElytraTarget.getInstance().isState();
    }

    private boolean checkElytraRaycast(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Vec3d targetVelocity = config.getTarget().getVelocity();
        float leadTicks = 0;
        if (ElytraTarget.shouldElytraTarget) {
            leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
        }
        Vec3d predictedPos = config.getTarget().getEntityPos().add(targetVelocity.multiply(leadTicks));
        Box predictedBox = new Box(
                predictedPos.x - config.getTarget().getWidth() / 2,
                predictedPos.y,
                predictedPos.z - config.getTarget().getWidth() / 2,
                predictedPos.x + config.getTarget().getWidth() / 2,
                predictedPos.y + config.getTarget().getHeight(),
                predictedPos.z + config.getTarget().getWidth() / 2
        );
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        return predictedBox.raycast(eyePos, eyePos.add(lookVec.multiply(config.getMaximumRange()))).isPresent();
    }

    private void executeAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        mc.interactionManager.attackEntity(mc.player, config.getTarget());
        mc.player.swingHand(Hand.MAIN_HAND);
        attackTimer.reset();
        count++;
    }

    void handleTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (shouldWaitForEating()) return;
        if (!RaycastAngle.rayTrace(config)) return;
        if (!canAttackTrigger(config, triggerBot, 0)) return;

        boolean noResetSprint = AutoSprint.getInstance() != null &&
                AutoSprint.getInstance().isState() &&
                AutoSprint.getInstance().getNoReset().isValue();

        if (!noResetSprint && !isInWater() && mc.player.isSprinting() && !mc.player.isGliding()) {
            AutoSprint.blockSprint();
        }

        if (AutoSprint.isBlocked() && AutoSprint.sprintBlockTicks < 1) return;

        executeAttack(config);
        if (AutoSprint.isBlocked()) AutoSprint.unblockSprint();
    }

    public boolean shouldResetSprinting(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (Aura.target == null) return false;
        if (shouldWaitForEating()) return false;
        if (isHoldingMace()) return true;
        return !mc.player.isOnGround() && canAttack(config, 1);
    }

    public boolean shouldResetSprintingForTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (triggerBot.target == null) return false;
        if (shouldWaitForEating()) return false;
        return !mc.player.isOnGround() && canAttackTrigger(config, triggerBot, 1);
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (shouldWaitForEating()) return false;
        if (isHoldingMace()) {
            return attackTimer.finished(25) && clickScheduler.isMaceFastAttack();
        }

        if (!attackTimer.finished(getMinAttackDelay())) {
            return false;
        }

        for (int i = 0; i <= ticks; i++) {
            if (canCrit(config, i)) return true;
        }
        return false;
    }

    public boolean canAttackTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot, int ticks) {
        if (shouldWaitForEating()) return false;

        if (!attackTimer.finished(getMinAttackDelay())) {
            return false;
        }

        for (int i = 0; i <= ticks; i++) {
            if (canCritTrigger(config, triggerBot, i)) return true;
        }
        return false;
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (isHoldingMace()) return true;

        if (mc.player.isUsingItem()
                && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(false, 1)) return false;

        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();
        boolean smartCritsEnabled = aura.getSmartCrits().isValue();

        if (isFullyUnderwater()) {
            return true;
        }

        if (isOnWaterSurface()) {
            if (!checkCritEnabled) {
                return true;
            }

            if (canCritOnWaterSurface() || isAboveWaterAndFalling()) {
                return true;
            }

            return false;
        }

        if (mc.player.isTouchingWater()) {
            return true;
        }

        if (hasLowCeiling()) {
            return true;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
        boolean noRestrictions = !hasMovementRestrictions(simulated);
        boolean critState = isPlayerInCriticalState(simulated, ticks);

        if (smartCritsEnabled && checkCritEnabled) {
            if (noRestrictions) {
                return critState || simulated.onGround;
            } else {
                return true;
            }
        }

        if (checkCritEnabled && noRestrictions) {
            return critState;
        }

        return true;
    }

    public boolean canCritTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot, int ticks) {
        if (mc.player.isUsingItem()
                && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(false, 1)) return false;

        boolean checkCritEnabled = triggerBot.isOnlyCrits();
        boolean smartCritsEnabled = triggerBot.getSmartCrits().isValue();

        if (isFullyUnderwater()) {
            return true;
        }

        if (isOnWaterSurface()) {
            if (!checkCritEnabled) {
                return true;
            }

            if (canCritOnWaterSurface() || isAboveWaterAndFalling()) {
                return true;
            }

            return false;
        }

        if (mc.player.isTouchingWater()) {
            return true;
        }

        if (hasLowCeiling()) {
            return true;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
        boolean noRestrictions = !hasMovementRestrictions(simulated);
        boolean critState = isPlayerInCriticalState(simulated, ticks);

        if (smartCritsEnabled && checkCritEnabled) {
            if (noRestrictions) {
                return critState || simulated.onGround;
            } else {
                return true;
            }
        }

        if (checkCritEnabled && noRestrictions) {
            return critState;
        }

        return true;
    }

    private boolean hasMovementRestrictions(PlayerSimulation simulated) {
        if (isInWater()) {
            return false;
        }

        if (hasLowCeiling()) {
            return true;
        }

        return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulated.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-1e-3), Blocks.COBWEB)
                || simulated.isInLava()
                || simulated.isClimbing()
                || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
                || simulated.player.getAbilities().flying;
    }

    private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
        double fall = simulated.fallDistance;
        return !simulated.onGround && fall > 0;
    }
}