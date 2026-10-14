package top.theillusivec4.curios.api.type.inventory;

public interface ICurioStacksHandler {
    String getIdentifier();
    IDynamicStackHandler getStacks();
    IDynamicStackHandler getCosmeticStacks();
    int getSlots();
    int getSizeShift();
    boolean isVisible();
    boolean hasCosmetic();
}
