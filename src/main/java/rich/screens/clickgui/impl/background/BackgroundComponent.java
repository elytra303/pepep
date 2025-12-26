package rich.screens.clickgui.impl.background;

import net.minecraft.client.gui.DrawContext;
import rich.IMinecraft;
import rich.modules.module.ModuleCategory;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.gif.GifRender;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BackgroundComponent implements IMinecraft {
    public static final int BG_WIDTH = 400;
    public static final int BG_HEIGHT = 250;

    private static final ModuleCategory[] CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final String[] CATEGORY_NAMES = {"Combat", "Movement", "Render", "Player", "Util"};

    private final Map<ModuleCategory, Float> categoryAnimations = new HashMap<>();

    private ModuleCategory previousCategory = null;
    private ModuleCategory currentCategory = null;
    private float headerTransition = 1f;

    private static final float ANIMATION_SPEED = 8f;
    private static final float HEADER_SPEED = 3f;
    private static final float MAX_OFFSET = 5f;
    private static final float BALL_SIZE = 3f;
    private static final float TEXT_SIZE = 6f;
    private static final float HEADER_SLIDE_DISTANCE = 8f;

    private long lastUpdateTime = System.currentTimeMillis();

    public BackgroundComponent() {
        for (ModuleCategory cat : CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
    }

    public void updateAnimations(ModuleCategory selectedCategory, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        if (currentCategory != selectedCategory) {
            previousCategory = currentCategory;
            currentCategory = selectedCategory;
            headerTransition = 0f;
        }

        if (headerTransition < 1f) {
            headerTransition += HEADER_SPEED * deltaTime;
            if (headerTransition > 1f) {
                headerTransition = 1f;
            }
        }

        for (ModuleCategory cat : CATEGORIES) {
            float target = cat == selectedCategory ? 1f : 0f;
            float current = categoryAnimations.getOrDefault(cat, 0f);

            float diff = target - current;
            float change = diff * ANIMATION_SPEED * deltaTime;

            if (Math.abs(diff) < 0.001f) {
                categoryAnimations.put(cat, target);
            } else {
                categoryAnimations.put(cat, current + change);
            }
        }
    }

    public void render(DrawContext context, float bgX, float bgY, ModuleCategory selectedCategory, float delta, float alphaMultiplier) {
        updateAnimations(selectedCategory, delta);

        int baseAlpha = (int) (255 * alphaMultiplier);
        int[] gradientColors = {
                new Color(26, 26, 26, baseAlpha).getRGB(),
                new Color(0, 0, 0, baseAlpha).getRGB(),
                new Color(26, 26, 26, baseAlpha).getRGB(),
                new Color(0, 0, 0, baseAlpha).getRGB(),
                new Color(26, 26, 20, baseAlpha).getRGB()
        };

        Render2D.gradientRect(bgX, bgY, BG_WIDTH, BG_HEIGHT, gradientColors, 15);

        context.getMatrices().pushMatrix();
        renderAvatar(context, bgX, bgY, alphaMultiplier);
        context.getMatrices().popMatrix();
    }

    public void renderCategoryPanel(float bgX, float bgY, float alphaMultiplier) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);
        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, new Color(128, 128, 128, panelAlpha).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 10);
    }

    public void renderHeader(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);
        Render2D.rect(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, new Color(128, 128, 128, panelAlpha).getRGB(), 8);
        Render2D.outline(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 8);

        float baseX = bgX + 100f;
        float baseY = bgY + 16f;

        float eased = easeOutQuart(headerTransition);

        if (previousCategory != null && headerTransition < 1f) {
            float oldAlpha = (1f - eased) * alphaMultiplier;
            float oldOffsetY = eased * HEADER_SLIDE_DISTANCE;

            int oldAlphaInt = (int) (128 * oldAlpha);
            if (oldAlphaInt > 0) {
                String oldName = previousCategory.getReadableName();
                Fonts.BOLD.draw(oldName, baseX, baseY + oldOffsetY, 7, new Color(128, 128, 128, oldAlphaInt).getRGB());
            }
        }

        if (currentCategory != null) {
            float newAlpha = eased * alphaMultiplier;
            float newOffsetY = (1f - eased) * -HEADER_SLIDE_DISTANCE;

            int newAlphaInt = (int) (128 * newAlpha);
            if (newAlphaInt > 0) {
                String newName = currentCategory.getReadableName();
                Fonts.BOLD.draw(newName, baseX, baseY + newOffsetY, 7, new Color(128, 128, 128, newAlphaInt).getRGB());
            }
        }
    }

    private float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1 - x, 4);
    }

    private void renderAvatar(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        context.getMatrices().pushMatrix();
        GifRender.drawBackground(bgX + 12.5f, bgY + 12.5f, 70, 30, 7, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(42, 42, 42, alpha).getRGB(), 15);
        GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 33, bgY + 33, 5, 5, new Color(0, 255, 0, alpha).getRGB(), 10);
        context.getMatrices().popMatrix();
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void renderCategoryNames(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);

            float offsetX = animation * MAX_OFFSET;

            int baseGray = 128;
            int targetWhite = 255;
            int colorValue = (int) (baseGray + (targetWhite - baseGray) * animation);
            int alpha = (int) ((128 + 127 * animation) * alphaMultiplier);
            Color textColor = new Color(colorValue, colorValue, colorValue, alpha);

            float textX = bgX + 17f + offsetX;
            float textY = bgY + 65f + i * 15f;

            float textWidth = Fonts.BOLD.getWidth(CATEGORY_NAMES[i], TEXT_SIZE);

            if (animation > 0.01f) {
                float lineWidth = textWidth * animation;
                float lineAlpha = animation * 60 * alphaMultiplier;
                Render2D.rect(
                        textX,
                        textY + 9f,
                        lineWidth, 0.5f,
                        new Color(255, 255, 255, (int) lineAlpha).getRGB(),
                        0
                );

                float ballAlpha = animation * 200 * alphaMultiplier;
                float ballX = bgX + 12f;
                float ballY = textY + 2.5f;

                Render2D.rect(ballX, ballY, BALL_SIZE, BALL_SIZE,
                        new Color(255, 255, 255, (int) ballAlpha).getRGB(),
                        BALL_SIZE / 2f
                );
            }

            Fonts.BOLD.draw(CATEGORY_NAMES[i], textX, textY, TEXT_SIZE, textColor.getRGB());
        }
    }

    public ModuleCategory getCategoryAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        if (mouseX < bgX + 10f || mouseX > bgX + 85f) return null;

        float[] yPositions = {60f, 75f, 90f, 105f, 120f};

        for (int i = 0; i < yPositions.length; i++) {
            if (mouseY >= bgY + yPositions[i] && mouseY <= bgY + yPositions[i] + 13f) {
                return CATEGORIES[i];
            }
        }
        return null;
    }
}