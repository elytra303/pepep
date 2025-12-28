package rich.screens.clickgui.impl.autobuy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.screens.clickgui.impl.autobuy.window.AutoBuyManager;
import rich.screens.clickgui.impl.autobuy.window.AutoBuySettingsManager;
import rich.screens.clickgui.impl.autobuy.window.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.window.ItemRegistry;
import rich.util.math.MathUtils;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;

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
    private final Map<AutoBuyableItem, Float> lastRenderY = new HashMap<>();

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
    private static final float ANIM_SPEED = 10f;

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

        float contentHeight = calculateContentHeight();
        float maxScroll = Math.max(0, contentHeight - height - 6f);
        targetScroll = clamp(targetScroll, -maxScroll, 0);

        float diff = targetScroll - smoothScroll;
        smoothScroll += diff * 0.3f;
        if (Math.abs(diff) < 0.1f) {
            smoothScroll = targetScroll;
        }

        int displayScroll = (int) smoothScroll;

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

        float currentY = y + 5 + displayScroll;
        List<CategoryItems> categories = getCategorizedItems();

        for (CategoryItems category : categories) {
            if (category.items.isEmpty()) continue;

            if (isInView(currentY, CATEGORY_HEIGHT, clipY, clipH)) {
                renderCategoryHeader(x + 5, currentY, width - 10, category.name, alphaMultiplier);
            }
            currentY += CATEGORY_HEIGHT;

            for (AutoBuyableItem item : category.items) {
                lastRenderY.put(item, currentY);

                if (isInView(currentY, ITEM_HEIGHT, clipY, clipH)) {
                    renderItem(context, item, x + 5, currentY, width - 10, mouseX, mouseY, alphaMultiplier);
                }
                currentY += ITEM_HEIGHT + ITEM_SPACING;
            }

            currentY += 8f;
        }

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

            boolean isHovered = item == hoveredItem;
            float targetHover = isHovered ? 1f : 0f;
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
        int bgAlpha = (int) (15 * alphaMultiplier);
        int outlineAlpha = (int) (215 * alphaMultiplier);

        Render2D.rect(x, y, width, height, new Color(64, 64, 64, bgAlpha).getRGB(), 7f);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 7f);
    }

    private void renderCategoryHeader(float catX, float catY, float catWidth, String name, float alphaMultiplier) {
        int textAlpha = (int) (180 * alphaMultiplier);
        float textWidth = Fonts.BOLD.getWidth(name, 5f);
        float lineWidth = (catWidth - textWidth - 16f) / 2f;

        int lineAlpha = (int) (60 * alphaMultiplier);
        Render2D.rect(catX, catY + 6f, lineWidth, 0.5f, new Color(100, 100, 100, lineAlpha).getRGB(), 0);
        Fonts.BOLD.draw(name, catX + lineWidth + 8f, catY + 3f, 5f, new Color(160, 160, 160, textAlpha).getRGB());
        Render2D.rect(catX + lineWidth + textWidth + 16f, catY + 6f, lineWidth, 0.5f, new Color(100, 100, 100, lineAlpha).getRGB(), 0);
    }

    private void renderItem(DrawContext context, AutoBuyableItem item, float itemX, float itemY, float itemW,
                            float mouseX, float mouseY, float alphaMultiplier) {

        if (alphaMultiplier <= 0.01f) return;

        int intItemY = (int) itemY;

        boolean hovered = MathUtils.isHovered(mouseX, mouseY, itemX, intItemY, itemW, ITEM_HEIGHT);
        if (hovered) {
            hoveredItem = item;
        }

        float toggleAnim = toggleAnimations.getOrDefault(item, item.isEnabled() ? 1f : 0f);
        float hoverAnim = hoverAnimations.getOrDefault(item, 0f);

        int bgAlpha = (int) ((25 + hoverAnim * 15) * alphaMultiplier);
        Render2D.rect(itemX, intItemY, itemW, ITEM_HEIGHT, new Color(64 + (int)(hoverAnim * 36), 64 + (int)(hoverAnim * 36), 64 + (int)(hoverAnim * 36), bgAlpha).getRGB(), 5f);

        float iconSize = 16f;
        int iconY = intItemY + (int)((ITEM_HEIGHT - iconSize) / 2f);
        int iconX = (int) itemX + 2;

        int scaledIconX = (int) (iconX * currentScale);
        int scaledIconY = (int) (iconY * currentScale);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(1f / currentScale, 1f / currentScale);
        try {
            context.drawItem(item.createItemStack(), scaledIconX, scaledIconY);
        } catch (Exception ignored) {}
        context.getMatrices().popMatrix();

        String displayName = item.getDisplayName();

        int textAlpha = item.isEnabled() ? (int) (255 * alphaMultiplier) : (int) (200 * alphaMultiplier);
        Color textColor = item.isEnabled()
                ? new Color(255, 255, 255, textAlpha)
                : new Color(180, 180, 180, textAlpha);

        Fonts.BOLD.draw(displayName, itemX + 20, intItemY + 5, 5f, textColor.getRGB());

        boolean isEditingPrice = editingItem == item && editingField == EditField.PRICE;
        String priceText = isEditingPrice ? inputText : "$" + item.getSettings().getBuyBelow();

        if (isEditingPrice) {
            float cursorAlphaVal = (float) (Math.sin(cursorBlink * Math.PI * 2) * 0.5 + 0.5);
            if (cursorAlphaVal > 0.5f) priceText += "|";
        }

        float priceX = itemX + 20;
        int priceY = intItemY + 13;

        int priceAlpha = isEditingPrice ? (int) (220 * alphaMultiplier) : (int) (180 * alphaMultiplier);
        Color priceColor = isEditingPrice
                ? new Color(100, 200, 100, priceAlpha)
                : new Color(140, 140, 140, priceAlpha);

        Fonts.BOLD.draw(priceText, priceX, priceY, 4f, priceColor.getRGB());

        if (item.getSettings().isCanHaveQuantity()) {
            boolean isEditingQty = editingItem == item && editingField == EditField.QUANTITY;
            String qtyText = isEditingQty ? inputText : "x" + item.getSettings().getMinQuantity();

            if (isEditingQty) {
                float cursorAlphaVal = (float) (Math.sin(cursorBlink * Math.PI * 2) * 0.5 + 0.5);
                if (cursorAlphaVal > 0.5f) qtyText += "|";
            }

            float qtyX = priceX + Fonts.BOLD.getWidth(priceText, 4f) + 8;
            int qtyAlpha = isEditingQty ? (int) (220 * alphaMultiplier) : (int) (180 * alphaMultiplier);
            Color qtyColor = isEditingQty
                    ? new Color(100, 200, 100, qtyAlpha)
                    : new Color(140, 140, 140, qtyAlpha);

            Fonts.BOLD.draw(qtyText, qtyX, priceY, 4f, qtyColor.getRGB());
        }

        float toggleW = 14f;
        float toggleH = 8f;
        float toggleX = itemX + itemW - toggleW - 4;
        int toggleY = intItemY + (int)(ITEM_HEIGHT / 2f - toggleH / 2f);
        renderToggle(toggleX, toggleY, toggleW, toggleH, toggleAnim, alphaMultiplier);

        if (item.isEnabled()) {
            float indicatorX = toggleX - 8;
            int indicatorY = intItemY + (int)(ITEM_HEIGHT / 2f) - 2;
            Render2D.rect(indicatorX, indicatorY, 4, 4, new Color(100, 200, 100, (int) (200 * alphaMultiplier)).getRGB(), 2);
        }
    }

    private void renderToggle(float x, float y, float w, float h, float anim, float alphaMultiplier) {
        int bgR = (int) (50 + anim * 30);
        int bgG = (int) (50 + anim * 100);
        int bgB = (int) (55 + anim * 20);
        int bgAlpha = (int) (160 * alphaMultiplier);

        Render2D.rect(x, y, w, h, new Color(bgR, bgG, bgB, bgAlpha).getRGB(), h / 2f);

        float knobSize = h - 2f;
        float knobX = x + 1f + anim * (w - knobSize - 2f);
        float knobY = y + 1f;

        int knobAlpha = (int) (220 * alphaMultiplier);
        Render2D.rect(knobX, knobY, knobSize, knobSize, new Color(200, 200, 200, knobAlpha).getRGB(), knobSize / 2f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY, float panelW, float panelH) {
        if (!MathUtils.isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            if (isEditing()) applyEdit();
            return false;
        }

        float clipY = y + 3;
        float clipH = height - 6;

        List<CategoryItems> categories = getCategorizedItems();

        for (CategoryItems category : categories) {
            if (category.items.isEmpty()) continue;

            for (AutoBuyableItem item : category.items) {
                Float displayItemY = lastRenderY.get(item);
                if (displayItemY == null) continue;

                if (!isInView(displayItemY, ITEM_HEIGHT, clipY, clipH)) continue;

                float itemX = x + 5;
                float itemW = width - 10;

                if (MathUtils.isHovered(mouseX, mouseY, itemX, displayItemY, itemW, ITEM_HEIGHT)) {
                    if (button == 0) {
                        float toggleW = 14f;
                        float toggleH = 8f;
                        float toggleX = itemX + itemW - toggleW - 4;
                        float toggleY = displayItemY + ITEM_HEIGHT / 2f - toggleH / 2f;

                        if (MathUtils.isHovered(mouseX, mouseY, toggleX - 4, toggleY - 4, toggleW + 8, toggleH + 8)) {
                            if (isEditing()) applyEdit();
                            item.setEnabled(!item.isEnabled());
                            return true;
                        }

                        float priceX = itemX + 20;
                        float priceY = displayItemY + 11;
                        String priceText = "$" + item.getSettings().getBuyBelow();
                        float priceW = Fonts.BOLD.getWidth(priceText, 4f) + 6;

                        if (MathUtils.isHovered(mouseX, mouseY, priceX - 2, priceY - 2, priceW, 10)) {
                            if (isEditing()) applyEdit();
                            startEditing(item, EditField.PRICE);
                            return true;
                        }

                        if (item.getSettings().isCanHaveQuantity()) {
                            float qtyX = priceX + priceW + 2;
                            String qtyText = "x" + item.getSettings().getMinQuantity();
                            float qtyW = Fonts.BOLD.getWidth(qtyText, 4f) + 6;

                            if (MathUtils.isHovered(mouseX, mouseY, qtyX - 2, priceY - 2, qtyW, 10)) {
                                if (isEditing()) applyEdit();
                                startEditing(item, EditField.QUANTITY);
                                return true;
                            }
                        }

                        if (isEditing()) applyEdit();
                        return true;
                    }
                }
            }
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

            AutoBuySettingsManager.getInstance().saveSettings(editingItem.getSettings().getItemName(), editingItem.getSettings());
            ItemRegistry.reloadSettings();

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
        if (MathUtils.isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            targetScroll += (float) amount * 25f;
            return true;
        }
        return false;
    }

    public void resetHover() {
        hoveredItem = null;
    }

    public void resetPositions() {
        lastRenderY.clear();
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