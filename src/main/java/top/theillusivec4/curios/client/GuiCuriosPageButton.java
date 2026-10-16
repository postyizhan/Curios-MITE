package top.theillusivec4.curios.client;

import net.minecraft.GuiButton;
import net.minecraft.Minecraft;
import net.minecraft.ResourceLocation;

/** Textured page control matching the legacy Curios inventory sheet. */
public final class GuiCuriosPageButton extends GuiButton {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("curios", "textures/gui/curios/inventory.png");
    private final boolean next;

    public GuiCuriosPageButton(int id, int x, int y, boolean next) {
        super(id, x, y, 11, 12, "");
        this.next = next;
    }

    @Override public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!drawButton) return;
        boolean hovered = enabled && mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width && mouseY < yPosition + height;
        int textureX = next ? 43 : 32;
        int textureY = enabled ? 25 : 37;
        if (hovered) textureX += 22;
        minecraft.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(xPosition, yPosition, textureX, textureY, width, height);
    }
}
