package com.aetherianartificer.townstead.client.animation;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.TownsteadConfig;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * DEBUG_LOGGING-gated dump of EMF's per-entity CEM animation variable state,
 * read reflectively from {@code EMFEntity#emf$getVariableMap} (EMF injects the
 * interface into every entity). Animation packs (Fresh Animations) keep their
 * stateful `var.*` integrators in this map, so when a GUI preview dummy plays
 * an animation it shouldn't, the looping variable names itself here instead of
 * being guessed from the pack's expression soup.
 */
public final class EmfVariableDebug {
    private static final long INTERVAL_NANOS = 2_000_000_000L;
    private static Method getVariableMap;
    private static boolean resolveFailed;
    private static long lastLogNanos;

    private EmfVariableDebug() {}

    /**
     * Pre-saturate FA's landing timer on a fresh preview entity. `var.t_land` starts at 0
     * and integrates up to its resting value of 1, playing one landing squash on the way;
     * in-world that sweep is masked by FA's age&lt;9 spawn guards, which the preview's
     * wall-clock age defeats. Seeding only applies before FA's first evaluation
     * (putIfAbsent) — afterwards the pack owns the variable.
     */
    public static void seedLandingSettled(LivingEntity entity) {
        Map<String, Float> vars = variableMap(entity);
        if (vars != null) vars.putIfAbsent("var.t_land", 1.0f);
    }

    public static void logPreviewEntity(LivingEntity entity) {
        if (!TownsteadConfig.DEBUG_LOGGING.get()) return;
        long now = System.nanoTime();
        if (now - lastLogNanos < INTERVAL_NANOS) return;
        Map<String, Float> vars = variableMap(entity);
        if (vars == null || vars.isEmpty()) return;
        lastLogNanos = now;
        StringBuilder sb = new StringBuilder();
        vars.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append('=')
                        .append(String.format(Locale.ROOT, "%.3f", e.getValue())).append(' '));
        Townstead.LOGGER.info(
                "[EmfVarDebug] entity={} ({}) onGround={} crouching={} tickCount={} pos={} vars: {}",
                entity.getUUID().toString().substring(0, 8),
                entity.getName().getString(),
                entity.onGround(),
                entity.isCrouching(),
                entity.tickCount,
                entity.position(),
                sb);
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
            Townstead.LOGGER.info("[EmfVarDebug] EMF variable map unavailable: {}", e.toString());
            return null;
        }
    }
}
