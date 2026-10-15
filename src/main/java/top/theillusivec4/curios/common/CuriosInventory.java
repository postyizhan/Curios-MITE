package top.theillusivec4.curios.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.BaseAttributeMap;
import net.minecraft.EntityLivingBase;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import top.theillusivec4.curios.CuriosNetwork;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Entity-owned Curios inventory with NBT persistence and server-authoritative changes. */
public final class CuriosInventory implements ICuriosItemHandler {
    private static final Map<EntityLivingBase, CuriosInventory> INSTANCES = new java.util.WeakHashMap<EntityLivingBase, CuriosInventory>();
    private final Map<String, Slots> slots = new LinkedHashMap<String, Slots>();
    private final Set<String> lockedSlots = new HashSet<String>();
    private final Set<String> initializedSlots = new HashSet<String>();
    private final Set<String> pendingLocks = new HashSet<String>();
    private final List<UnlockState> pendingUnlocks = new ArrayList<UnlockState>();
    private final WeakReference<EntityLivingBase> entity;

    public static synchronized CuriosInventory get(EntityLivingBase entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        CuriosInventory inventory = INSTANCES.get(entity);
        if (inventory == null) { inventory = new CuriosInventory(entity); INSTANCES.put(entity, inventory); }
        return inventory;
    }

    public static synchronized void onStackBroken(ItemStack broken) {
        if (broken == null) return;
        for (CuriosInventory inventory : new ArrayList<CuriosInventory>(INSTANCES.values())) {
            inventory.onBrokenCurio(broken);
        }
    }

    private CuriosInventory(EntityLivingBase entity) { this.entity = new WeakReference<EntityLivingBase>(entity); syncSlotTypes(); }

    private synchronized void syncSlotTypes() {
        for (ISlotType type : CuriosSlotRegistry.getAll().values()) {
            String identifier = type.getIdentifier();
            if (initializedSlots.add(identifier) && type.isLocked()) lockedSlots.add(identifier);
            if (lockedSlots.contains(identifier)) continue;
            Slots current = slots.get(type.getIdentifier());
            if (current == null) slots.put(identifier, new Slots(identifier, type.getSize(), type.isVisible(), type.hasCosmetic()));
            else {
                current.resize(Math.max(1, Math.min(256, type.getSize() + current.sizeShift)));
                current.cosmetic |= type.hasCosmetic();
            }
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
    public IDynamicStackHandler getEquippedCurios() { return new EquippedStacks(); }
    @Override public Map<String, ICurioStacksHandler> getCurios() {
        ensureSlotTypes();
        Map<String, ICurioStacksHandler> result = new LinkedHashMap<String, ICurioStacksHandler>();
        result.putAll(slots);
        return Collections.unmodifiableMap(result);
    }
    @Override public int getSlots() {
        ensureSlotTypes();
        int count = 0;
        for (Slots slot : slots.values()) count += slot.getSlots();
        return count;
    }
    @Override public int getVisibleSlots() {
        ensureSlotTypes();
        int count = 0;
        for (Slots slot : slots.values()) if (slot.isVisible()) count += slot.getSlots();
        return count;
    }
    @Override public Set<String> getLockedSlots() { return Collections.unmodifiableSet(lockedSlots); }

    @Override public boolean equip(String identifier, int index, ItemStack stack) {
        ensureSlotTypes();
        if (stack == null || stack.stackSize != 1) return false;
        StackHandler stacks = slot(identifier).stacks;
        if (!stacks.isValidSlot(index) || stacks.getStackInSlot(index) != null) return false;
        return replace(identifier, index, stack);
    }

    public boolean tryEquipFromRightClick(EntityPlayer player, ItemStack heldStack) {
        if (player == null || heldStack == null || heldStack.stackSize <= 0) return false;
        ICurio curio = getCurio(heldStack);
        if (curio == null || !curio.canRightClickEquip()) return false;

        ensureSlotTypes();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            StackHandler stacks = entry.getValue().stacks;
            if (!CuriosApi.isValidForSlot(heldStack.getItem(), entry.getKey())) continue;
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (stacks.getStackInSlot(index) != null) continue;
                if (player.worldObj.isRemote) {
                    if (curio.canEquip(entry.getKey(), player)) return true;
                    continue;
                }

                ItemStack equipped = heldStack.copy();
                equipped.stackSize = 1;
                if (!equip(entry.getKey(), index, equipped)) continue;

                int inventoryIndex = player.inventory.currentItem;
                heldStack.stackSize--;
                player.inventory.setInventorySlotContents(inventoryIndex, heldStack.stackSize > 0 ? heldStack : null);
                player.inventoryContainer.detectAndSendChanges();
                curio.playRightClickEquipSound(player);
                return true;
            }
        }
        return false;
    }

