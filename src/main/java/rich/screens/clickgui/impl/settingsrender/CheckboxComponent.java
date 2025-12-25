package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.util.interfaces.AbstractSettingComponent;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class CheckboxComponent extends AbstractSettingComponent {
    private final BooleanSetting booleanSetting;
    private float checkAnimation = 0f;
    private float hoverAnimation = 0f;

    public CheckboxComponent(BooleanSetting setting) {
        super(setting);
        this.booleanSetting = setting;
        this.checkAnimation = setting.isValue() ? 1f : 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        hoverAnimation += (hovered ? 1f : 0f - hoverAnimation) * 0.2f;
        hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));

        checkAnimation += (booleanSetting.isValue() ? 1f : 0f - checkAnimation) * 0.2f;
        checkAnimation = Math.max(0f, Math.min(1f, checkAnimation));

        Fonts.BOLD.draw(booleanSetting.getName(), x + 0.5f, y + height / 2 - 3.5f, 7, new Color(210, 210, 220, 200).getRGB());

        float checkboxSize = 10;
        float checkboxX = x + width - checkboxSize;
        float checkboxY = y + height / 2 - checkboxSize / 2;

        Render2D.rect(checkboxX, checkboxY, checkboxSize, checkboxSize, new Color(255, 255, 255, 25).getRGB(), 3f);

        int outlineAlpha = 60 + (int)(hoverAnimation * 40);
        Render2D.outline(checkboxX, checkboxY, checkboxSize, checkboxSize, 0.6f, new Color(255, 255, 255, outlineAlpha).getRGB(), 3f);

        if (checkAnimation > 0.01f) {
            float innerSize = checkboxSize - 3;
            float innerX = checkboxX + 1.5f;
            float innerY = checkboxY + 1.5f;

            float scaledSize = innerSize * checkAnimation;
            float offsetX = (innerSize - scaledSize) / 2;
            float offsetY = (innerSize - scaledSize) / 2;

            int alpha = Math.min(255, Math.max(0, (int)(checkAnimation * 200)));
            Render2D.rect(innerX + offsetX, innerY + offsetY, scaledSize, scaledSize, new Color(100, 220, 120, alpha).getRGB(), 2f);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHover(mouseX, mouseY) && button == 0) {
            booleanSetting.setValue(!booleanSetting.isValue());
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        checkAnimation += (booleanSetting.isValue() ? 1f : 0f - checkAnimation) * 0.2f;
        checkAnimation = Math.max(0f, Math.min(1f, checkAnimation));
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}