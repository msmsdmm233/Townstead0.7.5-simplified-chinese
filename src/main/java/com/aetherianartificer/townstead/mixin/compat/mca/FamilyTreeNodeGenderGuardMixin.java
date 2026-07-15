package com.aetherianartificer.townstead.mixin.compat.mca;

import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.FamilyTreeNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Never let a family-tree node's assigned gender be reset to UNASSIGNED. MCA's editor sync
 * reaches {@code setGender} with a gender parsed from packet NBT that several paths don't
 * actually carry (the 1.20.1 line's hair/clothing/gender commands read it from a data compound
 * holding only an {@code offset}; the 1.21.1 line reads a key its own save never writes), which
 * parses as UNASSIGNED and silently wipes the node — the player's gendered family dialogue
 * (children's parent lines) and AI-chat gender break with it. No MCA path intends
 * UNASSIGNED-over-assigned: its only setGender callers are the editor sync and the
 * metamorphosis potion, which always passes MALE/FEMALE.
 */
@Mixin(FamilyTreeNode.class)
public abstract class FamilyTreeNodeGenderGuardMixin {

    @Inject(method = "setGender", at = @At("HEAD"), cancellable = true, remap = false)
    private void townstead$keepAssignedGender(Gender gender, CallbackInfo ci) {
        if (gender == Gender.UNASSIGNED
                && ((FamilyTreeNode) (Object) this).gender() != Gender.UNASSIGNED) {
            ci.cancel();
        }
    }
}
