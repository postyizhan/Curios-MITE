package top.theillusivec4.curios.api.type.capability;

import com.google.common.collect.Multimap;
import java.util.Optional;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.ItemStack;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public interface ICuriosItemHandler {
    Optional<ICurioStacksHandler> getStacksHandler(String identifier);
    ItemStack getEquippedStack(String identifier, int index);
    boolean equip(String identifier, int index, ItemStack stack);
    ItemStack unequip(String identifier, int index);
    Multimap<Attribute, AttributeModifier> getAttributeModifiers();
    void tick();
}
