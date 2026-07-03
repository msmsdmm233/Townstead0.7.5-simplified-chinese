package com.aetherianartificer.townstead.client.species;

import com.aetherianartificer.townstead.client.root.ClientAbilities;
import com.aetherianartificer.townstead.root.ability.Ability;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Spider gravity, movement: while a local player is clung in first person, WASD moves along the wall surface
 * relative to where they look (the surface look from {@link ClimbState}), so they climb up/down/around by
 * looking. Forward goes toward the look direction flattened onto the wall, strafe along the surface; no input
 * sticks in place. Replaces the vanilla push-into-wall climb while clung. Client-side only (player movement is
 * client-authoritative); third person and villagers keep the vanilla climb.
 *
 * <p>The player stays locked to the current surface; holding crouch is the wrap modifier that lets the climb
 * commit to an adjoining surface (wall-&gt;ceiling, around a corner), and jump lets go. Without crouch, walking
 * toward a convex edge sticks at the lip instead of stepping off.</p>
 */
public final class ClimbMove {

    private ClimbMove() {}

    private static final double SPEED = 0.12; // along-the-wall move speed (blocks/tick)
    private static final double STICK = 0.08;
    private static final double PROBE = 0.18;
    private static final double STICKY_PROBE = 0.42;
    private static final double CORNER_AHEAD = 0.34;
    private static final boolean DEBUG = false; // flip true to log the clung movement each few ticks (latest.log)
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("townstead/climb");
    private static final int GRACE = 8;       // ticks after release that vanilla climb stays suppressed

    // Counts down after the controller last drove the player; while > 0 the vanilla climb is suppressed so it
    // can't fight the controller (e.g. climb the player back up at the wall base during a descent dismount).
    private static int controlGrace;
    private static Vector3f activeNormal;

    /** Decremented once per client tick (from ClimbState) so the suppression window closes after release. */
    public static void tickGrace() {
        if (controlGrace > 0) controlGrace--;
    }

    public static Vector3f activeNormal() {
        return activeNormal == null ? null : new Vector3f(activeNormal);
    }

    public static void reset() {
        activeNormal = null;
        controlGrace = 0;
    }

    /**
     * Whether vanilla climb should stay suppressed for this entity. Only during the dismount at the wall base
     * (on the ground): while still up on a wall (airborne) vanilla must hold normally, so the controller
     * handing off (e.g. switching to third person) does not drop the player.
     */
    public static boolean isSuppressing(net.minecraft.world.entity.LivingEntity e) {
        Minecraft mc = Minecraft.getInstance();
        return e == mc.player && controlGrace > 0 && mc.player.onGround();
    }

    /** Drives look-relative wall movement for a clung local player; true when it took over (cancel vanilla). */
    public static boolean tryTravel(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) return false;
        if (player.onGround() && (activeNormal == null || activeNormal.y() > 0.5f)) {
            activeNormal = null;
            return false;
        }
        // isActive reads the synced expressed genes, zeroed for a player not embodied as its species
        // (Player/Vanilla model mode), so there is no spider wall-movement there, just a plain player.
        if (!ClientAbilities.isActive(player, Ability.CLIMBING)) {
            activeNormal = null;
            return false;
        }
        if (mc.player.input.jumping) {
            activeNormal = null;
            return false;
        }
        // Crouch is the wrap modifier (commit to an adjoining surface), consumed in chooseSurface and the
        // edge-stop below; it no longer detaches. Jump is the sole release.
        boolean wrap = player.isShiftKeyDown();

        Vector3f normal = chooseSurface(player, wrap);
        if (normal == null) {
            activeNormal = null;
            return false;
        }
        activeNormal = normal;
        normal = new Vector3f(activeNormal).normalize();

        // A reoriented view (first person, or third person with REORIENT_THIRD_PERSON) uses the surface look.
        // A plain third-person view keeps vanilla climb on walls (push-to-climb); but a ceiling has nothing to
        // push into, so the controller must hold it there too, driven from the player's plain look.
        boolean reoriented = ClimbState.reorientedView();
        boolean ceiling = normal.y() < -0.5f;
        if (!reoriented && !ceiling) return false;

