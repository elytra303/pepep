package rich.util.render;

import net.minecraft.client.MinecraftClient;
import rich.util.render.font.FontRenderer;
import rich.util.render.font.Fonts;
import rich.util.render.pipeline.BlurPipeline;
import rich.util.render.pipeline.OutlinePipeline;
import rich.util.render.pipeline.RectPipeline;
import rich.util.render.pipeline.TexturePipeline;

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