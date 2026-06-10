package com.aetherianartificer.townstead.origin.attachment;

import org.jetbrains.annotations.Nullable;

/**
 * The synced form of an attachment: render placement plus the SHA-1 hashes of its
 * geometry and texture blobs (the bytes are content-addressed and pulled/cached
 * separately). Authored as {@code data/<ns>/attachments/<id>.json}; the server
 * resolves the geometry/texture file references to hashes and syncs this view.
 */
public record AttachmentDef(
        String id,
        String geoSha1,
        String textureSha1,
        @Nullable String slot,
        String bone,
        float[] offset,
        float[] rotation,
        float scale,
        int tint
) {}