        Vector3f moveFwd;
        if (reoriented) {
            moveFwd = ClimbState.lookForward(normal, player.getYRot());
        } else {
            Vec3 v = player.getViewVector(1.0f);
            moveFwd = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        }
        // Flatten the look onto the surface = "forward" on it.
        moveFwd.sub(new Vector3f(normal).mul(moveFwd.dot(normal)));
        if (moveFwd.lengthSquared() < 1.0e-4f) return false;
        moveFwd.normalize();
        Vector3f strafe = new Vector3f(normal).cross(moveFwd).normalize();

        Vector3f dir = new Vector3f(moveFwd).mul(player.zza).add(strafe.mul(player.xxa));
        double vx = -normal.x() * STICK;
        double vy = -normal.y() * STICK;
        double vz = -normal.z() * STICK;
        // Locked to one plane (not wrapping): only move if the plane continues ahead, so walking toward a
        // convex lip sticks at the edge instead of stepping off into a fall. Holding crouch (wrap) lifts this
        // so the corner logic can carry you over; moving along the edge still finds the plane and passes.
        if (dir.lengthSquared() > 1.0e-4f && (wrap || planeAhead(player, normal, dir))) {
            dir.normalize().mul((float) SPEED);
            vx += dir.x();
            vy += dir.y();
            vz += dir.z();
        }
        Vec3 vel = new Vec3(vx, vy, vz);
        Vec3 before = player.position();
        player.setDeltaMovement(vel);
        player.move(MoverType.SELF, vel);
        Vec3 moved = player.position().subtract(before);
        player.setDeltaMovement(Vec3.ZERO); // crisp stop, no momentum carry
        player.resetFallDistance();
        controlGrace = GRACE; // keep vanilla climb suppressed through the dismount
        if (DEBUG && player.tickCount % 3 == 0) {
            LOG.info("climb n=({},{},{}) in(z={},x={}) fwd=({},{},{}) vel=({},{},{}) moved=({},{},{})",
                    f2(normal.x()), f2(normal.y()), f2(normal.z()), f1(player.zza), f1(player.xxa),
                    f2(moveFwd.x()), f2(moveFwd.y()), f2(moveFwd.z()), f3(vx), f3(vy), f3(vz),
                    f3(moved.x), f3(moved.y), f3(moved.z));
        }
        return true;
    }

    private static Vector3f chooseSurface(Player player, boolean wrap) {
        Vector3f previous = activeNormal != null ? activeNormal : ClimbState.normal(player.getId());

        // Plane-lock: with no wrap key held, never switch faces. Re-confirm the current plane is still there
        // (probe only toward it) and keep its normal, so reaching a corner or a ceiling lip holds this face
        // instead of the best-contact probe swinging onto a neighbouring surface on its own. Once on a surface
        // you stick to it; crouch is the only thing that changes surface.
        if (!wrap && previous != null) {
            Candidate held = contactToward(player, previous, STICKY_PROBE, player.getBoundingBox());
            return held != null && held.normal.y() <= 0.5f ? held.normal : null;
        }

        // A fresh grab (no plane yet) also needs the crouch key, so walking into a wall does not auto-latch;
        // crouch is required to change surface from the ground onto a wall just as from a wall onto a ceiling.
        if (previous == null && !wrap) return null;

        // Wrap held (or a crouch-gated fresh attach): full best-contact plus a corner look-ahead so the climb
        // can commit to an adjoining surface.
        Vector3f look = previous != null
                ? ClimbState.lookForward(new Vector3f(previous).normalize(), player.getYRot())
                : worldLook(player);
        Vector3f desired = desiredMoveOn(previous, look, player.zza, player.xxa);

        Candidate best = bestContact(player, previous, STICKY_PROBE, player.getBoundingBox());
        Candidate ahead = desired == null ? null : bestContact(player, previous, STICKY_PROBE,
                player.getBoundingBox().move(desired.x() * CORNER_AHEAD, desired.y() * CORNER_AHEAD, desired.z() * CORNER_AHEAD));
        if (ahead != null && best != null && ahead.normal.dot(best.normal) < 0.25f) {
            best = ahead;
        } else if (ahead != null && (best == null || ahead.score > best.score + 0.5f)) {
            best = ahead;
        }

        if (best != null && best.normal.y() > 0.5f) return null;
        if (best != null && (previous != null || startIntent(player))) return best.normal;
        return null;
    }

    /** Whether the current plane still continues just ahead of the intended move (locked edge-stop probe). */
    private static boolean planeAhead(Player player, Vector3f normal, Vector3f move) {
        Vector3f m = new Vector3f(move);
        if (m.lengthSquared() < 1.0e-4f) return true;
        m.normalize();
        AABB dest = player.getBoundingBox().move(m.x() * CORNER_AHEAD, m.y() * CORNER_AHEAD, m.z() * CORNER_AHEAD);
        return contactToward(player, normal, STICKY_PROBE, dest) != null;
    }

    /** Contact with the single face the entity is pressed against (the axis of {@code normal}); null if gone. */
    private static Candidate contactToward(Player player, Vector3f normal, double reach, AABB box) {
        Direction into = dominantInto(normal);
        float distance = firstHit(player, box, into, reach);
        if (distance <= 0f) return null;
        return new Candidate(normalForProbe(into), 1.0f / distance);
    }

    /** The block-face direction the entity is pressed into for a surface with outward {@code normal}. */
    private static Direction dominantInto(Vector3f normal) {
        float x = -normal.x(), y = -normal.y(), z = -normal.z();
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ay >= ax && ay >= az) return y > 0f ? Direction.UP : Direction.DOWN;
        if (ax >= az) return x > 0f ? Direction.EAST : Direction.WEST;
        return z > 0f ? Direction.SOUTH : Direction.NORTH;
    }

    private static Vector3f desiredMoveOn(Vector3f normal, Vector3f look, float forwardInput, float strafeInput) {
        if (normal == null) return null;
        Vector3f n = new Vector3f(normal).normalize();
        Vector3f fwd = new Vector3f(look);
        fwd.sub(new Vector3f(n).mul(fwd.dot(n)));
        if (fwd.lengthSquared() < 1.0e-4f) return null;
        fwd.normalize();
        Vector3f strafe = new Vector3f(n).cross(fwd).normalize();
        Vector3f desired = fwd.mul(forwardInput).add(strafe.mul(strafeInput));
        if (desired.lengthSquared() < 1.0e-4f) return null;
        return desired.normalize();
    }

    private static Vector3f worldLook(Player player) {
        Vec3 v = player.getViewVector(1.0f);
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    private static Candidate bestContact(Player player, Vector3f previous, double reach, AABB box) {
        Candidate best = null;
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            Vector3f normal = normalForProbe(d);
            float distance = firstHit(player, box, d, reach);
            if (distance <= 0f) continue;
            float score = 1.0f / distance;
            if (normal.y() < -0.5f) score *= 4.0f;
            if (previous != null && previous.lengthSquared() > 1.0e-4f) {
                score += Math.max(0f, new Vector3f(previous).normalize().dot(normal)) * 2.0f;
            }
            if (best == null || score > best.score) best = new Candidate(normal, score);
        }
        return best;
    }

    private static float firstHit(Player player, AABB box, Direction d, double reach) {
        final int steps = 6;
        for (int i = 1; i <= steps; i++) {
            double r = reach * i / steps;
            AABB moved = box.move(d.getStepX() * r, d.getStepY() * r, d.getStepZ() * r);
            if (!player.level().noCollision(player, moved)) return (float) r;
        }
        return 0f;
    }

    private static Vector3f normalForProbe(Direction d) {
        return new Vector3f(-d.getStepX(), -d.getStepY(), -d.getStepZ()).normalize();
    }

    private static boolean startIntent(Player player) {
        if (!player.horizontalCollision) return false;
        Direction d = player.getDirection();
        AABB moved = player.getBoundingBox().move(d.getStepX() * PROBE, 0.0, d.getStepZ() * PROBE);
        return !player.level().noCollision(player, moved);
    }

    private record Candidate(Vector3f normal, float score) {
    }

    private static String f1(double v) { return String.format(java.util.Locale.ROOT, "%.1f", v); }
    private static String f2(double v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }
    private static String f3(double v) { return String.format(java.util.Locale.ROOT, "%.3f", v); }
}
