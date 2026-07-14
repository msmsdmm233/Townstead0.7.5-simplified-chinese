package com.aetherianartificer.townstead.pheno.condition.bientity.types;

import com.aetherianartificer.townstead.pheno.condition.bientity.BiEntityCondition;
import com.aetherianartificer.townstead.pheno.condition.bientity.BiEntityConditionType;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * True when one entity lies within {@code max_angle} degrees of the other's look
 * direction (Apoli's bi-entity {@code relative_rotation}, simplified to a forward
 * cone). {@code reference} picks whose eyes define the cone: {@code "actor"} (default)
 * tests whether the target is in the actor's view cone; {@code "target"} tests whether
 * the actor is in the target's - inverted, that reads "the target cannot see the actor",
 * the backstab gate.
 *
 * <p>JSON: {@code { "type":"pheno:relative_rotation", "max_angle":45,
 * "reference":"target" }}</p>
 */
public final class RelativeRotationBiConditionType implements BiEntityConditionType {

    public static final String KEY = "pheno:relative_rotation";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public BiEntityCondition parse(JsonObject json) {
        double maxAngle = GsonHelper.getAsDouble(json, "max_angle", 90);
        boolean fromTarget = "target".equalsIgnoreCase(GsonHelper.getAsString(json, "reference", "actor"));
        return (actor, target) -> {
            var looker = fromTarget ? target : actor;
            var looked = fromTarget ? actor : target;
            Vec3 dir = looked.getEyePosition().subtract(looker.getEyePosition());
            if (dir.lengthSqr() < 1.0e-6) return true;
            double dot = Mth.clamp(looker.getLookAngle().dot(dir.normalize()), -1, 1);
            return Math.toDegrees(Math.acos(dot)) <= maxAngle;
        };
    }
}
