package rich.util.inventory;

import lombok.experimental.UtilityClass;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class Simulations implements IMinecraft {


    public static final boolean moveKeyPressed(int keyNumber) {
        boolean w = mc.options.forwardKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();
        return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
    }


    public static final boolean w() {
        return moveKeyPressed(0);
    }

    public static final boolean a() {
        return moveKeyPressed(1);
    }

    public static final boolean s() {
        return moveKeyPressed(2);
    }

    public static final boolean d() {
        return moveKeyPressed(3);
    }

    public static final float moveYaw(float entityYaw) {
        return entityYaw + (float)(!a() || !d() || w() && s() || !w() && !s() ? (w() && s() && (!a() || !d()) && (a() || d()) ? (a() ? -90 : (d() ? 90 : 0)) : (a() && d() && (!w() || !s()) || w() && s() && (!a() || !d()) ? 0 : (!a() && !d() && !s() ? 0 : (w() && !s() ? 45 : (s() && !w() ? (!a() && !d() ? 180 : 135) : ((w() || s()) && (!w() || !s()) ? 0 : 90))) * (a() ? -1 : 1)))) : (w() ? 0 : (s() ? 180 : 0)));
    }

    public static float calculateBodyYaw(
            float yaw,
            float prevBodyYaw,
            double prevX,
            double prevZ,
            double currentX,
            double currentZ,
            float handSwingProgress
    ) {

        double motionX = currentX - prevX;
        double motionZ = currentZ - prevZ;
        float motionSquared = (float)(motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;
        float swing = mc.player.handSwingProgress;

        if (motionSquared > 0.0025000002F) {
            float movementYaw = (float)MathHelper.atan2(motionZ, motionX) * (180F / (float)Math.PI) - 90.0F;
            float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(yaw) - movementYaw);
            if (95.0F < yawDiff && yawDiff < 265.0F) {
                bodyYaw = movementYaw - 180.0F;
            } else {
                bodyYaw = movementYaw;
            }
        }


        float deltaYaw = MathHelper.wrapDegrees(bodyYaw - prevBodyYaw);
        bodyYaw = prevBodyYaw + deltaYaw * 0.3F;

        float yawOffsetDiff = MathHelper.wrapDegrees(yaw - bodyYaw);
        float maxHeadRotation = 52.0F;
        if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
            bodyYaw += yawOffsetDiff - (float)MathHelper.sign((double)yawOffsetDiff) * maxHeadRotation;
        }

        return bodyYaw;
    }

    public static double kizdamati() {
        return 1488;
    }

    public double getDegreesRelativeToView(
            Vec3d positionRelativeToPlayer,
            float yaw) {

        float optimalYaw =
                (float) Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(wrapDegrees(yaw));

        return Math.toDegrees(wrapDegrees((optimalYaw - currentYaw)));
    }

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
        boolean forwards = input.forward();
        boolean backwards = input.backward();
        boolean left = input.left();
        boolean right = input.right();

        if (dgs >= (-90.0F + deadAngle) && dgs <= (90.0F - deadAngle)) {
            forwards = true;
        } else if (dgs < (-90.0F - deadAngle) || dgs > (90.0F + deadAngle)) {
            backwards = true;
        }

        if (dgs >= (0.0F + deadAngle) && dgs <= (180.0F - deadAngle)) {
            right = true;
        } else if (dgs >= (-180.0F + deadAngle) && dgs <= (0.0F - deadAngle)) {
            left = true;
        }

        return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
        return getDirectionalInputForDegrees(input, dgs, 20.0F);
    }
}
