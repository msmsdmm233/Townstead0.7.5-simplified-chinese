package com.aetherianartificer.townstead.calendar;

import com.aetherianartificer.townstead.Townstead;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client snapshot of today's calendar state. Broadcast on player
 * login and on every day rollover (in {@link WorldCalendarTicker}).
 *
 * Text fields are carried as (translate key, fallback) pairs rather than
 * pre-resolved strings so each client resolves to its own locale. The client
 * reconstructs Components via
 * {@link CalendarClientStore.Snapshot#monthComponent()} etc. Empty key means
 * "use fallback as literal"; empty fallback means "no override, just the key."
 */
public record CalendarSyncPayload(
        long worldDay,
        int year,
        int monthIndex,
        int dayOfMonth,
        int dayOfYear,
        int dayOfWeek,
        String monthKey,
        String monthFallback,
        String profileKey,
        String profileFallback,
        String seasonKey
//? if neoforge {
) implements CustomPacketPayload {
//?} else {
/*) {
*///?}

    //? if neoforge {
    public static final Type<CalendarSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "calendar_sync"));

    public static final StreamCodec<FriendlyByteBuf, CalendarSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.worldDay());
                buf.writeVarInt(p.year());
                buf.writeVarInt(p.monthIndex());
                buf.writeVarInt(p.dayOfMonth());
                buf.writeVarInt(p.dayOfYear());
                buf.writeVarInt(p.dayOfWeek());
                buf.writeUtf(p.monthKey());
                buf.writeUtf(p.monthFallback());
                buf.writeUtf(p.profileKey());
                buf.writeUtf(p.profileFallback());
                buf.writeUtf(p.seasonKey());
            },
            buf -> new CalendarSyncPayload(
                    buf.readLong(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    //?}

    //? if neoforge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "calendar_sync");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "calendar_sync");
    *///?}

    //? if forge {
    /*public void write(FriendlyByteBuf buf) {
        buf.writeLong(worldDay);
        buf.writeVarInt(year);
        buf.writeVarInt(monthIndex);
        buf.writeVarInt(dayOfMonth);
        buf.writeVarInt(dayOfYear);
        buf.writeVarInt(dayOfWeek);
        buf.writeUtf(monthKey);
        buf.writeUtf(monthFallback);
        buf.writeUtf(profileKey);
        buf.writeUtf(profileFallback);
        buf.writeUtf(seasonKey);
    }

    public static CalendarSyncPayload read(FriendlyByteBuf buf) {
        return new CalendarSyncPayload(
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf()
        );
    }
    *///?}
}
