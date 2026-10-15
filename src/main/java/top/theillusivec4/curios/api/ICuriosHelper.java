package top.theillusivec4.curios.api;

import java.util.Optional;
import java.util.function.Predicate;
import com.google.common.collect.Multimap;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.EntityLivingBase;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Entity-level Curios queries used by content mods. */
public interface ICuriosHelper {
    Optional<ICurio> getCurio(ItemStack stack);
    Optional<ICuriosItemHandler> getCuriosHandler(EntityLivingBase entity);
    Optional<IDynamicStackHandler> getEquippedCurios(EntityLivingBase entity);
    Optional<EquippedCurio> findEquippedCurio(Item item, EntityLivingBase entity);
    Optional<EquippedCurio> findEquippedCurio(Predicate<ItemStack> predicate, EntityLivingBase entity);
    void onBrokenCurio(String identifier, int index, EntityLivingBase entity);
    Multimap<Attribute, AttributeModifier> getAttributeModifiers(String identifier, ItemStack stack);
}
