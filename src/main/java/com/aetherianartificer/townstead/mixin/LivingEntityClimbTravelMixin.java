package com.aetherianartificer.townstead.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spider gravity, movement: hands the local player's {@link LivingEntity#travel} over to
 * {@link com.aetherianartificer.townstead.client.species.ClimbMove} while clung in first person, so WASD moves
 * along the wall relative to where they look. Client-only (the helper touches client classes and player
 * movement is client-authoritative); when it doesn't take over, vanilla travel runs unchanged. 1.20.1 Forge
 * SRG: {@code m_7023_} travel.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbTravelMixin {

    //? if neoforge {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    //?} else {
    /*@Inject(method = "m_7023_", at = @At("HEAD"), cancellable = true, remap = false)
    *///?}
    private void townstead$wallTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide) return;
        if (!(self instanceof Player player)) return;
        if (com.aetherianartificer.townstead.client.species.ClimbMove.tryTravel(player)) {
            ci.cancel();
        }
    }
}
