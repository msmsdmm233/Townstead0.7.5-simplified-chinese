package com.aetherianartificer.townstead.emote;

import com.aetherianartificer.townstead.reaction.backend.EmoteDurationIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Optional;
import java.util.WeakHashMap;

/** Server-side view of entities with an active Townstead-triggered emote. */
public final class EmoteActivityTracker {

    private static final class State {
        long until;
        ResourceLocation emoteId;
    }

    private static final WeakHashMap<LivingEntity, State> ACTIVE = new WeakHashMap<>();
    private static final Object LOCK = new Object();

    private EmoteActivityTracker() {}

    public static void start(LivingEntity entity, ResourceLocation emoteId, int shots) {
        if (entity == null || emoteId == null || entity.level().isClientSide()) return;
        long now = entity.level().getGameTime();
        Optional<Integer> duration = EmoteDurationIndex.ticksFor(emoteName(emoteId), shots);
        synchronized (LOCK) {
            State state = ACTIVE.computeIfAbsent(entity, e -> new State());
            state.emoteId = emoteId;
            state.until = duration.map(ticks -> now + ticks).orElse(Long.MAX_VALUE);
        }
    }

    public static void stop(LivingEntity entity) {
        if (entity == null) return;
        synchronized (LOCK) {
            ACTIVE.remove(entity);
        }
    }

    public static boolean isEmoting(LivingEntity entity) {
        if (entity == null) return false;
        long now = entity.level().getGameTime();
        synchronized (LOCK) {
            State state = ACTIVE.get(entity);
            if (state == null) return false;
            if (now < state.until) return true;
            ACTIVE.remove(entity);
            return false;
        }
    }

    public static ResourceLocation activeEmote(LivingEntity entity) {
        if (!isEmoting(entity)) return null;
        synchronized (LOCK) {
            State state = ACTIVE.get(entity);
            return state == null ? null : state.emoteId;
        }
    }

    private static String emoteName(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.toLowerCase(Locale.ROOT);
    }
}
