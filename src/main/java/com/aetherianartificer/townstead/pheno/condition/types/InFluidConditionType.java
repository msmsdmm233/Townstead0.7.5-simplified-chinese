package com.aetherianartificer.townstead.pheno.condition.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionType;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;

import java.util.Locale;

/** True when the entity is standing in the given fluid tag. */
public final class InFluidConditionType implements ConditionType {

    public static final String KEY = "pheno:in_fluid";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Condition parse(JsonObject json) {
        TagKey<Fluid> tag;
        if (json.has("fluid_tag")) {
            ResourceLocation id = DataPackLang.parseId(GsonHelper.getAsString(json, "fluid_tag", ""));
            if (id == null) return null;
            tag = TagKey.create(Registries.FLUID, id);
        } else {
            tag = switch (GsonHelper.getAsString(json, "fluid", "water").toLowerCase(Locale.ROOT)) {
                case "lava", "minecraft:lava" -> FluidTags.LAVA;
                case "water", "minecraft:water" -> FluidTags.WATER;
                default -> {
                    ResourceLocation id = DataPackLang.parseId(GsonHelper.getAsString(json, "fluid", ""));
                    yield id == null ? FluidTags.WATER : TagKey.create(Registries.FLUID, id);
                }
            };
        }
        return ctx -> ctx.entity().getFluidHeight(tag) > 0;
    }
}
