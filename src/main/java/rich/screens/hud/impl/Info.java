package rich.screens.hud.impl;

import net.minecraft.client.gui.DrawContext;
import rich.screens.hud.drags.AbstractDraggable;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;

import java.awt.*;

public class Info extends AbstractDraggable {

    private final Decelerate widthAnim = new Decelerate();
    private float targetWidth = 160f;
    private float currentWidth = 160f;

    public Info() {
        super("Info", 10, 10, 280, 32, true);
        widthAnim.setMs(400);
        widthAnim.setValue(1);
        widthAnim.setDirection(Direction.FORWARDS);
    }

    @Override
    public void tick() {
//        int x = mc.player != null ? (int) mc.player.getX() : 0;
//        int y = mc.player != null ? (int) mc.player.getY() : 0;
//        int z = mc.player != null ? (int) mc.player.getZ() : 0;
//
//        int tps = 20;
//        int ping = mc.getNetworkHandler() != null && mc.player != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0 : 0;
//
//        String fullText = "X: " + x + " Y: " + y + " Z: " + z + "   ▸   TPS " + tps + "   ▸   Ping " + ping + "ms";
//
//        float textWidth = Fonts.SF.get(6).getWidth(fullText);
//        targetWidth = textWidth + 20f;
//
//        if (Math.abs(targetWidth - currentWidth) > 0.5f) {
//            widthAnim.reset();
//        }
//
//        if (!widthAnim.isDone()) {
//            Double progress = widthAnim.getOutput();
//            currentWidth = (float) (currentWidth + (targetWidth - currentWidth) * progress);
//        } else {
//            currentWidth = targetWidth;
//        }
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
//        int screenHeight = mc.getWindow().getScaledHeight();
//
//        float width = currentWidth;
//        float height = 17.5f;
//
//        float x = 10;
//        float y = screenHeight - height - 10;
//
//        setX((int) x);
//        setY((int) y);
//        setWidth((int) width);
//        setHeight((int) height);
//
//        int coordX = mc.player != null ? (int) mc.player.getX() : 0;
//        int coordY = mc.player != null ? (int) mc.player.getY() : 0;
//        int coordZ = mc.player != null ? (int) mc.player.getZ() : 0;
//
//        int tps = 20;
//        int ping = mc.getNetworkHandler() != null && mc.player != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0 : 0;
//
//        Fonts.SF.get(6).drawString("X: " + coordX + " Y: " + coordY + " Z: " + coordZ + "   ▸   Tps " + tps + "   ▸   Ping " + ping + "ms", x - 6, y + 15, new Color(245, 245, 245));
    }
}