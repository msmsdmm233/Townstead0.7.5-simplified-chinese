package com.aetherianartificer.townstead.pheno.condition.types;

import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionContext;
import com.aetherianartificer.townstead.pheno.condition.ConditionType;
import com.google.gson.JsonObject;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MoodGroup;
import net.minecraft.util.GsonHelper;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * {@code pheno:mood} — the villager's MCA mood, by raw value ({@code min}/{@code max}
 * over -15..15 in normal play) or by band name ({@code is}: one name or a list of
 * {@code depressed}/{@code sad}/{@code unhappy}/{@code passive}/{@code fine}/
 * {@code happy}/{@code overjoyed}). The mood value is entity-tracked, so this
 * evaluates identically on server and client (attachment poses). Non-villagers
 * never match.
 */
public final class MoodConditionType implements ConditionType {

    @Override
    public String key() {
        return "pheno:mood";
    }

    @Override
    public Condition parse(JsonObject json) {
        double min = GsonHelper.getAsDouble(json, "min", -Double.MAX_VALUE);
        double max = GsonHelper.getAsDouble(json, "max", Double.MAX_VALUE);
        Set<String> names = new HashSet<>();
        if (json.has("is")) {
            if (json.get("is").isJsonArray()) {
                for (var element : json.getAsJsonArray("is")) {
                    names.add(element.getAsString().toLowerCase(Locale.ROOT));
                }
            } else {
                names.add(GsonHelper.getAsString(json, "is", "").toLowerCase(Locale.ROOT));
            }
        }
        return ctx -> test(ctx, min, max, names);
    }

    private static boolean test(ConditionContext ctx, double min, double max, Set<String> names) {
        if (!(ctx.entity() instanceof VillagerEntityMCA villager)) return false;
        int value = villager.getVillagerBrain().getMoodValue();
        if (value < min || value > max) return false;
        return names.isEmpty()
                || names.contains(MoodGroup.INSTANCE.getMood(value).getName().toLowerCase(Locale.ROOT));
    }
}