    @Override public ItemStack unequip(String identifier, int index) {
        ensureSlotTypes();
        StackHandler stacks = slot(identifier).stacks;
        if (!stacks.isValidSlot(index)) return null;
        ItemStack old = stacks.getStackInSlot(index);
        return replace(identifier, index, null) ? old : null;
    }

    public void onBrokenCurio(String identifier, int index) {
        ensureSlotTypes();
        Slots slot = slots.get(identifier);
        EntityLivingBase owner = entity.get();
        if (slot == null || owner == null || owner.worldObj.isRemote || !slot.stacks.isValidSlot(index)) return;

        ItemStack broken = slot.stacks.getStackInSlot(index);
        if (broken == null) return;
        Multimap<Attribute, AttributeModifier> previousModifiers = getAttributeModifiers();
        ICurio curio = getCurio(broken);
        CuriosNetwork.sendBreakEvent(owner, identifier, index, broken.copy());
        slot.stacks.setRaw(index, null);
        applyAttributeChanges(previousModifiers);
        if (curio != null) curio.onUnequip(identifier, index, owner);
        slot.stacks.setPreviousStackInSlot(index, null);
        CuriosNetwork.sync(owner);
    }

    public void onBrokenCurio(ItemStack broken) {
        if (broken == null) return;
        ensureSlotTypes();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            StackHandler stacks = entry.getValue().stacks;
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (stacks.getStackInSlot(index) == broken) {
                    onBrokenCurio(entry.getKey(), index);
                    return;
                }
            }
        }
    }

    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        ensureSlotTypes();
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ICurio curio = getCurio(stacks.getStackInSlot(i));
                if (curio == null) continue;
                Multimap<Attribute, AttributeModifier> curioModifiers = curio.getAttributeModifiers(entry.getKey());
                for (Map.Entry<Attribute, AttributeModifier> modifierEntry : curioModifiers.entries()) {
                    AttributeModifier modifier = modifierEntry.getValue();
                    UUID id = UUID.nameUUIDFromBytes((entry.getKey() + ":" + i + ":" + modifier.getID())
                        .getBytes(StandardCharsets.UTF_8));
                    AttributeModifier slotModifier = new AttributeModifier(id, modifier.getName(),
                        modifier.getAmount(), modifier.getOperation()).setSaved(modifier.isSaved());
                    modifiers.put(modifierEntry.getKey(), slotModifier);
                }
            }
        }
        return modifiers;
    }

    public int getFortuneBonus() { return getEnchantmentBonus(true); }

    public int getLootingBonus() { return getEnchantmentBonus(false); }

    public void onDeath() {
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote || owner instanceof EntityPlayer) return;

        boolean keepInventory = owner.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory");
        boolean mobLoot = owner.worldObj.getGameRules().getGameRuleBooleanValue("doMobLoot");
        Multimap<Attribute, AttributeModifier> previousModifiers = getAttributeModifiers();
        List<Runnable> unequipCallbacks = new ArrayList<Runnable>();
        boolean changed = false;

        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            Slots slot = entry.getValue();
            for (int index = 0; index < slot.getSlots(); index++) {
                ItemStack stack = slot.stacks.getStackInSlot(index);
                ICurio curio = getCurio(stack);
                if (removeOnDeath(stack, curio, owner, keepInventory, mobLoot)) {
                    slot.stacks.setRaw(index, null);
                    slot.stacks.setPreviousStackInSlot(index, null);
                    if (curio != null) {
                        final String identifier = entry.getKey();
                        final int slotIndex = index;
                        unequipCallbacks.add(() -> curio.onUnequip(identifier, slotIndex, owner));
                    }
                    changed = true;
                }

                ItemStack cosmetic = slot.cosmetics.getStackInSlot(index);
                if (removeOnDeath(cosmetic, getCurio(cosmetic), owner, keepInventory, mobLoot)) {
                    slot.cosmetics.setRaw(index, null);
                    slot.cosmetics.setPreviousStackInSlot(index, null);
                    changed = true;
                }
            }
        }

        if (changed) {
            applyAttributeChanges(previousModifiers);
            for (Runnable callback : unequipCallbacks) callback.run();
            CuriosNetwork.sync(owner);
        }
    }

    private boolean removeOnDeath(ItemStack stack, ICurio curio, EntityLivingBase owner,
                                  boolean keepInventory, boolean mobLoot) {
        if (stack == null) return false;
        ICurio.DropRule rule = curio == null ? ICurio.DropRule.DEFAULT : curio.getDropRule(owner);
        DeathDisposition disposition = getDeathDisposition(rule, keepInventory, mobLoot);
        if (disposition == DeathDisposition.KEEP) return false;
        if (disposition == DeathDisposition.DROP) owner.dropItemStack(stack.copy(), 0.0F);
        return true;
    }

    static DeathDisposition getDeathDisposition(ICurio.DropRule rule, boolean keepInventory, boolean mobLoot) {
        if (rule == null) rule = ICurio.DropRule.DEFAULT;
        if (rule == ICurio.DropRule.ALWAYS_KEEP
            || (rule == ICurio.DropRule.DEFAULT && keepInventory)) return DeathDisposition.KEEP;
        if (rule == ICurio.DropRule.DESTROY || !mobLoot) return DeathDisposition.DESTROY;
        return DeathDisposition.DROP;
    }

    enum DeathDisposition { KEEP, DROP, DESTROY }

    private int getEnchantmentBonus(boolean fortune) {
        ensureSlotTypes();
        EntityLivingBase owner = entity.get();
        if (owner == null) return 0;

        int bonus = 0;
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int index = 0; index < stacks.getSlots(); index++) {
                ItemStack stack = stacks.getStackInSlot(index);
                ICurio curio = getCurio(stack);
                if (stack != null && curio != null) {
                    bonus += fortune
                        ? curio.getFortuneBonus(entry.getKey(), owner, stack, index)
                        : curio.getLootingBonus(entry.getKey(), owner, stack, index);
                }
            }
        }
        return bonus;
    }

    @Override public void tick() {
        ensureSlotTypes();
        EntityLivingBase owner = entity.get();
        if (owner == null) return;
        processSlots();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            Slots slot = entry.getValue();
            for (int i = 0; i < slot.getSlots(); i++) {
                ItemStack stack = slot.stacks.getStackInSlot(i);
                if (!owner.worldObj.isRemote) processEquipChange(slot, i, stack, owner);
                stack = slot.stacks.getStackInSlot(i);
                ICurio functional = getCurio(stack);
                if (functional != null) {
                    functional.curioTick(entry.getKey(), i, owner);
                    tickCurioData(owner, entry.getKey(), i, functional, false);
                }
                ICurio cosmetic = getCurio(slot.cosmetics.getStackInSlot(i));
                if (cosmetic != null) tickCurioData(owner, entry.getKey(), i, cosmetic, true);
            }
        }
    }

    private void processEquipChange(Slots slot, int index, ItemStack current, EntityLivingBase owner) {
        ItemStack previous = slot.stacks.getPreviousStackInSlot(index);
        if (ItemStack.areItemStacksEqual(current, previous)) return;

        boolean sameItem = current != null && previous != null
            && current.getItem() == previous.getItem();
        if (!sameItem) {
            ICurio previousCurio = getCurio(previous);
            ICurio currentCurio = getCurio(current);
            if (previousCurio != null) previousCurio.onUnequip(slot.id, index, owner);
            if (currentCurio != null) currentCurio.onEquip(slot.id, index, owner);
        }
        slot.stacks.setPreviousStackInSlot(index, slot.stacks.getStackInSlot(index));
    }

    private void markCurrentStacksAsPrevious() {
        for (Slots slot : slots.values()) {
            for (int index = 0; index < slot.getSlots(); index++) {
                slot.stacks.setPreviousStackInSlot(index, slot.stacks.getStackInSlot(index));
                slot.cosmetics.setPreviousStackInSlot(index, slot.cosmetics.getStackInSlot(index));
            }
        }
    }

    private void tickCurioData(EntityLivingBase owner, String identifier, int index,
                               ICurio curio, boolean cosmetic) {
        if (owner.worldObj.isRemote) curio.curioAnimate(identifier, index, owner);
        else if (curio.canSync(identifier, index, owner)) {
            CuriosNetwork.syncCurioData(owner, identifier, index, cosmetic, curio.writeSyncData());
        }
    }

    public NBTTagCompound writeToNBT() {
        ensureSlotTypes();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList savedSlots = new NBTTagList();
        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            Slots slot = entry.getValue();
            NBTTagCompound slotState = new NBTTagCompound();
            slotState.setString("Identifier", entry.getKey());
            slotState.setBoolean("State", true);
            slotState.setInteger("SizeShift", slot.sizeShift);
            slotState.setBoolean("Visible", slot.visible);
            slotState.setBoolean("CosmeticEnabled", slot.cosmetic);
            NBTTagList renderStates = new NBTTagList();
            for (int index = 0; index < slot.getSlots(); index++) {
                NBTTagCompound renderState = new NBTTagCompound();
                renderState.setInteger("Index", index);
                renderState.setBoolean("Render", slot.getRenders().get(index));
                renderStates.appendTag(renderState);
            }
            slotState.setTag("Renders", renderStates);
            savedSlots.appendTag(slotState);
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
        NBTTagList savedLocks = new NBTTagList();
        for (String identifier : lockedSlots) {
            NBTTagCompound lock = new NBTTagCompound();
            lock.setString("Identifier", identifier);
            savedLocks.appendTag(lock);
        }
        data.setTag("Locked", savedLocks);
        return data;
    }

    public void copyForClone(EntityPlayer target, boolean cloneAll) {
        EntityLivingBase owner = entity.get();
        if (!(owner instanceof EntityPlayer) || target == null || target == owner || owner.worldObj.isRemote) return;
        ensureSlotTypes();
        processSlots();

        Multimap<Attribute, AttributeModifier> sourceModifiers = getAttributeModifiers();
        CuriosInventory targetInventory = CuriosInventory.get(target);
        targetInventory.readFromNBT(writeToNBT());
        Multimap<Attribute, AttributeModifier> targetModifiers = targetInventory.getAttributeModifiers();
        boolean keepInventory = cloneAll || owner.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory");
        List<Runnable> cloneCallbacks = new ArrayList<Runnable>();

        for (Map.Entry<String, Slots> entry : slots.entrySet()) {
            Slots sourceSlot = entry.getValue();
            Slots targetSlot = targetInventory.slots.get(entry.getKey());
            if (targetSlot == null) continue;
            for (int index = 0; index < sourceSlot.getSlots(); index++) {
                ItemStack equipped = sourceSlot.stacks.getStackInSlot(index);
                boolean retained = transferOnClone((EntityPlayer) owner, targetSlot.stacks,
                    index, equipped, keepInventory);
                ICurio curio = getCurio(equipped);
                if (curio != null) {
                    final String identifier = entry.getKey();
                    final int slotIndex = index;
                    cloneCallbacks.add(() -> curio.onUnequip(identifier, slotIndex, owner));
                    if (retained) {
                        ItemStack transferred = targetSlot.stacks.getStackInSlot(index);
                        ICurio transferredCurio = getCurio(transferred);
                        if (transferredCurio != null) {
                            cloneCallbacks.add(() -> transferredCurio.onEquip(identifier, slotIndex, target));
                        }
                    }
                }
                transferOnClone((EntityPlayer) owner, targetSlot.cosmetics, index,
                    sourceSlot.cosmetics.getStackInSlot(index), keepInventory);
            }
            sourceSlot.stacks.clear();
            sourceSlot.cosmetics.clear();
        }

        applyAttributeChanges(sourceModifiers);
        targetInventory.applyAttributeChanges(targetModifiers);
        for (Runnable callback : cloneCallbacks) callback.run();
        markCurrentStacksAsPrevious();
        targetInventory.markCurrentStacksAsPrevious();
        CuriosNetwork.sync(target);
    }

    private boolean transferOnClone(EntityPlayer owner, StackHandler target, int index,
                                    ItemStack stack, boolean keepInventory) {
        if (stack == null) return false;
        ICurio curio = getCurio(stack);
        ICurio.DropRule rule = curio == null ? ICurio.DropRule.DEFAULT : curio.getDropRule(owner);
        if (rule == ICurio.DropRule.DESTROY) {
            target.setRaw(index, null);
            return false;
        } else if (rule == ICurio.DropRule.ALWAYS_DROP
            || (rule == ICurio.DropRule.DEFAULT && !keepInventory)) {
            target.setRaw(index, null);
            owner.dropPlayerItem(stack.copy());
            return false;
        }
        return true;
    }

    public void readFromNBT(NBTTagCompound data) {
        ensureSlotTypes();
        Multimap<Attribute, AttributeModifier> previous = getAttributeModifiers();
        for (Slots slot : slots.values()) {
            slot.stacks.clear();
            slot.cosmetics.clear();
        }
        slots.clear();
        lockedSlots.clear();
        initializedSlots.clear();
        for (ISlotType type : CuriosSlotRegistry.getAll().values()) {
            initializedSlots.add(type.getIdentifier());
            if (type.isLocked()) lockedSlots.add(type.getIdentifier());
            else slots.put(type.getIdentifier(), new Slots(type.getIdentifier(), type.getSize(), type.isVisible(), type.hasCosmetic()));
        }
        NBTTagList savedSlots = data.getTagList("Slots");
        for (int i = 0; i < savedSlots.tagCount(); i++) {
            if (!(savedSlots.tagAt(i) instanceof NBTTagCompound)) continue;
            NBTTagCompound savedSlot = (NBTTagCompound) savedSlots.tagAt(i);
            if (!savedSlot.hasKey("State")) continue;
            String identifier = savedSlot.getString("Identifier");
            ISlotType type = CuriosSlotRegistry.get(identifier).orElse(null);
            Slots slot = slots.get(identifier);
            if (slot == null) {
                slot = new Slots(identifier, type == null ? 1 : type.getSize(),
                    type == null || type.isVisible(), type != null && type.hasCosmetic());
                slots.put(identifier, slot);
                lockedSlots.remove(identifier);
            }
            slot.sizeShift = savedSlot.getInteger("SizeShift");
            slot.resize(Math.max(1, Math.min(256, (type == null ? 1 : type.getSize()) + slot.sizeShift)));
            slot.visible = savedSlot.getBoolean("Visible");
            slot.cosmetic = savedSlot.getBoolean("CosmeticEnabled");
            NBTTagList renderStates = savedSlot.getTagList("Renders");
            for (int renderIndex = 0; renderIndex < renderStates.tagCount(); renderIndex++) {
                if (!(renderStates.tagAt(renderIndex) instanceof NBTTagCompound)) continue;
                NBTTagCompound renderState = (NBTTagCompound) renderStates.tagAt(renderIndex);
                int index = renderState.getInteger("Index");
                if (index >= 0 && index < slot.renders.size()) {
                    slot.renders.set(index, renderState.getBoolean("Render"));
                }
            }
        }
        NBTTagList savedLocks = data.getTagList("Locked");
        for (int i = 0; i < savedLocks.tagCount(); i++) {
            if (savedLocks.tagAt(i) instanceof NBTTagCompound) {
                String identifier = ((NBTTagCompound) savedLocks.tagAt(i)).getString("Identifier");
                slots.remove(identifier);
                lockedSlots.add(identifier);
                initializedSlots.add(identifier);
            }
        }
        for (int i = 0; i < savedSlots.tagCount(); i++) {
            if (!(savedSlots.tagAt(i) instanceof NBTTagCompound)) continue;
            NBTTagCompound savedSlot = (NBTTagCompound) savedSlots.tagAt(i);
            if (savedSlot.hasKey("State")) continue;
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
        return CuriosApi.getCurio(stack).orElse(null);
    }

    private boolean replace(String identifier, int index, ItemStack stack) {
        StackHandler stacks = slot(identifier).stacks;
        if (!stacks.isValidSlot(index)) return false;
        if (stack != null && stack.stackSize != 1) return false;
        ItemStack previousStack = stacks.getStackInSlot(index);
        if (previousStack == stack) return true;
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return false;
        ICurio previousCurio = getCurio(previousStack);
        ICurio nextCurio = getCurio(stack);
        if (stack != null && (nextCurio == null || !CuriosApi.isValidForSlot(stack.getItem(), identifier))) return false;
        if (previousCurio != null && !previousCurio.canUnequip(identifier, owner)) return false;
        if (nextCurio != null && !nextCurio.canEquip(identifier, owner)) return false;

        Multimap<Attribute, AttributeModifier> previousModifiers = getAttributeModifiers();
        stacks.setPreviousStackInSlot(index, previousStack);
        stacks.setRaw(index, stack);
        applyAttributeChanges(previousModifiers);
        if (previousCurio != null) previousCurio.onUnequip(identifier, index, owner);
        if (nextCurio != null) nextCurio.onEquip(identifier, index, owner);
        stacks.setPreviousStackInSlot(index, stacks.getStackInSlot(index));
        CuriosNetwork.sync(owner);
        return true;
    }

    private Slots slot(String id) { Slots s = slots.get(id); if (s == null) throw new IllegalArgumentException("Unknown Curios slot: " + id); return s; }

    @Override public void unlockSlotType(String identifier, int amount, boolean visible, boolean cosmetic) {
        if (identifier == null || amount < 1 || amount > 256) throw new IllegalArgumentException("Invalid Curios slot unlock request");
        pendingUnlocks.add(new UnlockState(identifier, amount, visible, cosmetic));
    }

    @Override public void lockSlotType(String identifier) {
        if (identifier != null) pendingLocks.add(identifier);
    }

    @Override public void processSlots() {
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return;
        boolean changed = false;
        Multimap<Attribute, AttributeModifier> previousModifiers = getAttributeModifiers();
        List<Runnable> unequipCallbacks = new ArrayList<Runnable>();
        for (String identifier : pendingLocks) {
            Slots removed = slots.remove(identifier);
            if (removed == null) continue;
            changed = true;
            lockedSlots.add(identifier);
            for (int i = 0; i < removed.getSlots(); i++) {
                final int slotIndex = i;
                ICurio curio = getCurio(removed.stacks.getStackInSlot(i));
                if (curio != null) unequipCallbacks.add(() -> curio.onUnequip(identifier, slotIndex, owner));
                returnStack(owner, removed.stacks.getStackInSlot(i));
                returnStack(owner, removed.cosmetics.getStackInSlot(i));
            }
        }
        pendingLocks.clear();
        for (UnlockState unlock : pendingUnlocks) {
            if (slots.containsKey(unlock.identifier)) continue;
            Slots unlocked = new Slots(unlock.identifier, unlock.amount, unlock.visible, unlock.cosmetic);
            int registeredSize = CuriosSlotRegistry.get(unlock.identifier).map(ISlotType::getSize).orElse(1);
            unlocked.sizeShift = unlock.amount - registeredSize;
            slots.put(unlock.identifier, unlocked);
            lockedSlots.remove(unlock.identifier);
            initializedSlots.add(unlock.identifier);
            changed = true;
        }
        pendingUnlocks.clear();
        if (changed) {
            applyAttributeChanges(previousModifiers);
            for (Runnable callback : unequipCallbacks) callback.run();
            CuriosNetwork.sync(owner);
        }
    }

    @Override public void growSlotType(String identifier, int amount) {
        if (amount <= 0) return;
        ensureSlotTypes();
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return;
        Slots slot = slots.get(identifier);
        if (slot == null) return;
        if (amount > 256 - slot.getSlots()) {
            throw new IllegalArgumentException("Curios slot size cannot exceed 256: " + identifier);
        }
        slot.grow(amount);
        CuriosNetwork.sync(owner);
    }

    @Override public void shrinkSlotType(String identifier, int amount) {
        if (amount <= 0) return;
        ensureSlotTypes();
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return;
        Slots slot = slots.get(identifier);
        if (slot == null) return;
        int shrink = Math.min(amount, slot.getSlots() - 1);
        if (shrink == 0) return;
        Multimap<Attribute, AttributeModifier> previousModifiers = getAttributeModifiers();
        List<Runnable> unequipCallbacks = new ArrayList<Runnable>();
        for (int i = slot.getSlots() - shrink; i < slot.getSlots(); i++) {
            final int slotIndex = i;
            ICurio curio = getCurio(slot.stacks.getStackInSlot(slotIndex));
            if (curio != null) {
                unequipCallbacks.add(() -> curio.onUnequip(identifier, slotIndex, owner));
            }
            returnStack(owner, slot.stacks.getStackInSlot(i));
            returnStack(owner, slot.cosmetics.getStackInSlot(i));
        }
        slot.shrink(shrink);
        applyAttributeChanges(previousModifiers);
        for (Runnable callback : unequipCallbacks) callback.run();
        CuriosNetwork.sync(owner);
    }

    private void returnStack(EntityLivingBase owner, ItemStack stack) {
        if (stack == null) return;
        if (owner instanceof net.minecraft.ServerPlayer) {
            ((net.minecraft.ServerPlayer) owner).inventory.addItemStackToInventoryOrDropIt(stack);
        } else {
            owner.dropItemStack(stack, 0.0F);
        }
    }

    private boolean canSetCosmetic(String identifier, int index, ItemStack stack) {
        ensureSlotTypes();
        Slots slot = slot(identifier);
        if (!slot.cosmetic || !slot.cosmetics.isValidSlot(index)) return false;
        if (stack != null && stack.stackSize != 1) return false;
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return false;
        ItemStack previous = slot.cosmetics.getStackInSlot(index);
        if (previous == stack) return true;
        ICurio previousCurio = getCurio(previous);
        ICurio nextCurio = getCurio(stack);
        if (stack != null && !CuriosApi.isValidForSlot(stack.getItem(), identifier)) return false;
        if (previousCurio != null && !previousCurio.canUnequip(identifier, owner)) return false;
        return nextCurio == null || nextCurio.canEquip(identifier, owner);
    }

    public boolean setCosmetic(String identifier, int index, ItemStack stack) {
        if (!canSetCosmetic(identifier, index, stack)) return false;
        Slots slot = slot(identifier);
        ItemStack previous = slot.cosmetics.getStackInSlot(index);
        EntityLivingBase owner = entity.get();
        if (owner == null || owner.worldObj.isRemote) return false;
        slot.cosmetics.setPreviousStackInSlot(index, previous);
        slot.cosmetics.setRaw(index, stack);
        CuriosNetwork.sync(owner);
        return true;
    }

    private SlotAddress resolveEquippedSlot(int index) {
        ensureSlotTypes();
        if (index < 0) throw new IndexOutOfBoundsException("Curios slot index: " + index);
        for (Slots slot : slots.values()) {
            int size = slot.stacks.getSlots();
            if (index < size) return new SlotAddress(slot.stacks, index);
            index -= size;
        }
        throw new IndexOutOfBoundsException("Curios slot index: " + index);
    }

    private final class EquippedStacks implements IDynamicStackHandler {
        @Override public int getSlots() {
            ensureSlotTypes();
            int count = 0;
            for (Slots slot : slots.values()) count += slot.stacks.getSlots();
            return count;
        }
        @Override public ItemStack getStackInSlot(int index) {
            SlotAddress address = resolveEquippedSlot(index);
            return address.handler.getStackInSlot(address.index);
        }
        @Override public void setStackInSlot(int index, ItemStack stack) {
            SlotAddress address = resolveEquippedSlot(index);
            address.handler.setStackInSlot(address.index, stack);
        }
    }

    private static final class SlotAddress {
        private final IDynamicStackHandler handler;
        private final int index;
        private SlotAddress(IDynamicStackHandler handler, int index) { this.handler = handler; this.index = index; }
    }

    private final class Slots implements ICurioStacksHandler {
        private final String id; private final StackHandler stacks, cosmetics;
        private List<Boolean> renders;
        private int sizeShift;
        private boolean visible;
        private boolean cosmetic;
        private Slots(String id, int size, boolean visible, boolean cosmetic) {
            this.id = id;
            this.stacks = new StackHandler(id, size, true);
            this.cosmetics = new StackHandler(id, size, false);
            this.renders = new ArrayList<Boolean>(Collections.nCopies(size, Boolean.TRUE));
            this.visible = visible;
            this.cosmetic = cosmetic;
        }
        @Override public String getIdentifier() { return id; }
        @Override public IDynamicStackHandler getStacks() { return stacks; }
        @Override public IDynamicStackHandler getCosmeticStacks() { return cosmetics; }
        @Override public List<Boolean> getRenders() {
            int size = stacks.getSlots();
            while (renders.size() < size) renders.add(Boolean.TRUE);
            while (renders.size() > size) renders.remove(renders.size() - 1);
            for (int index = 0; index < size; index++) {
                if (renders.get(index) == null) renders.set(index, Boolean.TRUE);
            }
            return renders;
        }
        @Override public int getSlots() { return stacks.getSlots(); }
        @Override public int getSizeShift() { return sizeShift; }
        @Override public boolean isVisible() { return visible; }
        @Override public boolean hasCosmetic() { return cosmetic; }
        private void resize(int size) {
            stacks.resize(size);
            cosmetics.resize(size);
            List<Boolean> resizedRenders = new ArrayList<Boolean>(Collections.nCopies(size, Boolean.TRUE));
            for (int index = 0; index < Math.min(size, renders.size()); index++) {
                resizedRenders.set(index, renders.get(index));
            }
            renders = resizedRenders;
        }
        private void grow(int amount) { resize(getSlots() + amount); sizeShift += amount; }
        private void shrink(int amount) { resize(Math.max(1, getSlots() - amount)); sizeShift -= amount; }
    }

    private static final class UnlockState {
        private final String identifier;
        private final int amount;
        private final boolean visible;
        private final boolean cosmetic;
        private UnlockState(String identifier, int amount, boolean visible, boolean cosmetic) {
            this.identifier = identifier;
            this.amount = amount;
            this.visible = visible;
            this.cosmetic = cosmetic;
        }
    }
    private final class StackHandler implements IDynamicStackHandler {
        private ItemStack[] values;
        private ItemStack[] previousValues;
        private final String identifier;
        private final boolean functional;
        private StackHandler(String identifier, int size, boolean functional) {
            values = new ItemStack[size];
            previousValues = new ItemStack[size];
            this.identifier = identifier;
            this.functional = functional;
        }
        @Override public int getSlots() { return values.length; }
        @Override public ItemStack getStackInSlot(int slot) { return values[slot]; }
        @Override public ItemStack getPreviousStackInSlot(int slot) { return previousValues[slot]; }
        @Override public void setPreviousStackInSlot(int slot, ItemStack stack) {
            if (!isValidSlot(slot)) throw new IndexOutOfBoundsException("Curios slot index: " + slot);
            previousValues[slot] = stack == null ? null : stack.copy();
        }
        @Override public void grow(int amount) {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
            if (amount == 0) return;
            CuriosInventory.this.growSlotType(identifier, amount);
        }
        @Override public void shrink(int amount) {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
            if (amount == 0) return;
            CuriosInventory.this.shrinkSlotType(identifier, amount);
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (!isValidSlot(slot)) throw new IndexOutOfBoundsException("Curios slot index: " + slot);
            EntityLivingBase owner = entity.get();
            if (owner == null || owner.worldObj.isRemote) return;
            if (functional) replace(identifier, slot, stack);
            else if (CuriosInventory.this.slot(identifier).hasCosmetic()) setCosmetic(identifier, slot, stack);
        }
        private void setRaw(int slot, ItemStack stack) { values[slot] = stack; }
        private void clear() {
            java.util.Arrays.fill(values, null);
            java.util.Arrays.fill(previousValues, null);
        }
        private boolean isValidSlot(int slot) { return slot >= 0 && slot < values.length; }
        private void resize(int size) {
            if (size == values.length) return;
            values = java.util.Arrays.copyOf(values, size);
            previousValues = java.util.Arrays.copyOf(previousValues, size);
        }
    }
}
