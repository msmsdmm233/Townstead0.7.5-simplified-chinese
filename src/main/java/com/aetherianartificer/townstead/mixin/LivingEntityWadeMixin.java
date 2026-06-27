package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.Wade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives a {@code wade} gene's in-fluid movement-speed penalty each tick for villagers, other mobs,
 * and players alike. {@link Wade} is server-authoritative (it toggles a transient attribute modifier
 * that syncs to the client), so this only needs the one hook. Same hook both branches (SRG on Forge).
 */
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class LivingEntityWadeMixin {

    //? if neoforge {
    @Inject(method = "aiStep", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "m_8107_", at = @At("TAIL"), remap = false)
    *///?}
    private void townstead$wade(CallbackInfo ci) {
        Wade.tick((net.minecraft.world.entity.LivingEntity) (Object) this);
    }
}
