package top.theillusivec4.curios.mixin;

import net.minecraft.EntityPlayer;
import net.minecraft.Item;
import net.minecraft.RaycastCollision;
import net.minecraft.RightClickFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.common.CuriosInventory;

/** Handles auto-equip before an item-specific right-click override can consume the action. */
@Mixin(EntityPlayer.class)
public abstract class EntityPlayerRightClickMixin {
    @Inject(
        method = "onPlayerRightClick(Lnet/minecraft/RaycastCollision;Lnet/minecraft/RightClickFilter;FZ)Lnet/minecraft/RightClickFilter;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/Item;onItemRightClick(Lnet/minecraft/EntityPlayer;FZ)Z"
        ),
        cancellable = true
    )
    private void curios$equipBeforeItemAction(RaycastCollision collision, RightClickFilter filter,
                                               float progress, boolean alternate,
                                               CallbackInfoReturnable<RightClickFilter> cir) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (CuriosInventory.get(player).tryEquipFromRightClick(player, player.getHeldItemStack())) {
            cir.setReturnValue(filter.setExclusive(RightClickFilter.ON_ITEM_RIGHT_CLICK));
        }
    }
}
