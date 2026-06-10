package com.aetherianartificer.townstead.origin.gene.types;

import com.aetherianartificer.townstead.origin.gene.GeneDisplay;
import com.aetherianartificer.townstead.origin.gene.GeneInstance;
import com.aetherianartificer.townstead.origin.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * A racial resource meter (mana, blood, charge, ...) keyed by the gene id. Holds a
 * value in {@code [min,max]} that regenerates {@code regen} per {@code regen_interval}
 * ticks. Active abilities can cost it, and the {@code change_resource} action moves
 * it. The value lives server-side in {@code ResourceValues}; a HUD bar is a separate,
 * not-yet-built display layer.
 *
 * <p>JSON: {@code { "type":"townstead_origins:resource", "min":0, "max":100,
 * "start":100, "regen":1, "regen_interval":20 }}</p>
 */
public final class ResourceGeneType implements GeneType {

    public static final String KEY = "townstead_origins:resource";

    public record Instance(int min, int max, int start, int regen, int regenInterval, int color)
            implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.PRESENCE; }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public GeneInstance parse(JsonObject json) {
        int min = GsonHelper.getAsInt(json, "min", 0);
        int max = Math.max(min + 1, GsonHelper.getAsInt(json, "max", 100));
        int start = Math.max(min, Math.min(max, GsonHelper.getAsInt(json, "start", max)));
        int regen = GsonHelper.getAsInt(json, "regen", 1);
        int regenInterval = Math.max(1, GsonHelper.getAsInt(json, "regen_interval", 20));
        int color = parseHex(GsonHelper.getAsString(json, "color", "#3FA0FF"));
        return new Instance(min, max, start, regen, regenInterval, color);
    }

    private static int parseHex(String raw) {
        String s = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0x3FA0FF;
        }
    }
}
