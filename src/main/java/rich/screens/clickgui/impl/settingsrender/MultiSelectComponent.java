package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MultiSelectComponent extends AbstractSettingComponent {
    private final MultiSelectSetting multiSelectSetting;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private final Map<String, Float> checkAnimations = new HashMap<>();

    public MultiSelectComponent(MultiSelectSetting setting) {
        super(setting);
        this.multiSelectSetting = setting;
        for (String option : setting.getList()) {
            checkAnimations.put(option, setting.isSelected(option) ? 1f : 0f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        expandAnimation += (expanded ? 1f : 0f - expandAnimation) * 0.25f;
        expandAnimation = Math.max(0f, Math.min(1f, expandAnimation));

        Render2D.rect(x, y, width, height, new Color(255, 255, 255, hovered ? 25 : 20).getRGB(), 6f);

        if (hovered) {
            Render2D.outline(x, y, width, height, 0.6f, new Color(255, 255, 255, 30).getRGB(), 6f);
        }

        String displayText = multiSelectSetting.getName() + ": " + multiSelectSetting.getSelected().size();
        Fonts.BOLD.draw(displayText, x + 6, y + height / 2 - 3.5f, 7, new Color(210, 210, 220, 200).getRGB());

        String arrow = expanded ? "▼" : "▶";
        float arrowWidth = Fonts.BOLD.getWidth(arrow, 7);
        Fonts.BOLD.draw(arrow, x + width - arrowWidth - 6, y + height / 2 - 3.5f, 7, new Color(200, 200, 210, 180).getRGB());

        if (expandAnimation > 0.01f) {
            float optionY = y + height;
            int visibleOptions = (int)(multiSelectSetting.getList().size() * expandAnimation);

            for (int i = 0; i < visibleOptions && i < multiSelectSetting.getList().size(); i++) {
                String option = multiSelectSetting.getList().get(i);
                boolean optionHovered = mouseX >= x && mouseX <= x + width && mouseY >= optionY && mouseY <= optionY + height;
                boolean isSelected = multiSelectSetting.isSelected(option);

                float checkAnim = checkAnimations.getOrDefault(option, 0f);
                checkAnim += (isSelected ? 1f : 0f - checkAnim) * 0.2f;
                checkAnim = Math.max(0f, Math.min(1f, checkAnim));
                checkAnimations.put(option, checkAnim);

                Render2D.rect(x, optionY, width, height, new Color(255, 255, 255, optionHovered ? 25 : 20).getRGB(), 6f);

                if (optionHovered) {
                    Render2D.outline(x, optionY, width, height, 0.6f, new Color(255, 255, 255, 30).getRGB(), 6f);
                }

                float checkboxSize = 9;
                float checkboxX = x + 6;
                float checkboxY = optionY + height / 2 - checkboxSize / 2;

                Render2D.rect(checkboxX, checkboxY, checkboxSize, checkboxSize, new Color(255, 255, 255, 25).getRGB(), 3f);
                Render2D.outline(checkboxX, checkboxY, checkboxSize, checkboxSize, 0.6f, new Color(255, 255, 255, 60).getRGB(), 3f);

                if (checkAnim > 0.01f) {
                    float innerSize = checkboxSize - 3;
                    float innerX = checkboxX + 1.5f;
                    float innerY = checkboxY + 1.5f;

                    float scaledSize = innerSize * checkAnim;
                    float offsetX = (innerSize - scaledSize) / 2;
                    float offsetY = (innerSize - scaledSize) / 2;

                    int alpha = Math.min(255, Math.max(0, (int)(checkAnim * 200)));
                    Render2D.rect(innerX + offsetX, innerY + offsetY, scaledSize, scaledSize, new Color(100, 220, 120, alpha).getRGB(), 2f);
                }

                Fonts.BOLD.draw(option, x + checkboxSize + 12, optionY + height / 2 - 3.5f, 7, new Color(200, 200, 210, 200).getRGB());

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
                for (String option : multiSelectSetting.getList()) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= optionY && mouseY <= optionY + height) {
                        if (multiSelectSetting.isSelected(option)) {
                            multiSelectSetting.getSelected().remove(option);
                        } else {
                            multiSelectSetting.getSelected().add(option);
                        }
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

        for (String option : multiSelectSetting.getList()) {
            float currentAnim = checkAnimations.getOrDefault(option, 0f);
            float targetAnim = multiSelectSetting.isSelected(option) ? 1f : 0f;
            float newAnim = currentAnim + (targetAnim - currentAnim) * 0.2f;
            newAnim = Math.max(0f, Math.min(1f, newAnim));
            checkAnimations.put(option, newAnim);
        }
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getTotalHeight() {
        return height + (height * multiSelectSetting.getList().size() * expandAnimation);
    }
}