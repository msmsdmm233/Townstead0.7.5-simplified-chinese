package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.root.gene.Allele;
import com.aetherianartificer.townstead.root.gene.Gene;
import com.aetherianartificer.townstead.root.gene.GeneRegistry;
import com.aetherianartificer.townstead.root.gene.GeneVariant;
import com.aetherianartificer.townstead.root.gene.Genotype;
import com.aetherianartificer.townstead.pheno.power.Power;
import com.aetherianartificer.townstead.pheno.power.Powers;
import com.aetherianartificer.townstead.root.ability.GeneAbilityTicker;
import com.aetherianartificer.townstead.root.attribute.GeneAttributeApplier;
import com.aetherianartificer.townstead.root.personality.PersonalityResolver;
import net.conczin.mca.entity.ai.relationship.Personality;
import com.aetherianartificer.townstead.villager.TownsteadVillager;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Branch-agnostic server handling for {@link RootSetC2SPayload}: resolves the
 * target (player self or villager), validates the origin against the registry,
 * applies it (player id, or villager id + a gene re-roll into the origin's
 * ranges), or answers a request. MCA's {@code Genetics} is SynchedEntityData, so
 * the re-roll syncs to tracking clients the same tick (no reload). The two
 * network branches only differ in how they ship the returned {@link Result}
 * back, so the decision logic lives here once.
 */
public final class RootServerLogic {

    private RootServerLogic() {}

    /** What to sync back to clients; {@code targetId == SELF} ⇒ the player. */
    public record Result(int targetId, String rootId) {}

    /**
     * Apply the set (or answer the request) and return the value to sync, or
     * {@code null} if the request was invalid (unknown target/origin).
     */
    @Nullable
    public static Result applyOrRequest(ServerPlayer sp, int entityId, String rootId) {
        boolean request = rootId == null || rootId.isEmpty();

        if (entityId == RootSetC2SPayload.NONE) return null;

        if (entityId == RootSetC2SPayload.SELF) {
            if (request) {
                return new Result(RootSetC2SPayload.SELF, orDefault(PlayerRoot.getRootId(sp)));
            }
            ResourceLocation id = resolveKnown(rootId);
            if (id == null) return null;
            boolean changed = !id.toString().equals(orDefault(PlayerRoot.getRootId(sp)));
            List<Power> oldGenes = changed ? new ArrayList<>(Powers.active(sp)) : List.of();
            PlayerRoot.setRootId(sp, id.toString());
            StartingEquipment.grant(sp);
            if (changed) resetPassives(sp, oldGenes);
            return new Result(RootSetC2SPayload.SELF, id.toString());
        }

        Entity entity = sp.serverLevel().getEntity(entityId);
        if (!(entity instanceof VillagerEntityMCA villager)) return null;
        TownsteadVillager state = TownsteadVillagers.get(villager);

        if (request) {
            return new Result(villager.getId(), orDefault(state.life().rootId()));
        }
        ResourceLocation id = resolveKnown(rootId);
        if (id == null) return null;
        boolean changed = !id.toString().equals(state.life().rootId());
        List<Power> oldGenes = changed ? new ArrayList<>(Powers.active(villager)) : List.of();
        state.life().setRoot(id.toString());
        // Reseed the diploid genotype + heritage to the new origin. MCA's *float*
        // genes (size/skin-tone) are committed separately by the editor's WYSIWYG
        // preview, but the diploid layer (ears, skin variant, chronotype, diet, the
        // Heritage screen, cosmetic expression) is origin-derived and lives only
        // server-side, so it must be rolled here or it stays empty/stale. Reroll only
        // on an actual change (or when none exists yet), so re-applying the same
        // origin doesn't randomize an existing villager — matching the picker preview.
        if (changed || !state.life().hasGenotype()) {
            Heredity.seedFounder(state.life(), id, villager.getRandom());
        }
        // Roll a personality from the new origin's allowlist (the natural-spawn path does this too).
        // Also fill one in when the villager has none yet, so re-applying an origin to a pre-existing
        // villager grants the personality it should have had.
        if (changed || state.life().personalityId().isEmpty()) {
            RootSpawnHandler.assignPersonality(villager, state, id);
        }
        // Flush now: the origin lives in a data attachment that only persists when
        // the snapshot is written, and the periodic flush may not run before the
        // world saves/exits — which lost the origin (and so the skin tint) on reload.
        TownsteadVillagers.flush(villager);
        if (changed) resetPassives(villager, oldGenes);
        return new Result(villager.getId(), id.toString());
    }

