package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.Conditions;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Treats the bearer's empty mainhand as a tool: harvest checks and dig speed consult a
 * phantom stack of {@code item} (e.g. a mountain dwarf's hands mining as a stone pickaxe).
 * The hand itself stays genuinely empty, so empty-hand interactions (CarryOn pickup,
 * taming, feeding) and attack damage are untouched. Only ever upgrades: harvest is
 * granted, never revoked, and dig speed takes the max of hand and phantom tool.
 *
 * <p>JSON: {@code { "type":"pheno:innate_tool", "item":"minecraft:stone_pickaxe" }}</p>
 */
public final class InnateToolGeneType implements GeneType {

    public static final String KEY = "pheno:innate_tool";

    public record Instance(ItemStack tool, ResourceLocation itemId,
                           @Nullable Condition condition, String conditionJson)
            implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.innateTool(itemId.toString()); }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public GeneInstance parse(JsonObject json) {
        ResourceLocation id = DataPackLang.parseId(GsonHelper.getAsString(json, "item", ""));
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return null;
        Condition condition = json.has("condition") ? Conditions.parse(json.get("condition")) : null;
        String conditionJson = json.has("condition") ? json.get("condition").toString() : "";
        return new Instance(new ItemStack(item), id, condition, conditionJson);
    }
}
