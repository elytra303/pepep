package rich.screens.clickgui.impl.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.modules.module.category.ModuleCategory;
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

    private static final float STATE_BALL_SIZE = 3f;
    private static final float STATE_TEXT_OFFSET = 6f;
    private static final float STATE_ANIM_SPEED = 10f;

    private static final float HIGHLIGHT_DURATION = 2000f;
    private static final float ICON_ANIM_SPEED = 10f;
    private static final float FAVORITE_ANIM_SPEED = 8f;
    private static final float POSITION_ANIM_SPEED = 6f;

    private static final float BIND_BOX_HEIGHT = 9f;
    private static final float BIND_BOX_MIN_WIDTH = 18f;
    private static final float BIND_BOX_PADDING = 6f;
    private static final float BIND_WIDTH_ANIM_SPEED = 12f;

    private List<ModuleStructure> modules = new ArrayList<>();
    private List<ModuleStructure> displayModules = new ArrayList<>();
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

    private Map<ModuleStructure, Float> hoverAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> stateAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> selectedIconAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> favoriteAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> positionAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> moduleAlphaAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> bindBoxWidthAnimations = new HashMap<>();
    private Map<ModuleStructure, Float> bindBoxAlphaAnimations = new HashMap<>();
    private Map<ModuleStructure, String> lastBindTexts = new HashMap<>();

    private float selectedPulseAnimation = 0f;
    private long lastHoverUpdateTime = System.currentTimeMillis();
    private long lastStateUpdateTime = System.currentTimeMillis();
    private long lastIconUpdateTime = System.currentTimeMillis();
    private long lastFavoriteUpdateTime = System.currentTimeMillis();
    private long lastBindUpdateTime = System.currentTimeMillis();

    private ModuleStructure highlightedModule = null;
    private long highlightStartTime = 0;
    private float highlightAnimation = 0f;

    private boolean scrollToModule = false;
    private ModuleStructure scrollTargetModule = null;

    private static final float HOVER_ANIM_SPEED = 8f;
    private static final float PULSE_SPEED = 5.5f;

    private ModuleCategory currentCategory = null;

    private double moduleTargetScroll = 0, moduleDisplayScroll = 0;
    private double settingTargetScroll = 0, settingDisplayScroll = 0;
    private float moduleScrollTopFade = 0f, moduleScrollBottomFade = 0f;
    private float settingScrollTopFade = 0f, settingScrollBottomFade = 0f;

    private float lastSettingsPanelHeight = 0f;
    private float lastModuleListHeight = 0f;

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

    private float lastMouseX = 0;
    private float lastMouseY = 0;
    private float lastListX = 0;
    private float lastListY = 0;
    private float lastListWidth = 0;
    private float lastListHeight = 0;

    private Set<ModuleStructure> modulesWithSettings = new HashSet<>();

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
        rebuildDisplayList();

        moduleTargetScroll = moduleDisplayScroll = 0;
        moduleAnimations.clear();
        moduleAnimStartTimes.clear();
        hoverAnimations.clear();
        stateAnimations.clear();
        selectedIconAnimations.clear();
        modulesWithSettings.clear();
        bindBoxWidthAnimations.clear();
        bindBoxAlphaAnimations.clear();
        lastBindTexts.clear();

        long currentTime = System.currentTimeMillis();
        long delayBase = (long) (CATEGORY_TRANSITION_DURATION * 0.3f);
        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure mod = displayModules.get(i);
            moduleAnimations.put(mod, 0f);
            moduleAnimStartTimes.put(mod, currentTime + delayBase + i * 25L);
            hoverAnimations.put(mod, 0f);
            stateAnimations.put(mod, mod.isState() ? 1f : 0f);
            selectedIconAnimations.put(mod, mod == selectedModule ? 1f : 0f);
            favoriteAnimations.put(mod, mod.isFavorite() ? 1f : 0f);
            positionAnimations.put(mod, 1f);
            moduleAlphaAnimations.put(mod, 1f);

            if (hasModuleSettings(mod)) {
                modulesWithSettings.add(mod);
            }
        }

        if (scrollToModule && scrollTargetModule != null && displayModules.contains(scrollTargetModule)) {
            scrollToModuleAndHighlight(scrollTargetModule);
            scrollToModule = false;
            scrollTargetModule = null;
        } else if (!displayModules.isEmpty() && (selectedModule == null || !displayModules.contains(selectedModule))) {
            selectModule(displayModules.get(0));
        } else if (displayModules.isEmpty()) {
            selectedModule = null;
            settingComponents.clear();
        }
    }

    private void rebuildDisplayList() {
        displayModules.clear();

        List<ModuleStructure> favorites = new ArrayList<>();
        List<ModuleStructure> nonFavorites = new ArrayList<>();

        for (ModuleStructure mod : modules) {
            if (mod.isFavorite()) {
                favorites.add(mod);
            } else {
                nonFavorites.add(mod);
            }
        }

        displayModules.addAll(favorites);
        displayModules.addAll(nonFavorites);
    }

    public void toggleFavorite(ModuleStructure module) {
        if (module == null) return;

        module.switchFavorite();

        int oldIndex = displayModules.indexOf(module);
        rebuildDisplayList();
        int newIndex = displayModules.indexOf(module);

        if (oldIndex != newIndex) {
            for (ModuleStructure mod : displayModules) {
                if (!positionAnimations.containsKey(mod) || positionAnimations.get(mod) >= 0.99f) {
                    positionAnimations.put(mod, 0f);
                }
                if (!moduleAlphaAnimations.containsKey(mod)) {
                    moduleAlphaAnimations.put(mod, 1f);
                }
            }
            moduleAlphaAnimations.put(module, 0f);
        }
    }

    private boolean hasModuleSettings(ModuleStructure module) {
        if (module == null) return false;
        var settings = module.settings();
        return settings != null && !settings.isEmpty();
    }

    public void selectModuleFromSearch(ModuleStructure module) {
        scrollToModule = true;
        scrollTargetModule = module;
    }

    public void scrollToModuleAndHighlight(ModuleStructure module) {
        if (module == null || !displayModules.contains(module)) return;

        selectModule(module);

        int moduleIndex = displayModules.indexOf(module);
        if (moduleIndex >= 0 && lastModuleListHeight > 0) {
            float moduleY = moduleIndex * (MODULE_ITEM_HEIGHT + 2);
            float visibleHeight = lastModuleListHeight - CORNER_INSET * 2 - 4;
            float centerOffset = (visibleHeight - MODULE_ITEM_HEIGHT) / 2f;
            float targetScroll = -(moduleY - centerOffset);

            float maxScroll = Math.max(0, displayModules.size() * (MODULE_ITEM_HEIGHT + 2) - visibleHeight);
            targetScroll = Math.max(-maxScroll, Math.min(0, targetScroll));

            moduleTargetScroll = targetScroll;
        }

        highlightedModule = module;
        highlightStartTime = System.currentTimeMillis();
        highlightAnimation = 1f;
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

    private void updateHighlightAnimation() {
        if (highlightedModule == null) return;

        long elapsed = System.currentTimeMillis() - highlightStartTime;

        if (elapsed >= HIGHLIGHT_DURATION) {
            long fadeElapsed = elapsed - (long) HIGHLIGHT_DURATION;
            float fadeProgress = fadeElapsed / 500f;

            if (fadeProgress >= 1f) {
                highlightedModule = null;
                highlightAnimation = 0f;
            } else {
                highlightAnimation = 1f - fadeProgress;
            }
        } else {
            highlightAnimation = 1f;
        }
    }

    private void updateFavoriteAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastFavoriteUpdateTime) / 1000f, 0.1f);
        lastFavoriteUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            float currentFavAnim = favoriteAnimations.getOrDefault(module, 0f);
            float targetFavAnim = module.isFavorite() ? 1f : 0f;

            float diff = targetFavAnim - currentFavAnim;
            if (Math.abs(diff) < 0.001f) {
                favoriteAnimations.put(module, targetFavAnim);
            } else {
                favoriteAnimations.put(module, currentFavAnim + diff * FAVORITE_ANIM_SPEED * deltaTime);
            }

            float currentPosAnim = positionAnimations.getOrDefault(module, 1f);
            if (currentPosAnim < 1f) {
                float newPosAnim = currentPosAnim + POSITION_ANIM_SPEED * deltaTime;
                positionAnimations.put(module, Math.min(1f, newPosAnim));
            }

            float currentAlphaAnim = moduleAlphaAnimations.getOrDefault(module, 1f);
            if (currentAlphaAnim < 1f) {
                float newAlphaAnim = currentAlphaAnim + POSITION_ANIM_SPEED * deltaTime;
                moduleAlphaAnimations.put(module, Math.min(1f, newAlphaAnim));
            }
        }
    }

    private void updateBindAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastBindUpdateTime) / 1000f, 0.1f);
        lastBindUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            boolean isBinding = module == bindingModule;
            int key = module.getKey();
            boolean hasBind = key != GLFW.GLFW_KEY_UNKNOWN && key != -1;
            boolean shouldShow = hasBind || isBinding;

            float currentAlpha = bindBoxAlphaAnimations.getOrDefault(module, 0f);
            float targetAlpha = shouldShow ? 1f : 0f;

            float diff = targetAlpha - currentAlpha;
            if (Math.abs(diff) < 0.001f) {
                bindBoxAlphaAnimations.put(module, targetAlpha);
            } else {
                bindBoxAlphaAnimations.put(module, currentAlpha + diff * BIND_WIDTH_ANIM_SPEED * deltaTime);
            }
        }
    }

    private void updateSelectedIconAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastIconUpdateTime) / 1000f, 0.1f);
        lastIconUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            float currentAnim = selectedIconAnimations.getOrDefault(module, 0f);
            float targetAnim = (module == selectedModule) ? 1f : 0f;

            float diff = targetAnim - currentAnim;
            if (Math.abs(diff) < 0.001f) {
                selectedIconAnimations.put(module, targetAnim);
            } else {
                selectedIconAnimations.put(module, currentAnim + diff * ICON_ANIM_SPEED * deltaTime);
            }
        }
    }

    private void updateStateAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastStateUpdateTime) / 1000f, 0.1f);
        lastStateUpdateTime = currentTime;

        for (ModuleStructure module : displayModules) {
            float currentAnim = stateAnimations.getOrDefault(module, module.isState() ? 1f : 0f);
            float targetAnim = module.isState() ? 1f : 0f;

            float diff = targetAnim - currentAnim;
            if (Math.abs(diff) < 0.001f) {
                stateAnimations.put(module, targetAnim);
            } else {
                stateAnimations.put(module, currentAnim + diff * STATE_ANIM_SPEED * deltaTime);
            }
        }
    }

    private void updateHoverAnimations(float mouseX, float mouseY, float listX, float listY, float listWidth, float listHeight) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastHoverUpdateTime) / 1000f, 0.1f);
        lastHoverUpdateTime = currentTime;

        selectedPulseAnimation += deltaTime * PULSE_SPEED;
        if (selectedPulseAnimation > Math.PI * 2) {
            selectedPulseAnimation -= (float) (Math.PI * 2);
        }

        float startY = listY + CORNER_INSET + 2f + (float) moduleDisplayScroll;

        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure module = displayModules.get(i);
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);

            boolean isHovered = !isCategoryTransitioning &&
                    mouseX >= listX + 3 && mouseX <= listX + listWidth - 3 &&
                    mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT &&
                    mouseY >= listY && mouseY <= listY + listHeight;

            float currentHover = hoverAnimations.getOrDefault(module, 0f);
            float targetHover = isHovered ? 1f : 0f;

            float diff = targetHover - currentHover;
            if (Math.abs(diff) < 0.001f) {
                hoverAnimations.put(module, targetHover);
            } else {
                hoverAnimations.put(module, currentHover + diff * HOVER_ANIM_SPEED * deltaTime);
            }
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
        for (ModuleStructure mod : displayModules) {
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
        lastModuleListHeight = moduleListHeight;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);

        float maxModuleScroll = Math.max(0, displayModules.size() * 24f - moduleListHeight + 10);
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
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastListX = x;
        lastListY = y;
        lastListWidth = width;
        lastListHeight = height;

        updateCategoryTransition();
        updateModuleAnimations();
        updateStateAnimations();
        updateSelectedIconAnimations();
        updateFavoriteAnimations();
        updateBindAnimations();
        updateHighlightAnimation();
        updateHoverAnimations(mouseX, mouseY, x, y, width, height);

        int panelAlpha = (int) (15 * alphaMultiplier);
        int outlineAlpha = (int) (215 * alphaMultiplier);
        Render2D.rect(x, y, width, height, new Color(64, 64, 64, panelAlpha).getRGB(), MODULE_LIST_CORNER_RADIUS);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), MODULE_LIST_CORNER_RADIUS);

        float topInset = CORNER_INSET;
        float bottomInset = CORNER_INSET;
        float sideInset = CORNER_INSET;

        Scissor.enable(
                x + sideInset,
                y + topInset - 1.5f,
                width - sideInset * 2,
                height - topInset - bottomInset + 3,
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

        renderModuleItems(context, displayModules, moduleAnimations, x, y, width, height, mouseX, mouseY, newAlpha, newOffsetX, newScale, (float) moduleDisplayScroll, true, topInset, bottomInset);

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
            float posAnim = positionAnimations.getOrDefault(module, 1f);
            float alphaAnim = moduleAlphaAnimations.getOrDefault(module, 1f);
            float combinedAlpha = itemProgress * alphaMultiplier * alphaAnim;

            if (combinedAlpha <= 0.01f) continue;

            float itemAnimOffset = (1f - itemProgress) * 20f;
            float posAnimOffset = (1f - easeOutCubic(posAnim)) * 15f;

            float scaledModY = centerY + (modY - centerY) * scale;
            float scaledHeight = MODULE_ITEM_HEIGHT * scale;

            float animX = x + 3 + offsetX + itemAnimOffset + posAnimOffset;

            boolean selected = interactive && module == selectedModule;
            boolean isHighlighted = interactive && module == highlightedModule && highlightAnimation > 0.01f;
            float hoverAnim = interactive ? hoverAnimations.getOrDefault(module, 0f) : 0f;
            float stateAnim = interactive ? stateAnimations.getOrDefault(module, module.isState() ? 1f : 0f) : (module.isState() ? 1f : 0f);
            float selectedIconAnim = interactive ? selectedIconAnimations.getOrDefault(module, 0f) : 0f;
            float favoriteAnim = interactive ? favoriteAnimations.getOrDefault(module, 0f) : 0f;
            boolean hasSettings = modulesWithSettings.contains(module);

            int baseBgAlpha = 25;
            int hoverBgAlpha = 45;
            int selectedBgAlpha = 55;

            int bgAlpha;
            int bgColor;

            if (selected) {
                bgAlpha = (int) ((selectedBgAlpha + hoverAnim * 10) * combinedAlpha);
                bgColor = new Color(71, 71, 71, bgAlpha).getRGB();
            } else {
                bgAlpha = (int) ((baseBgAlpha + (hoverBgAlpha - baseBgAlpha) * hoverAnim) * combinedAlpha);
                int gray = (int) (64 + 36 * hoverAnim);
                bgColor = new Color(gray, gray, gray, bgAlpha).getRGB();
            }

            float scaledWidth = (width - 6) * scale;

            Render2D.rect(animX, scaledModY, scaledWidth, scaledHeight, bgColor, 5);

            if (selected) {
                float pulseValue = (float) (Math.sin(selectedPulseAnimation) * 0.5 + 0.5);

                float highlightBoost = isHighlighted ? highlightAnimation * 0.5f : 0f;

                int baseOutlineAlpha = (int) (80 + 80 * highlightBoost);
                int pulseOutlineAlpha = (int) (40 + 40 * highlightBoost);
                int outlineAlpha = (int) ((baseOutlineAlpha + pulseOutlineAlpha * pulseValue) * combinedAlpha);

                int baseColorValue = (int) (80 + 50 * highlightBoost);
                int outlineColorValue = (int) (baseColorValue + 30 * pulseValue);
                int outlineG = (int) (80 + 20 * pulseValue + 40 * highlightBoost);
                int outlineB = (int) (80 + 20 * pulseValue + 40 * highlightBoost);

                Render2D.outline(animX, scaledModY, scaledWidth, scaledHeight, 0.5f,
                        new Color(Math.min(255, outlineColorValue), Math.min(255, outlineG), Math.min(255, outlineB), outlineAlpha).getRGB(), 5);
            } else if (hoverAnim > 0.01f) {
                int outlineAlpha = (int) (60 * hoverAnim * combinedAlpha);
                Render2D.outline(animX, scaledModY, scaledWidth, scaledHeight, 0.5f,
                        new Color(120, 120, 120, outlineAlpha).getRGB(), 5);
            }

            float stateTextOffset = stateAnim * STATE_TEXT_OFFSET;

            if (stateAnim > 0.01f) {
                float ballAlpha = stateAnim * 200 * combinedAlpha;
                float ballX = animX + 4;
                float ballY = scaledModY + (scaledHeight - STATE_BALL_SIZE * scale) / 2f + 1F;
                Render2D.rect(ballX, ballY, STATE_BALL_SIZE * scale, STATE_BALL_SIZE * scale,
                        new Color(255, 255, 255, (int) ballAlpha).getRGB(),
                        STATE_BALL_SIZE * scale / 2f);
            }

            String name = module.getName();

            int baseGray = 128;
            int targetWhite = 255;
            int textBrightness = (int) (baseGray + (targetWhite - baseGray) * stateAnim);
            int textAlphaValue = (int) ((180 + 75 * stateAnim) * combinedAlpha);

            if (hoverAnim > 0.01f && stateAnim < 0.99f) {
                textBrightness = (int) (textBrightness + (40 * hoverAnim * (1 - stateAnim)));
                textAlphaValue = (int) (textAlphaValue + (40 * hoverAnim * (1 - stateAnim)));
            }

            if (isHighlighted) {
                textBrightness = (int) Math.min(255, textBrightness + 30 * highlightAnimation);
            }

            Color textColor = new Color(textBrightness, textBrightness, textBrightness, Math.min(255, textAlphaValue));

            float textX = animX + 5 + stateTextOffset;
            float textY = scaledModY + (scaledHeight - 6f * scale) / 2f;
            Fonts.BOLD.draw(name, textX, textY, 6 * scale, textColor.getRGB());

            if (interactive) {
                renderBindBox(module, animX, scaledModY, scaledWidth, scaledHeight, scale, combinedAlpha, stateTextOffset);

                float iconBaseX = animX + scaledWidth - 14;
                float iconY = scaledModY + (scaledHeight - 8f * scale) / 2f;

                float starX;
                if (hasSettings) {
                    starX = iconBaseX - 12;
                } else {
                    starX = iconBaseX;
                }

                int starGray = 50;
                int starR = (int) (starGray + (255 - starGray) * favoriteAnim);
                int starG = (int) (starGray + (215 - starGray) * favoriteAnim);
                int starB = (int) (starGray + (0 - starGray) * favoriteAnim);
                float starAlpha = (80 + 120 * favoriteAnim + 55 * hoverAnim) * combinedAlpha;

                Fonts.GUI_ICONS.draw("D", starX, iconY + 1, 8 * scale, new Color(starR, starG, starB, (int) starAlpha).getRGB());

                if (hasSettings) {
                    if (selectedIconAnim > 0.01f) {
                        float gearAlpha = (150 + 50 * (isHighlighted ? highlightAnimation : 0f)) * selectedIconAnim * combinedAlpha;
                        Fonts.GUI_ICONS.draw("B", iconBaseX, iconY + 1, 8 * scale, new Color(200, 200, 200, (int) gearAlpha).getRGB());
                    }

                    if (selectedIconAnim < 0.99f) {
                        float dotsAlpha = 120 * (1f - selectedIconAnim) * combinedAlpha;
                        Fonts.BOLD.draw("...", iconBaseX + 1f, iconY - 1f, 7 * scale, new Color(150, 150, 150, (int) dotsAlpha).getRGB());
                    }
                }
            }
        }
    }

    private void renderBindBox(ModuleStructure module, float moduleX, float moduleY, float moduleWidth, float moduleHeight, float scale, float combinedAlpha, float stateTextOffset) {
        boolean isBinding = module == bindingModule;
        int key = module.getKey();

        float bindAlpha = bindBoxAlphaAnimations.getOrDefault(module, 0f);

        if (bindAlpha <= 0.01f && !isBinding && (key == GLFW.GLFW_KEY_UNKNOWN || key == -1)) {
            return;
        }

        String bindText;
        if (isBinding) {
            bindText = "...";
        } else {
            bindText = getBindDisplayName(key);
        }

        float textWidth = Fonts.BOLD.getWidth(bindText, 5 * scale);
        float targetWidth = Math.max(BIND_BOX_MIN_WIDTH, textWidth + BIND_BOX_PADDING * 2);

        float currentWidth = bindBoxWidthAnimations.getOrDefault(module, targetWidth);

        float widthDiff = targetWidth - currentWidth;
        if (Math.abs(widthDiff) > 0.1f) {
            currentWidth += widthDiff * BIND_WIDTH_ANIM_SPEED * 0.016f;
            bindBoxWidthAnimations.put(module, currentWidth);
        } else {
            currentWidth = targetWidth;
            bindBoxWidthAnimations.put(module, currentWidth);
        }

        float boxHeight = BIND_BOX_HEIGHT * scale;
        float boxWidth = currentWidth * scale * bindAlpha;

        float nameWidth = Fonts.BOLD.getWidth(module.getName(), 6 * scale);
        float boxX = moduleX + 5 + stateTextOffset + nameWidth;
        float boxY = moduleY + (moduleHeight - boxHeight) / 2f + 0.5f;

        float finalAlpha = combinedAlpha * bindAlpha;

        int bgAlpha = (int) (30 * finalAlpha);
        Color bgColor = new Color(50, 50, 55, bgAlpha);

        Render2D.rect(boxX + 3, boxY + 0.5f, boxWidth - 6, boxHeight, bgColor.getRGB(), 3f * scale);

        int outlineAlpha = (int) (60 * finalAlpha);
        Color outlineColor = new Color(80, 80, 85, outlineAlpha);

        Render2D.outline(boxX + 3, boxY + 0.5f, boxWidth - 6, boxHeight, 0.5f, outlineColor.getRGB(), 3f * scale);

        if (bindAlpha > 0.5f) {
            int textAlpha = (int) (160 * finalAlpha);
            Color textColor = new Color(140, 140, 145, textAlpha);

            float textX = boxX + (boxWidth - textWidth) / 2f;
            float textY = boxY + (boxHeight - 5f * scale) / 2f;
            Fonts.BOLD.draw(bindText, textX, textY, 5 * scale, textColor.getRGB());
        }
    }

    private String getBindDisplayName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) return "";

        if (key == BindComponent.SCROLL_UP_BIND) return "Up";
        if (key == BindComponent.SCROLL_DOWN_BIND) return "Dn";
        if (key == BindComponent.MIDDLE_MOUSE_BIND) return "M3";

        String keyName = GLFW.glfwGetKeyName(key, 0);
        if (keyName != null) {
            return keyName.toUpperCase();
        }

        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LS";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RS";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LC";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RC";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LA";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RA";
            case GLFW.GLFW_KEY_SPACE -> "Sp";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Cap";
            case GLFW.GLFW_KEY_ENTER -> "Ent";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bk";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_HOME -> "Hm";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PU";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PD";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Dn";
            case GLFW.GLFW_KEY_LEFT -> "Lt";
            case GLFW.GLFW_KEY_RIGHT -> "Rt";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            default -> "K" + key;
        };
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
            Fonts.BOLD.draw(desc.length() > 52 ? desc.substring(0, 55) + "..." : desc, x + 15, y + 20, 5, new Color(128, 128, 128, (int) (150 * alphaMultiplier)).getRGB());
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
            String text = "This module doesn't have settings";
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
        for (int i = 0; i < displayModules.size(); i++) {
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);
            if (mouseX >= listX + 3 && mouseX <= listX + listWidth - 3 && mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT) {
                return displayModules.get(i);
            }
        }
        return null;
    }

    public boolean isStarClicked(double mouseX, double mouseY, float listX, float listY, float listWidth, float listHeight) {
        if (isCategoryTransitioning) return false;

        float startY = listY + CORNER_INSET + 2f + (float) moduleDisplayScroll;
        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure module = displayModules.get(i);
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);

            if (mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT) {
                float scaledWidth = listWidth - 6;
                float animX = listX + 3;
                boolean hasSettings = modulesWithSettings.contains(module);

                float starX;
                if (hasSettings) {
                    starX = animX + scaledWidth - 14 - 12;
                } else {
                    starX = animX + scaledWidth - 14;
                }

                if (mouseX >= starX && mouseX <= starX + 10) {
                    return true;
                }
            }
        }
        return false;
    }

    public ModuleStructure getModuleForStarClick(double mouseX, double mouseY, float listX, float listY, float listWidth, float listHeight) {
        if (isCategoryTransitioning) return null;

        float startY = listY + CORNER_INSET + 2f + (float) moduleDisplayScroll;
        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure module = displayModules.get(i);
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);

            if (mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT) {
                float scaledWidth = listWidth - 6;
                float animX = listX + 3;
                boolean hasSettings = modulesWithSettings.contains(module);

                float starX;
                if (hasSettings) {
                    starX = animX + scaledWidth - 14 - 12;
                } else {
                    starX = animX + scaledWidth - 14;
                }

                if (mouseX >= starX && mouseX <= starX + 10) {
                    return module;
                }
            }
        }
        return null;
    }

    public void handleModuleScroll(double vertical, float listHeight) {
        if (isCategoryTransitioning) return;
        float effectiveHeight = listHeight - CORNER_INSET * 2 - 2;
        float maxScroll = Math.max(0, displayModules.size() * 24f - effectiveHeight + 10);
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