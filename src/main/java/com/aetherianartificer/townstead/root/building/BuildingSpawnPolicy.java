package com.aetherianartificer.townstead.root.building;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A spawner building's root policy: which roots it may spawn ({@code allowedRoots}; empty means
 * "any"), which it must not ({@code deniedRoots}), and whether the village-majority disposition
 * filter still applies inside it ({@code checkDispositions}). Authored under
 * {@code data/<ns>/building_spawn/<building_type>.json}, keyed by MCA building-type id (e.g. {@code inn}).
 * Honored today for MCA's inn spawns; forward-compatible for any future building-driven spawn.
 */
public record BuildingSpawnPolicy(Set<String> allowedRoots, Set<String> deniedRoots, boolean checkDispositions) {

    /** Whether this building may spawn the given root id (deny wins; an allow list restricts). */
    public boolean allows(String rootId) {
        if (rootId == null) return false;
        if (deniedRoots.contains(rootId)) return false;
        return allowedRoots.isEmpty() || allowedRoots.contains(rootId);
    }

    /**
     * Parse a spawn-policy object: {@code allowed_roots} / {@code denied_roots} (root id lists,
     * normalized to canonical strings) and {@code check_village_dispositions} (default true). The legacy
     * {@code allowed_origins} / {@code denied_origins} keys are still honored as a fallback for older
     * packs. Shared by the canonical {@code extended_buildings} loader and the legacy
     * {@code building_spawn/} reader.
     */
    public static BuildingSpawnPolicy parse(JsonObject obj) {
        boolean check = GsonHelper.getAsBoolean(obj, "check_village_dispositions", true);
        return new BuildingSpawnPolicy(rootIds(obj, "allowed_roots", "allowed_origins"),
                rootIds(obj, "denied_roots", "denied_origins"), check);
    }

    private static Set<String> rootIds(JsonObject obj, String key, String legacyKey) {
        String chosen = obj.has(key) ? key : legacyKey;
        Set<String> out = new LinkedHashSet<>();
        if (obj.has(chosen) && obj.get(chosen).isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray(chosen)) {
                if (!e.isJsonPrimitive()) continue;
                ResourceLocation id = DataPackLang.parseId(e.getAsString());
                if (id != null) out.add(id.toString());
            }
        }
        return out;
    }
}
