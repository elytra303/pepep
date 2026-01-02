package rich.modules.impl.combat.aura.rotations;


import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.Initialization;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.util.timer.StopWatch;

import java.security.SecureRandom;

public class TestAngle extends RotateConstructor {
    public TestAngle() {
        super("Matrix");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean lookingAtHitbox = false;

        float preAttackSpeed = 0.85F;
        float postAttackSpeed = randomLerp(0.3F, 0.5F);
        float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.3f, 1.0f);
        float speedFactor2 = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.45f, 1.0f);
        float speed = canAttack ? preAttackSpeed * speedFactor2 : postAttackSpeed * speedFactor;
        float jitterYaw = canAttack ? 0 : (float) (3 * Math.sin(System.currentTimeMillis() / 65D));
        float jitterPitch = canAttack ? 0 : (float) (3 * Math.cos(System.currentTimeMillis() / 65D));
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 360);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

        if (!aura.isState() || entity == null) {
            speed = 1 * speedFactor;
            jitterYaw = 0;
            jitterPitch = 0;
        }

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) ;
        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) +  jitterPitch);

        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0.1, 0);
    }
}