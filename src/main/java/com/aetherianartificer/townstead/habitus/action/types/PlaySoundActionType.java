package com.aetherianartificer.townstead.habitus.action.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.habitus.action.Action;
import com.aetherianartificer.townstead.habitus.action.ActionType;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;

import java.util.Locale;

/**
 * Plays a sound at the actor (Apoli's {@code play_sound}). The raw {@code SoundEvent}
 * overload of {@code playSound} exists on both branches, so no version guard is needed.
 *
 * <p>JSON: {@code { "type":"townstead_origins:play_sound", "sound":"minecraft:entity.player.levelup",
 * "volume":1.0, "pitch":1.0 }}</p>
 */
public final class PlaySoundActionType implements ActionType {

    public static final String KEY = "townstead_origins:play_sound";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Action parse(JsonObject json) {
        ResourceLocation id = DataPackLang.parseId(GsonHelper.getAsString(json, "sound", ""));
        if (id == null) return null;
        float volume = GsonHelper.getAsFloat(json, "volume", 1.0f);
        float pitch = GsonHelper.getAsFloat(json, "pitch", 1.0f);
        SoundSource source = parseSource(GsonHelper.getAsString(json, "category", "neutral"));
        return ctx -> {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);
            if (sound == null) return;
            ctx.entity().level().playSound(null, ctx.entity().getX(), ctx.entity().getY(), ctx.entity().getZ(),
                    sound, source, volume, pitch);
        };
    }

    private static SoundSource parseSource(String raw) {
        try {
            return SoundSource.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SoundSource.NEUTRAL;
        }
    }
}
