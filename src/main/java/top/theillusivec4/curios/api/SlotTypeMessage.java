package top.theillusivec4.curios.api;

import net.minecraft.ResourceLocation;

/** Immutable slot registration request for mods migrating from Curios IMC. */
public final class SlotTypeMessage {
    public static final String REGISTER_TYPE = "register_type";
    public static final String MODIFY_TYPE = "modify_type";

    private final String identifier;
    private final Integer priority;
    private final int size;
    private final boolean locked;
    private final boolean visible;
    private final boolean cosmetic;
    private final ResourceLocation icon;

    private SlotTypeMessage(Builder builder) {
        this.identifier = builder.identifier;
        this.priority = builder.priority;
        this.size = builder.size;
        this.locked = builder.locked;
        this.visible = builder.visible;
        this.cosmetic = builder.cosmetic;
        this.icon = builder.icon;
    }

    public String getIdentifier() { return identifier; }
    public Integer getPriority() { return priority; }
    public int getSize() { return size; }
    public boolean isLocked() { return locked; }
    public boolean isVisible() { return visible; }
    public boolean hasCosmetic() { return cosmetic; }
    public ResourceLocation getIcon() { return icon; }

    public static final class Builder {
        private final String identifier;
        private int size = 1;
        private Integer priority;
        private boolean locked;
        private boolean visible = true;
        private boolean cosmetic;
        private ResourceLocation icon;

        public Builder(String identifier) { this.identifier = identifier; }

        public Builder icon(ResourceLocation icon) {
            this.icon = icon;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder lock() {
            this.locked = true;
            return this;
        }

        public Builder hide() {
            this.visible = false;
            return this;
        }

        public Builder cosmetic() {
            this.cosmetic = true;
            return this;
        }

        public SlotTypeMessage build() { return new SlotTypeMessage(this); }
    }
}
