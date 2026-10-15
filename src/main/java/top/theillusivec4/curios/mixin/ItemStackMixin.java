package top.theillusivec4.curios.mixin;

import net.minecraft.ItemDamageResult;
import net.minecraft.ItemStack;
import net.minecraft.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.common.CuriosInventory;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(
        method = "tryDamageItem(Lnet/minecraft/World;IZ)Lnet/minecraft/ItemDamageResult;",
        at = @At("RETURN")
    )
    private void curios$removeBrokenCurio(World world, int amount, boolean applyUnbreaking,
                                         CallbackInfoReturnable<ItemDamageResult> cir) {
        ItemDamageResult result = cir.getReturnValue();
        if (result != null && result.itemWasDestroyed() && world != null && !world.isRemote) {
            CuriosInventory.onStackBroken((ItemStack) (Object) this);
        }
    }
}
