package com.aetherianartificer.townstead.origin.gene.types;

import com.aetherianartificer.townstead.origin.gene.GeneDisplay;
import com.aetherianartificer.townstead.origin.gene.GeneInstance;
import com.aetherianartificer.townstead.origin.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * A race's skin colour as a gradient between two RGB endpoints given in HTML hex
 * ({@code from} → {@code to}); a villager lands at a point along it (the position
 * is the villager's roll, wired in the render phase). Arbitrary colours, so any
 * tone works (warm browns, orc green, undead grey) with no privileged palette.
 * If {@code to} is omitted it defaults to {@code from} (a flat colour).
 *
 * <p>JSON: {@code { "type":"townstead_origins:skin_tone", "from":"#F0D5B5",
 * "to":"#4A3020" }}</p>
 */
public final class SkinToneGeneType implements GeneType {

    public static final String KEY = "townstead_origins:skin_tone";

    public record Instance(int from, int to) implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.color(from, to); }
    }

    @Override
    public String key() { return KEY; }

    @Override
    public GeneInstance parse(JsonObject json) {
        int from = parseHex(GsonHelper.getAsString(json, "from", ""), 0xE8C6A2);
        int to = json.has("to") ? parseHex(GsonHelper.getAsString(json, "to", ""), from) : from;
        return new Instance(from, to);
    }

    /** Parse {@code #RRGGBB} / {@code RRGGBB} / {@code #RGB}; fall back on anything malformed. */
    private static int parseHex(String raw, int fallback) {
        if (raw == null) return fallback;
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
