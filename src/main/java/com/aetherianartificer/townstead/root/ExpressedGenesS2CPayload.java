package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.root.gene.Allele;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: an entity's expressed (dominant) gene alleles, so the client can
 * render that individual's actual genetics (attachments, glow, hidden features)
 * rather than the origin-typical set. Encodings are {@link Allele#encode()} strings;
 * {@code entityId == -1} is the player's own. Updates {@code RootClientStore}.
 */
//? if neoforge {
public record ExpressedGenesS2CPayload(int entityId, List<String> genes) implements CustomPacketPayload {
//?} else {
/*public record ExpressedGenesS2CPayload(int entityId, java.util.List<String> genes) {
*///?}

    //? if neoforge {
    public static final Type<ExpressedGenesS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "expressed_genes_s2c"));

    public static final StreamCodec<FriendlyByteBuf, ExpressedGenesS2CPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> p.write(buf), ExpressedGenesS2CPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    //?}

    //? if neoforge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Townstead.MOD_ID, "expressed_genes_s2c");
    //?} else {
    /*public static final ResourceLocation ID = new ResourceLocation(Townstead.MOD_ID, "expressed_genes_s2c");
    *///?}

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(genes.size());
        for (String gene : genes) buf.writeUtf(gene);
    }

    public static ExpressedGenesS2CPayload read(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int count = buf.readVarInt();
        List<String> genes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) genes.add(buf.readUtf());
        return new ExpressedGenesS2CPayload(entityId, genes);
    }

    /** Build the payload for a living entity (villager or player), keyed by {@code entityId}. */
    public static ExpressedGenesS2CPayload forEntity(int entityId, LivingEntity entity) {
        com.aetherianartificer.townstead.root.gene.Genotype genotype;
        Heritage heritage = null;
        if (entity instanceof VillagerEntityMCA villager) {
            var life = TownsteadVillagers.get(villager).life();
            genotype = life.genotype();
            if (life.hasHeritage()) heritage = life.heritage();
        } else if (entity instanceof Player player) {
            genotype = PlayerRoot.getGenotype(player);
            ResourceLocation rootId = ResourceLocation.tryParse(PlayerRoot.getRootId(player));
            heritage = RootRegistry.seedHeritage(rootId == null ? RootRegistry.DEFAULT_ID : rootId);
        } else {
            genotype = new com.aetherianartificer.townstead.root.gene.Genotype();
        }
        List<String> genes = new ArrayList<>();
        for (Allele allele : Heredity.expressedAlleles(genotype)) {
            genes.add(Heredity.scaleByHeritage(allele, heritage).encode());
            // Companions ride along their parent's expression server-side (GenePowerSource);
            // mirror them here so client-resolved render genes (opacity, attachments granted
            // as companions) see them too.
            if (allele.geneId() == null) continue;
            for (ResourceLocation companion
                    : com.aetherianartificer.townstead.root.gene.GeneRegistry.companionsOf(allele.geneId())) {
                genes.add(Allele.of(companion, null).encode());
            }
        }
        return new ExpressedGenesS2CPayload(entityId, genes);
    }
}
