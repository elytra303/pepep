package rich.modules.impl.combat;

import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.InputEvent;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.attack.StrikerConstructor;
import rich.modules.impl.combat.aura.impl.*;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.modules.impl.combat.aura.rotations.*;
import rich.modules.impl.combat.aura.target.MultiPoint;
import rich.modules.impl.combat.aura.target.TargetFinder;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;
import rich.util.math.TaskPriority;

import java.util.Objects;

public class Aura extends ModuleStructure {

    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    private final SelectSetting mode = new SelectSetting("Режим наводки", "Select aim mode")
            .value("Matrix", "FunTime Snap", "SpookyTime", "HolyWorld", "LonyGrief", "Test")
            .selected("Matrix");

    private final SelectSetting moveFix = new SelectSetting("Коррекция движения", "Select move fix mode")
            .value("Сфокусированная", "Свободная", "Преследование", "Таргет", "Отключена")
            .selected("Focus");

    public final SliderSettings attackrange = new SliderSettings("Дистанция удара", "Set range value")
            .range(2.0f, 6.0f)
            .setValue(3.0f);

    private final SliderSettings lookrange = new SliderSettings("Дистанция поиска", "Set look range value")
            .range(0.0f, 3.0f)
            .setValue(1.0f);

    public final MultiSelectSetting options = new MultiSelectSetting("Настройки", "Select settings")
            .value("Бить сквозь стены", "Интерполяция высоты")
            .selected();

