package com.aetherianartificer.townstead.villager;

/**
 * Shared profession-XP engine. Operates on the typed {@link ProfessionXp}
 * held behind a {@link ProfessionXpStore} (implemented by
 * {@link TownsteadVillager.ProfessionMemory}), parameterised by
 * {@link ProfessionXpType}.
 *
 * <p>Behaviour matches the former per-profession {@code *ProgressData} classes:
 * a daily XP cap, five XP-gated tiers, and a tier-up timestamp. {@link #getTier}
 * lazily backfills the stored tier from XP (for legacy/uninitialised data),
 * persisting the result like the originals did.
 */
public final class ProfessionProgress {
    private ProfessionProgress() {}

    public static int getXp(ProfessionXpStore store, ProfessionXpType type) {
        return Math.max(0, store.professionXp(type.id()).xp());
    }

    public static int getTier(ProfessionXpStore store, ProfessionXpType type) {
        ProfessionXp state = store.professionXp(type.id());
        int raw = state.tier();
        if (raw <= 0) {
            raw = type.tierForXp(Math.max(0, state.xp()));
            store.setProfessionXp(type.id(), state.withTier(raw));
        }
        return Math.max(1, Math.min(5, raw));
    }

    public static long getLastTierUpTick(ProfessionXpStore store, ProfessionXpType type) {
        return store.professionXp(type.id()).lastTierUpTick();
    }

    public static int getXpToNextTier(ProfessionXpStore store, ProfessionXpType type) {
        int tier = getTier(store, type);
        if (tier >= 5) return 0;
        int xp = getXp(store, type);
        return Math.max(0, type.thresholdForTier(tier) - xp);
    }

    public static GainResult addXp(ProfessionXpStore store, ProfessionXpType type, int requested, long gameTime) {
        int beforeTier = getTier(store, type);
        if (requested <= 0) return new GainResult(0, beforeTier, beforeTier, false);

        ProfessionXp state = store.professionXp(type.id());
        long day = gameTime / 24000L;
        long storedDay = state.xpDay();
        int gainedToday = Math.max(0, state.xpToday());
        if (storedDay != day) {
            storedDay = day;
            gainedToday = 0;
        }

        int allowance = Math.max(0, type.dailyXpCap() - gainedToday);
        int applied = Math.min(requested, allowance);
        if (applied <= 0) {
            store.setProfessionXp(type.id(),
                    new ProfessionXp(state.xp(), state.tier(), state.lastTierUpTick(), storedDay, gainedToday));
            return new GainResult(0, beforeTier, beforeTier, false);
        }

        int xp = Math.max(0, Math.min(type.maxXp(), state.xp() + applied));
        gainedToday += applied;
        int afterTier = type.tierForXp(xp);
        boolean tierUp = afterTier > beforeTier;
        long lastTierUpTick = tierUp ? gameTime : state.lastTierUpTick();

        store.setProfessionXp(type.id(), new ProfessionXp(xp, afterTier, lastTierUpTick, storedDay, gainedToday));
        return new GainResult(applied, beforeTier, afterTier, tierUp);
    }

    public record GainResult(int appliedXp, int tierBefore, int tierAfter, boolean tierUp) {}
}
