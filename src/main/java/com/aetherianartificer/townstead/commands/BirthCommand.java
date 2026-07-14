package com.aetherianartificer.townstead.commands;

import com.aetherianartificer.townstead.root.reproduction.DirectBirth;
import com.mojang.brigadier.CommandDispatcher;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Pregnancy;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Debug command to force an immediate birth, for testing root/heritage/genotype
 * inheritance without waiting out a gestation.
 *
 * <ul>
 *   <li>{@code /townstead birth} births from the MCA villager the caller is
 *       looking at (or the nearest one within 16 blocks).
 *   <li>{@code /townstead birth @target <father>} births from the looked-at
 *       villager as mother with an explicit co-parent ({@code @target} is a
 *       literal of this command, not a real entity selector).
 *   <li>{@code /townstead birth <mother>} births from an explicit mother; the
 *       co-parent is the mother's partner (villager or player), or the mother
 *       herself if unpartnered.
 *   <li>{@code /townstead birth <mother> @target} births from an explicit
 *       mother with the looked-at villager (never the mother herself) as father.
 *   <li>{@code /townstead birth <mother> <father>} births from two explicit
 *       parents; the father may be a villager or a player ({@code @s} to be the
 *       co-parent yourself).
 * </ul>
 *
 * <p>Births go through {@link DirectBirth#spawnOffspring}, so each child is built
 * by MCA's {@link Pregnancy#createChild} with the Townstead inheritance mixins
 * firing exactly as on a natural birth, litter-size genes roll the clutch, and a
 * player co-parent's heritage/genotype/parentage is re-resolved from both true
 * parents (the path {@code createChild} can't take directly).</p>
 */
public final class BirthCommand {
    private static final double LOOK_RANGE = 16.0;

    private BirthCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
        dispatcher.register(
                Commands.literal("townstead").then(Commands.literal("birth")
                        .requires(s -> s.hasPermission(2))
                        .executes(c -> birthAuto(c.getSource()))
                        .then(Commands.literal("@target")
                                .executes(c -> birthAuto(c.getSource()))
                                .then(Commands.argument("father", EntityArgument.entity())
                                        .executes(c -> birthLookedAt(
                                                c.getSource(),
                                                EntityArgument.getEntity(c, "father")))))
                        .then(Commands.argument("mother", EntityArgument.entity())
                                .executes(c -> birthExplicit(
                                        c.getSource(),
                                        EntityArgument.getEntity(c, "mother"),
                                        null))
                                .then(Commands.literal("@target")
                                        .executes(c -> birthFatherLookedAt(
                                                c.getSource(),
                                                EntityArgument.getEntity(c, "mother"))))
                                .then(Commands.argument("father", EntityArgument.entity())
                                        .executes(c -> birthExplicit(
                                                c.getSource(),
                                                EntityArgument.getEntity(c, "mother"),
                                                EntityArgument.getEntity(c, "father")))))));
    }

    private static int birthAuto(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This form must be run by a player. Supply <mother> instead."));
            return 0;
        }
        VillagerEntityMCA mother = pickLookedAtOrNearest(player, null);
        if (mother == null) {
            source.sendFailure(Component.literal("No MCA villager near your crosshair (within "
                    + (int) LOOK_RANGE + " blocks)."));
            return 0;
        }
        return birth(source, mother, null);
    }

    private static int birthLookedAt(CommandSourceStack source, Entity father) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("@target requires a player caller. Supply <mother> instead."));
            return 0;
        }
        VillagerEntityMCA mother = pickLookedAtOrNearest(player, null);
        if (mother == null) {
            source.sendFailure(Component.literal("No MCA villager near your crosshair (within "
                    + (int) LOOK_RANGE + " blocks)."));
            return 0;
        }
        return birthExplicit(source, mother, father);
    }

    private static int birthFatherLookedAt(CommandSourceStack source, Entity mother) {
        if (!(mother instanceof VillagerEntityMCA motherMca)) {
            source.sendFailure(Component.literal("Mother must be an MCA villager."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("@target requires a player caller. Supply <father> instead."));
            return 0;
        }
        VillagerEntityMCA father = pickLookedAtOrNearest(player, motherMca);
        if (father == null) {
            source.sendFailure(Component.literal("No MCA villager other than the mother near your crosshair (within "
                    + (int) LOOK_RANGE + " blocks)."));
            return 0;
        }
        return birth(source, motherMca, father);
    }

    private static int birthExplicit(CommandSourceStack source, Entity mother, Entity father) {
        if (!(mother instanceof VillagerEntityMCA motherMca)) {
            source.sendFailure(Component.literal("Mother must be an MCA villager."));
            return 0;
        }
        if (father != null && !(father instanceof VillagerEntityMCA) && !(father instanceof Player)) {
            source.sendFailure(Component.literal("Father must be an MCA villager or a player (@s for yourself)."));
            return 0;
        }
        return birth(source, motherMca, father);
    }

    private static int birth(CommandSourceStack source, VillagerEntityMCA mother, Entity father) {
        Entity coParent = father != null ? father
                : mother.getRelationships().getPartner()
                        .filter(e -> e instanceof VillagerEntityMCA || e instanceof Player)
                        .orElse(mother);

        List<VillagerEntityMCA> born = DirectBirth.spawnOffspring(mother, coParent);
        if (born.isEmpty()) {
            source.sendFailure(Component.literal("No child was born."));
            return 0;
        }

        boolean selfParented = coParent == mother;
        String names = String.join(", ", born.stream().map(c -> c.getName().getString()).toList());
        String parents = mother.getName().getString()
                + (selfParented ? " (self-parented)" : " and " + coParent.getName().getString());
        source.sendSuccess(() -> Component.literal(parents + " gave birth to " + names + "."), true);
        if (selfParented && father == null) {
            source.sendSuccess(() -> Component.literal(
                    "  No partner found; inherited from a single parent. Try /townstead birth @target @s to test cross-parent inheritance."),
                    false);
        }
        return born.size();
    }

    private static VillagerEntityMCA pickLookedAtOrNearest(ServerPlayer player, VillagerEntityMCA exclude) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 endpoint = eye.add(look.scale(LOOK_RANGE));
        AABB sweep = new AABB(eye, endpoint).inflate(LOOK_RANGE);
        VillagerEntityMCA bestLook = null;
        double bestLookT = Double.POSITIVE_INFINITY;
        VillagerEntityMCA bestNear = null;
        double bestNearDist = Double.POSITIVE_INFINITY;
        for (VillagerEntityMCA villager : level.getEntitiesOfClass(VillagerEntityMCA.class, sweep)) {
            if (villager == exclude) continue;
            Vec3 toVillager = villager.position().subtract(eye);
            double along = toVillager.dot(look);
            if (along > 0 && along <= LOOK_RANGE) {
                Vec3 closestOnRay = eye.add(look.scale(along));
                double perp = villager.position().distanceTo(closestOnRay);
                if (perp <= Math.max(0.7, villager.getBbWidth()) && along < bestLookT) {
                    bestLookT = along;
                    bestLook = villager;
                }
            }
            double dist = player.distanceToSqr(villager);
            if (dist < bestNearDist) {
                bestNearDist = dist;
                bestNear = villager;
            }
        }
        if (bestLook != null) return bestLook;
        if (bestNear != null && bestNearDist <= LOOK_RANGE * LOOK_RANGE) return bestNear;
        return null;
    }
}
