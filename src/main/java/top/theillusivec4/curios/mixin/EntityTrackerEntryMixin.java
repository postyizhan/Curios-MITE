package top.theillusivec4.curios.mixin;

import net.minecraft.Entity;
import net.minecraft.EntityTrackerEntry;
import net.minecraft.EntityLivingBase;
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

    @Inject(method = "tryStartWachingThis", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/NetServerHandler;sendPacketToPlayer(Lnet/minecraft/Packet;)V",
        ordinal = 0,
        shift = At.Shift.AFTER
    ))
    private void curios$syncLivingEntityToNewObserver(ServerPlayer observer, CallbackInfo info) {
        if (myEntity instanceof EntityLivingBase) {
            CuriosNetwork.syncTo((EntityLivingBase) myEntity, observer);
        }
    }
}
