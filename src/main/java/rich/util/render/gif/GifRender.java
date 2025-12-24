package rich.util.render.gif;

import net.minecraft.util.Identifier;
import rich.util.render.Render2D;

import java.util.ArrayList;
import java.util.List;

public class GifRender {

    private static final List<Identifier> avatarFrames = new ArrayList<>();
    private static final List<Identifier> backgroundFrames = new ArrayList<>();

    private static long lastAvatarTime = 0;
    private static long lastBackgroundTime = 0;

    private static int avatarFrameIndex = 0;
    private static int backgroundFrameIndex = 0;

    private static final long AVATAR_DELAY = 30;
    private static final long BACKGROUND_DELAY = 100;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        avatarFrames.clear();
        backgroundFrames.clear();

        for (int i = 1; i <= 119; i++) {
            String frameName = String.format("Image%03d", i);
            Identifier id = Identifier.of("minecraft", "images/gifs/avatar/" + frameName + ".png");
            avatarFrames.add(id);
        }

        for (int i = 1; i <= 22; i++) {
            String frameName = String.format("back%03d", i);
            Identifier id = Identifier.of("minecraft", "images/gifs/back/" + frameName + ".png");
            backgroundFrames.add(id);
        }

        lastAvatarTime = System.currentTimeMillis();
        lastBackgroundTime = System.currentTimeMillis();

        initialized = true;
    }

    public static void drawAvatar(float x, float y, float width, float height, int color) {
        if (!initialized) init();
        if (avatarFrames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAvatarTime >= AVATAR_DELAY) {
            avatarFrameIndex = (avatarFrameIndex + 1) % avatarFrames.size();
            lastAvatarTime = currentTime;
        }

        Identifier frame = avatarFrames.get(avatarFrameIndex);
        Render2D.texture(frame, x, y, width, height, 1, 15, color);
    }

    public static void drawAvatar(float x, float y, float width, float height, float radius, int color) {
        if (!initialized) init();
        if (avatarFrames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAvatarTime >= AVATAR_DELAY) {
            avatarFrameIndex = (avatarFrameIndex + 1) % avatarFrames.size();
            lastAvatarTime = currentTime;
        }

        Identifier frame = avatarFrames.get(avatarFrameIndex);
        Render2D.texture(frame, x, y, width, height, 1f, radius, color);
    }

    public static void drawBackground(float x, float y, float width, float height, int color) {
        if (!initialized) init();
        if (backgroundFrames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackgroundTime >= BACKGROUND_DELAY) {
            backgroundFrameIndex = (backgroundFrameIndex + 1) % backgroundFrames.size();
            lastBackgroundTime = currentTime;
        }

        Identifier frame = backgroundFrames.get(backgroundFrameIndex);
        Render2D.texture(frame, x, y, width, height, color);
    }

    public static void drawBackground(float x, float y, float width, float height, float radius, int color) {
        if (!initialized) init();
        if (backgroundFrames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackgroundTime >= BACKGROUND_DELAY) {
            backgroundFrameIndex = (backgroundFrameIndex + 1) % backgroundFrames.size();
            lastBackgroundTime = currentTime;
        }

        Identifier frame = backgroundFrames.get(backgroundFrameIndex);
        Render2D.texture(frame, x, y, width, height, 1f, radius, color);
    }

    public static void resetAvatar() {
        avatarFrameIndex = 0;
        lastAvatarTime = System.currentTimeMillis();
    }

    public static void resetBackground() {
        backgroundFrameIndex = 0;
        lastBackgroundTime = System.currentTimeMillis();
    }

    public static void reset() {
        resetAvatar();
        resetBackground();
    }
}