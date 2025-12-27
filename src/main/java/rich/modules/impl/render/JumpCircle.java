package rich.modules.impl.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import rich.events.api.EventHandler;
import rich.events.impl.Event3D;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.render.ClientPipelines;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircle extends ModuleStructure {

    final BooleanSetting useCustomColor = new BooleanSetting("Custom color", "").setValue(false);
    final ColorSetting customColor = new ColorSetting("Color", "").value(0x000000).visible(useCustomColor::isValue);
    final SelectSetting image = new SelectSetting("Image", "").value("Circle");

    enum Image {
        Circle("circle");
        final Identifier id;
        Image(String image) {
            id = Identifier.of("minecraft", "images/world/jump.png");
        }
    }

    static class Circle {
        public final Vec3d pos;
        public final long start = System.currentTimeMillis();
        public final long durationMs = 1500;
        public final float startSize = 0.2f;
        public final float endSize = 1.0f;

        public Circle(Vec3d pos) {
            this.pos = pos;
        }

        public float t() {
            return Math.min(1f, (System.currentTimeMillis() - start) / (float) durationMs);
        }

        private float easeOutCubic(float x) {
            float i = 1f - x;
            return 1f - i * i * i;
        }

        private float easeOutSine(float x) {
            return (float) Math.sin((x * Math.PI) / 2.0);
        }

        public boolean alive() {
            return System.currentTimeMillis() - start <= durationMs;
        }

        public float size() {
            float k = easeOutCubic(t());
            float base = startSize + (endSize - startSize) * k;
            float puls = (float) Math.sin(System.currentTimeMillis() * 0.012) * 0.08f * (1f - t());
            return base + puls;
        }

        public float alpha() {
            return 1f - easeOutSine(t());
        }
    }

    private final List<Circle> circles = new CopyOnWriteArrayList<>();
    private boolean wasOnGround = false;

    public JumpCircle() {
        super("JumpCircle", ModuleCategory.RENDER);
        setup(useCustomColor, customColor, image);
    }

    @Override
    public void activate() {
        circles.clear();
        wasOnGround = false;
    }

    @Override
    public void deactivate() {
        circles.clear();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        circles.removeIf(c -> !c.alive());

        boolean onGround = mc.player.isOnGround();
        double vy = mc.player.getVelocity().y;

        if (wasOnGround && !onGround && vy > 0.08) {
            Vec3d origin = new Vec3d(mc.player.getX(), mc.player.getY() + 0.1, mc.player.getZ());
            double yBelow = raycastDownY(origin, 3.5);
            double y = Double.isFinite(yBelow) ? yBelow + 0.01 : Math.floor(mc.player.getBoundingBox().minY) + 0.01;
            circles.add(new Circle(new Vec3d(origin.getX(), y, origin.getZ())));
        }

        wasOnGround = onGround;
    }

    @EventHandler
    public void onRender3D(Event3D e) {
        if (circles.isEmpty()) return;

        MatrixStack stack = e.stack;
        Identifier textureId = Image.Circle.id;
        VertexConsumer consumer = e.buffer.getBuffer(ClientPipelines.TARGET_ESP.apply(textureId));
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        for (Circle c : circles) {
            float alpha = c.alpha();
            float sz = c.size();
            int a = (int) (alpha * 255);

            int c1, c2;
            if (useCustomColor.isValue()) {
                c1 = c2 = customColor.getColor();
            } else {
                c1 = 0xFFFFFF;
                c2 = 0xFF00FF;
            }

            c1 = (c1 & 0x00FFFFFF) | (a << 24);
            c2 = (c2 & 0x00FFFFFF) | (a << 24);

            stack.push();
            stack.translate(c.pos.x - camPos.x, c.pos.y - camPos.y, c.pos.z - camPos.z);

            consumer.vertex(stack.peek(), -sz, 0, -sz).color(c2).texture(0, 0);
            consumer.vertex(stack.peek(), -sz, 0, sz).color(c1).texture(0, 1);
            consumer.vertex(stack.peek(), sz, 0, sz).color(c2).texture(1, 1);
            consumer.vertex(stack.peek(), sz, 0, -sz).color(c1).texture(1, 0);

            stack.pop();
        }
    }

    private double raycastDownY(Vec3d origin, double max) {
        for (double dy = 0; dy <= max; dy += 0.05) {
            Vec3d p = origin.subtract(0, dy, 0);
            BlockPos pos = BlockPos.ofFloored(p);
            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (!shape.isEmpty()) {
                Box box = shape.getBoundingBox();
                return pos.getY() + box.maxY;
            }
        }
        return Double.NaN;
    }
}