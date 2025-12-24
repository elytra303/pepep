package rich.screens.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.category.BackgroundRender;
import rich.screens.clickgui.impl.settingsrender.TextComponent;
import rich.util.ColorUtil;
import rich.util.interfaces.AbstractComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.gif.GifRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();
    private final List<AbstractComponent> components = new ArrayList<>();

    private static final int FIXED_GUI_SCALE = 2;
    private static final int BG_WIDTH = 400;
    private static final int BG_HEIGHT = 250;

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

            BackgroundRender panel = new BackgroundRender(category, categoryModuleStructures);
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

        int virtualWidth = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int virtualHeight = mc.getWindow().getHeight() / FIXED_GUI_SCALE;

        float bgX = (virtualWidth - BG_WIDTH) / 2f;
        float bgY = (virtualHeight - BG_HEIGHT) / 2f;

        int[] colors = {
                new Color(26, 26, 26, 255).getRGB(),
                new Color(0, 0, 0, 255).getRGB(),
                new Color(26, 26, 26, 255).getRGB(),
                new Color(0, 0, 0, 255).getRGB(),
                new Color(26, 26, 20, 255).getRGB()
        };

        //background
        Render2D.gradientRect(bgX, bgY, BG_WIDTH, BG_HEIGHT, colors, 15);

        //category panel
        Render2D.rect(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15, new Color(128, 128, 128, 25).getRGB(), 10);
        Render2D.outline(bgX + 7.5f, bgY + 7.5f, 80, BG_HEIGHT - 15,0.5f, new Color(55, 55, 55, 255).getRGB(),10);

        //foother bar
        Render2D.rect(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25, new Color(128, 128, 128, 25).getRGB(), 8);
        Render2D.outline(bgX + 92f, bgY + 7.5f, BG_WIDTH - 100f, 25,0.5f, new Color(55, 55, 55, 255).getRGB(),8);
        Fonts.BOLD.draw("Combat", bgX + 100f, bgY + 16f, 7, new Color(128, 128, 128, 128).getRGB());

        //lines
        Render2D.rect(bgX + 15f, bgY + 60f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 75f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 90f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 105f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 120f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);
        Render2D.rect(bgX + 15f, bgY + 135f, 65, 1f, new Color(64, 64, 64, 64).getRGB(), 10);

        //category names
        Fonts.BOLD.draw("Combat", bgX + 17f, bgY + 65f, 6, new Color(128, 128, 128, 128).getRGB());
        Fonts.BOLD.draw("Movement", bgX + 17f, bgY + 80f, 6, new Color(128, 128, 128, 128).getRGB());
        Fonts.BOLD.draw("Render", bgX + 17f, bgY + 95f, 6, new Color(128, 128, 128, 128).getRGB());
        Fonts.BOLD.draw("Player", bgX + 17f, bgY + 110f, 6, new Color(128, 128, 128, 128).getRGB());
        Fonts.BOLD.draw("Util", bgX + 17f, bgY + 125f, 6, new Color(128, 128, 128, 128).getRGB());

        //module
        Render2D.rect(bgX + 92f, bgY + 38f, 146, 40, new Color(64, 64, 64, 25).getRGB(), 7);
        Render2D.outline(bgX + 92f, bgY + 38f, 146, 40,0.5f, new Color(55, 55, 55, 215).getRGB(),7);

        //2 список модулей
        Render2D.rect(bgX + 245f, bgY + 38f, 146, 40, new Color(64, 64, 64, 25).getRGB(), 7);
        Render2D.outline(bgX + 245f, bgY + 38f, 146, 40,0.5f, new Color(55, 55, 55, 215).getRGB(),7);

        Render2D.rect(bgX + 100f, bgY + 55f, 130, 1f, new Color(64, 64, 64, 64).getRGB(), 10);

        Fonts.BOLD.draw("Module Name", bgX + 100f, bgY + 45f, 6, new Color(128, 128, 128, 128).getRGB());
        Fonts.BOLD.draw("This is description for module", bgX + 100f, bgY + 59f, 5, new Color(128, 128, 128, 128).getRGB());



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