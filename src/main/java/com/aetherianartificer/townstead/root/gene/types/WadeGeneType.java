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
 * Scales how fast the bearer moves while in a listed fluid: {@code speed} is a fraction of
 * normal movement (1.0 = unchanged, 0.5 = half, &gt;1 = faster). Independent of
 * {@code buoyancy} (which decides sink vs swim); pair them for a creature that both sinks and
 * trudges. Applied as a transient {@code movement_speed} modifier while in the fluid, so it
 * governs walking/striding (an undead walking the bottom, or anything in shallows) and syncs to
 * the owning client for free.
 *
 * <p>JSON: {@code { "type":"pheno:wade", "fluids":["minecraft:water"], "speed":0.6 }} (a bare
 * gene with no {@code fluids} defaults to water).</p>
 */
public final class WadeGeneType implements GeneType {

    public static final String KEY = "pheno:wade";

    public record Instance(Set<TagKey<Fluid>> fluids, float speed) implements GeneInstance {
        public Instance { fluids = Set.copyOf(fluids); }
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.PRESENCE; }
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
        if (fluids.isEmpty()) return null;
        float speed = Math.max(0f, GsonHelper.getAsFloat(json, "speed", 1f));
        return new Instance(fluids, speed);
    }

    private static void addFluid(Set<TagKey<Fluid>> out, String raw) {
        ResourceLocation id = DataPackLang.parseId(raw);
        if (id != null) out.add(TagKey.create(Registries.FLUID, id));
    }
}
