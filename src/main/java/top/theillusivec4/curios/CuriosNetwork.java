package top.theillusivec4.curios;

import com.google.common.eventbus.Subscribe;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.CompressedStreamTools;
import net.minecraft.Entity;
import net.minecraft.EntityLivingBase;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet250CustomPayload;
import net.minecraft.ServerPlayer;
import net.minecraft.ItemStack;
import net.minecraft.WorldServer;
import net.xiaoyu233.fml.reload.event.PlayerLoggedInEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.CuriosInventory;

/** Server-authoritative synchronization for Curios inventories. */
public final class CuriosNetwork {
    public static final String CHANNEL = "Curios|Sync";
    private static final Map<Integer, NBTTagCompound> PENDING = new HashMap<Integer, NBTTagCompound>();
    private static final Map<Integer, List<NBTTagCompound>> PENDING_CURIO_DATA =
        new HashMap<Integer, List<NBTTagCompound>>();
    private static final Map<Integer, List<NBTTagCompound>> PENDING_BREAK_EVENTS =
        new HashMap<Integer, List<NBTTagCompound>>();

    public static synchronized void clearPending() {
        PENDING.clear();
        PENDING_CURIO_DATA.clear();
        PENDING_BREAK_EVENTS.clear();
    }

    @Subscribe
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        sync(event.getPlayer());
    }

    public static void sync(EntityLivingBase entity) {
        if (entity == null || entity.worldObj.isRemote) return;
        Packet250CustomPayload packet = createSnapshot(entity);
        if (packet == null) return;
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            if (player.playerNetServerHandler != null) player.playerNetServerHandler.sendPacketToPlayer(packet);
            WorldServer world = player.getServerForPlayer();
            if (world != null) world.sendPacketToAllPlayersTrackingEntity(player, packet);
        } else if (entity.worldObj instanceof WorldServer) {
            ((WorldServer) entity.worldObj).sendPacketToAllPlayersTrackingEntity(entity, packet);
        }
    }

    public static void syncTo(EntityLivingBase entity, ServerPlayer receiver) {
        if (entity == null || receiver == null || entity.worldObj.isRemote
            || receiver.playerNetServerHandler == null) return;
        Packet250CustomPayload packet = createSnapshot(entity);
        if (packet != null) receiver.playerNetServerHandler.sendPacketToPlayer(packet);
    }

    public static void sendBreakEvent(EntityLivingBase entity, String identifier, int index, ItemStack stack) {
        if (entity == null || entity.worldObj.isRemote || stack == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Entity", entity.entityId);
        payload.setBoolean("Break", true);
        payload.setString("Identifier", identifier);
        payload.setInteger("Index", index);
        payload.setTag("Stack", stack.writeToNBT(new NBTTagCompound()));
        Packet250CustomPayload packet;
        try {
            packet = new Packet250CustomPayload(CHANNEL, CompressedStreamTools.compress(payload));
        } catch (Exception exception) {
            Curios.LOGGER.error("Failed to synchronize Curios break event for entity {}", entity.entityId, exception);
            return;
        }
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            if (player.playerNetServerHandler != null) player.playerNetServerHandler.sendPacketToPlayer(packet);
        }
        if (entity instanceof ServerPlayer) {
            WorldServer world = ((ServerPlayer) entity).getServerForPlayer();
            if (world != null) world.sendPacketToAllPlayersTrackingEntity(entity, packet);
        } else if (entity.worldObj instanceof WorldServer) {
            ((WorldServer) entity.worldObj).sendPacketToAllPlayersTrackingEntity(entity, packet);
        }
    }

    public static void syncCurioData(EntityLivingBase entity, String identifier, int index,
                                     boolean cosmetic, NBTTagCompound data) {
        if (entity == null || entity.worldObj.isRemote || data == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Entity", entity.entityId);
        payload.setBoolean("CurioData", true);
        payload.setString("Identifier", identifier);
        payload.setInteger("Index", index);
        payload.setBoolean("Cosmetic", cosmetic);
        payload.setTag("Data", data);
        Packet250CustomPayload packet;
        try {
            packet = new Packet250CustomPayload(CHANNEL, CompressedStreamTools.compress(payload));
        } catch (Exception exception) {
            Curios.LOGGER.error("Failed to synchronize Curios item data for entity {}", entity.entityId, exception);
            return;
        }
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            if (player.playerNetServerHandler != null) player.playerNetServerHandler.sendPacketToPlayer(packet);
        }
        if (entity instanceof ServerPlayer) {
            WorldServer world = ((ServerPlayer) entity).getServerForPlayer();
            if (world != null) world.sendPacketToAllPlayersTrackingEntity(entity, packet);
        } else if (entity.worldObj instanceof WorldServer) {
            ((WorldServer) entity.worldObj).sendPacketToAllPlayersTrackingEntity(entity, packet);
        }
    }

    private static Packet250CustomPayload createSnapshot(EntityLivingBase entity) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Entity", entity.entityId);
        payload.setTag("Inventory", CuriosInventory.get(entity).writeToNBT());
        try {
            return new Packet250CustomPayload(CHANNEL, CompressedStreamTools.compress(payload));
        } catch (Exception exception) {
            Curios.LOGGER.error("Failed to synchronize Curios inventory for entity {}", entity.entityId, exception);
            return null;
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

    public static synchronized void receive(NBTTagCompound payload, Entity entity) {
        if (payload == null || !payload.hasKey("Entity")) return;
        int entityId = payload.getInteger("Entity");
        // Packet handlers may run off the client tick thread; apply all entity state in applyPending.
        if (payload.getBoolean("Break")) {
            if (!payload.hasKey("Stack")) return;
            PENDING_BREAK_EVENTS.computeIfAbsent(entityId, ignored -> new ArrayList<NBTTagCompound>())
                .add((NBTTagCompound) payload.copy());
            return;
        }
        if (payload.getBoolean("CurioData")) {
            PENDING_CURIO_DATA.computeIfAbsent(entityId, ignored -> new ArrayList<NBTTagCompound>())
                .add((NBTTagCompound) payload.copy());
            return;
        }
        if (!payload.hasKey("Inventory")) return;
        PENDING.put(entityId, (NBTTagCompound) payload.getCompoundTag("Inventory").copy());
    }

    public static synchronized void applyPending(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) return;
        NBTTagCompound inventory = PENDING.remove(entity.entityId);
        if (inventory != null) CuriosInventory.get((EntityLivingBase) entity).readFromNBT(inventory);
        List<NBTTagCompound> curioData = PENDING_CURIO_DATA.remove(entity.entityId);
        if (curioData != null) {
            for (NBTTagCompound payload : curioData) applyCurioData(payload, (EntityLivingBase) entity);
        }
        List<NBTTagCompound> breakEvents = PENDING_BREAK_EVENTS.remove(entity.entityId);
        if (breakEvents != null) {
            for (NBTTagCompound payload : breakEvents) applyBreakEvent(payload, (EntityLivingBase) entity);
        }
    }

    private static void applyBreakEvent(NBTTagCompound payload, EntityLivingBase entity) {
        ItemStack broken = ItemStack.loadItemStackFromNBT(payload.getCompoundTag("Stack"));
        top.theillusivec4.curios.api.CuriosApi.getCurio(broken)
            .ifPresent(curio -> curio.curioBreak(broken, entity));
    }

    private static void applyCurioData(NBTTagCompound payload, EntityLivingBase entity) {
        if (!payload.hasKey("Identifier") || !payload.hasKey("Index")
            || !payload.hasKey("Cosmetic") || !payload.hasKey("Data")) return;
        String identifier = payload.getString("Identifier");
        int index = payload.getInteger("Index");
        ICurioStacksHandler slot = CuriosInventory.get(entity).getStacksHandler(identifier).orElse(null);
        if (slot == null || index < 0 || index >= slot.getSlots()) return;
        boolean cosmetic = payload.getBoolean("Cosmetic");
        ItemStack stack = (cosmetic ? slot.getCosmeticStacks() : slot.getStacks()).getStackInSlot(index);
        if (stack == null) return;
        top.theillusivec4.curios.api.CuriosApi.getCurio(stack)
            .ifPresent(curio -> curio.readSyncData(payload.getCompoundTag("Data")));
    }

    public static void handleServerAction(byte[] data, ServerPlayer player) {
        NBTTagCompound payload = decode(data);
        if (payload == null || player == null || player.worldObj.isRemote
            || !payload.hasKey("Source") || !payload.hasKey("Identifier") || !payload.hasKey("Index")) return;
        String identifier = payload.getString("Identifier");
        int index = payload.getInteger("Index");
        int source = payload.getInteger("Source");
        boolean cosmetic = payload.hasKey("Cosmetic") && payload.getBoolean("Cosmetic");
        CuriosInventory inventory = CuriosInventory.get(player);
        java.util.Optional<ICurioStacksHandler> slotHandler = inventory.getStacksHandler(identifier);
        if (!slotHandler.isPresent() || index < 0 || index >= slotHandler.get().getStacks().getSlots()) return;
        if (source == -1) {
            net.minecraft.ItemStack removed;
            if (cosmetic) {
                removed = slotHandler.get().getCosmeticStacks().getStackInSlot(index);
                if (!inventory.setCosmetic(identifier, index, null)) return;
            } else {
                removed = inventory.unequip(identifier, index);
            }
            if (removed != null) {
                player.inventory.addItemStackToInventoryOrDropIt(removed);
                player.inventoryContainer.detectAndSendChanges();
            }
            return;
        } else if (source < 0) return;
        if (source >= player.inventory.mainInventory.length) return;
        net.minecraft.ItemStack original = player.inventory.mainInventory[source];
        if (original == null || original.stackSize <= 0) return;
        net.minecraft.ItemStack equipped = original.copy();
        equipped.stackSize = 1;
        if (cosmetic) {
            if (!slotHandler.get().hasCosmetic()) return;
            net.minecraft.ItemStack previous = slotHandler.get().getCosmeticStacks().getStackInSlot(index);
            if (!inventory.setCosmetic(identifier, index, equipped)) return;
            if (original.stackSize == 1) player.inventory.setInventorySlotContents(source, null);
            else {
                original.stackSize--;
                player.inventory.setInventorySlotContents(source, original);
            }
            if (previous != null) player.inventory.addItemStackToInventoryOrDropIt(previous);
            player.inventoryContainer.detectAndSendChanges();
            return;
        }
        if (!inventory.equip(identifier, index, equipped)) return;
        if (original.stackSize == 1) player.inventory.setInventorySlotContents(source, null);
        else {
            original.stackSize--;
            player.inventory.setInventorySlotContents(source, original);
        }
        player.inventoryContainer.detectAndSendChanges();
    }
}
