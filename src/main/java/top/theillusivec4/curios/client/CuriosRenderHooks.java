package top.theillusivec4.curios.client;

import java.util.Map;
import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosRenderHooks {
    private CuriosRenderHooks() {}

    public static void render(EntityLivingBase entity, float partialTick) {
        ICuriosItemHandler handler = CuriosApi.getCuriosHandler(entity).orElse(null);
        if (handler == null) return;
        for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
            String identifier = entry.getKey();
            ICurioStacksHandler slot = entry.getValue();
            IDynamicStackHandler equipped = slot.getStacks();
            IDynamicStackHandler cosmetics = slot.getCosmeticStacks();
            for (int index = 0; index < equipped.getSlots(); index++) {
                if (!Boolean.TRUE.equals(slot.getRenders().get(index))) continue;
                ItemStack stack = slot.hasCosmetic() ? cosmetics.getStackInSlot(index) : null;
                if (stack == null) stack = equipped.getStackInSlot(index);
                ICurio curio = CuriosApi.getCurio(stack).orElse(null);
                if (curio != null && curio.canRender(identifier, index, entity)) {
                    curio.render(identifier, index, entity, partialTick);
                }
            }
        }
    }
}
