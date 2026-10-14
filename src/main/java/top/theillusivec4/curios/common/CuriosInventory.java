package top.theillusivec4.curios.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import top.theillusivec4.curios.CuriosNetwork;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.BaseAttributeMap;
import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Entity-owned Curios inventory with NBT persistence; GUI and network sync are subsequent slices. */
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
        slot(identifier).stacks.setStackInSlot(index, stack);
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
        slot(identifier).stacks.setStackInSlot(index, null);
        if (curio != null) curio.onUnequip(identifier, index, owner);
        return old;
    }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        ensureSlotTypes();
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ICurio curio = getCurio(stacks.getStackInSlot(i));
                if (curio != null) modifiers.putAll(curio.getAttributeModifiers(entry.getKey()));
            }
        }
        return modifiers;
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

    public NBTTagCompound writeToNBT() {
        ensureSlotTypes();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList savedSlots = new NBTTagList();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            Slots slot = entry.getValue();
            for (int i = 0; i < slot.stacks.getSlots(); i++) {
                ItemStack stack = slot.stacks.getStackInSlot(i);
                ItemStack cosmetic = slot.cosmetics.getStackInSlot(i);
                if (stack == null && cosmetic == null) continue;
                NBTTagCompound savedSlot = new NBTTagCompound();
                savedSlot.setString("Identifier", entry.getKey());
                savedSlot.setInteger("Index", i);
                if (stack != null) savedSlot.setTag("Stack", stack.writeToNBT(new NBTTagCompound()));
                if (cosmetic != null) savedSlot.setTag("Cosmetic", cosmetic.writeToNBT(new NBTTagCompound()));
                savedSlots.appendTag(savedSlot);
            }
        }
        data.setTag("Slots", savedSlots);
        return data;
    }

    public void readFromNBT(NBTTagCompound data) {
        ensureSlotTypes();
        Multimap<Attribute, AttributeModifier> previous = getAttributeModifiers();
        for (Slots slot : slots.values()) {
            slot.stacks.clear();
            slot.cosmetics.clear();
        }
        NBTTagList savedSlots = data.getTagList("Slots");
        for (int i = 0; i < savedSlots.tagCount(); i++) {
            if (!(savedSlots.tagAt(i) instanceof NBTTagCompound)) continue;
            NBTTagCompound savedSlot = (NBTTagCompound) savedSlots.tagAt(i);
            Slots slot = slots.get(savedSlot.getString("Identifier"));
            int index = savedSlot.getInteger("Index");
            if (slot == null || !slot.stacks.isValidSlot(index)) continue;
            if (savedSlot.hasKey("Stack")) {
                slot.stacks.setRaw(index, ItemStack.loadItemStackFromNBT(savedSlot.getCompoundTag("Stack")));
            }
            if (savedSlot.hasKey("Cosmetic")) {
                slot.cosmetics.setRaw(index, ItemStack.loadItemStackFromNBT(savedSlot.getCompoundTag("Cosmetic")));
            }
        }
        applyAttributeChanges(previous);
    }

    private void applyAttributeChanges(Multimap<Attribute, AttributeModifier> previous) {
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return;
        BaseAttributeMap attributes = owner.getAttributeMap();
        Multimap<Attribute, AttributeModifier> current = getAttributeModifiers();
        attributes.removeAttributeModifiers(previous);
        attributes.removeAttributeModifiers(current);
        attributes.applyAttributeModifiers(current);
    }

    private ICurio getCurio(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        return stack.getItem() instanceof ICurioItem ? ((ICurioItem) stack.getItem()).getCurio(stack) : null;
    }
    private Slots slot(String id) { Slots s = slots.get(id); if (s == null) throw new IllegalArgumentException("Unknown Curios slot: " + id); return s; }

    private final class Slots implements ICurioStacksHandler {
        private final String id; private final StackHandler stacks, cosmetics;
        private Slots(String id, int size) { this.id = id; this.stacks = new StackHandler(size, true); this.cosmetics = new StackHandler(size, false); }
        @Override public String getIdentifier() { return id; }
        @Override public IDynamicStackHandler getStacks() { return stacks; }
        @Override public IDynamicStackHandler getCosmeticStacks() { return cosmetics; }
        private void resize(int size) { stacks.resize(size); cosmetics.resize(size); }
    }
    private final class StackHandler implements IDynamicStackHandler {
        private ItemStack[] values;
        private final boolean functional;
        private StackHandler(int size, boolean functional) { values = new ItemStack[size]; this.functional = functional; }
        @Override public int getSlots() { return values.length; }
        @Override public ItemStack getStackInSlot(int slot) { return values[slot]; }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            Multimap<Attribute, AttributeModifier> previous = functional ? CuriosInventory.this.getAttributeModifiers() : null;
            setRaw(slot, stack);
            if (functional) {
                CuriosInventory.this.applyAttributeChanges(previous);
            }
            CuriosNetwork.sync(entity.get());
        }
        private void setRaw(int slot, ItemStack stack) { values[slot] = stack; }
        private void clear() { java.util.Arrays.fill(values, null); }
        private boolean isValidSlot(int slot) { return slot >= 0 && slot < values.length; }
        private void resize(int size) { if (size > values.length) values = java.util.Arrays.copyOf(values, size); }
    }
}
