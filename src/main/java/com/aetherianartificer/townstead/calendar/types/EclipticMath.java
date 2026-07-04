package com.aetherianartificer.townstead.calendar.types;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.calendar.CalendarDate;
import com.aetherianartificer.townstead.calendar.CalendarProfile;
import com.aetherianartificer.townstead.calendar.CalendarType;
import com.aetherianartificer.townstead.calendar.Season;
import com.aetherianartificer.townstead.calendar.WorldCalendarSavedData;
import com.aetherianartificer.townstead.compat.calendar.bridge.EclipticBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Ecliptic-authoritative calendar math. Both the <em>structure</em> (24
 * Solar-Term months of Ecliptic's configured {@code lastingDaysOfEachTerm})
 * and TODAY's <em>position</em> (day-of-year, term, day-of-term, season) are
 * read live from Ecliptic's own {@code solarTermsDay} counter at query time,
 * so the Townstead calendar exactly matches Ecliptic's display. Per
 * [[feedback_seasons_only_from_mods]] the season and cycle position MUST come
 * from the mod, never invented by Townstead.
 *
 * <p><b>Year.</b> Unlike Serene, Ecliptic keeps a monotonic absolute day
 * ({@code solarTermsDay}, ridden off {@code dayTime} at 24000/day) and a real
 * solar year, so no external cycle counting is needed — {@code solarTermsDay}
 * itself is the anchor and the year falls straight out of it.</p>
 *
 * <p><b>Historical dates</b> (villager DOB, village founding) are extrapolated
 * backward from today through the same uniform 24-term layout: absolute day =
 * Ecliptic's absolute day − (today − requested) worldDay delta. Season is left
 * null for any day that isn't today (Ecliptic only reports "now").</p>
 *
 * <p>Falls back to plain {@link VanillaMath} (the static {@code ecliptic.json}
 * layout) whenever Ecliptic's state can't be read — mod absent, dimension not
 * whitelisted, save data not yet initialized.</p>
 */
public class EclipticMath implements CalendarType {
    //? if >=1.21 {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "ecliptic_math");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "ecliptic_math");
    *///?}

    private static final int TERMS_PER_YEAR = 24;

    private final VanillaMath delegate = new VanillaMath();

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public CalendarDate compute(MinecraftServer server, CalendarProfile profile, long worldDay, int epochYearOffset) {
        CalendarDate base = delegate.compute(server, profile, worldDay, epochYearOffset);
        if (server == null) return base;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return base;
        Optional<EclipticBridge.EclipticState> live = EclipticBridge.currentState(overworld);
        if (live.isEmpty()) return base;
        EclipticBridge.EclipticState s = live.get();
        int termLen = s.termLength();
        if (termLen <= 0) return base;
        int daysPerYear = TERMS_PER_YEAR * termLen;

        WorldCalendarSavedData data = WorldCalendarSavedData.get(server);
        long counterToday = data.worldDayCounter();
        // Ecliptic's solarTermsDay is its own monotonic absolute day; anchor to it.
        long absToday = s.absDay();
        // The requested worldDay is a whole-day offset from today; history is a
        // uniform backward extrapolation of the live layout.
        long abs = absToday - (counterToday - worldDay);

        int year = (int) Math.floorDiv(abs, (long) daysPerYear) + epochYearOffset;
        int dayOfYear0 = (int) Math.floorMod(abs, (long) daysPerYear);
        int termIndex0 = dayOfYear0 / termLen;                   // 0..23
        int dayOfMonth = dayOfYear0 - termIndex0 * termLen + 1;  // 1-based
        int dpw = Math.max(1, profile.daysPerWeek());
        int dayOfWeek = (int) Math.floorMod(abs, (long) dpw);

        // Season is authoritative only for today; the past isn't recoverable.
        Season season = (worldDay == counterToday) ? s.season() : null;

        return new CalendarDate(year, termIndex0 + 1, dayOfMonth, dayOfWeek, dayOfYear0 + 1, season);
    }
}
