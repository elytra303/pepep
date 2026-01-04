package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.FovEvent;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.target.RaycastAngle;
import rich.modules.impl.player.NoEntityTrace;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

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

}