package com.aetherianartificer.townstead.client.animation.emote;

import com.aetherianartificer.townstead.client.species.RigModels;
import com.aetherianartificer.townstead.root.rig.RigDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

/**
 * Applies the active emote's {@code body} bone translation and rotation at the
 * entity-render matrix stack, mirroring Emotecraft's {@code PlayerRendererMixin}
 * which reads {@code get3DTransform("body", …)} and applies
 * {@code poseStack.translate(body.x, body.y + 0.7, body.z)} plus axis rotations
 * on the player.
 *
 * <p>The {@code body} bone is authored in <b>block units</b> (Emotecraft binary
 * stores it that way; GeckoLib divides Bedrock-format body translation by 16).
 * The sibling {@code torso} bone is a separate channel that targets the body
 * ModelPart cube in pixel units via {@link EmoteSampler} / {@link
 * EmoteBoneMapping}; reading {@code torso} as a matrix-level fallback would
 * apply pixel values as blocks and launch the entity 16× too far.</p>
 *
 * <p>The {@code +0.7 / -0.7} pivot offset matches Emotecraft's approach: lift to
 * roughly the entity's center, rotate around that, then bring the matrix back so
 * subsequent rendering proceeds from the entity's feet.</p>
 */
public final class EmoteBodyTransformSampler {
    private static final double PIVOT_HEIGHT = 0.7;

    private EmoteBodyTransformSampler() {}

    /** The blend-applied, UNSCALED body transform an emote authors (block units), or null if inactive. */
    private record Body(float xPos, float yPos, float zPos, float xRot, float yRot, float zRot) {}

    /**
     * Apply the body transform for a non-player entity (the villager path; players are handled by
     * {@link #applyPlayerCorrection}). The rig's {@link RigDefinition.BodyMotion} scales the lift/drop and
     * clamps how far it can sink below the feet; an entity with no emote rig block keeps the full transform.
     */
    public static void apply(LivingEntity entity, PoseStack poseStack, float partialTick) {
        Body b = sampleBody(entity, partialTick);
        if (b == null) return;

        RigDefinition.BodyMotion bm = bodyMotionFor(entity);
        if (!bm.active()) return;

        float xPos = b.xPos * bm.scale();
        float yPos = bm.clampY(b.yPos);
        float zPos = b.zPos * bm.scale();
        float xRot = b.xRot * bm.scale();
        float yRot = b.yRot * bm.scale();
        float zRot = b.zRot * bm.scale();

        boolean hasTrans = xPos != 0F || yPos != 0F || zPos != 0F;
        boolean hasRot = xRot != 0F || yRot != 0F || zRot != 0F;
        if (!hasTrans && !hasRot) return;

        poseStack.translate(xPos, yPos + PIVOT_HEIGHT, zPos);
        if (hasRot) {
            poseStack.mulPose(Axis.ZP.rotation(zRot));
            poseStack.mulPose(Axis.YP.rotation(yRot));
            poseStack.mulPose(Axis.XP.rotation(xRot));
        }
        poseStack.translate(0, -PIVOT_HEIGHT, 0);
    }

    /**
     * For a player whose rig limits body motion, correct Emotecraft's already-applied full body transform.
     * Emotecraft drives the player natively (its own renderer mixin), bypassing our {@code body_motion}
     * gate, so a low spider-folk player sinks on a humanoid-scale "sit". We add the world-space delta that
     * scales the translation and clamps the drop. For the descend emotes that actually sink (≈no body
     * rotation) the delta is exact; the rotation Emotecraft applied is left as-is. A normal player (no emote
     * rig block, or no declared limit) is untouched, so Emotecraft's transform stands.
     */
    public static void applyPlayerCorrection(LivingEntity entity, PoseStack poseStack, float partialTick) {
        RigDefinition def = RigModels.definition(RigModels.rigBaseFor(entity));
        if (def == null || def.emote() == null) return;
        RigDefinition.BodyMotion bm = def.emote().bodyMotion();
        if (!bm.limited()) return;

        Body b = sampleBody(entity, partialTick);
        if (b == null) return;

        float dx = b.xPos * bm.scale() - b.xPos;
        float dy = bm.clampY(b.yPos) - b.yPos;
        float dz = b.zPos * bm.scale() - b.zPos;
        if (dx == 0F && dy == 0F && dz == 0F) return;
        poseStack.translate(dx, dy, dz);
    }

    /** The rig's body-motion policy, or full motion when the entity declares no emote rig block. */
    private static RigDefinition.BodyMotion bodyMotionFor(LivingEntity entity) {
        RigDefinition def = RigModels.definition(RigModels.rigBaseFor(entity));
        if (def == null || def.emote() == null) return RigDefinition.BodyMotion.FULL;
        return def.emote().bodyMotion();
    }

