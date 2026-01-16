package rich.util.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import rich.util.render.font.FontRenderer;
import rich.util.render.font.Fonts;
import rich.util.render.pipeline.*;

public class RenderCore {

    private final RectPipeline rectPipeline;
    private final OutlinePipeline outlinePipeline;
    private final TexturePipeline texturePipeline;
    private final BlurPipeline blurPipeline;
    private final FontRenderer fontRenderer;
    private boolean fontsLoaded = false;

    public RenderCore() {
        this.rectPipeline = new RectPipeline();
        this.outlinePipeline = new OutlinePipeline();
        this.texturePipeline = new TexturePipeline();
        this.blurPipeline = new BlurPipeline();
        this.fontRenderer = new FontRenderer();
    }

    private void ensureFontsLoaded() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        fontRenderer.loadAllFonts(Fonts.getRegistry());
    }

    public void setupOverlayState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void restoreState() {
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void clearDepthBuffer() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    public RectPipeline getRectPipeline() {
        return rectPipeline;
    }

    public OutlinePipeline getOutlinePipeline() {
        return outlinePipeline;
    }

    public TexturePipeline getTexturePipeline() {
        return texturePipeline;
    }

    public BlurPipeline getBlurPipeline() {
        return blurPipeline;
    }

    public FontRenderer getFontRenderer() {
        ensureFontsLoaded();
        return fontRenderer;
    }

    public MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }

    public void close() {
        rectPipeline.close();
        outlinePipeline.close();
        texturePipeline.close();
        blurPipeline.close();
        fontRenderer.close();
    }
}