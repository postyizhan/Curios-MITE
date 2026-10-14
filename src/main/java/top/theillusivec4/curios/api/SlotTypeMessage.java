package top.theillusivec4.curios.api;

/** Immutable slot registration request for mods migrating from Curios IMC. */
public final class SlotTypeMessage {
    public static final String REGISTER_TYPE = "register_type";

    private final String identifier;
    private final int size;

    private SlotTypeMessage(String identifier, int size) {
        this.identifier = identifier;
        this.size = size;
    }

    public String getIdentifier() { return identifier; }
    public int getSize() { return size; }

    public static final class Builder {
        private final String identifier;
        private int size = 1;

        public Builder(String identifier) { this.identifier = identifier; }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public SlotTypeMessage build() { return new SlotTypeMessage(identifier, size); }
    }
}
