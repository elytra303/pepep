package rich.modules.impl.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.move.MoveUtil;
import rich.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Jesus extends ModuleStructure {

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения по воде")
            .value("Matrix", "MetaHVH")
            .selected("Matrix");

    StopWatch timer = new StopWatch();

    @NonFinal
    boolean isMoving;

    float melonBallSpeed = 0.44F;

    public Jesus() {
        super("Jesus", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @Override
    public void deactivate() {

    }

    @EventHandler
    public void tick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.isSelected("Matrix")) {
            handleMatrixMode();
        } else if (mode.isSelected("MetaHVH")) {
            handleMetaHVHMode();
        }
    }

    private void handleMatrixMode() {
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
            ItemStack offHandItem = mc.player.getOffHandStack();

            String itemName = offHandItem.getName().getString();
            float appliedSpeed = 0F;

            if (itemName.contains("Ломтик Дыни") && speedEffect != null && speedEffect.getAmplifier() == 2) {
                appliedSpeed = 0.4283F * 1.15F;
            } else {
                if (speedEffect != null) {
                    if (speedEffect.getAmplifier() == 2) {
                        appliedSpeed = melonBallSpeed * 1.15F;
                    } else if (speedEffect.getAmplifier() == 1) {
                        appliedSpeed = melonBallSpeed;
                    }
                } else {
                    appliedSpeed = melonBallSpeed * 0.68F;
                }
            }

            if (slowEffect != null) {
                appliedSpeed *= 0.85f;
            }

            MoveUtil.setVelocity(appliedSpeed);

            isMoving = mc.options.forwardKey.isPressed()
                    || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed()
                    || mc.options.rightKey.isPressed();

            if (!isMoving) {
                mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            }

            double yMotion = mc.options.jumpKey.isPressed() ? 0.019 : 0.003;
            mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
        }
    }

    private void handleMetaHVHMode() {
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);

            float appliedSpeed = 0.47F;

            if (speedEffect != null) {
                if (speedEffect.getAmplifier() == 2) {
                    appliedSpeed = 0.47F * 1.2F;
                } else if (speedEffect.getAmplifier() == 1) {
                    appliedSpeed = 0.47F * 1.05F;
                }
            } else {
                appliedSpeed = 0.47F * 0.7F;
            }

            if (slowEffect != null) {
                appliedSpeed *= 0.8f;
            }

            MoveUtil.setVelocity(appliedSpeed);

            isMoving = mc.options.forwardKey.isPressed()
                    || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed()
                    || mc.options.rightKey.isPressed();

            if (!isMoving) {
                mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            }

            double yMotion = mc.options.jumpKey.isPressed() ? 0.025 : 0.005;
            mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
        }
    }
}