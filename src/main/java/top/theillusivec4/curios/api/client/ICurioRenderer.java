package top.theillusivec4.curios.api.client;

import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import net.minecraft.ModelBiped;
import net.minecraft.ModelRenderer;

/**
 * Draws a curio relative to its slot's body part when a biped model is available.
 * Both model and anchor are null for entities using other model types.
 */
public interface ICurioRenderer {
    void render(ItemStack stack, String identifier, int index, EntityLivingBase entity,
                ModelBiped model, ModelRenderer anchor, float partialTick);
}
