package rich.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import rich.IMinecraft;
import rich.events.impl.PacketEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.TriggerBot;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.movement.AutoSprint;
import rich.modules.impl.movement.ElytraTarget;
import rich.util.player.PlayerSimulation;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

import java.util.concurrent.ThreadLocalRandom;

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

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.getTarget() == null || !config.getTarget().isAlive()) {
            resetPendingState();
            return;
        }

        boolean noResetSprint = AutoSprint.getInstance() != null &&
                AutoSprint.getInstance().isState() &&
                AutoSprint.getInstance().getNoReset().isValue();

        boolean inWater = mc.player.isSubmergedInWater();
        boolean needSprintReset = !noResetSprint && !inWater && mc.player.isSprinting();

        if (pendingAttack && AutoSprint.isBlocked() && AutoSprint.sprintBlockTicks >= 1) {
            if (!shouldAttack(config)) {
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

        if (!shouldAttack(config)) {
            if (pendingAttack) cancelPendingAttack();
            return;
        }

        boolean elytraMode = checkElytraMode(config);
        if (elytraMode && !checkElytraRaycast(config)) {
            if (pendingAttack) cancelPendingAttack();
            return;
        }

        if (!RaycastAngle.rayTrace(config)) {
            if (pendingAttack) cancelPendingAttack();
            return;
        }

        if (canAttack(config, 0)) {
            if (needSprintReset && !AutoSprint.isBlocked()) {
                handleShieldUnpress(config);
                AutoSprint.blockSprint();
                pendingAttack = true;
                return;
            }

            executeAttack(config);
            return;
        }

        if (canAttack(config, 2) && needSprintReset && !AutoSprint.isBlocked()) {
            handleShieldUnpress(config);
            AutoSprint.blockSprint();
            pendingAttack = true;
        }
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

        if (!mc.player.isOnGround() && convenientFallOffset() > 0.0F) {
            mc.world.playSound(
                    null,
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                    mc.player.getSoundCategory(),
                    1.0f,
                    1.0f
            );
            mc.player.addCritParticles(config.getTarget());
        }

        attackTimer.reset();
        count++;
    }

    private void handleShieldUnpress(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() &&
                mc.player.isUsingItem() &&
                mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
    }

    void handleTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (!shouldAttackTrigger(config, triggerBot)) return;
        if (!RaycastAngle.rayTrace(config)) return;
        if (!canAttackTrigger(config, triggerBot, 0)) return;

        boolean noResetSprint = AutoSprint.getInstance() != null &&
                AutoSprint.getInstance().isState() &&
                AutoSprint.getInstance().getNoReset().isValue();

        if (!noResetSprint && !mc.player.isSubmergedInWater() && mc.player.isSprinting()) {
            AutoSprint.blockSprint();
        }

        if (AutoSprint.isBlocked() && AutoSprint.sprintBlockTicks < 1) {
            return;
        }

        executeAttack(config);

        if (AutoSprint.isBlocked()) {
            AutoSprint.unblockSprint();
        }
    }

    public boolean shouldResetSprinting(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (Aura.target == null) return false;
        return !mc.player.isOnGround() && (canAttack(config, 1) || shouldAttack(config));
    }

    public boolean shouldResetSprintingForTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (triggerBot.target == null) return false;
        return !mc.player.isOnGround() && (canAttackTrigger(config, triggerBot, 1) || shouldAttackTrigger(config, triggerBot));
    }

    public boolean shouldAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Aura aura = Aura.getInstance();

        if (mc.player.distanceTo(config.getTarget()) > aura.attackrange.getValue()) {
            return false;
        }

        boolean crit = true;

        if (aura.getCheckCrit().isValue()) {
            float minFallDist = 0;
            if (aura.isRandomizeCrit()) {
                minFallDist = ThreadLocalRandom.current().nextDouble() > 0.75
                        ? randomFloat(0.1f, 0.4f)
                        : randomFloat(0.0f, 0.1f);
            }

            boolean fallDistance = convenientFallOffset() > minFallDist;

            boolean canCrit = (!mc.player.isOnGround()
                    || mc.world.getBlockState(mc.player.getBlockPos().add(0, -1, 0)).isAir()
                    || mc.player.input.playerInput.jump())
                    && fallDistance;

            canCrit = canCrit
                    && !mc.player.isOnGround()
                    && !mc.player.isClimbing()
                    && !mc.player.isTouchingWater();

            boolean isOnGround = mc.player.isOnGround() || !mc.player.input.playerInput.jump();
            boolean isLiquid = mc.player.isSwimming() || mc.player.isTouchingWater() || mc.player.isInFluid();

            if (aura.getSmartCrits().isValue()) {
                crit = isOnGround || canCrit || isLiquid;
            } else {
                crit = canCrit || isLiquid;
            }

            crit |= PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), Blocks.COBWEB);
            crit |= mc.player.isClimbing();
        }

        return crit;
    }

    public boolean shouldAttackTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (mc.player.distanceTo(config.getTarget()) > triggerBot.attackRange.getValue()) {
            return false;
        }

        boolean crit = true;

        if (triggerBot.isOnlyCrits()) {
            float minFallDist = 0;
            if (triggerBot.isRandomizeCrit()) {
                minFallDist = ThreadLocalRandom.current().nextDouble() > 0.75
                        ? randomFloat(0.1f, 0.4f)
                        : randomFloat(0.0f, 0.1f);
            }

            boolean fallDistance = convenientFallOffset() > minFallDist;

            boolean canCrit = (!mc.player.isOnGround()
                    || mc.world.getBlockState(mc.player.getBlockPos().add(0, -1, 0)).isAir()
                    || mc.player.input.playerInput.jump())
                    && fallDistance;

            canCrit = canCrit
                    && !mc.player.isOnGround()
                    && !mc.player.isClimbing()
                    && !mc.player.isTouchingWater();

            boolean isOnGround = mc.player.isOnGround() || !mc.player.input.playerInput.jump();
            boolean isLiquid = mc.player.isSwimming() || mc.player.isTouchingWater() || mc.player.isInFluid();

            if (triggerBot.getSmartCrits().isValue()) {
                crit = isOnGround || canCrit || isLiquid;
            } else {
                crit = canCrit || isLiquid;
            }

            crit |= PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), Blocks.COBWEB);
            crit |= mc.player.isClimbing();
        }

        return crit;
    }

    public double convenientFallOffset() {
        double fallOffset = mc.player.fallDistance;

        if (mc.world != null
                && !mc.player.isOnGround()
                && mc.player.getVelocity().y < -0.0784000015258789D
                && mc.world.getBlockState(mc.player.getBlockPos()).getFluidState().isEmpty()
                && !mc.world.getBlockState(mc.player.getBlockPos().up()).getFluidState().isEmpty()) {

            if (mc.player.fallDistance < -mc.player.getVelocity().y && ticksOnBlock > 6) {
                fallOffset = -mc.player.getVelocity().y;
            }
        }
        return fallOffset;
    }

    public float getFallDistance(int nextTicks) {
        Vector3f deltaMove = new Vector3f(0, (float) mc.player.getVelocity().y, 0);

        if (deltaMove.y == 0 || mc.player.isOnGround()) return 0;

        float fallDistance = 0;
        double gravity = 0.08D;
        boolean flag = deltaMove.y <= 0.0D;

        if (flag && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = 0.01D;
        }

        for (int i = 0; i < nextTicks + 1; i++) {
            double d2 = deltaMove.y;
            d2 -= gravity;
            deltaMove.y = (float) (d2 * 0.98F);

            if (deltaMove.y > 0) {
                fallDistance = 0;
            } else {
                fallDistance -= deltaMove.y;
            }
        }

        return fallDistance;
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        for (int i = 0; i <= ticks; i++) {
            if (canCrit(config, i) && attackTimer.finished(50)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAttackTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot, int ticks) {
        for (int i = 0; i <= ticks; i++) {
            if (canCritTrigger(config, triggerBot, i) && attackTimer.finished(50)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (mc.player.isUsingItem()
                && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(false, 1)) {
            return false;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);

        if (!hasMovementRestrictions(simulated)) {
            return isPlayerInCriticalState(simulated, ticks);
        }
        return true;
    }

    public boolean canCritTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot, int ticks) {
        if (mc.player.isUsingItem()
                && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(false, 1)) {
            return false;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);

        if (!hasMovementRestrictions(simulated)) {
            return isPlayerInCriticalStateTrigger(simulated, triggerBot, ticks);
        }
        return true;
    }

    private boolean hasMovementRestrictions(PlayerSimulation simulated) {
        return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulated.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-1e-3), Blocks.COBWEB)
                || simulated.isSubmergedInWater()
                || simulated.isInLava()
                || simulated.isClimbing()
                || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
                || simulated.player.getAbilities().flying;
    }

    private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
        Aura aura = Aura.getInstance();

        if (aura.getSmartCrits().isValue() && simulated.onGround) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5F + ticks);
            return cooldown > (mc.player.getMainHandStack().isEmpty() ? 0.99f : 0.94F);
        }

        boolean fall = simulated.fallDistance > 0 || getFallDistance(1 + ticks) > 0;
        return !simulated.onGround && fall;
    }

    private boolean isPlayerInCriticalStateTrigger(PlayerSimulation simulated, TriggerBot triggerBot, int ticks) {
        if (triggerBot.getSmartCrits().isValue() && simulated.onGround) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5F + ticks);
            return cooldown > (mc.player.getMainHandStack().isEmpty() ? 0.99f : 0.94F);
        }

        boolean fall = simulated.fallDistance > 0 || getFallDistance(1 + ticks) > 0;
        return !simulated.onGround && fall;
    }

    private float randomFloat(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
}