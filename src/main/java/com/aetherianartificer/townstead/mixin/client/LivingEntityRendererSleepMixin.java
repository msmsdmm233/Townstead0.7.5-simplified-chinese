package com.aetherianartificer.townstead.mixin.client;

import com.aetherianartificer.townstead.client.species.RigModels;
import com.aetherianartificer.townstead.root.rig.RigDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * whole method with the ordinary standing rotation ({@code YP(180 - bodyYaw)}), so the rig stands upright
 * on the bed facing along it, then cancels so the vanilla lay-down never runs. Humanoid rigs are left
 * untouched (they lie down correctly), as are all non-sleeping entities.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSleepMixin {

    private static boolean townstead$standUpright(LivingEntity entity, PoseStack poseStack, float bodyYaw) {
        if (!entity.isSleeping()) return false;
        String rigBase = RigModels.rigBaseFor(entity);
        if (!RigModels.isGeneric(rigBase)) return false;
        // Same orientation a standing rig gets; the sleeping entity's body yaw already points along the bed.
        // The rig's sleep pose dials the whole-body orientation on the mattress from the datapack.
        RigDefinition.BodyPose sleep = RigModels.sleepPose(rigBase);
        float yaw = sleep == null ? 0f : sleep.yaw();
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw + yaw));
        if (sleep != null) {
            if (sleep.pitch() != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(sleep.pitch()));
            if (sleep.roll() != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(sleep.roll()));
        }
        return true;
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
