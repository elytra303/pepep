package rich.util.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import rich.Initialization;
import rich.util.ColorUtil;

public class Render2D {

    private static boolean inOverlayMode = false;
    private static boolean savedDepthTest = false;
    private static boolean savedDepthMask = false;
    private static boolean savedBlend = false;

    public static void beginOverlay() {
        inOverlayMode = true;

        savedDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void endOverlay() {
        if (savedDepthMask) {
            GL11.glDepthMask(true);
        }
        if (savedDepthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        if (!savedBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }

        inOverlayMode = false;
    }

    public static void clearDepth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null) {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }
    }

    public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public static void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public static void depthMask(boolean mask) {
        GL11.glDepthMask(mask);
    }

    public static void rect(float x, float y, float width, float height, int color) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void rect(float x, float y, float width, float height, int color, float radius) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void rect(float x, float y, float width, float height, int color,
                            float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float radius) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float topLeft, float topRight,
                                    float bottomRight, float bottomLeft) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int topLeft, int topCenter, int topRight,
                                     int leftCenter, int center, int rightCenter,
                                     int bottomLeft, int bottomCenter, int bottomRight,
                                     float radius) {
        int[] colors = {topLeft, topCenter, topRight, leftCenter, center, rightCenter, bottomLeft, bottomCenter, bottomRight};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int[] colors9, float radius) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors9, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int[] colors9, float topLeft, float topRight,
                                     float bottomRight, float bottomLeft) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors9, radii);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int topLeft, int topCenter, int topRight,
                                     int leftCenter, int center, int rightCenter,
                                     int bottomLeft, int bottomCenter, int bottomRight,
                                     float radius, float innerBlur) {
        int[] colors = {topLeft, topCenter, topRight, leftCenter, center, rightCenter, bottomLeft, bottomCenter, bottomRight};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors, radii, innerBlur);
    }

    public static void gradientRect9(float x, float y, float width, float height,
                                     int[] colors9, float radius, float innerBlur) {
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getRectPipeline()
                .drawRect(x, y, width, height, colors9, radii, innerBlur);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color, float radius) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color,
                               float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void gradientOutline(float x, float y, float width, float height, float thickness,
                                       int[] colors, float radius) {
        float[] thicknesses = {thickness, thickness, thickness, thickness, thickness, thickness, thickness, thickness};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getOutlinePipeline()
                .drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void blur(float x, float y, float width, float height, float blurRadius, int tintColor) {
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getBlurPipeline()
                .drawBlur(x, y, width, height, blurRadius, radii, tintColor);
    }

    public static void blur(float x, float y, float width, float height, float blurRadius, float cornerRadius, int tintColor) {
        float[] radii = {cornerRadius, cornerRadius, cornerRadius, cornerRadius};
        Initialization.getInstance().getManager().getRenderCore().getBlurPipeline()
                .drawBlur(x, y, width, height, blurRadius, radii, tintColor);
    }

    public static void blur(float x, float y, float width, float height, float blurRadius,
                            float topLeft, float topRight, float bottomRight, float bottomLeft, int tintColor) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        Initialization.getInstance().getManager().getRenderCore().getBlurPipeline()
                .drawBlur(x, y, width, height, blurRadius, radii, tintColor);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, 1f, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, float smoothness, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, smoothness, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, float smoothness, float radius, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, smoothness, radius);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color) {
        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color, float radius) {
        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, radius);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color, float smoothness, float radius) {
        int[] colors = {color, color, color, color};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(id, x, y, width, height, u0, v0, u1, v1, colors, radii, smoothness);
    }

    public static void drawTexture(DrawContext context, Identifier id,
                                   float x, float y, float width, float height,
                                   float u, float v, float regionWidth, float regionHeight,
                                   float textureWidth, float textureHeight,
                                   int color) {
        float u0 = u / textureWidth;
        float v0 = v / textureHeight;
        float u1 = (u + regionWidth) / textureWidth;
        float v1 = (v + regionHeight) / textureHeight;

        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, 0f);
    }

    public static void drawTexture(DrawContext context, Identifier id,
                                   float x, float y, float width, float height,
                                   float u, float v, float regionWidth, float regionHeight,
                                   float textureWidth, float textureHeight,
                                   int color, float radius) {
        float u0 = u / textureWidth;
        float v0 = v / textureHeight;
        float u1 = (u + regionWidth) / textureWidth;
        float v1 = (v + regionHeight) / textureHeight;

        texture(id, x, y, width, height, u0, v0, u1, v1, color, 1f, radius);
    }

    public static void drawSprite(Sprite sprite, float x, float y, float width, float height, int color) {
        drawSprite(sprite, x, y, width, height, color, true);
    }

    public static void drawSprite(Sprite sprite, float x, float y, float width, float height, int color, boolean pixelPerfect) {
        if (sprite == null || width == 0 || height == 0) return;

        float smoothness = pixelPerfect ? 1f : 0f;
        texture(sprite.getAtlasId(), x, y, width, height,
                sprite.getMinU(), sprite.getMinV(),
                sprite.getMaxU(), sprite.getMaxV(),
                color, smoothness, 0f);
    }

    public static void drawSpriteSmooth(Sprite sprite, float x, float y, float width, float height, int color) {
        drawSprite(sprite, x, y, width, height, color, false);
    }

    public static void drawFramebufferTexture(int textureId, float x, float y, float width, float height,
                                              float r, float g, float b, float a) {
        int color = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        int[] colors = {color, color, color, color};
        float[] radii = {0, 0, 0, 0};

        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawFramebufferTexture(textureId, x, y, width, height, colors, radii, a);
    }

    public static boolean isInOverlayMode() {
        return inOverlayMode;
    }
}