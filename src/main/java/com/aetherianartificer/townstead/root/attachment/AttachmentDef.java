package com.aetherianartificer.townstead.root.attachment;

import org.jetbrains.annotations.Nullable;

/**
 * The synced form of an attachment: render placement plus the SHA-1 hashes of its
 * geometry and texture blobs (the bytes are content-addressed and pulled/cached
 * separately). Authored as {@code data/<ns>/attachment/<id>.json}; the server
 * resolves the geometry/texture file references to hashes and syncs this view.
 *
 * <p>Targeting precedence: {@code targetTag} (every point carrying the tag, so one
 * "ears" attachment fits any rig that exposes an {@code ear} point) -> {@code targetPoint}
 * (one named point) -> {@code bone} (anchor straight to a model bone).</p>
 *
 * <p>{@code skinTint} replaces the flat tint with the bearer's resolved skin tone
 * (author the texture in grayscale). A non-null {@code morphAxes} reads the size
 * value rolled on the gene that granted this attachment and scales the named
 * top-level geometry bones about their own pivots, each axis weighted by
 * {@code morphAxes} (1 = follows the value fully, 0 = unaffected); with no
 * {@code morphBones} the whole attachment scales about its anchor instead.</p>
 */
public record AttachmentDef(
        String id,
        String geoSha1,
        String textureSha1,
        @Nullable String targetTag,
        @Nullable String targetPoint,
        String bone,
        float[] offset,
        float[] rotation,
        float scale,
        int tint,
        boolean skinTint,
        float @Nullable [] morphAxes,
        java.util.List<String> morphBones
) {}
