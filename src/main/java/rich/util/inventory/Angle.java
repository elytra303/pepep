package rich.util.inventory;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;

@Getter
@Setter
@Data
public class Angle implements IMinecraft {
    public float yaw, pitch;

    public Angle(float yaw, float pitch) {
        this.yaw = Float.isNaN(yaw) ? 0 : yaw;
        this.pitch = Float.isNaN(pitch) ? 0 : pitch;
    }

    public Angle(float[] angles) {
        this(angles[0], angles[1]);
    }

    public Angle(Vec2f vec2f) {
        this.yaw = vec2f.x;
        this.pitch = vec2f.y;
    }


    public Angle() {
        yaw = 0;
        pitch = 0;
    }

    public final Vec3d toVector() {
        return Vec3d.fromPolar(pitch, yaw);
    }

    public void set(Angle other) {
        yaw = other.yaw;
        pitch = other.pitch;
    }

    public Angle copy() {
        return new Angle(yaw, pitch);
    }

    public Angle clamp(float yawSpeed, float pitchSpeed) {
        return new Angle(MathHelper.clamp(yaw, -yawSpeed, yawSpeed), MathHelper.clamp(pitch, -pitchSpeed, pitchSpeed));
    }

    public Angle towards(Angle other, float yawFactor, float pitchFactor) {
        Angle delta = delta(other);
        float length = delta.length();
        float slYaw = Math.abs(delta.yaw / length) * yawFactor;
        float slPitch = Math.abs(delta.pitch / length) * pitchFactor;

        return new Angle(yaw + MathHelper.clamp(delta.yaw, -slYaw, slYaw), pitch + MathHelper.clamp(delta.pitch, -slPitch, slPitch));
    }

    public Angle delta(Angle other) {
        float yaw = MathHelper.wrapDegrees(other.getYaw() - this.yaw);
        float pitch = other.getPitch() - this.pitch;
        return new Angle(yaw, MathHelper.clamp(pitch, -90, 90));
    }

    public float length() {
        return MathHelper.sqrt(yaw * yaw + pitch * pitch);
    }

}
