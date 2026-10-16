package top.theillusivec4.curios.client;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import net.minecraft.CompressedStreamTools;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.EntityPlayer;
import net.minecraft.GuiButton;
import net.minecraft.GuiInventory;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet250CustomPayload;
import net.minecraft.ResourceLocation;
import net.minecraft.RenderItem;
import net.minecraft.Tessellator;
import top.theillusivec4.curios.CuriosNetwork;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Vanilla inventory with the Curios panel attached to its left side. */
public final class GuiCurios extends GuiInventory {
    private static final int PANEL_WIDTH = 33;
    private static final int PANEL_SLOT_X = 8;
    private static final int PANEL_TOP = 8;
    private static final int SLOT_SIZE = 18;
    private static final ResourceLocation PANEL_TEXTURE =
        new ResourceLocation("curios", "textures/gui/curios/inventory.png");
    private static final ResourceLocation COSMETIC_SLOT_ICON =
        new ResourceLocation("curios", "textures/slot/empty_cosmetic_slot.png");
    private final RenderItem itemRenderer = new RenderItem();
    private final List<SlotRef> visibleSlots = new ArrayList<SlotRef>();
    private boolean panelClickHandled;
    private int page;
    private boolean cosmeticMode;

    public GuiCurios(EntityPlayer player) { super(player); }

