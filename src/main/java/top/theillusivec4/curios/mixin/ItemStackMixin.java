package top.theillusivec4.curios.mixin;

import net.minecraft.DamageSource;
import net.minecraft.EntityLivingBase;
import net.minecraft.ItemDamageResult;
import net.minecraft.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.common.CuriosInventory;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(
        method = "tryDamageItem(Lnet/minecraft/DamageSource;ILnet/minecraft/EntityLivingBase;)Lnet/minecraft/ItemDamageResult;",
        at = @At("RETURN")
    )
    private void curios$removeBrokenCurio(DamageSource source, int amount, EntityLivingBase entity,
                                         CallbackInfoReturnable<ItemDamageResult> cir) {
        ItemDamageResult result = cir.getReturnValue();
        if (result != null && result.itemWasDestroyed() && entity != null) {
            CuriosInventory.get(entity).onBrokenCurio((ItemStack) (Object) this);
        }
    }
}
