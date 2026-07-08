package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.root.GeneRange;
import com.aetherianartificer.townstead.root.RootGenes;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One gene describing a race's whole build, in two parts:
 *
 * <ul>
 *   <li><b>Body metrics</b> — {@code [min,max]} ranges over MCA's continuous float genes
 *       ({@code size}, {@code width}, …), keyed by their normalized MCA key. Rolled into MCA's
 *       genetics at spawn (so they drive the server hitbox and blend continuously at breeding),
 *       exactly like {@link BodyMetricGeneType} — bundled here so a race's whole build is one gene.</li>
 *   <li><b>Render proportions</b> — a part-name → multiplier map. MCA scales the whole model (every
 *       part) by the build's {@code (horiz, vert)}, which squashes non-uniform builds; each listed
 *       part has that squash neutralized (anchored at the geometric mean, so size is preserved) and is
 *       then scaled by its factor. {@code 1.0} = proportioned, no resize; {@code >1}/{@code <1} =
 *       stylize. An unlisted part keeps MCA's normal squash. This is a client render effect.
 *       <p>Part keys are <b>not</b> validated here — they are free-form and shape-specific. Each
 *       species' render hook owns its own vocabulary and binds the keys it recognizes to actual
 *       {@code ModelPart}s; keys it doesn't know are ignored ({@code GeneCatalogEntry.proportionScale}
 *       returns {@code NaN} → no-op). Humanoid (MCA villager model) reads {@code head}/{@code arms}/
 *       {@code legs}/{@code body} in {@code VillagerHeadProportionMixin}; a future shape would read its
 *       own (e.g. {@code leg_front}/{@code tail}) in its own hook.</li>
 * </ul>
 *
 * <p>JSON: {@code { "type":"pheno:proportions", "category":"Appearance",
 * "size":{"min":0.0,"max":0.2}, "width":{"min":0.5,"max":0.7}, "head":1.0, "arms":1.0, "legs":1.0 }}.
 * A part value may also be a 3-element array {@code [x, y, z]} for per-axis stylizing — thick
 * arms that aren't longer ({@code [1.25, 1.0, 1.25]}), a barrel torso that's deep but not tall
 * ({@code [1.15, 1.0, 1.4]}). A plain number applies to all three axes.
 * A part value may also be {@code {"lean": ..., "stout": ...}} (each a number or axis array):
 * the individual's rolled MCA <b>width</b> gene picks their spot between the two, mapped through
 * the gene's own width range when it declares one — so a race rolls lean and heavyset members
 * from one gene, the build inherits through MCA's width blending, and the editor's width slider
 * moves it live. {@code lean} defaults to proportioned (1.0) when omitted.
 * All body metrics in the bundle share one auto-locus, so a lineage's proportions gene replaces its
 * ancestry's whole build at once.</p>
 */
public final class ProportionsGeneType implements GeneType {

    public static final String KEY = "pheno:proportions";

    private static final ResourceLocation LOCUS = ResourceLocation.tryParse("pheno:proportions");

    public record Instance(Map<String, GeneRange> bodyMetrics, Map<String, float[]> partScales)
            implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() {
            return GeneDisplay.proportions(partScales, bodyMetrics.get("width"));
        }
    }

    @Override
    public String key() { return KEY; }

    @Override
    public GeneInstance parse(JsonObject json) {
        Map<String, GeneRange> metrics = new LinkedHashMap<>();
        Map<String, float[]> parts = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String raw = entry.getKey();
            JsonElement value = entry.getValue();
            String target = RootGenes.normalizeKey(raw);
            if (value.isJsonObject() && RootGenes.isKnown(target)) {
                JsonObject range = value.getAsJsonObject();
                float min = GsonHelper.getAsFloat(range, "min", 0f);
                float max = GsonHelper.getAsFloat(range, "max", 1f);
                metrics.put(target, new GeneRange(min, max));
            } else if (value.isJsonObject() && value.getAsJsonObject().has("stout")) {
                // Rolled bulk: the individual's width gene lerps between the two forms.
                JsonObject bulk = value.getAsJsonObject();
                float[] lean = readAxes(bulk.get("lean"));
                float[] stout = readAxes(bulk.get("stout"));
                if (lean == null) lean = new float[]{1f, 1f, 1f};
                if (stout != null) {
                    parts.put(raw, new float[]{lean[0], lean[1], lean[2], stout[0], stout[1], stout[2]});
                }
            } else {
                // Free-form, shape-specific part key (see class doc) — not validated against a vocabulary.
                float[] axes = readAxes(value);
                if (axes != null) parts.put(raw, axes);
            }
        }
        if (metrics.isEmpty() && parts.isEmpty()) return null;
        return new Instance(Map.copyOf(metrics), Map.copyOf(parts));
    }

    /** A number ({@code f} on all axes) or 3-element array as {@code {x, y, z}}; null otherwise. */
    private static float[] readAxes(JsonElement value) {
        if (value == null) return null;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            float f = value.getAsFloat();
            return new float[]{f, f, f};
        }
        if (value.isJsonArray() && value.getAsJsonArray().size() == 3) {
            var array = value.getAsJsonArray();
            return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(),
                    array.get(2).getAsFloat()};
        }
        return null;
    }

    @Override
    @Nullable
    public ResourceLocation defaultLocus(GeneInstance instance) {
        return LOCUS;
    }
}
