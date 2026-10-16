package top.theillusivec4.curios.api;

import java.util.Optional;
import net.minecraft.ResourceLocation;

/** Common slot names from the legacy Curios API. */
public enum SlotTypePreset {
    HEAD("head", 40),
    NECKLACE("necklace", 60),
    BACK("back", 80),
    BODY("body", 100),
    BRACELET("bracelet", 120),
    HANDS("hands", 140),
    RING("ring", 160),
    BELT("belt", 180),
    CHARM("charm", 200),
    FEET("feet", 220),
    CURIO("curio", 20);

    private final String identifier;
    private final int priority;

    SlotTypePreset(String identifier, int priority) {
        this.identifier = identifier;
        this.priority = priority;
    }

    public String getIdentifier() { return identifier; }
    public SlotTypeMessage.Builder getMessageBuilder() {
        return new SlotTypeMessage.Builder(identifier).priority(priority)
            .icon(new ResourceLocation("curios", "textures/slot/empty_" + identifier + "_slot.png"));
    }

    public static Optional<SlotTypePreset> findPreset(String identifier) {
        if (identifier == null) return Optional.empty();
        try {
            return Optional.of(valueOf(identifier.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
