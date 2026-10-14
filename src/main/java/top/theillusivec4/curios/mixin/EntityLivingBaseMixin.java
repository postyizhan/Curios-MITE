package top.theillusivec4.curios.mixin;

import net.minecraft.EntityLivingBase;
import net.minecraft.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.common.CuriosInventory;

/** Runs equipped curio callbacks from the entity lifecycle on both logical sides. */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin {
    @Inject(method = "onLivingUpdate", at = @At("TAIL"))
    private void curios$onLivingUpdate(CallbackInfo info) {
        CuriosInventory.get((EntityLivingBase) (Object) this).tick();
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void curios$writeToNBT(NBTTagCompound compound, CallbackInfo info) {
        compound.setTag(CuriosConstants.INVENTORY_NBT, CuriosInventory.get((EntityLivingBase) (Object) this).writeToNBT());
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void curios$readFromNBT(NBTTagCompound compound, CallbackInfo info) {
        if (compound.hasKey(CuriosConstants.INVENTORY_NBT)) {
            CuriosInventory.get((EntityLivingBase) (Object) this).readFromNBT(compound.getCompoundTag(CuriosConstants.INVENTORY_NBT));
        }
    }
}
