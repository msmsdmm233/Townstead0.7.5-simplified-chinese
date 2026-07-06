package com.aetherianartificer.townstead.root.attachment;

import java.util.List;

/**
 * A species attachment point: the anchor bone, a base offset, an optional base
 * orientation, and the tags an attachment can target to fit here ({@code ear},
 * {@code tail_root}, ...). Synced with the manifest.
 *
 * <p>{@code mirror} renders the attachment's geometry re-baked mirrored across X,
 * so one authored ear/wing fits both sides. {@code rig} scopes the point: empty
 * applies to every rig; a rig id applies only to entities resolved to that rig,
 * and a rig-scoped point beats a universal one with the same id (per-rig
 * placement overrides without forking the attachment).</p>
 */
public record AttachmentPointDef(String id, String bone, float[] offset, List<String> tags,
                                 float[] rotation, boolean mirror, String rig) {
    public AttachmentPointDef {
        tags = tags == null ? List.of() : List.copyOf(tags);
        rotation = rotation == null ? new float[3] : rotation;
        rig = rig == null ? "" : rig;
    }
}
