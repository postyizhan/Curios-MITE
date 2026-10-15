package top.theillusivec4.curios.mixin;

import net.minecraft.EntityLivingBase;
import net.minecraft.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.client.CuriosRenderHooks;

@Mixin(RenderPlayer.class)
public abstract class RenderPlayerMixin {
    @Inject(method = "renderEquippedItems", at = @At("TAIL"))
    private void curios$renderCurios(EntityLivingBase entity, float partialTick, CallbackInfo info) {
        CuriosRenderHooks.render(entity, partialTick);
    }
}
