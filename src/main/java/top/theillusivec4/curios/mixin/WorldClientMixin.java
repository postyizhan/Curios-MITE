package top.theillusivec4.curios.mixin;

import net.minecraft.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.CuriosNetwork;

@Mixin(WorldClient.class)
public abstract class WorldClientMixin {
    @Inject(method = "removeEntityFromWorld(I)Lnet/minecraft/Entity;", at = @At("HEAD"))
    private void curios$clearPendingForRemovedEntity(int entityId, CallbackInfoReturnable<net.minecraft.Entity> cir) {
        CuriosNetwork.clearPending(entityId);
    }
}
