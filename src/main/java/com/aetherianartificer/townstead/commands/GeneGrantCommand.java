package com.aetherianartificer.townstead.commands;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.root.Heredity;
import com.aetherianartificer.townstead.root.gene.Allele;
import com.aetherianartificer.townstead.root.gene.Gene;
import com.aetherianartificer.townstead.root.gene.GeneRegistry;
import com.aetherianartificer.townstead.villager.TownsteadVillager;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /townstead gene grant|revoke <villager> <geneId>} — the authoring loop for
 * gene-driven features: put any registered gene on a live villager (homozygous, with
 * a fresh variant/size roll) or take it off again, with the expressed set re-synced so
 * the change renders the same frame. How a pack author swings a physics tail without
 * wiring it into an origin first.
 */
public final class GeneGrantCommand {

    private GeneGrantCommand() {}

    private static final SuggestionProvider<CommandSourceStack> GENE_IDS = (c, b) -> {
        List<String> ids = new ArrayList<>();
        for (Gene gene : GeneRegistry.all()) ids.add("\"" + gene.id() + "\"");
        return SharedSuggestionProvider.suggest(ids, b);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("townstead").then(Commands.literal("gene")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("grant")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("id", StringArgumentType.string()).suggests(GENE_IDS)
                                        .executes(c -> apply(c.getSource(),
                                                EntityArgument.getEntity(c, "target"),
                                                StringArgumentType.getString(c, "id"), true)))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("id", StringArgumentType.string()).suggests(GENE_IDS)
                                        .executes(c -> apply(c.getSource(),
                                                EntityArgument.getEntity(c, "target"),
                                                StringArgumentType.getString(c, "id"), false)))))));
    }

    private static int apply(CommandSourceStack source, Entity entity, String geneId, boolean grant) {
        if (!(entity instanceof VillagerEntityMCA villager)) {
            source.sendFailure(Component.literal("Target must be a villager."));
            return 0;
        }
        ResourceLocation gid = DataPackLang.parseId(geneId);
        Gene gene = gid == null ? null : GeneRegistry.byId(gid);
        if (gene == null) {
            source.sendFailure(Component.literal("No gene '" + geneId + "' is registered."));
            return 0;
        }
        TownsteadVillager state = TownsteadVillagers.get(villager);
        if (!state.life().hasGenotype()) {
            source.sendFailure(Component.literal(
                    "That villager has no genotype yet (assign a root first, or wait for the stamper)."));
            return 0;
        }
        ResourceLocation locus = Heredity.locusOf(gene);
        Allele allele = grant ? Heredity.grantAllele(gene, villager.getRandom()) : Allele.WILD;
        state.life().genotype().set(locus, allele, allele);
        Heredity.recomputeExpressed(state.life());
        TownsteadVillagers.flush(villager);
        broadcastExpressed(villager);
        String rolled = grant && allele.variantId() != null ? " (" + allele.variantId() + ")" : "";
        source.sendSuccess(() -> Component.literal((grant ? "Granted " : "Revoked ") + gene.id()
                + rolled + (grant ? " to " : " from ") + villager.getName().getString()
                + " (homozygous; inherits like a natural roll)"), false);
        return 1;
    }

    private static void broadcastExpressed(VillagerEntityMCA villager) {
        com.aetherianartificer.townstead.root.ExpressedGenesS2CPayload payload =
                com.aetherianartificer.townstead.root.ExpressedGenesS2CPayload.forEntity(villager.getId(), villager);
        //? if neoforge {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(villager, payload);
        //?} else if forge {
        /*com.aetherianartificer.townstead.TownsteadNetwork.sendToTrackingEntity(villager, payload);
        *///?}
    }
}
