package rich.util.animations;

public class AlphaAnim {

    private float value;
    private float target;
    private float speed;
    private long lastUpdate;
    private boolean active;

    public AlphaAnim() {
        this(0.0f, 0.08f);
    }

    public AlphaAnim(float initialValue) {
        this(initialValue, 0.08f);
    }

    public AlphaAnim(float initialValue, float speed) {
        this.value = initialValue;
        this.target = initialValue;
        this.speed = speed;
        this.lastUpdate = System.currentTimeMillis();
        this.active = false;
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdate) / 16.666f;
        lastUpdate = currentTime;

        if (Math.abs(target - value) < 0.001f) {
            value = target;
            active = false;
            return;
        }

        active = true;
        float diff = target - value;
        float smoothSpeed = speed * deltaTime;
        smoothSpeed = Math.min(smoothSpeed, 1.0f);

        value += diff * smoothSpeed;
        value = clamp(value, 0.0f, 1.0f);
    }

    public void forceValue(float newValue) {
        this.value = clamp(newValue, 0.0f, 1.0f);
        this.target = this.value;
        this.active = false;
    }

    public void setTarget(float newTarget) {
        float oldTarget = this.target;
        this.target = clamp(newTarget, 0.0f, 1.0f);
        if (Math.abs(target - value) > 0.001f) {
            if (!active || oldTarget != this.target) {
                lastUpdate = System.currentTimeMillis();
            }
            active = true;
        }
    }

    public void animateIn() {
        setTarget(1.0f);
    }

    public void animateOut() {
        setTarget(0.0f);
    }

    public float getValue() {
        return value;
    }

    public float getTarget() {
        return target;
    }

    public float getEased() {
        return easeOutQuad(value);
    }

    public float getEasedCubic() {
        return easeOutCubic(value);
    }

    public float getEasedQuart() {
        return easeOutQuart(value);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFullyIn() {
        return value >= 0.999f;
    }

    public boolean isFullyOut() {
        return value <= 0.001f;
    }

    public boolean isAnimatingIn() {
        return target > value;
    }

    public boolean isAnimatingOut() {
        return target < value;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    private float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }

    private float easeOutQuart(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 4);
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}