package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.Initialization;
import rich.client.draggables.AbstractHudElement;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.animations.Direction;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;
import rich.util.string.KeyHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HotKeys extends AbstractHudElement {

    private List<ModuleStructure> keysList = new ArrayList<>();
    private long lastKeyChange = 0;
    private String currentRandomKey = "NONE";

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;

    public HotKeys() {
        super("HotKeys", 300, 40, 80, 23, true);
        startAnimation();
    }

    @Override
    public void tick() {
        if (Initialization.getInstance() == null ||
                Initialization.getInstance().getManager() == null ||
                Initialization.getInstance().getManager().getModuleProvider() == null) {
            return;
        }

        keysList = Initialization.getInstance().getManager().getModuleProvider().getModuleStructures().stream()
                .filter(module -> module.getAnimation().getOutput().floatValue() > 0
                        && module.getKey() != GLFW.GLFW_KEY_UNKNOWN)
                .toList();

        if (keysList.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKeyChange >= 1000) {
                List<String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey = availableKeys.get(new Random().nextInt(availableKeys.size()));
                lastKeyChange = currentTime;
            }
        }
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        long activeModules = keysList.stream()
                .filter(m -> !m.getAnimation().isFinished(Direction.BACKWARDS))
                .count();

        String moduleCountText = String.valueOf(activeModules);
        float countTextWidth = Fonts.BOLD.getWidth(moduleCountText, 6);
        float activeTextWidth = Fonts.BOLD.getWidth("Active:", 6);

        int offset = 23;
        float targetWidth = 80;

        if (keysList.isEmpty()) {
            offset += 11;
            String name = "Example Module";
            String bind = "[" + currentRandomKey + "]";
            float bindWidth = Fonts.BOLD.getWidth(bind, 6);
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            targetWidth = Math.max(nameWidth + bindWidth + 50, targetWidth);
        } else {
            for (ModuleStructure module : keysList) {
                float animation = module.getAnimation().getOutput().floatValue();
                offset += (int) (animation * 11);

                if (animation <= 0) continue;
                String bind = "[" + KeyHelper.getKeyName(module.getKey()) + "]";
                float bindWidth = Fonts.BOLD.getWidth(bind, 6);
                float nameWidth = Fonts.BOLD.getWidth(module.getName(), 6);
                targetWidth = Math.max(nameWidth + bindWidth + 50, targetWidth);
            }
        }

        float targetHeight = offset + 2;

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) {
            animatedWidth = targetWidth;
        }
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) {
            animatedHeight = targetHeight;
        }

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        float contentHeight = animatedHeight;

        if (contentHeight > 0) {
            Render2D.gradientRect(x, y, getWidth(), contentHeight,
                    new int[]{
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(32, 32, 32, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(32, 32, 32, 255).getRGB()
                    },
                    5);
            Render2D.outline(x, y, getWidth(), contentHeight, 0.35f, new Color(90, 90, 90, 255).getRGB(), 5);
        }

        Scissor.enable(x, y, getWidth(), contentHeight);

        Render2D.gradientRect(x + getWidth() - countTextWidth - activeTextWidth + 2, y + 5, 14, 12,
                new int[]{
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB()
                },
                3);

        Fonts.HUD_ICONS.draw("g", x + getWidth() - countTextWidth - activeTextWidth + 4, y + 6, 10, new Color(165, 165, 165, 255).getRGB());

        Fonts.BOLD.draw("Binds", x + 8, y + 6.5f, 6, new Color(255, 255, 255, 255).getRGB());

        int moduleOffset = 23;

        if (keysList.isEmpty()) {
            String name = "Example Module";
            String bind = "[" + currentRandomKey + "]";

            float bindWidth = Fonts.BOLD.getWidth(bind, 6);

            float bindBoxX = x + getWidth() - bindWidth - 11.5f;

            Render2D.gradientRect(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9,
                    new int[]{
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB()
                    },
                    3);

            Render2D.outline(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9, 0.05f,
                    new Color(132, 132, 132, 255).getRGB(), 2);

            Render2D.rect(x + 8, y + moduleOffset - 1, 1f, 7,
                    new Color(155, 155, 155, 128).getRGB(), 1);
            Fonts.BOLD.draw(name, x + 13, y + moduleOffset - 1.5f, 6,
                    new Color(255, 255, 255, 255).getRGB());
            Fonts.BOLD.draw(bind, bindBoxX + 2, y + moduleOffset - 1, 6,
                    new Color(165, 165, 165, 255).getRGB());
        } else {
            for (ModuleStructure module : keysList) {
                float animation = module.getAnimation().getOutput().floatValue();
                if (animation <= 0) continue;

                String bind = "[" + KeyHelper.getKeyName(module.getKey()) + "]";

                float bindWidth = Fonts.BOLD.getWidth(bind, 6);

                int textAlpha = (int) (255 * animation);
                int textColor = new Color(255, 255, 255, textAlpha).getRGB();
                int accentColor = new Color(165, 165, 165, textAlpha).getRGB();
                int separatorColor = new Color(155, 155, 155, (int) (128 * animation)).getRGB();

                float bindBoxX = x + getWidth() - bindWidth - 11.5f;

                Render2D.gradientRect(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9,
                        new int[]{
                                new Color(52, 52, 52, textAlpha).getRGB(),
                                new Color(52, 52, 52, textAlpha).getRGB(),
                                new Color(52, 52, 52, textAlpha).getRGB(),
                                new Color(52, 52, 52, textAlpha).getRGB()
                        },
                        3);

                Render2D.outline(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9, 0.05f,
                        new Color(132, 132, 132, textAlpha).getRGB(), 2);

                Render2D.rect(x + 8, y + moduleOffset - 1, 1f, 7, separatorColor, 1);
                Fonts.BOLD.draw(module.getName(), x + 13, y + moduleOffset - 1.5f, 6, textColor);
                Fonts.BOLD.draw(bind, bindBoxX + 2, y + moduleOffset - 1, 6, accentColor);

                moduleOffset += (int) (animation * 11);
            }
        }

        Scissor.disable();
    }
}