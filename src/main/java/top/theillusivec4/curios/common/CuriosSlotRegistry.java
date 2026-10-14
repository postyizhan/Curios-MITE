package top.theillusivec4.curios.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.ResourceLocation;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.type.ISlotType;

public final class CuriosSlotRegistry {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+(:[a-z0-9/._-]+)?");
    private static final Map<String, SlotType> SLOT_TYPES = new LinkedHashMap<String, SlotType>();
    private CuriosSlotRegistry() {}

    public static synchronized ISlotType register(String identifier, int size) {
        return register(new SlotTypeMessage.Builder(identifier).size(size).build());
    }

    public static synchronized ISlotType register(SlotTypeMessage message) {
        if (message == null) throw new IllegalArgumentException("Slot type message cannot be null");
        String identifier = message.getIdentifier();
        int size = message.getSize();
        if (identifier == null || !VALID_ID.matcher(identifier).matches()) throw new IllegalArgumentException("Invalid Curios slot identifier: " + identifier);
        if (size < 1 || size > 256) throw new IllegalArgumentException("Curios slot size must be between 1 and 256: " + size);
        SlotType current = SLOT_TYPES.get(identifier);
        if (current == null) {
            current = new SlotType(message);
            SLOT_TYPES.put(identifier, current);
        } else {
            current.merge(message);
        }
        return current;
    }

    public static synchronized Optional<ISlotType> get(String identifier) { return Optional.<ISlotType>ofNullable(SLOT_TYPES.get(identifier)); }
    public static synchronized Map<String, ISlotType> getAll() {
        Map<String, ISlotType> result = new LinkedHashMap<String, ISlotType>();
        List<SlotType> ordered = new ArrayList<SlotType>(SLOT_TYPES.values());
        ordered.sort(Comparator.comparingInt(SlotType::getPriority).thenComparing(SlotType::getIdentifier));
        for (SlotType type : ordered) result.put(type.getIdentifier(), type);
        return Collections.unmodifiableMap(result);
    }

    private static final class SlotType implements ISlotType {
        private final String identifier;
        private int size;
        private ResourceLocation icon;
        private int priority;
        private boolean locked;
        private boolean visible;
        private boolean cosmetic;

        private SlotType(SlotTypeMessage message) {
            this.identifier = message.getIdentifier();
            this.size = message.getSize();
            this.icon = message.getIcon();
            this.priority = message.getPriority() == null ? 0 : message.getPriority();
            this.locked = message.isLocked();
            this.visible = message.isVisible();
            this.cosmetic = message.hasCosmetic();
        }

        private void merge(SlotTypeMessage message) {
            size = Math.max(size, message.getSize());
            if (message.getIcon() != null) icon = message.getIcon();
            if (message.getPriority() != null) priority = message.getPriority();
            locked |= message.isLocked();
            visible &= message.isVisible();
            cosmetic |= message.hasCosmetic();
        }

        @Override public String getIdentifier() { return identifier; }
        @Override public int getSize() { return size; }
        @Override public ResourceLocation getIcon() { return icon; }
        @Override public int getPriority() { return priority; }
        @Override public boolean isLocked() { return locked; }
        @Override public boolean isVisible() { return visible; }
        @Override public boolean hasCosmetic() { return cosmetic; }
    }
}
