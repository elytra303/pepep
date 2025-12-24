package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.setting.implement.ColorSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting colorSetting;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private boolean draggingHue = false;
    private boolean draggingSaturation = false;
    private boolean draggingBrightness = false;
    private boolean draggingAlpha = false;

    private static final int SLIDER_HEIGHT = 12;
    private static final int SPACING = 3;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.colorSetting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        expandAnimation += (expanded ? 1f : 0f - expandAnimation) * 0.25f;
        expandAnimation = Math.max(0f, Math.min(1f, expandAnimation));

        Render2D.rect(x, y, width, height, new Color(255, 255, 255, hovered ? 25 : 20).getRGB(), 6f);

        if (hovered) {
            Render2D.outline(x, y, width, height, 0.6f, new Color(255, 255, 255, 30).getRGB(), 6f);
        }

        Fonts.BOLD.draw(colorSetting.getName(), x + 6, y + height / 2 - 3.5f, 7, new Color(210, 210, 220, 200).getRGB());

        float previewSize = 11;
        float previewX = x + width - previewSize - 20;
        float previewY = y + height / 2 - previewSize / 2;

        int colorValue = colorSetting.getColor();
        Color previewColor = new Color(colorValue, true);

        Render2D.rect(previewX, previewY, previewSize, previewSize, previewColor.getRGB(), 3f);
        Render2D.outline(previewX, previewY, previewSize, previewSize, 0.8f, new Color(previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue(), 180).getRGB(), 3f);

        String arrow = expanded ? "▼" : "▶";
        float arrowWidth = Fonts.BOLD.getWidth(arrow, 7);
        Fonts.BOLD.draw(arrow, x + width - arrowWidth - 6, y + height / 2 - 3.5f, 7, new Color(200, 200, 210, 180).getRGB());

        if (expandAnimation > 0.01f) {
            renderColorPicker(context, mouseX, mouseY);
        }
    }

    private void renderColorPicker(DrawContext context, int mouseX, int mouseY) {
        float pickerX = x;
        float pickerY = y + height + (SPACING * expandAnimation);
        float pickerWidth = width * expandAnimation;

        renderHueSlider(context, pickerX, pickerY, pickerWidth, mouseX, mouseY);
        pickerY += (SLIDER_HEIGHT + SPACING) * expandAnimation;

        renderSaturationSlider(context, pickerX, pickerY, pickerWidth, mouseX, mouseY);
        pickerY += (SLIDER_HEIGHT + SPACING) * expandAnimation;

        renderBrightnessSlider(context, pickerX, pickerY, pickerWidth, mouseX, mouseY);
        pickerY += (SLIDER_HEIGHT + SPACING) * expandAnimation;

        renderAlphaSlider(context, pickerX, pickerY, pickerWidth, mouseX, mouseY);
    }

    private void renderHueSlider(DrawContext context, float sliderX, float sliderY, float sliderWidth, int mouseX, int mouseY) {
        float actualHeight = SLIDER_HEIGHT * expandAnimation;

        for (int i = 0; i < sliderWidth; i++) {
            float hue = i / sliderWidth;
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            Color gradColor = new Color(color);
            Render2D.rect(sliderX + i, sliderY, 1, actualHeight, gradColor.getRGB(), 0f);
        }

        Render2D.rect(sliderX, sliderY, sliderWidth, actualHeight, new Color(255, 255, 255, (int)(10 * expandAnimation)).getRGB(), 4f);
        Render2D.outline(sliderX, sliderY, sliderWidth, actualHeight, 0.6f, new Color(255, 255, 255, (int)(60 * expandAnimation)).getRGB(), 4f);

        float huePos = colorSetting.getHue() * sliderWidth;
        float handleSize = 4f;
        int handleAlpha = Math.max(0, Math.min(255, (int)(240 * expandAnimation)));

        Render2D.rect(sliderX + huePos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
        Render2D.outline(sliderX + huePos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, 1.0f, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
    }

    private void renderSaturationSlider(DrawContext context, float sliderX, float sliderY, float sliderWidth, int mouseX, int mouseY) {
        float actualHeight = SLIDER_HEIGHT * expandAnimation;

        for (int i = 0; i < sliderWidth; i++) {
            float saturation = i / sliderWidth;
            int color = Color.HSBtoRGB(colorSetting.getHue(), saturation, colorSetting.getBrightness());
            Color gradColor = new Color(color);
            Render2D.rect(sliderX + i, sliderY, 1, actualHeight, gradColor.getRGB(), 0f);
        }

        Render2D.rect(sliderX, sliderY, sliderWidth, actualHeight, new Color(255, 255, 255, (int)(10 * expandAnimation)).getRGB(), 4f);
        Render2D.outline(sliderX, sliderY, sliderWidth, actualHeight, 0.6f, new Color(255, 255, 255, (int)(60 * expandAnimation)).getRGB(), 4f);

        float satPos = colorSetting.getSaturation() * sliderWidth;
        float handleSize = 4f;
        int handleAlpha = Math.max(0, Math.min(255, (int)(240 * expandAnimation)));

        Render2D.rect(sliderX + satPos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
        Render2D.outline(sliderX + satPos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, 1.0f, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
    }

    private void renderBrightnessSlider(DrawContext context, float sliderX, float sliderY, float sliderWidth, int mouseX, int mouseY) {
        float actualHeight = SLIDER_HEIGHT * expandAnimation;

        for (int i = 0; i < sliderWidth; i++) {
            float brightness = i / sliderWidth;
            int color = Color.HSBtoRGB(colorSetting.getHue(), colorSetting.getSaturation(), brightness);
            Color gradColor = new Color(color);
            Render2D.rect(sliderX + i, sliderY, 1, actualHeight, gradColor.getRGB(), 0f);
        }

        Render2D.rect(sliderX, sliderY, sliderWidth, actualHeight, new Color(255, 255, 255, (int)(10 * expandAnimation)).getRGB(), 4f);
        Render2D.outline(sliderX, sliderY, sliderWidth, actualHeight, 0.6f, new Color(255, 255, 255, (int)(60 * expandAnimation)).getRGB(), 4f);

        float brightPos = colorSetting.getBrightness() * sliderWidth;
        float handleSize = 4f;
        int handleAlpha = Math.max(0, Math.min(255, (int)(240 * expandAnimation)));

        Render2D.rect(sliderX + brightPos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
        Render2D.outline(sliderX + brightPos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, 1.0f, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
    }

    private void renderAlphaSlider(DrawContext context, float sliderX, float sliderY, float sliderWidth, int mouseX, int mouseY) {
        float actualHeight = SLIDER_HEIGHT * expandAnimation;

        for (int i = 0; i < sliderWidth; i++) {
            float alpha = i / sliderWidth;
            int baseColor = colorSetting.getColorWithAlpha() & 0x00FFFFFF;
            int color = baseColor | (Math.round(alpha * 255) << 24);
            Color gradColor = new Color(color, true);
            Render2D.rect(sliderX + i, sliderY, 1, actualHeight, gradColor.getRGB(), 0f);
        }

        Render2D.rect(sliderX, sliderY, sliderWidth, actualHeight, new Color(255, 255, 255, (int)(10 * expandAnimation)).getRGB(), 4f);
        Render2D.outline(sliderX, sliderY, sliderWidth, actualHeight, 0.6f, new Color(255, 255, 255, (int)(60 * expandAnimation)).getRGB(), 4f);

        float alphaPos = colorSetting.getAlpha() * sliderWidth;
        float handleSize = 4f;
        int handleAlpha = Math.max(0, Math.min(255, (int)(240 * expandAnimation)));

        Render2D.rect(sliderX + alphaPos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
        Render2D.outline(sliderX + alphaPos - handleSize / 2, sliderY - 1, handleSize, actualHeight + 2, 1.0f, new Color(255, 255, 255, handleAlpha).getRGB(), 2f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHover(mouseX, mouseY)) {
                expanded = !expanded;
                return true;
            }

            if (expanded && expandAnimation > 0.8f) {
                float pickerX = x;
                float pickerY = y + height + SPACING;
                float pickerWidth = width;

                if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth) {
                    if (mouseY >= pickerY && mouseY <= pickerY + SLIDER_HEIGHT) {
                        draggingHue = true;
                        updateHue(mouseX, pickerX, pickerWidth);
                        return true;
                    }

                    pickerY += SLIDER_HEIGHT + SPACING;
                    if (mouseY >= pickerY && mouseY <= pickerY + SLIDER_HEIGHT) {
                        draggingSaturation = true;
                        updateSaturation(mouseX, pickerX, pickerWidth);
                        return true;
                    }

                    pickerY += SLIDER_HEIGHT + SPACING;
                    if (mouseY >= pickerY && mouseY <= pickerY + SLIDER_HEIGHT) {
                        draggingBrightness = true;
                        updateBrightness(mouseX, pickerX, pickerWidth);
                        return true;
                    }

                    pickerY += SLIDER_HEIGHT + SPACING;
                    if (mouseY >= pickerY && mouseY <= pickerY + SLIDER_HEIGHT) {
                        draggingAlpha = true;
                        updateAlpha(mouseX, pickerX, pickerWidth);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingHue = false;
            draggingSaturation = false;
            draggingBrightness = false;
            draggingAlpha = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            float pickerX = x;
            float pickerWidth = width;

            if (draggingHue) {
                updateHue(mouseX, pickerX, pickerWidth);
                return true;
            }
            if (draggingSaturation) {
                updateSaturation(mouseX, pickerX, pickerWidth);
                return true;
            }
            if (draggingBrightness) {
                updateBrightness(mouseX, pickerX, pickerWidth);
                return true;
            }
            if (draggingAlpha) {
                updateAlpha(mouseX, pickerX, pickerWidth);
                return true;
            }
        }
        return false;
    }

    private void updateHue(double mouseX, float pickerX, float pickerWidth) {
        float percentage = (float)((mouseX - pickerX) / pickerWidth);
        percentage = Math.max(0, Math.min(1, percentage));
        colorSetting.setHue(percentage);
    }

    private void updateSaturation(double mouseX, float pickerX, float pickerWidth) {
        float percentage = (float)((mouseX - pickerX) / pickerWidth);
        percentage = Math.max(0, Math.min(1, percentage));
        colorSetting.setSaturation(percentage);
    }

    private void updateBrightness(double mouseX, float pickerX, float pickerWidth) {
        float percentage = (float)((mouseX - pickerX) / pickerWidth);
        percentage = Math.max(0, Math.min(1, percentage));
        colorSetting.setBrightness(percentage);
    }

    private void updateAlpha(double mouseX, float pickerX, float pickerWidth) {
        float percentage = (float)((mouseX - pickerX) / pickerWidth);
        percentage = Math.max(0, Math.min(1, percentage));
        colorSetting.setAlpha(percentage);
    }

    @Override
    public void tick() {
        expandAnimation += (expanded ? 1f : 0f - expandAnimation) * 0.25f;
        expandAnimation = Math.max(0f, Math.min(1f, expandAnimation));
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getTotalHeight() {
        return height + ((SLIDER_HEIGHT * 4 + SPACING * 3) * expandAnimation);
    }
}