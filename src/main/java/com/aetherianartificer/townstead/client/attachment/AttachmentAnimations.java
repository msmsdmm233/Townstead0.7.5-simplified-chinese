package com.aetherianartificer.townstead.client.attachment;

import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionContext;
import com.aetherianartificer.townstead.pheno.condition.Conditions;
import com.aetherianartificer.townstead.root.attachment.AttachmentAnimation;
import com.aetherianartificer.townstead.root.attachment.AttachmentDef;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side keyframe playback for attachments. Trigger entries mirror poses —
 * a named state or pheno condition starts its clip on activation and fades it out on
 * deactivation — plus a {@code random_idle} pool that fires a weighted one-shot when
 * nothing else is animating (ear flicks, tail flourishes). Clips run on the entity's
 * age clock; a clip's own {@code loop} flag decides whether it repeats, holds its
 * last frame, or ends.
 *
 * <p>Triggers re-evaluate once per entity tick; sampling and the fade envelope run
 * per frame. Output values stay in the animation file's Blockbench convention
 * (rotation degrees, position pixels) — the render layer does the Java-space flips,
 * the same contract as pose bone rotations. State is per entity+definition and
 * evicted when stale.</p>
 */
public final class AttachmentAnimations {

    // Parsed conditions per def id, index-aligned with def.animations(); cleared on
    // manifest so live adjusts and reloads re-parse.
    private static final Map<String, List<Condition>> CONDITIONS = new ConcurrentHashMap<>();

    private static final Map<Integer, EntityState> STATES = new HashMap<>();
    private static final long EVICT_AFTER_MS = 10_000;
    private static final float FADE_EPSILON = 0.01f;

    private AttachmentAnimations() {}

    public static void onManifest() {
        CONDITIONS.clear();
        STATES.clear();
    }

    /** Sampled bone channels: rotation degrees + position pixels (geo convention), scale multipliers. */
    public record Sample(Map<String, float[]> rotations, Map<String, float[]> positions,
                         Map<String, float[]> scales) {}

    /** The blended keyframe output for this entity+attachment right now, or null when silent. */
    @Nullable
    public static Sample sample(LivingEntity entity, AttachmentDef def, float ageInTicks) {
        if (def.animations().isEmpty()) return null;
        EntityState entityState = STATES.computeIfAbsent(entity.getId(), k -> new EntityState());
        entityState.lastTouchMs = Util.getMillis();
        if (STATES.size() > 256) evictStale();
        DefAnimState state = entityState.defs.computeIfAbsent(def.id(),
                k -> new DefAnimState(def.animations().size(), ageInTicks));

        int tick = (int) ageInTicks;
        if (state.lastEvalTick != tick) {
            state.lastEvalTick = tick;
            retrigger(entity, def, state, ageInTicks);
        }
        float dt = state.lastAge < 0 ? 1f : Math.max(0f, ageInTicks - state.lastAge);
        state.lastAge = ageInTicks;

        Map<String, float[]> rotations = null;
        Map<String, float[]> positions = null;
        Map<String, float[]> scales = null;
        List<AttachmentDef.AnimationEntry> entries = def.animations();
        float[] work = new float[3];
        for (int i = 0; i < entries.size(); i++) {
            Playback playback = state.playbacks[i];
            float target = playback.playing ? 1f : 0f;
            float transition = Math.max(0.01f, entries.get(i).transitionTicks());
            playback.fade += (target - playback.fade) * Math.min(1f, (float) (1.0 - Math.exp(-3.0 * dt / transition)));
            if (playback.fade < FADE_EPSILON) {
                playback.fade = playback.playing ? playback.fade : 0f;
                continue;
            }
            AttachmentAnimation.Clip clip = AttachmentClient.animationClip(
                    entries.get(i).animSha(), entries.get(i).clip());
            if (clip == null) continue;
            float time = ageInTicks - playback.startAge;
            if (clip.loop == AttachmentAnimation.Loop.LOOP && !entries.get(i).idle()) {
                time %= clip.lengthTicks;
            } else {
                time = Math.min(time, clip.lengthTicks);
            }
            for (var bone : clip.bones.entrySet()) {
                AttachmentAnimation.BoneTrack track = bone.getValue();
                if (track.rotation() != null) {
                    if (rotations == null) rotations = new LinkedHashMap<>();
                    accumulate(rotations, bone.getKey(), track.rotation().sample(time, work), playback.fade, 0f);
                }
                if (track.position() != null) {
                    if (positions == null) positions = new LinkedHashMap<>();
                    accumulate(positions, bone.getKey(), track.position().sample(time, work), playback.fade, 0f);
                }
                if (track.scale() != null) {
                    if (scales == null) scales = new LinkedHashMap<>();
                    accumulateScale(scales, bone.getKey(), track.scale().sample(time, work), playback.fade);
                }
            }
        }
        if (rotations == null && positions == null && scales == null) return null;
        return new Sample(rotations == null ? Map.of() : rotations,
                positions == null ? Map.of() : positions,
                scales == null ? Map.of() : scales);
    }

