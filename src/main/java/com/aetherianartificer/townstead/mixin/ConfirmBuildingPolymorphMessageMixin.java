package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.compat.mca.BuildingReportReconciler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCA 7.7.18+ can defer a successful building add into a polymorph confirmation
 * packet. The original report-message TAIL hook runs before that deferred commit,
 * so Townstead reconciles again after the confirmation lands. Pseudo keeps this
 * inert on older MCA builds where the packet class does not exist.
 */
@Pseudo
@Mixin(targets = "net.conczin.mca.network.c2s.ConfirmBuildingPolymorphMessage", remap = false)
public abstract class ConfirmBuildingPolymorphMessageMixin {
    private static final Logger TOWNSTEAD$LOG = LoggerFactory.getLogger("Townstead/ConfirmBuildingPolymorphMessageMixin");

    @Shadow(remap = false)
    public abstract BlockPos source();

    @Inject(method = "handleServer", at = @At("TAIL"), remap = false, require = 0)
    private void townstead$reconcileAfterPolymorphCommit(ServerPlayer player, CallbackInfo ci) {
        BuildingReportReconciler.reconcileNearest(player.serverLevel(), source(), true, TOWNSTEAD$LOG);
    }
}
