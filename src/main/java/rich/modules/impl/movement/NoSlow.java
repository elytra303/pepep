package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.TickEvent;
import rich.events.impl.UsingItemEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.Instance;
import rich.util.inventory.script.Script;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends ModuleStructure {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;

    public final SelectSetting itemMode = new SelectSetting("Режим предмета", "Выберите режим обхода").value("Grim Old", "SpookyTime");

    public NoSlow() {
        super("NoSlow", "No Slow", ModuleCategory.MOVEMENT);
        setup(itemMode);
    }
    private int ticks = 0;

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player.getActiveHand() == Hand.MAIN_HAND ||  mc.player.getActiveHand() == Hand.OFF_HAND) {
            ticks++;
        } else {
            ticks = 0;
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onUsingItem(UsingItemEvent e) {
        Hand first = mc.player.getActiveHand();
        Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;

        switch (e.getType()) {
            case EventType.ON -> {
                handleItemUse(e, first, second);
            }
            case EventType.POST -> {
                while (!script.isFinished()) script.update();
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleItemUse(UsingItemEvent e, Hand first, Hand second) {
        switch (itemMode.getSelected()) {
            case "Grim Old" -> {
                if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                    PlayerInteractionHelper.interactItem(first);
                    PlayerInteractionHelper.interactItem(second);
                    e.cancel();
                }
            }
            case "SpookyTime" -> {
                if (ticks > 1F && mc.player.getItemUseTime() > 2) {
                    e.cancel();
                    ticks = 0;
                }
            }
        }
    }
}