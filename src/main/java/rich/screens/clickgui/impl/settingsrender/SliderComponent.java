package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.util.interfaces.AbstractSettingComponent;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class SliderComponent extends AbstractSettingComponent {
    private final SliderSettings sliderSettings;
    private boolean dragging = false;
    private float animatedPercentage = 0f;
    private float hoverAnimation = 0f;

    public SliderComponent(SliderSettings setting) {
        super(setting);
        this.sliderSettings = setting;
        float range = sliderSettings.getMax() - sliderSettings.getMin();
        this.animatedPercentage = (sliderSettings.getValue() - sliderSettings.getMin()) / range;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        hoverAnimation += (hovered ? 1f : 0f - hoverAnimation) * 0.2f;
        hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));

        float range = sliderSettings.getMax() - sliderSettings.getMin();
        float targetPercentage = (sliderSettings.getValue() - sliderSettings.getMin()) / range;
        animatedPercentage += (targetPercentage - animatedPercentage) * 0.25f;

        int glassAlpha = 20 + (int)(hoverAnimation * 10);
        Render2D.rect(x, y, width, height, new Color(255, 255, 255, glassAlpha).getRGB(), 6f);

        if (hovered) {
            Render2D.outline(x, y, width, height, 0.6f, new Color(255, 255, 255, (int)(30 * hoverAnimation)).getRGB(), 6f);
        }

        float sliderWidth = width * animatedPercentage;
        if (sliderWidth > 0) {
            Render2D.rect(x, y, sliderWidth, height, new Color(100, 200, 120, 40 + (int)(hoverAnimation * 15)).getRGB(), 6f);
            Render2D.outline(x, y, sliderWidth, height, 0.8f, new Color(255, 255, 255, (int)(30 * hoverAnimation)).getRGB(), 6f);
        }

        String valueText = sliderSettings.isInteger() ? String.valueOf((int)sliderSettings.getValue()) : String.format("%.2f", sliderSettings.getValue());
        String displayText = sliderSettings.getName() + ": " + valueText;

        Fonts.BOLD.draw(displayText, x + 6, y + height / 2 - 3.5f, 7, new Color(210, 210, 220, 200).getRGB());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHover(mouseX, mouseY) && button == 0) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    private void updateValue(double mouseX) {
        float percentage = (float)((mouseX - x) / width);
        percentage = Math.max(0, Math.min(1, percentage));

        float range = sliderSettings.getMax() - sliderSettings.getMin();
        float newValue = sliderSettings.getMin() + (range * percentage);

        if (sliderSettings.isInteger()) {
            newValue = Math.round(newValue);
        }

        sliderSettings.setValue(newValue);
    }

    @Override
    public void tick() {
        hoverAnimation += (isHover(0, 0) ? 1f : 0f - hoverAnimation) * 0.2f;
        hoverAnimation = Math.max(0f, Math.min(1f, hoverAnimation));
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}