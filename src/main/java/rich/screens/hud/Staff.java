package rich.screens.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import rich.client.draggables.AbstractHudElement;
import rich.util.ColorUtil;
import rich.util.render.Render2D;
import rich.util.render.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Staff extends AbstractHudElement {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    private static class StaffInfo {
        String name;
        GameProfile profile;

        StaffInfo(String name, GameProfile profile) {
            this.name = name;
            this.profile = profile;
        }
    }

    private Map<String, StaffInfo> staffMap = new LinkedHashMap<>();
    private Map<String, Float> staffAnimations = new LinkedHashMap<>();
    private Set<String> activeStaffIds = new HashSet<>();
    private Identifier cachedRandomSkin = null;

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float FACE_SIZE = 8f;
    private static final float CIRCLE_SIZE = 5f;

    public Staff() {
        super("Staff", 300, 150, 80, 23, true);
        startAnimation();
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.world == null) {
            staffMap.clear();
            activeStaffIds.clear();
            return;
        }

        String myName = mc.player.getName().getString();
        activeStaffIds.clear();

        Scoreboard scoreboard = mc.world.getScoreboard();
        List<Team> teams = new ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));

        Collection<PlayerListEntry> online = mc.player.networkHandler.getPlayerList();
        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : online) {
            if (entry.getProfile() != null && entry.getProfile().name() != null) {
                onlineNames.add(entry.getProfile().name());
            }
        }

        if (cachedRandomSkin == null) {
            for (PlayerListEntry entry : online) {
                if (entry.getSkinTextures() != null && entry.getSkinTextures().body() != null) {
                    cachedRandomSkin = entry.getSkinTextures().body().id();
                    break;
                }
            }
        }

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) continue;
            String name = members.iterator().next();
            if (!NAME_PATTERN.matcher(name).matches()) continue;
            if (name.equals(myName)) continue;

            boolean isOnline = onlineNames.contains(name);

            if (!isOnline) {
                activeStaffIds.add(name);

                if (!staffMap.containsKey(name)) {
                    GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
                    staffMap.put(name, new StaffInfo(name, fakeProfile));
                }

                if (!staffAnimations.containsKey(name)) {
                    staffAnimations.put(name, 0f);
                }
            }
        }
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    private Identifier getSkinTexture(StaffInfo info) {
        if (cachedRandomSkin != null) {
            return cachedRandomSkin;
        }
        return null;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Float> entry : staffAnimations.entrySet()) {
            String id = entry.getKey();
            float currentAnim = entry.getValue();
            float targetAnim = activeStaffIds.contains(id) ? 1f : 0f;
            float newAnim = lerp(currentAnim, targetAnim, deltaTime);

            if (Math.abs(newAnim - targetAnim) < 0.01f) {
                newAnim = targetAnim;
            }

            if (newAnim <= 0.01f && targetAnim == 0f) {
                toRemove.add(id);
            } else {
                staffAnimations.put(id, newAnim);
            }
        }
        for (String id : toRemove) {
            staffAnimations.remove(id);
            staffMap.remove(id);
        }

        float x = getX();
        float y = getY();

        int offset = 23;
        float targetWidth = 80;

        boolean hasAnimatingStaff = !staffAnimations.isEmpty();

        if (!hasAnimatingStaff) {
            offset += 11;
            String name = "ExampleStaff";
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            targetWidth = Math.max(nameWidth + 55, targetWidth);
        } else {
            for (Map.Entry<String, Float> entry : staffAnimations.entrySet()) {
                String id = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                StaffInfo info = staffMap.get(id);
                if (info == null) continue;

                offset += (int) (animation * 11);

                float nameWidth = Fonts.BOLD.getWidth(info.name, 6);
                targetWidth = Math.max(nameWidth + 55, targetWidth);
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

        int staffCount = activeStaffIds.isEmpty() ? 1 : activeStaffIds.size();
        String countText = String.valueOf(staffCount);
        float countTextWidth = Fonts.BOLD.getWidth(countText, 6);
        float staffTextWidth = Fonts.BOLD.getWidth("Staff", 6);

        Render2D.gradientRect(x + getWidth() - 18.5f, y + 5, 14, 12,
                new int[]{
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB(),
                        new Color(52, 52, 52, 255).getRGB()
                },
                3);

        Fonts.ICONS.draw("E", x + getWidth() - 15.5f, y + 7.5f, 8, new Color(165, 165, 165, 255).getRGB());

        Fonts.BOLD.draw("Staff", x + 8, y + 6.5f, 6, new Color(255, 255, 255, 255).getRGB());

        int moduleOffset = 23;

        if (!hasAnimatingStaff) {
            String name = "ExampleStaff";

            float faceX = x + 8;
            float faceY = y + moduleOffset - 2f;

            if (cachedRandomSkin != null) {
                float u0 = 8f / 64f;
                float v0 = 8f / 64f;
                float u1 = 16f / 64f;
                float v1 = 16f / 64f;

                Render2D.texture(cachedRandomSkin, faceX, faceY, FACE_SIZE, FACE_SIZE,
                        u0, v0, u1, v1, new Color(255, 255, 255, 255).getRGB(), 0, 2f);

                float hatU0 = 40f / 64f;
                float hatV0 = 8f / 64f;
                float hatU1 = 48f / 64f;
                float hatV1 = 16f / 64f;

                float hatScale = 1.15f;
                float hatSize = FACE_SIZE * hatScale;
                float hatOffset = (hatSize - FACE_SIZE) / 2f;

                Render2D.texture(cachedRandomSkin, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                        hatU0, hatV0, hatU1, hatV1, new Color(255, 255, 255, 255).getRGB(), 0, 2f);

                int blurTint = ColorUtil.rgba(0, 0, 0, 0);
                Render2D.blur(faceX, faceY, 1, 1, 0f, 0, blurTint);
            } else {
                Render2D.rect(faceX, faceY, FACE_SIZE, FACE_SIZE,
                        new Color(100, 100, 100, 128).getRGB(), 2);
            }

            float nameX = x + 8 + FACE_SIZE + 4;
            Fonts.BOLD.draw(name, nameX, y + moduleOffset - 1.5f, 6,
                    new Color(255, 255, 255, 255).getRGB());

            float circleX = x + getWidth() - 14f;
            float circleY = y + moduleOffset - 0.5f;

            Render2D.gradientRect(circleX - 3, circleY - 2, 11, 9,
                    new int[]{
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB(),
                            new Color(52, 52, 52, 255).getRGB()
                    },
                    3);

            Render2D.outline(circleX - 3, circleY - 2, 11, 9, 0.35f, new Color(90, 90, 90, 255).getRGB(), 3);

            Render2D.rect(circleX, circleY, CIRCLE_SIZE, CIRCLE_SIZE, new Color(255, 80, 80, 255).getRGB(), CIRCLE_SIZE / 2f);

        } else {
            for (Map.Entry<String, Float> entry : staffAnimations.entrySet()) {
                String id = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                StaffInfo info = staffMap.get(id);
                if (info == null) continue;

                int textAlpha = (int) (255 * animation);
                int textColor = new Color(255, 255, 255, textAlpha).getRGB();

                Identifier skinTexture = getSkinTexture(info);
                float faceX = x + 8;
                float faceY = y + moduleOffset - 2f;

                if (skinTexture != null) {
                    float u0 = 8f / 64f;
                    float v0 = 8f / 64f;
                    float u1 = 16f / 64f;
                    float v1 = 16f / 64f;

                    int faceColor = new Color(255, 255, 255, textAlpha).getRGB();
                    Render2D.texture(skinTexture, faceX, faceY, FACE_SIZE, FACE_SIZE,
                            u0, v0, u1, v1, faceColor, 0, 2f);

                    float hatU0 = 40f / 64f;
                    float hatV0 = 8f / 64f;
                    float hatU1 = 48f / 64f;
                    float hatV1 = 16f / 64f;

                    float hatScale = 1.15f;
                    float hatSize = FACE_SIZE * hatScale;
                    float hatOffset = (hatSize - FACE_SIZE) / 2f;

                    Render2D.texture(skinTexture, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                            hatU0, hatV0, hatU1, hatV1, faceColor, 0, 2f);

                    int blurTint = ColorUtil.rgba(0, 0, 0, 0);
                    Render2D.blur(faceX, faceY, 1, 1, 0f, 0, blurTint);
                } else {
                    Render2D.rect(faceX, faceY, FACE_SIZE, FACE_SIZE,
                            new Color(100, 100, 100, textAlpha).getRGB(), 2);
                }

                float nameX = faceX + FACE_SIZE + 4;
                Fonts.BOLD.draw(info.name, nameX, y + moduleOffset - 1.5f, 6, textColor);

                int circleColor = new Color(255, 80, 80, textAlpha).getRGB();
                float circleX = x + getWidth() - 14f;
                float circleY = y + moduleOffset - 0.5f;

                Render2D.gradientRect(circleX - 3, circleY - 2, 11, 9,
                        new int[]{
                                new Color(52, 52, 52, 255).getRGB(),
                                new Color(52, 52, 52, 255).getRGB(),
                                new Color(52, 52, 52, 255).getRGB(),
                                new Color(52, 52, 52, 255).getRGB()
                        },
                        3);

                Render2D.outline(circleX - 3, circleY - 2, 11, 9, 0.35f, new Color(90, 90, 90, 255).getRGB(), 3);

                Render2D.rect(circleX, circleY, CIRCLE_SIZE, CIRCLE_SIZE, circleColor, CIRCLE_SIZE / 2f);

                moduleOffset += (int) (animation * 11);
            }
        }

        Scissor.disable();
    }
}