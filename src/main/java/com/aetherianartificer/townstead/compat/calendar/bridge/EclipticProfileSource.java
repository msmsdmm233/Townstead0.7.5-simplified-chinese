package com.aetherianartificer.townstead.compat.calendar.bridge;

import com.aetherianartificer.townstead.calendar.CalendarProfile;
import com.aetherianartificer.townstead.calendar.DynamicProfileSource;
import com.aetherianartificer.townstead.calendar.MonthDef;
import com.aetherianartificer.townstead.compat.ModCompat;
import com.aetherianartificer.townstead.compat.calendar.CalendarCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Synthesizes the {@code townstead_calendar:ecliptic} profile from Ecliptic
 * Seasons' <em>live</em> configuration so the calendar grid follows whatever
 * {@code lastingDaysOfEachTerm} the player set, rather than the fixed 15-day
 * terms baked into {@code ecliptic.json}. The 24 Solar Terms become 24 months,
 * each {@code termLength} long.
 *
 * <p><b>Names come from Ecliptic's own translation keys</b>
 * ({@code info.eclipticseasons.environment.solar_term.*}) so the term names
 * render exactly as Ecliptic's in-game calendar shows them ("Spring Equinox",
 * "Beginning of Spring", …) and follow the client's locale — not Townstead's
 * Pinyin romanizations. Each carries an English fallback for the rare client
 * that has the profile but not Ecliptic's assets.</p>
 *
 * <p>Returns empty (so the static {@code ecliptic.json} takes over) when
 * Ecliptic isn't loaded or its config can't be read. Shadows the JSON registry
 * entry via
 * {@link com.aetherianartificer.townstead.calendar.TownsteadCalendar#activeProfile},
 * which consults dynamic sources first.</p>
 *
 * <p>Ecliptic has no weekday concept; {@code daysPerWeek = 7} is purely a grid
 * wrapping width (matching the static profile) and the UI shows numeric column
 * headers.</p>
 */
public final class EclipticProfileSource implements DynamicProfileSource {

    private static final int TERMS_PER_YEAR = 24;
    private static final String NAME_KEY = "calendar_profile.townstead_calendar.ecliptic.name";
    private static final String TERM_KEY_PREFIX = "info.eclipticseasons.environment.solar_term.";

    /** Ecliptic's SolarTerm enum order (index 0..23), snake_case = lang suffix. */
    private static final String[] TERM_NAMES = {
            "beginning_of_spring", "rain_water", "insects_awakening", "spring_equinox", "fresh_green", "grain_rain",
            "beginning_of_summer", "lesser_fullness", "grain_in_ear", "summer_solstice", "lesser_heat", "greater_heat",
            "beginning_of_autumn", "end_of_heat", "white_dew", "autumnal_equinox", "cold_dew", "first_frost",
            "beginning_of_winter", "light_snow", "heavy_snow", "winter_solstice", "lesser_cold", "greater_cold"
    };

    /** English fallbacks (as Ecliptic renders them) for clients lacking its assets. */
    private static final String[] TERM_FALLBACKS = {
            "Beginning of Spring", "Rain Water", "Insects Awakening", "Spring Equinox", "Fresh Green", "Grain Rain",
            "Beginning of Summer", "Lesser Fullness", "Grain in Ear", "Summer Solstice", "Lesser Heat", "Greater Heat",
            "Beginning of Autumn", "End of Heat", "White Dew", "Autumnal Equinox", "Cold Dew", "First Frost",
            "Beginning of Winter", "Light Snow", "Heavy Snow", "Winter Solstice", "Lesser Cold", "Greater Cold"
    };

    @Override
    public Optional<CalendarProfile> tryBuild(ResourceLocation id) {
        if (!CalendarCompat.eclipticId().equals(id)) return Optional.empty();
        if (!ModCompat.isLoaded(CalendarCompat.ECLIPTIC_MOD_ID)) return Optional.empty();
        OptionalInt termLength = EclipticBridge.configTermLength();
        if (termLength.isEmpty()) return Optional.empty();
        int days = termLength.getAsInt();

        List<MonthDef> months = new ArrayList<>(TERMS_PER_YEAR);
        for (int i = 0; i < TERMS_PER_YEAR; i++) {
            months.add(new MonthDef(
                    Component.translatableWithFallback(TERM_KEY_PREFIX + TERM_NAMES[i], TERM_FALLBACKS[i]),
                    days));
        }
        return Optional.of(new CalendarProfile(id, Component.translatable(NAME_KEY), 7, months));
    }

    @Override
    public Set<ResourceLocation> knownIds() {
        return ModCompat.isLoaded(CalendarCompat.ECLIPTIC_MOD_ID)
                ? Set.of(CalendarCompat.eclipticId())
                : Set.of();
    }
}
