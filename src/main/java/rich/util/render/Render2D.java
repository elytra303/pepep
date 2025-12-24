package rich.util.render;

import net.minecraft.util.Identifier;
import rich.Initialization;
import rich.util.ColorUtil;

public class Render2D {

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
        texture(id, x, y, width, height, 1f, 0f, color);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, float smoothness, int color) {
        texture(id, x, y, width, height, smoothness, 0f, color);
    }

    public static void texture(Identifier id, float x, float y, float width, float height, float smoothness, float radius, int color) {
        int[] colors = {color, color, color, color};
        float[] radii = {radius, radius, radius, radius};
        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(id, x, y, width, height, 0, 0, 1, 1, colors, radii, smoothness);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1, int color) {
        int[] colors = {color, color, color, color};
        float[] radii = {0, 0, 0, 0};
        Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(id, x, y, width, height, u0, v0, u1, v1, colors, radii, 1f);
    }
}