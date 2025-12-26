package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.modules.module.setting.implement.ColorSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting colorSetting;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private float hoverAnimation = 0f;
    private float previewHoverAnimation = 0f;
    private float contentAlpha = 0f;

    private boolean draggingPalette = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    private float paletteHandleAnimation = 0f;
    private float hueHandleAnimation = 0f;
    private float alphaHandleAnimation = 0f;

    private boolean hexInputActive = false;
    private String hexInputText = "";
    private int hexCursorPosition = 0;
    private float hexInputAnimation = 0f;

    private float displayHue;
    private float displaySaturation;
    private float displayBrightness;
    private float displayAlpha;
    private boolean colorInitialized = false;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float FAST_ANIMATION_SPEED = 12f;
    private static final float COLOR_TRANSITION_SPEED = 6f;
    private static final float CONTENT_FADE_SPEED = 10f;
    private static final float PALETTE_SIZE = 70f;
    private static final float SLIDER_WIDTH = 8f;
    private static final float SPACING = 4f;
    private static final float PREVIEW_SIZE = 12f;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.colorSetting = setting;
        updateHexFromColor();

        displayHue = setting.getHue();
        displaySaturation = setting.getSaturation();
        displayBrightness = setting.getBrightness();
        displayAlpha = setting.getAlpha();
        colorInitialized = true;
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    private float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            return target;
        }
        return current + diff * Math.min(speed, 1f);
    }

    private float lerpHue(float current, float target, float speed) {
        float diff = target - current;

        if (diff > 0.5f) {
            diff -= 1f;
        } else if (diff < -0.5f) {
            diff += 1f;
        }

        if (Math.abs(diff) < 0.001f) {
            return target;
        }

        float result = current + diff * Math.min(speed, 1f);

        if (result < 0f) result += 1f;
        if (result > 1f) result -= 1f;

        return result;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private void updateDisplayColors(float deltaTime) {
        if (!colorInitialized) {
            displayHue = colorSetting.getHue();
            displaySaturation = colorSetting.getSaturation();
            displayBrightness = colorSetting.getBrightness();
            displayAlpha = colorSetting.getAlpha();
            colorInitialized = true;
            return;
        }

        float speed = deltaTime * COLOR_TRANSITION_SPEED;

        if (draggingPalette || draggingHue || draggingAlpha) {
            displayHue = colorSetting.getHue();
            displaySaturation = colorSetting.getSaturation();
            displayBrightness = colorSetting.getBrightness();
            displayAlpha = colorSetting.getAlpha();
        } else {
            displayHue = lerpHue(displayHue, colorSetting.getHue(), speed);
            displaySaturation = lerp(displaySaturation, colorSetting.getSaturation(), speed);
            displayBrightness = lerp(displayBrightness, colorSetting.getBrightness(), speed);
            displayAlpha = lerp(displayAlpha, colorSetting.getAlpha(), speed);
        }
    }

    private int getDisplayColor() {
        int rgb = Color.HSBtoRGB(displayHue, displaySaturation, displayBrightness);
        int alphaInt = Math.round(displayAlpha * 255);
        return (alphaInt << 24) | (rgb & 0x00FFFFFF);
    }

    private int getDisplayColorNoAlpha() {
        return Color.HSBtoRGB(displayHue, displaySaturation, displayBrightness) | 0xFF000000;
    }

    private Color applyContentAlpha(Color color) {
        int newAlpha = Math.max(0, Math.min(255, (int)(color.getAlpha() * alphaMultiplier * contentAlpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }

    private int applyContentAlpha(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, (int)(a * alphaMultiplier * contentAlpha)));
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaTime = getDeltaTime();

        updateDisplayColors(deltaTime);

        if (draggingPalette) {
            updatePalette(mouseX, mouseY);
        }
        if (draggingHue) {
            updateHue(mouseY);
        }
        if (draggingAlpha) {
            updateAlpha(mouseY);
        }

        boolean hovered = isHover(mouseX, mouseY);
        boolean previewHovered = isPreviewHover(mouseX, mouseY);

        hoverAnimation = lerp(hoverAnimation, hovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        previewHoverAnimation = lerp(previewHoverAnimation, previewHovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        expandAnimation = lerp(expandAnimation, expanded ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        hexInputAnimation = lerp(hexInputAnimation, hexInputActive ? 1f : 0f, deltaTime * FAST_ANIMATION_SPEED);

        float contentAlphaTarget = expanded ? 1f : 0f;
        float contentAlphaSpeed = expanded ? CONTENT_FADE_SPEED : CONTENT_FADE_SPEED * 1.5f;
        contentAlpha = lerp(contentAlpha, contentAlphaTarget, deltaTime * contentAlphaSpeed);

        paletteHandleAnimation = lerp(paletteHandleAnimation, draggingPalette ? 1f : 0f, deltaTime * FAST_ANIMATION_SPEED);
        hueHandleAnimation = lerp(hueHandleAnimation, draggingHue ? 1f : 0f, deltaTime * FAST_ANIMATION_SPEED);
        alphaHandleAnimation = lerp(alphaHandleAnimation, draggingAlpha ? 1f : 0f, deltaTime * FAST_ANIMATION_SPEED);

        Fonts.BOLD.draw(colorSetting.getName(), x + 0.5f, y + height / 2 - 7.5f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        String description = colorSetting.getDescription();
        if (description != null && !description.isEmpty()) {
            Fonts.BOLD.draw(description, x + 0.5f, y + height / 2 + 0.5f, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
        }

        renderColorPreview(mouseX, mouseY);

        if (expandAnimation > 0.01f) {
            renderColorPicker(context, mouseX, mouseY, deltaTime);
        }
    }

    private void renderColorPreview(int mouseX, int mouseY) {
        float previewX = x + width - 14;
        float previewY = y + height / 2 / 2;

        float scale = 1f + previewHoverAnimation * 0.1f;
        float scaledX = previewX - scale / 2 + 1;
        float scaledY = previewY - scale / 2;

        int colorValue = getDisplayColor();
        Color previewColor = new Color(colorValue, true);
        Render2D.rect(scaledX + 0.5f, scaledY + 0.5f, 9, 9, applyAlpha(previewColor).getRGB(), 15);
        int outlineAlpha = clamp((int)((255 + previewHoverAnimation * 60) * alphaMultiplier));

        Render2D.outline(scaledX, scaledY, 10, 10, 1f, new Color(125, 125, 125, outlineAlpha).getRGB(), 15);
    }

    private void renderColorPicker(DrawContext context, int mouseX, int mouseY, float deltaTime) {
        float pickerX = x;
        float pickerY = y + height + SPACING;
        float pickerWidth = width;

        float totalExpandedHeight = PALETTE_SIZE + SPACING + 18 + SPACING;
        float visibleHeight = totalExpandedHeight * expandAnimation;

        int bgAlpha = clamp((int)(220 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(pickerX, pickerY, pickerWidth, visibleHeight + 2, new Color(25, 25, 25, bgAlpha).getRGB(), 4f);

        int outlineAlpha = clamp((int)(60 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.outline(pickerX, pickerY, pickerWidth, visibleHeight + 2, 0.5f,
                new Color(80, 80, 85, outlineAlpha).getRGB(), 4f);

        if (expandAnimation < 0.3f || contentAlpha < 0.01f) return;

        Scissor.enable(pickerX, pickerY, pickerWidth, visibleHeight);

        float contentX = pickerX + SPACING;
        float contentY = pickerY + SPACING;
        float contentWidth = pickerWidth - SPACING * 2;

        float slidersWidth = SLIDER_WIDTH * 2 + SPACING;
        float paletteWidth = contentWidth - slidersWidth - SPACING;

        renderHueSlider(contentX, contentY, SLIDER_WIDTH, PALETTE_SIZE, mouseX, mouseY);
        renderAlphaSlider(contentX + SLIDER_WIDTH + SPACING, contentY, SLIDER_WIDTH, PALETTE_SIZE, mouseX, mouseY);
        renderSaturationBrightnessPalette(contentX + slidersWidth + SPACING, contentY, paletteWidth, PALETTE_SIZE, mouseX, mouseY);

        contentY += PALETTE_SIZE + SPACING;
        renderHexInput(contentX, contentY, contentWidth, 16, mouseX, mouseY);

        Scissor.disable();
    }

    private void renderSaturationBrightnessPalette(float paletteX, float paletteY, float paletteWidth, float paletteHeight, int mouseX, int mouseY) {
        int pureColor = Color.HSBtoRGB(displayHue, 1f, 1f);
        Color pure = new Color(pureColor);

        int[] gradientColors = {
                applyContentAlpha(Color.WHITE).getRGB(),
                applyContentAlpha(pure).getRGB(),
                applyContentAlpha(pure).getRGB(),
                applyContentAlpha(Color.WHITE).getRGB()
        };
        Render2D.gradientRect(paletteX, paletteY, paletteWidth, paletteHeight, gradientColors, 4f);

        int[] blackGradient = {
                new Color(0, 0, 0, 0).getRGB(),
                new Color(0, 0, 0, 0).getRGB(),
                applyContentAlpha(Color.BLACK).getRGB(),
                applyContentAlpha(Color.BLACK).getRGB()
        };

        Render2D.gradientRect(paletteX, paletteY, paletteWidth, paletteHeight, blackGradient, 3f);

        float handleX = paletteX + displaySaturation * paletteWidth;
        float handleY = paletteY + (1f - displayBrightness) * paletteHeight;
        float handleSize = 6f + paletteHandleAnimation * 2f;

        int handleOutlineAlpha = clamp((int)(255 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(handleX - handleSize / 2, handleY - handleSize / 2, handleSize, handleSize,
                new Color(255, 255, 255, handleOutlineAlpha).getRGB(), handleSize / 2);

        int currentColor = Color.HSBtoRGB(displayHue, displaySaturation, displayBrightness);
        Color handleColor = new Color(currentColor);
        Render2D.rect(handleX - handleSize / 2 + 1, handleY - handleSize / 2 + 1, handleSize - 2, handleSize - 2,
                applyContentAlpha(handleColor).getRGB(), (handleSize - 2) / 2);
    }

    private void renderHueSlider(float sliderX, float sliderY, float sliderWidth, float sliderHeight, int mouseX, int mouseY) {
        int[] hueColors = {
                Color.HSBtoRGB(0f, 1f, 1f),
                Color.HSBtoRGB(1f/6f, 1f, 1f),
                Color.HSBtoRGB(2f/6f, 1f, 1f),
                Color.HSBtoRGB(3f/6f, 1f, 1f),
                Color.HSBtoRGB(4f/6f, 1f, 1f),
                Color.HSBtoRGB(5f/6f, 1f, 1f),
                Color.HSBtoRGB(1f, 1f, 1f)
        };

        float segmentHeight = sliderHeight / 6f;

        int[] colorsTop = { applyContentAlpha(new Color(hueColors[0])).getRGB(), applyContentAlpha(new Color(hueColors[0])).getRGB(), applyContentAlpha(new Color(hueColors[1])).getRGB(), applyContentAlpha(new Color(hueColors[1])).getRGB() };
        Render2D.gradientRect(sliderX, sliderY, sliderWidth, segmentHeight, colorsTop, 2f, 2f, 0f, 0f);

        for (int i = 1; i < 5; i++) {
            float segY = sliderY + i * segmentHeight;
            int[] colors = { applyContentAlpha(new Color(hueColors[i])).getRGB(), applyContentAlpha(new Color(hueColors[i])).getRGB(), applyContentAlpha(new Color(hueColors[i + 1])).getRGB(), applyContentAlpha(new Color(hueColors[i + 1])).getRGB() };
            Render2D.gradientRect(sliderX, segY, sliderWidth, segmentHeight + 0.5f, colors, 0f);
        }

        int[] colorsBottom = { applyContentAlpha(new Color(hueColors[5])).getRGB(), applyContentAlpha(new Color(hueColors[5])).getRGB(), applyContentAlpha(new Color(hueColors[6])).getRGB(), applyContentAlpha(new Color(hueColors[6])).getRGB() };
        Render2D.gradientRect(sliderX, sliderY + 5 * segmentHeight, sliderWidth, segmentHeight, colorsBottom, 0f, 0f, 2f, 2f);

        int hueOutlineAlpha = clamp((int)(80 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.outline(sliderX, sliderY, sliderWidth, sliderHeight, 0.5f,
                new Color(100, 100, 105, hueOutlineAlpha).getRGB(), 3f);

        float handleY = sliderY + displayHue * sliderHeight;
        float handleHeight = 3f + hueHandleAnimation * 1f;
        float handleWidth = sliderWidth + 2f;

        int handleAlpha = clamp((int)(255 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(sliderX - 1, handleY - handleHeight / 2, handleWidth, handleHeight,
                new Color(255, 255, 255, handleAlpha).getRGB(), 1.5f);
        int handleShadowAlpha = clamp((int)(100 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.outline(sliderX - 1, handleY - handleHeight / 2, handleWidth, handleHeight, 0.5f,
                new Color(0, 0, 0, handleShadowAlpha).getRGB(), 1.5f);
    }

    private void renderAlphaSlider(float sliderX, float sliderY, float sliderWidth, float sliderHeight, int mouseX, int mouseY) {
        int checkAlpha = clamp((int)(150 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(sliderX, sliderY, sliderWidth, sliderHeight, new Color(180, 180, 180, checkAlpha).getRGB(), 2f);

        int baseColor = getDisplayColorNoAlpha() & 0x00FFFFFF;

        int transparentColor = baseColor;
        int opaqueColor = baseColor | 0xFF000000;

        int[] alphaGradient = {
                applyContentAlpha(new Color(transparentColor, true), 0f).getRGB(),
                applyContentAlpha(new Color(transparentColor, true), 0f).getRGB(),
                applyContentAlpha(new Color(opaqueColor, true)).getRGB(),
                applyContentAlpha(new Color(opaqueColor, true)).getRGB()
        };
        Render2D.gradientRect(sliderX, sliderY, sliderWidth, sliderHeight, alphaGradient, 2f);

        int alphaOutlineAlpha = clamp((int)(80 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.outline(sliderX, sliderY, sliderWidth, sliderHeight, 0.5f,
                new Color(100, 100, 105, alphaOutlineAlpha).getRGB(), 3f);

        float handleY = sliderY + displayAlpha * sliderHeight;
        float handleHeight = 3f + alphaHandleAnimation * 1f;
        float handleWidth = sliderWidth + 2f;

        int handleAlpha = clamp((int)(255 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(sliderX - 1, handleY - handleHeight / 2, handleWidth, handleHeight,
                new Color(255, 255, 255, handleAlpha).getRGB(), 1.5f);
        int handleShadowAlpha = clamp((int)(100 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.outline(sliderX - 1, handleY - handleHeight / 2, handleWidth, handleHeight, 0.5f,
                new Color(0, 0, 0, handleShadowAlpha).getRGB(), 1.5f);
    }

    private Color applyContentAlpha(Color color, float extraAlpha) {
        int newAlpha = Math.max(0, Math.min(255, (int)(color.getAlpha() * alphaMultiplier * contentAlpha * extraAlpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }

    private void renderHexInput(float inputX, float inputY, float inputWidth, float inputHeight, int mouseX, int mouseY) {
        boolean inputHovered = mouseX >= inputX && mouseX <= inputX + inputWidth &&
                mouseY >= inputY && mouseY <= inputY + inputHeight;

        int bgAlpha = clamp((int)((40 + hexInputAnimation * 20 + (inputHovered ? 10 : 0)) * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(inputX, inputY, inputWidth, inputHeight, new Color(35, 35, 40, bgAlpha).getRGB(), 3f);

        int hexOutlineAlpha = clamp((int)((60 + hexInputAnimation * 80 + (inputHovered ? 20 : 0)) * expandAnimation * contentAlpha * alphaMultiplier));
        Color outlineColor = hexInputActive
                ? new Color(100, 140, 180, hexOutlineAlpha)
                : new Color(80, 80, 85, hexOutlineAlpha);
        Render2D.outline(inputX, inputY, inputWidth, inputHeight, 0.5f, outlineColor.getRGB(), 3f);

        String label = "HEX: ";
        float labelWidth = Fonts.BOLD.getWidth(label, 5);
        int labelAlpha = clamp((int)(150 * expandAnimation * contentAlpha * alphaMultiplier));
        Fonts.BOLD.draw(label, inputX + 4, inputY + inputHeight / 2 - 2.5f, 5,
                new Color(140, 140, 150, labelAlpha).getRGB());

        String displayText = hexInputActive ? hexInputText : getDisplayHexString();
        int textAlpha = clamp((int)((180 + hexInputAnimation * 40) * expandAnimation * contentAlpha * alphaMultiplier));
        Fonts.BOLD.draw("#" + displayText, inputX + 4 + labelWidth, inputY + inputHeight / 2 - 2.5f, 5,
                new Color(210, 210, 220, textAlpha).getRGB());

        if (hexInputActive) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime % 1000) < 500) {
                String beforeCursor = hexInputText.substring(0, hexCursorPosition);
                float cursorX = inputX + 4 + labelWidth + Fonts.BOLD.getWidth("#" + beforeCursor, 5);
                int cursorAlpha = clamp((int)(255 * hexInputAnimation * expandAnimation * contentAlpha * alphaMultiplier));
                Render2D.rect(cursorX, inputY + 3, 0.5f, inputHeight - 6,
                        new Color(180, 180, 185, cursorAlpha).getRGB(), 0f);
            }
        }

        float miniPreviewX = inputX + inputWidth - 15;
        float miniPreviewY = inputY + 3;
        float miniPreviewSize = inputHeight - 6;

        int miniCheckAlpha = clamp((int)(120 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.rect(miniPreviewX, miniPreviewY, miniPreviewSize, miniPreviewSize,
                new Color(150, 150, 150, miniCheckAlpha).getRGB(), 3f);
        Render2D.rect(miniPreviewX, miniPreviewY, miniPreviewSize, miniPreviewSize,
                applyContentAlpha(new Color(getDisplayColor(), true)).getRGB(), 3f);
        int miniOutlineAlpha = clamp((int)(80 * expandAnimation * contentAlpha * alphaMultiplier));
        Render2D.outline(miniPreviewX, miniPreviewY, miniPreviewSize, miniPreviewSize, 0.5f,
                new Color(80, 80, 85, miniOutlineAlpha).getRGB(), 3f);
    }

    private String getDisplayHexString() {
        int color = getDisplayColor();
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return String.format("%02X%02X%02X%02X", r, g, b, a);
    }

    private boolean isPreviewHover(double mouseX, double mouseY) {
        float previewX = x + width - PREVIEW_SIZE - 4;
        float previewY = y + height / 2 - PREVIEW_SIZE / 2;
        return mouseX >= previewX && mouseX <= previewX + PREVIEW_SIZE &&
                mouseY >= previewY && mouseY <= previewY + PREVIEW_SIZE;
    }

    private boolean isPaletteHover(double mouseX, double mouseY) {
        float pickerX = x;
        float pickerY = y + height + SPACING;
        float contentX = pickerX + SPACING;
        float contentY = pickerY + SPACING;
        float contentWidth = width - SPACING * 2;
        float slidersWidth = SLIDER_WIDTH * 2 + SPACING;
        float paletteWidth = contentWidth - slidersWidth - SPACING;
        float paletteX = contentX + slidersWidth + SPACING;
        return mouseX >= paletteX && mouseX <= paletteX + paletteWidth &&
                mouseY >= contentY && mouseY <= contentY + PALETTE_SIZE;
    }

    private boolean isHueSliderHover(double mouseX, double mouseY) {
        float pickerX = x;
        float pickerY = y + height + SPACING;
        float contentX = pickerX + SPACING;
        float contentY = pickerY + SPACING;
        return mouseX >= contentX && mouseX <= contentX + SLIDER_WIDTH &&
                mouseY >= contentY && mouseY <= contentY + PALETTE_SIZE;
    }

    private boolean isAlphaSliderHover(double mouseX, double mouseY) {
        float pickerX = x;
        float pickerY = y + height + SPACING;
        float contentX = pickerX + SPACING;
        float contentY = pickerY + SPACING;
        float alphaSliderX = contentX + SLIDER_WIDTH + SPACING;
        return mouseX >= alphaSliderX && mouseX <= alphaSliderX + SLIDER_WIDTH &&
                mouseY >= contentY && mouseY <= contentY + PALETTE_SIZE;
    }

    private boolean isHexInputHover(double mouseX, double mouseY) {
        float pickerX = x;
        float pickerY = y + height + SPACING;
        float contentX = pickerX + SPACING;
        float contentY = pickerY + SPACING + PALETTE_SIZE + SPACING;
        float contentWidth = width - SPACING * 2;
        return mouseX >= contentX && mouseX <= contentX + contentWidth &&
                mouseY >= contentY && mouseY <= contentY + 16;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isPreviewHover(mouseX, mouseY)) {
                expanded = !expanded;
                if (!expanded) {
                    hexInputActive = false;
                    draggingPalette = false;
                    draggingHue = false;
                    draggingAlpha = false;
                }
                return true;
            }

            if (expanded && expandAnimation > 0.8f && contentAlpha > 0.5f) {
                if (isPaletteHover(mouseX, mouseY)) {
                    draggingPalette = true;
                    updatePalette(mouseX, mouseY);
                    hexInputActive = false;
                    return true;
                }

                if (isHueSliderHover(mouseX, mouseY)) {
                    draggingHue = true;
                    updateHue(mouseY);
                    hexInputActive = false;
                    return true;
                }

                if (isAlphaSliderHover(mouseX, mouseY)) {
                    draggingAlpha = true;
                    updateAlpha(mouseY);
                    hexInputActive = false;
                    return true;
                }

                if (isHexInputHover(mouseX, mouseY)) {
                    hexInputActive = true;
                    hexInputText = getHexString();
                    hexCursorPosition = hexInputText.length();
                    return true;
                } else if (hexInputActive) {
                    applyHexInput();
                    hexInputActive = false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasDragging = draggingPalette || draggingHue || draggingAlpha;
            draggingPalette = false;
            draggingHue = false;
            draggingAlpha = false;
            if (wasDragging) {
                updateHexFromColor();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            if (draggingPalette) {
                updatePalette(mouseX, mouseY);
                return true;
            }
            if (draggingHue) {
                updateHue(mouseY);
                return true;
            }
            if (draggingAlpha) {
                updateAlpha(mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!hexInputActive) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                applyHexInput();
                hexInputActive = false;
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                hexInputActive = false;
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hexCursorPosition > 0) {
                    hexInputText = hexInputText.substring(0, hexCursorPosition - 1) + hexInputText.substring(hexCursorPosition);
                    hexCursorPosition--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hexCursorPosition < hexInputText.length()) {
                    hexInputText = hexInputText.substring(0, hexCursorPosition) + hexInputText.substring(hexCursorPosition + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (hexCursorPosition > 0) hexCursorPosition--;
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (hexCursorPosition < hexInputText.length()) hexCursorPosition++;
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                hexCursorPosition = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                hexCursorPosition = hexInputText.length();
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                    if (clipboard != null) {
                        clipboard = clipboard.replaceAll("[^0-9A-Fa-f]", "");
                        if (hexInputText.length() + clipboard.length() <= 8) {
                            hexInputText = hexInputText.substring(0, hexCursorPosition) + clipboard + hexInputText.substring(hexCursorPosition);
                            hexCursorPosition += clipboard.length();
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!hexInputActive) return false;

        if (isHexChar(chr) && hexInputText.length() < 8) {
            hexInputText = hexInputText.substring(0, hexCursorPosition) + Character.toUpperCase(chr) + hexInputText.substring(hexCursorPosition);
            hexCursorPosition++;
            return true;
        }

        return false;
    }

    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private void updatePalette(double mouseX, double mouseY) {
        float pickerX = x;
        float pickerY = y + height + SPACING;
        float contentX = pickerX + SPACING;
        float contentY = pickerY + SPACING;
        float contentWidth = width - SPACING * 2;
        float slidersWidth = SLIDER_WIDTH * 2 + SPACING;
        float paletteWidth = contentWidth - slidersWidth - SPACING;
        float paletteX = contentX + slidersWidth + SPACING;

        float saturation = (float)((mouseX - paletteX) / paletteWidth);
        float brightness = 1f - (float)((mouseY - contentY) / PALETTE_SIZE);

        colorSetting.setSaturation(saturation);
        colorSetting.setBrightness(brightness);
    }

    private void updateHue(double mouseY) {
        float pickerY = y + height + SPACING;
        float contentY = pickerY + SPACING;

        float hue = (float)((mouseY - contentY) / PALETTE_SIZE);
        colorSetting.setHue(hue);
    }

    private void updateAlpha(double mouseY) {
        float pickerY = y + height + SPACING;
        float contentY = pickerY + SPACING;

        float alpha = (float)((mouseY - contentY) / PALETTE_SIZE);
        colorSetting.setAlpha(alpha);
    }

    private String getHexString() {
        int color = colorSetting.getColor();
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return String.format("%02X%02X%02X%02X", r, g, b, a);
    }

    private void updateHexFromColor() {
        hexInputText = getHexString();
        hexCursorPosition = hexInputText.length();
    }

    private void applyHexInput() {
        String hex = hexInputText.toUpperCase();

        try {
            int r, g, b, a = 255;

            if (hex.length() == 6) {
                r = Integer.parseInt(hex.substring(0, 2), 16);
                g = Integer.parseInt(hex.substring(2, 4), 16);
                b = Integer.parseInt(hex.substring(4, 6), 16);
            } else if (hex.length() == 8) {
                r = Integer.parseInt(hex.substring(0, 2), 16);
                g = Integer.parseInt(hex.substring(2, 4), 16);
                b = Integer.parseInt(hex.substring(4, 6), 16);
                a = Integer.parseInt(hex.substring(6, 8), 16);
            } else if (hex.length() == 3) {
                r = Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                g = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                b = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
            } else {
                updateHexFromColor();
                return;
            }

            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            colorSetting.setHue(hsb[0]);
            colorSetting.setSaturation(hsb[1]);
            colorSetting.setBrightness(hsb[2]);
            colorSetting.setAlpha(a / 255f);

        } catch (NumberFormatException e) {
            updateHexFromColor();
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getTotalHeight() {
        float totalExpandedHeight = PALETTE_SIZE + SPACING + 22 + SPACING * 2;
        float expandedHeight = totalExpandedHeight * expandAnimation;
        return height + expandedHeight;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isHexInputActive() {
        return hexInputActive;
    }

    public boolean isDragging() {
        return draggingPalette || draggingHue || draggingAlpha;
    }
}