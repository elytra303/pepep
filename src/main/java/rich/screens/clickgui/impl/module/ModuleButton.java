package rich.screens.clickgui.impl.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.screens.clickgui.impl.settingsrender.*;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.Setting;
import rich.modules.module.setting.SettingComponentAdder;
import rich.util.interfaces.AbstractComponent;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.interfaces.ResizableMovable;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleButton extends AbstractComponent {
    private final ModuleStructure moduleStructure;
    private final List<AbstractSettingComponent> settingComponents = new ArrayList<>();
    private boolean expanded = false;
    private boolean binding = false;
    private boolean isLast = false;

    private float originalY;
    private float renderY;

    private static final int SETTING_INDENT = 4;
    private static final int SETTING_HEIGHT = 16;
    private static final int SETTING_SPACING = 2;
    private static final int BASE_HEIGHT = 20;

    public ModuleButton(ModuleStructure moduleStructure) {
        this.moduleStructure = moduleStructure;
        initializeSettings();
    }

    private void initializeSettings() {
        SettingComponentAdder adder = new SettingComponentAdder();
        adder.addSettingComponent(moduleStructure.settings(), settingComponents);
    }

    @Override
    public ResizableMovable position(float x, float y) {
        this.x = x;
        this.y = y;
        this.originalY = y;
        return this;
    }

    public void setRenderY(float renderY) {
        this.renderY = renderY;
    }

    public void setLast(boolean isLast) {
        this.isLast = isLast;
    }

    public float getOriginalY() {
        return originalY;
    }

    public float getRenderY() {
        return renderY;
    }

    private float calculateSettingsHeight() {
        float totalHeight = 0f;
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

    private void updateSettingPositions() {
        float settingY = renderY + BASE_HEIGHT;
        for (AbstractSettingComponent settingComponent : settingComponents) {
            if (settingComponent.getSetting().isVisible()) {
                settingComponent.position(x + SETTING_INDENT, settingY);
                settingComponent.size(width - SETTING_INDENT * 2, SETTING_HEIGHT);

                float componentHeight = SETTING_HEIGHT;
                if (settingComponent instanceof SelectComponent) {
                    componentHeight = ((SelectComponent) settingComponent).getTotalHeight();
                } else if (settingComponent instanceof MultiSelectComponent) {
                    componentHeight = ((MultiSelectComponent) settingComponent).getTotalHeight();
                } else if (settingComponent instanceof ColorComponent) {
                    componentHeight = ((ColorComponent) settingComponent).getTotalHeight();
                }

                settingY += componentHeight + SETTING_SPACING;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean enabled = moduleStructure.isState();

        Render2D.rect(x - 8f, renderY, width + 16.5f, 1f, new Color(55, 55, 55, 100).getRGB());

        if (isLast) {
            Render2D.rect(x - 8f, renderY + 20.5f, width + 16, 1f, new Color(55, 55, 55, 100).getRGB());
        }

        String moduleName = moduleStructure.getName();

        if (moduleStructure.getKey() != GLFW.GLFW_KEY_UNKNOWN && !binding) {
            String keyName = GLFW.glfwGetKeyName(moduleStructure.getKey(), 0);
            if (keyName == null) {
                keyName = "KEY" + moduleStructure.getKey();
            }
            moduleName = moduleName + " [" + keyName.toUpperCase() + "]";
        } else if (binding) {
            moduleName = moduleName + " [...]";
        }

        Color textColor = enabled ? new Color(255, 255, 255, 255) : new Color(155, 155, 155, 155);
        Fonts.BOLD.draw(moduleName, x + 4, renderY + 6.5f, 7, textColor.getRGB());

        Fonts.BOLD.draw("...", x + 83, renderY + 0.5f, 12, textColor.getRGB());

        if (!settingComponents.isEmpty()) {
            String arrow = expanded ? "▼" : "▶";
            float arrowWidth = Fonts.BOLD.getWidth(arrow, 7);
            Fonts.BOLD.draw(arrow, x + width - arrowWidth - 8, renderY + 6.5f, 7, new Color(200, 200, 205, 180).getRGB());
        }

        if (expanded && !settingComponents.isEmpty()) {
            updateSettingPositions();

            for (AbstractSettingComponent settingComponent : settingComponents) {
                Setting setting = settingComponent.getSetting();
                if (setting.isVisible()) {
                    settingComponent.render(context, mouseX, mouseY, delta);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= renderY && mouseY <= renderY + BASE_HEIGHT && mouseX >= x && mouseX <= x + width) {
            if (button == 0) {
                moduleStructure.switchState();
                return true;
            } else if (button == 1 && !settingComponents.isEmpty()) {
                expanded = !expanded;
                return true;
            } else if (button == 2) {
                binding = true;
                return true;
            }
        }

        if (expanded) {
            updateSettingPositions();

            for (AbstractSettingComponent settingComponent : settingComponents) {
                if (settingComponent.getSetting().isVisible()) {
                    float componentTop = settingComponent.y;
                    float componentBottom = componentTop + settingComponent.height;

                    if (settingComponent instanceof SelectComponent) {
                        componentBottom = componentTop + ((SelectComponent) settingComponent).getTotalHeight();
                    } else if (settingComponent instanceof MultiSelectComponent) {
                        componentBottom = componentTop + ((MultiSelectComponent) settingComponent).getTotalHeight();
                    } else if (settingComponent instanceof ColorComponent) {
                        componentBottom = componentTop + ((ColorComponent) settingComponent).getTotalHeight();
                    }

                    if (mouseY >= componentTop && mouseY <= componentBottom) {
                        if (settingComponent.mouseClicked(mouseX, mouseY, button)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (expanded) {
            updateSettingPositions();

            for (AbstractSettingComponent settingComponent : settingComponents) {
                if (settingComponent.getSetting().isVisible()) {
                    if (settingComponent.mouseReleased(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (expanded) {
            updateSettingPositions();

            for (AbstractSettingComponent settingComponent : settingComponents) {
                if (settingComponent.getSetting().isVisible()) {
                    if (settingComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                moduleStructure.setKey(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                moduleStructure.setKey(keyCode);
            }
            binding = false;
            return true;
        }

        if (expanded) {
            updateSettingPositions();

            for (AbstractSettingComponent component : settingComponents) {
                if (component.getSetting().isVisible()) {
                    if (component.keyPressed(keyCode, scanCode, modifiers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (expanded) {
            updateSettingPositions();

            for (AbstractSettingComponent component : settingComponents) {
                if (component.getSetting().isVisible()) {
                    if (component.charTyped(chr, modifiers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (expanded) {
            updateSettingPositions();

            for (AbstractSettingComponent component : settingComponents) {
                component.tick();
            }
        }
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        float currentHeight = expanded ? BASE_HEIGHT + calculateSettingsHeight() : BASE_HEIGHT;
        return mouseX >= x && mouseX <= x + width && mouseY >= renderY && mouseY <= renderY + currentHeight;
    }

    public float getTotalHeight() {
        return expanded ? BASE_HEIGHT + calculateSettingsHeight() : BASE_HEIGHT;
    }

    public float getY() {
        return renderY;
    }

    public float getX() {
        return x;
    }
}