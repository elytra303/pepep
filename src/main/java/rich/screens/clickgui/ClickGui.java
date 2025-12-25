package rich.screens.clickgui;

import lombok.Getter;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.DragHandler;
import rich.screens.clickgui.impl.background.BackgroundComponent;
import rich.screens.clickgui.impl.module.ModuleComponent;
import rich.screens.clickgui.impl.settingsrender.TextComponent;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.math.FrameRateCounter;
import rich.util.render.gif.GifRender;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();
    private static final int FIXED_GUI_SCALE = 2;

    private final BackgroundComponent background = new BackgroundComponent();
    private final ModuleComponent moduleComponent = new ModuleComponent();
    private final DragHandler dragHandler = new DragHandler();
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;

    public ClickGui() {
        super(Text.of("ClickGui"));
    }

    @Override
    protected void init() {
        super.init();
        updateModules();
    }

    private void updateModules() {
        List<ModuleStructure> modules = new ArrayList<>();
        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure m : repo.modules()) {
                    if (m.getCategory() == selectedCategory) modules.add(m);
                }
            }
        } catch (Exception ignored) {}
        moduleComponent.updateModules(modules, selectedCategory);
    }

    public void openGui() {
        if (mc.currentScreen == null) mc.setScreen(this);
    }

    @Override
    public void tick() {
        GifRender.tick();
        moduleComponent.tick();
        super.tick();
    }

    private float[] calculateBackground(float scale) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        float bgX = (vw - BackgroundComponent.BG_WIDTH) / 2f + dragHandler.getOffsetX();
        float bgY = (vh - BackgroundComponent.BG_HEIGHT) / 2f + dragHandler.getOffsetY();
        return new float[]{bgX, bgY, vw, vh};
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        FrameRateCounter.INSTANCE.recordFrame();
        float scrollSpeed = Math.min(1f, 60f / Math.max(FrameRateCounter.INSTANCE.getFps(), 1));

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;

        float mx = mouseX / scale, my = mouseY / scale;
        dragHandler.update(mx, my);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        background.render(context, bgX, bgY, selectedCategory, delta);
        background.renderCategoryPanel(bgX, bgY);
        background.renderHeader(bgX, bgY, selectedCategory);
        background.renderCategoryNames(bgX, bgY, selectedCategory);

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 46f;
        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 46f;

        moduleComponent.updateScroll(delta, scrollSpeed);
        moduleComponent.updateScrollFades(delta, scrollSpeed, mlH, spH);
        moduleComponent.renderModuleList(context, mlX, mlY, mlW, mlH, mx, my, FIXED_GUI_SCALE);
        moduleComponent.renderSettingsPanel(context, spX, spY, spW, spH, mx, my, delta, FIXED_GUI_SCALE);

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = click.x() / scale, my = click.y() / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;

        if (click.button() == 2) {
            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                moduleComponent.setBindingModule(module);
                return true;
            }

            if (dragHandler.startDrag(mx, my, bgX, bgY, BackgroundComponent.BG_WIDTH, BackgroundComponent.BG_HEIGHT)) {
                return true;
            }
        }

        ModuleCategory cat = background.getCategoryAtPosition(mx, my, bgX, bgY);
        if (cat != null) {
            selectedCategory = cat;
            updateModules();
            return true;
        }

        ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
        if (module != null) {
            if (click.button() == 0) module.switchState();
            else if (click.button() == 1) moduleComponent.selectModule(module);
            return true;
        }

        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
            for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                if (c.getSetting().isVisible() && c.mouseClicked(mx, my, click.button())) return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = mouseX / scale, my = mouseY / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= mlX && mx <= mlX + mlW && my >= mlY && my <= mlY + mlH) {
            moduleComponent.handleModuleScroll(vertical, mlH);
            return true;
        }

        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
            moduleComponent.handleSettingScroll(vertical, spH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (dragHandler.isResetNeeded(input.key(), input.modifiers())) {
            dragHandler.reset();
            return true;
        }

        ModuleStructure binding = moduleComponent.getBindingModule();
        if (binding != null) {
            binding.setKey(input.key() == GLFW.GLFW_KEY_DELETE || input.key() == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : input.key());
            moduleComponent.setBindingModule(null);
            return true;
        }
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.charTyped((char) input.codepoint(), input.modifiers())) return true;
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
        moduleComponent.setBindingModule(null);
        dragHandler.stopDrag();
        super.close();
    }
}