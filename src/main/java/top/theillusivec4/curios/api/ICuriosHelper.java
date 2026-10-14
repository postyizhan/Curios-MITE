package top.theillusivec4.curios.api;

import java.util.Optional;
import net.minecraft.EntityLivingBase;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Entity-level Curios queries used by content mods. */
public interface ICuriosHelper {
    Optional<ICuriosItemHandler> getCuriosHandler(EntityLivingBase entity);
    Optional<IDynamicStackHandler> getEquippedCurios(EntityLivingBase entity);
}
