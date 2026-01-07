package rich.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.IMinecraft;
import rich.events.api.EventManager;
import rich.events.impl.Event3D;
import rich.modules.impl.render.NoRender;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements IMinecraft {

    @Inject(method = "renderBlockDamage", at = @At("TAIL"))
    private void onRenderBlockDamage(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, WorldRenderState renderStates, CallbackInfo ci) {
        if (mc.player != null && mc.world != null) {
            Event3D event = new Event3D(matrices, immediate);
            EventManager.callEvent(event);
        }
    }

    @Inject(method = "hasBlindnessOrDarkness", at = @At("HEAD"), cancellable = true)
    private void onHasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.isState()) return;

        Entity entity = camera.getFocusedEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        boolean hasBlindness = livingEntity.hasStatusEffect(StatusEffects.BLINDNESS);
        boolean hasDarkness = livingEntity.hasStatusEffect(StatusEffects.DARKNESS);

        if (noRender.modeSetting.isSelected("Bad Effects") && hasBlindness && !hasDarkness) {
            cir.setReturnValue(false);
        }

        if (noRender.modeSetting.isSelected("Darkness") && hasDarkness && !hasBlindness) {
            cir.setReturnValue(false);
        }

        if (noRender.modeSetting.isSelected("Bad Effects") && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(false);
        }
    }
}