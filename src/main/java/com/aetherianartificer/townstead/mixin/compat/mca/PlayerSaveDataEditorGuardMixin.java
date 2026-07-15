package com.aetherianartificer.townstead.mixin.compat.mca;

import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Heals the villager-like snapshot MCA stores for a player before it replaces the old one
 * (editor saves, metamorphosis potion). MCA's editor save ships a dummy-villager save whose
 * gender key differs by MCA line ({@code gender} on 1.20.1, {@code Gender} on the 1.21.1 line)
 * while each line's readers ({@code PlayerSaveData.getGender}, {@code syncFamilyTree}) only
 * check one of them — a mismatch collapses the player's gender to UNASSIGNED, which flips the
 * {@code #G} dialogue-key marker and the family-tree node gender, breaking gendered family
 * dialogue (children stop using their parent lines) and AI-chat gender.
 *
 * <p>The incoming compound is the SAME instance MCA hands to {@code syncFamilyTree} right
 * after, on every MCA line, so normalizing it here heals both the stored snapshot and the
 * family-tree write in one place.</p>
 */
@Mixin(PlayerSaveData.class)
public abstract class PlayerSaveDataEditorGuardMixin {

    @Inject(method = "setEntityData", at = @At("HEAD"), remap = false)
    private void townstead$healPlayerSnapshot(CompoundTag entityData, CallbackInfo ci) {
        if (entityData == null) return;
        CompoundTag stored = ((PlayerSaveData) (Object) this).getEntityData();

        // Gender: first meaningful value across both lines' keys, falling back to the
        // previously stored snapshot, then the family-tree node; mirrored onto every key
        // so whichever one this MCA build reads sees it. 0 = UNASSIGNED on all lines.
        int gender = townstead$gender(entityData);
        if (gender == 0 && stored != null) gender = townstead$gender(stored);
        if (gender == 0) {
            gender = ((PlayerSaveData) (Object) this).getFamilyEntry().gender().ordinal();
        }
        if (gender != 0) {
            entityData.putInt("gender", gender);
            entityData.putInt("Gender", gender);
            if (entityData.contains("MCAData", 10)) {
                entityData.getCompound("MCAData").putInt("Gender", gender);
            }
        }

        // Name: the 1.20.1 line's syncFamilyTree unconditionally renames the player's
        // family-tree node from this key; a dummy without one blanks the node's name.
        if (entityData.getString("villagerName").isBlank() && stored != null) {
            String name = stored.getString("villagerName");
            if (!name.isBlank()) entityData.putString("villagerName", name);
        }

        // The 1.21.1 line ships a FULL dummy save; its throwaway UUID has no business in
        // the player's snapshot.
        entityData.remove("UUID");
    }

    @org.spongepowered.asm.mixin.Unique
    private static int townstead$gender(CompoundTag nbt) {
        int g = nbt.getInt("gender");
        if (g == 0) g = nbt.getInt("Gender");
        if (g == 0 && nbt.contains("MCAData", 10)) g = nbt.getCompound("MCAData").getInt("Gender");
        return g;
    }
}
