# Curios-MITE Integration

Curios-MITE is a FishModLoader API for Minecraft 1.6.4 MITE. Add it as a required mod dependency and compile against its published or locally built `curios-mite` jar. The API uses MITE `Item`, `ItemStack`, `EntityLivingBase`, and NBT types; it does not use Forge capabilities or item-handler classes.

## Register Slot Types

Register slot types during mod initialization, before players enter a world. Repeated registrations merge into one slot type: the largest requested size is used, cosmetic support is enabled if any registration requests it, visibility is disabled if any registration hides it, and the most recent non-null icon and priority are used.

```java
CuriosApi.registerSlotType(
    new SlotTypeMessage.Builder("example:ring")
        .size(2)
        .priority(160)
        .icon(new ResourceLocation("example", "slot/empty_ring"))
        .cosmetic()
        .build()
);
```

Identifiers must match `[a-z0-9_.-]+(:[a-z0-9/._-]+)?`. Slot size must be between 1 and 256. `SlotTypePreset` provides common names and icons, for example `SlotTypePreset.RING.getMessageBuilder().size(2).build()`.

Slots requested with `.lock()` start locked. An entity can unlock them through its handler; changes are queued and applied by `processSlots()` on the server, which is also called during the entity tick:

```java
CuriosApi.getCuriosHandler(player).ifPresent(handler ->
    handler.unlockSlotType("example:ring", 2, true, true));
```

## Attach Item Behavior

An item can implement `ICurioItem` and construct behavior for each stack. Registering a provider is useful when the item class cannot implement the interface or when behavior depends on stack NBT.

```java
public final class ExampleCharm extends Item implements ICurioItem {
    @Override
    public ICurio getCurio(ItemStack stack) {
        return new ICurio() {
            @Override
            public boolean canRightClickEquip() {
                return true;
            }

            @Override
            public void curioTick(String identifier, int index, EntityLivingBase entity) {
                // Server-side equipped effect.
            }

            @Override
            public Multimap<Attribute, AttributeModifier> getAttributeModifiers(String identifier) {
                Multimap<Attribute, AttributeModifier> result = HashMultimap.create();
                // Add this stack's attribute modifiers.
                return result;
            }
        };
    }
}
```

`onEquip`, `onUnequip`, `curioTick`, `canEquip`, `canUnequip`, attribute modifiers, Fortune/Looting bonuses, break callbacks, and death drop rules are exposed on `ICurio`. `DEFAULT` follows `keepInventory`; `ALWAYS_DROP`, `ALWAYS_KEEP`, and `DESTROY` override that rule. Player death evaluates these rules while cloning the player. Other living entities evaluate them in `onDeath`; `doMobLoot` suppresses non-player drops. Curio instances are requested from the current `ItemStack`, so implementations should not cache stack-specific state in a shared item singleton.

## Restrict Slot Types

Items registered to a MITE item identifier can be selected by item tag files. Put files under `data/curios/tags/items/`; a top-level file name is an unnamespaced slot identifier. For a namespaced slot such as `example:ring`, use `data/curios/tags/items/example/ring.json`:

```json
{
  "replace": false,
  "values": [
    "example:charm",
    "#curios:ring"
  ]
}
```

Tag files from loaded mods are merged unless `replace` is true. Nested tags use `#namespace:path`. Tags loaded from files and tags registered with `CuriosApi.registerCurioTag(item, "example:ring")` are combined. When an item has no tags, it is accepted by any slot; when it has tags, it must include the target identifier or the `curio` tag.

## Read and Change Equipment

Slot and flat equipment indices are zero-based. Query the typed handler when slot identity matters:

```java
CuriosApi.getCuriosHandler(entity).ifPresent(handler -> {
    ICurioStacksHandler rings = handler.getStacksHandler("example:ring").orElse(null);
    if (rings == null) return;

    ItemStack firstRing = rings.getStacks().getStackInSlot(0);
    CuriosApi.getCuriosHelper().findEquippedCurio(
        stack -> stack.getItem() == EXAMPLE_CHARM, entity
    ).ifPresent(found -> {
        String slotId = found.getIdentifier();
        int slotIndex = found.getIndex();
    });
});
```

Use `handler.equip(identifier, index, stack)` and `handler.unequip(identifier, index)` for server-side changes. Each functional slot holds exactly one stack; equipment changes validate slot tags and `canEquip`/`canUnequip`, update attributes, invoke callbacks, and synchronize the entity. Cosmetic stacks are separate from functional stacks and can be set through `rings.getCosmeticStacks().setStackInSlot(index, stack)` on the server.

Cosmetic stacks are display-only and do not need an `ICurio` implementation. Items restricted by Curios tags must still match the target slot. To render a cosmetic item on an entity, register an `ICurioRenderer` for it on the client. Per-slot rendering flags are read through `getRenders()` and changed on the server with `setRender(index, enabled)`; changes are saved with the entity and synchronized to tracking clients.

## Client Rendering

Register renderers from a client-only entrypoint. `ICurioRenderer` is invoked relative to a `ModelBiped` anchor when the entity renderer has one. For entities using other model types, implement `ICurio.canRender` and `ICurio.render` or provide a renderer that does not depend on an anchor.

```java
CuriosClientApi.registerRenderer(EXAMPLE_CHARM,
    (stack, identifier, index, entity, model, anchor, partialTick) -> {
        // Draw relative to anchor; it is null for a non-biped model.
    });
```

The player inventory screen opens the Curios equipment screen. Visible slot types are shown there; cosmetic-capable slots have a separate appearance mode.
