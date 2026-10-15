package top.theillusivec4.curios.mixin;

import net.minecraft.EnchantmentHelper;
import net.minecraft.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getFortuneModifier(Lnet/minecraft/EntityLivingBase;)I", at = @At("RETURN"), cancellable = true)
    private static void curios$addFortuneBonus(EntityLivingBase entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValueI() + CuriosApi.getFortuneBonus(entity));
    }

    @Inject(method = "getLootingModifier(Lnet/minecraft/EntityLivingBase;)I", at = @At("RETURN"), cancellable = true)
    private static void curios$addLootingBonus(EntityLivingBase entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValueI() + CuriosApi.getLootingBonus(entity));
    }
}
