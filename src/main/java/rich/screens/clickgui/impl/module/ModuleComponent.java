package rich.screens.clickgui.impl.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.SettingComponentAdder;
import rich.screens.clickgui.impl.settingsrender.*;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
@Setter
public class ModuleComponent implements IMinecraft {
    private static final int SETTING_HEIGHT = 16;
    private static final int SETTING_SPACING = 2;
    private static final float MODULE_ITEM_HEIGHT = 22f;

    private static final float MODULE_LIST_CORNER_RADIUS = 6f;
    private static final float SETTINGS_PANEL_CORNER_RADIUS = 7f;
    private static final float CORNER_INSET = 3f;

    private List<ModuleStructure> modules = new ArrayList<>();
    private List<ModuleStructure> oldModules = new ArrayList<>();
    private ModuleStructure selectedModule = null;
    private ModuleStructure bindingModule = null;
    private List<AbstractSettingComponent> settingComponents = new ArrayList<>();

    private Map<ModuleStructure, Float> moduleAnimations = new HashMap<>();
    private Map<ModuleStructure, Long> moduleAnimStartTimes = new HashMap<>();
    private Map<ModuleStructure, Float> oldModuleAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Float> settingAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Long> settingAnimStartTimes = new HashMap<>();
    private Map<AbstractSettingComponent, Float> visibilityAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Float> heightAnimations = new HashMap<>();

    private ModuleCategory currentCategory = null;

    private double moduleTargetScroll = 0, moduleDisplayScroll = 0;
    private double settingTargetScroll = 0, settingDisplayScroll = 0;
    private float moduleScrollTopFade = 0f, moduleScrollBottomFade = 0f;
    private float settingScrollTopFade = 0f, settingScrollBottomFade = 0f;

    private float lastSettingsPanelHeight = 0f;

    private long lastScrollUpdateTime = System.currentTimeMillis();
    private long lastVisibilityUpdateTime = System.currentTimeMillis();

    private static final float MODULE_ANIM_DURATION = 300f;
    private static final float SETTING_ANIM_DURATION = 450f;
    private static final float SCROLL_SPEED = 12f;
    private static final float FADE_SPEED = 8f;
    private static final float VISIBILITY_ANIM_SPEED = 8f;
    private static final float HEIGHT_ANIM_SPEED = 10f;

    private boolean isCategoryTransitioning = false;
    private float categoryTransitionProgress = 1f;
    private long categoryTransitionStartTime = 0;
    private static final float CATEGORY_TRANSITION_DURATION = 280f;
    private static final float CATEGORY_SLIDE_DISTANCE = 40f;

    private double oldModuleDisplayScroll = 0;

    private int savedGuiScale = 1;

    public void updateModules(List<ModuleStructure> newModules, ModuleCategory category) {
        if (category == currentCategory) {
            return;
        }

        if (!modules.isEmpty()) {
            oldModules = new ArrayList<>(modules);
            oldModuleAnimations = new HashMap<>(moduleAnimations);
            oldModuleDisplayScroll = moduleDisplayScroll;
            isCategoryTransitioning = true;
            categoryTransitionStartTime = System.currentTimeMillis();
            categoryTransitionProgress = 0f;
        }

        currentCategory = category;
        modules = newModules;
        moduleTargetScroll = moduleDisplayScroll = 0;
        moduleAnimations.clear();
        moduleAnimStartTimes.clear();

        long currentTime = System.currentTimeMillis();
        long delayBase = (long) (CATEGORY_TRANSITION_DURATION * 0.3f);
        for (int i = 0; i < modules.size(); i++) {
            ModuleStructure mod = modules.get(i);
            moduleAnimations.put(mod, 0f);
            moduleAnimStartTimes.put(mod, currentTime + delayBase + i * 25L);
        }

        if (!modules.isEmpty() && (selectedModule == null || !modules.contains(selectedModule))) {
            selectModule(modules.get(0));
        } else if (modules.isEmpty()) {
            selectedModule = null;
            settingComponents.clear();
        }
    }

