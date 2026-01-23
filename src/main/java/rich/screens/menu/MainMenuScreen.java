package rich.screens.menu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.sounds.SoundManager;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainMenuScreen extends Screen {

    private static final int BUTTON_SIZE = 42;
    private static final int BUTTON_SPACING = 16;
    private static final int OUTLINE_COLOR = 0x60FFFFFF;
    private static final float BLUR_RADIUS = 15f;
    private static final float OUTLINE_THICKNESS = 1f;
    private static final String[] BUTTON_ICONS = {"a", "b", "x", "s", "i"};

    private static final long UNLOCK_FADE_DURATION = 300L;
    private static final long MENU_APPEAR_DURATION = 800L;
    private static final long MENU_APPEAR_DELAY = 200L;

    private static final float ZOOM_INITIAL = 1.08f;
    private static final float ZOOM_NORMAL = 1.0f;
    private static final float ZOOM_SPEED = 3f;

    private long screenStartTime = 0L;
    private boolean initialized = false;

    private float[] buttonScales = new float[5];
    private float[] buttonHoverProgress = new float[5];
    private int hoveredButton = -1;
    private int lastHoveredButton = -1;
    private long lastRenderTime = 0L;

    private boolean welcomeSoundPlayed = false;
    private boolean isUnlocked = false;
    private long unlockTime = 0L;
    private float unlockTextPulse = 0f;

    private float currentZoom = ZOOM_INITIAL;
    private float targetZoom = ZOOM_INITIAL;

    private float exitButtonRedProgress = 0f;

    public MainMenuScreen() {
        super(Text.literal("Main Menu"));
        for (int i = 0; i < 5; i++) {
            buttonScales[i] = 1f;
            buttonHoverProgress[i] = 0f;
        }
    }

    @Override
    protected void init() {
    }

    private float getScale() {
        return 2f / (float) this.client.options.getGuiScale().getValue();
    }

    private void unlock() {
        if (!isUnlocked) {
            isUnlocked = true;
            unlockTime = Util.getMeasuringTimeMs();
            targetZoom = ZOOM_NORMAL;
        }
    }

    private float getUnlockTextAlpha(long currentTime) {
        if (!isUnlocked) return 1f;
        long elapsed = currentTime - unlockTime;
        return 1f - MathHelper.clamp((float) elapsed / UNLOCK_FADE_DURATION, 0f, 1f);
    }

    private float getMenuProgress(long currentTime) {
        if (!isUnlocked) return 0f;
        long elapsed = currentTime - unlockTime - MENU_APPEAR_DELAY;
        if (elapsed < 0) return 0f;
        return MathHelper.clamp((float) elapsed / MENU_APPEAR_DURATION, 0f, 1f);
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    private float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1f - x, 4);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = Util.getMeasuringTimeMs();

        if (!initialized) {
            screenStartTime = currentTime;
            lastRenderTime = currentTime;
            initialized = true;
        }

        float deltaTime = (currentTime - lastRenderTime) / 1000f;
        lastRenderTime = currentTime;
        deltaTime = MathHelper.clamp(deltaTime, 0f, 0.1f);

        unlockTextPulse += deltaTime * 3f;

        currentZoom = MathHelper.lerp(deltaTime * ZOOM_SPEED, currentZoom, targetZoom);

        float contentAlpha = MathHelper.clamp((float) (currentTime - screenStartTime) / 500f, 0f, 1f);
        float unlockTextAlpha = getUnlockTextAlpha(currentTime);
        float menuProgress = easeOutQuart(getMenuProgress(currentTime));

        if (!welcomeSoundPlayed && menuProgress > 0.1f) {
            SoundManager.playSoundDirect(SoundManager.WELCOME, 1.0f, 1.0f);
            welcomeSoundPlayed = true;
        }

        float scale = getScale();
        float scaledMouseX = mouseX / scale;
        float scaledMouseY = mouseY / scale;

        hoveredButton = (menuProgress > 0.8f) ? getHoveredButton(scaledMouseX, scaledMouseY, scale, menuProgress) : -1;

        if (hoveredButton != lastHoveredButton) {
            lastHoveredButton = hoveredButton;
        }

        updateButtonAnimations(deltaTime);

        Render2D.beginOverlay();
        Render2D.backgroundImage(1.0f, currentZoom);

        Fonts.TEST.drawCentered("Rich Client © All Rights Reserved", this.width / scale / 2f, this.height / scale - 6, 5f, withAlpha(0xFFFFFFFF, (int) (contentAlpha * 100)));

        if (unlockTextAlpha > 0.01f) {
            renderUnlockText(contentAlpha * unlockTextAlpha, scale);
        }

        if (menuProgress > 0.01f) {
            renderTime(contentAlpha * menuProgress, scale, menuProgress);
            renderButtons(scaledMouseX, scaledMouseY, contentAlpha, scale, menuProgress);
        }

        Render2D.endOverlay();
    }

    private void updateButtonAnimations(float deltaTime) {
        for (int i = 0; i < 5; i++) {
            float targetHover = (hoveredButton == i) ? 1f : 0f;
            buttonHoverProgress[i] = MathHelper.lerp(deltaTime * 10f, buttonHoverProgress[i], targetHover);

            float targetScale = (hoveredButton == i) ? 1.08f : 1f;
            buttonScales[i] = MathHelper.lerp(deltaTime * 12f, buttonScales[i], targetScale);
        }

        float targetRed = (hoveredButton == 4) ? 1f : 0f;
        exitButtonRedProgress = MathHelper.lerp(deltaTime * 8f, exitButtonRedProgress, targetRed);
    }

    private void renderUnlockText(float opacity, float scale) {
        if (opacity < 0.01f) return;

        float scaledWidth = this.width / scale;
        float scaledHeight = this.height / scale;
        float centerX = scaledWidth / 2f;
        float centerY = scaledHeight / 2f;

        String text = "Press any key to continue";
        float fontSize = 14f;
        float pulse = (float) Math.sin(unlockTextPulse) * 0.15f + 0.85f;
        int textAlpha = (int) (opacity * 255 * pulse);

        Fonts.REGULARNEW.drawCentered(text, centerX, centerY, fontSize, withAlpha(0xFFFFFFFF, textAlpha));

        float arrowY = centerY + 25;
        float arrowBounce = (float) Math.sin(unlockTextPulse * 1.5f) * 3f;
        int arrowAlpha = (int) (opacity * 200 * pulse);
        Fonts.REGULARNEW.drawCentered("▼", centerX, arrowY + arrowBounce, fontSize, withAlpha(0xFFFFFFFF, arrowAlpha));
    }

    private void renderTime(float opacity, float scale, float menuProgress) {
        float scaledWidth = this.width / scale;
        float scaledHeight = this.height / scale;
        float centerX = scaledWidth / 2f;

        float slideOffset = (1f - menuProgress) * 40f;
        float centerY = scaledHeight / 2f - 55 + slideOffset;

        LocalTime now = LocalTime.now();
        String timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        int textAlpha = (int) (opacity * 255);
        float fontSize = 48f;
        float textHeight = Fonts.BOLD.getHeight(fontSize);

        Fonts.BOLD.drawCentered(timeText, centerX, centerY - textHeight / 2f, fontSize, withAlpha(0xFFFFFFFF, textAlpha));

        String dateText = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.ENGLISH)
        );
        int dateAlpha = (int) (opacity * 200);
        Fonts.BOLD.drawCentered(dateText, centerX, centerY + textHeight / 2f + 4, 12f, withAlpha(0xFFFFFFFF, dateAlpha));
    }

    private void renderButtons(float mouseX, float mouseY, float opacity, float scale, float menuProgress) {
        float scaledWidth = this.width / scale;
        float scaledHeight = this.height / scale;

        float totalWidth = BUTTON_SIZE * 5 + BUTTON_SPACING * 4;
        float startX = (scaledWidth - totalWidth) / 2f;

        float slideOffset = (1f - menuProgress) * 60f;
        float centerY = scaledHeight / 2f + 30 + slideOffset;

        for (int i = 0; i < 5; i++) {
            float buttonDelay = i * 0.12f;
            float buttonProgress = MathHelper.clamp((menuProgress - buttonDelay) / (1f - buttonDelay * 0.5f), 0f, 1f);
            float easedProgress = easeOutCubic(buttonProgress);

            float buttonX = startX + i * (BUTTON_SIZE + BUTTON_SPACING);
            float buttonOpacity = opacity * easedProgress;

            renderCircleButton(i, buttonX, centerY, buttonOpacity);
        }
    }

    private void renderCircleButton(int index, float x, float y, float opacity) {
        if (opacity < 0.01f) return;

        float scale = buttonScales[index];
        float hoverProgress = buttonHoverProgress[index];

        float size = BUTTON_SIZE * scale;
        float halfSize = size / 2f;
        float centerX = x + BUTTON_SIZE / 2f;
        float centerY = y + BUTTON_SIZE / 2f;
        float drawX = centerX - halfSize;
        float drawY = centerY - halfSize;
        float radius = size / 2f;

        int blurColorBase;
        int outlineColorBase;
        int iconColorBase;

        if (index == 4) {
            int r = (int) MathHelper.lerp(exitButtonRedProgress, 80, 140);
            int g = (int) MathHelper.lerp(exitButtonRedProgress, 80, 50);
            int b = (int) MathHelper.lerp(exitButtonRedProgress, 80, 50);
            blurColorBase = new Color(r, g, b, 15).getRGB();

            int outR = (int) MathHelper.lerp(exitButtonRedProgress, 255, 255);
            int outG = (int) MathHelper.lerp(exitButtonRedProgress, 255, 140);
            int outB = (int) MathHelper.lerp(exitButtonRedProgress, 255, 140);
            outlineColorBase = new Color(outR, outG, outB, 96).getRGB();

            int iconR = 255;
            int iconG = (int) MathHelper.lerp(exitButtonRedProgress, 255, 160);
            int iconB = (int) MathHelper.lerp(exitButtonRedProgress, 255, 160);
            iconColorBase = new Color(iconR, iconG, iconB).getRGB();
        } else {
            blurColorBase = new Color(80, 80, 80, 15).getRGB();
            outlineColorBase = OUTLINE_COLOR;
            iconColorBase = 0xFFFFFFFF;
        }

        int blurAlpha = (int) (opacity * 255 * (0.4f + hoverProgress * 0.2f));
        Render2D.blur(drawX, drawY, size, size, BLUR_RADIUS, radius, withAlpha(blurColorBase, blurAlpha));

        int outlineAlpha = (int) (opacity * 155 * (0.3f + hoverProgress * 0.4f));
        Render2D.outline(drawX, drawY, size, size, OUTLINE_THICKNESS, withAlpha(outlineColorBase, outlineAlpha), radius);

        float iconSize = 17f * scale;
        String icon = BUTTON_ICONS[index];
        float iconWidth = Fonts.MAINMENUSCREEN.getWidth(icon, iconSize);
        float iconHeight = Fonts.MAINMENUSCREEN.getHeight(iconSize);

        int iconAlpha = (int) (opacity * 255 * (0.8f + hoverProgress * 0.2f));
        Fonts.MAINMENUSCREEN.draw(icon, centerX - iconWidth / 2f + 0.5f, centerY - iconHeight / 2f, iconSize, withAlpha(iconColorBase, iconAlpha));
    }

    private int getHoveredButton(float mouseX, float mouseY, float scale, float menuProgress) {
        float scaledWidth = this.width / scale;
        float scaledHeight = this.height / scale;

        float totalWidth = BUTTON_SIZE * 5 + BUTTON_SPACING * 4;
        float startX = (scaledWidth - totalWidth) / 2f;

        float slideOffset = (1f - menuProgress) * 60f;
        float centerY = scaledHeight / 2f + 30 + slideOffset;

        for (int i = 0; i < 5; i++) {
            float buttonX = startX + i * (BUTTON_SIZE + BUTTON_SPACING);
            float buttonCenterX = buttonX + BUTTON_SIZE / 2f;
            float buttonCenterY = centerY + BUTTON_SIZE / 2f;

            float dx = mouseX - buttonCenterX;
            float dy = mouseY - buttonCenterY;

            if (dx * dx + dy * dy <= (BUTTON_SIZE / 2f) * (BUTTON_SIZE / 2f)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!isUnlocked) {
            unlock();
            return true;
        }

        if (click.button() == 0 && hoveredButton >= 0) {
            handleButtonClick(hoveredButton);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!isUnlocked) {
            unlock();
            return true;
        }
        return super.keyPressed(input);
    }

    private void handleButtonClick(int index) {
        switch (index) {
            case 0 -> this.client.setScreen(new SelectWorldScreen(this));
            case 1 -> {
                Screen screen = this.client.options.skipMultiplayerWarning
                        ? new MultiplayerScreen(this)
                        : new MultiplayerWarningScreen(this);
                this.client.setScreen(screen);
            }
            case 2 -> {}
            case 3 -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            case 4 -> this.client.scheduleStop();
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }
}