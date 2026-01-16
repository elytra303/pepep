package rich.client.draggables;

import net.minecraft.client.gui.DrawContext;
import rich.modules.impl.render.Hud;
import rich.screens.hud.*;

import java.util.ArrayList;
import java.util.List;

public class HudManager {

    private final List<HudElement> elements = new ArrayList<>();

    public HudManager() {
    }

    public void initElements() {
        register(new Watermark());
        register(new HotKeys());
        register(new Potions());
        register(new TargetHud());
        register(new Info());
        register(new Staff());
        register(new Inventory());
    }

    public void register(HudElement element) {
        elements.add(element);
    }

    public void render(DrawContext context, float tickDelta, int mouseX, int mouseY) {
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return;

        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                element.render(context, tickDelta);
            }
        }
    }

    public void tick() {
        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                element.tick();
            }
        }
    }

    private boolean isElementEnabled(HudElement element) {
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return false;

        String name = element.getName();
        return hud.interfaceSettings.isSelected(name);
    }

    public HudElement getElementAt(double mouseX, double mouseY) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            if (isElementEnabled(element)) {
                if (mouseX >= element.getX() && mouseX <= element.getX() + element.getWidth() &&
                        mouseY >= element.getY() && mouseY <= element.getY() + element.getHeight()) {
                    return element;
                }
            }
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                if (element.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void saveConfig() {
    }

    public void loadConfig() {
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public List<HudElement> getEnabledElements() {
        List<HudElement> enabled = new ArrayList<>();
        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                enabled.add(element);
            }
        }
        return enabled;
    }
}