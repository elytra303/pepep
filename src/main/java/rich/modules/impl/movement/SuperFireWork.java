package rich.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.FireworkEvent;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperFireWork extends ModuleStructure {
    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима")
            .value("BravoHvH", "ReallyWorld", "PulseHVH", "Custom");

    SliderSettings customSpeedSetting = new SliderSettings("Скорость", "Скорость для Custom режима")
            .range(1.5f, 3f)
            .setValue(1.963f)
            .visible(() -> modeSetting.isSelected("Custom"));

    BooleanSetting nearBoostSetting = new BooleanSetting("", "");

    public SuperFireWork() {
        super("SuperFireWork", "Super FireWork", ModuleCategory.MOVEMENT);
        setup(modeSetting, customSpeedSetting);
    }

    @EventHandler
    public void onFirework(FireworkEvent e) {
        if (mc.player == null || !mc.player.isGliding()) return;

        float yaw = AngleConnection.INSTANCE.getRotation().getYaw() % 360f;
        if (yaw < 0) yaw += 360f;

        boolean isDiagonal = false;
        boolean nearPlayer = false;
        double speedXZ = 1.5;
        double speedY = 1.5;

        if (modeSetting.isSelected("ReallyWorld")) {
            float[] diagonals = {45f, 135f, 225f, 315f};
            float closestDiff = 180f;

            for (float d : diagonals) {
                float diff = Math.abs(yaw - d);
                diff = Math.min(diff, 360f - diff);
                if (diff < closestDiff) closestDiff = diff;
            }

            if (closestDiff <= 4) {
                speedXZ = 2.2;
            } else if (closestDiff <= 8) {
                speedXZ = 2.06;
            } else if (closestDiff <= 12) {
                speedXZ = 1.98;
            } else if (closestDiff <= 16) {
                speedXZ = 1.87;
            } else if (closestDiff <= 20) {
                speedXZ = 1.8;
            } else if (closestDiff <= 24) {
                speedXZ = 1.74;
            } else if (closestDiff <= 28) {
                speedXZ = 1.7;
            } else if (closestDiff <= 32) {
                speedXZ = 1.65;
            } else if (closestDiff <= 36) {
                speedXZ = 1.63;
            } else {
                speedXZ = 1.61;
                speedY = 1.61;
            }

            Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
            Vec3d currentVelocity = e.getVector();

            e.setVector(currentVelocity.add(
                    rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
                    rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
                    rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
            ));
        } else if (modeSetting.isSelected("BravoHvH")) {
            for (float d : new float[]{45f, 135f, 225f, 315f}) {
                float diff = Math.abs(yaw - d);
                diff = Math.min(diff, 360f - diff);
                if (diff <= 16f) {
                    isDiagonal = true;
                    break;
                }
            }

            if (nearBoostSetting.isValue() && mc.world != null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player) continue;
                    if (player.distanceTo(mc.player) <= 4f) {
                        nearPlayer = true;
                        break;
                    }
                }
            }

            if (isDiagonal) {
                speedXZ = 1.963;
            } else if (nearBoostSetting.isValue() && nearPlayer) {
                speedXZ = 1.82;
                speedY = 1.67;
            } else {
                speedXZ = 1.675;
                speedY = 1.66;
            }

            Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
            Vec3d currentVelocity = e.getVector();

            e.setVector(currentVelocity.add(
                    rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
                    rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
                    rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
            ));
        } else if (modeSetting.isSelected("PulseHVH")) {
            for (float d : new float[]{45f, 135f, 225f, 315f}) {
                float diff = Math.abs(yaw - d);
                diff = Math.min(diff, 360f - diff);
                if (diff <= 16f) {
                    isDiagonal = true;
                    break;
                }
            }

            if (nearBoostSetting.isValue() && mc.world != null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player) continue;
                    if (player.distanceTo(mc.player) <= 5f) {
                        nearPlayer = true;
                        break;
                    }
                }
            }

            if (isDiagonal) {
                speedXZ = 1.963;
            } else if (nearBoostSetting.isValue() && nearPlayer) {
                speedXZ = 1.82;
                speedY = 1.67;
            } else {
                speedXZ = 1.675;
                speedY = 1.66;
            }

            Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
            Vec3d currentVelocity = e.getVector();

            e.setVector(currentVelocity.add(
                    rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
                    rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
                    rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
            ));
        } else if (modeSetting.isSelected("Custom")) {
            for (float d : new float[]{45f, 135f, 225f, 315f}) {
                float diff = Math.abs(yaw - d);
                diff = Math.min(diff, 360f - diff);
                if (diff <= 16f) {
                    isDiagonal = true;
                    break;
                }
            }

            if (nearBoostSetting.isValue() && mc.world != null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player) continue;
                    if (player.distanceTo(mc.player) <= 5f) {
                        nearPlayer = true;
                        break;
                    }
                }
            }

            if (isDiagonal) {
                speedXZ = customSpeedSetting.getValue();
            } else if (nearBoostSetting.isValue() && nearPlayer) {
                speedXZ = customSpeedSetting.getValue() - 0.1f;
                speedY = 1.67;
            } else {
                speedXZ = 1.675;
                speedY = 1.66;
            }

            Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
            Vec3d currentVelocity = e.getVector();

            e.setVector(currentVelocity.add(
                    rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
                    rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
                    rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
            ));
        }
    }
}