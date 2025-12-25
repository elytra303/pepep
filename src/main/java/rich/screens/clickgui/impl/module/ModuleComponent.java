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

    private List<ModuleStructure> modules = new ArrayList<>();
    private ModuleStructure selectedModule = null;
    private ModuleStructure bindingModule = null;
    private List<AbstractSettingComponent> settingComponents = new ArrayList<>();

    private Map<ModuleStructure, Float> moduleAnimations = new HashMap<>();
    private Map<ModuleStructure, Long> moduleAnimStartTimes = new HashMap<>();
    private Map<AbstractSettingComponent, Float> settingAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Long> settingAnimStartTimes = new HashMap<>();

    private ModuleCategory currentCategory = null;

    private double moduleTargetScroll = 0, moduleDisplayScroll = 0;
    private double settingTargetScroll = 0, settingDisplayScroll = 0;
    private float moduleScrollTopFade = 0f, moduleScrollBottomFade = 0f;
    private float settingScrollTopFade = 0f, settingScrollBottomFade = 0f;

    private long lastScrollUpdateTime = System.currentTimeMillis();

    private static final float MODULE_ANIM_DURATION = 300f;
    private static final float SETTING_ANIM_DURATION = 200f;
    private static final float SCROLL_SPEED = 12f;
    private static final float FADE_SPEED = 8f;

    public void updateModules(List<ModuleStructure> newModules, ModuleCategory category) {
        if (category == currentCategory) {
            return;
        }

        currentCategory = category;
        modules = newModules;
        moduleTargetScroll = moduleDisplayScroll = 0;
        moduleAnimations.clear();
        moduleAnimStartTimes.clear();

        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < modules.size(); i++) {
            ModuleStructure mod = modules.get(i);
            moduleAnimations.put(mod, 0f);
            moduleAnimStartTimes.put(mod, currentTime + i * 30L);
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

        if (module == null) return;

        new SettingComponentAdder().addSettingComponent(module.settings(), settingComponents);
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent comp = settingComponents.get(i);
            settingAnimations.put(comp, 0f);
            settingAnimStartTimes.put(comp, currentTime + i * 25L);
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

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1 - x, 3);
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

    public float calculateTotalSettingHeight() {
        float total = 0;
        for (AbstractSettingComponent c : settingComponents) {
            if (!c.getSetting().isVisible()) continue;
            total += (c instanceof SelectComponent ? ((SelectComponent) c).getTotalHeight() :
                    c instanceof MultiSelectComponent ? ((MultiSelectComponent) c).getTotalHeight() :
                            c instanceof ColorComponent ? ((ColorComponent) c).getTotalHeight() : SETTING_HEIGHT) + SETTING_SPACING;
        }
        return total;
    }

    public void renderModuleList(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY, int guiScale) {
        updateModuleAnimations();

        Render2D.rect(x, y, width, height, new Color(64, 64, 64, 15).getRGB(), 7);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, 215).getRGB(), 7);

        Scissor.enable(x, y, width, height, guiScale);
        float startY = y + 5f + (float) moduleDisplayScroll;

        for (int i = 0; i < modules.size(); i++) {
            ModuleStructure module = modules.get(i);
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);
            if (modY + MODULE_ITEM_HEIGHT < y || modY > y + height) continue;

            float progress = moduleAnimations.getOrDefault(module, 1f);
            float animX = x + 3 + (1f - progress) * 20f;

            boolean selected = module == selectedModule;
            boolean hovered = mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT;

            Color bg = selected ? new Color(100, 150, 200, (int) (60 * progress)) :
                    hovered ? new Color(100, 100, 100, (int) (40 * progress)) : new Color(64, 64, 64, (int) (25 * progress));

            Render2D.rect(animX, modY, width - 6, MODULE_ITEM_HEIGHT, bg.getRGB(), 5);
            if (selected) Render2D.outline(animX, modY, width - 6, MODULE_ITEM_HEIGHT, 0.5f, new Color(100, 150, 200, (int) (100 * progress)).getRGB(), 5);

            String name = module.getName();
            if (module == bindingModule) name += " [...]";
            else if (module.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
                String key = GLFW.glfwGetKeyName(module.getKey(), 0);
                name += " [" + (key != null ? key.toUpperCase() : "KEY" + module.getKey()) + "]";
            }

            Color text = module.isState() ? new Color(255, 255, 255, (int) (255 * progress)) : new Color(128, 128, 128, (int) (180 * progress));
            Fonts.BOLD.draw(name, animX + 5, modY + 7f, 6, text.getRGB());
        }
        Scissor.disable();

        renderScrollFade(x, y, width, height, moduleScrollTopFade, moduleScrollBottomFade, 80, 15);
    }

    public void renderSettingsPanel(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY, float delta, int guiScale) {
        updateSettingAnimations();

        Render2D.rect(x, y, width, height, new Color(64, 64, 64, 15).getRGB(), 7);
        Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, 215).getRGB(), 7);

        if (selectedModule == null) {
            String text = "Select a module";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, 150).getRGB());
            return;
        }

        Fonts.BOLD.draw(selectedModule.getName(), x + 8, y + 8, 7, new Color(255, 255, 255, 200).getRGB());
        String desc = selectedModule.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Fonts.BOLD.draw(desc.length() > 30 ? desc.substring(0, 27) + "..." : desc, x + 8, y + 20, 5, new Color(128, 128, 128, 150).getRGB());
        }
        Render2D.rect(x + 8, y + 30, width - 16, 1.25f, new Color(64, 64, 64, 64).getRGB(), 10);

        float clipY = y + 31, clipH = height - 32;
        Scissor.enable(x, clipY, width, clipH, guiScale);

        float startY = y + 38f + (float) settingDisplayScroll;
        for (AbstractSettingComponent c : settingComponents) {
            if (!c.getSetting().isVisible()) continue;

            float ch = c instanceof SelectComponent ? ((SelectComponent) c).getTotalHeight() :
                    c instanceof MultiSelectComponent ? ((MultiSelectComponent) c).getTotalHeight() :
                            c instanceof ColorComponent ? ((ColorComponent) c).getTotalHeight() : SETTING_HEIGHT;

            float progress = settingAnimations.getOrDefault(c, 1f);

            c.position(x + 8 + (1f - progress) * 15f, startY);
            c.size(width - 16f, SETTING_HEIGHT);

            if (startY + ch >= clipY && startY <= clipY + clipH) {
                context.getMatrices().pushMatrix();
                c.render(context, (int) mouseX, (int) mouseY, delta);
                context.getMatrices().popMatrix();
            }
            startY += ch + SETTING_SPACING;
        }
        Scissor.disable();

        if (settingComponents.isEmpty() || settingComponents.stream().noneMatch(c -> c.getSetting().isVisible())) {
            String text = "No settings";
            float textSize = 6f;
            float textWidth = Fonts.BOLD.getWidth(text, textSize);
            float textHeight = Fonts.BOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f + 10f;
            Fonts.BOLD.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, 150).getRGB());
        }

        renderScrollFade(x, clipY, width, clipH, settingScrollTopFade, settingScrollBottomFade, 60, 12);
    }

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade, int alpha, int size) {
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                Render2D.rect(x + 1, y + i, w - 2, 1, new Color(20, 20, 20, (int) (alpha * topFade * (1f - i / (float) size))).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                Render2D.rect(x + 1, y + h - size + i, w - 2, 1, new Color(20, 20, 20, (int) (alpha * bottomFade * (1f - i / (float) size))).getRGB(), 0);
            }
        }
    }

    public ModuleStructure getModuleAtPosition(double mouseX, double mouseY, float listX, float listY, float listWidth, float listHeight) {
        if (mouseX < listX || mouseX > listX + listWidth || mouseY < listY || mouseY > listY + listHeight) return null;

        float startY = listY + 5f + (float) moduleDisplayScroll;
        for (int i = 0; i < modules.size(); i++) {
            float modY = startY + i * (MODULE_ITEM_HEIGHT + 2);
            if (mouseX >= listX + 3 && mouseX <= listX + listWidth - 3 && mouseY >= modY && mouseY <= modY + MODULE_ITEM_HEIGHT) {
                return modules.get(i);
            }
        }
        return null;
    }

    public void handleModuleScroll(double vertical, float listHeight) {
        float maxScroll = Math.max(0, modules.size() * 24f - listHeight + 10);
        moduleTargetScroll = Math.max(-maxScroll, Math.min(0, moduleTargetScroll + vertical * 25));
    }

    public void handleSettingScroll(double vertical, float panelHeight) {
        float maxScroll = Math.max(0, calculateTotalSettingHeight() - panelHeight + 45);
        settingTargetScroll = Math.max(-maxScroll, Math.min(0, settingTargetScroll + vertical * 25));
    }

    public void tick() {
        settingComponents.forEach(AbstractSettingComponent::tick);
    }
}