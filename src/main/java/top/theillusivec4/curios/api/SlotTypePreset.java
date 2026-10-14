package top.theillusivec4.curios.api;

/** Common slot names from the legacy Curios API. */
public enum SlotTypePreset {
    HEAD("head"),
    NECKLACE("necklace"),
    BACK("back"),
    BODY("body"),
    HANDS("hands"),
    RING("ring"),
    CHARM("charm");

    private final String identifier;

    SlotTypePreset(String identifier) { this.identifier = identifier; }

    public String getIdentifier() { return identifier; }
    public SlotTypeMessage.Builder getMessageBuilder() { return new SlotTypeMessage.Builder(identifier); }
}
