package top.theillusivec4.curios.mixin;

import net.minecraft.ChatAllowedCharacters;
import net.minecraft.FontRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {
    @Shadow protected abstract float renderUnicodeChar(char character, boolean italic);
    @Shadow private int[] charWidth;
    @Shadow private byte[] glyphWidth;
    @Shadow private boolean unicodeFlag;
    @Shadow public abstract int getCharWidth(char character);

    @Inject(method = "renderCharAtPos(ICZ)F", at = @At("HEAD"), cancellable = true)
    private void curios$renderExtendedCharacter(int characterIndex, char character, boolean italic,
                                               CallbackInfoReturnable<Float> cir) {
        if (!unicodeFlag && characterIndex >= charWidth.length - 32) {
            cir.setReturnValue(renderUnicodeChar(character, italic));
        }
    }

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
        if (unicodeFlag) return;

        int allowedCharacterIndex = ChatAllowedCharacters.allowedCharacters.indexOf(character);
        if (allowedCharacterIndex < charWidth.length - 32) return;

        // MITE's extended allowed-character list is much larger than the base font array.
        // Resolve its width here so vanilla never indexes charWidth with the extended index.
        if (character >= glyphWidth.length) {
            cir.setReturnValue(8);
            return;
        }

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

    @Redirect(method = "renderStringAtPos(Ljava/lang/String;Z)V",
              at = @At(value = "FIELD", target = "Lnet/minecraft/FontRenderer;charWidth:[I",
                  opcode = Opcodes.GETFIELD, args = "array=get"), require = 2)
    private int curios$readAllowedCharacterWidth(int[] widths, int index) {
        if (widths == charWidth && (index < 0 || index >= widths.length)) {
            int allowedCharacterIndex = index - 32;
            if (allowedCharacterIndex >= 0
                && allowedCharacterIndex < ChatAllowedCharacters.allowedCharacters.length()) {
                return getCharWidth(ChatAllowedCharacters.allowedCharacters.charAt(allowedCharacterIndex));
            }
        }
        return widths[index];
    }
}
