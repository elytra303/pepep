package rich.screens.clickgui.impl.background.render;

import rich.modules.module.category.ModuleCategory;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CategoryRenderer {

    private static final ModuleCategory[] MAIN_CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final String[] MAIN_CATEGORY_NAMES = {"Combat", "Movement", "Render", "Player", "Util"};

    private static final ModuleCategory[] EXTRA_CATEGORIES = {
            ModuleCategory.AUTOBUY, ModuleCategory.CONFIGS
    };
    private static final String[] EXTRA_CATEGORY_NAMES = {"AutoBuy", "Configs"};

    private final Map<ModuleCategory, Float> categoryAnimations = new HashMap<>();

    private static final float ANIMATION_SPEED = 8f;
    private static final float MAX_OFFSET = 5f;
    private static final float BALL_SIZE = 3f;
    private static final float TEXT_SIZE = 6f;

    public CategoryRenderer() {
        for (ModuleCategory cat : MAIN_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
        for (ModuleCategory cat : EXTRA_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
    }

    public void updateAnimations(ModuleCategory selectedCategory, float deltaTime) {
        for (ModuleCategory cat : MAIN_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
        for (ModuleCategory cat : EXTRA_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
    }

    private void updateCategoryAnimation(ModuleCategory cat, ModuleCategory selected, float deltaTime) {
        float target = cat == selected ? 1f : 0f;
        float current = categoryAnimations.getOrDefault(cat, 0f);

        float diff = target - current;
        float change = diff * ANIMATION_SPEED * deltaTime;

        if (Math.abs(diff) < 0.001f) {
            categoryAnimations.put(cat, target);
        } else {
            categoryAnimations.put(cat, current + change);
        }
    }

    public void render(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        renderMainCategories(bgX, bgY, alphaMultiplier);
        renderSeparator(bgX, bgY, alphaMultiplier);
        renderExtraCategories(bgX, bgY, alphaMultiplier);
        renderBottomSeparator(bgX, bgY, alphaMultiplier);
    }

    private void renderMainCategories(float bgX, float bgY, float alphaMultiplier) {
        for (int i = 0; i < MAIN_CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = MAIN_CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);
            float textY = bgY + 65f + i * 15f;
            renderCategoryItem(bgX, textY, MAIN_CATEGORY_NAMES[i], animation, alphaMultiplier);
        }
    }

    private void renderExtraCategories(float bgX, float bgY, float alphaMultiplier) {
        float separatorY = bgY + 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        float extraStartY = separatorY + 9f;

        for (int i = 0; i < EXTRA_CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = EXTRA_CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);
            float textY = extraStartY + i * 15f;
            renderCategoryItem(bgX, textY, EXTRA_CATEGORY_NAMES[i], animation, alphaMultiplier);
        }
    }

    private void renderCategoryItem(float bgX, float textY, String name, float animation, float alphaMultiplier) {
        float offsetX = animation * MAX_OFFSET;

        int baseGray = 128;
        int targetWhite = 255;
        int colorValue = (int) (baseGray + (targetWhite - baseGray) * animation);
        int alpha = (int) ((128 + 127 * animation) * alphaMultiplier);
        Color textColor = new Color(colorValue, colorValue, colorValue, alpha);

        float textX = bgX + 17f + offsetX;
        float textWidth = Fonts.BOLD.getWidth(name, TEXT_SIZE);

        if (animation > 0.01f) {
            float lineWidth = textWidth * animation;
            float lineAlpha = animation * 60 * alphaMultiplier;
            Render2D.rect(textX, textY + 9f, lineWidth, 0.5f, new Color(255, 255, 255, (int) lineAlpha).getRGB(), 0);

            float ballAlpha = animation * 200 * alphaMultiplier;
            float ballX = bgX + 12f;
            float ballY = textY + 2.5f;
            Render2D.rect(ballX, ballY, BALL_SIZE, BALL_SIZE, new Color(255, 255, 255, (int) ballAlpha).getRGB(), BALL_SIZE / 2f);
        }

        Fonts.BOLD.draw(name, textX, textY, TEXT_SIZE, textColor.getRGB());
    }

    private void renderSeparator(float bgX, float bgY, float alphaMultiplier) {
        float separatorY = bgY + 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        int separatorAlpha = (int) (80 * alphaMultiplier);
        Render2D.rect(bgX + 15f, separatorY, 65f, 0.5f, new Color(100, 100, 100, separatorAlpha).getRGB(), 0);
    }

    private void renderBottomSeparator(float bgX, float bgY, float alphaMultiplier) {
        float separatorY = bgY + 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        float extraStartY = separatorY + 9f;
        float bottomSeparatorY = extraStartY + EXTRA_CATEGORY_NAMES.length * 15f + 1f;
        int separatorAlpha = (int) (80 * alphaMultiplier);
        Render2D.rect(bgX + 15f, bottomSeparatorY, 65f, 0.5f, new Color(100, 100, 100, separatorAlpha).getRGB(), 0);
    }

    public ModuleCategory getCategoryAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        if (mouseX < bgX + 10f || mouseX > bgX + 85f) return null;

        for (int i = 0; i < MAIN_CATEGORY_NAMES.length; i++) {
            float catY = 65f + i * 15f;
            if (mouseY >= bgY + catY && mouseY <= bgY + catY + 13f) {
                return MAIN_CATEGORIES[i];
            }
        }

        float separatorY = 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        float extraStartY = separatorY + 9f;

        for (int i = 0; i < EXTRA_CATEGORIES.length; i++) {
            float catY = extraStartY + i * 15f;
            if (mouseY >= bgY + catY && mouseY <= bgY + catY + 13f) {
                return EXTRA_CATEGORIES[i];
            }
        }

        return null;
    }
}