package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.Townstead;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;

/**
 * Client → server: set a target's origin. {@code entityId == -1} targets the
 * sending player (their {@link PlayerRoot}); otherwise the loaded villager
 * with that network id. An empty {@code rootId} is a request only — the server
 * replies with the current value via {@link RootSyncS2CPayload} and changes
 * nothing.
 */
//? if neoforge {
public record RootSetC2SPayload(int entityId, String rootId) implements CustomPacketPayload {
//?} else {
/*public record RootSetC2SPayload(int entityId, String rootId) {
*///?}

    public static final int SELF = -1;

    /** No valid target (e.g. the edited villager couldn't be resolved client-side); the server ignores it. */
    public static final int NONE = Integer.MIN_VALUE;

    //? if neoforge {
    public static final Type<RootSetC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "origin_set_c2s"));

    public static final StreamCodec<FriendlyByteBuf, RootSetC2SPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> p.write(buf), RootSetC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    //?}

    //? if neoforge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "origin_set_c2s");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "origin_set_c2s");
    *///?}

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(rootId);
    }

    public static RootSetC2SPayload read(FriendlyByteBuf buf) {
        return new RootSetC2SPayload(buf.readInt(), buf.readUtf());
    }
}
