package rich.screens.clickgui.impl.category;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import rich.screens.clickgui.impl.module.ModuleButton;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.util.interfaces.AbstractComponent;
import rich.util.interfaces.ResizableMovable;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BackgroundRender extends AbstractComponent {
    private final ModuleCategory category;
    private final List<ModuleStructure> moduleStructures;
    private final List<ModuleButton> moduleButtons = new ArrayList<>();

    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private double scroll = 0;
    private double smoothedScroll = 0;

    private static final int HEADER_HEIGHT = 20;
    private static final int MODULE_HEIGHT = 40;
    private static final int PADDING = 8;
    private static final int MODULE_SPACING = 0;

    public BackgroundRender(ModuleCategory category, List<ModuleStructure> moduleStructures) {
        this.category = category;
        this.moduleStructures = moduleStructures;
        initializeButtons();
    }

    private void initializeButtons() {
        moduleButtons.clear();
        for (ModuleStructure module : moduleStructures) {
            ModuleButton button = new ModuleButton(module);
            moduleButtons.add(button);
        }
    }

    @Override
    public ResizableMovable position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateButtonPositions();

        for (int i = 0; i < moduleButtons.size(); i++) {
            ModuleButton button = moduleButtons.get(i);
            button.setLast(i == moduleButtons.size() - 1);

            float buttonY = button.getOriginalY() + (float) smoothedScroll;
            if (buttonY + button.getTotalHeight() >= y + HEADER_HEIGHT && buttonY <= y + height) {
                button.setRenderY(buttonY);
                button.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private void updateButtonPositions() {
        float currentY = y + HEADER_HEIGHT + PADDING;

        for (ModuleButton button : moduleButtons) {
            button.position(x + PADDING, currentY);
            button.size(width - PADDING * 2, MODULE_HEIGHT);
            currentY += button.getTotalHeight() + MODULE_SPACING;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHoveringHeader(mouseX, mouseY) && button == 0) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }

        if (mouseY >= y + HEADER_HEIGHT && mouseY <= y + height) {
            for (ModuleButton moduleButton : moduleButtons) {
                float renderButtonY = moduleButton.getRenderY();
                if (mouseY >= renderButtonY && mouseY <= renderButtonY + moduleButton.getTotalHeight()) {
                    if (moduleButton.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }

        for (ModuleButton moduleButton : moduleButtons) {
            if (moduleButton.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            x = (float) (mouseX - dragOffsetX);
            y = (float) (mouseY - dragOffsetY);
            return true;
        }

        for (ModuleButton moduleButton : moduleButtons) {
            if (moduleButton.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isHover(mouseX, mouseY)) {
            float totalHeight = 0;
            for (ModuleButton button : moduleButtons) {
                totalHeight += button.getTotalHeight() + MODULE_SPACING;
            }
            double maxScroll = Math.max(0, totalHeight - (height - HEADER_HEIGHT - PADDING * 2));
            scroll = Math.max(-maxScroll, Math.min(0, scroll + amount * 15));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleButton button : moduleButtons) {
            if (button.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (ModuleButton button : moduleButtons) {
            if (button.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        smoothedScroll += (scroll - smoothedScroll) * 0.25;

        for (ModuleButton button : moduleButtons) {
            button.tick();
        }
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isHoveringHeader(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }
}