package top.theillusivec4.curios.api;

import java.util.Optional;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.Attribute;
import net.minecraft.AttributeModifier;
import net.minecraft.EntityLivingBase;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.CuriosSlotRegistry;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.CuriosInventory;

/** Stable access point for mods such as BountifulBaubles. */
public final class CuriosApi {
    private static final Map<Item, Function<ItemStack, ICurio>> CURIO_PROVIDERS =
        Collections.synchronizedMap(new IdentityHashMap<Item, Function<ItemStack, ICurio>>());
    private static final ICuriosHelper HELPER = new ICuriosHelper() {
        @Override public Optional<ICurio> getCurio(ItemStack stack) { return CuriosApi.getCurio(stack); }

        @Override public Optional<ICuriosItemHandler> getCuriosHandler(EntityLivingBase entity) {
            return CuriosApi.getCuriosHandler(entity);
        }

        @Override public Optional<IDynamicStackHandler> getEquippedCurios(EntityLivingBase entity) {
            return entity == null ? Optional.<IDynamicStackHandler>empty()
                : Optional.<IDynamicStackHandler>of(CuriosInventory.get(entity).getEquippedCurios());
        }

        @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(String identifier, ItemStack stack) {
            return CuriosApi.getCurio(stack).map(curio -> curio.getAttributeModifiers(identifier))
                .orElseGet(HashMultimap::create);
        }
    };

    private CuriosApi() {}

    public static ICuriosHelper getCuriosHelper() { return HELPER; }

    public static void registerCurioProvider(Item item, Function<ItemStack, ICurio> provider) {
        if (item == null || provider == null) throw new IllegalArgumentException("Curio item and provider are required");
        CURIO_PROVIDERS.put(item, provider);
    }

    public static Optional<ICurio> getCurio(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return Optional.empty();
        Function<ItemStack, ICurio> provider = CURIO_PROVIDERS.get(stack.getItem());
        ICurio curio = provider == null
            ? (stack.getItem() instanceof ICurioItem ? ((ICurioItem) stack.getItem()).getCurio(stack) : null)
            : provider.apply(stack);
        return Optional.ofNullable(curio);
    }

    public static ISlotType registerSlotType(String identifier, int size) {
        return CuriosSlotRegistry.register(identifier, size);
    }

    public static ISlotType registerSlotType(SlotTypeMessage message) {
        if (message == null) throw new IllegalArgumentException("Slot type message cannot be null");
        return CuriosSlotRegistry.register(message);
    }

    public static Optional<ISlotType> getSlotType(String identifier) {
        return CuriosSlotRegistry.get(identifier);
    }

    public static Map<String, ISlotType> getSlotTypes() {
        return CuriosSlotRegistry.getAll();
    }

    public static Optional<ICuriosItemHandler> getCuriosHandler(EntityLivingBase entity) {
        return entity == null ? Optional.<ICuriosItemHandler>empty() : Optional.<ICuriosItemHandler>of(CuriosInventory.get(entity));
    }
}
