package com.aetherianartificer.townstead.mixin.compat.mca;

import net.conczin.mca.network.c2s.GetVillagerRequest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Strips stale editor-transient family keys from a player's editor buffer as it's fetched.
 * MCA's player editor save stores the WHOLE buffer as the player's snapshot, so one session's
 * father/mother/spouse edits ({@code tree_*_new} on the 1.20.1 line, {@code FamilyTreeNew*Name}
 * on the 1.21.1 line) persist and REPLAY on every later save — a stale blank silently removes
 * the player's typed-in parents (the "my parent names poofed" report). The returned compound is
 * the live stored instance, so this also scrubs the persisted snapshot. Removing a parent on
 * purpose still works: that key rides the live save packet and is consumed before this runs.
 */
@Mixin(GetVillagerRequest.class)
public abstract class GetVillagerRequestEditorGuardMixin {

    @org.spongepowered.asm.mixin.Unique
    private static final String[] TOWNSTEAD$TRANSIENT_FAMILY_KEYS = {
            "tree_father_new", "tree_mother_new", "tree_spouse_new",
            "FamilyTreeNewFatherName", "FamilyTreeNewMotherName", "FamilyTreeNewSpouseName"
    };

    @Inject(method = "getVillagerData", at = @At("RETURN"), remap = false)
    private static void townstead$stripStaleFamilyEdits(Entity e, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag data = cir.getReturnValue();
        if (data == null || !(e instanceof ServerPlayer)) return;
        for (String key : TOWNSTEAD$TRANSIENT_FAMILY_KEYS) {
            data.remove(key);
        }
    }
}
