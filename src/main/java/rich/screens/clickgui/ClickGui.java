package rich.screens.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.SettingComponentAdder;
import rich.screens.clickgui.impl.settingsrender.*;
import rich.screens.clickgui.impl.settingsrender.TextComponent;
import rich.util.animations.Animation;
import rich.util.animations.Decelerate;
import rich.util.animations.Direction;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;
import rich.util.render.gif.GifRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();

    private static final int FIXED_GUI_SCALE = 2;
    private static final int BG_WIDTH = 400;
    private static final int BG_HEIGHT = 250;

    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private List<ModuleStructure> currentModules = new ArrayList<>();
    private ModuleStructure selectedModule = null;
    private ModuleStructure bindingModule = null;
    private List<AbstractSettingComponent> settingComponents = new ArrayList<>();

    private double moduleScroll = 0;
    private double smoothModuleScroll = 0;
    private double settingScroll = 0;
    private double smoothSettingScroll = 0;

    private Map<ModuleStructure, Animation> moduleAnimations = new HashMap<>();
    private Map<AbstractSettingComponent, Animation> settingAnimations = new HashMap<>();

    private float moduleScrollTopFade = 0f;
    private float moduleScrollBottomFade = 0f;
    private float settingScrollTopFade = 0f;
    private float settingScrollBottomFade = 0f;

    private static final int SETTING_HEIGHT = 16;
    private static final int SETTING_SPACING = 2;

    public ClickGui() {
        super(Text.of("MenuScreen"));
    }

    @Override
    protected void init() {
        super.init();
        updateModules();
    }

    private void updateModules() {
        currentModules.clear();
        try {
            if (Initialization.getInstance() != null &&
                    Initialization.getInstance().getManager() != null &&
                    Initialization.getInstance().getManager().getModuleRepository() != null) {
                for (ModuleStructure moduleStructure : Initialization.getInstance().getManager().getModuleRepository().modules()) {
                    if (moduleStructure.getCategory() == selectedCategory) {
                        currentModules.add(moduleStructure);
                    }
                }
            }
        } catch (Exception e) {
        }
        moduleScroll = 0;
        smoothModuleScroll = 0;

        moduleAnimations.clear();
        for (int i = 0; i < currentModules.size(); i++) {
            ModuleStructure module = currentModules.get(i);
            Animation anim = new Decelerate().setMs(200 + i * 30).setValue(1);
            anim.setDirection(Direction.FORWARDS);
            moduleAnimations.put(module, anim);
        }

        if (!currentModules.isEmpty() && selectedModule == null) {
            selectModule(currentModules.get(0));
        } else if (selectedModule != null && selectedModule.getCategory() != selectedCategory) {
            if (!currentModules.isEmpty()) {
                selectModule(currentModules.get(0));
            } else {
                selectedModule = null;
                settingComponents.clear();
            }
        }
    }

    private void selectModule(ModuleStructure module) {
        selectedModule = module;
        settingScroll = 0;
        smoothSettingScroll = 0;
        initializeSettings();
    }

    private void initializeSettings() {
        settingComponents.clear();
        settingAnimations.clear();
        if (selectedModule == null) return;

        SettingComponentAdder adder = new SettingComponentAdder();
        adder.addSettingComponent(selectedModule.settings(), settingComponents);

        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent component = settingComponents.get(i);
            Animation anim = new Decelerate().setMs(150 + i * 25).setValue(1);
            anim.setDirection(Direction.FORWARDS);
            settingAnimations.put(component, anim);
        }
    }

    public void openGui() {
        if (mc.currentScreen == null) {
            mc.setScreen(this);
        }
    }

    @Override
    public void tick() {
        GifRender.tick();
        smoothModuleScroll += (moduleScroll - smoothModuleScroll) * 0.2;
        smoothSettingScroll += (settingScroll - smoothSettingScroll) * 0.3;

        float moduleListHeight = BG_HEIGHT - 46f;
        float totalModuleHeight = currentModules.size() * 24f;
        float maxModuleScroll = Math.max(0, totalModuleHeight - moduleListHeight + 10);

        if (smoothModuleScroll < 0) {
            moduleScrollTopFade += (1f - moduleScrollTopFade) * 0.15f;
        } else {
            moduleScrollTopFade += (0f - moduleScrollTopFade) * 0.15f;
        }

        if (smoothModuleScroll > -maxModuleScroll && maxModuleScroll > 0) {
            moduleScrollBottomFade += (1f - moduleScrollBottomFade) * 0.15f;
        } else {
            moduleScrollBottomFade += (0f - moduleScrollBottomFade) * 0.-5f;
        }

        moduleScrollTopFade = Math.max(0f, Math.min(1f, moduleScrollTopFade));
        moduleScrollBottomFade = Math.max(0f, Math.min(1f, moduleScrollBottomFade));

        float settingsPanelHeight = BG_HEIGHT - 46f;
        float totalSettingHeight = calculateTotalSettingHeight();
        float maxSettingScroll = Math.max(0, totalSettingHeight - settingsPanelHeight + 45);

        if (smoothSettingScroll < 0) {
            settingScrollTopFade += (1f - settingScrollTopFade) * 0.15f;
        } else {
            settingScrollTopFade += (0f - settingScrollTopFade) * 0.15f;
        }

        if (smoothSettingScroll > -maxSettingScroll && maxSettingScroll > 0) {
            settingScrollBottomFade += (1f - settingScrollBottomFade) * 0.15f;
        } else {
            settingScrollBottomFade += (0f - settingScrollBottomFade) * 0.15f;
        }

        settingScrollTopFade = Math.max(0f, Math.min(1f, settingScrollTopFade));
        settingScrollBottomFade = Math.max(0f, Math.min(1f, settingScrollBottomFade));

        for (AbstractSettingComponent component : settingComponents) {
            component.tick();
        }
        super.tick();
    }

    private float calculateTotalSettingHeight() {
        float totalHeight = 0;
        for (AbstractSettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                if (component instanceof SelectComponent) {
                    totalHeight += ((SelectComponent) component).getTotalHeight() + SETTING_SPACING;
                } else if (component instanceof MultiSelectComponent) {
                    totalHeight += ((MultiSelectComponent) component).getTotalHeight() + SETTING_SPACING;
                } else if (component instanceof ColorComponent) {
                    totalHeight += ((ColorComponent) component).getTotalHeight() + SETTING_SPACING;
                } else {
                    totalHeight += SETTING_HEIGHT + SETTING_SPACING;
                }
            }
        }
        return totalHeight;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / currentGuiScale;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float scaledMouseX = mouseX / scale;
        float scaledMouseY = mouseY / scale;

        int virtualWidth = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int virtualHeight = mc.getWindow().getHeight() / FIXED_GUI_SCALE;

        float bgX = (virtualWidth - BG_WIDTH) / 2f;
        float bgY = (virtualHeight - BG_HEIGHT) / 2f;

        int[] colors = {
                new Color(26, 26, 26, 255).getRGB(),
                new Color(0, 0, 0, 255).getRGB(),
                new Color(26, 26, 26, 255).getRGB(),
                new Color(0, 0, 0, 255).getRGB(),
                new Color(26, 26, 20, 255).getRGB()
        };

        Render2D.gradientRect(bgX, bgY, BG_WIDTH, BG_HEIGHT, colors, 15);

        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, new Color(128, 128, 128, 25).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, 0.5f, new Color(55, 55, 55, 255).getRGB(), 10);

        Render2D.rect(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, new Color(128, 128, 128, 25).getRGB(), 8);
        Render2D.outline(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, 0.5f, new Color(55, 55, 55, 255).getRGB(), 8);

        Render2D.rect(bgX + 15f, bgY + 60f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 75f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 90f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 105f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 120f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 135f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);

        context.getMatrices().pushMatrix();
        GifRender.drawBackground(bgX + 12.5f, bgY + 12.5f, 70, 30, 7, -1);
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(12, 12, 12, 255).getRGB(), 15);
        GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, -1);
        context.getMatrices().popMatrix();

        Fonts.BOLD.draw(selectedCategory.getReadableName(), bgX + 100f, bgY + 16f, 7, new Color(128, 128, 128, 128).getRGB());

        Color combatColor = selectedCategory == ModuleCategory.COMBAT ? new Color(255, 255, 255, 255) : new Color(128, 128, 128, 128);
        Color movementColor = selectedCategory == ModuleCategory.MOVEMENT ? new Color(255, 255, 255, 255) : new Color(128, 128, 128, 128);
        Color renderColor = selectedCategory == ModuleCategory.RENDER ? new Color(255, 255, 255, 255) : new Color(128, 128, 128, 128);
        Color playerColor = selectedCategory == ModuleCategory.PLAYER ? new Color(255, 255, 255, 255) : new Color(128, 128, 128, 128);
        Color utilColor = selectedCategory == ModuleCategory.MISC ? new Color(255, 255, 255, 255) : new Color(128, 128, 128, 128);

        Fonts.BOLD.draw("Combat", bgX + 17f, bgY + 65f, 6, combatColor.getRGB());
        Fonts.BOLD.draw("Movement", bgX + 17f, bgY + 80f, 6, movementColor.getRGB());
        Fonts.BOLD.draw("Render", bgX + 17f, bgY + 95f, 6, renderColor.getRGB());
        Fonts.BOLD.draw("Player", bgX + 17f, bgY + 110f, 6, playerColor.getRGB());
        Fonts.BOLD.draw("Util", bgX + 17f, bgY + 125f, 6, utilColor.getRGB());

        float moduleListX = bgX + 92f;
        float moduleListY = bgY + 38f;
        float moduleListWidth = 120f;
        float moduleListHeight = BG_HEIGHT - 46f;

        Render2D.rect(moduleListX, moduleListY, moduleListWidth, moduleListHeight, new Color(64, 64, 64, 15).getRGB(), 7);
        Render2D.outline(moduleListX, moduleListY, moduleListWidth, moduleListHeight, 0.5f, new Color(55, 55, 55, 215).getRGB(), 7);

        Scissor.enable(moduleListX, moduleListY, moduleListWidth, moduleListHeight, FIXED_GUI_SCALE);

        float moduleItemHeight = 22f;
        float moduleStartY = moduleListY + 5f + (float) smoothModuleScroll;

        for (int i = 0; i < currentModules.size(); i++) {
            ModuleStructure module = currentModules.get(i);
            float modY = moduleStartY + i * (moduleItemHeight + 2);

            Animation anim = moduleAnimations.get(module);
            float animProgress = anim != null ? anim.getOutput().floatValue() : 1f;

            float animatedX = moduleListX + 3 + (1f - animProgress) * 20f;
            float alpha = animProgress;

            if (modY + moduleItemHeight >= moduleListY && modY <= moduleListY + moduleListHeight) {
                boolean isSelected = module == selectedModule;
                boolean enabled = module.isState();
                boolean hovered = scaledMouseX >= moduleListX + 3 && scaledMouseX <= moduleListX + moduleListWidth - 3 &&
                        scaledMouseY >= modY && scaledMouseY <= modY + moduleItemHeight;

                Color bgColor;
                if (isSelected) {
                    bgColor = new Color(100, 150, 200, (int)(60 * alpha));
                } else if (hovered) {
                    bgColor = new Color(100, 100, 100, (int)(40 * alpha));
                } else {
                    bgColor = new Color(64, 64, 64, (int)(25 * alpha));
                }

                Render2D.rect(animatedX, modY, moduleListWidth - 6, moduleItemHeight, bgColor.getRGB(), 5);

                if (isSelected) {
                    Render2D.outline(animatedX, modY, moduleListWidth - 6, moduleItemHeight, 0.5f, new Color(100, 150, 200, (int)(100 * alpha)).getRGB(), 5);
                }

                String moduleName = module.getName();
                if (module == bindingModule) {
                    moduleName = moduleName + " [...]";
                } else if (module.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
                    String keyName = GLFW.glfwGetKeyName(module.getKey(), 0);
                    if (keyName == null) {
                        keyName = "KEY" + module.getKey();
                    }
                    moduleName = moduleName + " [" + keyName.toUpperCase() + "]";
                }

                Color textColor = enabled ? new Color(255, 255, 255, (int)(255 * alpha)) : new Color(128, 128, 128, (int)(180 * alpha));
                Fonts.BOLD.draw(moduleName, animatedX + 5, modY + 7f, 6, textColor.getRGB());
            }
        }

        Scissor.disable();

        if (moduleScrollTopFade > 0.01f) {
            int fadeAlpha = (int)(80 * moduleScrollTopFade);
            for (int i = 0; i < 15; i++) {
                float gradientAlpha = fadeAlpha * (1f - i / 15f);
                Render2D.rect(moduleListX + 1, moduleListY + i, moduleListWidth - 2, 1, new Color(20, 20, 20, (int)gradientAlpha).getRGB(), 0);
            }
        }

        if (moduleScrollBottomFade > 0.01f) {
            int fadeAlpha = (int)(80 * moduleScrollBottomFade);
            for (int i = 0; i < 15; i++) {
                float gradientAlpha = fadeAlpha * (1f - i / 15f);
                Render2D.rect(moduleListX + 1, moduleListY + moduleListHeight - 15 + i, moduleListWidth - 2, 1, new Color(20, 20, 20, (int)gradientAlpha).getRGB(), 0);
            }
        }

        float settingsPanelX = bgX + 218f;
        float settingsPanelY = bgY + 38f;
        float settingsPanelWidth = 172f;
        float settingsPanelHeight = BG_HEIGHT - 46f;

        Render2D.rect(settingsPanelX, settingsPanelY, settingsPanelWidth, settingsPanelHeight, new Color(64, 64, 64, 15).getRGB(), 7);
        Render2D.outline(settingsPanelX, settingsPanelY, settingsPanelWidth, settingsPanelHeight, 0.5f, new Color(55, 55, 55, 215).getRGB(), 7);

        if (selectedModule != null) {
            Fonts.BOLD.draw(selectedModule.getName(), settingsPanelX + 8, settingsPanelY + 8, 7, new Color(255, 255, 255, 200).getRGB());

            String desc = selectedModule.getDescription();
            if (desc != null && !desc.isEmpty()) {
                if (desc.length() > 30) {
                    desc = desc.substring(0, 27) + "...";
                }
                Fonts.BOLD.draw(desc, settingsPanelX + 8, settingsPanelY + 20, 5, new Color(128, 128, 128, 150).getRGB());
            }

            Render2D.rect(settingsPanelX + 8, settingsPanelY + 30, settingsPanelWidth - 16, 1f, new Color(64, 64, 64, 64).getRGB(), 10);

            float settingsClipY = settingsPanelY + 35;
            float settingsClipHeight = settingsPanelHeight - 40;

            Scissor.enable(settingsPanelX, settingsClipY, settingsPanelWidth, settingsClipHeight, FIXED_GUI_SCALE);

            float settingStartY = settingsPanelY + 38f + (float) smoothSettingScroll;
            float settingWidth = settingsPanelWidth - 16f;

            for (AbstractSettingComponent component : settingComponents) {
                if (component.getSetting().isVisible()) {
                    float componentHeight = SETTING_HEIGHT;
                    if (component instanceof SelectComponent) {
                        componentHeight = ((SelectComponent) component).getTotalHeight();
                    } else if (component instanceof MultiSelectComponent) {
                        componentHeight = ((MultiSelectComponent) component).getTotalHeight();
                    } else if (component instanceof ColorComponent) {
                        componentHeight = ((ColorComponent) component).getTotalHeight();
                    }

                    Animation anim = settingAnimations.get(component);
                    float animProgress = anim != null ? anim.getOutput().floatValue() : 1f;

                    float animatedX = settingsPanelX + 8 + (1f - animProgress) * 15f;
                    float alpha = animProgress;

                    component.position(animatedX, settingStartY);
                    component.size(settingWidth, SETTING_HEIGHT);

                    if (settingStartY + componentHeight >= settingsClipY && settingStartY <= settingsClipY + settingsClipHeight) {
                        context.getMatrices().pushMatrix();
                        component.render(context, (int) scaledMouseX, (int) scaledMouseY, delta);
                        context.getMatrices().popMatrix();
                    }

                    settingStartY += componentHeight + SETTING_SPACING;
                }
            }

            Scissor.disable();

            if (settingScrollTopFade > 0.01f) {
                int fadeAlpha = (int)(60 * settingScrollTopFade);
                for (int i = 0; i < 12; i++) {
                    float gradientAlpha = fadeAlpha * (1f - i / 12f);
                    Render2D.rect(settingsPanelX + 1, settingsClipY + i, settingsPanelWidth - 2, 1, new Color(20, 20, 20, (int)gradientAlpha).getRGB(), 0);
                }
            }

            if (settingScrollBottomFade > 0.01f) {
                int fadeAlpha = (int)(60 * settingScrollBottomFade);
                for (int i = 0; i < 12; i++) {
                    float gradientAlpha = fadeAlpha * (1f - i / 12f);
                    Render2D.rect(settingsPanelX + 1, settingsClipY + settingsClipHeight - 12 + i, settingsPanelWidth - 2, 1, new Color(20, 20, 20, (int)gradientAlpha).getRGB(), 0);
                }
            }

            if (settingComponents.isEmpty() || settingComponents.stream().noneMatch(c -> c.getSetting().isVisible())) {
                Fonts.BOLD.draw("No settings", settingsPanelX + 8, settingsPanelY + 50, 6, new Color(100, 100, 100, 150).getRGB());
            }
        } else {
            Fonts.BOLD.draw("Select a module", settingsPanelX + 8, settingsPanelY + 50, 6, new Color(100, 100, 100, 150).getRGB());
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / currentGuiScale;

        double scaledMouseX = click.x() / scale;
        double scaledMouseY = click.y() / scale;

        int virtualWidth = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int virtualHeight = mc.getWindow().getHeight() / FIXED_GUI_SCALE;

        float bgX = (virtualWidth - BG_WIDTH) / 2f;
        float bgY = (virtualHeight - BG_HEIGHT) / 2f;

        if (scaledMouseX >= bgX + 15f && scaledMouseX <= bgX + 80f) {
            if (scaledMouseY >= bgY + 60f && scaledMouseY <= bgY + 73f) {
                selectedCategory = ModuleCategory.COMBAT;
                updateModules();
                return true;
            }
            if (scaledMouseY >= bgY + 75f && scaledMouseY <= bgY + 88f) {
                selectedCategory = ModuleCategory.MOVEMENT;
                updateModules();
                return true;
            }
            if (scaledMouseY >= bgY + 90f && scaledMouseY <= bgY + 103f) {
                selectedCategory = ModuleCategory.RENDER;
                updateModules();
                return true;
            }
            if (scaledMouseY >= bgY + 105f && scaledMouseY <= bgY + 118f) {
                selectedCategory = ModuleCategory.PLAYER;
                updateModules();
                return true;
            }
            if (scaledMouseY >= bgY + 120f && scaledMouseY <= bgY + 133f) {
                selectedCategory = ModuleCategory.MISC;
                updateModules();
                return true;
            }
        }

        float moduleListX = bgX + 92f;
        float moduleListY = bgY + 38f;
        float moduleListWidth = 120f;
        float moduleListHeight = BG_HEIGHT - 46f;
        float moduleItemHeight = 22f;
        float moduleStartY = moduleListY + 5f + (float) smoothModuleScroll;

        if (scaledMouseX >= moduleListX && scaledMouseX <= moduleListX + moduleListWidth &&
                scaledMouseY >= moduleListY && scaledMouseY <= moduleListY + moduleListHeight) {

            for (int i = 0; i < currentModules.size(); i++) {
                ModuleStructure module = currentModules.get(i);
                float modY = moduleStartY + i * (moduleItemHeight + 2);

                if (scaledMouseX >= moduleListX + 3 && scaledMouseX <= moduleListX + moduleListWidth - 3 &&
                        scaledMouseY >= modY && scaledMouseY <= modY + moduleItemHeight) {

                    if (click.button() == 0) {
                        module.switchState();
                        return true;
                    } else if (click.button() == 1) {
                        selectModule(module);
                        return true;
                    } else if (click.button() == 2) {
                        bindingModule = module;
                        return true;
                    }
                }
            }
        }

        float settingsPanelX = bgX + 218f;
        float settingsPanelY = bgY + 38f;
        float settingsPanelWidth = 172f;
        float settingsPanelHeight = BG_HEIGHT - 46f;

        if (scaledMouseX >= settingsPanelX && scaledMouseX <= settingsPanelX + settingsPanelWidth &&
                scaledMouseY >= settingsPanelY && scaledMouseY <= settingsPanelY + settingsPanelHeight) {

            for (AbstractSettingComponent component : settingComponents) {
                if (component.getSetting().isVisible()) {
                    if (component.mouseClicked(scaledMouseX, scaledMouseY, click.button())) {
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / currentGuiScale;

        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        int virtualWidth = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int virtualHeight = mc.getWindow().getHeight() / FIXED_GUI_SCALE;

        float bgX = (virtualWidth - BG_WIDTH) / 2f;
        float bgY = (virtualHeight - BG_HEIGHT) / 2f;

        float moduleListX = bgX + 92f;
        float moduleListY = bgY + 38f;
        float moduleListWidth = 120f;
        float moduleListHeight = BG_HEIGHT - 46f;

        if (scaledMouseX >= moduleListX && scaledMouseX <= moduleListX + moduleListWidth &&
                scaledMouseY >= moduleListY && scaledMouseY <= moduleListY + moduleListHeight) {

            float totalHeight = currentModules.size() * 24f;
            double maxScroll = Math.max(0, totalHeight - moduleListHeight + 10);
            moduleScroll = Math.max(-maxScroll, Math.min(0, moduleScroll + vertical * 20));
            return true;
        }

        float settingsPanelX = bgX + 218f;
        float settingsPanelY = bgY + 38f;
        float settingsPanelWidth = 172f;
        float settingsPanelHeight = BG_HEIGHT - 46f;

        if (scaledMouseX >= settingsPanelX && scaledMouseX <= settingsPanelX + settingsPanelWidth &&
                scaledMouseY >= settingsPanelY && scaledMouseY <= settingsPanelY + settingsPanelHeight) {

            float totalHeight = calculateTotalSettingHeight();
            double maxScroll = Math.max(0, totalHeight - settingsPanelHeight + 45);
            settingScroll = Math.max(-maxScroll, Math.min(0, settingScroll + vertical * 20));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (bindingModule != null) {
            if (input.key() == GLFW.GLFW_KEY_DELETE || input.key() == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.setKey(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                bindingModule.setKey(input.key());
            }
            bindingModule = null;
            return true;
        }

        for (AbstractSettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                if (component.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                    return true;
                }
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        for (AbstractSettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                if (component.charTyped((char) input.codepoint(), input.modifiers())) {
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        TextComponent.typing = false;
        bindingModule = null;
        super.close();
    }
}