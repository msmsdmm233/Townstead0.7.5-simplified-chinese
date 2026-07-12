package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.ability.FallFlightBridge;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements {@link FallFlightBridge} on every entity: exposes the protected
 * shared-flag setter so villager glide AI can enter fall-flight (flag 7) without
 * the player-only {@code startFallFlying} path. Parity: SRG name on Forge.
 */
@Mixin(Entity.class)
public abstract class EntityFallFlightMixin implements FallFlightBridge {

    //? if neoforge {
    @Shadow protected abstract void setSharedFlag(int flag, boolean value);
    //?} else {
    /*@Shadow(remap = false) protected abstract void m_20115_(int flag, boolean value);
    *///?}

    @Unique
    @Override
    public void townstead$setFallFlying(boolean value) {
        //? if neoforge {
        this.setSharedFlag(7, value);
        //?} else {
        /*this.m_20115_(7, value);
        *///?}
    }
}
