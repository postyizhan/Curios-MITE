package top.theillusivec4.curios.mixin;

import net.minecraft.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {
    @Shadow protected abstract float renderDefaultChar(int character, boolean italic);
    @Shadow protected abstract float renderUnicodeChar(char character, boolean italic);

    @Redirect(
        method = "renderCharAtPos",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/FontRenderer;renderDefaultChar(IZ)F")
    )
    private float curios$renderUnicodeCharacters(FontRenderer renderer, int character, boolean italic) {
        if (character > 255) return renderUnicodeChar((char) character, italic);
        return renderDefaultChar(character, italic);
    }
}
