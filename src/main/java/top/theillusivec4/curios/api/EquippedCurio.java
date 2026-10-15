package top.theillusivec4.curios.api;

import net.minecraft.ItemStack;

/** Identifies an equipped stack by its Curios slot type and index. */
public final class EquippedCurio {
    private final String identifier;
    private final int index;
    private final ItemStack stack;

    public EquippedCurio(String identifier, int index, ItemStack stack) {
        this.identifier = identifier;
        this.index = index;
        this.stack = stack;
    }

    public String getIdentifier() { return identifier; }
    public int getIndex() { return index; }
    public ItemStack getStack() { return stack; }
    public String getLeft() { return identifier; }
    public Integer getMiddle() { return index; }
    public ItemStack getRight() { return stack; }
}
