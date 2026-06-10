package com.aetherianartificer.townstead.habitus.condition.bientity.types;

import com.aetherianartificer.townstead.habitus.condition.Condition;
import com.aetherianartificer.townstead.habitus.condition.ConditionContext;
import com.aetherianartificer.townstead.habitus.condition.Conditions;
import com.aetherianartificer.townstead.habitus.condition.bientity.BiEntityCondition;
import com.aetherianartificer.townstead.habitus.condition.bientity.BiEntityConditionType;
import com.google.gson.JsonObject;

/**
 * Applies an entity {@code condition} to the actor, the target, or both (Apoli's
 * bi-entity {@code actor_condition} / {@code target_condition} / {@code both} /
 * {@code either} metas), selected by {@link Scope}.
 *
 * <p>JSON: {@code { "type":"townstead_origins:target_condition",
 * "condition":{ "type":"townstead_origins:on_fire" } }}</p>
 */
public final class EntityScopedBiConditionType implements BiEntityConditionType {

    public enum Scope { ACTOR, TARGET, BOTH, EITHER }

    private final String key;
    private final Scope scope;

    public EntityScopedBiConditionType(String key, Scope scope) {
        this.key = key;
        this.scope = scope;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public BiEntityCondition parse(JsonObject json) {
        Condition inner = Conditions.parse(json.get("condition"));
        if (inner == null) return null;
        return (actor, target) -> switch (scope) {
            case ACTOR -> inner.test(new ConditionContext(actor));
            case TARGET -> inner.test(new ConditionContext(target));
            case BOTH -> inner.test(new ConditionContext(actor)) && inner.test(new ConditionContext(target));
            case EITHER -> inner.test(new ConditionContext(actor)) || inner.test(new ConditionContext(target));
        };
    }
}