    /** Additive channels sum their faded values across active clips. */
    private static void accumulate(Map<String, float[]> map, String bone, float[] value,
                                   float fade, float identity) {
        float[] slot = map.computeIfAbsent(bone, k -> new float[]{identity, identity, identity});
        for (int i = 0; i < 3; i++) slot[i] += value[i] * fade;
    }

    /** Scale multiplies, eased toward identity by the fade so clips blend in/out smoothly. */
    private static void accumulateScale(Map<String, float[]> map, String bone, float[] value, float fade) {
        float[] slot = map.computeIfAbsent(bone, k -> new float[]{1f, 1f, 1f});
        for (int i = 0; i < 3; i++) slot[i] *= 1f + (value[i] - 1f) * fade;
    }

    /** Once per tick: start/stop trigger entries, roll the idle pool when everything is quiet. */
    private static void retrigger(LivingEntity entity, AttachmentDef def, DefAnimState state, float age) {
        List<Condition> conditions = CONDITIONS.computeIfAbsent(def.id(), k -> parseConditions(def));
        ConditionContext ctx = new ConditionContext(entity);
        List<AttachmentDef.AnimationEntry> entries = def.animations();
        boolean anyActive = false;
        for (int i = 0; i < entries.size(); i++) {
            AttachmentDef.AnimationEntry entry = entries.get(i);
            Playback playback = state.playbacks[i];
            if (entry.idle()) {
                // An idle one-shot ends at its clip length (a looping idle would never yield).
                if (playback.playing && pastEnd(entry, playback, age, false)) {
                    playback.playing = false;
                    playback.cooldownUntil = age + entry.cooldownTicks();
                }
            } else {
                boolean should = entry.state().isEmpty()
                        ? conditions.get(i) != null && conditions.get(i).test(ctx)
                        : AttachmentPoses.stateActive(entity, entry.state());
                if (should && !playback.wasActive) {
                    playback.playing = true;
                    playback.startAge = age;
                } else if (!should) {
                    playback.playing = false;
                } else if (playback.playing && pastEnd(entry, playback, age, true)) {
                    playback.playing = false;   // ONCE clip finished; re-arms on the next activation
                }
                playback.wasActive = should;
            }
            anyActive |= playback.playing || playback.fade >= FADE_EPSILON;
        }
        if (anyActive || age < state.nextIdleRoll) return;
        rollIdle(entries, state, age);
    }

    /** Whether a playing clip has run past its end (loop/hold clips never do while active). */
    private static boolean pastEnd(AttachmentDef.AnimationEntry entry, Playback playback,
                                   float age, boolean respectLoop) {
        AttachmentAnimation.Clip clip = AttachmentClient.animationClip(entry.animSha(), entry.clip());
        if (clip == null) return false;
        if (respectLoop && clip.loop != AttachmentAnimation.Loop.ONCE) return false;
        return age - playback.startAge > clip.lengthTicks;
    }

    private static void rollIdle(List<AttachmentDef.AnimationEntry> entries, DefAnimState state, float age) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        state.nextIdleRoll = age + 40f + random.nextFloat() * 80f;
        float totalWeight = 0f;
        for (int i = 0; i < entries.size(); i++) {
            AttachmentDef.AnimationEntry entry = entries.get(i);
            if (entry.idle() && age >= state.playbacks[i].cooldownUntil) {
                totalWeight += Math.max(0f, entry.weight());
            }
        }
        if (totalWeight <= 0f) return;
        float pick = random.nextFloat() * totalWeight;
        for (int i = 0; i < entries.size(); i++) {
            AttachmentDef.AnimationEntry entry = entries.get(i);
            if (!entry.idle() || age < state.playbacks[i].cooldownUntil) continue;
            pick -= Math.max(0f, entry.weight());
            if (pick <= 0f) {
                state.playbacks[i].playing = true;
                state.playbacks[i].startAge = age;
                return;
            }
        }
    }

    private static List<Condition> parseConditions(AttachmentDef def) {
        List<Condition> out = new ArrayList<>(def.animations().size());
        for (AttachmentDef.AnimationEntry entry : def.animations()) {
            Condition condition = null;
            if (!entry.conditionJson().isEmpty()) {
                try {
                    condition = Conditions.parse(JsonParser.parseString(entry.conditionJson()));
                } catch (Exception ignored) {
                }
            }
            out.add(condition);
        }
        return out;
    }

    private static void evictStale() {
        long now = Util.getMillis();
        STATES.values().removeIf(state -> now - state.lastTouchMs > EVICT_AFTER_MS);
    }

    private static final class EntityState {
        long lastTouchMs;
        final Map<String, DefAnimState> defs = new HashMap<>();
    }

    private static final class DefAnimState {
        final Playback[] playbacks;
        int lastEvalTick = Integer.MIN_VALUE;
        float lastAge = -1f;
        float nextIdleRoll;

        DefAnimState(int entryCount, float age) {
            playbacks = new Playback[entryCount];
            for (int i = 0; i < entryCount; i++) playbacks[i] = new Playback();
            // First idle fires a little after the entity appears, never instantly.
            nextIdleRoll = age + 40f + ThreadLocalRandom.current().nextFloat() * 80f;
        }
    }

    private static final class Playback {
        boolean playing;
        boolean wasActive;
        float startAge;
        float fade;
        float cooldownUntil;
    }
}
