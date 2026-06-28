package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Nullifies a fluid's physics for the bearer: while it stands in a listed fluid it
 * moves with normal land movement instead of swimming, sinking under gravity and
 * walking the bottom, with no buoyancy, drag, or current push (an undead that treats
 * water as if it weren't there). Enforced by forcing {@code isAffectedByFluids} false
 * in that fluid, on both the {@code LivingEntity} and {@code Player} paths so a player
 * predicts it client-side and does not rubber-band. Drowning is handled separately by
 * a {@code damage_modifier} gene.
 *
 * <p>JSON: {@code { "type":"pheno:buoyancy", "fluids":["minecraft:water"] }} (a bare
 * gene with no {@code fluids} defaults to water).</p>
 */
public final class BuoyancyGeneType implements GeneType {

    public static final String KEY = "pheno:buoyancy";

    public record Instance(Set<TagKey<Fluid>> fluids) implements GeneInstance {
        public Instance { fluids = Set.copyOf(fluids); }
        @Override public String typeKey() { return KEY; }

        /** Packs the nullified fluid ids into the catalog so the owning client can predict the same movement. */
        @Override public GeneDisplay display() {
            Set<ResourceLocation> ids = new LinkedHashSet<>();
            for (TagKey<Fluid> fluid : fluids) ids.add(fluid.location());
            return GeneDisplay.buoyancy(ids);
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public GeneInstance parse(JsonObject json) {
        Set<TagKey<Fluid>> fluids = new LinkedHashSet<>();
        if (json.has("fluids") && json.get("fluids").isJsonArray()) {
            for (var element : json.getAsJsonArray("fluids")) addFluid(fluids, element.getAsString());
        } else if (json.has("fluid")) {
            addFluid(fluids, GsonHelper.getAsString(json, "fluid", ""));
        } else {
            addFluid(fluids, "minecraft:water");
        }
        return fluids.isEmpty() ? null : new Instance(fluids);
    }

    private static void addFluid(Set<TagKey<Fluid>> out, String raw) {
        ResourceLocation id = DataPackLang.parseId(raw);
        if (id != null) out.add(TagKey.create(Registries.FLUID, id));
    }
}
