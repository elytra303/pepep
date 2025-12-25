package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class SelectComponent extends AbstractSettingComponent {
    private final SelectSetting selectSetting;
    private boolean expanded = false;
    private float expandAnimation = 0f;

    public SelectComponent(SelectSetting setting) {
        super(setting);
        this.selectSetting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        expandAnimation += (expanded ? 1f : 0f - expandAnimation) * 0.25f;
        expandAnimation = Math.max(0f, Math.min(1f, expandAnimation));

        if (hovered) {
            Render2D.outline(x, y, width, height, 0.6f, new Color(255, 255, 255, 30).getRGB(), 6f);
        }

        String displayText = selectSetting.getName() + ": " + selectSetting.getSelected();
        Fonts.BOLD.draw(displayText, x + 6, y + height / 2 - 3.5f, 7, new Color(210, 210, 220, 200).getRGB());

        String arrow = expanded ? "▼" : "▶";
        float arrowWidth = Fonts.BOLD.getWidth(arrow, 7);
        Fonts.BOLD.draw(arrow, x + width - arrowWidth - 6, y + height / 2 - 3.5f, 7, new Color(200, 200, 210, 180).getRGB());

        if (expandAnimation > 0.01f) {
            float optionY = y + height;
            int visibleOptions = (int)(selectSetting.getList().size() * expandAnimation);

            for (int i = 0; i < visibleOptions && i < selectSetting.getList().size(); i++) {
                String option = selectSetting.getList().get(i);
                boolean optionHovered = mouseX >= x && mouseX <= x + width && mouseY >= optionY && mouseY <= optionY + height;
                boolean isSelected = selectSetting.isSelected(option);

                int optionAlpha = isSelected ? 35 : (optionHovered ? 25 : 20);
                Color optionColor = isSelected
                        ? new Color(100, 220, 120, optionAlpha)
                        : new Color(255, 255, 255, optionAlpha);

                Render2D.rect(x, optionY, width, height, optionColor.getRGB(), 6f);

                if (isSelected) {
                    Render2D.outline(x, optionY, width, height, 0.8f, new Color(120, 240, 140, 100).getRGB(), 6f);
                } else if (optionHovered) {
                    Render2D.outline(x, optionY, width, height, 0.6f, new Color(255, 255, 255, 30).getRGB(), 6f);
                }

                Color textColor = isSelected ? new Color(140, 255, 150, 220) : new Color(200, 200, 210, 200);
                Fonts.BOLD.draw(option, x + 10, optionY + height / 2 - 3.5f, 7, textColor.getRGB());

                optionY += height;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHover(mouseX, mouseY)) {
                expanded = !expanded;
                return true;
            }

            if (expanded && expandAnimation > 0.8f) {
                float optionY = y + height;
                for (String option : selectSetting.getList()) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= optionY && mouseY <= optionY + height) {
                        selectSetting.setSelected(option);
                        return true;
                    }
                    optionY += height;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        expandAnimation += (expanded ? 1f : 0f - expandAnimation) * 0.25f;
        expandAnimation = Math.max(0f, Math.min(1f, expandAnimation));
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getTotalHeight() {
        return height + (height * selectSetting.getList().size() * expandAnimation);
    }
}