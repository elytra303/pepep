package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SpectatorHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.IMinecraft;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.DrawEvent;
import rich.modules.impl.render.Hud;
import rich.screens.clickgui.ClickGui;
import rich.util.render.Render2D;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements IMinecraft {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (client.currentScreen instanceof ClickGui) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRenderCustomHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!this.client.options.hudHidden) {
            context.createNewRootLayer();

            Render2D.beginOverlay();

            context.getMatrices().pushMatrix();

            DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickProgress(false));
            EventManager.callEvent(event);

            context.getMatrices().popMatrix();

            context.getMatrices().pushMatrix();

            Hud hud = Hud.getInstance();
            if (hud != null && hud.isState()) {
                Initialization.getInstance().getManager().getDraggableRepository().draggable().forEach(draggable -> {
                    try {
                        if (draggable.canDraw(hud, draggable)) {
                            draggable.startAnimation();
                        } else {
                            draggable.stopAnimation();
                        }

                        float scale = draggable.getScaleAnimation().getOutput().floatValue();
                        if (!draggable.isCloseAnimationFinished()) {
                            draggable.validPosition();
                            draggable.drawDraggable(context, (int) (scale * 255));
                        }
                    } catch (Exception ignored) {
                    }
                });
            }

            context.getMatrices().popMatrix();

            Render2D.endOverlay();
        }
    }
}