package com.aetherianartificer.townstead.compat.calendar.bridge;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.calendar.Season;
import com.aetherianartificer.townstead.compat.ModCompat;
import com.aetherianartificer.townstead.compat.calendar.CalendarCompat;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Reflection-only bridge to Ecliptic Seasons' public API
 * ({@code com.teamtea.eclipticseasons.api}). The API is stable across the
 * Forge 1.20.1 and NeoForge 1.21.x branches; 1.21.x adds methods on top
 * (we don't depend on those).
 *
 * Strategy: read the current SolarTerm via
 * {@code EclipticUtil.getNowSolarTerm(Level)}, then ask the term what season
 * it belongs to. Falls back to {@code EclipticUtil.INSTANCE.getSeason(level)}
 * if the term-to-season accessor isn't present.
 */
public final class EclipticBridge {

    private static volatile boolean probeAttempted = false;
    private static volatile boolean probeOk = false;
    private static Method getNowSolarTerm;
    private static Method solarTermGetSeason;
    private static Object eclipticUtilInstance;
    private static Method instanceGetSeason;
    private static Method getNowSolarDay;
    private static Field lastingDaysField;
    private static Method configValueGet;
    private static Map<String, Season> seasonByName;

    private EclipticBridge() {}

    /**
     * Live cycle state for {@code level}. {@code absDay} is Ecliptic's own
     * monotonic {@code solarTermsDay} counter (rides {@code dayTime} at
     * 24000/day, same as Townstead's counter); {@code termLength} is the
     * configured {@code lastingDaysOfEachTerm}; {@code season} is nullable.
     * Unlike Serene, Ecliptic tracks a real year and an absolute day, so no
     * external cycle counting is needed — the anchor is {@code absDay} itself.
     */
    public record EclipticState(int absDay, int termLength, Season season) {}

    public static Optional<Season> currentSeason(ServerLevel level) {
        if (level == null) return Optional.empty();
        if (!ModCompat.isLoaded(CalendarCompat.ECLIPTIC_MOD_ID)) return Optional.empty();
        if (!ensureProbe()) return Optional.empty();
        try {
            if (getNowSolarTerm != null && solarTermGetSeason != null) {
                Object term = getNowSolarTerm.invoke(null, level);
                if (term != null) {
                    Object season = solarTermGetSeason.invoke(term);
                    if (season instanceof Enum<?> e) {
                        return Optional.ofNullable(seasonByName.get(e.name()));
                    }
                }
            }
            if (instanceGetSeason != null && eclipticUtilInstance != null) {
                Object season = instanceGetSeason.invoke(eclipticUtilInstance, level);
                if (season instanceof Enum<?> e) {
                    return Optional.ofNullable(seasonByName.get(e.name()));
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return Optional.empty();
    }

    /**
     * Live state (absolute day + configured term length + season) for
     * {@code level}, or empty when Ecliptic isn't present, its save data isn't
     * ready (current term is {@code NONE}), or the API can't be read. Used by
     * {@code EclipticMath} so the displayed date tracks Ecliptic's own clock.
     */
    public static Optional<EclipticState> currentState(ServerLevel level) {
        if (level == null) return Optional.empty();
        if (!ModCompat.isLoaded(CalendarCompat.ECLIPTIC_MOD_ID)) return Optional.empty();
        if (!ensureProbe()) return Optional.empty();
        if (getNowSolarTerm == null || getNowSolarDay == null) return Optional.empty();
        try {
            Object term = getNowSolarTerm.invoke(null, level);
            if (term == null) return Optional.empty();
            // NONE = no save data yet; treat as unavailable and fall back.
            if (term instanceof Enum<?> te && "NONE".equals(te.name())) return Optional.empty();
            OptionalInt termLen = configTermLength();
            if (termLen.isEmpty()) return Optional.empty();
            int absDay = ((Number) getNowSolarDay.invoke(null, level)).intValue();
            Season season = null;
            if (solarTermGetSeason != null) {
                Object seasonObj = solarTermGetSeason.invoke(term);
                if (seasonObj instanceof Enum<?> se) season = seasonByName.get(se.name());
            }
            return Optional.of(new EclipticState(absDay, termLen.getAsInt(), season));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    /**
     * Ecliptic's configured Solar-Term length in days
     * ({@code lastingDaysOfEachTerm}), read from the config value with no level
     * required. Empty when Ecliptic isn't present. Used by
     * {@code EclipticProfileSource} to size the 24 term "months".
     */
    public static OptionalInt configTermLength() {
        if (!ModCompat.isLoaded(CalendarCompat.ECLIPTIC_MOD_ID)) return OptionalInt.empty();
        if (!ensureProbe()) return OptionalInt.empty();
        if (lastingDaysField == null) return OptionalInt.empty();
        try {
            Object value = lastingDaysField.get(null);
            if (value == null) return OptionalInt.empty();
            if (configValueGet == null) configValueGet = value.getClass().getMethod("get");
            Object got = configValueGet.invoke(value);
            int termLen = ((Number) got).intValue();
            return termLen > 0 ? OptionalInt.of(termLen) : OptionalInt.empty();
        } catch (Throwable ignored) {
            return OptionalInt.empty();
        }
    }

    private static synchronized boolean ensureProbe() {
        if (probeAttempted) return probeOk;
        probeAttempted = true;
        try {
            Class<?> util = Class.forName("com.teamtea.eclipticseasons.api.util.EclipticUtil");
            Class<?> levelCls = Class.forName("net.minecraft.world.level.Level");
            try {
                getNowSolarTerm = util.getMethod("getNowSolarTerm", levelCls);
            } catch (NoSuchMethodException ignored) {}

            try {
                Class<?> solarTermCls = Class.forName("com.teamtea.eclipticseasons.api.constant.solar.SolarTerm");
                try {
                    solarTermGetSeason = solarTermCls.getMethod("getSeason");
                } catch (NoSuchMethodException ignored) {}
            } catch (Throwable ignored) {}

            try {
                getNowSolarDay = util.getMethod("getNowSolarDay", levelCls);
            } catch (NoSuchMethodException ignored) {}

            try {
                Class<?> seasonCfg = Class.forName("com.teamtea.eclipticseasons.config.CommonConfig$Season");
                lastingDaysField = seasonCfg.getField("lastingDaysOfEachTerm");
            } catch (Throwable ignored) {}

            try {
                java.lang.reflect.Field inst = util.getField("INSTANCE");
                eclipticUtilInstance = inst.get(null);
            } catch (Throwable ignored) {}
            if (eclipticUtilInstance != null) {
                try {
                    instanceGetSeason = util.getMethod("getSeason", levelCls);
                } catch (NoSuchMethodException ignored) {}
            }

            seasonByName = new HashMap<>();
            seasonByName.put("SPRING", Season.SPRING);
            seasonByName.put("SUMMER", Season.SUMMER);
            seasonByName.put("AUTUMN", Season.AUTUMN);
            seasonByName.put("WINTER", Season.WINTER);

            probeOk = getNowSolarTerm != null || instanceGetSeason != null;
        } catch (Throwable t) {
            Townstead.LOGGER.info("[Calendar] Ecliptic Seasons bridge unavailable: {}", t.getMessage());
            probeOk = false;
        }
        return probeOk;
    }
}
