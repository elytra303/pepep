package rich.screens.hud.impl;

import net.minecraft.client.gui.DrawContext;
import rich.screens.hud.drags.AbstractDraggable;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;


import java.awt.*;

public class Watermark extends AbstractDraggable {

    private float pulseAnim = 0f;
    private final Decelerate widthAnim = new Decelerate();
    private float targetWidth = 160f;
    private float currentWidth = 160f;

    public Watermark() {
        super("Watermark", 10, 10, 280, 32, true);
        widthAnim.setMs(400);
        widthAnim.setValue(1);
        widthAnim.setDirection(Direction.FORWARDS);
    }

    @Override
    public void tick() {
//        pulseAnim += 0.05f;
//        if (pulseAnim > Math.PI * 2) pulseAnim = 0;
//
//        String role = "Role";
//        String username = "Username";
//        String fps = "Fps " + mc.getCurrentFps();
//        String fullText = "Rich Modern" + "   ▸   " + role + "   ▸   " + username + "   ▸   " + fps;
//
//        float textWidth = Fonts.SF.get(6).getWidth(fullText);
//        targetWidth = textWidth + 20f;
//
//        if (Math.abs(targetWidth - currentWidth) > 0.5f) {
//            widthAnim.reset();
//        }
//
//        if (!widthAnim.isDone()) {
//            Double progress =  widthAnim.getOutput();
//            currentWidth = (float) (currentWidth + (targetWidth - currentWidth) * progress);
//        } else {
//            currentWidth = targetWidth;
//        }
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
//        float x = getX();
//        float y = getY();
//
//        float width = currentWidth;
//        float height = 17.5f;
//
//        setWidth((int) width);
//        setHeight((int) height);
//
//        String role = "Role";
//        String username = "Username";
//        String fps = "Fps " + mc.getCurrentFps();
//
//        Render2D.glass(x, y, width, height, 9, 2f, 25f, 15, 1.0f, 0.06f, 2f, new Color(128, 128, 128));
//        Render2D.rect(x, y, width, height, 9, 2, new Color(15, 15, 15, 125));
//
//        Fonts.SF.get(6).drawString("Rich Modern" + "   ▸   " + role + "   ▸   " + username + "   ▸   " + fps, x + 9, y + 5.5f, new Color(245, 245, 245));
    }
}