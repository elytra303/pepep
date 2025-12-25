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

    private static final int[] GRADIENT_COLORS = {
            new Color(26, 26, 26, 255).getRGB(),
            new Color(0, 0, 0, 255).getRGB(),
            new Color(26, 26, 26, 255).getRGB(),
            new Color(0, 0, 0, 255).getRGB(),
            new Color(26, 26, 20, 255).getRGB()
    };

    private static final ModuleCategory[] CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final String[] CATEGORY_NAMES = {"Combat", "Movement", "Render", "Player", "Util"};

    private final Map<ModuleCategory, Float> categoryAnimations = new HashMap<>();
    private final Map<ModuleCategory, Float> headerAlphaAnimations = new HashMap<>();

    private static final float ANIMATION_SPEED = 8f;
    private static final float HEADER_ALPHA_SPEED = 8f;
    private static final float MAX_OFFSET = 5f;
    private static final float BALL_SIZE = 3f;
    private static final float TEXT_SIZE = 6f;

    private long lastUpdateTime = System.currentTimeMillis();

    public BackgroundComponent() {
        for (ModuleCategory cat : CATEGORIES) {
            categoryAnimations.put(cat, 0f);
            headerAlphaAnimations.put(cat, 0f);
        }
    }

    public void updateAnimations(ModuleCategory selectedCategory, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

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

            float headerTarget = cat == selectedCategory ? 1f : 0f;
            float headerCurrent = headerAlphaAnimations.getOrDefault(cat, 0f);
            float headerDiff = headerTarget - headerCurrent;
            float headerChange = headerDiff * HEADER_ALPHA_SPEED * deltaTime;

            if (Math.abs(headerDiff) < 0.001f) {
                headerAlphaAnimations.put(cat, headerTarget);
            } else {
                headerAlphaAnimations.put(cat, headerCurrent + headerChange);
            }
        }
    }

    public void render(DrawContext context, float bgX, float bgY, ModuleCategory selectedCategory, float delta) {
        updateAnimations(selectedCategory, delta);

        Render2D.gradientRect(bgX, bgY, BG_WIDTH, BG_HEIGHT, GRADIENT_COLORS, 15);

        context.getMatrices().pushMatrix();
        renderAvatar(context, bgX, bgY);
        context.getMatrices().popMatrix();
    }

    public void renderCategoryPanel(float bgX, float bgY) {
        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, new Color(128, 128, 128, 25).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, 0.5f, new Color(55, 55, 55, 255).getRGB(), 10);
    }

    public void renderHeader(float bgX, float bgY, ModuleCategory selectedCategory) {
        Render2D.rect(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, new Color(128, 128, 128, 25).getRGB(), 8);
        Render2D.outline(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, 0.5f, new Color(55, 55, 55, 255).getRGB(), 8);

        float baseX = bgX + 100f;
        float baseY = bgY + 16f;

        for (ModuleCategory cat : CATEGORIES) {
            float alpha = headerAlphaAnimations.getOrDefault(cat, 0f);

            if (alpha > 0.01f) {
                int alphaInt = (int) (128 * alpha);
                String name = cat.getReadableName();
                Fonts.BOLD.draw(name, baseX, baseY, 7, new Color(128, 128, 128, alphaInt).getRGB());
            }
        }
    }

    private void renderAvatar(DrawContext context, float bgX, float bgY) {
        context.getMatrices().pushMatrix();
        GifRender.drawBackground(bgX + 12.5f, bgY + 12.5f, 70, 30, 7, -1);
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(12, 12, 12, 255).getRGB(), 15);
        GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, -1);
        context.getMatrices().popMatrix();
    }

    public void renderCategoryNames(float bgX, float bgY, ModuleCategory selectedCategory) {
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);

            float offsetX = animation * MAX_OFFSET;

            int baseGray = 128;
            int targetWhite = 255;
            int colorValue = (int) (baseGray + (targetWhite - baseGray) * animation);
            int alpha = (int) (128 + 127 * animation);
            Color textColor = new Color(colorValue, colorValue, colorValue, alpha);

            float textX = bgX + 17f + offsetX;
            float textY = bgY + 65f + i * 15f;

            float textWidth = Fonts.BOLD.getWidth(CATEGORY_NAMES[i], TEXT_SIZE);

            if (animation > 0.01f) {
                float lineWidth = textWidth * animation;
                float lineAlpha = animation * 60;
                Render2D.rect(
                        textX,
                        textY + 9f,
                        lineWidth, 0.5f,
                        new Color(255, 255, 255, (int) lineAlpha).getRGB(),
                        0
                );

                float ballAlpha = animation * 200;
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