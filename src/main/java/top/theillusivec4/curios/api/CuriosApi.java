package top.theillusivec4.curios.api;

import java.util.Optional;
import java.util.Map;
import net.minecraft.EntityLivingBase;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.CuriosSlotRegistry;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.CuriosInventory;

/** Stable access point for mods such as BountifulBaubles. */
public final class CuriosApi {
    private CuriosApi() {}

    public static ISlotType registerSlotType(String identifier, int size) {
        return CuriosSlotRegistry.register(identifier, size);
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
