package com.aetherianartificer.townstead.origin.loot;

import com.aetherianartificer.townstead.Townstead;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@link DeathLootDef}s from {@code data/<ns>/death_loot/<origin-path>.json} (server data pack);
 * the file path is the origin id. Registered as a server reload listener; the loot is rolled + dropped
 * server-side, so nothing syncs to clients.
 */
public final class DeathLootLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Townstead.MOD_ID + "/DeathLootLoader");
    private static final Gson GSON = new Gson();

    public DeathLootLoader() {
        super(GSON, "death_loot");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, DeathLootDef> defs = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            // The key already has the "death_loot/" prefix and ".json" stripped, so it IS the origin id.
            ResourceLocation originId = entry.getKey();
            try {
                defs.put(originId, parse(GsonHelper.convertToJsonObject(entry.getValue(), originId.toString())));
            } catch (Exception ex) {
                LOGGER.warn("Failed to parse death loot {}: {}", originId, ex.getMessage());
            }
        }
        DeathLootRegistry.set(defs);
        LOGGER.info("Loaded {} death-loot table(s)", defs.size());
    }

    private static DeathLootDef parse(JsonObject json) {
        Map<String, List<DeathLootDef.Drop>> stages = new LinkedHashMap<>();
        if (json.has("stages") && json.get("stages").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("stages").entrySet()) {
                if (e.getValue().isJsonArray()) stages.put(e.getKey(), parseDrops(e.getValue().getAsJsonArray()));
            }
        }
        List<DeathLootDef.Drop> def = json.has("default") && json.get("default").isJsonArray()
                ? parseDrops(json.getAsJsonArray("default")) : List.of();
        return new DeathLootDef(stages, def);
    }

    private static List<DeathLootDef.Drop> parseDrops(JsonArray arr) {
        List<DeathLootDef.Drop> out = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String item = GsonHelper.getAsString(o, "item", "");
            ResourceLocation id = item.isEmpty() ? null : ResourceLocation.tryParse(item);
            if (id == null) continue;
            int min;
            int max;
            if (o.has("count")) {
                min = max = GsonHelper.getAsInt(o, "count", 1);
            } else {
                min = GsonHelper.getAsInt(o, "min", 1);
                max = GsonHelper.getAsInt(o, "max", min);
            }
            if (max < min) max = min;
            float chance = GsonHelper.getAsFloat(o, "chance", 1.0f);
            out.add(new DeathLootDef.Drop(id, Math.max(0, min), Math.max(0, max),
                    Math.max(0f, Math.min(1f, chance))));
        }
        return out;
    }
}
