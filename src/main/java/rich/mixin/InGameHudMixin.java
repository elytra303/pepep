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

import java.util.Map;
import java.util.function.Supplier;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements IMinecraft {
    @Shadow
    private int overlayRemaining;
    @Shadow
    @Nullable
    private Text overlayMessage;
    @Shadow
    private int heldItemTooltipFade;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private SpectatorHud spectatorHud;
    @Shadow
    private Pair<InGameHud.BarType, Bar> currentBar;
    @Shadow
    @Final
    private Map<InGameHud.BarType, Supplier<Bar>> bars;
    @Shadow
    @Final
    private DebugHud debugHud;

    @Shadow
    public abstract void render(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderMiscOverlays(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderCrosshair(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderHotbar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderStatusBars(DrawContext context);

    @Shadow
    protected abstract void renderMountHealth(DrawContext context);

    @Shadow
    protected abstract InGameHud.BarType getCurrentBarType();

    @Shadow
    protected abstract void renderHeldItemTooltip(DrawContext context);

    @Shadow
    protected abstract void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderBossBarHud(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderSleepOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderDemoTimer(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderChat(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderPlayerList(DrawContext context, RenderTickCounter tickCounter);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRenderCustomHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ci.cancel();

        boolean isClickGuiOpen = this.client.currentScreen instanceof ClickGui;
        boolean debugHudVisible = this.debugHud.shouldShowDebugHud();

        if (!this.client.options.hudHidden && !debugHudVisible) {
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
        }

        if (!this.client.options.hudHidden) {
            this.renderMiscOverlays(context, tickCounter);

            if (!isClickGuiOpen) {
                this.renderCrosshair(context, tickCounter);
            }

            context.createNewRootLayer();

            if (this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
                this.spectatorHud.renderSpectatorMenu(context);
            } else {
                this.renderHotbar(context, tickCounter);
            }

            if (this.client.interactionManager.hasStatusBars()) {
                this.renderStatusBars(context);
            }

            this.renderMountHealth(context);
            InGameHud.BarType barType = this.getCurrentBarType();
            if (barType != this.currentBar.getKey()) {
                this.currentBar = Pair.of(barType, this.bars.get(barType).get());
            }

            this.currentBar.getValue().renderBar(context, tickCounter);
            if (this.client.interactionManager.hasExperienceBar() && this.client.player.experienceLevel > 0) {
                Bar.drawExperienceLevel(context, this.client.textRenderer, this.client.player.experienceLevel);
            }

            this.currentBar.getValue().renderAddons(context, tickCounter);

            if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                this.renderHeldItemTooltip(context);
            } else if (this.client.player.isSpectator()) {
                this.spectatorHud.render(context);
            }

            this.renderStatusEffectOverlay(context, tickCounter);
            this.renderBossBarHud(context, tickCounter);
        }

        this.renderSleepOverlay(context, tickCounter);

        if (!this.client.options.hudHidden) {
            this.renderDemoTimer(context, tickCounter);
            this.renderScoreboardSidebar(context, tickCounter);
            this.renderOverlayMessage(context, tickCounter);
            this.renderTitleAndSubtitle(context, tickCounter);
            this.renderChat(context, tickCounter);
            this.renderPlayerList(context, tickCounter);
        }

        if (debugHudVisible) {
            context.createNewRootLayer();
            this.debugHud.render(context);
        }
    }
}