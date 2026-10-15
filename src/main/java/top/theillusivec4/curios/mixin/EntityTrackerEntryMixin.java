package top.theillusivec4.curios.mixin;

import net.minecraft.Entity;
import net.minecraft.EntityTrackerEntry;
import net.minecraft.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.CuriosNetwork;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
    @Shadow public Entity myEntity;

    @Inject(method = "tryStartWachingThis", at = @At("TAIL"))
    private void curios$syncPlayerToNewObserver(ServerPlayer observer, CallbackInfo info) {
        if (myEntity instanceof ServerPlayer) {
            CuriosNetwork.syncTo((ServerPlayer) myEntity, observer);
        }
    }
}