    private static Body sampleBody(LivingEntity entity, float partialTick) {
        EmotePlayback playback = EmotePlaybackRegistry.get(entity.getUUID());
        if (playback == null) return null;
        ParsedEmote emote = EmoteRegistry.get(playback.emoteId()).orElse(null);
        if (emote == null) return null;
        ParsedBoneAnimation bodyBone = emote.bones().get("body");
        if (bodyBone == null) return null;

        long now = entity.level().getGameTime();
        float elapsed = ((now - playback.startGameTime()) + partialTick) * playback.speedMultiplier();
        float t = resolveTime(elapsed, emote, playback.loopType());
        if (t < 0F && playback.frozenTick() < 0F) return null;
        if (t < 0F) t = playback.frozenTick();

        float blend = blend(playback, now, partialTick, elapsed);
        if (blend <= 0F) return null;

        boolean easingBefore = emote.easingBefore();
        float xPos = bodyBone.translationKeyed()
                ? sample(bodyBone.xPos(), bodyBone.xPosDefault(), t, easingBefore) * blend : 0F;
        float yPos = bodyBone.translationKeyed()
                ? sample(bodyBone.yPos(), bodyBone.yPosDefault(), t, easingBefore) * blend : 0F;
        float zPos = bodyBone.translationKeyed()
                ? sample(bodyBone.zPos(), bodyBone.zPosDefault(), t, easingBefore) * blend : 0F;
        float xRot = sample(bodyBone.xRot(), bodyBone.xRotDefault(), t, easingBefore) * blend;
        float yRot = sample(bodyBone.yRot(), bodyBone.yRotDefault(), t, easingBefore) * blend;
        float zRot = sample(bodyBone.zRot(), bodyBone.zRotDefault(), t, easingBefore) * blend;
        return new Body(xPos, yPos, zPos, xRot, yRot, zRot);
    }

    private static float blend(EmotePlayback playback, long now, float partialTick, float elapsedTicks) {
        if (playback.isFadingOut()) {
            float duration = playback.fadeOutDurationTicks() > 0F ? playback.fadeOutDurationTicks() : 6.0F;
            float fadeElapsed = (now - playback.fadingOutStartGameTime()) + partialTick;
            if (fadeElapsed <= 0F) return 1F;
            if (fadeElapsed >= duration) return 0F;
            return 1F - (fadeElapsed / duration);
        }
        return Math.min(1F, Math.max(0F, elapsedTicks / 4.0F));
    }

    private static float resolveTime(float elapsed, ParsedEmote emote, ParsedEmote.LoopType loopType) {
        int stop = Math.max(emote.stopTick(), Math.max(emote.endTick(), 1));
        if (loopType == ParsedEmote.LoopType.LOOP) {
            int loopStart = Math.max(0, emote.returnToTick());
            int loopSpan = Math.max(1, stop - loopStart);
            if (elapsed < 0F) return 0F;
            if (elapsed <= stop) return elapsed;
            float past = elapsed - stop;
            return loopStart + (past % loopSpan);
        }
        if (elapsed < 0F) return 0F;
        if (elapsed > stop) return -1F;
        return elapsed;
    }

    private static float sample(List<ParsedKeyframe> keyframes, float restValue, float tick, boolean easingBefore) {
        if (keyframes == null || keyframes.isEmpty()) return restValue;
        ParsedKeyframe first = keyframes.get(0);
        if (tick <= 0F) return restValue;
        if (tick <= first.tick()) {
            return easeBetween(restValue, first.value(), 0F, first.tick(), tick, first.easing(), first.easingArg());
        }
        for (int i = 1; i < keyframes.size(); i++) {
            ParsedKeyframe prev = keyframes.get(i - 1);
            ParsedKeyframe next = keyframes.get(i);
            if (tick <= next.tick()) {
                // Upstream KeyframeAnimationPlayer$Axis: pick prev when
                // isEasingBefore=false (legacy default), next when true.
                ParsedKeyframe carrier = easingBefore ? next : prev;
                return easeBetween(prev.value(), next.value(), prev.tick(), next.tick(), tick, carrier.easing(), carrier.easingArg());
            }
        }
        return keyframes.get(keyframes.size() - 1).value();
    }

    private static float easeBetween(float a, float b, float startTick, float endTick, float tick, EmoteEasing easing, Float easingArg) {
        float span = endTick - startTick;
        if (span <= 0F) return b;
        float alpha = Mth.clamp((tick - startTick) / span, 0F, 1F);
        return Mth.lerp(easing.apply(alpha, easingArg), a, b);
    }
}
