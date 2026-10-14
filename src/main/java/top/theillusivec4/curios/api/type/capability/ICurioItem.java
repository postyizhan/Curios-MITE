package top.theillusivec4.curios.api.type.capability;

import net.minecraft.ItemStack;

/** Implement on an item class to expose its equipped behaviour. */
public interface ICurioItem {
    ICurio getCurio(ItemStack stack);
}
