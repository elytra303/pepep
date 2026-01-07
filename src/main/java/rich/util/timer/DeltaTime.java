package rich.util.timer;

import lombok.Getter;
import rich.IMinecraft;

public class DeltaTime implements IMinecraft {
    
    public static final DeltaTime INSTANCE = new DeltaTime();
    
    private static final float TARGET_FPS = 60.0f;
    private static final float TARGET_FRAME_TIME = 1000.0f / TARGET_FPS;
    
    private long lastFrameTime = System.currentTimeMillis();
    private float deltaTime = 16.67f;
    @Getter
    private float factor = 1.0f;
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        deltaTime = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
        
        deltaTime = Math.max(1.0f, Math.min(deltaTime, 100.0f));
        
        factor = deltaTime / TARGET_FRAME_TIME;
    }
    
    public float getDeltaTime() {
        return deltaTime;
    }
    
    public float getDeltaSeconds() {
        return deltaTime / 1000.0f;
    }
    
    public float getSmooth(float speed) {
        return speed * factor;
    }
    
    public double getSmooth(double speed) {
        return speed * factor;
    }
    
    public float lerp(float current, float target, float speed) {
        float smoothSpeed = Math.min(1.0f, speed * factor);
        return current + (target - current) * smoothSpeed;
    }
    
    public double lerp(double current, double target, double speed) {
        double smoothSpeed = Math.min(1.0, speed * factor);
        return current + (target - current) * smoothSpeed;
    }
    
    public static float smooth(float speed) {
        return INSTANCE.getSmooth(speed);
    }
    
    public static double smooth(double speed) {
        return INSTANCE.getSmooth(speed);
    }
}