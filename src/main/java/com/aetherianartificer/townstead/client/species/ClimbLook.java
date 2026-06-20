package com.aetherianartificer.townstead.client.species;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

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

    // Wall-frame look, accumulated from the mouse while clung; reset to neutral (along the wall) on attach.
    private static float wallYaw;
    private static float wallPitch;
    private static boolean clung;

    // TEMP debug: throttled so a stream of mouse events doesn't flood the log.
    private static final Logger LOG = LogUtils.getLogger();
    private static long lastDebug;

    public static void debug(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastDebug < 400L) return;
        lastDebug = now;
        LOG.info("[ClimbLook] {}", msg);
    }

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
        boolean isPlayer = self == Minecraft.getInstance().player;
        int id = self.getId();
        float f = ClimbAnim.factor(id);
        Vector3f up = ClimbAnim.normal(id);
        if (!isPlayer || f <= 0f || up == null) return false;

        wallYaw -= (float) (yawDelta * TURN_SCALE);
        wallPitch -= (float) (pitchDelta * TURN_SCALE);
        wallPitch = Math.max(-89f, Math.min(89f, wallPitch));
        debug("turn wallYaw=" + wallYaw + " wallPitch=" + wallPitch);
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
        Vector3f fwd = new Vector3f(up).cross(0f, 1f, 0f); // n x worldUp = horizontal tangent along the wall
        if (fwd.lengthSquared() < 1.0e-4f) {
            float y = (float) Math.toRadians(entityYaw);
            fwd.set(-Mth.sin(y), 0f, Mth.cos(y));
            fwd.sub(new Vector3f(up).mul(fwd.dot(up)));
            if (fwd.lengthSquared() < 1.0e-4f) fwd.set(1f, 0f, 0f);
        }
        fwd.normalize();
        Vector3f right = new Vector3f(fwd).cross(up).normalize(); // camForward x camUp
        // Columns are the images of view +X, +Y, +Z; view forward is -Z, so +Z maps to -camForward.
        Quaternionf base = new Matrix3f().set(
                right.x(), right.y(), right.z(),
                up.x(), up.y(), up.z(),
                -fwd.x(), -fwd.y(), -fwd.z()
        ).getNormalizedRotation(new Quaternionf());
        return base.rotateY((float) Math.toRadians(wallYaw)).rotateX((float) Math.toRadians(wallPitch));
    }
}
