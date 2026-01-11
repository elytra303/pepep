package rich.client.draggables.impl;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractDraggable;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;

public class Watermark extends AbstractDraggable {

    public Watermark() {
        super("Watermark", 10, 10, 280, 32, true);
    }

    @Override
    public void tick() {
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
    }
}