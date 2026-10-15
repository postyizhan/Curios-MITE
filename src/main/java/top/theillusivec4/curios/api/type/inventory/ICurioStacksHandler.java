package top.theillusivec4.curios.api.type.inventory;

import java.util.List;

public interface ICurioStacksHandler {
    String getIdentifier();
    IDynamicStackHandler getStacks();
    IDynamicStackHandler getCosmeticStacks();
    List<Boolean> getRenders();
    void setRender(int slot, boolean render);
    int getSlots();
    int getSizeShift();
    boolean isVisible();
    boolean hasCosmetic();
}
