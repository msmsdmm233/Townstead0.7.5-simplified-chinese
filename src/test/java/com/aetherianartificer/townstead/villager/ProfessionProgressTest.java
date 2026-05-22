package com.aetherianartificer.townstead.villager;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfessionProgressTest {

    /** In-memory {@link ProfessionXpStore} so the engine is tested without a full villager. */
    private static final class FakeStore implements ProfessionXpStore {
        private final Map<String, ProfessionXp> map = new HashMap<>();

        @Override
        public ProfessionXp professionXp(String professionId) {
            return map.getOrDefault(professionId, ProfessionXp.EMPTY);
        }

        @Override
        public void setProfessionXp(String professionId, ProfessionXp value) {
            map.put(professionId, value);
        }
    }

    /** Seed raw XP with tier 0 so getTier recomputes from XP (mirrors the legacy "force recalc" path). */
    private static void setXp(ProfessionXpStore store, int xp) {
        store.setProfessionXp(ProfessionXpType.COOK.id(), new ProfessionXp(xp, 0, 0L, 0L, 0));
    }

    @Test
    void tierThresholds() {
        FakeStore store = new FakeStore();
        assertEquals(1, ProfessionProgress.getTier(store, ProfessionXpType.COOK));

        setXp(store, 109);
        assertEquals(1, ProfessionProgress.getTier(store, ProfessionXpType.COOK));

        setXp(store, 110);
        assertEquals(2, ProfessionProgress.getTier(store, ProfessionXpType.COOK));

        setXp(store, 300);
        assertEquals(3, ProfessionProgress.getTier(store, ProfessionXpType.COOK));

        setXp(store, 660);
        assertEquals(4, ProfessionProgress.getTier(store, ProfessionXpType.COOK));

        setXp(store, 1250);
        assertEquals(5, ProfessionProgress.getTier(store, ProfessionXpType.COOK));
    }

    @Test
    void dailyXpCap() {
        FakeStore store = new FakeStore();
        long gameTime = 24000L; // day 1

        assertEquals(230, ProfessionProgress.addXp(store, ProfessionXpType.COOK, 230, gameTime).appliedXp());
        assertEquals(0, ProfessionProgress.addXp(store, ProfessionXpType.COOK, 10, gameTime).appliedXp());
    }

    @Test
    void dayRolloverResetsDailyCounter() {
        FakeStore store = new FakeStore();
        long day1 = 24000L;

        ProfessionProgress.addXp(store, ProfessionXpType.COOK, 230, day1);
        assertEquals(0, ProfessionProgress.addXp(store, ProfessionXpType.COOK, 10, day1).appliedXp());

        long day2 = 48000L;
        assertEquals(10, ProfessionProgress.addXp(store, ProfessionXpType.COOK, 10, day2).appliedXp());
    }

    @Test
    void getXpToNextTier() {
        FakeStore store = new FakeStore();
        // tier 1, xp 0 -> next at 110
        assertEquals(110, ProfessionProgress.getXpToNextTier(store, ProfessionXpType.COOK));

        setXp(store, 50);
        assertEquals(60, ProfessionProgress.getXpToNextTier(store, ProfessionXpType.COOK));

        setXp(store, 1250);
        assertEquals(0, ProfessionProgress.getXpToNextTier(store, ProfessionXpType.COOK)); // tier 5
    }

    @Test
    void addXpReturnsTierUpFlag() {
        FakeStore store = new FakeStore();
        ProfessionProgress.GainResult r = ProfessionProgress.addXp(store, ProfessionXpType.COOK, 110, 24000L);
        assertTrue(r.tierUp());
        assertEquals(1, r.tierBefore());
        assertEquals(2, r.tierAfter());
    }

    @Test
    void addXpNoTierUp() {
        FakeStore store = new FakeStore();
        ProfessionProgress.GainResult r = ProfessionProgress.addXp(store, ProfessionXpType.COOK, 5, 24000L);
        assertFalse(r.tierUp());
        assertEquals(1, r.tierBefore());
        assertEquals(1, r.tierAfter());
    }

    @Test
    void maxXpCap() {
        FakeStore store = new FakeStore();
        store.setProfessionXp(ProfessionXpType.COOK.id(), new ProfessionXp(199990, 5, 0L, 0L, 0));

        ProfessionProgress.GainResult r = ProfessionProgress.addXp(store, ProfessionXpType.COOK, 100, 24000L);
        // appliedXp reflects what passed the daily cap, not the MAX_XP clamp
        assertEquals(100, r.appliedXp());
        assertEquals(200000, ProfessionProgress.getXp(store, ProfessionXpType.COOK));
    }
}