    /**
     * Pin a target's carried variant for one variant gene (the editor's variant picker). Sets both
     * genotype alleles to the chosen variant so it expresses, persists, and inherits like a roll,
     * then returns the resolved entity id for re-sync, or {@link RootSetC2SPayload#NONE} if invalid.
     */
    public static int setVariant(ServerPlayer sp, int entityId, String geneId, String variantId) {
        ResourceLocation gid = DataPackLang.parseId(geneId);
        if (gid == null) return RootSetC2SPayload.NONE;
        Gene gene = GeneRegistry.byId(gid);
        if (gene == null || !gene.hasVariants()) return RootSetC2SPayload.NONE;
        boolean valid = false;
        for (GeneVariant v : gene.variants()) {
            if (v.id().equals(variantId)) { valid = true; break; }
        }
        if (!valid) return RootSetC2SPayload.NONE;
        ResourceLocation locus = Heredity.locusOf(gene);
        Allele allele = Allele.of(gid, variantId);

        if (entityId == RootSetC2SPayload.SELF) {
            Genotype genotype = PlayerRoot.getGenotype(sp);
            if (genotype.isEmpty()) return RootSetC2SPayload.NONE;
            genotype.set(locus, allele, allele);
            PlayerRoot.setGenotype(sp, genotype);
            return sp.getId();
        }
        Entity entity = sp.serverLevel().getEntity(entityId);
        if (!(entity instanceof VillagerEntityMCA villager)) return RootSetC2SPayload.NONE;
        TownsteadVillager state = TownsteadVillagers.get(villager);
        state.life().genotype().set(locus, allele, allele);
        Heredity.recomputeExpressed(state.life());
        TownsteadVillagers.flush(villager);
        return villager.getId();
    }

    /**
     * Set a villager's personality from the editor's dynamic picker. Stores the chosen ref on the Life
     * (drives display + voice) and sets the MCA brain personality to the base enum it maps to. Returns
     * the entity id so the caller can re-broadcast the life sync, or {@link RootSetC2SPayload#NONE}.
     */
    public static int setPersonality(ServerPlayer sp, int entityId, String ref) {
        Entity entity = sp.serverLevel().getEntity(entityId);
        if (!(entity instanceof VillagerEntityMCA villager)) return RootSetC2SPayload.NONE;
        TownsteadVillager state = TownsteadVillagers.get(villager);
        state.life().setPersonalityId(ref == null ? "" : ref);
        Personality base = PersonalityResolver.baseOf(ref);
        if (base != null) villager.getVillagerBrain().setPersonality(base);
        TownsteadVillagers.flush(villager);
        return villager.getId();
    }

    /**
     * Clear the old origin's lingering passive state on a change. The ability/attribute
     * tickers only ever undo a gene they are still iterating, so a gene that leaves the
     * expressed set would orphan its applied state (a stuck no-gravity/sprint flag, glow
     * tag, or attribute modifier). The convergent tickers re-apply the new origin's set
     * on their next pass.
     */
    private static void resetPassives(LivingEntity entity, List<Power> oldGenes) {
        GeneAbilityTicker.resetPassives(entity);
        GeneAttributeApplier.removeFor(entity, oldGenes);
    }

    @Nullable
    private static ResourceLocation resolveKnown(String rootId) {
        ResourceLocation id = DataPackLang.parseId(rootId);
        return id != null && RootRegistry.byId(id) != null ? id : null;
    }

    /** Treat an unset origin as the default (everyone is an Overworlder by default). */
    private static String orDefault(String rootId) {
        return rootId == null || rootId.isEmpty() ? RootRegistry.DEFAULT_ID.toString() : rootId;
    }
}
