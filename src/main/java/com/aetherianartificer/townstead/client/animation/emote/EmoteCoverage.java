package com.aetherianartificer.townstead.client.animation.emote;

import com.aetherianartificer.townstead.client.species.RigModels;
import com.aetherianartificer.townstead.root.rig.RigDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides whether an entity's rig is willing to play an emote ("a spider can wave but should not try to
 * Cossack-dance"), using the rig's {@link RigDefinition.EmotePolicy}. The default gate is automatic:
 * each emote's rotational motion energy is split into channels the rig can express vs not, and an emote
 * whose expressible share falls below {@code min_coverage} is refused. Explicit {@code allow}/{@code deny}
 * lists override, and a {@code fallback} emote can substitute for a refused one.
 *
 * <p>A rig with no {@code emote} block imposes no restriction (humanoid bodies express every emote the
 * normal way), so this only ever gates rigs that opt in.</p>
 */
public final class EmoteCoverage {
    private static final Map<ResourceLocation, Map<String, Float>> ENERGY_CACHE = new ConcurrentHashMap<>();

    private EmoteCoverage() {}

    /** Drop cached per-emote energy when the emote registry reloads. */
    public static void invalidate() {
        ENERGY_CACHE.clear();
    }

    /** Allowed = the rig will play it as-is; otherwise {@code fallback} (or null) names a substitute. */
    public record Decision(boolean allowed, ResourceLocation fallback) {
        static final Decision ALLOW = new Decision(true, null);
    }

    public static Decision decide(LivingEntity entity, ParsedEmote emote, ResourceLocation emoteId) {
        RigDefinition def = RigModels.definition(RigModels.rigBaseFor(entity));
        if (def == null || def.emote() == null) return Decision.ALLOW;
        RigDefinition.EmoteMap map = def.emote();
        RigDefinition.EmotePolicy policy = map.policy();

        if (matches(policy.deny(), emoteId, emote)) return refuse(policy);
        if (matches(policy.allow(), emoteId, emote)) return Decision.ALLOW;

        float min = policy.minCoverage();
        if (min <= 0F) return Decision.ALLOW;

        Map<String, Float> energy = energyOf(emote);
        float total = 0F;
        float expressible = 0F;
        for (Map.Entry<String, Float> e : energy.entrySet()) {
            total += e.getValue();
            if (map.expresses(e.getKey())) expressible += e.getValue();
        }
        // An emote with no rotational motion (e.g. a pure whole-body lift) has nothing to gate on; allow.
        if (total <= 1.0e-6F) return Decision.ALLOW;
        return (expressible / total) >= min ? Decision.ALLOW : refuse(policy);
    }

    private static Decision refuse(RigDefinition.EmotePolicy policy) {
        String fb = policy.fallback();
        if (fb == null || fb.isEmpty()) return new Decision(false, null);
        ResourceLocation id = ResourceLocation.tryParse(fb);
        return new Decision(false, id);
    }

    /** Per-channel summed rotation amplitude (radians) of an emote, cached by id. */
    private static Map<String, Float> energyOf(ParsedEmote emote) {
        return ENERGY_CACHE.computeIfAbsent(emote.id(), k -> computeEnergy(emote));
    }

    private static Map<String, Float> computeEnergy(ParsedEmote emote) {
        Map<String, Float> out = new HashMap<>();
        for (Map.Entry<String, ParsedBoneAnimation> e : emote.bones().entrySet()) {
            // Key by the canonical channel ("right_arm"), not the emote's normalized bone key
            // ("rightarm"), so it lines up with the rig's channel map in expresses(). Unmapped bones and
            // the matrix "body" channel resolve to null and don't count toward coverage.
            String channel = EmoteBoneMapping.mapTargets(e.getKey()).primary();
            if (channel == null) continue;
            float amp = amplitude(e.getValue());
            if (amp > 0F) out.merge(channel, amp, Float::sum);
        }
        return out;
    }

    private static float amplitude(ParsedBoneAnimation bone) {
        return range(bone.xRot()) + range(bone.yRot()) + range(bone.zRot());
    }

    private static float range(java.util.List<ParsedKeyframe> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) return 0F;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (ParsedKeyframe kf : keyframes) {
            float v = kf.value();
            if (v < min) min = v;
            if (v > max) max = v;
        }
        return Math.abs(max - min);
    }

    private static boolean matches(java.util.Set<String> names, ResourceLocation id, ParsedEmote emote) {
        if (names == null || names.isEmpty()) return false;
        if (names.contains(id.getPath().toLowerCase(java.util.Locale.ROOT))) return true;
        String display = emote.displayName();
        return display != null && names.contains(display.toLowerCase(java.util.Locale.ROOT));
    }
}
