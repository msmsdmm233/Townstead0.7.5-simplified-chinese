package com.aetherianartificer.townstead.root.rig;

import com.aetherianartificer.townstead.calendar.LifeClientStore;
import com.aetherianartificer.townstead.client.root.RootCatalogClient;
import com.aetherianartificer.townstead.client.root.RootClientStore;
import com.aetherianartificer.townstead.root.RootCatalogEntry;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Resolves the {@link RigDefinition.Hitbox} an MCA villager should use from the rig it currently renders
 * as (its life-stage rig override, else its species rig), and turns it into {@link EntityDimensions} for
 * the {@code EntityEvent.Size} hook. Both the server (collision/pathing) and the client (interaction
 * raycast) need it, but they read different stores, so the lookup is split by side: server registries vs.
 * the synced client catalog. The client path touches only rendering-free data classes, so this class is
 * safe to load on a dedicated server (the client branch simply never runs there).
 */
public final class RigHitboxes {

    private RigHitboxes() {}

    /**
     * Max collision WIDTH any rig's hitbox is clamped to, so a rig can render large yet still path through a
     * 1-block doorway. An open door leaf juts ~3px (0.1875) into the opening, leaving ~0.81 clear, and MCA's
     * village/building navigation assumes a roughly vanilla width; 0.9 could not fit. The on-screen size is a
     * separate client render scale, so this clamp never changes how big the entity looks.
     */
    private static final float DOOR_SAFE_WIDTH = 0.7f;

    /**
     * The dimensions this entity's rig imposes, or null to leave MCA's scale-derived default. Sleeping
     * villagers keep MCA's sleeping box. Non-villagers and rigs without a declared hitbox return null.
     */
    public static EntityDimensions dimensionsFor(Entity entity, Pose pose) {
        if (pose == Pose.SLEEPING) return null;
        // Villagers (life-stage / species rig) and players (origin's species rig) both take their rig's
        // box; any other entity keeps its vanilla dimensions. A player without a hitbox-declaring rig
        // (the default humanoid origins) resolves null below and stays vanilla 0.6 x 1.8.
        if (!(entity instanceof VillagerEntityMCA) && !(entity instanceof Player)) return null;
        if (!(entity instanceof LivingEntity living)) return null;
        RigDefinition.Hitbox box = entity.level().isClientSide ? forClient(living) : forServer(living);
        if (box == null) return null;
        // Clamp width to stay door-passable (see DOOR_SAFE_WIDTH); height is left as declared (short is fine,
        // tall just needs headroom). Visual size is unaffected — it is a separate client render scale.
        float width = Math.min(box.width(), DOOR_SAFE_WIDTH);
        return EntityDimensions.scalable(width, box.height());
    }

    private static RigDefinition.Hitbox forServer(LivingEntity entity) {
        RigDefinition def = ServerRig.defFor(entity);
        return def == null ? null : def.hitbox();
    }

    private static RigDefinition.Hitbox forClient(LivingEntity entity) {
        String rootId = RootClientStore.resolve(entity);
        RootCatalogEntry origin = RootCatalogClient.origin(rootId);
        if (origin == null) return null;
        String rigBase = clientStageRig(entity, origin);
        if (rigBase == null || rigBase.isEmpty()) rigBase = origin.rigBase();
        RigDefinition def = RootCatalogClient.rig(rigBase);
        return def == null ? null : def.hitbox();
    }

    /** The current stage's rig override from the synced catalog + client life snapshot, or null. */
    private static String clientStageRig(LivingEntity entity, RootCatalogEntry origin) {
        List<String> rigs = origin.stageRigs();
        if (rigs == null || rigs.isEmpty()) return null;
        LifeClientStore.Snapshot snap = LifeClientStore.get(entity.getId());
        if (snap == null) return null;
        int idx = snap.currentStageIndex();
        return idx >= 0 && idx < rigs.size() ? rigs.get(idx) : null;
    }
}
