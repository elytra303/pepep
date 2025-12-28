package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;
import rich.util.render.gif.GifRender;

import java.awt.*;

public class AvatarRenderer {

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        int alphaFon = (int) (105 * alphaMultiplier);
        int alphaText = (int) (200 * alphaMultiplier);

        context.getMatrices().pushMatrix();
        GifRender.drawBackground(bgX + 12.5f, bgY + 12.5f, 70, 30, 7, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(42, 42, 42, alpha).getRGB(), 15);
        GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 33, bgY + 33, 5, 5, new Color(0, 255, 0, alpha).getRGB(), 10);
        context.getMatrices().popMatrix();

        Render2D.rect(bgX + 12.5f, bgY + 12.5f, 70, 30, new Color(0, 0, 0, alphaFon).getRGB(), 7);

        float textX = bgX + 44;
        float textY = bgY + 22;
        float maxTextWidth = 35f;
        float textHeight = 14f;

        Scissor.enable(textX, textY - 2, maxTextWidth, textHeight);
        Fonts.BOLD.draw("Baflllik", textX, textY, 6, new Color(255, 255, 255, alphaText).getRGB());
        Fonts.BOLD.draw("Uid: 1", textX, textY + 7, 5, new Color(255, 255, 255, alphaText).getRGB());
        Scissor.disable();
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}