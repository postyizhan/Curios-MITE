package top.theillusivec4.curios.client;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import net.minecraft.CompressedStreamTools;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet250CustomPayload;
import net.minecraft.ResourceLocation;
import net.minecraft.RenderItem;
import net.minecraft.RenderManager;
import net.minecraft.Tessellator;
import top.theillusivec4.curios.CuriosNetwork;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Compact inventory page for equipping and removing Curios. */
public final class GuiCurios extends GuiScreen {
    private static final ResourceLocation COSMETIC_SLOT_ICON =
        new ResourceLocation("curios", "slot/empty_cosmetic_slot");
    private static final int CONTENT_TOP = 52;
    private static final int CONTENT_BOTTOM_MARGIN = 60;
    private static final int SOURCE_ROW_HEIGHT = 18;
    private static final int SLOT_ROW_HEIGHT = 28;
    private static final int GRID_COLUMN_WIDTH = 150;
    private final RenderItem itemRenderer = new RenderItem();
    private final List<SlotRef> visibleSlots = new ArrayList<SlotRef>();
    private final List<Integer> visibleSources = new ArrayList<Integer>();
    private ItemStack[] sourceSnapshot;
    private int selectedSource = -1;
    private int slotPage;
    private int sourcePage;
    private boolean cosmeticMode;

