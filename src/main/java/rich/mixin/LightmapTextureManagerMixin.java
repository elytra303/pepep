package rich.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rich.Initialization;
import rich.modules.impl.render.FullBright;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float leet$getValue(Double instance) {
        if (Initialization.getInstance().getManager().getModuleProvider().get(FullBright.class).isState()) {
            return 200F;
        }
        return instance.floatValue();
    }

}
