package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.Render2D;
import rich.util.render.item.ItemRender;

import java.awt.*;

public class Inventory extends AbstractHudElement {

    private static final int SLOT_SIZE = 12;
    private static final int SLOTS_PER_ROW = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final float ITEM_SCALE = 0.5f;

    public Inventory() {
        super("Inventory", 20, 60, 200, 80, true);
        startAnimation();
    }

    @Override
    public void tick() {
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (mc.player == null) return;

        float x = getX();
        float y = getY();

        float padding = 6;
        float slotGap = 1;

        float slotsWidth = SLOTS_PER_ROW * SLOT_SIZE + (SLOTS_PER_ROW - 1) * slotGap;
        float slotsHeight = INVENTORY_ROWS * SLOT_SIZE + (INVENTORY_ROWS - 1) * slotGap;

        float contentWidth = slotsWidth + padding * 2;
        float contentHeight = slotsHeight + padding * 2;

        setWidth((int) contentWidth);
        setHeight((int) (contentHeight + 4));

        int filledSlots = 0;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                filledSlots++;
            }
        }

        float contentY = y;

        Render2D.gradientRect(x + 2, contentY + 2, contentWidth - 4, contentHeight - 4,
                new int[]{
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(32, 32, 32, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(32, 32, 32, 255).getRGB()
                },
                5);

        Render2D.outline(x + 2, contentY + 2, contentWidth - 4, contentHeight - 4, 0.35f, new Color(90, 90, 90, 255).getRGB(), 5);

        float slotsStartX = x + padding;
        float slotsStartY = contentY + padding;

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotIndex = 9 + row * SLOTS_PER_ROW + col;

                float slotX = slotsStartX + col * (SLOT_SIZE + slotGap);
                float slotY = slotsStartY + row * (SLOT_SIZE + slotGap);

                ItemStack stack = mc.player.getInventory().getStack(slotIndex);

                if (stack.isEmpty()) {
                    Render2D.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE, new Color(28, 28, 28, 255).getRGB(), 2);
                } else {
                    Render2D.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE, new Color(28, 28, 28, 255).getRGB(), 2);

                    float itemSize = 16 * ITEM_SCALE;
                    float itemX = slotX + (SLOT_SIZE - itemSize) / 2;
                    float itemY = slotY + (SLOT_SIZE - itemSize) / 2;

                    if (ItemRender.needsContextRender(stack)) {
                        ItemRender.drawItemWithContext(context, stack, itemX, itemY, ITEM_SCALE, 1.0f);
                    } else {
                        ItemRender.drawItem(stack, itemX, itemY, ITEM_SCALE, 1.0f);
                    }
                }
            }
        }
    }
}