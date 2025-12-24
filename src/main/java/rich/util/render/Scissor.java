package rich.util.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

public class Scissor {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void enable(float x, float y, float width, float height, float guiScale) {
        int windowHeight = mc.getWindow().getHeight();

        int scissorX = (int) (x * guiScale);
        int scissorY = (int) (windowHeight - (y + height) * guiScale);
        int scissorWidth = (int) (width * guiScale);
        int scissorHeight = (int) (height * guiScale);

        scissorX = Math.max(0, scissorX);
        scissorY = Math.max(0, scissorY);
        scissorWidth = Math.max(0, scissorWidth);
        scissorHeight = Math.max(0, scissorHeight);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void enable(float x, float y, float width, float height) {
        int currentGuiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        enable(x, y, width, height, currentGuiScale);
    }

    public static void disable() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}