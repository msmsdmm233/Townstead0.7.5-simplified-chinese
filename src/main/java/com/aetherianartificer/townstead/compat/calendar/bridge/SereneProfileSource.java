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
 * Synthesizes the {@code townstead_calendar:serene} profile from Serene
 * Seasons' <em>live</em> configuration so the calendar grid follows whatever
 * {@code sub_season_duration} the player set, rather than the fixed 8-day
 * months baked into {@code serene.json}. The 12 sub-seasons become 12 months,
 * each {@code subSeasonDays} long; names stay Townstead's (resolved from the
 * shared {@code calendar_profile.townstead_calendar.serene.month.*} keys, whose
 * fallbacks the sync layer fills from the data-pack lang sidecar).
 *
 * <p>Returns empty (so the static {@code serene.json} takes over) when Serene
 * isn't loaded or its config can't be read. Shadows the JSON registry entry via
 * {@link com.aetherianartificer.townstead.calendar.TownsteadCalendar#activeProfile},
 * which consults dynamic sources first.</p>
 *
 * <p>Serene has no weekday concept; {@code daysPerWeek = 6} is purely a grid
 * wrapping width (matching the static profile) and the UI shows numeric column
 * headers.</p>
 */
public final class SereneProfileSource implements DynamicProfileSource {

    private static final String NAME_KEY = "calendar_profile.townstead_calendar.serene.name";
    private static final String MONTH_KEY_PREFIX = "calendar_profile.townstead_calendar.serene.month.";
    private static final String[] MONTH_SHORT_NAMES = {
            "early_spring", "mid_spring", "late_spring",
            "early_summer", "mid_summer", "late_summer",
            "early_autumn", "mid_autumn", "late_autumn",
            "early_winter", "mid_winter", "late_winter"
    };

    @Override
    public Optional<CalendarProfile> tryBuild(ResourceLocation id) {
        if (!CalendarCompat.sereneId().equals(id)) return Optional.empty();
        if (!ModCompat.isLoaded(CalendarCompat.SERENE_MOD_ID)) return Optional.empty();
        OptionalInt subSeasonDays = SereneBridge.configSubSeasonDays();
        if (subSeasonDays.isEmpty()) return Optional.empty();
        int days = subSeasonDays.getAsInt();

        List<MonthDef> months = new ArrayList<>(MONTH_SHORT_NAMES.length);
        for (String shortName : MONTH_SHORT_NAMES) {
            months.add(new MonthDef(Component.translatable(MONTH_KEY_PREFIX + shortName), days));
        }
        return Optional.of(new CalendarProfile(id, Component.translatable(NAME_KEY), 6, months));
    }

    @Override
    public Set<ResourceLocation> knownIds() {
        return ModCompat.isLoaded(CalendarCompat.SERENE_MOD_ID)
                ? Set.of(CalendarCompat.sereneId())
                : Set.of();
    }
}
