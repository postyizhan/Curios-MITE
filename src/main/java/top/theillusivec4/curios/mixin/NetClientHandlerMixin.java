package top.theillusivec4.curios.mixin;

import net.minecraft.NetClientHandler;
import net.minecraft.Entity;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet250CustomPayload;
import net.minecraft.Packet1Login;
import net.minecraft.Packet9Respawn;
import net.minecraft.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.CuriosNetwork;

@Mixin(NetClientHandler.class)
public abstract class NetClientHandlerMixin {
    @Shadow private WorldClient worldClient;

    @Inject(method = "handleLogin(Lnet/minecraft/Packet1Login;)V", at = @At("HEAD"))
    private void curios$clearPendingOnLogin(Packet1Login packet, CallbackInfo info) {
        CuriosNetwork.clearPending();
    }

    @Inject(method = "handleRespawn(Lnet/minecraft/Packet9Respawn;)V", at = @At("HEAD"))
    private void curios$clearPendingOnRespawn(Packet9Respawn packet, CallbackInfo info) {
        CuriosNetwork.clearPending();
    }

    @Inject(method = "cleanup()V", at = @At("HEAD"))
    private void curios$clearPendingOnDisconnect(CallbackInfo info) {
        CuriosNetwork.clearPending();
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void curios$handlePayload(Packet250CustomPayload packet, CallbackInfo info) {
        if (!CuriosNetwork.CHANNEL.equals(packet.channel)) return;
        NBTTagCompound payload = CuriosNetwork.decode(packet.data);
        Entity entity = payload == null || worldClient == null
            ? null : worldClient.getEntityByID(payload.getInteger("Entity"));
        CuriosNetwork.receive(payload, entity);
        info.cancel();
    }
}
