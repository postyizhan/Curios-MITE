package top.theillusivec4.curios.api.client;

import net.minecraft.EntityLivingBase;
import net.minecraft.ItemStack;
import net.minecraft.ModelBiped;
import net.minecraft.ModelRenderer;

/** Draws a curio relative to the body part associated with its slot type. */
public interface ICurioRenderer {
    void render(ItemStack stack, String identifier, int index, EntityLivingBase entity,
                ModelBiped model, ModelRenderer anchor, float partialTick);
}
