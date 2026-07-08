package com.aetherianartificer.townstead.root.ability;

/**
 * Duck interface merged onto every {@link net.minecraft.world.entity.Entity} by
 * {@code EntityFallFlightMixin}: lets villager AI enter or leave fall-flight (the
 * shared flag players set via {@code startFallFlying}) without a player-only path.
 */
public interface FallFlightBridge {

    void townstead$setFallFlying(boolean value);
}
