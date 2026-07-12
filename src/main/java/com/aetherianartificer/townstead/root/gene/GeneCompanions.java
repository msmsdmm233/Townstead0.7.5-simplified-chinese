package com.aetherianartificer.townstead.root.gene;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.root.gene.types.ResourceGeneType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Peels a gene's root {@code resources} and {@code companions} sections into companion genes
 * that <em>ride along</em> the parent's expression (see {@code GenePowerSource}). {@code resources}
 * declares meters next to the behavior that uses them (a triple-jump's jump counter, a spell's
 * mana); {@code companions} declares supporting components of any gene type (an active ability's
 * break-on-attack trigger, a form's aura) that belong to the same identity and should never be
 * inherited or displayed apart from it. Each becomes a real gene with the derived id
 * {@code <parentId>/<name>}, so it ticks and syncs exactly like a standalone gene.
 *
 * <p>Within the same gene, a bare {@code "resource": "name"} (or {@code compared_to_resource})
 * is rewritten to the derived id, so authors reference the short local name. References that
 * already carry a namespace are left untouched (a global resource).</p>
 */
public final class GeneCompanions {

    private GeneCompanions() {}

    /**
     * Extract a gene root's companion sections, mutating {@code root} in place: the
     * {@code resources} and {@code companions} blocks are removed and local resource references
     * are rewritten to derived ids. Returns the companion configs keyed by derived id (empty
     * when none are declared).
     */
    public static Map<ResourceLocation, JsonObject> extract(ResourceLocation file, JsonObject root) {
        boolean hasResources = root.has("resources") && root.get("resources").isJsonObject();
        boolean hasCompanions = root.has("companions") && root.get("companions").isJsonObject();
        if (!hasResources && !hasCompanions) return Map.of();
        Map<ResourceLocation, JsonObject> companions = new LinkedHashMap<>();
        if (hasResources) {
            JsonObject block = root.getAsJsonObject("resources");
            Map<String, String> rename = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : block.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                String name = entry.getKey();
                ResourceLocation derived = DataPackLang.parseId(file.getNamespace() + ":" + file.getPath() + "/" + name);
                if (derived == null) continue;
                JsonObject config = entry.getValue().getAsJsonObject().deepCopy();
                if (!config.has("type")) config.addProperty("type", ResourceGeneType.KEY);
                companions.put(derived, config);
                rename.put(name, derived.toString());
            }
            root.remove("resources");
            // The companions block is still on the root here, so its configs get local
            // resource references rewritten too.
            rewriteRefs(root, rename);
        }
        if (hasCompanions) {
            JsonObject block = root.getAsJsonObject("companions");
            for (Map.Entry<String, JsonElement> entry : block.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                ResourceLocation derived = DataPackLang.parseId(
                        file.getNamespace() + ":" + file.getPath() + "/" + entry.getKey());
                if (derived == null) continue;
                companions.put(derived, entry.getValue().getAsJsonObject().deepCopy());
            }
            root.remove("companions");
        }
        return companions;
    }

    /** Rewrite local {@code resource}/{@code compared_to_resource} string values to their derived ids. */
    private static void rewriteRefs(JsonElement element, Map<String, String> rename) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) rewriteRefs(child, rename);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject obj = element.getAsJsonObject();
        for (String key : new ArrayList<>(obj.keySet())) {
            JsonElement value = obj.get(key);
            boolean ref = (key.equals("resource") || key.equals("compared_to_resource"))
                    && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
            if (ref) {
                String renamed = rename.get(value.getAsString());
                if (renamed != null) obj.addProperty(key, renamed);
            } else {
                rewriteRefs(value, rename);
            }
        }
    }
}
