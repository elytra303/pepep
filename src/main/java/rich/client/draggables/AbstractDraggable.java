package rich.client.draggables;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import rich.IMinecraft;
import rich.events.impl.PacketEvent;
import rich.events.impl.SetScreenEvent;
import rich.modules.impl.render.Hud;
import rich.util.animations.Animation;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;

@Setter
@Getter
public abstract class AbstractDraggable implements Draggable, IMinecraft {
    private String name;
    private int x, y, width, height;
    private boolean dragging, canDrag;
    private int dragX, dragY;

    private float animatedX, animatedY;
    private float targetX, targetY;
    private static final float SMOOTHNESS = 0.15f;

    private static final float FIXED_GUI_SCALE = 2.0f;

    public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.canDrag = canDrag;
        this.animatedX = x;
        this.animatedY = y;
        this.targetX = x;
        this.targetY = y;
    }

    public final Animation scaleAnimation = new Decelerate().setValue(1).setMs(200);

    private int getFixedScaledWidth() {
        return (int) (window.getWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        return (int) (window.getHeight() / FIXED_GUI_SCALE);
    }

    private int toFixedX(double mouseX) {
        return (int) (mouseX / window.getScaledWidth() * getFixedScaledWidth());
    }

    private int toFixedY(double mouseY) {
        return (int) (mouseY / window.getScaledHeight() * getFixedScaledHeight());
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {}

    @Override
    public void packet(PacketEvent e) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int fixedMouseX = toFixedX(mouseX);
        int fixedMouseY = toFixedY(mouseY);

        if (!dragging) {
            dragX = 0;
            dragY = 0;
        }

        Hud hud = Hud.getInstance();
        int mouseDragX = fixedMouseX + dragX;
        int mouseDragY = fixedMouseY + dragY;
        int windowWidth = getFixedScaledWidth();
        int windowHeight = getFixedScaledHeight();

        if (dragging) {
            targetX = Math.max(0, Math.min(mouseDragX, windowWidth - width));
            targetY = Math.max(0, Math.min(mouseDragY, windowHeight - height));
        }

        float deltaMultiplier = Math.min(delta * 60f, 1.0f);
        float lerpFactor = 1.0f - (float) Math.pow(1.0f - SMOOTHNESS, deltaMultiplier);

        animatedX += (targetX - animatedX) * lerpFactor;
        animatedY += (targetY - animatedY) * lerpFactor;

        this.x = Math.round(animatedX);
        this.y = Math.round(animatedY);
    }

    public void drawRect(float x, float y, float width, float height) {
//        Render2D.rect(x, y, width, height, new Color(255, 45, 85, 180));
    }

    public boolean isChat(Screen screen) {
        return screen instanceof ChatScreen;
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (!isChat(e.getScreen()) && dragging) {
            dragging = false;
            dragX = 0;
            dragY = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int fixedMouseX = toFixedX(mouseX);
        int fixedMouseY = toFixedY(mouseY);

        if (isHovered(fixedMouseX, fixedMouseY) && button == 0 && canDrag) {
            dragging = true;
            dragX = x - fixedMouseX;
            dragY = y - fixedMouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            dragX = 0;
            dragY = 0;
            return true;
        }
        return false;
    }

    public abstract void drawDraggable(DrawContext context, int alpha);

    public void stopAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    public void startAnimation() {
        scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void validPosition() {
        int windowWidth = getFixedScaledWidth();
        int windowHeight = getFixedScaledHeight();

        if (x + width > windowWidth) x = windowWidth - width;
        if (y + height > windowHeight) y = windowHeight - height;
        if (y < 0) y = 0;
        if (x < 0) x = 0;

        targetX = x;
        targetY = y;
        animatedX = x;
        animatedY = y;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isCloseAnimationFinished() {
        return scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        return hud.isState() && hud.interfaceSettings.isSelected(draggable.getName()) && visible();
    }
}