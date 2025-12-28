package rich.screens.clickgui.impl.autobuy.autobuyui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.ItemRegistry;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;
import rich.util.render.item.ItemRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AutoBuyGuiComponent implements IMinecraft {
    private float x, y, width, height;
    private float targetScroll = 0f;
    private float smoothScroll = 0f;

    private final Map<AutoBuyableItem, Float> toggleAnimations = new HashMap<>();
    private final Map<AutoBuyableItem, Float> hoverAnimations = new HashMap<>();
    private final Map<AutoBuyableItem, Float> enabledAnimations = new HashMap<>();

    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    private AutoBuyableItem hoveredItem = null;
    private AutoBuyableItem editingItem = null;
    private EditField editingField = EditField.NONE;
    private String inputText = "";
    private float cursorBlink = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private float panelAlpha = 1f;
    private float currentScale = 1f;

    private static final float ITEM_HEIGHT = 22f;
    private static final float ITEM_SPACING = 3f;
    private static final float CATEGORY_HEIGHT = 18f;
    private static final float ANIM_SPEED = 11f;

    private final List<PendingIcon> pendingIcons = new ArrayList<>();
    private final List<PendingBlockIcon> pendingBlockIcons = new ArrayList<>();

    private static class PendingIcon {
        ItemStack stack;
        float x, y, scale, alpha;

        PendingIcon(ItemStack stack, float x, float y, float scale, float alpha) {
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.alpha = alpha;
        }
    }

    private static class PendingBlockIcon {
        ItemStack stack;
        float x, y, scale, alpha;

        PendingBlockIcon(ItemStack stack, float x, float y, float scale, float alpha) {
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.alpha = alpha;
        }
    }

    public enum EditField {
        NONE, PRICE, QUANTITY
    }

    public AutoBuyGuiComponent() {
    }

    public void position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void size(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setAlpha(float alpha) {
        this.panelAlpha = alpha;
    }

    public boolean isEditing() {
        return editingItem != null && editingField != EditField.NONE;
    }

    private boolean isHovered(double mx, double my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    private int clampAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public void render(DrawContext context, float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        this.panelAlpha = alphaMultiplier;

        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        this.currentScale = (float) guiScale / currentGuiScale;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        updateAnimations(deltaTime);

        cursorBlink += deltaTime * 2f;
        if (cursorBlink > 1f) cursorBlink -= 1f;

        hoveredItem = null;
        pendingIcons.clear();
        pendingBlockIcons.clear();

        float contentHeight = calculateContentHeight();
        float maxScroll = Math.max(0, contentHeight - height + 10f);
        targetScroll = clamp(targetScroll, -maxScroll, 0);

        float diff = targetScroll - smoothScroll;
        smoothScroll += diff * 0.3f;
        if (Math.abs(diff) < 0.1f) {
            smoothScroll = targetScroll;
        }

        renderPanelBackground(alphaMultiplier);

        float clipX = x + 3;
        float clipY = y + 1;
        float clipW = width - 6;
        float clipH = height - 3;

        int scaledClipX = (int) (clipX * currentScale);
        int scaledClipY = (int) (clipY * currentScale);
        int scaledClipX2 = (int) ((clipX + clipW) * currentScale);
        int scaledClipY2 = (int) ((clipY + clipH) * currentScale);

        context.enableScissor(scaledClipX, scaledClipY, scaledClipX2, scaledClipY2);
        Scissor.enable(clipX, clipY, clipW, clipH, guiScale);

        float currentY = y + 5 + smoothScroll;
        List<CategoryItems> categories = getCategorizedItems();

        for (CategoryItems category : categories) {
            if (category.items.isEmpty()) continue;

            if (isInView(currentY, CATEGORY_HEIGHT, clipY, clipH)) {
                renderCategoryHeader(x + 5, currentY, width - 10, category.name, alphaMultiplier);
            }
            currentY += CATEGORY_HEIGHT;

            for (AutoBuyableItem item : category.items) {
                if (isInView(currentY, ITEM_HEIGHT, clipY, clipH)) {
                    renderItem(context, item, x + 5, currentY, width - 10, mouseX, mouseY, alphaMultiplier);
                }
                currentY += ITEM_HEIGHT + ITEM_SPACING;
            }

            currentY += 8f;
        }

        for (PendingIcon icon : pendingIcons) {
            ItemRender.drawItem(icon.stack, icon.x, icon.y, icon.scale, icon.alpha);
        }
        pendingIcons.clear();

        for (PendingBlockIcon icon : pendingBlockIcons) {
            ItemRender.drawBlockItem(context, icon.stack, icon.x, icon.y, icon.scale, icon.alpha);
        }
        pendingBlockIcons.clear();

        Scissor.disable();
        context.disableScissor();
    }

    private boolean isInView(float itemY, float itemHeight, float clipY, float clipH) {
        float itemBottom = itemY + itemHeight;
        float clipBottom = clipY + clipH;
        return itemBottom > clipY && itemY < clipBottom;
    }

    private void updateAnimations(float deltaTime) {
        for (AutoBuyableItem item : ItemRegistry.getAllItems()) {
            float targetToggle = item.isEnabled() ? 1f : 0f;
            float currentToggle = toggleAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
            float newToggle = smoothLerp(currentToggle, targetToggle, ANIM_SPEED * deltaTime);
            toggleAnimations.put(item, newToggle);

            float targetEnabled = item.isEnabled() ? 1f : 0f;
            float currentEnabled = enabledAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
            float newEnabled = smoothLerp(currentEnabled, targetEnabled, ANIM_SPEED * deltaTime);
            enabledAnimations.put(item, newEnabled);

            boolean isHov = item == hoveredItem;
            float targetHover = isHov ? 1f : 0f;
            float currentHover = hoverAnimations.getOrDefault(item, 0f);
            hoverAnimations.put(item, smoothLerp(currentHover, targetHover, ANIM_SPEED * deltaTime));
        }
    }

    private float smoothLerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * clamp(speed, 0f, 1f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float calculateContentHeight() {
        float total = 5f;
        List<CategoryItems> categories = getCategorizedItems();

        for (CategoryItems category : categories) {
            if (category.items.isEmpty()) continue;
            total += CATEGORY_HEIGHT;
            total += category.items.size() * (ITEM_HEIGHT + ITEM_SPACING);
            total += 8f;
        }

        return total;
    }

    private void renderPanelBackground(float alphaMultiplier) {
        int bgAlpha = clampAlpha((int) (15 * alphaMultiplier));
        int outlineAlpha = clampAlpha((int) (215 * alphaMultiplier));

        Render2D.rect(x, y, width, height, new Color(64, 64, 64, bgAlpha).getRGB(), 7f);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 7f);
    }

    private void renderCategoryHeader(float catX, float catY, float catWidth, String name, float alphaMultiplier) {
        int textAlpha = clampAlpha((int) (180 * alphaMultiplier));
        float textWidth = Fonts.BOLD.getWidth(name, 5f);
        float lineWidth = (catWidth - textWidth - 16f) / 2f;

        int lineAlpha = clampAlpha((int) (60 * alphaMultiplier));
        Render2D.rect(catX, catY + 6f, lineWidth, 0.5f, new Color(100, 100, 100, lineAlpha).getRGB(), 0);
        Fonts.BOLD.draw(name, catX + lineWidth + 8f, catY + 3f, 5f, new Color(160, 160, 160, textAlpha).getRGB());
        Render2D.rect(catX + lineWidth + textWidth + 16f, catY + 6f, lineWidth, 0.5f, new Color(100, 100, 100, lineAlpha).getRGB(), 0);
    }

    private void renderItem(DrawContext context, AutoBuyableItem item, float itemX, float itemY, float itemW,
                            float mouseX, float mouseY, float alphaMultiplier) {

        if (alphaMultiplier <= 0.01f) return;

        boolean hovered = isHovered(mouseX, mouseY, itemX, itemY, itemW, ITEM_HEIGHT);
        if (hovered) {
            hoveredItem = item;
        }

        float toggleAnim = toggleAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
        float enabledAnim = enabledAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
        float hoverAnim = hoverAnimations.getOrDefault(item, 0f);

        float dimFactor = 0.35f + 0.65f * enabledAnim;

        int baseBg = 64 + (int)(hoverAnim * 36);
        int bgR = clampColor((int) (baseBg * dimFactor));
        int bgG = clampColor((int) (baseBg * dimFactor));
        int bgB = clampColor((int) (baseBg * dimFactor));
        int bgAlpha = clampAlpha((int) ((25 + hoverAnim * 15) * alphaMultiplier));
        Render2D.rect(itemX, itemY, itemW, ITEM_HEIGHT, new Color(bgR, bgG, bgB, bgAlpha).getRGB(), 5f);

        float iconSize = 16f;
        float iconY = itemY + (ITEM_HEIGHT - iconSize) / 2f;
        float iconX = itemX + 2;

        queueItemIcon(item, iconX, iconY, iconSize, enabledAnim, alphaMultiplier);

        String displayName = item.getDisplayName();

        int baseTextBrightness = clampColor((int) (120 + 135 * enabledAnim));
        int textAlpha = clampAlpha((int) ((120 + 135 * enabledAnim) * alphaMultiplier));
        Color textColor = new Color(baseTextBrightness, baseTextBrightness, baseTextBrightness, textAlpha);

        Fonts.BOLD.draw(displayName, itemX + 20, itemY + 5, 5f, textColor.getRGB());

        boolean isEditingPrice = editingItem == item && editingField == EditField.PRICE;
        String priceText = isEditingPrice ? inputText : "$" + item.getSettings().getBuyBelow();

        if (isEditingPrice) {
            float cursorAlphaVal = (float) (Math.sin(cursorBlink * Math.PI * 2) * 0.5 + 0.5);
            if (cursorAlphaVal > 0.5f) priceText += "|";
        }

        float priceX = itemX + 20;
        float priceY = itemY + 13;

        int priceBrightness = clampColor((int) (80 + 60 * enabledAnim));
        int priceAlpha = clampAlpha(isEditingPrice ? (int) (220 * alphaMultiplier) : (int) ((100 + 80 * enabledAnim) * alphaMultiplier));
        Color priceColor = isEditingPrice
                ? new Color(100, 200, 100, priceAlpha)
                : new Color(priceBrightness, priceBrightness, priceBrightness, priceAlpha);

        Fonts.BOLD.draw(priceText, priceX, priceY, 4f, priceColor.getRGB());

        if (item.getSettings().isCanHaveQuantity()) {
            boolean isEditingQty = editingItem == item && editingField == EditField.QUANTITY;
            String qtyText = isEditingQty ? inputText : "x" + item.getSettings().getMinQuantity();

            if (isEditingQty) {
                float cursorAlphaVal = (float) (Math.sin(cursorBlink * Math.PI * 2) * 0.5 + 0.5);
                if (cursorAlphaVal > 0.5f) qtyText += "|";
            }

            float qtyX = priceX + Fonts.BOLD.getWidth(priceText, 4f) + 8;
            int qtyBrightness = clampColor((int) (80 + 60 * enabledAnim));
            int qtyAlpha = clampAlpha(isEditingQty ? (int) (220 * alphaMultiplier) : (int) ((100 + 80 * enabledAnim) * alphaMultiplier));
            Color qtyColor = isEditingQty
                    ? new Color(100, 200, 100, qtyAlpha)
                    : new Color(qtyBrightness, qtyBrightness, qtyBrightness, qtyAlpha);

            Fonts.BOLD.draw(qtyText, qtyX, priceY, 4f, qtyColor.getRGB());
        }

        float toggleW = 14f;
        float toggleH = 8f;
        float toggleX = itemX + itemW - toggleW - 4;
        float toggleY = itemY + ITEM_HEIGHT / 2f - toggleH / 2f;
        renderToggle(toggleX, toggleY, toggleW, toggleH, toggleAnim, enabledAnim, alphaMultiplier);

        float indicatorX = toggleX - 8;
        float indicatorY = itemY + ITEM_HEIGHT / 2f - 2;

        int indicatorR = clampColor((int) (70 + 30 * enabledAnim));
        int indicatorG = clampColor((int) (70 + 130 * enabledAnim));
        int indicatorB = clampColor((int) (70 + 30 * enabledAnim));
        int indicatorAlpha = clampAlpha((int) ((80 + 120 * enabledAnim) * alphaMultiplier));

        Render2D.rect(indicatorX, indicatorY, 4, 4, new Color(indicatorR, indicatorG, indicatorB, indicatorAlpha).getRGB(), 2);
    }

    private void queueItemIcon(AutoBuyableItem item, float iconX, float iconY, float iconSize, float enabledAnim, float alphaMultiplier) {
        float combinedAlpha = (0.3f + 0.7f * enabledAnim) * alphaMultiplier;

        if (combinedAlpha <= 0.01f) return;

        ItemStack stack = item.createItemStack();
        float scale = (0.85f + 0.15f * enabledAnim);
        float size = iconSize * scale;
        float offsetX = (iconSize - size) / 2f;
        float offsetY = (iconSize - size) / 2f;

        if (ItemRender.isBlockItem(stack)) {
            pendingBlockIcons.add(new PendingBlockIcon(stack, iconX + offsetX, iconY + offsetY, scale, combinedAlpha));
        } else {
            pendingIcons.add(new PendingIcon(stack, iconX + offsetX, iconY + offsetY, scale, combinedAlpha));
        }
    }

    private void renderToggle(float tx, float ty, float tw, float th, float anim, float enabledAnim, float alphaMultiplier) {
        int bgR = clampColor((int) (40 + anim * 40));
        int bgG = clampColor((int) (40 + anim * 110));
        int bgB = clampColor((int) (45 + anim * 30));

        bgR = clampColor((int) (bgR * (0.5f + 0.5f * enabledAnim)));
        bgG = clampColor((int) (bgG * (0.5f + 0.5f * enabledAnim)));
        bgB = clampColor((int) (bgB * (0.5f + 0.5f * enabledAnim)));

        int bgAlpha = clampAlpha((int) (160 * alphaMultiplier));

        Render2D.rect(tx, ty, tw, th, new Color(bgR, bgG, bgB, bgAlpha).getRGB(), th / 2f);

        float knobSize = th - 2f;
        float knobX = tx + 1f + anim * (tw - knobSize - 2f);
        float knobY = ty + 1f;

        int knobBrightness = clampColor((int) (120 + 80 * enabledAnim));
        int knobAlpha = clampAlpha((int) (220 * alphaMultiplier));
        Render2D.rect(knobX, knobY, knobSize, knobSize, new Color(knobBrightness, knobBrightness, knobBrightness, knobAlpha).getRGB(), knobSize / 2f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY, float panelW, float panelH) {
        if (!isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            if (isEditing()) applyEdit();
            return false;
        }

        if (button != 0) {
            if (isEditing()) applyEdit();
            return true;
        }

        float clipY = panelY + 3;
        float clipH = panelH - 6;

        float currentY = panelY + 5 + smoothScroll;
        float itemX = panelX + 5;
        float itemW = panelW - 10;

        List<CategoryItems> categories = getCategorizedItems();

        for (CategoryItems category : categories) {
            if (category.items.isEmpty()) continue;

            currentY += CATEGORY_HEIGHT;

            for (AutoBuyableItem item : category.items) {
                float itemY = currentY;

                boolean inView = (itemY + ITEM_HEIGHT > clipY) && (itemY < clipY + clipH);

                if (inView && isHovered(mouseX, mouseY, itemX, itemY, itemW, ITEM_HEIGHT)) {

                    float toggleW = 14f;
                    float toggleH = 8f;
                    float toggleX = itemX + itemW - toggleW - 4;
                    float toggleY = itemY + ITEM_HEIGHT / 2f - toggleH / 2f;

                    float toggleHitX = toggleX - 15;
                    float toggleHitY = toggleY - 10;
                    float toggleHitW = toggleW + 30;
                    float toggleHitH = toggleH + 20;

                    if (isHovered(mouseX, mouseY, toggleHitX, toggleHitY, toggleHitW, toggleHitH)) {
                        if (isEditing()) applyEdit();
                        ItemRegistry.saveItemState(item);
                        return true;
                    }

                    float priceX = itemX + 20;
                    float priceY = itemY + 11;
                    String priceText = "$" + item.getSettings().getBuyBelow();
                    float priceW = Fonts.BOLD.getWidth(priceText, 4f) + 15;

                    if (isHovered(mouseX, mouseY, priceX - 5, priceY - 5, priceW, 18)) {
                        if (isEditing()) applyEdit();
                        startEditing(item, EditField.PRICE);
                        return true;
                    }

                    if (item.getSettings().isCanHaveQuantity()) {
                        float qtyX = priceX + priceW;
                        String qtyText = "x" + item.getSettings().getMinQuantity();
                        float qtyW = Fonts.BOLD.getWidth(qtyText, 4f) + 15;

                        if (isHovered(mouseX, mouseY, qtyX - 5, priceY - 5, qtyW, 18)) {
                            if (isEditing()) applyEdit();
                            startEditing(item, EditField.QUANTITY);
                            return true;
                        }
                    }

                    if (isEditing()) applyEdit();
                    return true;
                }

                currentY += ITEM_HEIGHT + ITEM_SPACING;
            }

            currentY += 8f;
        }

        if (isEditing()) applyEdit();
        return true;
    }

    private void startEditing(AutoBuyableItem item, EditField field) {
        editingItem = item;
        editingField = field;
        cursorBlink = 0f;

        if (field == EditField.PRICE) {
            inputText = String.valueOf(item.getSettings().getBuyBelow());
        } else if (field == EditField.QUANTITY) {
            inputText = String.valueOf(item.getSettings().getMinQuantity());
        }
    }

    private void applyEdit() {
        if (editingItem == null || editingField == EditField.NONE) return;

        try {
            int value = Integer.parseInt(inputText);

            if (editingField == EditField.PRICE) {
                editingItem.getSettings().setBuyBelow(Math.max(1, value));
            } else if (editingField == EditField.QUANTITY) {
                editingItem.getSettings().setMinQuantity(Math.max(1, Math.min(64, value)));
            }

            ItemRegistry.saveItemSettings(editingItem);

        } catch (NumberFormatException ignored) {}

        editingItem = null;
        editingField = EditField.NONE;
        inputText = "";
    }

    private void cancelEdit() {
        editingItem = null;
        editingField = EditField.NONE;
        inputText = "";
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEditing()) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applyEdit();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelEdit();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputText.isEmpty()) {
            inputText = inputText.substring(0, inputText.length() - 1);
            return true;
        }

        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!isEditing()) return false;

        if (Character.isDigit(chr)) {
            int maxLen = editingField == EditField.PRICE ? 9 : 2;
            if (inputText.length() < maxLen) {
                inputText += chr;
            }
            return true;
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float panelX, float panelY, float panelW, float panelH) {
        if (isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            targetScroll += (float) amount * 25f;
            return true;
        }
        return false;
    }

    public void resetHover() {
        hoveredItem = null;
    }

    public void resetPositions() {
        smoothScroll = targetScroll;
    }

    private List<CategoryItems> getCategorizedItems() {
        List<CategoryItems> categories = new ArrayList<>();
        categories.add(new CategoryItems("Крушитель", ItemRegistry.getKrush()));
        categories.add(new CategoryItems("Талисманы", ItemRegistry.getTalismans()));
        categories.add(new CategoryItems("Сферы", ItemRegistry.getSpheres()));
        categories.add(new CategoryItems("Разное", ItemRegistry.getMisc()));
        categories.add(new CategoryItems("Донаторские", ItemRegistry.getDonator()));
        categories.add(new CategoryItems("Зелья", ItemRegistry.getPotions()));
        return categories;
    }

    private static class CategoryItems {
        String name;
        List<AutoBuyableItem> items;

        CategoryItems(String name, List<AutoBuyableItem> items) {
            this.name = name;
            this.items = items != null ? items : new ArrayList<>();
        }
    }
}