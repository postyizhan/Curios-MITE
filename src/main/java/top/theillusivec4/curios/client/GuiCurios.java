package top.theillusivec4.curios.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.CompressedStreamTools;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet250CustomPayload;
import top.theillusivec4.curios.CuriosNetwork;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

/** Compact inventory page for equipping and removing Curios. */
public final class GuiCurios extends GuiScreen {
    private static final int SLOT_PAGE_SIZE = 18;
    private final List<SlotRef> visibleSlots = new ArrayList<SlotRef>();
    private int selectedSource = -1;
    private int slotPage;

    @Override public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 28, 90, 20, "上一页"));
        buttonList.add(new GuiButton(1, width / 2 + 10, height - 28, 90, 20, "下一页"));
        buttonList.add(new GuiButton(2, width - 88, height - 28, 80, 20, "返回"));
        rebuildVisibleSlots();
    }

    private void rebuildVisibleSlots() {
        visibleSlots.clear();
        for (ISlotType type : CuriosApi.getSlotTypes().values()) {
            if (!type.isVisible()) continue;
            for (int index = 0; index < type.getSize(); index++) {
                visibleSlots.add(new SlotRef(type.getIdentifier(), index));
            }
        }
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) slotPage = Math.max(0, slotPage - 1);
        else if (button.id == 1) slotPage = Math.min(Math.max(0, (visibleSlots.size() - 1) / SLOT_PAGE_SIZE), slotPage + 1);
        else if (button.id == 2) mc.displayGuiScreen(null);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseY >= 52 && mouseY < 52 + 18 * 18) {
            int column = (mouseX - 16) / 150;
            int row = (mouseY - 52) / 18;
            if (column >= 0 && column < 2 && mouseX >= 16 + column * 150 && mouseX < 16 + (column + 1) * 150) {
                int inventorySlot = column * 18 + row;
                EntityClientPlayerMP player = mc.thePlayer;
                if (player != null && inventorySlot < player.inventory.mainInventory.length) {
                    ItemStack stack = player.inventory.mainInventory[inventorySlot];
                    if (stack != null && stack.getItem() instanceof top.theillusivec4.curios.api.type.capability.ICurioItem) {
                        selectedSource = selectedSource == inventorySlot ? -1 : inventorySlot;
                    }
                    return;
                }
            }
        }
        if (mouseButton == 0 && mouseX >= width / 2 + 2 && mouseX < width - 12 && mouseY >= 52 && mouseY < 52 + 9 * 34) {
            int column = (mouseX - (width / 2 + 12)) / 150;
            int row = (mouseY - 52) / 34;
            if (column >= 0 && column < 2 && row >= 0 && row < 9) {
                int listIndex = slotPage * SLOT_PAGE_SIZE + row * 2 + column;
                if (listIndex < visibleSlots.size()) {
                    SlotRef slot = visibleSlots.get(listIndex);
                    sendAction(selectedSource, slot.identifier, slot.index);
                    selectedSource = -1;
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "饰品", width / 2, 12, 0xFFFFFF);
        drawString(fontRenderer, "选择背包中的饰品，再选择目标槽位；空手选择槽位可卸下", 16, 32, 0xD0D0D0);
        drawString(fontRenderer, "背包物品", 16, 40, 0xFFFFFF);
        drawString(fontRenderer, "已注册槽位", width / 2 + 12, 40, 0xFFFFFF);
        drawSources(mouseX, mouseY);
        drawSlots(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    private void drawSources(int mouseX, int mouseY) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 18; row++) {
                int index = column * 18 + row;
                if (index >= player.inventory.mainInventory.length) continue;
                ItemStack stack = player.inventory.mainInventory[index];
                if (stack == null || !(stack.getItem() instanceof top.theillusivec4.curios.api.type.capability.ICurioItem)) continue;
                int x = 16 + column * 150;
                int y = 52 + row * 18;
                int color = selectedSource == index ? 0xFFFF55 : 0xFFFFFF;
                drawString(fontRenderer, index + ": " + trim(stack.getDisplayName(), 16), x, y, color);
            }
        }
    }

    private void drawSlots(int mouseX, int mouseY) {
        EntityClientPlayerMP player = mc.thePlayer;
        ICuriosItemHandler handler = player == null ? null : CuriosApi.getCuriosHandler(player).orElse(null);
        int start = slotPage * SLOT_PAGE_SIZE;
        int end = Math.min(visibleSlots.size(), start + SLOT_PAGE_SIZE);
        for (int listIndex = start; listIndex < end; listIndex++) {
            int local = listIndex - start;
            int column = local % 2;
            int row = local / 2;
            int x = width / 2 + 12 + column * 150;
            int y = 52 + row * 34;
            SlotRef slot = visibleSlots.get(listIndex);
            ItemStack stack = handler == null ? null : handler.getEquippedStack(slot.identifier, slot.index);
            String label = slot.identifier + " " + (slot.index + 1) + ": " + (stack == null ? "空" : trim(stack.getDisplayName(), 12));
            int color = mouseX >= x - 3 && mouseX < x + 145 && mouseY >= y - 3 && mouseY < y + 20 ? 0xFFFF55 : 0xFFFFFF;
            drawString(fontRenderer, trim(label, 24), x, y, color);
        }
        if (visibleSlots.isEmpty()) drawString(fontRenderer, "暂无已注册槽位", width / 2 + 12, 56, 0xAAAAAA);
        drawCenteredString(fontRenderer, (slotPage + 1) + " / " + Math.max(1, (visibleSlots.size() + SLOT_PAGE_SIZE - 1) / SLOT_PAGE_SIZE), width / 2, height - 44, 0xAAAAAA);
    }

    private String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private void sendAction(int sourceSlot, String identifier, int index) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.sendQueue == null) return;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("Source", sourceSlot);
        payload.setString("Identifier", identifier);
        payload.setInteger("Index", index);
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
        private SlotRef(String identifier, int index) { this.identifier = identifier; this.index = index; }
    }
}
