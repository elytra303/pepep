package rich.modules.impl.combat.aura.rotations;

import rich.Initialization;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.combat.aura.target.Vector;
import rich.util.move.MoveUtil;
import rich.util.timer.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HWAngle extends RotateConstructor {

    private static float headDropProgress = 0f;
    private static long lastAttackTime = 0;

    public HWAngle() {
        super("HolyWorld");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        if (entity != null && canAttack) {
            Vec3d aimPoint = Vector.hitbox(entity, 1, entity.isOnGround() ? 0.85F : 1.3F, 1, 2);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        boolean lookingAtHitbox = false;
        if (entity != null && !canAttack) {
            lookingAtHitbox = RaycastAngle.rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), 4.0, entity.getBoundingBox());
        }

        float preAttackSpeed = 1F;
        float postAttackSpeed = lookingAtHitbox ? 0.04F : randomLerp(0.2F, 0.5F);
        float speed = canAttack ? preAttackSpeed : postAttackSpeed;

        float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 360) : 360;
        float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 180;

        float jitterYaw = 0;
        float jitterPitch = 0;

        if (!canAttack && entity != null) {
            double time = System.currentTimeMillis();

            if (MoveUtil.hasPlayerMovement()) {
                jitterYaw = (float) (12 * Math.sin(time / 40D)) + (float) (4 * Math.sin(time / 17D));
                jitterPitch = (float) (2.5 * Math.cos(time / 65D));
            } else {
                jitterYaw = (float) (7 * Math.sin(time / 55D)) + (float) (2 * Math.cos(time / 25D));
                jitterPitch = (float) (1.5 * Math.cos(time / 80D));
            }
        }

        long currentTime = System.currentTimeMillis();

        if (canAttack) {
            lastAttackTime = currentTime;
            headDropProgress = MathHelper.lerp(0.2f, headDropProgress, 1.0f);
        } else {
            long timeSinceAttack = currentTime - lastAttackTime;

            if (timeSinceAttack < 150) {
                headDropProgress = MathHelper.lerp(0.08f, headDropProgress, 0.8f);
            } else if (timeSinceAttack < 350) {
                headDropProgress = MathHelper.lerp(0.1f, headDropProgress, 0f);
            } else {
                headDropProgress = MathHelper.lerp(0.15f, headDropProgress, 0f);
            }
        }

        float headDropOffset = headDropProgress * 20f;

        float resolve1 = canAttack ? 0 : 11;
        float resolve2 = canAttack ? 0 : 5;

        if (!aura.isState() || entity == null) {
            float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.1f, 1.0f);
            speed = !attackTimer.finished(550) ? 0.05F : 0.8F * speedFactor;
            jitterYaw = 0;
            jitterPitch = 0;
            resolve1 = 0;
            resolve2 = 0;
            headDropOffset = 0;
            headDropProgress = MathHelper.lerp(0.2f, headDropProgress, 0f);
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw) + resolve1;
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) + resolve2;

        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getPitch(), currentAngle.getPitch() + movePitch + headDropOffset - 15) + jitterPitch);

        moveAngle.setPitch(MathHelper.clamp(moveAngle.getPitch(), -90f, 90f));

        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}