package top.theillusivec4.curios;

import com.google.common.eventbus.Subscribe;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.CompressedStreamTools;
import net.minecraft.Entity;
import net.minecraft.EntityLivingBase;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet250CustomPayload;
import net.minecraft.ServerPlayer;
import net.xiaoyu233.fml.reload.event.PlayerLoggedInEvent;
import top.theillusivec4.curios.common.CuriosInventory;

/** Server-authoritative synchronization for the owning player's Curios inventory. */
public final class CuriosNetwork {
    public static final String CHANNEL = "Curios|Sync";
    private static final Map<Integer, NBTTagCompound> PENDING = new HashMap<Integer, NBTTagCompound>();

    @Subscribe
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        sync(event.getPlayer());
    }

    public static void sync(EntityLivingBase entity) {
        if (!(entity instanceof ServerPlayer) || entity.worldObj.isRemote) return;
        ServerPlayer player = (ServerPlayer) entity;
        if (player.playerNetServerHandler == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Entity", player.entityId);
        payload.setTag("Inventory", CuriosInventory.get(player).writeToNBT());
        try {
            player.playerNetServerHandler.sendPacketToPlayer(
                new Packet250CustomPayload(CHANNEL, CompressedStreamTools.compress(payload)));
        } catch (Exception exception) {
            Curios.LOGGER.error("Failed to synchronize Curios inventory for entity {}", player.entityId, exception);
        }
    }

    public static NBTTagCompound decode(byte[] data) {
        try {
            return CompressedStreamTools.decompress(data);
        } catch (Exception exception) {
            Curios.LOGGER.error("Failed to read Curios inventory synchronization payload", exception);
            return null;
        }
    }

    public static void receive(NBTTagCompound payload, Entity entity) {
        if (payload == null || !payload.hasKey("Entity") || !payload.hasKey("Inventory")) return;
        int entityId = payload.getInteger("Entity");
        PENDING.remove(entityId);
        NBTTagCompound inventory = payload.getCompoundTag("Inventory");
        EntityLivingBase living = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        if (living == null) PENDING.put(entityId, inventory);
        else CuriosInventory.get(living).readFromNBT(inventory);
    }

    public static void applyPending(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) return;
        NBTTagCompound inventory = PENDING.remove(entity.entityId);
        if (inventory != null) CuriosInventory.get((EntityLivingBase) entity).readFromNBT(inventory);
    }
}
