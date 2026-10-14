package top.theillusivec4.curios.api.type.inventory;

public interface ICurioStacksHandler {
    String getIdentifier();
    IDynamicStackHandler getStacks();
    IDynamicStackHandler getCosmeticStacks();
}
