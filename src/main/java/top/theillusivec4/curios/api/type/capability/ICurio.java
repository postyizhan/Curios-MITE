package top.theillusivec4.curios.api.type.capability;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;

/** Behaviour attached to an item while it is equipped in a Curios slot. */
public interface ICurio {
    enum DropRule { DEFAULT, ALWAYS_DROP, ALWAYS_KEEP, DESTROY }

    default ItemStack getStack() { return null; }
    default void onEquip(String identifier, int index, EntityLivingBase entity) {}
    default void onUnequip(String identifier, int index, EntityLivingBase entity) {}
    default void curioTick(String identifier, int index, EntityLivingBase entity) {}
    default void curioAnimate(String identifier, int index, EntityLivingBase entity) {}
    default boolean canEquip(String identifier, EntityLivingBase entity) { return true; }
    default boolean canUnequip(String identifier, EntityLivingBase entity) { return true; }
    default int getFortuneBonus(String identifier, EntityLivingBase entity, ItemStack stack, int index) { return 0; }
    default int getLootingBonus(String identifier, EntityLivingBase entity, ItemStack stack, int index) { return 0; }
    default boolean canRightClickEquip() { return false; }
    default void playRightClickEquipSound(EntityLivingBase entity) {
        entity.playSound("random.pop", 0.2F, 1.0F);
    }
    default void curioBreak(ItemStack stack, EntityLivingBase entity) {}
    default boolean canSync(String identifier, int index, EntityLivingBase entity) { return false; }
    default NBTTagCompound writeSyncData() { return new NBTTagCompound(); }
    default void readSyncData(NBTTagCompound data) {}
    default boolean canRender(String identifier, int index, EntityLivingBase entity) { return false; }
    default void render(String identifier, int index, EntityLivingBase entity, float partialTick) {}
    default DropRule getDropRule(EntityLivingBase entity) { return DropRule.DEFAULT; }
    default Multimap<Attribute, AttributeModifier> getAttributeModifiers(String identifier) {
        return HashMultimap.create();
    }
}
