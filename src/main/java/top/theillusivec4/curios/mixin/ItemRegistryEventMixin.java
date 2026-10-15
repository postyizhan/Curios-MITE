package top.theillusivec4.curios.mixin;

import net.minecraft.CreativeTabs;
import net.minecraft.Item;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(ItemRegistryEvent.class)
public abstract class ItemRegistryEventMixin {
    @Inject(method = "register(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/Item;Lnet/minecraft/CreativeTabs;)Lnet/minecraft/Item;", at = @At("RETURN"))
    private void curios$recordItemId(String namespace, String path, String name, Item item, CreativeTabs tab,
                                     CallbackInfoReturnable<Item> cir) {
        CuriosApi.registerItemIdentifier(cir.getReturnValue(), namespace, path);
    }

    @Inject(method = "register(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/Item;Lnet/minecraft/CreativeTabs;)Lnet/minecraft/Item;", at = @At("RETURN"))
    private void curios$recordItemId(String namespace, String path, Item item, CreativeTabs tab,
                                     CallbackInfoReturnable<Item> cir) {
        CuriosApi.registerItemIdentifier(cir.getReturnValue(), namespace, path);
    }

    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/Item;Lnet/minecraft/CreativeTabs;)Lnet/minecraft/Item;", at = @At("RETURN"))
    private void curios$recordItemId(String namespace, Item item, CreativeTabs tab,
                                     CallbackInfoReturnable<Item> cir) {
        CuriosApi.registerItemIdentifier(cir.getReturnValue(), namespace, item.getUnlocalizedName());
    }

    @Inject(method = "register(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/Item;)Lnet/minecraft/Item;", at = @At("RETURN"))
    private void curios$recordItemId(String namespace, String path, Item item,
                                     CallbackInfoReturnable<Item> cir) {
        CuriosApi.registerItemIdentifier(cir.getReturnValue(), namespace, path);
    }

    @Inject(method = "register(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/Item;)Lnet/minecraft/Item;", at = @At("RETURN"))
    private void curios$recordItemId(String namespace, String path, String name, Item item,
                                     CallbackInfoReturnable<Item> cir) {
        CuriosApi.registerItemIdentifier(cir.getReturnValue(), namespace, path);
    }

    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/Item;)Lnet/minecraft/Item;", at = @At("RETURN"))
    private void curios$recordItemId(String namespace, Item item, CallbackInfoReturnable<Item> cir) {
        CuriosApi.registerItemIdentifier(cir.getReturnValue(), namespace, item.getUnlocalizedName());
    }
}
