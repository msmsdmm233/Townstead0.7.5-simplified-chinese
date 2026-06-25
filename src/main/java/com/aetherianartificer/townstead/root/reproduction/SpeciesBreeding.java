package com.aetherianartificer.townstead.root.reproduction;

import com.aetherianartificer.townstead.root.RootRegistry;
import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The species barrier on reproduction: two prospective parents must share an effective species to
 * conceive. Ancestries and lineages within one species interbreed freely; different species do not.
 * (The future Heritage tier is the intended cross-species exception.) Species is resolved from each
 * parent's true origin, the same way rigs and origin conditions resolve it, so a player and a villager
 * are compared on equal footing and appearance never enters into it.
 */
public final class SpeciesBreeding {

    /** Action-bar feedback shown to a player whose conception is refused by the species barrier. */
    public static final String DIFFERENT_SPECIES_KEY = "message.townstead.breeding_different_species";

    private SpeciesBreeding() {}

    /** Whether two prospective parents may produce offspring under the species barrier. */
    public static boolean sameSpecies(LivingEntity a, LivingEntity b) {
        ResourceLocation sa = speciesOf(a);
        ResourceLocation sb = speciesOf(b);
        // An indeterminate species never blocks; the gate fires only on a confirmed mismatch.
        return sa == null || sb == null || sa.equals(sb);
    }

    @Nullable
    private static ResourceLocation speciesOf(LivingEntity entity) {
        return RootRegistry.effectiveSpecies(ResourceLocation.tryParse(rootId(entity)));
    }

    private static String rootId(LivingEntity entity) {
        if (entity instanceof Player player) return PlayerRoot.getRootId(player);
        if (entity instanceof VillagerEntityMCA villager) {
            return TownsteadVillagers.get(villager).life().rootId();
        }
        return "";
    }

    /** If {@code parent} is a player, tell them why the conception was refused. */
    public static void notifyIfPlayer(Entity parent) {
        if (parent instanceof Player player) {
            player.displayClientMessage(Component.translatable(DIFFERENT_SPECIES_KEY), true);
        }
    }
}
