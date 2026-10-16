package top.theillusivec4.curios.client;

import net.minecraft.GuiButton;
import net.minecraft.Minecraft;
import net.minecraft.ResourceLocation;
import net.minecraft.Tessellator;
import org.lwjgl.opengl.GL11;

/** Small textured entry button used by the vanilla inventory screen. */
public final class GuiCuriosButton extends GuiButton {
    private static final ResourceLocation NORMAL =
        new ResourceLocation("curios", "textures/gui/curios/button.png");
    private static final ResourceLocation HIGHLIGHTED =
        new ResourceLocation("curios", "textures/gui/curios/button_highlighted.png");

    public GuiCuriosButton(int id, int x, int y) { super(id, x, y, 10, 10, ""); }

    @Override public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!drawButton) return;
        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width && mouseY < yPosition + height;
        ResourceLocation texture = hovered ? HIGHLIGHTED : NORMAL;
        minecraft.getTextureManager().bindTexture(texture);
        GL11.glColor4f(1, 1, 1, 1);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(xPosition, yPosition + height, zLevel, 0, 1);
        tessellator.addVertexWithUV(xPosition + width, yPosition + height, zLevel, 1, 1);
        tessellator.addVertexWithUV(xPosition + width, yPosition, zLevel, 1, 0);
        tessellator.addVertexWithUV(xPosition, yPosition, zLevel, 0, 0);
        tessellator.draw();
    }
}
