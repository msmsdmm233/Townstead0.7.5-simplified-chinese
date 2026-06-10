package com.aetherianartificer.townstead.origin.attachment;

/** A species mount point: the anchor bone and a base offset. Synced with the manifest. */
public record AttachmentSlotDef(String id, String bone, float[] offset) {}
