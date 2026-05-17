package com.aetherianartificer.townstead.calendar;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side mirror of the server's calendar state. Updated by
 * {@link CalendarSyncPayload} handlers, cleared on logout.
 *
 * Text fields arrive as (translate key, fallback) pairs and are reconstructed
 * into Components on the client so each player sees their own locale.
 */
public final class CalendarClientStore {

    public record Snapshot(
            long worldDay,
            int year,
            int monthIndex,
            int dayOfMonth,
            int dayOfYear,
            int dayOfWeek,
            String monthKey,
            String monthFallback,
            String profileKey,
            String profileFallback,
            String seasonKey
    ) {
        public Component monthComponent() { return ComponentSync.reconstruct(monthKey, monthFallback); }
        public Component profileComponent() { return ComponentSync.reconstruct(profileKey, profileFallback); }
        public Component seasonComponent() {
            if (seasonKey == null || seasonKey.isEmpty()) return Component.empty();
            return Component.translatable(seasonKey);
        }
        public boolean hasSeason() { return seasonKey != null && !seasonKey.isEmpty(); }
    }

    private static volatile @Nullable Snapshot current;

    private CalendarClientStore() {}

    public static void setFrom(CalendarSyncPayload payload) {
        current = new Snapshot(
                payload.worldDay(),
                payload.year(),
                payload.monthIndex(),
                payload.dayOfMonth(),
                payload.dayOfYear(),
                payload.dayOfWeek(),
                payload.monthKey(),
                payload.monthFallback(),
                payload.profileKey(),
                payload.profileFallback(),
                payload.seasonKey()
        );
    }

    @Nullable
    public static Snapshot get() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}
