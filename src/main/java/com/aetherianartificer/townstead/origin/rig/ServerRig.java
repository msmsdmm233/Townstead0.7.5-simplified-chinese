package com.aetherianartificer.townstead.origin.rig;

import com.aetherianartificer.townstead.origin.LifeStage;
import com.aetherianartificer.townstead.origin.LifeStageProgression;
import com.aetherianartificer.townstead.origin.OriginRegistry;
import com.aetherianartificer.townstead.origin.PlayerOrigin;
import com.aetherianartificer.townstead.origin.Species;
import com.aetherianartificer.townstead.origin.SpeciesRegistry;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Resolves the {@link RigDefinition} an entity currently uses, server-side, from the server registries
 * (the client reads the synced catalog instead). A villager takes its life-stage rig override when the
 * current stage declares one, else its species rig; a player takes its origin's species rig (players have
 * no life stages). Returns null for entities with no Townstead rig. Used by server-side rig consumers
 * (hitboxes, the equipment gate) so they all resolve a rig the same way.
 */
public final class ServerRig {

    private ServerRig() {}

    /** The rig definition this entity renders as, server-side, or null if it has none. */
    public static RigDefinition defFor(LivingEntity entity) {
        String id = rigIdFor(entity);
        return id == null || id.isEmpty() ? null : RigRegistry.byId(id);
    }

    /** The rig id for this entity (life-stage override / species rig), or null. */
    public static String rigIdFor(LivingEntity entity) {
        if (entity instanceof VillagerEntityMCA villager) {
            LifeStage stage = LifeStageProgression.currentStage(villager);
            if (stage != null && stage.rig() != null && !stage.rig().isEmpty()) return stage.rig();
            return speciesRig(TownsteadVillagers.get(villager).life().originId());
        }
        if (entity instanceof Player player) {
            return speciesRig(PlayerOrigin.getOriginId(player));
        }
        return null;
    }

    private static String speciesRig(String originIdRaw) {
        ResourceLocation originId = ResourceLocation.tryParse(originIdRaw);
        ResourceLocation speciesId = OriginRegistry.effectiveSpecies(originId);
        Species species = speciesId == null ? null : SpeciesRegistry.byId(speciesId);
        return species == null ? null : species.rig().base();
    }
}
