package rich.util.render.render3D;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rich.IMinecraft;
import rich.events.impl.WorldRenderEvent;
import rich.util.ColorUtil;
import rich.util.math.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Render3D implements IMinecraft {
    private final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<>();
    private final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();

    public final List<Line> LINE_DEPTH = new ArrayList<>();
    public final List<Line> LINE = new ArrayList<>();
    public final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public final List<Quad> QUAD = new ArrayList<>();
    public final List<GradientQuad> GRADIENT_QUAD = new ArrayList<>();
    public final List<GradientQuad> GRADIENT_QUAD_DEPTH = new ArrayList<>();

    @Setter
    public Matrix4f lastProjMat = new Matrix4f();
    @Setter
    public MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();
    @Setter
    public float lastTickDelta = 1.0f;

    private float espValue = 1f;
    private float espSpeed = 1f;
    private float prevEspValue;
    private float circleStep;
    private boolean flipSpeed;

    private double smoothY = 0;
    private double smoothY2 = 0;

    public void updateTargetEsp() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
        circleStep += 0.06f;
    }

    public float getEspValue() {
        return espValue;
    }

    public float getPrevEspValue() {
        return prevEspValue;
    }

    public float getCircleStep() {
        return circleStep;
    }

    private double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    private double smoothSinAnimation(double input) {
        double sin = (Math.sin(input) + 1) / 2;
        return easeInOutSine(sin);
    }

    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        renderGradientQuads(matrices, immediate, cameraPos);
        renderQuads(matrices, immediate, cameraPos);
        renderLines(matrices, immediate, cameraPos);

        immediate.draw();
    }

    private void renderLines(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (LINE.isEmpty() && LINE_DEPTH.isEmpty()) return;

        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lines());

        for (Line line : LINE) {
            drawLineVertex(matrices, buffer, line, cameraPos);
        }
        for (Line line : LINE_DEPTH) {
            drawLineVertex(matrices, buffer, line, cameraPos);
        }

        LINE.clear();
        LINE_DEPTH.clear();
    }

    private void drawLineVertex(MatrixStack matrices, VertexConsumer buffer, Line line, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normal = getNormal(line.start.toVector3f(), line.end.toVector3f());

        float x1 = (float) (line.start.x - cameraPos.x);
        float y1 = (float) (line.start.y - cameraPos.y);
        float z1 = (float) (line.start.z - cameraPos.z);

        float x2 = (float) (line.end.x - cameraPos.x);
        float y2 = (float) (line.end.y - cameraPos.y);
        float z2 = (float) (line.end.z - cameraPos.z);

        buffer.vertex(entry, x1, y1, z1)
                .color(line.colorStart)
                .normal(entry, normal)
                .lineWidth(line.width);
        buffer.vertex(entry, x2, y2, z2)
                .color(line.colorEnd)
                .normal(entry, normal)
                .lineWidth(line.width);
    }

    private void renderQuads(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (QUAD.isEmpty() && QUAD_DEPTH.isEmpty()) return;

        VertexConsumer buffer = immediate.getBuffer(RenderLayers.debugFilledBox());

        for (Quad quad : QUAD) {
            drawQuadVertex(matrices, buffer, quad, cameraPos);
        }
        for (Quad quad : QUAD_DEPTH) {
            drawQuadVertex(matrices, buffer, quad, cameraPos);
        }

        QUAD.clear();
        QUAD_DEPTH.clear();
    }

    private void drawQuadVertex(MatrixStack matrices, VertexConsumer buffer, Quad quad, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();

        float x1 = (float) (quad.x.x - cameraPos.x);
        float y1 = (float) (quad.x.y - cameraPos.y);
        float z1 = (float) (quad.x.z - cameraPos.z);

        float x2 = (float) (quad.y.x - cameraPos.x);
        float y2 = (float) (quad.y.y - cameraPos.y);
        float z2 = (float) (quad.y.z - cameraPos.z);

        float x3 = (float) (quad.w.x - cameraPos.x);
        float y3 = (float) (quad.w.y - cameraPos.y);
        float z3 = (float) (quad.w.z - cameraPos.z);

        float x4 = (float) (quad.z.x - cameraPos.x);
        float y4 = (float) (quad.z.y - cameraPos.y);
        float z4 = (float) (quad.z.z - cameraPos.z);

        buffer.vertex(entry, x1, y1, z1).color(quad.color);
        buffer.vertex(entry, x2, y2, z2).color(quad.color);
        buffer.vertex(entry, x3, y3, z3).color(quad.color);
        buffer.vertex(entry, x4, y4, z4).color(quad.color);
    }

    private void renderGradientQuads(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (GRADIENT_QUAD.isEmpty() && GRADIENT_QUAD_DEPTH.isEmpty()) return;

        VertexConsumer buffer = immediate.getBuffer(RenderLayers.debugFilledBox());

        for (GradientQuad quad : GRADIENT_QUAD) {
            drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
        }
        for (GradientQuad quad : GRADIENT_QUAD_DEPTH) {
            drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
        }

        GRADIENT_QUAD.clear();
        GRADIENT_QUAD_DEPTH.clear();
    }

    private void drawGradientQuadVertex(MatrixStack matrices, VertexConsumer buffer, GradientQuad quad, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();

        float x1 = (float) (quad.p1.x - cameraPos.x);
        float y1 = (float) (quad.p1.y - cameraPos.y);
        float z1 = (float) (quad.p1.z - cameraPos.z);

        float x2 = (float) (quad.p2.x - cameraPos.x);
        float y2 = (float) (quad.p2.y - cameraPos.y);
        float z2 = (float) (quad.p2.z - cameraPos.z);

        float x3 = (float) (quad.p3.x - cameraPos.x);
        float y3 = (float) (quad.p3.y - cameraPos.y);
        float z3 = (float) (quad.p3.z - cameraPos.z);

        float x4 = (float) (quad.p4.x - cameraPos.x);
        float y4 = (float) (quad.p4.y - cameraPos.y);
        float z4 = (float) (quad.p4.z - cameraPos.z);

        buffer.vertex(entry, x1, y1, z1).color(quad.c1);
        buffer.vertex(entry, x2, y2, z2).color(quad.c2);
        buffer.vertex(entry, x3, y3, z3).color(quad.c3);
        buffer.vertex(entry, x4, y4, z4).color(quad.c4);
    }

    public void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red, int baseColor) {
        double cs = MathUtils.interpolate(circleStep - 0.17, circleStep);
        Vec3d target = MathUtils.interpolate(lastTarget);
        boolean canSee = mc.player != null && mc.player.canSee(lastTarget);

        float hitEffect = Math.min(red * 2f, 1f);
        float distanceMultiplier = 1.0f + (float) Math.sin(hitEffect * Math.PI) * 0.18f;

        int size = 64;

        float entityWidth = lastTarget.getWidth() * distanceMultiplier;
        float entityHeight = lastTarget.getHeight();

        double targetY = smoothSinAnimation(cs) * entityHeight;
        double targetY2 = smoothSinAnimation(cs - 0.35) * entityHeight;

        smoothY = lerp(smoothY, targetY, 0.12);
        smoothY2 = lerp(smoothY2, targetY2, 0.10);

        int color = ColorUtil.multRed(baseColor, 1 + red * 125);

        int brightColor = ColorUtil.multAlpha(color, 0.8f * anim);
        int fadeColor = ColorUtil.multAlpha(color, 0f);

        for (int i = 0; i <= size; i++) {
            Vec3d cosSin = MathUtils.cosSin(i, size, entityWidth);
            Vec3d nextCosSin = MathUtils.cosSin(i + 1, size, entityWidth);

            Vec3d circlePoint = target.add(cosSin.x, smoothY, cosSin.z);
            Vec3d trailPoint = target.add(cosSin.x, smoothY2, cosSin.z);
            Vec3d nextCirclePoint = target.add(nextCosSin.x, smoothY, nextCosSin.z);
            Vec3d nextTrailPoint = target.add(nextCosSin.x, smoothY2, nextCosSin.z);

            drawGradientQuad(
                    circlePoint,
                    nextCirclePoint,
                    nextTrailPoint,
                    trailPoint,
                    brightColor,
                    brightColor,
                    fadeColor,
                    fadeColor,
                    canSee
            );

            drawGradientQuad(
                    trailPoint,
                    nextTrailPoint,
                    nextCirclePoint,
                    circlePoint,
                    fadeColor,
                    fadeColor,
                    brightColor,
                    brightColor,
                    canSee
            );

            int trailColorTop = ColorUtil.multAlpha(color, 0.15f * anim);
            int trailColorBottom = ColorUtil.multAlpha(color, 0f);
            drawLineGradient(circlePoint, trailPoint, trailColorTop, trailColorBottom, 6f, canSee);

            int circleColor = ColorUtil.multAlpha(color, 1f * anim);
            drawLine(circlePoint, nextCirclePoint, circleColor, 2f, canSee);
        }
    }

    private double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public void drawGradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4, boolean depth) {
        GradientQuad quad = new GradientQuad(p1, p2, p3, p4, c1, c2, c3, c4);
        if (depth) GRADIENT_QUAD_DEPTH.add(quad);
        else GRADIENT_QUAD.add(quad);
    }

    public void drawLineGradient(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(null, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }

    public Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        if (sqrt < 0.0001f) return new Vector3f(0, 1, 0);
        return normal.div(sqrt);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (SHAPE_BOXES.containsKey(voxelShape)) {
            SHAPE_BOXES.get(voxelShape).forEach(box -> {
                Box offsetBox = box.offset(blockPos);
                drawBox(offsetBox, color, width, true, fill, depth);
            });
            return;
        }
        SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
    }

    public void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);

        if (SHAPE_OUTLINES.containsKey(voxelShape)) {
            Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.get(voxelShape);
            if (fill) {
                pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
            }
            pair.getRight().forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
            return;
        }
        List<Line> lines = new ArrayList<>();
        voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) ->
                lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
        SHAPE_OUTLINES.put(voxelShape, new Pair<>(voxelShape.getBoundingBoxes(), lines));
    }

    public void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }

    public void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        drawBox(null, box, color, width, line, fill, depth);
    }

    public void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        if (fill) {
            int fillColor = ColorUtil.multAlpha(color, 0.3f);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }

        if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }

    public void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }

    public void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }

    public void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        drawQuad(null, x, y, w, z, color, depth);
    }

    public void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) QUAD_DEPTH.add(quad);
        else QUAD.add(quad);
    }

    public void resetCircleSmoothing() {
        smoothY = 0;
        smoothY2 = 0;
    }

    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
    }

    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
    }

    public record GradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4) {
    }
}