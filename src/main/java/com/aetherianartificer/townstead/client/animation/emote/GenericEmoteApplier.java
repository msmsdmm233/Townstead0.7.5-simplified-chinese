package com.aetherianartificer.townstead.client.animation.emote;

import com.aetherianartificer.townstead.client.animation.emote.loader.EmoteReflection;
import com.aetherianartificer.townstead.root.rig.RigDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Drives an Emotecraft emote onto a non-humanoid (generic) rig's bones, applying the rig's per-channel
 * remap. The humanoid bridge ({@link com.aetherianartificer.townstead.client.animation.McaAnimationBridge})
 * is structurally bound to {@code HumanoidModel} (breasts, wear-sync, MCA params) and can't drive a
 * spider model, so this is a lean, model-agnostic parallel path: resolve the active playback (mirroring
 * {@link EmoteBodyTransformSampler}'s standalone resolution), sample each emote channel, rebase its
 * rotation into the target bone's frame per the rig's {@link RigDefinition.EmoteChannel}, and write it.
 *
 * <p>Called from the generic render branch AFTER the model's own {@code setupAnim} and the rig pose, so
 * {@link RigDefinition.EmoteMode#ADDITIVE} channels add their delta on top of the live gait pose.</p>
 */
public final class GenericEmoteApplier {
    private static final float FADE_IN_TICKS = 4.0F;

    private GenericEmoteApplier() {}

    public static void apply(LivingEntity entity, ModelPart root, RigDefinition def, float partialTick) {
        if (root == null || def == null || def.emote() == null) return;
        RigDefinition.EmoteMap map = def.emote();

        // Bend (unlike rotation) is not reset by the model's setupAnim and would otherwise persist on the
        // shared cached model after the emote ends or onto the next entity sharing this rig. Clear it on
        // every bend-enabled bone up front, before any early-out, then re-apply below while active.
        clearBends(root, map);

        UUID uuid = entity.getUUID();
        EmotePlayback playback = EmotePlaybackRegistry.get(uuid);
        if (playback == null) return;
        ParsedEmote emote = EmoteRegistry.get(playback.emoteId()).orElse(null);
        if (emote == null) return;

        long now = entity.level().getGameTime();
        float elapsed = ((now - playback.startGameTime()) + partialTick) * playback.speedMultiplier();
        float t = resolveTime(elapsed, emote, playback.loopType());
        if (t < 0F && playback.frozenTick() < 0F) return;
        if (t < 0F) t = playback.frozenTick();

        float blend = blend(playback, now, partialTick, elapsed);
        if (blend <= 0F) return;

        Set<String> skipped = playback.skippedBones();
        boolean easingBefore = emote.easingBefore();

        for (Map.Entry<String, ParsedBoneAnimation> entry : emote.bones().entrySet()) {
            // Emote bone keys are normalized (lowercased, underscores stripped: "rightarm"); the rig's
            // channel map is keyed by canonical names ("right_arm"). Route through EmoteBoneMapping to
            // line them up, exactly as the humanoid EmoteSampler does. Null = an unmapped bone or the
            // matrix "body" channel (handled by EmoteBodyTransformSampler), so skip here.
            String channel = EmoteBoneMapping.mapTargets(entry.getKey()).primary();
            if (channel == null) continue;
            if (skipped != null && skipped.contains(channel)) continue;
            RigDefinition.EmoteChannel ch = map.channels().get(channel);
            if (ch == null) continue; // channel not expressible on this body
            ParsedBoneAnimation bone = entry.getValue();
            if (!bone.hasAnyKeyframes()) continue;

            EmoteSampler.BonePose pose = EmoteSampler.samplePose(bone, t, easingBefore);
            applyChannel(root, ch, pose, blend);
        }
    }

    private static void applyChannel(ModelPart root, RigDefinition.EmoteChannel ch,
                                     EmoteSampler.BonePose pose, float blend) {
        applyOne(root, ch.bone(), ch.mode(), ch.axisPerm(), ch.axisSign(), ch.gain(), ch.euler(),
                ch.clampMin(), ch.clampMax(), ch.translation(), pose, blend);
        // Segment bend rides only the primary bone (the leg/abdomen that flexes), scaled by bendGain.
        if (ch.bend() && pose.hasBend() && root.hasChild(ch.bone())) {
            EmoteReflection.applyBend(root.getChild(ch.bone()), pose.bendDirection(),
                    blend * ch.bendGain() * pose.bend());
        }
        // Fan-out followers always ADD (they ride the primary's motion) with their own gain.
        for (RigDefinition.EmoteFan fan : ch.also()) {
            applyOne(root, fan.bone(), RigDefinition.EmoteMode.ADDITIVE, ch.axisPerm(), ch.axisSign(),
                    fan.gain(), ch.euler(), ch.clampMin(), ch.clampMax(), false, pose, blend);
        }
    }

    /** Zero the bend on every bend-enabled channel bone, clearing any value left from a prior frame/entity. */
    private static void clearBends(ModelPart root, RigDefinition.EmoteMap map) {
        for (RigDefinition.EmoteChannel ch : map.channels().values()) {
            if (ch.bend() && root.hasChild(ch.bone())) {
                EmoteReflection.applyBend(root.getChild(ch.bone()), 0F, 0F);
            }
        }
    }

    private static void applyOne(ModelPart root, String boneName, RigDefinition.EmoteMode mode,
                                 int[] perm, float[] sign, float[] gain, float[] euler,
                                 float[] clampMin, float[] clampMax, boolean translation,
                                 EmoteSampler.BonePose pose, float blend) {
        if (boneName == null || boneName.isEmpty() || !root.hasChild(boneName)) return;
        ModelPart part = root.getChild(boneName);

        float[] src = {pose.xRot(), pose.yRot(), pose.zRot()};
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) {
            out[i] = Mth.clamp(sign[i] * gain[i] * src[perm[i]] + euler[i], clampMin[i], clampMax[i]);
        }

        if (mode == RigDefinition.EmoteMode.ADDITIVE) {
            part.xRot += blend * out[0];
            part.yRot += blend * out[1];
            part.zRot += blend * out[2];
        } else {
            part.xRot = Mth.lerp(blend, part.xRot, out[0]);
            part.yRot = Mth.lerp(blend, part.yRot, out[1]);
            part.zRot = Mth.lerp(blend, part.zRot, out[2]);
        }

        // Translation is opt-in and uncommon (waves/nods/points are rotation-only; whole-body lift is
        // handled separately by EmoteBodyTransformSampler). NOTE: the vanilla model's setupAnim resets
        // rotations every frame but NOT positions, so an ADDITIVE translation here would persist on the
        // shared cached model — fine while the emote runs, but a position-translating emote should be
        // dialed against that (capture-base like the crouch pose) before relying on it broadly.
        if (translation && pose.hasTranslation()) {
            float[] srcPos = {pose.xPos(), pose.yPos(), pose.zPos()};
            float[] outPos = new float[3];
            for (int i = 0; i < 3; i++) outPos[i] = sign[i] * gain[i] * srcPos[perm[i]];
            if (mode == RigDefinition.EmoteMode.ADDITIVE) {
                part.x += blend * outPos[0];
                part.y += blend * outPos[1];
                part.z += blend * outPos[2];
            } else {
                part.x = Mth.lerp(blend, part.x, outPos[0]);
                part.y = Mth.lerp(blend, part.y, outPos[1]);
                part.z = Mth.lerp(blend, part.z, outPos[2]);
            }
        }
    }

    // --- playback time/blend resolution (mirrors EmoteBodyTransformSampler; kept standalone so the
    // humanoid adapter stays untouched) ---

    private static float blend(EmotePlayback playback, long now, float partialTick, float elapsedTicks) {
        if (playback.isFadingOut()) {
            float duration = playback.fadeOutDurationTicks() > 0F ? playback.fadeOutDurationTicks() : 6.0F;
            float fadeElapsed = (now - playback.fadingOutStartGameTime()) + partialTick;
            if (fadeElapsed <= 0F) return 1F;
            if (fadeElapsed >= duration) return 0F;
            return 1F - (fadeElapsed / duration);
        }
        return Math.min(1F, Math.max(0F, elapsedTicks / FADE_IN_TICKS));
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
}
