package rich.modules.impl.combat.aura.rotations;

import net.minecraft.client.Minecraft;
import rich.Initialization;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.combat.aura.target.Vector;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

/**
 * Рефакторинг SPAngle: Эмуляция физики мыши и обход эвристик MX Anticheat.
 */
public class SPAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();
    private final Minecraft mc = Minecraft.getMinecraft();

    private float currentJitterYaw, currentJitterPitch;
    private float lastStepYaw, lastStepPitch;

    // Поля для динамических паттернов
    private int currentPattern = 0;
    private long lastPatternSwitch = 0;
    private float noiseState = 0;

    private float circlePhase = 0;
    private float circleRadius = 0;
    private float currentSpeed = 0;

    public SPAngle() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        // 1. Динамическая точка прицеливания с учетом скорости (Prediction)
        if (entity != null && canAttack) {
            // Базовая точка + небольшое смещение на основе движения цели
            double predictX = entity.posX - entity.prevPosX;
            double predictZ = entity.posZ - entity.prevPosZ;
            Vec3d aimPoint = Vector.hitbox(entity, 1, entity.isOnGround() ? 1F : 1.2F, 1, 2)
                    .addVector(predictX * 1.5, 0, predictZ * 1.5);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = MathHelper.wrapDegrees(angleDelta.getYaw());
        float pitchDelta = angleDelta.getPitch();
        float dist = (float) Math.hypot(yawDelta, pitchDelta);

        // 2. Рандомизация паттернов (Уход от предсказуемых циклов)
        if (System.currentTimeMillis() - lastPatternSwitch > 1200 + random.nextInt(1500)) {
            currentPattern = random.nextInt(4);
            lastPatternSwitch = System.currentTimeMillis();
        }

        // 3. Многослойный шум (Вместо простых sin/cos)
        noiseState += 0.05f + random.nextFloat() * 0.1f;
        float layeredNoiseYaw = (float) (Math.sin(noiseState) * 1.2 + Math.sin(noiseState * 0.5) * 2.0 + Math.cos(noiseState * 1.5) * 0.5);
        float layeredNoisePitch = (float) (Math.cos(noiseState * 0.7) * 0.8 + Math.sin(noiseState * 2.1) * 1.1);

        // 4. Динамическое сглаживание и Эмуляция инерции (Ease In/Out)
        // Чем дальше цель, тем выше начальная скорость, при приближении — торможение.
        float targetSpeedFactor = MathHelper.clamp(dist / 40f, 0.2f, 1.0f);
        float acceleration = (dist > 5) ? 0.15f : 0.08f;
        currentSpeed = moveTowards(currentSpeed, targetSpeedFactor, acceleration, 0.1f);

        // Применяем кривую Безье (простейшая аппроксимация через cubic easing)
        float t = currentSpeed;
        float smoothT = t * t * (3 - 2 * t);

        // 5. Расчет финальных углов с джиттером
        float moveYaw = yawDelta * smoothT;
        float movePitch = pitchDelta * smoothT;

        float jitterMult = canAttack ? 0.4f : 1.2f;
        float finalYaw = currentAngle.getYaw() + moveYaw + (layeredNoiseYaw * jitterMult);
        float finalPitch = currentAngle.getPitch() + movePitch + (layeredNoisePitch * jitterMult);

        // 6. GCD Fix: Нормализация под чувствительность мыши
        return applyGCD(finalYaw, finalPitch, currentAngle);
    }

    /**
     * Исправление GCD (Greatest Common Divisor).
     * Эмулирует дискретные шаги сенсора мыши, что является критическим для обхода MX Anticheat.
     */
    private Angle applyGCD(float yaw, float pitch, Angle prevAngle) {
        float sensitivity = mc.gameSettings.mouseSensitivity;

        // Формула Minecraft для шага камеры
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;

        float deltaYaw = yaw - prevAngle.getYaw();
        float deltaPitch = pitch - prevAngle.getPitch();

        // Округление дельты до ближайшего кратного шагу GCD
        float fixedYaw = prevAngle.getYaw() + (Math.round(deltaYaw / gcd) * gcd);
        float fixedPitch = prevAngle.getPitch() + (Math.round(deltaPitch / gcd) * gcd);

        return new Angle(fixedYaw, MathHelper.clamp(fixedPitch, -90, 90));
    }

    /**
     * Плавное изменение значения для эмуляции веса/инерции
     */
    private float moveTowards(float current, float target, float accel, float decel) {
        if (current < target) {
            current = Math.min(current + accel, target);
        } else {
            current = Math.max(current - decel, target);
        }
        return current;
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(
                (random.nextFloat() - 0.5) * 0.02,
                (random.nextFloat() - 0.5) * 0.02,
                (random.nextFloat() - 0.5) * 0.02
        );
    }
}