package rich.screens.clickgui.impl.background;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;
import rich.util.render.gif.GifRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundComponent implements IMinecraft {
    public static final int BG_WIDTH = 400;
    public static final int BG_HEIGHT = 250;

    private static final ModuleCategory[] MAIN_CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final String[] MAIN_CATEGORY_NAMES = {"Combat", "Movement", "Render", "Player", "Util"};

    private static final ModuleCategory[] EXTRA_CATEGORIES = {
            ModuleCategory.AUTOBUY, ModuleCategory.CONFIGS
    };
    private static final String[] EXTRA_CATEGORY_NAMES = {"AutoBuy", "Configs"};

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

    private boolean searchActive = false;
    private String searchText = "";
    private int searchCursorPosition = 0;
    private int searchSelectionStart = -1;
    private int searchSelectionEnd = -1;
    private float searchCursorBlink = 0f;
    private float searchBoxAnimation = 0f;
    private float searchFocusAnimation = 0f;
    private float searchPanelAlpha = 0f;
    private float normalPanelAlpha = 1f;
    private float searchSelectionAnimation = 0f;
    private List<ModuleStructure> searchResults = new ArrayList<>();
    private Map<ModuleStructure, Float> searchResultAnimations = new HashMap<>();
    private Map<ModuleStructure, Long> searchResultAnimStartTimes = new HashMap<>();
    private float searchScrollOffset = 0f;
    private float searchTargetScroll = 0f;
    private int hoveredSearchIndex = -1;
    private ModuleStructure selectedSearchModule = null;

    private static final float SEARCH_ANIM_SPEED = 8f;
    private static final float PANEL_FADE_SPEED = 15f;
    private static final float SEARCH_RESULT_HEIGHT = 18f;
    private static final float SEARCH_RESULT_ANIM_DURATION = 200f;

    public BackgroundComponent() {
        for (ModuleCategory cat : MAIN_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
        for (ModuleCategory cat : EXTRA_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
    }

    public boolean isSearchActive() {
        return searchActive;
    }

    public float getSearchPanelAlpha() {
        return searchPanelAlpha;
    }

    public float getNormalPanelAlpha() {
        return normalPanelAlpha;
    }

    public void setSearchActive(boolean active) {
        if (active && !searchActive) {
            searchText = "";
            searchCursorPosition = 0;
            searchSelectionStart = -1;
            searchSelectionEnd = -1;
            searchResults.clear();
            searchResultAnimations.clear();
            searchResultAnimStartTimes.clear();
            searchScrollOffset = 0f;
            searchTargetScroll = 0f;
            hoveredSearchIndex = -1;
            selectedSearchModule = null;
        }
        searchActive = active;
    }

    public String getSearchText() {
        return searchText;
    }

    public List<ModuleStructure> getSearchResults() {
        return searchResults;
    }

    public ModuleStructure getSelectedSearchModule() {
        return selectedSearchModule;
    }

    private boolean isControlDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean isShiftDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private boolean hasSearchSelection() {
        return searchSelectionStart != -1 && searchSelectionEnd != -1 && searchSelectionStart != searchSelectionEnd;
    }

    private int getSearchSelectionStart() {
        return Math.min(searchSelectionStart, searchSelectionEnd);
    }

    private int getSearchSelectionEnd() {
        return Math.max(searchSelectionStart, searchSelectionEnd);
    }

    private void clearSearchSelection() {
        searchSelectionStart = -1;
        searchSelectionEnd = -1;
    }

    private void selectAllSearchText() {
        searchSelectionStart = 0;
        searchSelectionEnd = searchText.length();
        searchCursorPosition = searchText.length();
    }

    private void deleteSelectedSearchText() {
        if (hasSearchSelection()) {
            int start = getSearchSelectionStart();
            int end = getSearchSelectionEnd();
            searchText = searchText.substring(0, start) + searchText.substring(end);
            searchCursorPosition = start;
            clearSearchSelection();
            updateSearchResults();
        }
    }

    private String getSelectedSearchText() {
        if (!hasSearchSelection()) return "";
        return searchText.substring(getSearchSelectionStart(), getSearchSelectionEnd());
    }

    private void copySearchToClipboard() {
        if (hasSearchSelection()) {
            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), getSelectedSearchText());
        }
    }

    private void pasteToSearch() {
        String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (clipboardText != null && !clipboardText.isEmpty()) {
            clipboardText = clipboardText.replaceAll("[\n\r\t]", "");

            if (hasSearchSelection()) {
                deleteSelectedSearchText();
            }

            searchText = searchText.substring(0, searchCursorPosition) + clipboardText + searchText.substring(searchCursorPosition);
            searchCursorPosition += clipboardText.length();
            updateSearchResults();
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

        for (ModuleCategory cat : MAIN_CATEGORIES) {
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

        for (ModuleCategory cat : EXTRA_CATEGORIES) {
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

        float searchTarget = searchActive ? 1f : 0f;
        float searchDiff = searchTarget - searchBoxAnimation;
        if (Math.abs(searchDiff) < 0.001f) {
            searchBoxAnimation = searchTarget;
        } else {
            searchBoxAnimation += searchDiff * SEARCH_ANIM_SPEED * deltaTime;
        }

        float focusTarget = searchActive ? 1f : 0f;
        float focusDiff = focusTarget - searchFocusAnimation;
        if (Math.abs(focusDiff) < 0.001f) {
            searchFocusAnimation = focusTarget;
        } else {
            searchFocusAnimation += focusDiff * SEARCH_ANIM_SPEED * deltaTime;
        }

        float searchPanelTarget = searchActive ? 1f : 0f;
        float searchPanelDiff = searchPanelTarget - searchPanelAlpha;
        if (Math.abs(searchPanelDiff) < 0.001f) {
            searchPanelAlpha = searchPanelTarget;
        } else {
            searchPanelAlpha += searchPanelDiff * PANEL_FADE_SPEED * deltaTime;
        }

        float normalPanelTarget = searchActive ? 0f : 1f;
        float normalPanelDiff = normalPanelTarget - normalPanelAlpha;
        if (Math.abs(normalPanelDiff) < 0.001f) {
            normalPanelAlpha = normalPanelTarget;
        } else {
            normalPanelAlpha += normalPanelDiff * PANEL_FADE_SPEED * deltaTime;
        }

        float selectionTarget = hasSearchSelection() ? 1f : 0f;
        float selectionDiff = selectionTarget - searchSelectionAnimation;
        if (Math.abs(selectionDiff) < 0.001f) {
            searchSelectionAnimation = selectionTarget;
        } else {
            searchSelectionAnimation += selectionDiff * SEARCH_ANIM_SPEED * deltaTime;
        }

        if (searchActive) {
            searchCursorBlink += deltaTime * 2f;
            if (searchCursorBlink > 1f) searchCursorBlink -= 1f;
        }

        for (ModuleStructure mod : searchResults) {
            Long startTime = searchResultAnimStartTimes.get(mod);
            if (startTime == null) continue;

            float elapsed = currentTime - startTime;
            float progress = Math.min(1f, Math.max(0f, elapsed / SEARCH_RESULT_ANIM_DURATION));
            progress = easeOutCubic(progress);
            searchResultAnimations.put(mod, progress);
        }

        float scrollDiff = searchTargetScroll - searchScrollOffset;
        if (Math.abs(scrollDiff) < 0.5f) {
            searchScrollOffset = searchTargetScroll;
        } else {
            searchScrollOffset += scrollDiff * 12f * deltaTime;
        }
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
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
        int blurAlpha = (int) (155 * alphaMultiplier);
        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, new Color(128, 128, 128, panelAlpha).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 10);

        Render2D.outline(bgX + 12.5f, bgY + 220.5f, 70, 17, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 5);

        Fonts.GUI_ICONS.draw("X", bgX + 21.15f, bgY + 217.5f, 19, new Color(58, 58, 58, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Y", bgX + 40f, bgY + 217f, 20, new Color(58, 58, 58, outlineAlpha).getRGB());
        Fonts.GUI_ICONS.draw("Z", bgX + 60f, bgY + 217f, 20, new Color(58, 58, 58, outlineAlpha).getRGB());

        Render2D.blur(bgX + 12.5f, bgY + 220.5f, 70, 17, 4, 5, new Color(25, 25, 25, blurAlpha).getRGB());

        float textSize = 6f;
        String soonText = "Soon...";
        float textWidth = Fonts.BOLD.getWidth(soonText, textSize);
        float textHeight = Fonts.BOLD.getHeight(textSize);
        float centerX = bgX + 12.5f + (70 - textWidth) / 2f;
        float centerY = bgY + 220.5f + (17 - textHeight) / 2f;
        Fonts.BOLD.draw(soonText, centerX, centerY, textSize, new Color(150, 150, 150, (int)(200 * alphaMultiplier)).getRGB());
    }

    public void renderHeader(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        int panelAlpha = (int) (25 * alphaMultiplier);
        int outlineAlpha = (int) (255 * alphaMultiplier);
        Render2D.rect(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, new Color(128, 128, 128, panelAlpha).getRGB(), 8);
        Render2D.outline(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 8);

        float searchBoxX = bgX + 315f;
        float searchBoxY = bgY + 12.5f;
        float searchBoxW = 70f;
        float searchBoxH = 15f;

        Color searchOutline;
        if (searchActive) {
            searchOutline = new Color(180, 180, 180, (int) (255 * alphaMultiplier));
        } else {
            searchOutline = new Color(55, 55, 55, outlineAlpha);
        }

        int searchBgAlpha = (int) ((25 + searchFocusAnimation * 15) * alphaMultiplier);
        Render2D.rect(searchBoxX, searchBoxY, searchBoxW, searchBoxH, new Color(40, 40, 45, searchBgAlpha).getRGB(), 4);
        Render2D.outline(searchBoxX, searchBoxY, searchBoxW, searchBoxH, 0.5f, searchOutline.getRGB(), 4);

        float textAreaX = searchBoxX + 5;
        float textAreaWidth = searchBoxW - 22;

        if (searchActive && !searchText.isEmpty()) {
            Scissor.enable(searchBoxX + 3, searchBoxY, searchBoxW - 20, searchBoxH);

            if (hasSearchSelection() && searchSelectionAnimation > 0.01f) {
                int start = getSearchSelectionStart();
                int end = getSearchSelectionEnd();
                String beforeSelection = searchText.substring(0, start);
                String selection = searchText.substring(start, end);

                float selectionX = textAreaX + Fonts.BOLD.getWidth(beforeSelection, 5);
                float selectionWidth = Fonts.BOLD.getWidth(selection, 5);

                int selAlpha = (int) (100 * searchSelectionAnimation * alphaMultiplier);
                Render2D.rect(selectionX, searchBoxY + 2, selectionWidth, searchBoxH - 4,
                        new Color(100, 140, 180, selAlpha).getRGB(), 2f);
            }

            Fonts.BOLD.draw(searchText, textAreaX, searchBoxY + 5f, 5, new Color(210, 210, 220, (int) (255 * alphaMultiplier)).getRGB());
            Scissor.disable();

            if (!hasSearchSelection()) {
                float cursorAlpha = (float) (Math.sin(searchCursorBlink * Math.PI * 2) * 0.5 + 0.5);
                if (cursorAlpha > 0.3f) {
                    String beforeCursor = searchText.substring(0, searchCursorPosition);
                    float cursorX = textAreaX + Fonts.BOLD.getWidth(beforeCursor, 5);
                    int cursorAlphaInt = (int) (255 * cursorAlpha * alphaMultiplier);
                    Render2D.rect(cursorX, searchBoxY + 3, 0.5f, searchBoxH - 6, new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0);
                }
            }
        } else if (searchActive) {
            Fonts.BOLD.draw("Type to search...", textAreaX, searchBoxY + 5f, 5, new Color(100, 100, 105, (int) (150 * alphaMultiplier)).getRGB());

            float cursorAlpha = (float) (Math.sin(searchCursorBlink * Math.PI * 2) * 0.5 + 0.5);
            if (cursorAlpha > 0.3f) {
                int cursorAlphaInt = (int) (255 * cursorAlpha * alphaMultiplier);
                Render2D.rect(textAreaX, searchBoxY + 3, 0.5f, searchBoxH - 6, new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0);
            }
        } else {
            Fonts.BOLD.draw("Search Modules...", textAreaX, searchBoxY + 5f, 5, new Color(128, 128, 128, outlineAlpha).getRGB());
        }

        Render2D.rect(searchBoxX + 53, searchBoxY + 3.5f, 1, searchBoxH - 7, new Color(128, 128, 128, panelAlpha).getRGB(), 8);
        Fonts.ICONS.draw("U", searchBoxX + 55, searchBoxY + 1.5f, 12, new Color(128, 128, 128, outlineAlpha).getRGB());

        float baseX = bgX + 100f;
        float baseY = bgY + 16f;

        float categoryAlpha = normalPanelAlpha * alphaMultiplier;
        if (categoryAlpha > 0.01f) {
            float eased = easeOutQuart(headerTransition);

            if (previousCategory != null && headerTransition < 1f) {
                float oldAlpha = (1f - eased) * categoryAlpha;
                float oldOffsetY = eased * HEADER_SLIDE_DISTANCE;

                int oldAlphaInt = (int) (128 * oldAlpha);
                if (oldAlphaInt > 0) {
                    String oldName = previousCategory.getReadableName();
                    Fonts.BOLD.draw(oldName, baseX, baseY + oldOffsetY, 7, new Color(128, 128, 128, oldAlphaInt).getRGB());
                }
            }

            if (currentCategory != null) {
                float newAlpha = eased * categoryAlpha;
                float newOffsetY = (1f - eased) * -HEADER_SLIDE_DISTANCE;

                int newAlphaInt = (int) (128 * newAlpha);
                if (newAlphaInt > 0) {
                    String newName = currentCategory.getReadableName();
                    Fonts.BOLD.draw(newName, baseX, baseY + newOffsetY, 7, new Color(128, 128, 128, newAlphaInt).getRGB());
                }
            }
        }

        float searchLabelAlpha = searchPanelAlpha * alphaMultiplier;
        if (searchLabelAlpha > 0.01f) {
            int searchLabelAlphaInt = (int) (180 * searchLabelAlpha);
            if (searchLabelAlphaInt > 0) {
                String searchLabel = "Search Results";
                if (!searchText.isEmpty()) {
                    searchLabel = "Results for \"" + (searchText.length() > 12 ? searchText.substring(0, 12) + "..." : searchText) + "\"";
                }
                Fonts.BOLD.draw(searchLabel, baseX, baseY, 7, new Color(160, 160, 160, searchLabelAlphaInt).getRGB());
            }
        }
    }

    private float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1 - x, 4);
    }

    private void renderAvatar(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        int alphaFon = (int) (105 * alphaMultiplier);
        int alphaText = (int) (200 * alphaMultiplier);
        context.getMatrices().pushMatrix();
        GifRender.drawBackground(bgX + 12.5f, bgY + 12.5f, 70, 30, 7, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(42, 42, 42, alpha).getRGB(), 15);
        GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 33, bgY + 33, 5, 5, new Color(0, 255, 0, alpha).getRGB(), 10);
        context.getMatrices().popMatrix();

        Render2D.rect(bgX + 12.5f, bgY + 12.5f, 70, 30, new Color(0, 0, 0, alphaFon).getRGB(), 7);

        float textX = bgX + 44;
        float textY = bgY + 22;
        float maxTextWidth = 35f;
        float textHeight = 14f;

        Scissor.enable(textX, textY - 2, maxTextWidth, textHeight);
        Fonts.BOLD.draw("Baflllik", textX, textY, 6, new Color(255, 255, 255, alphaText).getRGB());
        Fonts.BOLD.draw("Uid: 1", textX, textY + 7, 5, new Color(255, 255, 255, alphaText).getRGB());
        Scissor.disable();
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void renderCategoryNames(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        for (int i = 0; i < MAIN_CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = MAIN_CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);

            float offsetX = animation * MAX_OFFSET;

            int baseGray = 128;
            int targetWhite = 255;
            int colorValue = (int) (baseGray + (targetWhite - baseGray) * animation);
            int alpha = (int) ((128 + 127 * animation) * alphaMultiplier);
            Color textColor = new Color(colorValue, colorValue, colorValue, alpha);

            float textX = bgX + 17f + offsetX;
            float textY = bgY + 65f + i * 15f;

            float textWidth = Fonts.BOLD.getWidth(MAIN_CATEGORY_NAMES[i], TEXT_SIZE);

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

            Fonts.BOLD.draw(MAIN_CATEGORY_NAMES[i], textX, textY, TEXT_SIZE, textColor.getRGB());
        }

        float separatorY = bgY + 65f + MAIN_CATEGORY_NAMES.length * 15f + 1f;
        int separatorAlpha = (int) (80 * alphaMultiplier);
        Render2D.rect(bgX + 15f, separatorY, 65f, 0.5f, new Color(100, 100, 100, separatorAlpha).getRGB(), 0);

        float extraStartY = separatorY + 9f;

        for (int i = 0; i < EXTRA_CATEGORY_NAMES.length; i++) {
            ModuleCategory cat = EXTRA_CATEGORIES[i];
            float animation = categoryAnimations.getOrDefault(cat, 0f);

            float offsetX = animation * MAX_OFFSET;

            int baseGray = 128;
            int targetWhite = 255;
            int colorValue = (int) (baseGray + (targetWhite - baseGray) * animation);
            int alpha = (int) ((128 + 127 * animation) * alphaMultiplier);
            Color textColor = new Color(colorValue, colorValue, colorValue, alpha);

            float textX = bgX + 17f + offsetX;
            float textY = extraStartY + i * 15f;

            float textWidth = Fonts.BOLD.getWidth(EXTRA_CATEGORY_NAMES[i], TEXT_SIZE);

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

            Fonts.BOLD.draw(EXTRA_CATEGORY_NAMES[i], textX, textY, TEXT_SIZE, textColor.getRGB());
        }

        float bottomSeparatorY = extraStartY + EXTRA_CATEGORY_NAMES.length * 15f + 1f;
        Render2D.rect(bgX + 15f, bottomSeparatorY, 65f, 0.5f, new Color(100, 100, 100, separatorAlpha).getRGB(), 0);
    }

    public void renderSearchResults(DrawContext context, float bgX, float bgY, float mouseX, float mouseY, int guiScale, float alphaMultiplier) {
        if (searchPanelAlpha <= 0.01f) return;

        float panelX = bgX + 92f;
        float panelY = bgY + 38f;
        float panelW = BG_WIDTH - 100f;
        float panelH = BG_HEIGHT - 46f;

        float resultAlpha = searchPanelAlpha * alphaMultiplier;

        int panelBgAlpha = (int) (15 * resultAlpha);
        int outlineAlpha = (int) (215 * resultAlpha);
        Render2D.rect(panelX, panelY, panelW, panelH, new Color(64, 64, 64, panelBgAlpha).getRGB(), 7f);
        Render2D.outline(panelX, panelY, panelW, panelH, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), 7f);

        if (searchResults.isEmpty()) {
            String noResults = searchText.isEmpty() ? "Start typing to search..." : "No modules found";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(noResults, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = panelX + (panelW - textWidth) / 2f;
            float centerY = panelY + (panelH - textHeight) / 2f;
            Fonts.BOLD.draw(noResults, centerX, centerY, textSize, new Color(100, 100, 100, (int) (150 * resultAlpha)).getRGB());
            return;
        }

        Scissor.enable(panelX + 3, panelY + 3, panelW - 6, panelH - 6, guiScale);

        float startY = panelY + 5 + searchScrollOffset;
        hoveredSearchIndex = -1;

        for (int i = 0; i < searchResults.size(); i++) {
            ModuleStructure module = searchResults.get(i);
            float itemY = startY + i * (SEARCH_RESULT_HEIGHT + 2);

            if (itemY + SEARCH_RESULT_HEIGHT < panelY || itemY > panelY + panelH) continue;

            float itemAnim = searchResultAnimations.getOrDefault(module, 0f);
            float itemAlpha = itemAnim * resultAlpha;

            if (itemAlpha <= 0.01f) continue;

            float itemOffsetX = (1f - itemAnim) * 20f;

            boolean hovered = mouseX >= panelX + 5 && mouseX <= panelX + panelW - 5 &&
                    mouseY >= itemY && mouseY <= itemY + SEARCH_RESULT_HEIGHT;

            if (hovered) {
                hoveredSearchIndex = i;
            }

            boolean selected = module == selectedSearchModule;

            Color bg;
            if (selected) {
                bg = new Color(140, 140, 140, (int) (60 * itemAlpha));
            } else if (hovered) {
                bg = new Color(100, 100, 100, (int) (40 * itemAlpha));
            } else {
                bg = new Color(64, 64, 64, (int) (25 * itemAlpha));
            }

            float itemX = panelX + 5 + itemOffsetX;
            float itemW = panelW - 10;

            Render2D.rect(itemX, itemY, itemW, SEARCH_RESULT_HEIGHT, bg.getRGB(), 5);

            if (selected) {
                Render2D.outline(itemX, itemY, itemW, SEARCH_RESULT_HEIGHT, 0.5f, new Color(160, 160, 160, (int) (100 * itemAlpha)).getRGB(), 5);
            }

            Color textColor;
            if (module.isState()) {
                textColor = new Color(255, 255, 255, (int) (255 * itemAlpha));
            } else {
                textColor = new Color(180, 180, 180, (int) (200 * itemAlpha));
            }

            Fonts.BOLD.draw(module.getName(), itemX + 5, itemY + 3, 6, textColor.getRGB());

            String categoryName = module.getCategory().getReadableName();
            Color categoryColor = new Color(140, 140, 140, (int) (180 * itemAlpha));
            Fonts.BOLD.draw(categoryName, itemX + 5, itemY + 11, 4, categoryColor.getRGB());

            if (module.isState()) {
                float indicatorX = itemX + itemW - 10;
                float indicatorY = itemY + SEARCH_RESULT_HEIGHT / 2 - 2;
                Render2D.rect(indicatorX, indicatorY, 4, 4, new Color(100, 200, 100, (int) (200 * itemAlpha)).getRGB(), 2);
            }
        }

        Scissor.disable();

        float maxScroll = Math.max(0, searchResults.size() * (SEARCH_RESULT_HEIGHT + 2) - panelH + 10);
        if (maxScroll > 0) {
            if (searchScrollOffset < -0.5f) {
                for (int i = 0; i < 10; i++) {
                    float fadeAlpha = 60 * resultAlpha * (1f - i / 10f);
                    Render2D.rect(panelX + 3, panelY + 3 + i, panelW - 6, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
                }
            }
            if (searchScrollOffset > -maxScroll + 0.5f) {
                for (int i = 0; i < 10; i++) {
                    float fadeAlpha = 60 * resultAlpha * (i / 10f);
                    Render2D.rect(panelX + 3, panelY + panelH - 13 + i, panelW - 6, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
                }
            }
        }
    }

    public void updateSearchResults() {
        if (searchText.isEmpty()) {
            searchResults.clear();
            searchResultAnimations.clear();
            searchResultAnimStartTimes.clear();
            searchScrollOffset = 0f;
            searchTargetScroll = 0f;
            selectedSearchModule = null;
            return;
        }

        String query = searchText.toLowerCase();

        List<ModuleStructure> newResults = new ArrayList<>();
        Map<ModuleStructure, Float> oldAnimations = new HashMap<>(searchResultAnimations);

        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure module : repo.modules()) {
                    if (module.getName().toLowerCase().contains(query)) {
                        newResults.add(module);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        searchResultAnimations.clear();
        searchResultAnimStartTimes.clear();

        long currentTime = System.currentTimeMillis();
        int newIndex = 0;

        for (int i = 0; i < newResults.size(); i++) {
            ModuleStructure module = newResults.get(i);

            if (oldAnimations.containsKey(module)) {
                float oldProgress = oldAnimations.get(module);
                searchResultAnimations.put(module, Math.max(oldProgress, 0.5f));
                searchResultAnimStartTimes.put(module, currentTime - (long)(SEARCH_RESULT_ANIM_DURATION * 0.85f));
            } else {
                searchResultAnimations.put(module, 0f);
                searchResultAnimStartTimes.put(module, currentTime + newIndex * 40L);
                newIndex++;
            }
        }

        searchResults = newResults;

        if (!searchResults.isEmpty()) {
            if (selectedSearchModule == null || !searchResults.contains(selectedSearchModule)) {
                selectedSearchModule = searchResults.get(0);
            }
        } else {
            selectedSearchModule = null;
        }
    }

    public boolean handleSearchChar(char chr) {
        if (!searchActive) return false;

        if (Character.isISOControl(chr)) return false;

        if (hasSearchSelection()) {
            deleteSelectedSearchText();
        }

        searchText = searchText.substring(0, searchCursorPosition) + chr + searchText.substring(searchCursorPosition);
        searchCursorPosition++;
        clearSearchSelection();
        updateSearchResults();
        return true;
    }

    public boolean handleSearchKey(int keyCode) {
        if (!searchActive) return false;

        if (isControlDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> {
                    selectAllSearchText();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    copySearchToClipboard();
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    pasteToSearch();
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    if (hasSearchSelection()) {
                        copySearchToClipboard();
                        deleteSelectedSearchText();
                    }
                    return true;
                }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSearchSelection()) {
                    deleteSelectedSearchText();
                } else if (searchCursorPosition > 0) {
                    searchText = searchText.substring(0, searchCursorPosition - 1) + searchText.substring(searchCursorPosition);
                    searchCursorPosition--;
                    updateSearchResults();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSearchSelection()) {
                    deleteSelectedSearchText();
                } else if (searchCursorPosition < searchText.length()) {
                    searchText = searchText.substring(0, searchCursorPosition) + searchText.substring(searchCursorPosition + 1);
                    updateSearchResults();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (hasSearchSelection() && !isShiftDown()) {
                    searchCursorPosition = getSearchSelectionStart();
                    clearSearchSelection();
                } else {
                    if (searchCursorPosition > 0) {
                        if (isShiftDown()) {
                            if (searchSelectionStart == -1) {
                                searchSelectionStart = searchCursorPosition;
                            }
                            searchCursorPosition--;
                            searchSelectionEnd = searchCursorPosition;
                        } else {
                            searchCursorPosition--;
                            clearSearchSelection();
                        }
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (hasSearchSelection() && !isShiftDown()) {
                    searchCursorPosition = getSearchSelectionEnd();
                    clearSearchSelection();
                } else {
                    if (searchCursorPosition < searchText.length()) {
                        if (isShiftDown()) {
                            if (searchSelectionStart == -1) {
                                searchSelectionStart = searchCursorPosition;
                            }
                            searchCursorPosition++;
                            searchSelectionEnd = searchCursorPosition;
                        } else {
                            searchCursorPosition++;
                            clearSearchSelection();
                        }
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (isShiftDown()) {
                    if (searchSelectionStart == -1) {
                        searchSelectionStart = searchCursorPosition;
                    }
                    searchCursorPosition = 0;
                    searchSelectionEnd = searchCursorPosition;
                } else {
                    searchCursorPosition = 0;
                    clearSearchSelection();
                }
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (isShiftDown()) {
                    if (searchSelectionStart == -1) {
                        searchSelectionStart = searchCursorPosition;
                    }
                    searchCursorPosition = searchText.length();
                    searchSelectionEnd = searchCursorPosition;
                } else {
                    searchCursorPosition = searchText.length();
                    clearSearchSelection();
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (!searchResults.isEmpty() && selectedSearchModule != null) {
                    int currentIndex = searchResults.indexOf(selectedSearchModule);
                    if (currentIndex > 0) {
                        selectedSearchModule = searchResults.get(currentIndex - 1);
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (!searchResults.isEmpty() && selectedSearchModule != null) {
                    int currentIndex = searchResults.indexOf(selectedSearchModule);
                    if (currentIndex < searchResults.size() - 1) {
                        selectedSearchModule = searchResults.get(currentIndex + 1);
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER -> {
                if (selectedSearchModule != null) {
                    selectedSearchModule.switchState();
                }
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                setSearchActive(false);
                return true;
            }
        }
        return false;
    }

    public void handleSearchScroll(double vertical, float panelHeight) {
        if (!searchActive || searchResults.isEmpty()) return;

        float maxScroll = Math.max(0, searchResults.size() * (SEARCH_RESULT_HEIGHT + 2) - panelHeight + 10);
        searchTargetScroll = (float) Math.max(-maxScroll, Math.min(0, searchTargetScroll + vertical * 25));
    }

    public boolean isSearchBoxHovered(double mouseX, double mouseY, float bgX, float bgY) {
        float searchBoxX = bgX + 315f;
        float searchBoxY = bgY + 12.5f;
        float searchBoxW = 70f;
        float searchBoxH = 15f;

        return mouseX >= searchBoxX && mouseX <= searchBoxX + searchBoxW &&
                mouseY >= searchBoxY && mouseY <= searchBoxY + searchBoxH;
    }

    public ModuleStructure getSearchModuleAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        if (!searchActive || searchResults.isEmpty()) return null;

        float panelX = bgX + 92f;
        float panelY = bgY + 38f;
        float panelW = BG_WIDTH - 100f;
        float panelH = BG_HEIGHT - 46f;

        if (mouseX < panelX + 5 || mouseX > panelX + panelW - 5 ||
                mouseY < panelY || mouseY > panelY + panelH) return null;

        float startY = panelY + 5 + searchScrollOffset;

        for (int i = 0; i < searchResults.size(); i++) {
            float itemY = startY + i * (SEARCH_RESULT_HEIGHT + 2);

            if (mouseY >= itemY && mouseY <= itemY + SEARCH_RESULT_HEIGHT) {
                return searchResults.get(i);
            }
        }

        return null;
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