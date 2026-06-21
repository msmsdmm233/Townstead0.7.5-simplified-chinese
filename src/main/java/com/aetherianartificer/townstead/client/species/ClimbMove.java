package com.aetherianartificer.townstead.client.species;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Spider gravity, movement: while a local player is clung in first person, WASD moves along the wall surface
 * relative to where they look (the wall-frame look from {@link ClimbLook}), so they climb up/down/around by
 * looking. Forward goes toward the look direction flattened onto the wall, strafe along the surface; no input
 * sticks in place. Replaces the vanilla push-into-wall climb while clung. Client-side only (player movement is
 * client-authoritative); third person and villagers keep the vanilla climb.
 */
public final class ClimbMove {

    private ClimbMove() {}

    private static final double SPEED = 0.12; // along-the-wall move speed (blocks/tick)
    private static final double STICK = 0.08; // pull toward the wall each tick so the player stays attached

    /** Drives look-relative wall movement for a clung local player; true when it took over (cancel vanilla). */
    public static boolean tryTravel(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player) return false;
        if (!mc.options.getCameraType().isFirstPerson()) return false; // third person keeps the vanilla climb
        if (mc.player.input.jumping) return false; // jump = let go, fall via vanilla
        int id = player.getId();
        if (ClimbAnim.factor(id) < 1f) return false; // only once fully attached
        Vector3f n = ClimbAnim.normal(id);
        if (n == null) return false;

        Vector3f normal = new Vector3f(n).normalize();
        // Where you look, flattened onto the wall = "forward" on the surface.
        Vector3f moveFwd = ClimbLook.wallLookForward(normal, player.getYRot());
        moveFwd.sub(new Vector3f(normal).mul(moveFwd.dot(normal)));
        if (moveFwd.lengthSquared() < 1.0e-4f) return false;
        moveFwd.normalize();
        Vector3f strafe = new Vector3f(normal).cross(moveFwd).normalize();

        Vector3f dir = new Vector3f(moveFwd).mul(player.zza).add(strafe.mul(player.xxa));
        double vx = -normal.x() * STICK;
        double vy = -normal.y() * STICK;
        double vz = -normal.z() * STICK;
        if (dir.lengthSquared() > 1.0e-4f) {
            dir.normalize().mul((float) SPEED);
            vx += dir.x();
            vy += dir.y();
            vz += dir.z();
        }
        Vec3 vel = new Vec3(vx, vy, vz);
        player.setDeltaMovement(vel);
        player.move(MoverType.SELF, vel);
        player.setDeltaMovement(Vec3.ZERO); // crisp stop, no momentum carry
        player.resetFallDistance();
        return true;
    }
}
