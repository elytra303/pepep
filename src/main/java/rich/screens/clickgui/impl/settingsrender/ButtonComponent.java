package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.setting.implement.ButtonSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class ButtonComponent extends AbstractSettingComponent {
    private final ButtonSetting buttonSetting;
    private float pressAnimation = 0f;
    private float hoverAnimation = 0f;
    private boolean wasPressed = false;

    public ButtonComponent(ButtonSetting setting) {
        super(setting);
        this.buttonSetting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        hoverAnimation += (hovered ? 1f : 0f - hoverAnimation) * 0.2f;
        hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));

        pressAnimation += (wasPressed ? 1f : 0f - pressAnimation) * 0.3f;
        if (pressAnimation < 0.05f && wasPressed) {
            wasPressed = false;
        }

        int glassAlpha = 30 + (int)(hoverAnimation * 15) + (int)(pressAnimation * 20);
        Render2D.rect(x, y, width, height, new Color(120, 220, 140, glassAlpha).getRGB(), 8f);

        int outlineAlpha = 80 + (int)(hoverAnimation * 70) + (int)(pressAnimation * 100);
        float outlineThickness = 1.0f + pressAnimation * 0.5f;
        Render2D.outline(x, y, width, height, outlineThickness, new Color(130, 240, 150, outlineAlpha).getRGB(), 8f);

        String buttonText = buttonSetting.getButtonName() != null ? buttonSetting.getButtonName() : buttonSetting.getName();
        float textWidth = Fonts.BOLD.getWidth(buttonText, 7);
        float textX = x + width / 2 - textWidth / 2;

        int textAlpha = 200 + (int)(hoverAnimation * 55);
        Fonts.BOLD.draw(buttonText, textX, y + height / 2 - 3.5f, 7, new Color(220, 240, 220, textAlpha).getRGB());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHover(mouseX, mouseY) && button == 0) {
            if (buttonSetting.getRunnable() != null) {
                buttonSetting.getRunnable().run();
            }
            wasPressed = true;
            pressAnimation = 1f;
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        pressAnimation += (wasPressed ? 1f : 0f - pressAnimation) * 0.3f;
        pressAnimation = Math.max(0f, Math.min(1f, pressAnimation));

        if (pressAnimation < 0.05f && wasPressed) {
            wasPressed = false;
        }
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}