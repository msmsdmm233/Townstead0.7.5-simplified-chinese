package com.aetherianartificer.townstead.villager;

/**
 * Minimal storage seam for {@link ProfessionProgress}. Implemented by
 * {@link TownsteadVillager.ProfessionMemory}; lets the XP engine be exercised
 * without constructing a full villager.
 */
public interface ProfessionXpStore {
    /** Current XP for a profession id, or {@link ProfessionXp#EMPTY} if none. */
    ProfessionXp professionXp(String professionId);

    /** Replace the stored XP for a profession id. */
    void setProfessionXp(String professionId, ProfessionXp value);
}
