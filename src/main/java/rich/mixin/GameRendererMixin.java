package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.FovEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.player.NoEntityTrace;
import rich.modules.impl.render.NoRender;
import rich.util.render.render3D.Render3D;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private Camera camera;

    @Unique
    private final MatrixStack matrices = new MatrixStack();

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Shadow
    public abstract float getFov(Camera camera, float tickDelta, boolean changingFov);

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        if (Initialization.getInstance() != null && Initialization.getInstance().getManager() != null && Initialization.getInstance().getManager().getRenderCore() != null) {
            Initialization.getInstance().getManager().getRenderCore().close();
        }
    }

    @ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        FovEvent event = new FovEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) return event.getFov();
        return original;
    }

    @Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
    private void updateCrosshairTargetHook(float tickProgress, CallbackInfo ci) {
        NoEntityTrace noEntityTrace = NoEntityTrace.getInstance();
        if (noEntityTrace != null && noEntityTrace.shouldIgnoreEntityTrace()) {
            Entity entity = this.client.getCameraEntity();
            if (entity != null && this.client.world != null && this.client.player != null) {
                double range = Math.max(
                        this.client.player.getBlockInteractionRange(),
                        this.client.player.getEntityInteractionRange()
                );
                this.client.crosshairTarget = entity.raycast(range, tickProgress, false);
                this.client.targetedEntity = null;
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f view, @Local(ordinal = 0) float tickDelta, @Local MatrixStack matrixStack) {
        if (client.world == null || client.player == null) return;

        MatrixStack worldSpaceStack = new MatrixStack();
        worldSpaceStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        worldSpaceStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        Render3D.lastProjMat.set(client.gameRenderer.getBasicProjectionMatrix(getFov(camera, tickDelta, true)));
        Render3D.lastModMat.set(RenderSystem.getModelViewMatrix());
        Render3D.lastWorldSpaceMatrix.set(worldSpaceStack.peek().getPositionMatrix());

        Render3D.setLastWorldSpaceEntry(matrixStack.peek());
        Render3D.setLastTickDelta(tickDelta);
        Render3D.setLastCameraPos(camera.getCameraPos());
        Render3D.setLastCameraRotation(new Quaternionf(camera.getRotation()));

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix().mul(view);

        matrices.push();
        tiltViewWhenHurt(matrices, camera.getLastTickProgress());
        if (client.options.getBobView().getValue()) {
            bobView(matrices, camera.getLastTickProgress());
        }
        modelViewStack.mul(matrices.peek().getPositionMatrix().invert(new Matrix4f()));
        matrices.pop();

        WorldRenderEvent event = new WorldRenderEvent(matrixStack, tickDelta);
        EventManager.callEvent(event);
        Render3D.onWorldRender(event);

        modelViewStack.popMatrix();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Damage")) {
            ci.cancel();
        }
    }
}