    public void selectModule(ModuleStructure module) {
        if (module == selectedModule) {
            return;
        }

        selectedModule = module;
        settingTargetScroll = settingDisplayScroll = 0;
        settingComponents.clear();
        settingAnimations.clear();
        settingAnimStartTimes.clear();
        visibilityAnimations.clear();
        heightAnimations.clear();

        if (module == null) return;

        new SettingComponentAdder().addSettingComponent(module.settings(), settingComponents);
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent comp = settingComponents.get(i);
            settingAnimations.put(comp, 0f);
            settingAnimStartTimes.put(comp, currentTime + i * 25L);
            boolean visible = comp.getSetting().isVisible();
            visibilityAnimations.put(comp, visible ? 1f : 0f);
            heightAnimations.put(comp, visible ? 1f : 0f);
        }
    }

    private void updateCategoryTransition() {
        if (!isCategoryTransitioning) return;

        long elapsed = System.currentTimeMillis() - categoryTransitionStartTime;
        float progress = Math.min(1f, elapsed / CATEGORY_TRANSITION_DURATION);
        categoryTransitionProgress = easeOutCubic(progress);

        if (progress >= 1f) {
            isCategoryTransitioning = false;
            oldModules.clear();
            oldModuleAnimations.clear();
            categoryTransitionProgress = 1f;
        }
    }

    private void updateModuleAnimations() {
        long currentTime = System.currentTimeMillis();
        for (ModuleStructure mod : modules) {
            Long startTime = moduleAnimStartTimes.get(mod);
            if (startTime == null) continue;

            float elapsed = currentTime - startTime;
            float progress = Math.min(1f, Math.max(0f, elapsed / MODULE_ANIM_DURATION));
            progress = easeOutCubic(progress);
            moduleAnimations.put(mod, progress);
        }
    }

    private void updateSettingAnimations() {
        long currentTime = System.currentTimeMillis();
        for (AbstractSettingComponent comp : settingComponents) {
            Long startTime = settingAnimStartTimes.get(comp);
            if (startTime == null) continue;

            float elapsed = currentTime - startTime;
            float progress = Math.min(1f, Math.max(0f, elapsed / SETTING_ANIM_DURATION));
            progress = easeOutCubic(progress);
            settingAnimations.put(comp, progress);
        }
    }

    private void updateVisibilityAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastVisibilityUpdateTime) / 1000f, 0.1f);
        lastVisibilityUpdateTime = currentTime;

        for (AbstractSettingComponent comp : settingComponents) {
            boolean isVisible = comp.getSetting().isVisible();
            float currentVisAnim = visibilityAnimations.getOrDefault(comp, isVisible ? 1f : 0f);
            float currentHeightAnim = heightAnimations.getOrDefault(comp, isVisible ? 1f : 0f);

            float visTarget = isVisible ? 1f : 0f;
            float heightTarget = isVisible ? 1f : 0f;

            float heightDiff = heightTarget - currentHeightAnim;
            if (Math.abs(heightDiff) < 0.001f) {
                heightAnimations.put(comp, heightTarget);
            } else {
                float newValue = currentHeightAnim + heightDiff * HEIGHT_ANIM_SPEED * deltaTime;
                heightAnimations.put(comp, newValue);
            }

            float visDiff = visTarget - currentVisAnim;
            if (Math.abs(visDiff) < 0.001f) {
                visibilityAnimations.put(comp, visTarget);
            } else {
                float newValue = currentVisAnim + visDiff * VISIBILITY_ANIM_SPEED * deltaTime;
                visibilityAnimations.put(comp, newValue);
            }
        }

        correctScrollPosition();
    }

    private void correctScrollPosition() {
        if (lastSettingsPanelHeight <= 0) return;

        float maxScroll = Math.max(0, calculateTotalSettingHeight() - lastSettingsPanelHeight + 45);
        if (settingTargetScroll < -maxScroll) {
            settingTargetScroll = -maxScroll;
        }
        if (settingDisplayScroll < -maxScroll) {
            settingDisplayScroll = -maxScroll;
        }
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
    }

    private float easeInCubic(float x) {
        return x * x * x;
    }

    private float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1 - x, 4);
    }

    public void updateScroll(float delta, float scrollSpeed) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);
        lastScrollUpdateTime = currentTime;

        moduleDisplayScroll = smoothScrollDelta(moduleDisplayScroll, moduleTargetScroll, deltaTime);
        settingDisplayScroll = smoothScrollDelta(settingDisplayScroll, settingTargetScroll, deltaTime);
    }

    private double smoothScrollDelta(double current, double target, float deltaTime) {
        double diff = target - current;
        if (Math.abs(diff) < 0.5) return target;
        return current + diff * SCROLL_SPEED * deltaTime;
    }

    public void updateScrollFades(float delta, float scrollSpeed, float moduleListHeight, float settingsPanelHeight) {
        lastSettingsPanelHeight = settingsPanelHeight;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);

        float maxModuleScroll = Math.max(0, modules.size() * 24f - moduleListHeight + 10);
        float maxSettingScroll = Math.max(0, calculateTotalSettingHeight() - settingsPanelHeight + 45);

        moduleScrollTopFade = updateFadeDelta(moduleScrollTopFade, moduleDisplayScroll < -0.5f, deltaTime);
        moduleScrollBottomFade = updateFadeDelta(moduleScrollBottomFade, moduleDisplayScroll > -maxModuleScroll + 0.5f && maxModuleScroll > 0, deltaTime);
        settingScrollTopFade = updateFadeDelta(settingScrollTopFade, settingDisplayScroll < -0.5f, deltaTime);
        settingScrollBottomFade = updateFadeDelta(settingScrollBottomFade, settingDisplayScroll > -maxSettingScroll + 0.5f && maxSettingScroll > 0, deltaTime);
    }

    private float updateFadeDelta(float current, boolean condition, float deltaTime) {
        float target = condition ? 1f : 0f;
        float diff = target - current;
        if (Math.abs(diff) < 0.01f) return target;
        return current + diff * FADE_SPEED * deltaTime;
    }

    private float getComponentBaseHeight(AbstractSettingComponent c) {
        if (c instanceof SelectComponent) return ((SelectComponent) c).getTotalHeight();
        if (c instanceof MultiSelectComponent) return ((MultiSelectComponent) c).getTotalHeight();
        if (c instanceof ColorComponent) return ((ColorComponent) c).getTotalHeight();
        return SETTING_HEIGHT;
    }

    public float calculateTotalSettingHeight() {
        float total = 0;
        for (AbstractSettingComponent c : settingComponents) {
            float heightAnim = heightAnimations.getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            if (heightAnim <= 0.001f) continue;

            float baseHeight = getComponentBaseHeight(c);
            total += (baseHeight + SETTING_SPACING) * heightAnim;
        }
        return total;
    }

    public void renderModuleList(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY, int guiScale, float alphaMultiplier) {
        updateCategoryTransition();
        updateModuleAnimations();

        int panelAlpha = (int) (15 * alphaMultiplier);
        int outlineAlpha = (int) (215 * alphaMultiplier);
        Render2D.rect(x, y, width, height, new Color(64, 64, 64, panelAlpha).getRGB(), MODULE_LIST_CORNER_RADIUS);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), MODULE_LIST_CORNER_RADIUS);

        float topInset = CORNER_INSET;
        float bottomInset = CORNER_INSET + 2;
        float sideInset = CORNER_INSET;

        Scissor.enable(
                x + sideInset,
                y + topInset,
                width - sideInset * 2,
                height - topInset - bottomInset,
                guiScale
        );

        if (isCategoryTransitioning && !oldModules.isEmpty()) {
            float oldAlpha = (1f - categoryTransitionProgress) * alphaMultiplier;
            float oldOffsetX = easeInCubic(categoryTransitionProgress) * -CATEGORY_SLIDE_DISTANCE;
            float oldScale = 1f - categoryTransitionProgress * 0.1f;

            renderModuleItems(context, oldModules, oldModuleAnimations, x, y, width, height, mouseX, mouseY, oldAlpha, oldOffsetX, oldScale, (float) oldModuleDisplayScroll, false, topInset, bottomInset);
        }

        float newAlpha;
        float newOffsetX;
        float newScale;

        if (isCategoryTransitioning) {
            float entryProgress = Math.max(0f, (categoryTransitionProgress - 0.2f) / 0.8f);
            entryProgress = easeOutQuart(entryProgress);
            newAlpha = entryProgress * alphaMultiplier;
            newOffsetX = (1f - entryProgress) * CATEGORY_SLIDE_DISTANCE;
            newScale = 0.9f + entryProgress * 0.1f;
        } else {
            newAlpha = alphaMultiplier;
            newOffsetX = 0f;
            newScale = 1f;
        }

        renderModuleItems(context, modules, moduleAnimations, x, y, width, height, mouseX, mouseY, newAlpha, newOffsetX, newScale, (float) moduleDisplayScroll, true, topInset, bottomInset);

        Scissor.disable();

        renderScrollFade(x, y + topInset, width, height - topInset - bottomInset, moduleScrollTopFade * alphaMultiplier, moduleScrollBottomFade * alphaMultiplier, 80, 15);
    }

    private void renderModuleItems(DrawContext context, List<ModuleStructure> moduleList, Map<ModuleStructure, Float> animations, float x, float y, float width, float height, float mouseX, float mouseY, float alphaMultiplier, float offsetX, float scale, float scrollOffset, boolean interactive, float topInset, float bottomInset) {
        if (alphaMultiplier <= 0.01f) return;

        float startY = y + topInset + 2f + scrollOffset;
        float centerY = y + height / 2f;
        float visibleTop = y + topInset;
        float visibleBottom = y + height - bottomInset;

        for (int i = 0; i < moduleList.size(); i++) {
            ModuleStructure module = moduleList.get(i);
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);

            if (modY + MODULE_ITEM_HEIGHT < visibleTop || modY > visibleBottom) continue;

            float itemProgress = animations.getOrDefault(module, 1f);
            float combinedAlpha = itemProgress * alphaMultiplier;

            if (combinedAlpha <= 0.01f) continue;

            float itemAnimOffset = (1f - itemProgress) * 20f;

            float scaledModY = centerY + (modY - centerY) * scale;
            float scaledHeight = MODULE_ITEM_HEIGHT * scale;

            float animX = x + 3 + offsetX + itemAnimOffset;

            boolean selected = interactive && module == selectedModule;
            boolean hovered = interactive && mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT;

            Color bg;
            if (selected) {
                bg = new Color(100, 150, 200, (int) (60 * combinedAlpha));
            } else if (hovered) {
                bg = new Color(100, 100, 100, (int) (40 * combinedAlpha));
            } else {
                bg = new Color(64, 64, 64, (int) (25 * combinedAlpha));
            }

            float scaledWidth = (width - 6) * scale;

            Render2D.rect(animX, scaledModY, scaledWidth, scaledHeight, bg.getRGB(), 5);

            if (selected) {
                Render2D.outline(animX, scaledModY, scaledWidth, scaledHeight, 0.5f, new Color(100, 150, 200, (int) (100 * combinedAlpha)).getRGB(), 5);
            }

            String name = module.getName();
            if (interactive && module == bindingModule) {
                name += " [...]";
            } else if (module.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
                String key = GLFW.glfwGetKeyName(module.getKey(), 0);
                name += " [" + (key != null ? key.toUpperCase() : "KEY" + module.getKey()) + "]";
            }

            Color textColor;
            if (module.isState()) {
                textColor = new Color(255, 255, 255, (int) (255 * combinedAlpha));
            } else {
                textColor = new Color(128, 128, 128, (int) (180 * combinedAlpha));
            }

            float textY = scaledModY + (scaledHeight - 6f * scale) / 2f;
            Fonts.BOLD.draw(name, animX + 5, textY, 6 * scale, textColor.getRGB());
        }
    }

    public void renderSettingsPanel(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        updateSettingAnimations();
        updateVisibilityAnimations();
        savedGuiScale = guiScale;

        int panelAlpha = (int) (15 * alphaMultiplier);
        int outlineAlpha = (int) (215 * alphaMultiplier);
        Render2D.rect(x, y, width, height, new Color(64, 64, 64, panelAlpha).getRGB(), SETTINGS_PANEL_CORNER_RADIUS);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), SETTINGS_PANEL_CORNER_RADIUS);

        if (selectedModule == null) {
            String text = "Select a module";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, (int) (150 * alphaMultiplier)).getRGB());
            return;
        }

        Fonts.BOLD.draw(selectedModule.getName(), x + 8, y + 8, 7, new Color(255, 255, 255, (int) (200 * alphaMultiplier)).getRGB());
        String desc = selectedModule.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Fonts.BOLD.draw(desc.length() > 52   ? desc.substring(0, 55) + "..." : desc, x + 15, y + 20, 5, new Color(128, 128, 128, (int) (150 * alphaMultiplier)).getRGB());
            Fonts.GUI_ICONS.draw("C", x + 8, y + 20, 6, new Color(128, 128, 128, (int) (150 * alphaMultiplier)).getRGB());
        }
        Render2D.rect(x + 8, y + 30, width - 16, 1.25f, new Color(64, 64, 64, (int) (64 * alphaMultiplier)).getRGB(), 10);

        float sideInset = CORNER_INSET;
        float bottomInset = CORNER_INSET + 3;

        float clipY = y + 31;
        float clipH = height - 31 - bottomInset;

        float clipX = x + sideInset;
        float clipW = width - sideInset * 2;

        Scissor.enable(clipX, clipY, clipW, clipH, guiScale);

        List<Float> finalYPositions = new ArrayList<>();
        List<Float> animatedHeights = new ArrayList<>();
        float posY = y + 38f + (float) settingDisplayScroll;

        for (AbstractSettingComponent c : settingComponents) {
            float heightAnim = heightAnimations.getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (heightAnim <= 0.001f) {
                finalYPositions.add(null);
                animatedHeights.add(0f);
                continue;
            }

            finalYPositions.add(posY);

            float baseHeight = getComponentBaseHeight(c);
            float layoutHeight = baseHeight * heightAnim;
            animatedHeights.add(layoutHeight);
            posY += layoutHeight + SETTING_SPACING * heightAnim;
        }

        float visibleTop = clipY;
        float visibleBottom = clipY + clipH;

        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent c = settingComponents.get(i);
            Float startY = finalYPositions.get(i);

            if (startY == null) continue;

            float visAnim = visibilityAnimations.getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            float heightAnim = heightAnimations.getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (visAnim <= 0.001f && heightAnim <= 0.001f) continue;

            float animatedHeight = animatedHeights.get(i);

            float progress = settingAnimations.getOrDefault(c, 1f);
            float componentAlpha = progress * visAnim * alphaMultiplier;

            c.position(x + 8, startY);
            c.size(width - 16f, SETTING_HEIGHT);
            c.setAlphaMultiplier(componentAlpha);

            if (startY + animatedHeight >= visibleTop && startY <= visibleBottom && componentAlpha > 0.01f) {
                float itemClipTop = Math.max(startY, visibleTop);
                float itemClipBottom = Math.min(startY + animatedHeight, visibleBottom);
                float itemClipHeight = itemClipBottom - itemClipTop;

                if (itemClipHeight > 0.5f) {
                    Scissor.enable(clipX, itemClipTop, clipW, itemClipHeight, guiScale);
                    context.getMatrices().pushMatrix();
                    c.render(context, (int) mouseX, (int) mouseY, delta);
                    context.getMatrices().popMatrix();
                    Scissor.disable();
                }
            }
        }

        Scissor.disable();

        boolean hasVisibleSettings = false;
        for (AbstractSettingComponent c : settingComponents) {
            float visAnim = visibilityAnimations.getOrDefault(c, 0f);
            if (visAnim > 0.01f) {
                hasVisibleSettings = true;
                break;
            }
        }

        if (!hasVisibleSettings) {
            String text = "No settings";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f + 10f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, (int) (150 * alphaMultiplier)).getRGB());
        }

        renderScrollFade(x + sideInset, clipY, width - sideInset * 2, clipH, settingScrollTopFade * alphaMultiplier, settingScrollBottomFade * alphaMultiplier, 60, 12);
    }

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade, int alpha, int size) {
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = alpha * topFade * (1f - i / (float) size);
                Render2D.rect(x, y + i, w, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = alpha * bottomFade * (i / (float) size);
                Render2D.rect(x, y + h - size + i, w, 1, new Color(20, 20, 20, (int) fadeAlpha).getRGB(), 0);
            }
        }
    }

    public ModuleStructure getModuleAtPosition(double mouseX, double mouseY, float listX, float listY, float listWidth, float listHeight) {
        if (isCategoryTransitioning) return null;
        if (mouseX < listX || mouseX > listX + listWidth || mouseY < listY || mouseY > listY + listHeight) return null;

        float startY = listY + CORNER_INSET + 2f + (float) moduleDisplayScroll;
        for (int i = 0; i < modules.size(); i++) {
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);
            if (mouseX >= listX + 3 && mouseX <= listX + listWidth - 3 && mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT) {
                return modules.get(i);
            }
        }
        return null;
    }

    public void handleModuleScroll(double vertical, float listHeight) {
        if (isCategoryTransitioning) return;
        float effectiveHeight = listHeight - CORNER_INSET * 2 - 2;
        float maxScroll = Math.max(0, modules.size() * 24f - effectiveHeight + 10);
        moduleTargetScroll = Math.max(-maxScroll, Math.min(0, moduleTargetScroll + vertical * 25));
    }

    public void handleSettingScroll(double vertical, float panelHeight) {
        float effectiveHeight = panelHeight - 31 - CORNER_INSET - 3;
        float maxScroll = Math.max(0, calculateTotalSettingHeight() - effectiveHeight + 10);
        settingTargetScroll = Math.max(-maxScroll, Math.min(0, settingTargetScroll + vertical * 25));
    }

    public void tick() {
        settingComponents.forEach(AbstractSettingComponent::tick);
    }

    public boolean isTransitioning() {
        return isCategoryTransitioning;
    }
}