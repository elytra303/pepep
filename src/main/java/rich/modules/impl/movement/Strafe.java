package rich.modules.impl.movement;

import net.minecraft.client.MinecraftClient;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.math.TaskPriority;
import rich.util.move.MoveUtil;

public class Strafe extends ModuleStructure {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public SelectSetting mode = new SelectSetting("Режим", "Выберите тип стрейфов")
            .value("Matrix", "Grim")
            .selected("Matrix");
    SliderSettings speed = new SliderSettings("Скорость", "Выберите скорость для стрейфа")
            .setValue(0.42F).range(0F, 1F).visible(() -> mode.isSelected("Matrix"));

    private float lastYaw, lastPitch;
    private final Angle rot = new Angle(0, 0);

    public Strafe() {
        super("Strafe", "Strafe", ModuleCategory.MOVEMENT);
        setup(mode, speed);
    }

    public static Strafe getInstance() {
        return Instance.get(Strafe.class);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean moving = MoveUtil.hasPlayerMovement();

        float yaw = mc.player.getYaw();

        if (mode.isSelected("Matrix")) {
            if (moving) {
                yaw = MoveUtil.moveYaw(mc.player.getYaw());
                double motion = speed.getValue() * 1.5f;
                MoveUtil.setVelocity(motion);
            } else {
                MoveUtil.setVelocity(0);
            }
            mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
        } else if (mode.isSelected("Grim")) {
            if (moving) {
                AngleConfig.freeCorrection = true;
                yaw = MoveUtil.moveYaw(mc.player.getYaw());
                rot.setYaw(yaw);
                rot.setPitch(mc.player.getPitch());
                if (Aura.getInstance().target == null) {
                    AngleConnection.INSTANCE.rotateTo(rot, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1 , this);
                }
            }
        }

        lastYaw = yaw;
        lastPitch = 0;
    }


    @Override
    public void activate() {
        super.activate();
        lastYaw = mc.player != null ? mc.player.getYaw() : 0;
        lastPitch = mc.player != null ? mc.player.getPitch() : 0;
    }
}
