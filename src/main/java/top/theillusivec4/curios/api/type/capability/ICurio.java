package top.theillusivec4.curios.api.type.capability;

import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;

/** Behaviour attached to an item while it is equipped in a Curios slot. */
public interface ICurio {
    default ItemStack getStack() { return null; }
    default void onEquip(String identifier, int index, EntityLivingBase entity) {}
    default void onUnequip(String identifier, int index, EntityLivingBase entity) {}
    default void curioTick(String identifier, int index, EntityLivingBase entity) {}
    default boolean canEquip(String identifier, EntityLivingBase entity) { return true; }
    default boolean canUnequip(String identifier, EntityLivingBase entity) { return true; }
}
