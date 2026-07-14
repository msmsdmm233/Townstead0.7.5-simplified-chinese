package com.aetherianartificer.townstead.pheno.action.types;

import com.aetherianartificer.townstead.pheno.action.Action;
import com.aetherianartificer.townstead.pheno.action.ActionType;
import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionContext;
import com.aetherianartificer.townstead.pheno.condition.Conditions;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * Makes the acting entity (a mob) attack the context's {@code other()} entity. Sets
 * last-hurt-by as well as the live target, so revenge goals take over and same-type
 * pack alerts fire, outlasting the mob's next AI re-evaluation. An optional
 * {@code condition} filters which mobs answer (e.g. only spiders); non-mob actors and
 * a missing/dead/self other are no-ops. Pair with {@code area_of_effect} off a combat
 * trigger: dark elf Broodcall runs it on every entity near the bearer's attacker.
 *
 * <p>JSON: {@code { "type":"pheno:set_attack_target",
 * "condition":{ "type":"pheno:entity_type", "entity_type":"minecraft:spider" } }}</p>
 */
public final class SetAttackTargetActionType implements ActionType {

    public static final String KEY = "pheno:set_attack_target";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Action parse(JsonObject json) {
        @Nullable Condition condition = json.has("condition") ? Conditions.parse(json.get("condition")) : null;
        if (json.has("condition") && condition == null) return null;
        return ctx -> {
            if (!(ctx.entity() instanceof Mob mob)) return;
            LivingEntity victim = ctx.other();
            if (victim == null || victim == mob || !victim.isAlive()) return;
            if (condition != null && !condition.test(new ConditionContext(mob))) return;
            mob.setLastHurtByMob(victim);
            mob.setTarget(victim);
        };
    }
}
