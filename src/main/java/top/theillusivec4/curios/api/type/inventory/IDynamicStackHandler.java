package top.theillusivec4.curios.api.type.inventory;

import net.minecraft.ItemStack;

public interface IDynamicStackHandler {
    int getSlots();
    ItemStack getStackInSlot(int slot);
    void setStackInSlot(int slot, ItemStack stack);
    default int getSlotLimit(int slot) { return 1; }
}
