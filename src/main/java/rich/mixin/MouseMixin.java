package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.KeyEvent;

@Mixin(Mouse.class)
public class MouseMixin {
    @Final @Shadow private MinecraftClient client;
    @Shadow public double cursorDeltaX, cursorDeltaY;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseButtonHook(long window, MouseInput input, int action, CallbackInfo ci) {
        if (input.button() != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.MOUSE, input.button(), action));
        }
    }
}