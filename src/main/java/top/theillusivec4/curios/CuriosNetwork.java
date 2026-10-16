package top.theillusivec4.curios;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private static final int MAX_PENDING_ENTITIES = 512;
    private static final int MAX_PENDING_CURIO_DATA_PER_ENTITY = 256;
    private static final int MAX_PENDING_BREAK_EVENTS_PER_ENTITY = 64;
    private static final long PENDING_EXPIRY_NANOS = 60_000_000_000L;
    private static final Map<Integer, NBTTagCompound> PENDING = new LinkedHashMap<Integer, NBTTagCompound>();
    private static final Map<Integer, List<NBTTagCompound>> PENDING_CURIO_DATA =
        new LinkedHashMap<Integer, List<NBTTagCompound>>();
    private static final Map<Integer, List<NBTTagCompound>> PENDING_BREAK_EVENTS =
        new LinkedHashMap<Integer, List<NBTTagCompound>>();
    private static final Map<Integer, Long> PENDING_SINCE = new LinkedHashMap<Integer, Long>();

    public static synchronized void clearPending() {
        PENDING.clear();
        PENDING_CURIO_DATA.clear();
        PENDING_BREAK_EVENTS.clear();
        PENDING_SINCE.clear();
    }

    public static synchronized void clearPending(int entityId) {
        PENDING.remove(entityId);
        PENDING_CURIO_DATA.remove(entityId);
        PENDING_BREAK_EVENTS.remove(entityId);
        PENDING_SINCE.remove(entityId);
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
        expirePending();
        if (payload == null || !payload.hasKey("Entity")) return;
        int entityId = payload.getInteger("Entity");
        // Packet handlers may run off the client tick thread; apply all entity state in applyPending.
        if (payload.getBoolean("Break")) {
            if (!payload.hasKey("Stack")) return;
            List<NBTTagCompound> events = pendingEvents(entityId);
            if (events.size() == MAX_PENDING_BREAK_EVENTS_PER_ENTITY) events.remove(0);
            events.add((NBTTagCompound) payload.copy());
            return;
        }
        if (payload.getBoolean("CurioData")) {
            if (!payload.hasKey("Identifier") || !payload.hasKey("Index")
                || !payload.hasKey("Cosmetic") || !payload.hasKey("Data")) return;
            List<NBTTagCompound> data = pendingCurioData(entityId);
            String identifier = payload.getString("Identifier");
            int index = payload.getInteger("Index");
            boolean cosmetic = payload.getBoolean("Cosmetic");
            for (Iterator<NBTTagCompound> iterator = data.iterator(); iterator.hasNext();) {
                NBTTagCompound previous = iterator.next();
                if (previous.getString("Identifier").equals(identifier)
                    && previous.getInteger("Index") == index
                    && previous.getBoolean("Cosmetic") == cosmetic) {
                    iterator.remove();
                    break;
                }
            }
            if (data.size() == MAX_PENDING_CURIO_DATA_PER_ENTITY) data.remove(0);
            data.add((NBTTagCompound) payload.copy());
            return;
        }
        if (!payload.hasKey("Inventory")) return;
        trackPendingEntity(entityId);
        PENDING.put(entityId, (NBTTagCompound) payload.getCompoundTag("Inventory").copy());
    }

    public static synchronized void applyPending(Entity entity) {
        expirePending();
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
        removePendingTimestampIfEmpty(entity.entityId);
    }

    private static List<NBTTagCompound> pendingCurioData(int entityId) {
        trackPendingEntity(entityId);
        return PENDING_CURIO_DATA.computeIfAbsent(entityId, ignored -> new ArrayList<NBTTagCompound>());
    }

    private static List<NBTTagCompound> pendingEvents(int entityId) {
        trackPendingEntity(entityId);
        return PENDING_BREAK_EVENTS.computeIfAbsent(entityId, ignored -> new ArrayList<NBTTagCompound>());
    }

    private static void trackPendingEntity(int entityId) {
        PENDING_SINCE.putIfAbsent(entityId, System.nanoTime());
        while (PENDING_SINCE.size() > MAX_PENDING_ENTITIES) {
            Integer oldest = PENDING_SINCE.keySet().iterator().next();
            clearPending(oldest);
        }
    }

    private static void expirePending() {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, Long>> iterator = PENDING_SINCE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            if (now - entry.getValue() < PENDING_EXPIRY_NANOS) continue;
            int entityId = entry.getKey();
            iterator.remove();
            PENDING.remove(entityId);
            PENDING_CURIO_DATA.remove(entityId);
            PENDING_BREAK_EVENTS.remove(entityId);
        }
    }

    private static void removePendingTimestampIfEmpty(int entityId) {
        if (!PENDING.containsKey(entityId) && !PENDING_CURIO_DATA.containsKey(entityId)
            && !PENDING_BREAK_EVENTS.containsKey(entityId)) {
            PENDING_SINCE.remove(entityId);
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
        if (payload.getBoolean("Carried")) {
            ItemStack carried = player.inventory.getItemStack();
            if (carried == null || carried.stackSize <= 0) return;
            ItemStack equipped = carried.copy();
            equipped.stackSize = 1;
            if (cosmetic) {
                if (!slotHandler.get().hasCosmetic()) return;
                ItemStack previous = slotHandler.get().getCosmeticStacks().getStackInSlot(index);
                if (!inventory.setCosmetic(identifier, index, equipped)) return;
                consumeCarriedItem(player, carried);
                if (previous != null) player.inventory.addItemStackToInventoryOrDropIt(previous);
            } else {
                ItemStack previous = slotHandler.get().getStacks().getStackInSlot(index);
                if (!inventory.setEquippedStack(identifier, index, equipped)) return;
                consumeCarriedItem(player, carried);
                if (previous != null) player.inventory.addItemStackToInventoryOrDropIt(previous);
            }
            player.inventoryContainer.detectAndSendChanges();
            return;
        }
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
        ItemStack previous = slotHandler.get().getStacks().getStackInSlot(index);
        if (!inventory.setEquippedStack(identifier, index, equipped)) return;
        if (original.stackSize == 1) player.inventory.setInventorySlotContents(source, null);
        else {
            original.stackSize--;
            player.inventory.setInventorySlotContents(source, original);
        }
        if (previous != null) player.inventory.addItemStackToInventoryOrDropIt(previous);
        player.inventoryContainer.detectAndSendChanges();
    }

    private static void consumeCarriedItem(ServerPlayer player, ItemStack carried) {
        if (carried.stackSize <= 1) player.inventory.setItemStack(null);
        else {
            carried.stackSize--;
            player.inventory.setItemStack(carried);
        }
    }
}
