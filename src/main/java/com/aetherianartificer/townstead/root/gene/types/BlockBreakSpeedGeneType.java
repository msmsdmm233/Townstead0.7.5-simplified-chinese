package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.Conditions;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Multiplies dig speed against blocks matching a filter (block {@code tag} or {@code block}
 * id) -- the block-scoped complement to {@code pheno:modifier break_speed}, which folds
 * through the capability layer and cannot see the target block. An optional entity
 * {@code condition} gates it (e.g. only while the mainhand is empty).
 *
 * <p>JSON: {@code { "type":"pheno:block_break_speed", "tag":"minecraft:logs", "value":0.25 }}</p>
 */
public final class BlockBreakSpeedGeneType implements GeneType {

    public static final String KEY = "pheno:block_break_speed";

    public record Instance(@Nullable TagKey<Block> tag, @Nullable ResourceLocation blockId,
                           float value, @Nullable Condition condition, String conditionJson)
            implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() {
            String spec = tag != null ? "#" + tag.location() : String.valueOf(blockId);
            return GeneDisplay.blockBreakSpeed(spec, value);
        }

        public boolean matches(BlockState state) {
            if (tag != null && state.is(tag)) return true;
            return blockId != null && state.is(BuiltInRegistries.BLOCK.get(blockId));
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public GeneInstance parse(JsonObject json) {
        TagKey<Block> tag = null;
        ResourceLocation blockId = null;
        if (json.has("tag")) {
            ResourceLocation id = DataPackLang.parseId(GsonHelper.getAsString(json, "tag", ""));
            if (id != null) tag = TagKey.create(Registries.BLOCK, id);
        } else if (json.has("block")) {
            blockId = DataPackLang.parseId(GsonHelper.getAsString(json, "block", ""));
        }
        if (tag == null && blockId == null) return null;
        float value = GsonHelper.getAsFloat(json, "value", 1f);
        Condition condition = json.has("condition") ? Conditions.parse(json.get("condition")) : null;
        String conditionJson = json.has("condition") ? json.get("condition").toString() : "";
        return new Instance(tag, blockId, value, condition, conditionJson);
    }
}
