package com.aetherianartificer.townstead.origin.port;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Translates an Apoli entity-condition JSON into the Townstead {@code condition}
 * subset (see {@code origin/condition}). Returns {@code null} for anything outside
 * the subset, so the porting tool can drop the whole gated power rather than emit a
 * half-correct gate. Used only by the offline {@code /townstead origins port} tool.
 */
public final class ApoliConditionTranslator {

    private ApoliConditionTranslator() {}

    @Nullable
    public static JsonObject translate(@Nullable JsonObject apoli) {
        if (apoli == null) return null;
        String type = stripNamespace(GsonHelper.getAsString(apoli, "type", ""));
        JsonObject out = base(type, apoli);
        if (out == null) return null;
        if (GsonHelper.getAsBoolean(apoli, "inverted", false)) out.addProperty("inverted", true);
        return out;
    }

    @Nullable
    private static JsonObject base(String type, JsonObject apoli) {
        switch (type) {
            case "in_rain": return simple("in_rain");
            case "on_fire": return simple("on_fire");
            case "sneaking": return simple("sneaking");
            case "sprinting": return simple("sprinting");
            case "moving": return simple("moving");
            case "submerged_in": return simple("submerged");
            case "daytime": return simple("daytime");
            case "exposed_to_sky": return simple("exposed_to_sky");
            case "on_ground": return simple("on_ground");
            case "brightness": return brightness(apoli);
            case "dimension": return copyString(apoli, "dimension", "townstead_origins:dimension", "dimension");
            case "biome": return apoli.has("biome")
                    ? copyString(apoli, "biome", "townstead_origins:biome", "biome") : null;
            case "and": return composite("townstead_origins:and", apoli);
            case "or": return composite("townstead_origins:or", apoli);
            default: return null;
        }
    }

    private static JsonObject simple(String name) {
        JsonObject out = new JsonObject();
        out.addProperty("type", "townstead_origins:" + name);
        return out;
    }

    private static JsonObject brightness(JsonObject apoli) {
        // Apoli compares a value: {"comparison":">=","compare_to":7}.
        String comparison = GsonHelper.getAsString(apoli, "comparison", ">=");
        float compareTo = GsonHelper.getAsFloat(apoli, "compare_to", 0f);
        JsonObject out = simple("brightness");
        if (comparison.startsWith(">")) out.addProperty("min", (int) Math.ceil(compareTo));
        else if (comparison.startsWith("<")) out.addProperty("max", (int) Math.floor(compareTo));
        else { out.addProperty("min", (int) compareTo); out.addProperty("max", (int) compareTo); }
        return out;
    }

    private static JsonObject copyString(JsonObject apoli, String fromKey, String type, String toKey) {
        if (!apoli.has(fromKey)) return null;
        JsonObject out = new JsonObject();
        out.addProperty("type", type);
        out.addProperty(toKey, GsonHelper.getAsString(apoli, fromKey, ""));
        return out;
    }

    @Nullable
    private static JsonObject composite(String type, JsonObject apoli) {
        JsonArray source = GsonHelper.getAsJsonArray(apoli, "conditions", new JsonArray());
        JsonArray translated = new JsonArray();
        for (var element : source) {
            if (!element.isJsonObject()) return null;
            JsonObject child = translate(element.getAsJsonObject());
            if (child == null) return null;
            translated.add(child);
        }
        JsonObject out = new JsonObject();
        out.addProperty("type", type);
        out.add("conditions", translated);
        return out;
    }

    static String stripNamespace(String type) {
        int colon = type.indexOf(':');
        return (colon < 0 ? type : type.substring(colon + 1)).toLowerCase(Locale.ROOT);
    }
}
