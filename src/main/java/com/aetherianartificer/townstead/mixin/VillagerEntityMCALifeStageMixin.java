package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.LifeStageProgression;
import com.aetherianartificer.townstead.root.RootSpawnHandler;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.AgeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Substitutes MCA's {@code setAgeState} parameter with the canonical AgeState
 * Townstead derives from the villager's {@link com.aetherianartificer.townstead.root.LifeCycle}
 * (per-villager stage durations + Lifespan gene). MCA's downstream brain/clothes
 * refresh runs unchanged with our substituted state. The Senior canonical
 * collapses to MCA's ADULT (with {@code Life.isSenior} flipped) so MCA's
 * marriage/work gates still work.
 *
 * <p>Server-side only. On the client, the parameter passes through unmodified;
 * the server has already substituted and the value arrives via tracked-data
 * sync.</p>
 *
 * <p>{@code remap=false}: target method and parameter type live in MCA, not
 * vanilla — bytecode names are stable across both stonecutter branches.</p>
 */
@Mixin(value = VillagerEntityMCA.class, remap = false)
public class VillagerEntityMCALifeStageMixin {

    @ModifyVariable(method = "setAgeState", at = @At("HEAD"), argsOnly = true)
    private AgeState townstead$substituteCanonicalAgeState(AgeState requested) {
        VillagerEntityMCA self = (VillagerEntityMCA) (Object) this;
        if (self.level().isClientSide) return requested;
        AgeState resolved = LifeStageProgression.resolveMcaAgeState(self, requested);
        return resolved == null ? requested : resolved;
    }

    /**
     * On a genuine age-stage transition (setAgeState returns true only when the old state was already
     * a real stage, so spawn/load pass-throughs don't fire), re-roll the personality within the root's
     * policy. Runs after MCA's body, overriding its out-of-policy age-up randomization on newer builds
     * and supplying the drift entirely on older ones. Server-only.
     */
    @Inject(method = "setAgeState", at = @At("RETURN"))
    private void townstead$rerollPersonalityOnAgeChange(AgeState state, CallbackInfoReturnable<Boolean> cir) {
        VillagerEntityMCA self = (VillagerEntityMCA) (Object) this;
        if (self.level().isClientSide || !cir.getReturnValueZ()) return;
        RootSpawnHandler.rerollPersonalityForAgeChange(self);
    }
}
