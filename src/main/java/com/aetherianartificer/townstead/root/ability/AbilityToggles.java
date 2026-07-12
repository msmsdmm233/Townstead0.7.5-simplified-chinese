package com.aetherianartificer.townstead.root.ability;

import com.aetherianartificer.townstead.root.gene.types.AbilityGeneType;
import com.aetherianartificer.townstead.pheno.power.Power;
import com.aetherianartificer.townstead.pheno.power.Powers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-entity on/off state for toggle-mode ability genes (transient; resets on
 * reload, like cooldowns). Flipped by the Root Ability key; read by the ability
 * ticker to decide whether a toggle ability applies this tick.
 */
public final class AbilityToggles {

    private static final Map<UUID, Set<ResourceLocation>> ON = new ConcurrentHashMap<>();

    private AbilityToggles() {}

    public static boolean isOn(LivingEntity entity, ResourceLocation geneId) {
        Set<ResourceLocation> set = ON.get(entity.getUUID());
        return set != null && set.contains(geneId);
    }

    /** Flip a toggle ability's state; returns the new state. */
    public static boolean flip(LivingEntity entity, ResourceLocation geneId) {
        Set<ResourceLocation> set = ON.computeIfAbsent(entity.getUUID(), k -> ConcurrentHashMap.newKeySet());
        if (set.contains(geneId)) {
            set.remove(geneId);
            return false;
        }
        set.add(geneId);
        return true;
    }

    /** Force a toggle state (villager AI deploys and folds wings without a keybind). */
    public static void set(LivingEntity entity, ResourceLocation geneId, boolean on) {
        Set<ResourceLocation> set = ON.computeIfAbsent(entity.getUUID(), k -> ConcurrentHashMap.newKeySet());
        if (on) set.add(geneId);
        else set.remove(geneId);
    }

    /** Whether the entity has any toggle switched on — a cheap gate for per-tick AI. */
    public static boolean hasAny(LivingEntity entity) {
        Set<ResourceLocation> set = ON.get(entity.getUUID());
        return set != null && !set.isEmpty();
    }

    /**
     * Every expressed gene whose state lives in this map and is currently on: toggle-mode
     * abilities AND standalone {@code pheno:toggle} genes. Both sync paths must use this —
     * filtering by one gene kind silently strands the other kind's state server-side
     * (the client gates then never fire, invisibly).
     */
    private static List<String> onSet(LivingEntity entity) {
        List<String> on = new ArrayList<>();
        for (Power gene : Powers.active(entity)) {
            boolean toggleKind = gene.component() instanceof AbilityGeneType.Instance ability
                    && ability.mode() == AbilityGeneType.Mode.TOGGLE
                    || gene.component()
                    instanceof com.aetherianartificer.townstead.root.gene.types.ToggleGeneType.Instance;
            if (toggleKind && isOn(entity, gene.id())) {
                on.add(gene.id().toString());
            }
        }
        return on;
    }

    /** Sync a non-player entity's on-set to everyone tracking it (a villager's deployed wings). */
    public static void syncEntity(LivingEntity entity) {
        AbilityTogglesS2CPayload payload = new AbilityTogglesS2CPayload(entity.getId(), onSet(entity));
        //? if neoforge {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        //?} else {
        /*com.aetherianartificer.townstead.TownsteadNetwork.sendToTrackingEntity(entity, payload);
        *///?}
    }

    public static void clear(UUID uuid) {
        ON.remove(uuid);
    }

    /**
     * Sync the player's live on-set, keyed by network id: to the player themself
     * (so the controlling client can predict toggle-driven movement abilities) AND
     * to everyone tracking them (so toggle-gated visuals — deployed wings — render
     * on other clients too). Carries only currently-expressed toggle genes that are on.
     */
    public static void syncTo(ServerPlayer player) {
        AbilityTogglesS2CPayload payload = new AbilityTogglesS2CPayload(player.getId(), onSet(player));
        //? if neoforge {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
        //?} else {
        /*com.aetherianartificer.townstead.TownsteadNetwork.sendToPlayer(player, payload);
        com.aetherianartificer.townstead.TownsteadNetwork.sendToTrackingEntity(player, payload);
        *///?}
    }
}
