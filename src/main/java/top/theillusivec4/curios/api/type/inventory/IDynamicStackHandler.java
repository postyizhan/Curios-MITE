package top.theillusivec4.curios.api.type.inventory;

import net.minecraft.ItemStack;

public interface IDynamicStackHandler {
    int getSlots();
    ItemStack getStackInSlot(int slot);
    void setStackInSlot(int slot, ItemStack stack);
    default ItemStack getPreviousStackInSlot(int slot) { return getStackInSlot(slot); }
    default void setPreviousStackInSlot(int slot, ItemStack stack) {}
    default void grow(int amount) { throw new UnsupportedOperationException(); }
    default void shrink(int amount) { throw new UnsupportedOperationException(); }
    default int getSlotLimit(int slot) { return 1; }
}
