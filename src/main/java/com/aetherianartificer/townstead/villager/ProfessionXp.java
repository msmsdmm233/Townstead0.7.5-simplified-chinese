package com.aetherianartificer.townstead.villager;

/**
 * Immutable per-profession XP state. Persisted inside
 * {@link TownsteadVillager.ProfessionMemory} and mutated only through
 * {@link ProfessionProgress}.
 */
public record ProfessionXp(int xp, int tier, long lastTierUpTick, long xpDay, int xpToday) {
    public static final ProfessionXp EMPTY = new ProfessionXp(0, 0, 0L, 0L, 0);

    public boolean isEmpty() {
        return xp == 0 && tier == 0 && lastTierUpTick == 0L && xpDay == 0L && xpToday == 0;
    }

    public ProfessionXp withTier(int newTier) {
        return new ProfessionXp(xp, newTier, lastTierUpTick, xpDay, xpToday);
    }
}
