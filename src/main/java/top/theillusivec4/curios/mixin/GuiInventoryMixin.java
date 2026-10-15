package top.theillusivec4.curios.mixin;

import net.minecraft.GuiButton;
import net.minecraft.GuiInventory;
import net.minecraft.GuiScreen;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.client.GuiCurios;

@Mixin(GuiInventory.class)
public abstract class GuiInventoryMixin extends GuiScreen {
    @Inject(method = "initGui", at = @At("TAIL"))
    private void curios$addButton(CallbackInfo info) {
        buttonList.add(new GuiButton(1846, width / 2 + 92, 5, 72, 20, "饰品"));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void curios$openScreen(GuiButton button, CallbackInfo info) {
        if (button.id != 1846) return;
        Minecraft.getMinecraft().displayGuiScreen(new GuiCurios((GuiScreen) (Object) this));
        info.cancel();
    }
}
