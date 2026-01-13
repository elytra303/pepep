package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import rich.events.api.EventHandler;
import rich.events.impl.DrawEvent;
import rich.events.impl.TickEvent;
import rich.events.impl.WorldLoadEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.combat.AntiBot;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.math.Projection;
import rich.util.network.Network;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.item.ItemRender;
import rich.util.render.render3D.Render3D;
import rich.util.repository.friend.FriendUtils;
import rich.util.string.PlayerInteractionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Esp extends ModuleStructure {

    public static Esp getInstance() {
        return Instance.get(Esp.class);
    }

    Identifier TEXTURE = Identifier.of("textures/features/esp/container.png");
    List<PlayerEntity> players = new ArrayList<>();

    public MultiSelectSetting entityType = new MultiSelectSetting("Тип сущности", "Сущности, которые будут отображаться")
            .value("Player", "Item", "TNT").selected("Player", "Item");

    MultiSelectSetting playerSetting = new MultiSelectSetting("Настройки игрока", "Настройки для игроков")
            .value("Box", "Armor", "NameTags", "Hand Items").selected("Box", "Armor", "NameTags", "Hand Items")
            .visible(() -> entityType.isSelected("Player"));

    public SelectSetting boxType = new SelectSetting("Тип", "Тип")
            .value("Corner", "Full", "3D Box", "Skeleton").selected("3D Box")
            .visible(() -> playerSetting.isSelected("Box"));

    public BooleanSetting flatBoxOutline = new BooleanSetting("Контур", "Контур для плоских боксов")
            .visible(() -> playerSetting.isSelected("Box") && (boxType.isSelected("Corner") || boxType.isSelected("Full")));

    public SliderSettings boxAlpha = new SliderSettings("Прозрачность", "Прозрачность бокса")
            .setValue(1.0F).range(0.1F, 1.0F).visible(() -> boxType.isSelected("3D Box"));

    public SliderSettings skeletonWidth = new SliderSettings("Толщина линий", "Толщина линий скелета")
            .setValue(2.5f).range(2.5f, 4.0f).visible(() -> boxType.isSelected("Skeleton"));

    private static final float DISTANCE = 128.0f;

    public Esp() {
        super("Esp", "Esp", ModuleCategory.RENDER);
        setup(entityType, playerSetting, boxType, flatBoxOutline, boxAlpha, skeletonWidth);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world != null) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player)
                    .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
                    .forEach(players::add);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!entityType.isSelected("Player")) return;
        float tickDelta = e.getPartialTicks();

        for (PlayerEntity player : players) {
            if (player == null || player == mc.player) continue;
            if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;

            double interpX = MathHelper.lerp(tickDelta, player.lastX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.lastY, player.getY());
            double interpZ = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());
            Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);

            float distance = (float) mc.gameRenderer.getCamera().getCameraPos().distanceTo(interpCenter);
            if (distance < 1) continue;

            boolean friend = FriendUtils.isFriend(player);
            int baseColor = friend ? getFriendColor() : getClientColor();
            int alpha = (int) (boxAlpha.getValue() * 255);
            int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
            int outlineColor = baseColor | 0xFF000000;

            if (boxType.isSelected("3D Box")) {
                Box interpBox = player.getDimensions(player.getPose()).getBoxAt(interpX, interpY, interpZ);
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, false, false);
            } else if (boxType.isSelected("Skeleton") && playerSetting.isSelected("Box")) {
                if (distance > DISTANCE) continue;
                renderSkeleton(player, tickDelta, baseColor);
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        float tickDelta = e.getPartialTicks();
        float size = 5.5f;

        if (entityType.isSelected("Player")) {
            for (PlayerEntity player : players) {
                if (player == null || player == mc.player) continue;
                if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;

                Vector4d vec4d = Projection.getVector4D(player, tickDelta);
                float distance = (float) mc.gameRenderer.getCamera().getCameraPos().distanceTo(player.getBoundingBox().getCenter());
                boolean friend = FriendUtils.isFriend(player);

                if (distance < 1) continue;
                if (Projection.cantSee(vec4d)) continue;

                if (playerSetting.isSelected("Box") && !boxType.isSelected("Skeleton") && !boxType.isSelected("3D Box")) {
                    drawBox(friend, vec4d, player);
                }
                if (playerSetting.isSelected("Armor")) {
                    drawArmor(context, player, vec4d);
                }
                if (playerSetting.isSelected("Hand Items")) {
                    drawHands(context, player, vec4d, size);
                }

                String text = getTextPlayer(player, friend);
                drawText(text, Projection.centerX(vec4d), vec4d.y - 2, size);
            }
        }

        List<Entity> entities = PlayerInteractionHelper.streamEntities()
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .toList();

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity item && entityType.isSelected("Item")) {
                Vector4d vec4d = Projection.getVector4D(entity, tickDelta);
                ItemStack stack = item.getStack();
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();

                if (Projection.cantSee(vec4d)) continue;

                String text = item.getStack().getName().getString();


                if (!list.isEmpty()) {
                    drawShulkerBox(context, stack, list, vec4d);
                } else {
                    drawText(text, Projection.centerX(vec4d), vec4d.y, size);
                }
            } else if (entity instanceof TntEntity tnt && entityType.isSelected("TNT")) {
                Vector4d vec4d = Projection.getVector4D(entity, tickDelta);
                if (Projection.cantSee(vec4d)) continue;
                drawText(tnt.getDisplayName().getString(), Projection.centerX(vec4d), vec4d.y, size);
            }
        }
    }

    private void renderSkeleton(PlayerEntity player, float partialTicks, int color) {
        Vec3d pos = Projection.interpolate(player, partialTicks);
        float width = skeletonWidth.getValue();

        float limbSwing = player.limbAnimator.getAnimationProgress(partialTicks);
        float limbSwingAmount = player.limbAnimator.getAmplitude(partialTicks);

        float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, player.lastBodyYaw, player.bodyYaw);
        float bodyYawRad = (float) Math.toRadians(-bodyYaw + 90);

        boolean isSwimming = player.isSwimming() || player.isGliding();
        float sneakOffset = player.isSneaking() ? 0.2f : 0f;
        float swimOffset = isSwimming ? 0.6f : 0f;

        Vec3d head = pos.add(0, 1.62f - sneakOffset - swimOffset, 0);
        Vec3d neck = pos.add(0, 1.4f - sneakOffset - swimOffset, 0);
        Vec3d body = pos.add(0, 0.9f - sneakOffset - swimOffset, 0);
        Vec3d pelvis = pos.add(0, 0.6f - sneakOffset - swimOffset, 0);

        Render3D.drawLine(head, neck, color, width, false);
        Render3D.drawLine(neck, body, color, width, false);
        Render3D.drawLine(body, pelvis, color, width, false);

        float rightArmSwing = MathHelper.cos(limbSwing * 0.6662f) * limbSwingAmount * 0.5f;
        float leftArmSwing = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * limbSwingAmount * 0.5f;
        float rightLegSwing = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * limbSwingAmount * 0.7f;
        float leftLegSwing = MathHelper.cos(limbSwing * 0.6662f) * limbSwingAmount * 0.7f;

        Vec3d rightShoulder = neck.add(Math.sin(bodyYawRad) * 0.3, -0.1, Math.cos(bodyYawRad) * 0.3);
        Vec3d rightElbow = rightShoulder.add(
                Math.sin(bodyYawRad) * 0.05 + Math.sin(bodyYawRad + Math.PI / 2) * rightArmSwing * 0.15,
                -0.25 - Math.abs(rightArmSwing) * 0.1,
                Math.cos(bodyYawRad) * 0.05 + Math.cos(bodyYawRad + Math.PI / 2) * rightArmSwing * 0.15
        );
        Vec3d rightHand = rightElbow.add(
                Math.sin(bodyYawRad + Math.PI / 2) * rightArmSwing * 0.1,
                -0.25 - Math.abs(rightArmSwing) * 0.05,
                Math.cos(bodyYawRad + Math.PI / 2) * rightArmSwing * 0.1
        );
        Render3D.drawLine(rightShoulder, rightElbow, color, width, false);
        Render3D.drawLine(rightElbow, rightHand, color, width, false);

        Vec3d leftShoulder = neck.add(-Math.sin(bodyYawRad) * 0.3, -0.1, -Math.cos(bodyYawRad) * 0.3);
        Vec3d leftElbow = leftShoulder.add(
                -Math.sin(bodyYawRad) * 0.05 + Math.sin(bodyYawRad + Math.PI / 2) * leftArmSwing * 0.15,
                -0.25 - Math.abs(leftArmSwing) * 0.1,
                -Math.cos(bodyYawRad) * 0.05 + Math.cos(bodyYawRad + Math.PI / 2) * leftArmSwing * 0.15
        );
        Vec3d leftHand = leftElbow.add(
                Math.sin(bodyYawRad + Math.PI / 2) * leftArmSwing * 0.1,
                -0.25 - Math.abs(leftArmSwing) * 0.05,
                Math.cos(bodyYawRad + Math.PI / 2) * leftArmSwing * 0.1
        );
        Render3D.drawLine(leftShoulder, leftElbow, color, width, false);
        Render3D.drawLine(leftElbow, leftHand, color, width, false);

        Vec3d rightHip = pelvis.add(Math.sin(bodyYawRad) * 0.15, 0, Math.cos(bodyYawRad) * 0.15);
        Vec3d rightKnee = rightHip.add(
                Math.sin(bodyYawRad + Math.PI / 2) * rightLegSwing * 0.1,
                -0.35 + Math.max(0, rightLegSwing) * 0.05,
                Math.cos(bodyYawRad + Math.PI / 2) * rightLegSwing * 0.1
        );
        Vec3d rightFoot = rightKnee.add(
                Math.sin(bodyYawRad + Math.PI / 2) * rightLegSwing * 0.08,
                -0.35 - Math.max(0, -rightLegSwing) * 0.05,
                Math.cos(bodyYawRad + Math.PI / 2) * rightLegSwing * 0.08
        );
        Render3D.drawLine(rightHip, rightKnee, color, width, false);
        Render3D.drawLine(rightKnee, rightFoot, color, width, false);

        Vec3d leftHip = pelvis.add(-Math.sin(bodyYawRad) * 0.15, 0, -Math.cos(bodyYawRad) * 0.15);
        Vec3d leftKnee = leftHip.add(
                Math.sin(bodyYawRad + Math.PI / 2) * leftLegSwing * 0.1,
                -0.35 + Math.max(0, leftLegSwing) * 0.05,
                Math.cos(bodyYawRad + Math.PI / 2) * leftLegSwing * 0.1
        );
        Vec3d leftFoot = leftKnee.add(
                Math.sin(bodyYawRad + Math.PI / 2) * leftLegSwing * 0.08,
                -0.35 - Math.max(0, -leftLegSwing) * 0.05,
                Math.cos(bodyYawRad + Math.PI / 2) * leftLegSwing * 0.08
        );
        Render3D.drawLine(leftHip, leftKnee, color, width, false);
        Render3D.drawLine(leftKnee, leftFoot, color, width, false);

        Render3D.drawLine(rightShoulder, leftShoulder, color, width, false);
        Render3D.drawLine(rightHip, leftHip, color, width, false);
    }

    private void drawBox(boolean friend, Vector4d vec, PlayerEntity player) {
        int client = friend ? getFriendColor() : getClientColor();
        int black = 0x80000000;

        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 3;

        if (boxType.isSelected("Corner")) {
            Render2D.rect(posX - 0.5F, posY - 0.5F, size, 0.5F, client);
            Render2D.rect(posX - 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2D.rect(posX - 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2D.rect(posX - 0.5F, endPosY - 0.5F, size, 0.5F, client);
            Render2D.rect(endPosX - size + 1, posY - 0.5F, size, 0.5F, client);
            Render2D.rect(endPosX + 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2D.rect(endPosX + 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2D.rect(endPosX - size + 1, endPosY - 0.5F, size, 0.5F, client);

            if (flatBoxOutline.isValue()) {
                Render2D.rect(posX - 1F, posY - 1, size + 1, 1.5F, black);
                Render2D.rect(posX - 1F, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.rect(posX - 1F, endPosY - size - 1, 1.5F, size, black);
                Render2D.rect(posX - 1F, endPosY - 1, size + 1, 1.5F, black);
                Render2D.rect(endPosX - size + 0.5F, posY - 1, size + 1, 1.5F, black);
                Render2D.rect(endPosX, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.rect(endPosX, endPosY - size - 1, 1.5F, size, black);
                Render2D.rect(endPosX - size + 0.5F, endPosY - 1, size + 1, 1.5F, black);
            }
        } else if (boxType.isSelected("Full")) {
            if (flatBoxOutline.isValue()) {
                Render2D.rect(posX - 1F, posY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2D.rect(posX - 1F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
                Render2D.rect(posX - 1F, endPosY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2D.rect(endPosX - 0.5F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
            }
            Render2D.rect(posX - 0.5F, posY - 0.5F, endPosX - posX + 1F, 0.5F, client);
            Render2D.rect(posX - 0.5F, posY - 0.5F, 0.5F, endPosY - posY + 1F, client);
            Render2D.rect(posX - 0.5F, endPosY - 0.5F, endPosX - posX + 1F, 0.5F, client);
            Render2D.rect(endPosX, posY - 0.5F, 0.5F, endPosY - posY + 1F, client);
        }
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec) {
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        float posX = (float) (Projection.centerX(vec) - items.size() * 4.5f);
        float posY = (float) (vec.y - 20);
        float offset = 0;

        for (ItemStack stack : items) {
            ItemRender.drawItemWithContext(context, stack, posX + offset, posY, 0.5F, 1.0F);
            offset += 11;
        }
    }

    private void drawHands(DrawContext context, PlayerEntity player, Vector4d vec, float size) {
        double posY = vec.w;

        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

        for (ItemStack stack : new ItemStack[]{mainHand, offHand}) {
            if (stack.isEmpty()) continue;
            String text = stack.getName().getString();
            posY += Fonts.BOLD.getHeight(size) / 2.0 + 6;
            drawText(text, Projection.centerX(vec), posY, size);
        }
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        int width = 176;
        int height = 67;
        int color = ((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color | 0xFF000000;

        float drawX = (float) (Projection.centerX(vec) - (double) width / 4);
        float drawY = (float) (vec.w + 2);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(drawX, drawY);
        context.getMatrices().scale(0.5F, 0.5F);

        Render2D.texture(TEXTURE, 0, 0, width, height, color);

        int posX = 7;
        int posY = 6;
        for (ItemStack stack : stacks) {
            ItemRender.drawItemWithContext(context, stack, posX, posY, 1F, 1F);
            posX += 18;
            if (posX >= 165) {
                posY += 18;
                posX = 7;
            }
        }
        context.getMatrices().popMatrix();
    }

    private void drawText(String text, double startX, double startY, float size) {
        float width = Fonts.TEST.getWidth(text, size);
        float height = Fonts.TEST.getHeight(size);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;

         Render2D.rect(posX - 4, posY - 1, width + 8, height + 2, 0x80000000,  0f);
        Fonts.TEST.draw(text, posX, posY, size, 0xFFFFFFFF);
    }

    private String getTextPlayer(PlayerEntity player, boolean friend) {
        float health = getHealth(player);
        StringBuilder text = new StringBuilder();

        if (friend) text.append("[Friend] - ");
        if (AntiBot.getInstance() != null && AntiBot.getInstance().isState() && AntiBot.getInstance().isBot(player)) {
            text.append("[BOT] ");
        }

        if (playerSetting.isSelected("NameTags")) {
            text.append(player.getDisplayName().getString());
        } else {
            text.append(player.getName().getString());
        }

        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        if (offHand.getItem().equals(Items.PLAYER_HEAD) || offHand.getItem().equals(Items.TOTEM_OF_UNDYING)) {
            text.append(getSphere(offHand));
        }
//
//        if (health >= 0 && health <= player.getMaxHealth()) {
//            text.append(" [").append(getHealthString(health)).append("]");
//        }

        return text.toString();
    }

    private String getSphere(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (Network.isFunTime() && component != null) {
            NbtCompound compound = component.copyNbt();
            int tslevel = compound.getInt("tslevel").orElse(0);
            if (tslevel != 0) {
                String donItem = compound.getString("don-item").orElse("");
                return " [" + donItem.replace("sphere-", "").toUpperCase() + "]";
            }
        }
        return "";
    }

    private float getHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private String getHealthString(float hp) {
        return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
    }

    private int getFriendColor() {
        return 0xFF00FF00;
    }

    private int getClientColor() {
        return 0xFF5555FF;
    }
}