    private final MultiSelectSetting targetType = new MultiSelectSetting("Настройка целей", "Select target settings")
            .value("Игроки", "Мобы", "Животные", "Друзья", "Стойки для брони")
            .selected("Игроки", "Мобы", "Животные");

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(mode, attackrange, lookrange, options, targetType, moveFix
        );
    }

    @NonFinal
    public static LivingEntity target;
    @NonFinal
    public LivingEntity lastTarget;
    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    @EventHandler
    private void tick(TickEvent event) {

//        mc.options.jumpKey.setPressed(true);
//        mc.options.forwardKey.setPressed(true);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                boolean passed = false;
                if (mode.isSelected("FunTime Snap") || mode.isSelected("HolyWorld")) {
                    passed = true;
                }
                if (target != null && passed && target.distanceTo(mc.player) <= attackrange.getValue() + 0.25F) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
                if (target != null && !passed) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                if (target != null) {
                    Initialization.getInstance().getManager().getAttackPerpetrator().performAttack(getConfig());
                }
            }
        }
    }

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackrange.getValue() + 0.25F;


        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                AngleConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                options.isSelected("Бить сквозь стены")
        );

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();
        if (mc.player.isGliding() && target.isGliding()) {
            Vec3d targetVelocity = target.getVelocity();
            double targetSpeed = targetVelocity.horizontalLength();

            float leadTicks = 0;

            if (targetSpeed > 0.35) {

                Vec3d predictedPos = target.getEntityPos().add(targetVelocity.multiply(leadTicks));
                computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

                hitbox = new Box(
                        predictedPos.x - target.getWidth() / 2,
                        predictedPos.y,
                        predictedPos.z - target.getWidth() / 2,
                        predictedPos.x + target.getWidth() / 2,
                        predictedPos.y + target.getHeight(),
                        predictedPos.z + target.getWidth() / 2
                );
            }
        }

        Angle angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));
        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                options.getSelected(),
                mode,
                hitbox
        );
    }

    public AngleConfig getRotationConfig() {
        boolean visibleCorrection = !moveFix.isSelected("Отключена");
        boolean freeCorrection = moveFix.isSelected("Свободная");
        return new AngleConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        AngleConnection controller = AngleConnection.INSTANCE;
        Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
        AngleConfig rotationConfig = getRotationConfig();

        switch (mode.getSelected()) {

            case "FunTime Snap" -> {
                if (attackHandler.canAttack(config, 5)) {
                    controller.clear();
                    controller.rotateTo(rotation, target, 60, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "HolyWorld" -> {
                if (attackHandler.canAttack(config, 6) || !attackHandler.getAttackTimer().finished(67)) {
                    controller.rotateTo(rotation, target, 60, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "Matrix", "SpookyTime", "LonyGrief", "Test" -> {
                controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

        };

    }

    @EventHandler
    public void onInput(InputEvent event) {
        PlayerInput input = mc.player.input.playerInput;
        if (Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(getConfig(), 2) && Aura.getInstance().target != null && Aura.getInstance().isState() && Aura.target.distanceTo(mc.player) <= Aura.getInstance().attackrange.getValue()) {
            event.setDirectional(false, input.backward(), input.left(), input.right(), input.sneak(), false, input.jump());
            mc.player.setSprinting(false);
        }

        if (mc.player == null || mc.world == null) return;
        if (target == null || !target.isAlive()) return;

        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (moveFix.isSelected("Таргет")) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();

            Vec3d moveTarget = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
            Vec3d dir = moveTarget.subtract(playerPos).normalize();

            float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
            float moveAngle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);

            boolean forward = false, back = false, left = false, right = false;

            if (angleDiff >= -22.5 && angleDiff < 22.5) {
                forward = true;
            } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                forward = true; right = true;
            } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                right = true;
            } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                back = true; right = true;
            } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                forward = true; left = true;
            } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                left = true;
            } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                back = true; left = true;
            } else {
                back = true;
            }

            event.setDirectionalLow(forward, back, left, right);
            return;
        }

        if (moveFix.isSelected("Преследование")) {
            if (!w && !s && !a && !d) return;

            Vec3d playerPos = mc.player.getEntityPos();
            Box targetBox = target.getBoundingBox();
            Vec3d center = targetBox.getCenter();

            float targetYaw = target.getYaw();
            double rad = Math.toRadians(targetYaw);

            Vec3d forwardDir = new Vec3d(-Math.sin(rad), 0, Math.cos(rad)).normalize();
            Vec3d rightDir = new Vec3d(-forwardDir.z, 0, forwardDir.x).normalize();
            Vec3d leftDir = rightDir.multiply(-1);

            double halfWidth = target.getWidth() / 2.0;
            double offset = halfWidth + 0.1;

            Vec3d moveTarget = center;
            Vec3d offsetVec = Vec3d.ZERO;

            if (w) offsetVec = offsetVec.add(forwardDir);
            if (s) offsetVec = offsetVec.add(forwardDir.multiply(-1.0));
            if (a) offsetVec = offsetVec.add(leftDir);
            if (d) offsetVec = offsetVec.add(rightDir);

            if (offsetVec.lengthSquared() > 0) {
                offsetVec = offsetVec.normalize().multiply(offset);
                moveTarget = center.add(offsetVec);
            }

            moveTarget = new Vec3d(moveTarget.x, playerPos.y, moveTarget.z);
            Vec3d dir = moveTarget.subtract(playerPos).normalize();

            float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
            float moveAngle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);

            boolean forward = false, back = false, left = false, right = false;

            if (angleDiff >= -22.5 && angleDiff < 22.5) {
                forward = true;
            } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                forward = true; right = true;
            } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                right = true;
            } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                back = true; right = true;
            } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                forward = true; left = true;
            } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                left = true;
            } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                back = true; left = true;
            } else {
                back = true;
            }

            event.setDirectionalLow(forward, back, left, right);
        }
    }


    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackrange.getValue() + 0.25F + lookrange.getValue();

        float dynamicFov = 360;

        targetSelector.searchTargets(mc.world.getEntities(), range, dynamicFov, options.isSelected("Бить сквозь стены"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    public RotateConstructor getSmoothMode() {
        return switch (mode.getSelected()) {
            case "FunTime Snap" -> new FTAngle();
            case "HolyWorld" -> new HWAngle();
            case "Test" -> new TestAngle();
            case "LonyGrief" -> new LonyAngle();
            case "SpookyTime" -> new SPAngle();
            case "Matrix" -> new MatrixAngle();
            default -> new LinearConstructor();
        };
    }
}
