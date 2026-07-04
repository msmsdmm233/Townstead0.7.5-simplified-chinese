package com.aetherianartificer.townstead.mixin.client;

import com.aetherianartificer.townstead.client.species.RigModels;
import com.aetherianartificer.townstead.root.rig.RigDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rests a non-humanoid species rig (e.g. a spider) upright when it sleeps.
 *
 * <p>The host renderer's {@code setupRotations} applies the vanilla humanoid sleeping transform (yaw to
 * the bed, a 90° roll to lay flat, then a 270° spin), which is right for a humanoid but lays a spider on
 * its side clipping through the mattress. For a sleeping generic (non-humanoid) rig this replaces the
 * whole method with an upright rotation keyed to the bed's orientation (so the rig faces the same way
 * every night regardless of how the entity climbed in), then cancels so the vanilla lay-down never runs.
 * Humanoid rigs are left untouched (they lie down correctly), as are all non-sleeping entities.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSleepMixin {

    private static boolean townstead$standUpright(LivingEntity entity, PoseStack poseStack, float bodyYaw) {
        if (!entity.isSleeping()) return false;
        String rigBase = RigModels.rigBaseFor(entity);
        if (!RigModels.isGeneric(rigBase)) return false;
        // Orient off the bed, not the entity: a sleeping villager's body yaw drifts with how it climbed
        // in, so keying off it faces the rig a different way each night. The bed direction is fixed, so
        // the rig lands the same way every time (matching how vanilla derives humanoid sleep facing).
        // Datapack sleep.yaw is the offset from the bed's foot; fall back to standing yaw if no bed.
        RigDefinition.BodyPose sleep = RigModels.sleepPose(rigBase);
        float yaw = sleep == null ? 0f : sleep.yaw();
        Direction bed = entity.getBedOrientation();
        float base = bed != null ? townstead$bedRotation(bed) : 180f - bodyYaw;
        poseStack.mulPose(Axis.YP.rotationDegrees(base + yaw));
        if (sleep != null) {
            if (sleep.pitch() != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(sleep.pitch()));
            if (sleep.roll() != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(sleep.roll()));
        }
        return true;
    }

    // Fixed yaw per bed direction, mirroring vanilla LivingEntityRenderer.sleepDirectionToRotation.
    private static float townstead$bedRotation(Direction bed) {
        return switch (bed) {
            case SOUTH -> 90f;
            case WEST -> 0f;
            case NORTH -> 270f;
            case EAST -> 180f;
            default -> 0f;
        };
    }

    //? if neoforge {
    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void townstead$restUprightWhenSleeping(
            LivingEntity entity, PoseStack poseStack, float ageInTicks, float bodyYaw,
            float partialTick, float scale, CallbackInfo ci) {
        if (townstead$standUpright(entity, poseStack, bodyYaw)) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "m_7523_", remap = false, at = @At("HEAD"), cancellable = true, require = 0)
    private void townstead$restUprightWhenSleeping(
            LivingEntity entity, PoseStack poseStack, float ageInTicks, float bodyYaw,
            float partialTick, CallbackInfo ci) {
        if (townstead$standUpright(entity, poseStack, bodyYaw)) ci.cancel();
    }
    *///?}
}
