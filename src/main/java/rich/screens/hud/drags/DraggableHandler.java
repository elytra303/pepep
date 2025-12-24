package rich.screens.hud.drags;

import net.minecraft.client.gui.DrawContext;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.impl.render.Hud;

public class DraggableHandler implements IMinecraft {

    private static DraggableHandler instance;

    public static DraggableHandler getInstance() {
        if (instance == null) {
            instance = new DraggableHandler();
        }
        return instance;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        DraggableRepository repository = getRepository();
        if (repository == null) return;

        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return;

        for (AbstractDraggable draggable : repository.draggable()) {
            try {
                if (draggable.canDraw(hud, draggable)) {
                    draggable.render(context, mouseX, mouseY, delta);
                }

                if (!draggable.isCloseAnimationFinished()) {
                    draggable.validPosition();
                }

            } catch (Exception ignored) {}
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        DraggableRepository repository = getRepository();
        if (repository == null) return false;

        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return false;

        for (int i = repository.draggable().size() - 1; i >= 0; i--) {
            AbstractDraggable draggable = repository.draggable().get(i);
            if (draggable.canDraw(hud, draggable) && draggable.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        DraggableRepository repository = getRepository();
        if (repository == null) return false;

        for (AbstractDraggable draggable : repository.draggable()) {
            if (draggable.isDragging()) {
                draggable.mouseReleased(mouseX, mouseY, button);
                return true;
            }
        }
        return false;
    }

    public boolean isDragging() {
        DraggableRepository repository = getRepository();
        if (repository == null) return false;

        for (AbstractDraggable draggable : repository.draggable()) {
            if (draggable.isDragging()) return true;
        }
        return false;
    }

    private DraggableRepository getRepository() {
        Initialization init = Initialization.getInstance();
        if (init == null || init.getManager() == null) return null;
        return init.getManager().getDraggableRepository();
    }
}