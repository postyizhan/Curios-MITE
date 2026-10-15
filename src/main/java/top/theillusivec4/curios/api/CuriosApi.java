package top.theillusivec4.curios.api;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.CuriosTagLoader;
import top.theillusivec4.curios.common.CuriosSlotRegistry;
import top.theillusivec4.curios.common.CuriosInventory;

/** Stable access point for mods such as BountifulBaubles. */
public final class CuriosApi {
    private static final Map<Item, Function<ItemStack, ICurio>> CURIO_PROVIDERS =
        Collections.synchronizedMap(new IdentityHashMap<Item, Function<ItemStack, ICurio>>());
    private static final Map<Item, Set<String>> CURIO_TAGS =
        Collections.synchronizedMap(new IdentityHashMap<Item, Set<String>>());
    private static final Map<Item, Set<String>> LOADED_CURIO_TAGS =
        Collections.synchronizedMap(new IdentityHashMap<Item, Set<String>>());
    private static final Map<Item, String> ITEM_IDENTIFIERS =
        Collections.synchronizedMap(new IdentityHashMap<Item, String>());
    private static final ICuriosHelper HELPER = new ICuriosHelper() {
        @Override public Optional<ICurio> getCurio(ItemStack stack) { return CuriosApi.getCurio(stack); }

        @Override public Optional<ICuriosItemHandler> getCuriosHandler(EntityLivingBase entity) {
            return CuriosApi.getCuriosHandler(entity);
        }

        @Override public Optional<IDynamicStackHandler> getEquippedCurios(EntityLivingBase entity) {
            return entity == null ? Optional.<IDynamicStackHandler>empty()
                : Optional.<IDynamicStackHandler>of(CuriosInventory.get(entity).getEquippedCurios());
        }

        @Override public Set<String> getCurioTags(Item item) {
            if (item == null) return Collections.emptySet();
            CuriosTagLoader.loadIfNeeded();
            synchronized (CURIO_TAGS) {
                Set<String> result = new LinkedHashSet<String>();
                Set<String> registered = CURIO_TAGS.get(item);
                Set<String> loaded = LOADED_CURIO_TAGS.get(item);
                if (registered != null) result.addAll(registered);
                if (loaded != null) result.addAll(loaded);
                return Collections.unmodifiableSet(result);
            }
        }

        @Override public Optional<EquippedCurio> findEquippedCurio(Item item, EntityLivingBase entity) {
            return item == null ? Optional.empty()
                : findEquippedCurio(stack -> stack.getItem() == item, entity);
        }

        @Override public Optional<EquippedCurio> findEquippedCurio(Predicate<ItemStack> predicate,
                                                                    EntityLivingBase entity) {
            if (predicate == null || entity == null) return Optional.empty();
            for (Map.Entry<String, ICurioStacksHandler> entry : CuriosInventory.get(entity).getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();
                for (int index = 0; index < stacks.getSlots(); index++) {
                    ItemStack stack = stacks.getStackInSlot(index);
                    if (stack != null && predicate.test(stack)) {
                        return Optional.of(new EquippedCurio(entry.getKey(), index, stack));
                    }
                }
            }
            return Optional.empty();
        }

        @Override public void onBrokenCurio(String identifier, int index, EntityLivingBase entity) {
            if (entity != null) CuriosInventory.get(entity).onBrokenCurio(identifier, index);
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

    public static void registerCurioTag(Item item, String identifier) {
        if (item == null || identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Curio item and tag identifier are required");
        }
        synchronized (CURIO_TAGS) {
            CURIO_TAGS.computeIfAbsent(item, ignored -> new LinkedHashSet<String>()).add(identifier);
        }
    }

    public static void reloadCurioTags() {
        CuriosTagLoader.reload();
    }

    public static void registerItemIdentifier(Item item, String namespace, String path) {
        if (item == null || namespace == null || path == null || namespace.isEmpty() || path.isEmpty()) return;
        if (path.startsWith("item.")) path = path.substring("item.".length());
        ITEM_IDENTIFIERS.put(item, namespace.toLowerCase(java.util.Locale.ROOT) + ":"
            + path.toLowerCase(java.util.Locale.ROOT));
    }

    public static String getItemIdentifier(Item item) {
        return ITEM_IDENTIFIERS.get(item);
    }

    public static void replaceLoadedCurioTags(Map<Item, Set<String>> tags) {
        synchronized (CURIO_TAGS) {
            LOADED_CURIO_TAGS.clear();
            for (Map.Entry<Item, Set<String>> entry : tags.entrySet()) {
                LOADED_CURIO_TAGS.put(entry.getKey(), new LinkedHashSet<String>(entry.getValue()));
            }
        }
    }

    public static boolean isValidForSlot(Item item, String identifier) {
        Set<String> tags = HELPER.getCurioTags(item);
        return tags.isEmpty() || tags.contains(identifier) || tags.contains("curio");
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

    public static int getFortuneBonus(EntityLivingBase entity) {
        return entity == null ? 0 : CuriosInventory.get(entity).getFortuneBonus();
    }

    public static int getLootingBonus(EntityLivingBase entity) {
        return entity == null ? 0 : CuriosInventory.get(entity).getLootingBonus();
    }
}
