package top.theillusivec4.curios.api.type;

import net.minecraft.ResourceLocation;

public interface ISlotType extends Comparable<ISlotType> {
    String getIdentifier();
    int getSize();
    default ResourceLocation getIcon() { return null; }
    default int getPriority() { return 0; }
    default boolean isLocked() { return false; }
    default boolean isVisible() { return true; }
    default boolean hasCosmetic() { return false; }

    @Override default int compareTo(ISlotType other) {
        int priorityOrder = Integer.compare(getPriority(), other.getPriority());
        return priorityOrder != 0 ? priorityOrder : getIdentifier().compareTo(other.getIdentifier());
    }
}
