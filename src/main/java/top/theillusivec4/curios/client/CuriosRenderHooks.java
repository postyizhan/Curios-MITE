package top.theillusivec4.curios.client;

import java.util.Map;
import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import net.minecraft.ModelBiped;
import net.minecraft.ModelRenderer;
import org.lwjgl.opengl.GL11;
import top.theillusivec4.curios.api.client.CuriosClientApi;
import top.theillusivec4.curios.api.client.ICurioRenderer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosRenderHooks {
    private CuriosRenderHooks() {}

    public static void render(EntityLivingBase entity, ModelBiped model, float partialTick) {
        if (model == null) return;
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
                if (stack == null) continue;
                ModelRenderer anchor = getAnchor(model, identifier, index);
                if (anchor == null) continue;
                ICurioRenderer renderer = CuriosClientApi.getRenderer(stack);
                boolean renderCurio = curio != null && curio.canRender(identifier, index, entity);
                if (renderer == null && !renderCurio) continue;

                GL11.glPushMatrix();
                try {
                    anchor.postRender(0.0625F);
                    if (renderer != null) {
                        renderer.render(stack, identifier, index, entity, model, anchor, partialTick);
                    }
                    if (renderCurio) curio.render(identifier, index, entity, partialTick);
                } finally {
                    GL11.glPopMatrix();
                }
            }
        }
    }

    private static ModelRenderer getAnchor(ModelBiped model, String identifier, int index) {
        switch (identifier) {
            case "head":
            case "necklace":
            case "charm":
                return model.bipedHead;
            case "back":
                return model.bipedCloak == null ? model.bipedBody : model.bipedCloak;
            case "hands":
            case "ring":
            case "bracelet":
                return (index & 1) == 0 ? model.bipedRightArm : model.bipedLeftArm;
            case "feet":
                return (index & 1) == 0 ? model.bipedRightLeg : model.bipedLeftLeg;
            case "body":
            case "belt":
            default:
                return model.bipedBody;
        }
    }
}
