package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.Initialization;
import rich.events.api.EventManager;
import rich.events.impl.SetScreenEvent;
import rich.util.window.WindowStyle;

import static rich.IMinecraft.mc;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        new Initialization().init();
    }

    @Inject(method = "setScreen", at = @At(value = "HEAD"), cancellable = true)
    public void setScreenHook(Screen screen, CallbackInfo ci) {
        SetScreenEvent event = new SetScreenEvent(screen);
        EventManager.callEvent(event);

        Initialization instance = Initialization.getInstance();
        if (instance != null && instance.getManager() != null && instance.getManager().getDraggableRepository() != null) {
            instance.getManager().getDraggableRepository().draggable().forEach(drag -> drag.setScreen(event));
        }

        Screen eventScreen = event.getScreen();
        if (screen != eventScreen) {
            mc.setScreen(eventScreen);
            ci.cancel();
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void applyDarkMode(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        WindowStyle.setDarkMode(client.getWindow().getHandle());
    }
}