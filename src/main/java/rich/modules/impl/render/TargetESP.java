package rich.modules.impl.render;

import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.animations.Animation;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;
import rich.util.render.render3D.Render3D;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends ModuleStructure implements IMinecraft {

    private static TargetESP instance;

    public static TargetESP getInstance() {
        return instance;
    }

    Animation espAnim = new Decelerate().setMs(400).setValue(1);

    SelectSetting targetEspType = new SelectSetting("Отображения таргета", "Выбирает тип цели esp")
            .value("Circle")
            .selected("Circle");

    public ColorSetting colorSetting = new ColorSetting("Цвет", "Выберите цвет для esp")
            .setColor(new Color(255, 101, 57, 255).getRGB());

    public TargetESP() {
        super("TargetEsp", "Target Esp", ModuleCategory.RENDER);
        instance = this;
        setup(targetEspType, colorSetting);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        Render3D.updateTargetEsp();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        LivingEntity currentTarget = null;
        LivingEntity lastTarget = null;

        if (Aura.getInstance() != null && Aura.getInstance().isState()) {
            currentTarget = Aura.target;
            lastTarget = Aura.getInstance().lastTarget;
        }

        espAnim.setDirection(currentTarget != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = espAnim.getOutput().floatValue();

        if (lastTarget != null && !espAnim.isFinished(Direction.BACKWARDS)) {
            float red = MathHelper.clamp((lastTarget.hurtTime - e.getPartialTicks()) / 20f, 0f, 1f);
            Render3D.drawCircle(e.getStack(), lastTarget, anim, red, colorSetting.getColor());
        }
    }
}