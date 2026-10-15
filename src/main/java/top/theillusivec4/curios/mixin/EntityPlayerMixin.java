package top.theillusivec4.curios.mixin;

import net.minecraft.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.CuriosInventory;

@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin {
    @Inject(method = "clonePlayer", at = @At("TAIL"))
    private void curios$copyCurios(EntityPlayer oldPlayer, boolean cloneAll, CallbackInfo info) {
        CuriosInventory.get(oldPlayer).copyForClone((EntityPlayer) (Object) this, cloneAll);
    }
}
