package rich.util.inventory.impl;

import rich.events.api.EventHandler;
import rich.events.api.EventManager;
import rich.events.impl.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;

public class Scheduler {
    private final ArrayList<SchedulerAction> actions = new ArrayList<>();

    public Scheduler() {
        EventManager.register(this);
    }

    public void scheduleOnce(Runnable runnable, int ticks) {
        actions.add(new SchedulerAction(runnable, ticks, false));
    }

    public void scheduleForever(Runnable runnable, int interval) {
        actions.add(new SchedulerAction(runnable, interval, true));
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (actions.isEmpty()) return;

        Iterator<SchedulerAction> iterator = actions.iterator();
        while (iterator.hasNext()) {
            SchedulerAction action = iterator.next();
            action.tick--;
            if (action.tick <= 0) {
                action.action.run();
                if (!action.forever)
                    iterator.remove();
                else
                    action.tick = action.startTick;
            }
        }
    }

    private static class SchedulerAction {
        int tick, startTick;
        Runnable action;
        boolean forever;

        public SchedulerAction(Runnable action, int tick, boolean forever) {
            this.action = action;
            this.tick = tick;
            this.startTick = tick;
            this.forever = forever;
        }
    }
}