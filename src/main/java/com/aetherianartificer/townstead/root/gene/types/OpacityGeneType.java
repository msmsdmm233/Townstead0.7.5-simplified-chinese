package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.Conditions;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Body render opacity while the condition holds: 1 = solid, 0 = unseen. Overrides the
 * default target (0 when the invisible flag is set, 1 otherwise), so it expresses both
 * imperfect invisibility (a shimmer at {@code alpha} 0.15 gated on {@code pheno:invisible})
 * and standing translucency (a ghost race at 0.5 with no condition). Pure render — mob
 * sight and nameplate hiding are untouched. Applied client-side by {@code InvisFade},
 * which eases between opacity targets; the condition is re-evaluated there on the
 * bearer's client, so it should stick to entity-state conditions (server-only conditions
 * like {@code structure} never hold).
 *
 * <p>JSON: {@code { "type":"pheno:opacity", "alpha":0.15,
 * "condition":{ "type":"pheno:invisible" } }}</p>
 */
public final class OpacityGeneType implements GeneType {

    public static final String KEY = "pheno:opacity";

    public record Instance(float alpha, @Nullable Condition condition, String conditionJson)
            implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.opacity(alpha); }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public GeneInstance parse(JsonObject json) {
        float alpha = GsonHelper.getAsFloat(json, "alpha", 1f);
        Condition condition = json.has("condition") ? Conditions.parse(json.get("condition")) : null;
        // A condition that was declared but failed to parse must reject the gene: dropping
        // the gate would silently widen when the opacity applies.
        if (json.has("condition") && condition == null) return null;
        // The raw condition rides the catalog sync so the bearer's client re-evaluates the
        // same gate per frame (the parsed closure can't cross the wire).
        String conditionJson = json.has("condition") ? json.get("condition").toString() : "";
        return new Instance(Math.max(0f, Math.min(1f, alpha)), condition, conditionJson);
    }
}
