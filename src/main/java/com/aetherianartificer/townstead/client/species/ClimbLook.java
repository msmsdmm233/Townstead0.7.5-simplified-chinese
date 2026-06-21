package com.aetherianartificer.townstead.client.species;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spider gravity, stage 3 (input): drives a decoupled "standing on the wall floor" first-person camera while
 * clung, so turning is intuitive and never sits on the gimbal pole. Reorienting the player's actual gaze does
 * not work, because a climbing player faces into the wall, which the reorientation turns into looking straight
 * up the vertical pole, where any turn reads as roll. Instead this holds its own wall-frame look ({@link
 * #wallYaw}/{@link #wallPitch}) that the mouse drives: neutral is looking along the wall with a level horizon
 * (off the pole), horizontal mouse turns about the wall normal, vertical mouse tilts off the surface. {@link
 * ClimbView} builds the camera from {@link #wallCameraOrientation} and eases into it. Client/local-player only.
 */
public final class ClimbLook {

    private ClimbLook() {}

    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    // Vanilla's per-step turn scale (Entity.turn multiplies the raw delta by this before applying).
    private static final double TURN_SCALE = 0.15;

    // How far the neutral gaze tilts off straight-up-the-wall toward the surface, to sit clear of the
    // look-straight-up singularity (0 = straight up the wall, larger = more downward at the surface).
    private static final float NEUTRAL_DOWN = 0.4f;

    // Wall-frame look, accumulated from the mouse while clung; reset to neutral (up the wall) on attach.
    private static float wallYaw;
    private static float wallPitch;
    private static boolean clung;

    /** Tracks attach/detach so the wall-frame look resets to neutral each time the player latches on. */
    public static void updateClungState(float f) {
        if (f > 0f && !clung) {
            clung = true;
            wallYaw = 0f;
            wallPitch = 0f;
        } else if (f <= 0f && clung) {
            clung = false;
        }
    }

    /**
     * Applies the mouse delta to the wall-frame look for a clung local player. Returns true when it handled
     * the turn (the caller should cancel vanilla's), false to let vanilla run unchanged.
     */
    public static boolean tryTurn(LivingEntity self, double yawDelta, double pitchDelta) {
        Minecraft mc = Minecraft.getInstance();
        int id = self.getId();
        float f = ClimbAnim.factor(id);
        Vector3f up = ClimbAnim.normal(id);
        if (self != mc.player || f <= 0f || up == null) return false;
        // Only the first-person wall-frame camera is driven here; in third person let vanilla turn the body
        // (and the orbit camera) so it isn't frozen.
        if (!mc.options.getCameraType().isFirstPerson()) return false;

        wallYaw -= (float) (yawDelta * TURN_SCALE);
        wallPitch -= (float) (pitchDelta * TURN_SCALE);
        wallPitch = Math.max(-89f, Math.min(89f, wallPitch));
        return true;
    }

    /**
     * The camera-to-world orientation for the wall-frame view (view forward -Z, matching MC's view matrix
     * {@code Rz(roll) Rx(pitch) Ry(yaw+180)}). Up is the wall normal; forward starts along the wall (the
     * horizontal tangent) and is turned/tilted by {@link #wallYaw}/{@link #wallPitch}. For a vertical normal
     * (ceiling) the tangent is degenerate, so the entity facing is used as the forward reference.
     */
    public static Quaternionf wallCameraOrientation(Vector3f n, float entityYaw) {
        Vector3f up = new Vector3f(n).normalize();
        // Up-the-wall tangent (world up projected onto the wall) = the climb direction the neutral gaze faces.
        Vector3f upWall = new Vector3f(0f, 1f, 0f).sub(new Vector3f(up).mul(up.y()));
        if (upWall.lengthSquared() < 1.0e-4f) { // vertical normal (ceiling): no up-the-wall, use entity facing
            float y = (float) Math.toRadians(entityYaw);
            upWall.set(-Mth.sin(y), 0f, Mth.cos(y));
            upWall.sub(new Vector3f(up).mul(upWall.dot(up)));
            if (upWall.lengthSquared() < 1.0e-4f) upWall.set(1f, 0f, 0f);
        }
        upWall.normalize();
        // Neutral forward looks up the wall, tilted toward the surface so it sits off the straight-up pole.
        Vector3f fwd = new Vector3f(upWall).sub(new Vector3f(up).mul(NEUTRAL_DOWN)).normalize();
        // Camera up = the wall normal made perpendicular to forward; right completes the basis.
        Vector3f camUp = new Vector3f(up).sub(new Vector3f(fwd).mul(up.dot(fwd))).normalize();
        Vector3f right = new Vector3f(fwd).cross(camUp).normalize();
        // Columns are the images of view +X, +Y, +Z; view forward is -Z, so +Z maps to -camForward.
        Quaternionf base = new Matrix3f().set(
                right.x(), right.y(), right.z(),
                camUp.x(), camUp.y(), camUp.z(),
                -fwd.x(), -fwd.y(), -fwd.z()
        ).getNormalizedRotation(new Quaternionf());
        // Turn about the wall normal (the floor's up) to keep the clean left/right, then pitch about right.
        return new Quaternionf().rotationAxis((float) Math.toRadians(wallYaw), up.x(), up.y(), up.z())
                .mul(base).rotateX((float) Math.toRadians(wallPitch));
    }

    /** The world direction the wall-frame camera looks (view forward), used to steer look-relative movement. */
    public static Vector3f wallLookForward(Vector3f n, float entityYaw) {
        return wallCameraOrientation(n, entityYaw).transform(new Vector3f(0f, 0f, -1f));
    }
}
