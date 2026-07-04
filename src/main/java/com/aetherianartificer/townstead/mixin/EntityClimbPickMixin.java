package com.aetherianartificer.townstead.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spider gravity (aim): while the local player is clung and its view is reoriented onto a surface, block and
 * entity picking must follow the reoriented crosshair, not the entity's raw look. {@code Entity.pick} shoots
 * along {@link Entity#getViewVector}, which is built from the untouched yaw/pitch fields; the clung camera is
 * only tipped at render time (see {@code ClimbView}), so without this the pick ray and the crosshair diverge
 * and blocks land where the un-tipped look points. {@link
 * com.aetherianartificer.townstead.client.species.ClimbState#reorientedPick} supplies the surface-frame look
 * for the local player instead. Client-only (guarded by {@code isClientSide}, so the helper never links
 * server-side). 1.20.1 Forge SRG: {@code m_20252_} getViewVector.
 */
@Mixin(Entity.class)
public abstract class EntityClimbPickMixin {

    //? if neoforge {
    @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
    //?} else {
    /*@Inject(method = "m_20252_", at = @At("HEAD"), cancellable = true, remap = false)
    *///?}
    private void townstead$reorientPick(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide) return;
        if (!(self instanceof LivingEntity living)) return;
        Vec3 look = com.aetherianartificer.townstead.client.species.ClimbState.reorientedPick(living);
        if (look != null) cir.setReturnValue(look);
    }
}
