package com.aetherianartificer.townstead.root.harvest;

import com.aetherianartificer.townstead.pheno.condition.ConditionContext;
import com.aetherianartificer.townstead.root.ExpressedGenes;
import com.aetherianartificer.townstead.root.gene.types.BlockBreakSpeedGeneType;
import com.aetherianartificer.townstead.root.gene.types.InnateToolGeneType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Hand-gene hooks for mining: {@code innate_tool} consults a phantom tool stack while the
 * mainhand is empty, and {@code block_break_speed} genes scale digging against matching
 * blocks. Called from the BreakSpeed/HarvestCheck events on both loaders. Block breaking is
 * client-paced, so on the client both queries route to the synced-catalog mirror
 * ({@code ClientInnateTool}) the same way {@code MovementAbilities} predicts movement; the
 * client-only class is never linked on a dedicated server.
 */
public final class InnateTool {

    private InnateTool() {}

    /** Whether an innate-tool gene harvests this block (mainhand must be empty). */
    public static boolean allowsHarvest(Player player, BlockState state) {
        if (player == null || state == null) return false;
        if (player.level().isClientSide) {
            return com.aetherianartificer.townstead.client.root.ClientInnateTool.allowsHarvest(player, state);
        }
        if (!player.getMainHandItem().isEmpty()) return false;
        List<InnateToolGeneType.Instance> genes =
                ExpressedGenes.instancesOf(player, InnateToolGeneType.Instance.class);
        if (genes.isEmpty()) return false;
        ConditionContext ctx = new ConditionContext(player);
        for (InnateToolGeneType.Instance gene : genes) {
            if (gene.condition() != null && !gene.condition().test(ctx)) continue;
            if (gene.tool().isCorrectToolForDrops(state)) return true;
        }
        return false;
    }

    /** Dig speed after innate tools (max, empty mainhand only) and block-scoped multipliers. */
    public static float breakSpeed(Player player, BlockState state, float current) {
        if (player == null || state == null) return current;
        if (player.level().isClientSide) {
            return com.aetherianartificer.townstead.client.root.ClientInnateTool.breakSpeed(player, state, current);
        }
        float speed = current;
        ConditionContext ctx = null;
        if (player.getMainHandItem().isEmpty()) {
            for (InnateToolGeneType.Instance gene
                    : ExpressedGenes.instancesOf(player, InnateToolGeneType.Instance.class)) {
                if (gene.condition() != null) {
                    if (ctx == null) ctx = new ConditionContext(player);
                    if (!gene.condition().test(ctx)) continue;
                }
                speed = Math.max(speed, gene.tool().getDestroySpeed(state));
            }
        }
        for (BlockBreakSpeedGeneType.Instance gene
                : ExpressedGenes.instancesOf(player, BlockBreakSpeedGeneType.Instance.class)) {
            if (!gene.matches(state)) continue;
            if (gene.condition() != null) {
                if (ctx == null) ctx = new ConditionContext(player);
                if (!gene.condition().test(ctx)) continue;
            }
            speed *= gene.value();
        }
        return speed;
    }
}
