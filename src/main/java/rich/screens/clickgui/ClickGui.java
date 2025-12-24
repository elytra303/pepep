package rich.screens.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.category.CategoryPanel;
import rich.screens.clickgui.impl.settingsrender.TextComponent;
import rich.util.interfaces.AbstractComponent;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();
    private final List<AbstractComponent> components = new ArrayList<>();

    private static final int FIXED_GUI_SCALE = 2;

    public ClickGui() {
        super(Text.of("MenuScreen"));
    }

    @Override
    protected void init() {
        super.init();
        initialize();
    }

    public void initialize() {
        components.clear();

        int panelWidth = 115;
        int panelHeight = 250;
        int spacing = 7;

        ModuleCategory[] categories = {
                ModuleCategory.COMBAT,
                ModuleCategory.MOVEMENT,
                ModuleCategory.RENDER,
                ModuleCategory.PLAYER,
                ModuleCategory.MISC
        };

        int virtualWidth = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int virtualHeight = mc.getWindow().getHeight() / FIXED_GUI_SCALE;

        int totalWidth = (panelWidth + spacing) * categories.length - spacing;
        int startX = (virtualWidth - totalWidth) / 2;
        int startY = (virtualHeight - panelHeight) / 2;

        for (int i = 0; i < categories.length; i++) {
            ModuleCategory category = categories[i];
            List<ModuleStructure> categoryModuleStructures = getModulesByCategory(category);

            CategoryPanel panel = new CategoryPanel(category, categoryModuleStructures);
            panel.position(startX + (panelWidth + spacing) * i, startY);
            panel.size(panelWidth, panelHeight);

            components.add(panel);
        }
    }

    private List<ModuleStructure> getModulesByCategory(ModuleCategory category) {
        List<ModuleStructure> moduleStructures = new ArrayList<>();
        try {
            if (Initialization.getInstance() != null &&
                    Initialization.getInstance().getManager() != null &&
                    Initialization.getInstance().getManager().getModuleRepository() != null) {
                for (ModuleStructure moduleStructure : Initialization.getInstance().getManager().getModuleRepository().modules()) {
                    if (moduleStructure.getCategory() == category) {
                        moduleStructures.add(moduleStructure);
                    }
                }
            }
        } catch (Exception e) {
        }
        return moduleStructures;
    }

    public void openGui() {
        if (mc.currentScreen == null) {
            mc.setScreen(this);
        }
    }

    @Override
    public void tick() {
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / currentGuiScale;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float scaledMouseX = mouseX / scale;
        float scaledMouseY = mouseY / scale;

        for (AbstractComponent component : components) {
            component.render(context, (int)scaledMouseX, (int)scaledMouseY, delta);
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        initialize();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / currentGuiScale;

        double scaledMouseX = click.x() / scale;
        double scaledMouseY = click.y() / scale;

        for (AbstractComponent component : components) {
            if (component.mouseClicked(scaledMouseX, scaledMouseY, click.button())) {
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / currentGuiScale;

        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        for (AbstractComponent component : components) {
            if (component.mouseScrolled(scaledMouseX, scaledMouseY, vertical)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        for (AbstractComponent component : components) {
            if (component.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        for (AbstractComponent component : components) {
            if (component.charTyped((char)input.codepoint(), input.modifiers())) {
                return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        TextComponent.typing = false;
        super.close();
    }
}