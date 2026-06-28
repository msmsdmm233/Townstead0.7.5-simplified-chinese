package com.aetherianartificer.townstead.reaction.trigger.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.WeakHashMap;

/** Short-lived social interaction facts that Pheno and reaction context can query. */
public final class SocialInteractionTracker {
    private static final int RECENT_WINDOW_TICKS = 60;

    private record State(long heartIncreasedAt, long heartDecreasedAt) {}

    private static final WeakHashMap<LivingEntity, State> BY_ENTITY = new WeakHashMap<>();
    private static final Object LOCK = new Object();

    private SocialInteractionTracker() {}

    public static void markHeartChange(LivingEntity entity, int delta, long gameTime) {
        if (entity == null || delta == 0) return;
        synchronized (LOCK) {
            State state = state(entity);
            BY_ENTITY.put(entity, delta > 0
                    ? new State(gameTime, state.heartDecreasedAt)
                    : new State(state.heartIncreasedAt, gameTime));
        }
    }

    public static boolean heartIncreasedRecently(LivingEntity entity, long gameTime) {
        return recent(entity, gameTime, Kind.HEART_INCREASED);
    }

    public static boolean heartDecreasedRecently(LivingEntity entity, long gameTime) {
        return recent(entity, gameTime, Kind.HEART_DECREASED);
    }

    public static void prune(long gameTime) {
        synchronized (LOCK) {
            BY_ENTITY.entrySet().removeIf(entry -> {
                State state = entry.getValue();
                return !isRecent(state.heartIncreasedAt, gameTime)
                        && !isRecent(state.heartDecreasedAt, gameTime);
            });
        }
    }

    private enum Kind {
        HEART_INCREASED,
        HEART_DECREASED
    }

    private static boolean recent(LivingEntity entity, long gameTime, Kind kind) {
        if (entity == null) return false;
        synchronized (LOCK) {
            State state = BY_ENTITY.get(entity);
            if (state == null) return false;
            return switch (kind) {
                case HEART_INCREASED -> isRecent(state.heartIncreasedAt, gameTime);
                case HEART_DECREASED -> isRecent(state.heartDecreasedAt, gameTime);
            };
        }
    }

    private static State state(LivingEntity entity) {
        State state = BY_ENTITY.get(entity);
        return state != null ? state : new State(Long.MIN_VALUE, Long.MIN_VALUE);
    }

    private static boolean isRecent(long markedAt, long gameTime) {
        return markedAt != Long.MIN_VALUE && gameTime - markedAt <= RECENT_WINDOW_TICKS;
    }
}
