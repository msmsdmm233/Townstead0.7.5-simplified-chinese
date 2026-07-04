package com.aetherianartificer.townstead.calendar.types;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.calendar.CalendarDate;
import com.aetherianartificer.townstead.calendar.CalendarProfile;
import com.aetherianartificer.townstead.calendar.CalendarType;
import com.aetherianartificer.townstead.calendar.Season;
import com.aetherianartificer.townstead.calendar.WorldCalendarSavedData;
import com.aetherianartificer.townstead.compat.calendar.bridge.SereneBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Serene-authoritative calendar math. Both the <em>structure</em> (12
 * sub-season months of Serene's configured {@code sub_season_duration}) and
 * TODAY's <em>position</em> (day-of-year, month, day-of-month, season) are read
 * live from Serene's {@code seasonCycleTicks} at query time, so the Townstead
 * calendar exactly matches Serene's own display. Per
 * [[feedback_seasons_only_from_mods]] the season and cycle position MUST come
 * from the mod, never invented by Townstead.
 *
 * <p><b>Year.</b> Serene tracks no year — its cycle just wraps Late Winter →
 * Early Spring forever. Townstead counts those wraps into
 * {@link WorldCalendarSavedData#sereneCycleCount} (advanced on the daily
 * rollover by {@code WorldCalendarTicker}), giving a year that increments in
 * lockstep with the season reset.</p>
 *
 * <p><b>Historical dates</b> (villager DOB, village founding) can't be
 * recovered from Serene, which only knows "now". They're extrapolated
 * backward from today through the same uniform 12-month layout: absolute day
 * = today's absolute day − (today − requested) worldDay delta. Season is left
 * null for any day that isn't today.</p>
 *
 * <p>Falls back to plain {@link VanillaMath} (the static {@code serene.json}
 * layout) whenever Serene's state can't be read — mod absent, dimension not
 * whitelisted, early startup — so the calendar degrades gracefully.</p>
 */
public class SereneMath implements CalendarType {
    //? if >=1.21 {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "serene_math");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "serene_math");
    *///?}

    private final VanillaMath delegate = new VanillaMath();

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public CalendarDate compute(MinecraftServer server, CalendarProfile profile, long worldDay, int epochYearOffset) {
        CalendarDate base = delegate.compute(server, profile, worldDay, epochYearOffset);
        if (server == null) return base;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return base;
        Optional<SereneBridge.SereneState> live = SereneBridge.currentState(overworld);
        if (live.isEmpty()) return base;
        SereneBridge.SereneState s = live.get();
        int subDays = s.subSeasonDays();
        int daysPerCycle = s.daysPerCycle();
        if (subDays <= 0 || daysPerCycle <= 0) return base;

        WorldCalendarSavedData data = WorldCalendarSavedData.get(server);
        long counterToday = data.worldDayCounter();
        int cycleCount = data.sereneCycleCount();

        // Absolute Serene-day for today, anchored by the persisted cycle count.
        long absToday = (long) cycleCount * daysPerCycle + s.dayOfYear();
        // The requested worldDay is a whole-day offset from today; history is a
        // uniform backward extrapolation of the live layout.
        long abs = absToday - (counterToday - worldDay);

        int year = (int) Math.floorDiv(abs, (long) daysPerCycle) + epochYearOffset;
        int dayOfYear0 = (int) Math.floorMod(abs, (long) daysPerCycle);
        int monthIndex0 = dayOfYear0 / subDays;                  // 0..11
        int dayOfMonth = dayOfYear0 - monthIndex0 * subDays + 1; // 1-based
        int dpw = Math.max(1, profile.daysPerWeek());
        int dayOfWeek = (int) Math.floorMod(abs, (long) dpw);

        // Season is authoritative only for today; the past isn't recoverable.
        Season season = (worldDay == counterToday) ? s.season() : null;

        return new CalendarDate(year, monthIndex0 + 1, dayOfMonth, dayOfWeek, dayOfYear0 + 1, season);
    }
}
