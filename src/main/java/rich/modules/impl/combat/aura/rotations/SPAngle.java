package rich.modules.impl.combat.aura.rotations;

import rich.Initialization;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.util.timer.StopWatch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class SPAngle extends RotateConstructor {
    private boolean redirectDirectionChosen = false;
    private boolean redirectToRight = true;
    private float currentSpeed = 0.67f;
    public SPAngle() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        Aura aura = Aura.getInstance();
        int count = attackHandler.getCount();

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float baseSpeed = canAttack ? 1F : 0.8F;
        float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.1f, 1.0f);
        float speedFactor2 = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.4f, 1.0f);
        float speed = baseSpeed * (canAttack ? speedFactor2 : speedFactor);
        float jitterYaw = canAttack ? 0 : (float) (8 * Math.sin(System.currentTimeMillis() / 62D));
        float jitterPitch = canAttack ? 0 : (float) (5 * Math.sin(System.currentTimeMillis() / 54D));

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 100);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        if (!aura.isState() || entity == null) {
            float speedFactor3 = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.1f, 1.0f);
            speed = !attackTimer.finished(450) ? 0.15F : 0.98F * speedFactor3;
            jitterYaw = 0;
            jitterPitch = 0;
        }
        if (entity instanceof LivingEntity livingEntity) {
            double targetHeight = livingEntity.getHeight();
            double torsoHeight = targetHeight * 0.36;
            Vec3d playerPos = MinecraftClient.getInstance().player.getEntityPos().add(0, 1.5, 0);
            Vec3d entityPos = livingEntity.getEntityPos();
            double deltaY = (entityPos.y + torsoHeight) - playerPos.y;
            double deltaX = entityPos.x - playerPos.x;
            double deltaZ = entityPos.z - playerPos.z;
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float torsoPitch = (float) Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));
            torsoPitch = MathHelper.clamp(torsoPitch, -90.0f, 90.0f);

            if (currentAngle.getPitch() > torsoPitch) {
                float pitchAdjustment = Math.min(8.0f, currentAngle.getPitch() - torsoPitch);
                movePitch -= pitchAdjustment;
            }
        }
        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);

        float pitchSpeed = pitchDelta < 0 ? 0.45F : 0.8F;
        moveAngle.setPitch(MathHelper.lerp(pitchSpeed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);
        return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
    }


    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}