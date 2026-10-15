package top.theillusivec4.curios.mixin;

import net.minecraft.ChatAllowedCharacters;
import net.minecraft.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {
    @Shadow protected abstract float renderUnicodeChar(char character, boolean italic);
    @Shadow private int[] charWidth;
    @Shadow private byte[] glyphWidth;
    @Shadow private boolean unicodeFlag;

    @Inject(method = "renderDefaultChar(IZ)F", at = @At("HEAD"), cancellable = true)
    private void curios$renderExtendedCharacters(int characterIndex, boolean italic,
                                                CallbackInfoReturnable<Float> cir) {
        int allowedCharacterIndex = characterIndex - 32;
        if (!unicodeFlag && characterIndex >= charWidth.length
            && allowedCharacterIndex >= 0
            && allowedCharacterIndex < ChatAllowedCharacters.allowedCharacters.length()) {
            char character = ChatAllowedCharacters.allowedCharacters.charAt(allowedCharacterIndex);
            cir.setReturnValue(renderUnicodeChar(character, italic));
        }
    }

    @Inject(method = "getCharWidth(C)I", at = @At("HEAD"), cancellable = true)
    private void curios$measureExtendedCharacters(char character, CallbackInfoReturnable<Integer> cir) {
        if (unicodeFlag || ChatAllowedCharacters.allowedCharacters.indexOf(character) < charWidth.length - 32) return;

        int packedWidth = glyphWidth[character] & 255;
        if (packedWidth == 0) {
            cir.setReturnValue(0);
            return;
        }

        int left = packedWidth >>> 4;
        int right = packedWidth & 15;
        if (right > 7) {
            right = 15;
            left = 0;
        }
        cir.setReturnValue((right + 1 - left) / 2 + 1);
    }
}
