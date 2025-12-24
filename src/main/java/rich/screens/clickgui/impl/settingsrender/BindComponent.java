package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.modules.module.setting.implement.BindSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.string.StringHelper;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class BindComponent extends AbstractSettingComponent {
    private boolean listening = false;
    private float listeningAnimation = 0f;
    private float hoverAnimation = 0f;

    public BindComponent(BindSetting setting) {
        super(setting);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        hoverAnimation += (hovered ? 1f : 0f - hoverAnimation) * 0.2f;
        hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));

        listeningAnimation += (listening ? 1f : 0f - listeningAnimation) * 0.2f;
        listeningAnimation = Math.max(0f, Math.min(1f, listeningAnimation));

        int glassAlpha = 20 + (int)(hoverAnimation * 10) + (int)(listeningAnimation * 25);
        Color glassColor = listening
                ? new Color(150, 180, 255, glassAlpha)
                : new Color(255, 255, 255, glassAlpha);

        Render2D.rect(x, y, width, height, glassColor.getRGB(), 6f);

        if (listening) {
            long time = System.currentTimeMillis();
            float pulseAlpha = (float)(Math.sin(time / 150.0) * 0.3 + 0.7);
            int pulseOutlineAlpha = (int)(120 * pulseAlpha * listeningAnimation);
            Render2D.outline(x, y, width, height, 1.2f, new Color(150, 180, 255, pulseOutlineAlpha).getRGB(), 6f);
        } else if (hovered) {
            Render2D.outline(x, y, width, height, 0.6f, new Color(255, 255, 255, (int)(30 * hoverAnimation)).getRGB(), 6f);
        }

        BindSetting bindSetting = (BindSetting) getSetting();
        String bindName = bindSetting.getKey() == -1 ? "None" : StringHelper.getBindName(bindSetting.getKey());
        String displayText = getSetting().getName() + ": " + (listening ? "..." : bindName);

        Color textColor = listening
                ? new Color(220, 230, 255, 220)
                : new Color(210, 210, 220, 200);

        Fonts.BOLD.draw(displayText, x + 6, y + height / 2 - 3.5f, 7, textColor.getRGB());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHover(mouseX, mouseY)) {
            if (button == 0) {
                listening = !listening;
                return true;
            } else if (button == 1) {
                ((BindSetting) getSetting()).setKey(-1);
                listening = false;
                return true;
            } else if (listening && button >= 0 && button <= 7) {
                ((BindSetting) getSetting()).setKey(button);
                listening = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                ((BindSetting) getSetting()).setKey(-1);
                listening = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ((BindSetting) getSetting()).setKey(-1);
                listening = false;
                return true;
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                ((BindSetting) getSetting()).setKey(keyCode);
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