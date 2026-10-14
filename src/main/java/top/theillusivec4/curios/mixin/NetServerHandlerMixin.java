package top.theillusivec4.curios.mixin;

import net.minecraft.NetServerHandler;
import net.minecraft.Packet250CustomPayload;
import net.minecraft.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.CuriosNetwork;

@Mixin(NetServerHandler.class)
public abstract class NetServerHandlerMixin {
    @Shadow private ServerPlayer playerEntity;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void curios$handlePayload(Packet250CustomPayload packet, CallbackInfo info) {
        if (!CuriosNetwork.CHANNEL.equals(packet.channel)) return;
        CuriosNetwork.handleServerAction(packet.data, playerEntity);
        info.cancel();
    }
}
