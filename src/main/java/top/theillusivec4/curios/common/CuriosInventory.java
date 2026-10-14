package top.theillusivec4.curios.common;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** In-memory first implementation; persistence and GUI are subsequent migration slices. */
public final class CuriosInventory implements ICuriosItemHandler {
    private static final Map<EntityLivingBase, CuriosInventory> INSTANCES = new java.util.WeakHashMap<EntityLivingBase, CuriosInventory>();
    private final Map<String, Slots> slots = new LinkedHashMap<String, Slots>();
    private final WeakReference<EntityLivingBase> entity;

    public static synchronized CuriosInventory get(EntityLivingBase entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        CuriosInventory inventory = INSTANCES.get(entity);
        if (inventory == null) { inventory = new CuriosInventory(entity); INSTANCES.put(entity, inventory); }
        return inventory;
    }

    private CuriosInventory(EntityLivingBase entity) { this.entity = new WeakReference<EntityLivingBase>(entity); syncSlotTypes(); }

    private synchronized void syncSlotTypes() {
        for (ISlotType type : CuriosSlotRegistry.getAll().values()) {
            Slots current = slots.get(type.getIdentifier());
            if (current == null) slots.put(type.getIdentifier(), new Slots(type.getIdentifier(), type.getSize()));
            else current.resize(type.getSize());
        }
    }

    private void ensureSlotTypes() { syncSlotTypes(); }

    @Override public Optional<ICurioStacksHandler> getStacksHandler(String identifier) {
        ensureSlotTypes();
        return Optional.<ICurioStacksHandler>ofNullable(slots.get(identifier));
    }
    @Override public ItemStack getEquippedStack(String identifier, int index) {
        ensureSlotTypes();
        StackHandler stacks = slot(identifier).stacks;
        return stacks.isValidSlot(index) ? stacks.getStackInSlot(index) : null;
    }
    @Override public boolean equip(String identifier, int index, ItemStack stack) {
        ensureSlotTypes();
        if (stack == null) return false;
        StackHandler stacks = slot(identifier).stacks;
        if (!stacks.isValidSlot(index) || stacks.getStackInSlot(index) != null) return false;
        EntityLivingBase owner = entity.get();
        if (owner == null) return false;
        ICurio curio = getCurio(stack);
        if (curio != null && !curio.canEquip(identifier, owner)) return false;
        slot(identifier).getStacks().setStackInSlot(index, stack);
        if (curio != null) curio.onEquip(identifier, index, owner);
        return true;
    }
    @Override public ItemStack unequip(String identifier, int index) {
        ensureSlotTypes();
        StackHandler stacks = slot(identifier).stacks;
        if (!stacks.isValidSlot(index)) return null;
        ItemStack old = stacks.getStackInSlot(index);
        EntityLivingBase owner = entity.get();
        if (owner == null) return null;
        ICurio curio = getCurio(old);
        if (curio != null && !curio.canUnequip(identifier, owner)) return null;
        slot(identifier).getStacks().setStackInSlot(index, null);
        if (curio != null) curio.onUnequip(identifier, index, owner);
        return old;
    }
    @Override public void tick() {
        ensureSlotTypes();
        EntityLivingBase owner = entity.get();
        if (owner == null) return;
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ICurio curio = getCurio(stacks.getStackInSlot(i));
                if (curio != null) curio.curioTick(entry.getKey(), i, owner);
            }
        }
    }
    private ICurio getCurio(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        return stack.getItem() instanceof ICurioItem ? ((ICurioItem) stack.getItem()).getCurio(stack) : null;
    }
    private Slots slot(String id) { Slots s = slots.get(id); if (s == null) throw new IllegalArgumentException("Unknown Curios slot: " + id); return s; }

    private static final class Slots implements ICurioStacksHandler {
        private final String id; private final StackHandler stacks, cosmetics;
        private Slots(String id, int size) { this.id = id; this.stacks = new StackHandler(size); this.cosmetics = new StackHandler(size); }
        @Override public String getIdentifier() { return id; }
        @Override public IDynamicStackHandler getStacks() { return stacks; }
        @Override public IDynamicStackHandler getCosmeticStacks() { return cosmetics; }
        private void resize(int size) { stacks.resize(size); cosmetics.resize(size); }
    }
    private static final class StackHandler implements IDynamicStackHandler {
        private ItemStack[] values;
        private StackHandler(int size) { values = new ItemStack[size]; }
        @Override public int getSlots() { return values.length; }
        @Override public ItemStack getStackInSlot(int slot) { return values[slot]; }
        @Override public void setStackInSlot(int slot, ItemStack stack) { values[slot] = stack; }
        private boolean isValidSlot(int slot) { return slot >= 0 && slot < values.length; }
        private void resize(int size) { if (size > values.length) values = java.util.Arrays.copyOf(values, size); }
    }
}
