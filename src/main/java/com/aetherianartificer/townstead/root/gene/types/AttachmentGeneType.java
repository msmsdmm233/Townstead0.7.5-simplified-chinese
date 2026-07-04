package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * A cosmetic model attachment a race wears (elf ears, tusks, horns, …). The
 * {@code attachment} id points at a client-side attachment definition (geometry
 * + texture + anchor part + tint), rendered as an extra layer anchored to the
 * villager's model so it follows animation.
 *
 * <p>An optional {@code size} block makes the attachment's scale a heritable
 * quantitative trait: each allele carries a value rolled in {@code [min,max]}
 * (1.0 = the authored geometry's neutral size), two carried copies express their
 * mean, and an optional heritage coupling multiplies the expressed value by the
 * bearer's share of one ancestry — a half-elf grows visibly smaller elven ears
 * than a full elf. The attachment definition's {@code morph} block maps the
 * value onto per-axis bone scales on the render side.</p>
 *
 * <p>JSON: {@code { "type":"townstead_roots:attachment",
 * "attachment":"townstead_roots:elf_ears",
 * "size": { "min":0.85, "max":1.15,
 *           "heritage": { "ancestry":"townstead_classic:elf", "floor":0.35 } } }}</p>
 */
public final class AttachmentGeneType implements GeneType {

    public static final String KEY = "townstead_roots:attachment";

    /**
     * The heritable size roll: its range, the optional ancestry coupling, and the editor slider's
     * label ({@code labelKey} = translate key from the size block's {@code label}, {@code labelText}
     * = literal fallback; both empty → the editor falls back to the gene's display name).
     */
    public record Size(float min, float max, @Nullable ResourceLocation heritageAncestry, float heritageFloor,
                       String labelKey, String labelText) {

        /** The heritage multiplier for a bearer holding {@code fraction} of the coupled ancestry. */
        public float heritageFactor(float fraction) {
            if (heritageAncestry == null) return 1f;
            float f = Math.max(0f, Math.min(1f, fraction));
            return heritageFloor + (1f - heritageFloor) * f;
        }
    }

    public record Instance(String attachmentId, @Nullable Size size) implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() {
            return size == null ? GeneDisplay.attachment(attachmentId)
                    : GeneDisplay.sizedAttachment(attachmentId, size.min(), size.max());
        }
    }

    @Override
    public String key() { return KEY; }

    @Override
    public GeneInstance parse(JsonObject json) {
        String id = GsonHelper.getAsString(json, "attachment", "");
        if (id.isBlank()) return null;
        Size size = null;
        if (json.has("size") && json.get("size").isJsonObject()) {
            JsonObject sizeJson = json.getAsJsonObject("size");
            ResourceLocation ancestry = null;
            float floor = 0f;
            if (sizeJson.has("heritage") && sizeJson.get("heritage").isJsonObject()) {
                JsonObject heritage = sizeJson.getAsJsonObject("heritage");
                ancestry = DataPackLang.parseId(GsonHelper.getAsString(heritage, "ancestry", ""));
                floor = Math.max(0f, Math.min(1f, GsonHelper.getAsFloat(heritage, "floor", 0f)));
            }
            String labelKey = "";
            String labelText = "";
            if (sizeJson.has("label")) {
                var label = sizeJson.get("label");
                if (label.isJsonObject()) {
                    labelKey = GsonHelper.getAsString(label.getAsJsonObject(), "translate", "");
                    labelText = GsonHelper.getAsString(label.getAsJsonObject(), "text", "");
                } else if (label.isJsonPrimitive()) {
                    labelText = label.getAsString();
                }
            }
            size = new Size(GsonHelper.getAsFloat(sizeJson, "min", 1f),
                    GsonHelper.getAsFloat(sizeJson, "max", 1f), ancestry, floor, labelKey, labelText);
        }
        return new Instance(id, size);
    }
}
