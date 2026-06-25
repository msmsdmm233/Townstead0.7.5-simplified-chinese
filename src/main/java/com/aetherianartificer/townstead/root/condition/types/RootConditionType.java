package com.aetherianartificer.townstead.root.condition.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionType;
import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import com.google.gson.JsonObject;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * True when the entity's chosen origin is the given {@code origin} id (Apoli's
 * {@code origin}). Reads the player's stored origin or the villager's life origin.
 *
 * <p>JSON: {@code { "type":"pheno:origin", "origin":"pheno:overworlder" }}</p>
 */
public final class RootConditionType implements ConditionType {

    public static final String KEY = "pheno:origin";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Condition parse(JsonObject json) {
        ResourceLocation id = DataPackLang.parseId(GsonHelper.getAsString(json, "origin", ""));
        if (id == null) return null;
        String target = id.toString();
        return ctx -> rootId(ctx.entity()).equals(target);
    }

    private static String rootId(LivingEntity entity) {
        if (entity instanceof Player player) return PlayerRoot.getRootId(player);
        if (entity instanceof VillagerEntityMCA villager) {
            return TownsteadVillagers.get(villager).life().rootId();
        }
        return "";
    }
}
