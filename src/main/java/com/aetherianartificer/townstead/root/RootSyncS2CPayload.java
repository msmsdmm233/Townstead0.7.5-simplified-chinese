package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.Townstead;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: the current origin id of a target. {@code entityId == -1}
 * is the player's own origin; otherwise a villager. Updates
 * {@code RootClientStore}.
 */
//? if neoforge {
public record RootSyncS2CPayload(int entityId, String rootId) implements CustomPacketPayload {
//?} else {
/*public record RootSyncS2CPayload(int entityId, String rootId) {
*///?}

    //? if neoforge {
    public static final Type<RootSyncS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "origin_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, RootSyncS2CPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> p.write(buf), RootSyncS2CPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    //?}

    //? if neoforge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "origin_sync_s2c");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "origin_sync_s2c");
    *///?}

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(rootId);
    }

    public static RootSyncS2CPayload read(FriendlyByteBuf buf) {
        return new RootSyncS2CPayload(buf.readInt(), buf.readUtf());
    }
}
