package top.theillusivec4.curios.api.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.Item;
import net.minecraft.ItemStack;

/** Client-only Curios renderer registration. */
public final class CuriosClientApi {
    private static final Map<Item, ICurioRenderer> RENDERERS =
        Collections.synchronizedMap(new IdentityHashMap<Item, ICurioRenderer>());

    private CuriosClientApi() {}

    public static void registerRenderer(Item item, ICurioRenderer renderer) {
        if (item == null || renderer == null) throw new IllegalArgumentException("Item and renderer are required");
        RENDERERS.put(item, renderer);
    }

    public static void unregisterRenderer(Item item) {
        if (item != null) RENDERERS.remove(item);
    }

    public static ICurioRenderer getRenderer(ItemStack stack) {
        return stack == null || stack.getItem() == null ? null : RENDERERS.get(stack.getItem());
    }
}
