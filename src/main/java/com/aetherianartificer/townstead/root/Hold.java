package com.aetherianartificer.townstead.root;

/**
 * A species' held-item anchoring: per hand, which rig bone the item is gripped from, plus an
 * offset (model pixels) and rotation (degrees) nudge on top of the standard grip frame. Authored as
 * {@code "hold": { "mainhand": { "bone": "right_arm", "offset": [x,y,z], "rotation": [x,y,z] }, ... }}.
 *
 * <p>A null {@link Grip} (hand key omitted) means that hand cannot hold, so its item is not
 * rendered. {@link #NONE} (both hands null) means the rig holds nothing. The bone name is resolved
 * against the rig model's baked parts client-side by the rig render layer; for a wolf both hands
 * would name {@code "head"}, for a spider a front leg, and so on.</p>
 */
public record Hold(Grip mainhand, Grip offhand) {

    public static final Hold NONE = new Hold(null, null);

    /**
     * One hand's grip: the anchor bone plus an offset (pixels) and rotation (degrees) nudge for the
     * third-person item, and a separate {@code fpOffset}/{@code fpRotation} that seat the bone into the
     * first-person view when a non-humanoid rig draws it as the held-item arm (a humanoid rig ignores
     * them, drawing its own arm). Authored as {@code "first_person": { "offset": [x,y,z], "rotation":
     * [x,y,z] }} inside the hand's grip object.
     */
    public record Grip(String bone, float[] offset, float[] rotation, float[] fpOffset, float[] fpRotation) {
        public Grip {
            offset = offset == null || offset.length < 3 ? new float[]{0f, 0f, 0f} : offset;
            rotation = rotation == null || rotation.length < 3 ? new float[]{0f, 0f, 0f} : rotation;
            fpOffset = fpOffset == null || fpOffset.length < 3 ? new float[]{0f, 0f, 0f} : fpOffset;
            fpRotation = fpRotation == null || fpRotation.length < 3 ? new float[]{0f, 0f, 0f} : fpRotation;
        }
    }
}
