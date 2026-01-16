package rich.client.draggables;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import rich.Initialization;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.animations.AlphaAnim;
import rich.util.render.Render2D;

import java.util.HashMap;
import java.util.Map;

public class Drag {

    private static final float OUTLINE_OFFSET = 3.0f;
    private static final float OUTLINE_THICKNESS = 0.5f;
    private static final int OUTLINE_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    private static HudElement draggingElement;
    private static int startX, startY;
    private static final Map<HudElement, AlphaAnim> hoverAnimations = new HashMap<>();

    public static void onDraw(DrawContext context, int mouseX, int mouseY, float delta) {
        HudManager hudManager = getHudManager();
        if (hudManager == null) return;

        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return;

        if (draggingElement != null) {
            draggingElement.setX(mouseX - startX);
            draggingElement.setY(mouseY - startY);
        }

        hudManager.render(context, delta, mouseX, mouseY);

        for (HudElement element : hudManager.getEnabledElements()) {
            if (!element.visible()) {
                hoverAnimations.remove(element);
                continue;
            }

            boolean isHovered = isHovered(element, mouseX, mouseY);

            float rounding = element.getRoundingRadius();
            float offset = OUTLINE_OFFSET;
            float outlineX = element.getX() - offset;
            float outlineY = element.getY() - offset;
            float outlineWidth = element.getWidth() + offset * 2;
            float outlineHeight = element.getHeight() + offset * 2;
            float outlineRounding = Math.max(0, rounding + offset);

            AlphaAnim anim = hoverAnimations.computeIfAbsent(element, e -> new AlphaAnim(0.0f, 0.3f));

            if (isHovered) {
                anim.setTarget(1.0f);
            } else {
                anim.setTarget(0.0f);
            }

            anim.update();
            float alpha = anim.getValue();

            if (alpha > 0.01f && outlineWidth > 0 && outlineHeight > 0) {
                int outlineColor = ColorUtil.withAlpha(OUTLINE_COLOR, alpha * 0.8f);
                Render2D.outline(outlineX, outlineY, outlineWidth, outlineHeight, OUTLINE_THICKNESS, outlineColor, outlineRounding);
            } else if (!isHovered && alpha <= 0.001f) {
                hoverAnimations.remove(element);
            }
        }
    }

    public static void onMouseClick(Click click) {
        if (click.button() == 0) {
            HudManager hudManager = getHudManager();
            if (hudManager == null) return;

            double mouseX = click.x();
            double mouseY = click.y();

            HudElement element = hudManager.getElementAt(mouseX, mouseY);
            if (element != null) {
                if (element instanceof AbstractHudElement abstractElement && abstractElement.isDraggable()) {
                    draggingElement = element;
                    startX = (int) mouseX - element.getX();
                    startY = (int) mouseY - element.getY();
                }
            }
        }
    }

    public static void onMouseRelease(Click click) {
        if (click.button() == 0 && draggingElement != null) {
            draggingElement = null;
        }
    }

    public static boolean isDragging() {
        return draggingElement != null;
    }

    private static boolean isHovered(HudElement element, double mouseX, double mouseY) {
        int x = element.getX();
        int y = element.getY();
        int width = element.getWidth();
        int height = element.getHeight();
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    private static HudManager getHudManager() {
        if (Initialization.getInstance() == null) return null;
        if (Initialization.getInstance().getManager() == null) return null;
        return Initialization.getInstance().getManager().getHudManager();
    }

    public static void tick() {
        HudManager hudManager = getHudManager();
        if (hudManager != null) {
            hudManager.tick();
        }
    }
}