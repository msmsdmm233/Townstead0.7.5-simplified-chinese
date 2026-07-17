package com.aetherianartificer.townstead.client.animation;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Seeds EMF's per-entity CEM animation variable map for GUI preview entities, read reflectively
 * from {@code EMFEntity#emf$getVariableMap} (EMF injects the interface into every entity). No-op
 * when EMF isn't present.
 */
public final class EmfPreviewSeed {
    private static Method getVariableMap;
    private static boolean resolveFailed;

    private EmfPreviewSeed() {}

    /**
     * Pre-saturate Fresh Animations' landing timer on a fresh preview entity. {@code var.t_land}
     * starts at 0 and integrates up to its resting value of 1, playing one landing squash on the
     * way; in-world that sweep is masked by FA's age&lt;9 spawn guards, which the preview's
     * wall-clock age defeats. Seeding only applies before FA's first evaluation
     * ({@code putIfAbsent}) — afterwards the pack owns the variable.
     */
    public static void seedLandingSettled(LivingEntity entity) {
        Map<String, Float> vars = variableMap(entity);
        if (vars != null) vars.putIfAbsent("var.t_land", 1.0f);
    }

    private static Map<String, Float> variableMap(LivingEntity entity) {
        if (resolveFailed) return null;
        try {
            if (getVariableMap == null) {
                getVariableMap = entity.getClass().getMethod("emf$getVariableMap");
            }
            Object map = getVariableMap.invoke(entity);
            if (map instanceof Map<?, ?> raw) {
                @SuppressWarnings("unchecked")
                Map<String, Float> typed = (Map<String, Float>) raw;
                return typed;
            }
            return null;
        } catch (ReflectiveOperationException | ClassCastException e) {
            resolveFailed = true;
            return null;
        }
    }
}
