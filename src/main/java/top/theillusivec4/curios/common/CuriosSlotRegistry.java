package top.theillusivec4.curios.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import top.theillusivec4.curios.api.type.ISlotType;

public final class CuriosSlotRegistry {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+(:[a-z0-9/._-]+)?");
    private static final Map<String, SlotType> SLOT_TYPES = new LinkedHashMap<String, SlotType>();
    private CuriosSlotRegistry() {}

    public static synchronized ISlotType register(String identifier, int size) {
        if (identifier == null || !VALID_ID.matcher(identifier).matches()) throw new IllegalArgumentException("Invalid Curios slot identifier: " + identifier);
        if (size < 1 || size > 256) throw new IllegalArgumentException("Curios slot size must be between 1 and 256: " + size);
        SlotType current = SLOT_TYPES.get(identifier);
        if (current == null || size > current.getSize()) { current = new SlotType(identifier, size); SLOT_TYPES.put(identifier, current); }
        return current;
    }

    public static synchronized Optional<ISlotType> get(String identifier) { return Optional.<ISlotType>ofNullable(SLOT_TYPES.get(identifier)); }
    public static synchronized Map<String, ISlotType> getAll() {
        Map<String, ISlotType> result = new LinkedHashMap<String, ISlotType>();
        result.putAll(SLOT_TYPES);
        return Collections.unmodifiableMap(result);
    }

    private static final class SlotType implements ISlotType {
        private final String identifier; private final int size;
        private SlotType(String identifier, int size) { this.identifier = identifier; this.size = size; }
        @Override public String getIdentifier() { return identifier; }
        @Override public int getSize() { return size; }
    }
}