    @Override public void initGui() {
        super.initGui();
        for (java.util.Iterator<Object> iterator = buttonList.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if (object instanceof GuiButton && ((GuiButton) object).id == 1846) iterator.remove();
        }
        guiLeft += PANEL_WIDTH;
        buttonList.add(new GuiCuriosButton(1846, guiLeft - PANEL_WIDTH + 28, guiTop + 10));
        rebuildVisibleSlots();
        updatePageButtons();
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 1846) {
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            return;
        }
        if (button.id == 1848) { page = Math.max(0, page - 1); return; }
        if (button.id == 1849) { page = Math.min(pageCount() - 1, page + 1); return; }
        super.actionPerformed(button);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
        refreshVisibleSlots();
        drawCuriosPanel(mouseX, mouseY);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTick) {
        super.drawScreen(mouseX, mouseY, partialTick);
        ItemStack stack = getHoveredCurioStack(mouseX, mouseY);
        if (stack != null && mc.thePlayer != null && mc.thePlayer.inventory.getItemStack() == null) {
            drawItemStackTooltip(stack, mouseX, mouseY);
        }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int panelX = guiLeft - PANEL_WIDTH;
        if (mouseButton == 0 && hasCosmeticSlots() && mouseX >= guiLeft - 27 && mouseX < guiLeft - 7
            && mouseY >= guiTop - 18 && mouseY < guiTop - 1) {
            cosmeticMode = !cosmeticMode;
            page = 0;
            rebuildVisibleSlots();
            updatePageButtons();
            return;
        }
        if (mouseX >= panelX + PANEL_SLOT_X && mouseX < panelX + PANEL_SLOT_X + SLOT_SIZE
            && mouseY >= guiTop + PANEL_TOP + (pageCount() > 1 ? 8 : 0)
            && mouseY < guiTop + PANEL_TOP + (pageCount() > 1 ? 8 : 0) + slotRows() * SLOT_SIZE) {
            panelClickHandled = mouseButton == 0;
            if (mouseButton == 0) {
                SlotRef slot = getPanelSlot(mouseY);
                EntityClientPlayerMP player = mc.thePlayer;
                ItemStack carried = player == null ? null : player.inventory.getItemStack();
                if (slot != null && carried == null) sendAction(null, slot.identifier, slot.index, cosmeticMode);
                else placeCarriedItem(slot, player, carried);
            }
            return;
        }
        panelClickHandled = false;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (panelClickHandled) {
                panelClickHandled = false;
                return;
            }
            if (isPanelSlot(mouseX, mouseY)) {
                EntityClientPlayerMP player = mc.thePlayer;
                ItemStack carried = player == null ? null : player.inventory.getItemStack();
                if (carried != null) {
                    placeCarriedItem(getPanelSlot(mouseY), player, carried);
                    return;
                }
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
    }

    private boolean isPanelSlot(int mouseX, int mouseY) {
        int panelX = guiLeft - PANEL_WIDTH;
        int slotTop = guiTop + PANEL_TOP + (pageCount() > 1 ? 8 : 0);
        return mouseX >= panelX + PANEL_SLOT_X && mouseX < panelX + PANEL_SLOT_X + SLOT_SIZE
            && mouseY >= slotTop && mouseY < slotTop + slotRows() * SLOT_SIZE;
    }

    private SlotRef getPanelSlot(int mouseY) {
        int slotTop = guiTop + PANEL_TOP + (pageCount() > 1 ? 8 : 0);
        int index = page * pageSize() + (mouseY - slotTop) / SLOT_SIZE;
        return index >= 0 && index < visibleSlots.size() ? visibleSlots.get(index) : null;
    }

    private void placeCarriedItem(SlotRef slot, EntityClientPlayerMP player, ItemStack carried) {
        if (slot == null || player == null || carried == null) return;
        if (cosmeticMode || CuriosApi.getCurio(carried).isPresent()) {
            sendAction(carried, slot.identifier, slot.index, cosmeticMode);
            consumeCarriedItem(player, carried);
        }
    }

    private void drawCuriosPanel(int mouseX, int mouseY) {
        int panelX = guiLeft - PANEL_WIDTH;
        boolean pageOffset = pageCount() > 1;
        int upperHeight = 7 + slotRows() * SLOT_SIZE + (pageOffset ? 8 : 0);
        mc.getTextureManager().bindTexture(PANEL_TEXTURE);
        GL11.glColor4f(1, 1, 1, 1);
        drawTexturedModalRect(panelX, guiTop, 91, 0, 25, upperHeight);
        drawTexturedModalRect(panelX + 7, guiTop, 98, 0, 25, upperHeight);
        drawTexturedModalRect(panelX, guiTop + upperHeight, 91, 159, 25, 7);
        drawTexturedModalRect(panelX + 7, guiTop + upperHeight, 98, 159, 25, 7);
        drawTexturedModalRect(guiLeft - 26, guiTop + 7 + (pageOffset ? 8 : 0),
            7, 7, 18, slotRows() * SLOT_SIZE);

        ICuriosItemHandler handler = getHandler();
        if (handler == null) return;
        if (hasCosmeticSlots(handler)) {
            mc.getTextureManager().bindTexture(PANEL_TEXTURE);
            drawTexturedModalRect(guiLeft - 31, guiTop - 23, 32, 0, 28, 24);
            boolean hovered = mouseX >= guiLeft - 27 && mouseX < guiLeft - 7
                && mouseY >= guiTop - 18 && mouseY < guiTop - 1;
            String cosmeticName = cosmeticMode ? "cosmetic_on" : "cosmetic_off";
            if (hovered) cosmeticName += "_highlighted";
            ResourceLocation cosmeticTexture = new ResourceLocation("curios", "textures/gui/curios/"
                + cosmeticName + ".png");
            drawSmallTexture(cosmeticTexture, guiLeft - 27, guiTop - 18, 20, 17);
        }
        int start = page * pageSize();
        int end = Math.min(visibleSlots.size(), start + pageSize());
        int slotTop = guiTop + PANEL_TOP + (pageOffset ? 8 : 0);
        for (int listIndex = start; listIndex < end; listIndex++) {
            int row = listIndex - start;
            SlotRef ref = visibleSlots.get(listIndex);
            ICurioStacksHandler stacks = handler.getStacksHandler(ref.identifier).orElse(null);
            ItemStack stack = stacks == null ? null : (cosmeticMode
                ? stacks.getCosmeticStacks().getStackInSlot(ref.index)
                : stacks.getStacks().getStackInSlot(ref.index));
            int x = panelX + PANEL_SLOT_X;
            int y = slotTop + row * SLOT_SIZE;
            if (stack != null) itemRenderer.renderItemAndEffectIntoGUI(fontRenderer,
                mc.getTextureManager(), stack, x, y);
            else {
                ResourceLocation icon = cosmeticMode ? COSMETIC_SLOT_ICON
                    : CuriosApi.getSlotType(ref.identifier).map(type -> type.getIcon()).orElse(null);
                drawSlotIcon(icon, x, y);
            }
        }
    }

    private void drawSlotIcon(ResourceLocation icon, int x, int y) {
        if (icon == null) return;
        drawSmallTexture(icon, x, y, 16, 16);
    }

    private void drawSmallTexture(ResourceLocation texture, int x, int y, int width, int height) {
        mc.getTextureManager().bindTexture(texture);
        GL11.glColor4f(1, 1, 1, 1);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, zLevel, 0, 1);
        tessellator.addVertexWithUV(x + width, y + height, zLevel, 1, 1);
        tessellator.addVertexWithUV(x + width, y, zLevel, 1, 0);
        tessellator.addVertexWithUV(x, y, zLevel, 0, 0);
        tessellator.draw();
    }

    private ItemStack getHoveredCurioStack(int mouseX, int mouseY) {
        int panelX = guiLeft - PANEL_WIDTH;
        int slotTop = guiTop + PANEL_TOP + (pageCount() > 1 ? 8 : 0);
        if (mouseX < panelX || mouseX >= guiLeft || mouseY < slotTop
            || mouseY >= slotTop + slotRows() * SLOT_SIZE) return null;
        int index = page * pageSize() + (mouseY - slotTop) / SLOT_SIZE;
        if (index < 0 || index >= visibleSlots.size()) return null;
        SlotRef ref = visibleSlots.get(index);
        ICuriosItemHandler handler = getHandler();
        if (handler == null) return null;
        ICurioStacksHandler stacks = handler.getStacksHandler(ref.identifier).orElse(null);
        if (stacks == null) return null;
        return cosmeticMode ? stacks.getCosmeticStacks().getStackInSlot(ref.index)
            : stacks.getStacks().getStackInSlot(ref.index);
    }

    private void rebuildVisibleSlots() {
        visibleSlots.clear();
        EntityClientPlayerMP player = mc.thePlayer;
        ICuriosItemHandler handler = getHandler();
        if (handler == null) return;
        if (cosmeticMode && !hasCosmeticSlots(handler)) cosmeticMode = false;
        for (ICurioStacksHandler type : handler.getCurios().values()) {
            if (!type.isVisible() || (cosmeticMode && !type.hasCosmetic())) continue;
            for (int index = 0; index < type.getSlots(); index++) {
                visibleSlots.add(new SlotRef(type.getIdentifier(), index));
            }
        }
        page = Math.max(0, Math.min(page, pageCount() - 1));
    }

    private void refreshVisibleSlots() {
        rebuildVisibleSlots();
        updatePageButtons();
    }

    private ICuriosItemHandler getHandler() {
        EntityClientPlayerMP player = mc.thePlayer;
        return player == null ? null : CuriosApi.getCuriosHandler(player).orElse(null);
    }

    private boolean hasCosmeticSlots() { return hasCosmeticSlots(getHandler()); }

    private boolean hasCosmeticSlots(ICuriosItemHandler handler) {
        if (handler == null) return false;
        for (ICurioStacksHandler type : handler.getCurios().values()) {
            if (type.isVisible() && type.hasCosmetic()) return true;
        }
        return false;
    }

    private int pageSize() { return 8; }
    private int pageCount() { return Math.max(1, (visibleSlots.size() + pageSize() - 1) / pageSize()); }
    private int slotRows() {
        return Math.max(1, Math.min(pageSize(), visibleSlots.size() - page * pageSize()));
    }

    private void updatePageButtons() {
        GuiCuriosPageButton previous = null;
        GuiCuriosPageButton next = null;
        for (Object object : buttonList) {
            if (object instanceof GuiCuriosPageButton) {
                GuiCuriosPageButton button = (GuiCuriosPageButton) object;
                if (button.id == 1848) previous = button;
                else if (button.id == 1849) next = button;
            }
        }
        if (pageCount() > 1) {
            if (previous == null) {
                previous = new GuiCuriosPageButton(1848, guiLeft - 28, guiTop + 2, false);
                buttonList.add(previous);
            }
            if (next == null) {
                next = new GuiCuriosPageButton(1849, guiLeft - 17, guiTop + 2, true);
                buttonList.add(next);
            }
            previous.enabled = page > 0;
            next.enabled = page + 1 < pageCount();
        } else {
            for (java.util.Iterator<Object> iterator = buttonList.iterator(); iterator.hasNext();) {
                Object object = iterator.next();
                if (object instanceof GuiCuriosPageButton) iterator.remove();
            }
        }
    }

    private void sendAction(ItemStack carried, String identifier, int index, boolean cosmetic) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.sendQueue == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Source", carried == null ? -1 : -2);
        payload.setString("Identifier", identifier);
        payload.setInteger("Index", index);
        payload.setBoolean("Cosmetic", cosmetic);
        if (carried != null) {
            payload.setBoolean("Carried", true);
            payload.setTag("Stack", carried.writeToNBT(new NBTTagCompound()));
        }
        try {
            player.sendQueue.addToSendQueue(new Packet250CustomPayload(CuriosNetwork.CHANNEL,
                CompressedStreamTools.compress(payload)));
        } catch (Exception exception) {
            top.theillusivec4.curios.Curios.LOGGER.error("Failed to send Curios slot action", exception);
        }
    }

    private void consumeCarriedItem(EntityClientPlayerMP player, ItemStack carried) {
        if (carried.stackSize <= 1) player.inventory.setItemStack(null);
        else {
            carried.stackSize--;
            player.inventory.setItemStack(carried);
        }
    }

    private static final class SlotRef {
        private final String identifier;
        private final int index;
        private SlotRef(String identifier, int index) { this.identifier = identifier; this.index = index; }
    }
}
