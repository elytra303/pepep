package rich.modules.impl.movement;

import lombok.Getter;
import net.minecraft.entity.effect.StatusEffects;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.TriggerBot;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.Instance;

public class AutoSprint extends ModuleStructure {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    public static int tickStop;

    @Getter
    private final MultiSelectSetting settings = new MultiSelectSetting("Игнорировать", "Не дает спринтиться при эффектах")
            .value("Голод", "Замедление", "Слепота")
            .selected();


    public AutoSprint() {
        super("AutoSprint", ModuleCategory.MOVEMENT);
        setup(settings);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean hasSlowness = mc.player.hasStatusEffect(StatusEffects.SLOWNESS);
        boolean hasBlindness = mc.player.hasStatusEffect(StatusEffects.BLINDNESS);

        boolean shouldCancelSprintDueToSlowness = hasSlowness && !settings.isSelected("Замедление");
        boolean shouldCancelSprintDueToBlindness = hasBlindness && !settings.isSelected("Слепота");

        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();

        if (shouldPreventSprintForCombat()) {
            tickStop = 2;
        }

        if (tickStop > 0 || sneaking || shouldCancelSprintDueToSlowness || shouldCancelSprintDueToBlindness) {
            mc.player.setSprinting(false);
        } else if (!horizontal && mc.player.forwardSpeed > 0 && !mc.options.sprintKey.isPressed()) {
            mc.player.setSprinting(true);
        }

        tickStop--;
    }

    private boolean shouldPreventSprintForCombat() {
        Aura aura = Aura.getInstance();
        if (aura != null && aura.isState() && Aura.target != null && Aura.target.isAlive()) {
            if (aura.isResetSprintLegit() || aura.isResetSprintPacket()) {
                float distance = mc.player.distanceTo(Aura.target);
                if (distance <= aura.attackrange.getValue() + 0.5f) {
                    return true;
                }
            }
        }

        TriggerBot triggerBot = TriggerBot.getInstance();
        if (triggerBot != null && triggerBot.isState() && triggerBot.target != null && triggerBot.target.isAlive()) {
            if (triggerBot.isResetSprintLegit() || triggerBot.isResetSprintPacket()) {
                float distance = mc.player.distanceTo(triggerBot.target);
                if (distance <= triggerBot.attackRange.getValue() + 0.5f) {
                    return true;
                }
            }
        }

        return false;
    }
}