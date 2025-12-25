package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.modules.module.setting.implement.BindSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BindComponent extends AbstractSettingComponent {
    private boolean listening = false;
    private float listeningAnimation = 0f;
    private float hoverAnimation = 0f;
    private float bindHoverAnimation = 0f;

    public BindComponent(BindSetting setting) {
        super(setting);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);
        boolean bindHovered = isBindHover(mouseX, mouseY);

        hoverAnimation += (hovered ? 1f : 0f - hoverAnimation) * 0.2f;
        hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));

        bindHoverAnimation += (bindHovered ? 1f : 0f - bindHoverAnimation) * 0.2f;
        bindHoverAnimation = Math.max(0f, Math.min(1f, bindHoverAnimation));

        listeningAnimation += (listening ? 1f : 0f - listeningAnimation) * 0.2f;
        listeningAnimation = Math.max(0f, Math.min(1f, listeningAnimation));

        Fonts.BOLD.draw(getSetting().getName(), x + 0.5f, y + height / 2 - 3.5f, 7, new Color(210, 210, 220, 200).getRGB());

        float bindBoxX = x + width - 22.5F;
        float bindBoxY = y + height / 2 - 5;
        float bindBoxWidth = 20;
        float bindBoxHeight = 10;

        BindSetting bindSetting = (BindSetting) getSetting();
        int bindKey = bindSetting.getKey();
        int bindType = bindSetting.getType();
        String bindText = listening ? "..." : getBindDisplayName(bindKey, bindType);

        int glassAlpha = 20 + (int)(bindHoverAnimation * 10) + (int)(listeningAnimation * 25);
        Color glassColor = listening
                ? new Color(150, 180, 255, glassAlpha)
                : new Color(255, 255, 255, glassAlpha);

        Render2D.rect(bindBoxX, bindBoxY, bindBoxWidth, bindBoxHeight, glassColor.getRGB(), 3f);

        if (listening) {
            long time = System.currentTimeMillis();
            float pulseAlpha = (float)(Math.sin(time / 150.0) * 0.3 + 0.7);
            int pulseOutlineAlpha = (int)(120 * pulseAlpha * listeningAnimation);
            Render2D.outline(bindBoxX, bindBoxY, bindBoxWidth, bindBoxHeight, 0.5f, new Color(150, 180, 255, pulseOutlineAlpha).getRGB(), 3f);
        } else if (bindHovered) {
            Render2D.outline(bindBoxX, bindBoxY, bindBoxWidth, bindBoxHeight, 0.5f, new Color(255, 255, 255, (int)(50 * bindHoverAnimation)).getRGB(), 3f);
        }

        Color bindTextColor = listening
                ? new Color(180, 200, 255, 220)
                : (bindKey != GLFW.GLFW_KEY_UNKNOWN ? new Color(150, 220, 150, 200) : new Color(150, 150, 160, 150));

        Fonts.BOLD.drawCentered(bindText, bindBoxX + bindBoxWidth / 2, bindBoxY + bindBoxHeight / 2 - 3f, 5, bindTextColor.getRGB());
    }

    private String getBindDisplayName(int key, int type) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) return "None";

        if (type == 0) {
            return switch (key) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                case GLFW.GLFW_MOUSE_BUTTON_6 -> "M6";
                case GLFW.GLFW_MOUSE_BUTTON_7 -> "M7";
                case GLFW.GLFW_MOUSE_BUTTON_8 -> "M8";
                default -> "M" + key;
            };
        }

        String keyName = GLFW.glfwGetKeyName(key, 0);
        if (keyName == null) {
            return switch (key) {
                case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
                case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
                case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
                case GLFW.GLFW_KEY_SPACE -> "Space";
                case GLFW.GLFW_KEY_TAB -> "Tab";
                case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
                case GLFW.GLFW_KEY_ENTER -> "Enter";
                case GLFW.GLFW_KEY_BACKSPACE -> "Back";
                case GLFW.GLFW_KEY_INSERT -> "Ins";
                case GLFW.GLFW_KEY_DELETE -> "Del";
                case GLFW.GLFW_KEY_HOME -> "Home";
                case GLFW.GLFW_KEY_END -> "End";
                case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
                case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
                case GLFW.GLFW_KEY_UP -> "Up";
                case GLFW.GLFW_KEY_DOWN -> "Down";
                case GLFW.GLFW_KEY_LEFT -> "Left";
                case GLFW.GLFW_KEY_RIGHT -> "Right";
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
                case GLFW.GLFW_KEY_PRINT_SCREEN -> "Print";
                case GLFW.GLFW_KEY_SCROLL_LOCK -> "Scroll";
                case GLFW.GLFW_KEY_PAUSE -> "Pause";
                case GLFW.GLFW_KEY_NUM_LOCK -> "NumLk";
                case GLFW.GLFW_KEY_KP_0 -> "Num0";
                case GLFW.GLFW_KEY_KP_1 -> "Num1";
                case GLFW.GLFW_KEY_KP_2 -> "Num2";
                case GLFW.GLFW_KEY_KP_3 -> "Num3";
                case GLFW.GLFW_KEY_KP_4 -> "Num4";
                case GLFW.GLFW_KEY_KP_5 -> "Num5";
                case GLFW.GLFW_KEY_KP_6 -> "Num6";
                case GLFW.GLFW_KEY_KP_7 -> "Num7";
                case GLFW.GLFW_KEY_KP_8 -> "Num8";
                case GLFW.GLFW_KEY_KP_9 -> "Num9";
                case GLFW.GLFW_KEY_KP_DECIMAL -> "Num.";
                case GLFW.GLFW_KEY_KP_DIVIDE -> "Num/";
                case GLFW.GLFW_KEY_KP_MULTIPLY -> "Num*";
                case GLFW.GLFW_KEY_KP_SUBTRACT -> "Num-";
                case GLFW.GLFW_KEY_KP_ADD -> "Num+";
                case GLFW.GLFW_KEY_KP_ENTER -> "NumEnt";
                default -> "Key" + key;
            };
        }
        return keyName.toUpperCase();
    }

    private boolean isBindHover(double mouseX, double mouseY) {
        float bindBoxX = x + width - 35;
        float bindBoxY = y + height / 2 - 5;
        float bindBoxWidth = 32;
        float bindBoxHeight = 10;
        return mouseX >= bindBoxX && mouseX <= bindBoxX + bindBoxWidth &&
                mouseY >= bindBoxY && mouseY <= bindBoxY + bindBoxHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isBindHover(mouseX, mouseY)) {
            if (button == 1) {
                ((BindSetting) getSetting()).setKey(GLFW.GLFW_KEY_UNKNOWN);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            } else if (listening) {
                ((BindSetting) getSetting()).setKey(button);
                ((BindSetting) getSetting()).setType(0);
                listening = false;
                return true;
            } else if (button == 0) {
                listening = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                ((BindSetting) getSetting()).setKey(GLFW.GLFW_KEY_UNKNOWN);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ((BindSetting) getSetting()).setKey(GLFW.GLFW_KEY_UNKNOWN);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                ((BindSetting) getSetting()).setKey(keyCode);
                ((BindSetting) getSetting()).setType(1);
                listening = false;
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        listeningAnimation += (listening ? 1f : 0f - listeningAnimation) * 0.2f;
        listeningAnimation = Math.max(0f, Math.min(1f, listeningAnimation));
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}