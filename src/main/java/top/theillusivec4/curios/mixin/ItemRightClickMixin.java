package top.theillusivec4.curios.mixin;

import net.minecraft.EntityPlayer;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.common.CuriosInventory;

@Mixin(Item.class)
public abstract class ItemRightClickMixin {
    @Inject(method = "onItemRightClick(Lnet/minecraft/EntityPlayer;FZ)Z", at = @At("HEAD"), cancellable = true)
    private void curios$equipFromRightClick(EntityPlayer player, float progress, boolean alternate,
                                            CallbackInfoReturnable<Boolean> cir) {
        ItemStack heldStack = player.getHeldItemStack();
        if (CuriosInventory.get(player).tryEquipFromRightClick(player, heldStack)) {
            cir.setReturnValue(true);
        }
    }
}
