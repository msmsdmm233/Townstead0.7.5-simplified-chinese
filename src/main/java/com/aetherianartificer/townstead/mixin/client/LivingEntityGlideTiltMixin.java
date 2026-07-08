package com.aetherianartificer.townstead.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tilts any fall-flying mob horizontal, the way {@code PlayerRenderer.setupRotations}
 * does for players. Vanilla's base {@code LivingEntityRenderer} has no fall-flying
 * branch at all (no vanilla mob glides), so a gene-gliding villager rendered bolt
 * upright mid-flight. Same math as the player: ease-in over the first ten flight
 * ticks, pitch to the view angle, roll toward the drift direction. Players are
 * excluded — their own renderer already applies this and routes through here via
 * {@code super}, which would double it.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityGlideTiltMixin {

    //? if neoforge {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void townstead$glideTilt(
            LivingEntity entity, PoseStack poseStack, float ageInTicks, float bodyYaw,
            float partialTick, float scale, CallbackInfo ci) {
        townstead$applyGlideTilt(entity, poseStack, partialTick);
    }
    //?} else {
    /*@Inject(method = "m_7523_", remap = false, at = @At("TAIL"), require = 0)
    private void townstead$glideTilt(
            LivingEntity entity, PoseStack poseStack, float ageInTicks, float bodyYaw,
            float partialTick, CallbackInfo ci) {
        townstead$applyGlideTilt(entity, poseStack, partialTick);
    }
    *///?}

    @Unique
    private static void townstead$applyGlideTilt(LivingEntity entity, PoseStack poseStack, float partialTick) {
        if (entity instanceof Player || !entity.isFallFlying()) return;
        float flight = entity.getFallFlyingTicks() + partialTick;
        float ease = Mth.clamp(flight * flight / 100.0F, 0.0F, 1.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(ease * (-90.0F - entity.getViewXRot(partialTick))));
        Vec3 view = entity.getViewVector(partialTick);
        Vec3 motion = entity.getDeltaMovement();
        double motionSqr = motion.horizontalDistanceSqr();
        double viewSqr = view.horizontalDistanceSqr();
        if (motionSqr > 0.0 && viewSqr > 0.0) {
            double cos = (motion.x * view.x + motion.z * view.z) / Math.sqrt(motionSqr * viewSqr);
            double cross = motion.x * view.z - motion.z * view.x;
            poseStack.mulPose(Axis.YP.rotation((float) (Math.signum(cross) * Math.acos(Mth.clamp(cos, -1.0, 1.0)))));
        }
    }
}
