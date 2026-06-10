package com.aetherianartificer.townstead.origin.attachment;

import com.aetherianartificer.townstead.Townstead;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the attachment manifest (every {@link AttachmentDef} and slot,
 * each def carrying the SHA-1 of its geometry and texture). The client diffs this
 * against its on-disk cache and requests only the blobs it lacks.
 */
//? if neoforge {
public record AttachmentManifestS2CPayload(List<AttachmentDef> defs, List<AttachmentSlotDef> slots)
        implements CustomPacketPayload {
//?} else {
/*public record AttachmentManifestS2CPayload(java.util.List<AttachmentDef> defs, java.util.List<AttachmentSlotDef> slots) {
*///?}

    //? if neoforge {
    public static final Type<AttachmentManifestS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "attachment_manifest_s2c"));

    public static final StreamCodec<FriendlyByteBuf, AttachmentManifestS2CPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> p.write(buf), AttachmentManifestS2CPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    //?}

    //? if neoforge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "attachment_manifest_s2c");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "attachment_manifest_s2c");
    *///?}

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(defs.size());
        for (AttachmentDef def : defs) {
            buf.writeUtf(def.id());
            buf.writeUtf(def.geoSha1());
            buf.writeUtf(def.textureSha1());
            buf.writeUtf(def.slot() == null ? "" : def.slot());
            buf.writeUtf(def.bone());
            writeVec(buf, def.offset());
            writeVec(buf, def.rotation());
            buf.writeFloat(def.scale());
            buf.writeInt(def.tint());
        }
        buf.writeVarInt(slots.size());
        for (AttachmentSlotDef slot : slots) {
            buf.writeUtf(slot.id());
            buf.writeUtf(slot.bone());
            writeVec(buf, slot.offset());
        }
    }

    public static AttachmentManifestS2CPayload read(FriendlyByteBuf buf) {
        int defCount = buf.readVarInt();
        List<AttachmentDef> defs = new ArrayList<>(defCount);
        for (int i = 0; i < defCount; i++) {
            String id = buf.readUtf();
            String geo = buf.readUtf();
            String tex = buf.readUtf();
            String slot = buf.readUtf();
            String bone = buf.readUtf();
            float[] offset = readVec(buf);
            float[] rotation = readVec(buf);
            float scale = buf.readFloat();
            int tint = buf.readInt();
            defs.add(new AttachmentDef(id, geo, tex, slot.isEmpty() ? null : slot, bone, offset, rotation, scale, tint));
        }
        int slotCount = buf.readVarInt();
        List<AttachmentSlotDef> slots = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            slots.add(new AttachmentSlotDef(buf.readUtf(), buf.readUtf(), readVec(buf)));
        }
        return new AttachmentManifestS2CPayload(defs, slots);
    }

    private static void writeVec(FriendlyByteBuf buf, float[] v) {
        buf.writeFloat(v[0]);
        buf.writeFloat(v[1]);
        buf.writeFloat(v[2]);
    }

    private static float[] readVec(FriendlyByteBuf buf) {
        return new float[]{buf.readFloat(), buf.readFloat(), buf.readFloat()};
    }
}
