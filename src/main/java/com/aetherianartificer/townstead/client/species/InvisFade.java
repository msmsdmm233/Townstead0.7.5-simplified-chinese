package com.aetherianartificer.townstead.client.species;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only alpha fade for the invisible flag. Vanilla pops an entity out of
 * existence the tick the (already client-synced) flag flips; render layers ask this
 * tracker instead and get a short gradient: 1 (fully drawn) eases to 0 over
 * {@link #FADE_TICKS} after the entity turns invisible, and back up after it
 * reappears. Purely observational — no networking, no server state. Transitions are
 * detected on query, so an entity that flips while off-screen simply starts its fade
 * when next seen.
 */
public final class InvisFade {

    private static final int FADE_TICKS = 8;

    private record State(boolean invisible, long since) {}

    /** Seed marker: no transition observed yet, alpha sits at the flag's value. */
    private static final long NEVER = Long.MIN_VALUE;

    private static final Map<Integer, State> STATES = new HashMap<>();

    private InvisFade() {}

    /** Once per client tick: drop entries for entities no longer in the level. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STATES.clear();
            return;
        }
        STATES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    public static void clear() {
        STATES.clear();
    }

    /**
     * The draw alpha for this entity right now: 1 fully visible, 0 fully invisible,
     * in between while fading. First sight seeds at the flag's value so entities
     * entering render distance never fade in from nothing.
     */
    public static float alpha(LivingEntity entity, float partialTick) {
        boolean invisible = entity.isInvisible();
        long now = entity.level().getGameTime();
        State s = STATES.get(entity.getId());
        if (s == null) {
            s = new State(invisible, NEVER);
            STATES.put(entity.getId(), s);
        } else if (s.invisible != invisible) {
            s = new State(invisible, now);
            STATES.put(entity.getId(), s);
        }
        float t = s.since == NEVER ? 1f
                : Math.min(1f, ((now - s.since) + partialTick) / FADE_TICKS);
        return invisible ? 1f - t : t;
    }
}
