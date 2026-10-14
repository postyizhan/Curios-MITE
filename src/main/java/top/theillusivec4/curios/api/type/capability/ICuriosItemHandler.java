package top.theillusivec4.curios.api.type.capability;

import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.ItemStack;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public interface ICuriosItemHandler {
    Map<String, ICurioStacksHandler> getCurios();
    int getSlots();
    int getVisibleSlots();
    Set<String> getLockedSlots();
    Optional<ICurioStacksHandler> getStacksHandler(String identifier);
    ItemStack getEquippedStack(String identifier, int index);
    boolean equip(String identifier, int index, ItemStack stack);
    ItemStack unequip(String identifier, int index);
    void unlockSlotType(String identifier, int amount, boolean visible, boolean cosmetic);
    void lockSlotType(String identifier);
    void processSlots();
    void growSlotType(String identifier, int amount);
    void shrinkSlotType(String identifier, int amount);
    Multimap<Attribute, AttributeModifier> getAttributeModifiers();
    void tick();
}
