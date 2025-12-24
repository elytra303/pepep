package rich.util.render.font;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FontRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Initialization/FontRenderer");

    private final FontPipeline pipeline;
    private final Map<String, FontAtlas> fonts;

    public FontRenderer() {
        this.pipeline = new FontPipeline();
        this.fonts = new HashMap<>();
    }

    public void loadFont(String name, String path) {
        Identifier jsonId = Identifier.of("minecraft", "fonts/" + path + ".json");
        Identifier textureId = Identifier.of("minecraft", "fonts/" + path + ".png");
        FontAtlas atlas = new FontAtlas(jsonId, textureId);
        fonts.put(name, atlas);
        LOGGER.info("Registered font: {} -> {}", name, path);
    }

    public void loadAllFonts(Map<String, String> registry) {
        for (Map.Entry<String, String> entry : registry.entrySet()) {
            loadFont(entry.getKey(), entry.getValue());
        }
    }

    public FontAtlas getFont(String name) {
        return fonts.get(name);
    }

    public void drawText(String fontName, String text, float x, float y, float size, int color) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color);
    }

    public void drawTextWithOutline(String fontName, String text, float x, float y, float size,
                                    int color, float outlineWidth, int outlineColor) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        pipeline.drawText(atlas, text, x, y, size, color, outlineWidth, outlineColor);
    }

    public void drawCenteredText(String fontName, String text, float x, float y, float size, int color) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return;
        float width = pipeline.getTextWidth(atlas, text, size);
        pipeline.drawText(atlas, text, x - width / 2, y, size, color);
    }

    public float getTextWidth(String fontName, String text, float size) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return 0;
        return pipeline.getTextWidth(atlas, text, size);
    }

    public float getLineHeight(String fontName, float size) {
        FontAtlas atlas = fonts.get(fontName);
        if (atlas == null) return size;
        atlas.ensureLoaded();
        return (atlas.getLineHeight() / atlas.getFontSize()) * size;
    }

    public void close() {
        pipeline.close();
        fonts.clear();
    }
}