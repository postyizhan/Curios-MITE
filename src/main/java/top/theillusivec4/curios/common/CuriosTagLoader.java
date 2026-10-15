package top.theillusivec4.curios.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.Item;
import net.xiaoyu233.fml.FishModLoader;
import net.xiaoyu233.fml.api.item.IItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.mixin.ItemAccessor;

/** Loads legacy Curios item tags from FishModLoader mod resource roots. */
public final class CuriosTagLoader {
    private static final Logger LOGGER = LogManager.getLogger("curios");
    private static boolean loaded;

    private CuriosTagLoader() {}

    public static synchronized void loadIfNeeded() {
        if (!loaded) reload();
    }

    public static synchronized void reload() {
        Map<String, JsonObject> definitions = new LinkedHashMap<String, JsonObject>();
        List<net.fabricmc.loader.api.ModContainer> mods = new ArrayList<net.fabricmc.loader.api.ModContainer>();
        Set<String> seen = new LinkedHashSet<String>();
        for (net.fabricmc.loader.api.ModContainer mod : FishModLoader.getModsMap().values()) {
            if (seen.add(mod.getMetadata().getId())) mods.add(mod);
        }
        mods.sort(Comparator.comparing(mod -> mod.getMetadata().getId()));

        for (net.fabricmc.loader.api.ModContainer mod : mods) {
            for (Path root : mod.getRootPaths()) {
                Path tagsRoot = root.resolve("data/curios/tags/items");
                if (!Files.isDirectory(tagsRoot)) continue;
                try (java.util.stream.Stream<Path> paths = Files.walk(tagsRoot)) {
                    paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json"))
                        .sorted().forEach(path -> loadDefinition(path, tagsRoot, definitions));
                } catch (IOException exception) {
                    LOGGER.warn("Could not read Curios item tags from mod {}", mod.getMetadata().getId(), exception);
                }
            }
        }

        Map<Item, Set<String>> resolved = new IdentityHashMap<Item, Set<String>>();
        for (String tag : definitions.keySet()) {
            Set<String> visiting = new LinkedHashSet<String>();
            resolveTag(tag, definitions, resolved, visiting);
        }
        CuriosApi.replaceLoadedCurioTags(resolved);
        loaded = true;
        LOGGER.info("Loaded {} Curios item tag definitions", definitions.size());
    }

    private static void loadDefinition(Path path, Path tagsRoot, Map<String, JsonObject> definitions) {
        String tag = "curios:" + tagsRoot.relativize(path).toString().replace('\\', '/');
        tag = tag.substring(0, tag.length() - ".json".length());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            if (!element.isJsonObject()) throw new IllegalArgumentException("Tag root must be an object");
            JsonObject incoming = element.getAsJsonObject();
            if (!incoming.has("values") || !incoming.get("values").isJsonArray()) {
                throw new IllegalArgumentException("Tag must contain a values array");
            }

            JsonObject current = definitions.get(tag);
            if (current == null || (incoming.has("replace") && incoming.get("replace").getAsBoolean())) {
                current = new JsonObject();
                current.add("values", new JsonArray());
                definitions.put(tag, current);
            }
            JsonArray values = current.getAsJsonArray("values");
            for (JsonElement value : incoming.getAsJsonArray("values")) values.add(value);
        } catch (Exception exception) {
            LOGGER.warn("Ignoring invalid Curios item tag {} at {}", tag, path, exception);
        }
    }

    private static Set<String> resolveTag(String tag, Map<String, JsonObject> definitions,
                                          Map<Item, Set<String>> resolved, Set<String> visiting) {
        if (!visiting.add(tag)) {
            LOGGER.warn("Ignoring cyclic Curios item tag reference {}", tag);
            return Collections.emptySet();
        }
        Set<String> itemIds = new LinkedHashSet<String>();
        JsonObject definition = definitions.get(tag);
        if (definition != null) {
            for (JsonElement entry : definition.getAsJsonArray("values")) {
                String value;
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                    value = entry.getAsString();
                } else if (entry.isJsonObject() && entry.getAsJsonObject().has("id")) {
                    value = entry.getAsJsonObject().get("id").getAsString();
                } else {
                    LOGGER.warn("Ignoring malformed entry in Curios item tag {}", tag);
                    continue;
                }
                if (value.startsWith("#")) {
                    String nested = normalizeTagId(value.substring(1));
                    itemIds.addAll(resolveTag(nested, definitions, resolved, visiting));
                } else {
                    itemIds.add(normalizeTagId(value));
                }
            }
        }
        visiting.remove(tag);
        Set<String> identifiers = identifiersForTag(tag);
        for (Item item : Item.itemsList) {
            if (item == null || !(item instanceof IItem)) continue;
            String itemId = getItemId(item);
            if (itemId != null && itemIds.contains(itemId)) {
                resolved.computeIfAbsent(item, ignored -> new LinkedHashSet<String>()).addAll(identifiers);
            }
        }
        return itemIds;
    }

    private static String getItemId(Item item) {
        String registeredId = CuriosApi.getItemIdentifier(item);
        if (registeredId != null) return registeredId;
        String namespace = ((IItem) item).getNamespace();
        String path = ((ItemAccessor) item).curios$getIconString();
        if (namespace == null || path == null || path.isEmpty()) return null;
        int separator = path.indexOf(':');
        if (separator >= 0) {
            namespace = path.substring(0, separator);
            path = path.substring(separator + 1);
        }
        return namespace.toLowerCase(java.util.Locale.ROOT) + ":" + path.toLowerCase(java.util.Locale.ROOT);
    }

    private static Set<String> identifiersForTag(String tag) {
        if (!tag.startsWith("curios:")) return Collections.emptySet();
        String identifier = tag.substring("curios:".length());
        int separator = identifier.indexOf('/');
        if (separator < 0) return Collections.singleton(identifier);
        String namespace = identifier.substring(0, separator);
        String path = identifier.substring(separator + 1);
        if (namespace.isEmpty() || path.isEmpty()) return Collections.emptySet();
        return Collections.singleton(namespace + ":" + path);
    }

    private static String normalizeTagId(String id) {
        return id.indexOf(':') >= 0 ? id.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + id.toLowerCase(java.util.Locale.ROOT);
    }
}
