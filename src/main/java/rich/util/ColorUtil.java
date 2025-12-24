package rich.util;

public class ColorUtil {

    public static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int rgba(int r, int g, int b, float a) {
        return rgba(r, g, b, (int)(a * 255));
    }

    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    public static int[] solid(int color) {
        return new int[]{color, color, color, color, color, color, color, color, color};
    }

    public static int[] solid8(int color) {
        return new int[]{color, color, color, color, color, color, color, color};
    }

    public static int[] horizontal(int left, int right) {
        return new int[]{
                left, blend(left, right, 0.5f), right,
                left, blend(left, right, 0.5f), right,
                left, blend(left, right, 0.5f), right
        };
    }

    public static int[] vertical(int top, int bottom) {
        return new int[]{
                top, top, top,
                blend(top, bottom, 0.5f), blend(top, bottom, 0.5f), blend(top, bottom, 0.5f),
                bottom, bottom, bottom
        };
    }

    public static int[] diagonal(int topLeft, int bottomRight) {
        int topRight = blend(topLeft, bottomRight, 0.5f);
        int bottomLeft = blend(topLeft, bottomRight, 0.5f);
        int center = blend(topLeft, bottomRight, 0.5f);
        return new int[]{
                topLeft, blend(topLeft, topRight, 0.5f), topRight,
                blend(topLeft, bottomLeft, 0.5f), center, blend(topRight, bottomRight, 0.5f),
                bottomLeft, blend(bottomLeft, bottomRight, 0.5f), bottomRight
        };
    }

    public static int[] corners(int topLeft, int topRight, int bottomLeft, int bottomRight) {
        int top = blend(topLeft, topRight, 0.5f);
        int bottom = blend(bottomLeft, bottomRight, 0.5f);
        int left = blend(topLeft, bottomLeft, 0.5f);
        int right = blend(topRight, bottomRight, 0.5f);
        int center = blend(blend(topLeft, bottomRight, 0.5f), blend(topRight, bottomLeft, 0.5f), 0.5f);
        return new int[]{
                topLeft, top, topRight,
                left, center, right,
                bottomLeft, bottom, bottomRight
        };
    }

    public static int[] full(int tl, int tc, int tr, int ml, int c, int mr, int bl, int bc, int br) {
        return new int[]{tl, tc, tr, ml, c, mr, bl, bc, br};
    }

    public static int[] horizontal8(int left, int right) {
        int mid = blend(left, right, 0.5f);
        return new int[]{left, mid, right, right, right, mid, left, left};
    }

    public static int[] vertical8(int top, int bottom) {
        int mid = blend(top, bottom, 0.5f);
        return new int[]{top, top, top, mid, bottom, bottom, bottom, mid};
    }

    public static int[] corners8(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        int top = blend(topLeft, topRight, 0.5f);
        int right = blend(topRight, bottomRight, 0.5f);
        int bottom = blend(bottomRight, bottomLeft, 0.5f);
        int left = blend(bottomLeft, topLeft, 0.5f);
        return new int[]{topLeft, top, topRight, right, bottomRight, bottom, bottomLeft, left};
    }

    public static int[] rainbow8(int alpha) {
        return new int[]{
                rgba(255, 0, 0, alpha),
                rgba(255, 127, 0, alpha),
                rgba(255, 255, 0, alpha),
                rgba(0, 255, 0, alpha),
                rgba(0, 255, 255, alpha),
                rgba(0, 127, 255, alpha),
                rgba(0, 0, 255, alpha),
                rgba(127, 0, 255, alpha)
        };
    }

    public static float[] uniform8(float value) {
        return new float[]{value, value, value, value, value, value, value, value};
    }

    public static float[] varying8(float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8) {
        return new float[]{v1, v2, v3, v4, v5, v6, v7, v8};
    }

    public static int blend(int color1, int color2, float factor) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int withAlpha(int color, float alpha) {
        return withAlpha(color, (int)(alpha * 255));
    }
}