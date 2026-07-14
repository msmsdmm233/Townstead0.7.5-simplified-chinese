package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * An eye set for a custom-faced rig (skeletownies): a variant gene whose variants are the selectable
 * eye styles. Each variant carries its sprite-strip {@code texture} (frames in a fixed order, see
 * {@code SpeciesFaceLayer}) and a per-set {@code glow} flag (emissive, full-bright eyes). Rolled at
 * spawn, editable via the variant picker, inherited like any variant gene. The eye colour is a
 * separate {@code eye_color} tint gene; the texture is drawn greyscale and tinted.
 *
 * <p>JSON variant: {@code { "texture":"ns:textures/face/eyes/round.png", "glow":true }}</p>
 */
public final class EyesGeneType implements GeneType {

    public static final String KEY = "townstead_roots:eyes";

    // One eye set per creature: every eyes gene shares a locus so cross-ancestry
    // children inherit them as competing alleles instead of stacking both.
    private static final net.minecraft.resources.ResourceLocation LOCUS =
            com.aetherianartificer.townstead.data.DataPackLang.parseId(KEY);

    public record Instance(String texture, boolean glow) implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.PRESENCE; }
    }

    @Override
    public String key() { return KEY; }

    @Override
    public GeneInstance parse(JsonObject json) {
        return new Instance(GsonHelper.getAsString(json, "texture", ""),
                GsonHelper.getAsBoolean(json, "glow", false));
    }

    @Override
    public net.minecraft.resources.ResourceLocation defaultLocus(GeneInstance instance) {
        return LOCUS;
    }
}