    @Override public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 28, 90, 20, "上一页"));
        buttonList.add(new GuiButton(1, width / 2 + 10, height - 28, 90, 20, "下一页"));
        buttonList.add(new GuiButton(2, width - 88, height - 28, 80, 20, "返回"));
        buttonList.add(new GuiButton(3, width / 2 - 45, height - 52, 90, 20, "外观: 关闭"));
        buttonList.add(new GuiButton(4, 16, height - 28, 26, 20, "<"));
        buttonList.add(new GuiButton(5, 46, height - 28, 26, 20, ">"));
        rebuildVisibleSlots();
        rebuildVisibleSources();
    }

    private void rebuildVisibleSlots() {
        visibleSlots.clear();
        EntityClientPlayerMP player = mc.thePlayer;
        ICuriosItemHandler handler = player == null ? null : CuriosApi.getCuriosHandler(player).orElse(null);
        if (handler == null) return;
        for (ICurioStacksHandler type : handler.getCurios().values()) {
            if (!type.isVisible() || (cosmeticMode && !type.hasCosmetic())) continue;
            for (int index = 0; index < type.getSlots(); index++) {
                visibleSlots.add(new SlotRef(type.getIdentifier(), index, type.hasCosmetic()));
            }
        }
        slotPage = Math.min(slotPage, pageCount(visibleSlots.size(), getSlotPageSize()) - 1);
    }

    private void refreshVisibleSlotsIfChanged() {
        EntityClientPlayerMP player = mc.thePlayer;
        ICuriosItemHandler handler = player == null ? null : CuriosApi.getCuriosHandler(player).orElse(null);
        List<SlotRef> current = new ArrayList<SlotRef>();
        if (handler != null) {
            for (ICurioStacksHandler type : handler.getCurios().values()) {
                if (!type.isVisible() || (cosmeticMode && !type.hasCosmetic())) continue;
                for (int index = 0; index < type.getSlots(); index++) {
                    current.add(new SlotRef(type.getIdentifier(), index, type.hasCosmetic()));
                }
            }
        }
        if (current.size() == visibleSlots.size()) {
            boolean unchanged = true;
            for (int index = 0; index < current.size(); index++) {
                SlotRef oldSlot = visibleSlots.get(index);
                SlotRef newSlot = current.get(index);
                if (!oldSlot.identifier.equals(newSlot.identifier) || oldSlot.index != newSlot.index
                    || oldSlot.hasCosmetic != newSlot.hasCosmetic) {
                    unchanged = false;
                    break;
                }
            }
            if (unchanged) return;
        }
        visibleSlots.clear();
        visibleSlots.addAll(current);
        slotPage = Math.max(0, Math.min(slotPage,
            pageCount(visibleSlots.size(), getSlotPageSize()) - 1));
    }

    private void rebuildVisibleSources() {
        visibleSources.clear();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) {
            sourceSnapshot = null;
            return;
        }
        sourceSnapshot = new ItemStack[player.inventory.mainInventory.length];
        for (int index = 0; index < player.inventory.mainInventory.length; index++) {
            ItemStack stack = player.inventory.mainInventory[index];
            sourceSnapshot[index] = stack == null ? null : stack.copy();
            if (stack != null && (cosmeticMode || CuriosApi.getCurio(stack).isPresent())) {
                visibleSources.add(index);
            }
        }
        if (selectedSource >= 0 && !visibleSources.contains(selectedSource)) selectedSource = -1;
        sourcePage = Math.min(sourcePage, Math.max(0, pageCount(visibleSources.size(), getSourcePageSize()) - 1));
    }

    private void refreshVisibleSourcesIfChanged() {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;
        ItemStack[] inventory = player.inventory.mainInventory;
        if (sourceSnapshot == null || sourceSnapshot.length != inventory.length) {
            rebuildVisibleSources();
            return;
        }
        for (int index = 0; index < inventory.length; index++) {
            if (!ItemStack.areItemStacksEqual(inventory[index], sourceSnapshot[index])) {
                rebuildVisibleSources();
                return;
            }
        }
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) slotPage = Math.max(0, slotPage - 1);
        else if (button.id == 1) slotPage = Math.min(pageCount(visibleSlots.size(), getSlotPageSize()) - 1, slotPage + 1);
        else if (button.id == 2) mc.displayGuiScreen(null);
        else if (button.id == 3) {
            cosmeticMode = !cosmeticMode;
            slotPage = 0;
            sourcePage = 0;
            selectedSource = -1;
            button.displayString = cosmeticMode ? "外观: 开启" : "外观: 关闭";
            rebuildVisibleSlots();
            rebuildVisibleSources();
        }
        else if (button.id == 4) sourcePage = Math.max(0, sourcePage - 1);
        else if (button.id == 5) sourcePage = Math.min(pageCount(visibleSources.size(), getSourcePageSize()) - 1, sourcePage + 1);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseY >= CONTENT_TOP && mouseY < CONTENT_TOP + getSourceRows() * SOURCE_ROW_HEIGHT) {
            int column = (mouseX - 16) / GRID_COLUMN_WIDTH;
            int row = (mouseY - CONTENT_TOP) / SOURCE_ROW_HEIGHT;
            if (mouseX >= 16 && column >= 0 && column < getSourceColumns() && mouseX < width / 2 - 12) {
                int sourceIndex = sourcePage * getSourcePageSize() + row * getSourceColumns() + column;
                if (sourceIndex < visibleSources.size()) {
                    int inventorySlot = visibleSources.get(sourceIndex);
                    EntityClientPlayerMP player = mc.thePlayer;
                    ItemStack stack = player.inventory.mainInventory[inventorySlot];
                    if (stack != null) selectedSource = selectedSource == inventorySlot ? -1 : inventorySlot;
                }
                return;
            }
        }
        if (mouseButton == 0 && mouseX >= width / 2 + 2 && mouseX < width - 12
            && mouseY >= CONTENT_TOP && mouseY < CONTENT_TOP + getSlotRows() * SLOT_ROW_HEIGHT) {
            int column = (mouseX - (width / 2 + 12)) / GRID_COLUMN_WIDTH;
            int row = (mouseY - CONTENT_TOP) / SLOT_ROW_HEIGHT;
            if (column >= 0 && column < getSlotColumns() && row >= 0 && row < getSlotRows()) {
                int listIndex = slotPage * getSlotPageSize() + row * getSlotColumns() + column;
                if (listIndex < visibleSlots.size()) {
                    SlotRef slot = visibleSlots.get(listIndex);
                    sendAction(selectedSource, slot.identifier, slot.index, cosmeticMode);
                    selectedSource = -1;
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        refreshVisibleSourcesIfChanged();
        refreshVisibleSlotsIfChanged();
        ((GuiButton) buttonList.get(0)).enabled = slotPage > 0;
        ((GuiButton) buttonList.get(1)).enabled = slotPage + 1 < pageCount(visibleSlots.size(), getSlotPageSize());
        ((GuiButton) buttonList.get(4)).enabled = sourcePage > 0;
        ((GuiButton) buttonList.get(5)).enabled = sourcePage + 1 < pageCount(visibleSources.size(), getSourcePageSize());
        drawCenteredString(fontRenderer, "饰品", width / 2, 12, 0xFFFFFF);
        drawString(fontRenderer, cosmeticMode ? "选择物品后点击外观槽，空手选择槽位可卸下" : "选择饰品后点击槽位，空手选择槽位可卸下", 16, 32, 0xD0D0D0);
        drawString(fontRenderer, "背包物品 " + (sourcePage + 1) + "/" + pageCount(visibleSources.size(), getSourcePageSize()), 16, 40, 0xFFFFFF);
        drawString(fontRenderer, "已注册槽位 " + (slotPage + 1) + "/" + pageCount(visibleSlots.size(), getSlotPageSize()), width / 2 + 12, 40, 0xFFFFFF);
        drawSources(mouseX, mouseY);
        drawSlots(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void drawSources(int mouseX, int mouseY) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;
        int start = sourcePage * getSourcePageSize();
        int end = Math.min(visibleSources.size(), start + getSourcePageSize());
        for (int sourceIndex = start; sourceIndex < end; sourceIndex++) {
            int local = sourceIndex - start;
            int column = local % getSourceColumns();
            int row = local / getSourceColumns();
            int inventoryIndex = visibleSources.get(sourceIndex);
            ItemStack stack = player.inventory.mainInventory[inventoryIndex];
            if (stack == null) continue;
            int x = 16 + column * GRID_COLUMN_WIDTH;
            int y = CONTENT_TOP + row * SOURCE_ROW_HEIGHT;
            int color = selectedSource == inventoryIndex ? 0xFFFF55 : 0xFFFFFF;
            itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, RenderManager.instance.renderEngine, stack, x, y - 1);
            drawString(fontRenderer, inventoryIndex + ": " + trim(stack.getDisplayName(), 13), x + 19, y, color);
        }
    }

    private void drawSlots(int mouseX, int mouseY) {
        EntityClientPlayerMP player = mc.thePlayer;
        ICuriosItemHandler handler = player == null ? null : CuriosApi.getCuriosHandler(player).orElse(null);
        int start = slotPage * getSlotPageSize();
        int end = Math.min(visibleSlots.size(), start + getSlotPageSize());
        for (int listIndex = start; listIndex < end; listIndex++) {
            int local = listIndex - start;
            int column = local % getSlotColumns();
            int row = local / getSlotColumns();
            int x = width / 2 + 12 + column * GRID_COLUMN_WIDTH;
            int y = CONTENT_TOP + row * SLOT_ROW_HEIGHT;
            SlotRef slot = visibleSlots.get(listIndex);
            ICurioStacksHandler stacks = handler == null ? null : handler.getStacksHandler(slot.identifier).orElse(null);
            ItemStack stack = stacks == null ? null : (cosmeticMode
                ? stacks.getCosmeticStacks().getStackInSlot(slot.index)
                : stacks.getStacks().getStackInSlot(slot.index));
            if (stack != null) {
                itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, RenderManager.instance.renderEngine, stack, x, y - 2);
            } else {
                ResourceLocation icon = cosmeticMode ? COSMETIC_SLOT_ICON
                    : CuriosApi.getSlotType(slot.identifier).map(type -> type.getIcon()).orElse(null);
                drawSlotIcon(icon, x, y - 2);
            }
            String label = slot.identifier + " " + (slot.index + 1) + ": " + (stack == null ? "空" : trim(stack.getDisplayName(), 9));
            int color = mouseX >= x - 3 && mouseX < x + 145 && mouseY >= y - 3 && mouseY < y + SLOT_ROW_HEIGHT - 4 ? 0xFFFF55 : 0xFFFFFF;
            drawString(fontRenderer, trim(label, 20), x + 19, y, color);
        }
        if (visibleSlots.isEmpty()) drawString(fontRenderer, "暂无已注册槽位", width / 2 + 12, 56, 0xAAAAAA);
    }

    private void drawSlotIcon(ResourceLocation icon, int x, int y) {
        if (icon == null || RenderManager.instance == null || RenderManager.instance.renderEngine == null) return;
        String path = icon.getResourcePath();
        if (!path.startsWith("textures/")) path = "textures/" + path;
        if (!path.endsWith(".png")) path += ".png";
        RenderManager.instance.renderEngine.bindTexture(new ResourceLocation(icon.getResourceDomain(), path));

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + 16, zLevel, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x + 16, y + 16, zLevel, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x + 16, y, zLevel, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x, y, zLevel, 0.0D, 0.0D);
        tessellator.draw();
    }

    private int getSourceRows() { return Math.max(1, (height - CONTENT_TOP - CONTENT_BOTTOM_MARGIN) / SOURCE_ROW_HEIGHT); }
    private int getSlotRows() { return Math.max(1, (height - CONTENT_TOP - CONTENT_BOTTOM_MARGIN) / SLOT_ROW_HEIGHT); }
    private int getSourceColumns() { return Math.max(1, Math.min(2, (width / 2 - 36) / GRID_COLUMN_WIDTH)); }
    private int getSlotColumns() { return Math.max(1, Math.min(2, (width / 2 - 36) / GRID_COLUMN_WIDTH)); }
    private int getSourcePageSize() { return getSourceRows() * getSourceColumns(); }
    private int getSlotPageSize() { return getSlotRows() * getSlotColumns(); }
    private static int pageCount(int size, int pageSize) { return Math.max(1, (size + pageSize - 1) / pageSize); }

    private String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private void sendAction(int sourceSlot, String identifier, int index, boolean cosmetic) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.sendQueue == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Source", sourceSlot);
        payload.setString("Identifier", identifier);
        payload.setInteger("Index", index);
        payload.setBoolean("Cosmetic", cosmetic);
        try {
            player.sendQueue.addToSendQueue(new Packet250CustomPayload(
                CuriosNetwork.CHANNEL, CompressedStreamTools.compress(payload)));
        } catch (Exception exception) {
            top.theillusivec4.curios.Curios.LOGGER.error("Failed to send Curios slot action", exception);
        }
    }

    private static final class SlotRef {
        private final String identifier;
        private final int index;
        private final boolean hasCosmetic;
        private SlotRef(String identifier, int index, boolean hasCosmetic) {
            this.identifier = identifier;
            this.index = index;
            this.hasCosmetic = hasCosmetic;
        }
    }
}
