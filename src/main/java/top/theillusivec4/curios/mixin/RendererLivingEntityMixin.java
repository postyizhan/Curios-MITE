package top.theillusivec4.curios.mixin;

import net.minecraft.EntityLivingBase;
import net.minecraft.EntityPlayer;
import net.minecraft.ModelBiped;
import net.minecraft.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.client.CuriosRenderHooks;

@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {
    @Shadow protected net.minecraft.ModelBase mainModel;

    @Inject(method = "renderEquippedItems", at = @At("TAIL"))
    private void curios$renderCurios(EntityLivingBase entity, float partialTick, CallbackInfo info) {
        if (entity instanceof EntityPlayer) return;
        CuriosRenderHooks.render(entity, mainModel instanceof ModelBiped ? (ModelBiped) mainModel : null, partialTick);
    }
}
