package com.aetherianartificer.townstead.profession.skill;

import com.aetherianartificer.townstead.profession.def.ProfessionDef;
import com.aetherianartificer.townstead.profession.def.ProfessionDefs;
import com.aetherianartificer.townstead.profession.def.RetrainingPolicy;
import com.aetherianartificer.townstead.profession.def.SkillDef;
import com.aetherianartificer.townstead.profession.def.SkillDefs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-entity set of learned skills, the runtime state professions grant capabilities from.
 * {@link #learn} enforces the declared model that is checkable from the skill graph today
 * (prerequisites and exclusivity); tier, point cost, unlock model, and XP gating are enforced
 * once per-entity progression state exists (deferred Townstead integration) and are noted on
 * each method. {@link #forget} respects the profession's retraining policy and cascades, so
 * dropping a prerequisite also drops everything that depended on it; LOCKED is permanent and
 * COSTLY is rejected until its payment mechanism exists (never silently free). {@link #forceLearn}
 * and {@link #forceForget} are explicit admin bypasses.
 *
 * <p>State is transient (in memory, keyed by entity UUID); durable persistence is part of the
 * deferred progression work. Mirrors the transient per-entity stores already used for ability
 * toggles and stacking effects.
 */
public final class LearnedSkills {

    private static final Map<UUID, Set<ResourceLocation>> STATE = new ConcurrentHashMap<>();

    private LearnedSkills() {}

    public static Set<ResourceLocation> learned(LivingEntity entity) {
        return learned(entity.getUUID());
    }

    public static Set<ResourceLocation> learned(UUID uuid) {
        Set<ResourceLocation> set = STATE.get(uuid);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    public static boolean has(LivingEntity entity, ResourceLocation skill) {
        return has(entity.getUUID(), skill);
    }

    public static boolean has(UUID uuid, ResourceLocation skill) {
        Set<ResourceLocation> set = STATE.get(uuid);
        return set != null && set.contains(skill);
    }

    /** Drop an entity's learned set. Wired to player logout so the transient store stays bounded. */
    public static void clear(UUID uuid) {
        STATE.remove(uuid);
    }

    public static Result learn(LivingEntity entity, ResourceLocation skillId) {
        return learn(entity.getUUID(), skillId);
    }

    public static Result forceLearn(LivingEntity entity, ResourceLocation skillId) {
        return forceLearn(entity.getUUID(), skillId);
    }

    public static ForgetResult forget(LivingEntity entity, ResourceLocation skillId) {
        return forget(entity.getUUID(), skillId);
    }

    public static ForgetResult forceForget(LivingEntity entity, ResourceLocation skillId) {
        return forceForget(entity.getUUID(), skillId);
    }

    /**
     * Learn a skill, enforcing prerequisites and exclusivity. Tier / points / unlock model / XP
     * gating is deferred until per-entity progression state exists; use {@link #forceLearn} to
     * bypass for admin setup.
     */
    public static Result learn(UUID uuid, ResourceLocation skillId) {
        SkillDef skill = SkillDefs.byId(skillId);
        if (skill == null) return Result.fail("unknown skill '" + skillId + "'");
        Set<ResourceLocation> set = STATE.computeIfAbsent(uuid, u -> new LinkedHashSet<>());
        if (set.contains(skillId)) return Result.fail("already learned");
        for (ResourceLocation req : skill.requires()) {
            if (!set.contains(req)) return Result.fail("missing prerequisite '" + req + "'");
        }
        ResourceLocation conflict = exclusivityConflict(skill, set);
        if (conflict != null) return Result.fail("excluded by learned skill '" + conflict + "'");
        set.add(skillId);
        return Result.success();
    }

    /** Admin bypass: record a learned skill without prerequisite or exclusivity checks. */
    public static Result forceLearn(UUID uuid, ResourceLocation skillId) {
        if (SkillDefs.byId(skillId) == null) return Result.fail("unknown skill '" + skillId + "'");
        STATE.computeIfAbsent(uuid, u -> new LinkedHashSet<>()).add(skillId);
        return Result.success();
    }

    /**
     * Forget a skill, honoring the profession's retraining policy and cascading to every learned
     * skill that (transitively) required it, so the learned set never becomes graph-invalid.
     */
    public static ForgetResult forget(UUID uuid, ResourceLocation skillId) {
        Set<ResourceLocation> set = STATE.get(uuid);
        if (set == null || !set.contains(skillId)) return ForgetResult.fail("not learned");
        SkillDef skill = SkillDefs.byId(skillId);
        if (skill != null) {
            ProfessionDef profession = ProfessionDefs.byId(skill.profession());
            if (profession != null) {
                switch (profession.retraining()) {
                    case LOCKED -> {
                        return ForgetResult.fail("retraining is locked for this profession");
                    }
                    // The cost (resources/time) is Townstead-owned and not built yet; until it is,
                    // costly retraining is rejected rather than silently treated as free.
                    case COSTLY -> {
                        return ForgetResult.fail("costly retraining is not available yet");
                    }
                    default -> { }
                }
            }
        }
        return ForgetResult.removed(cascadeRemove(set, skillId));
    }

    /** Admin bypass: forget regardless of retraining policy; still cascades to dependents. */
    public static ForgetResult forceForget(UUID uuid, ResourceLocation skillId) {
        Set<ResourceLocation> set = STATE.get(uuid);
        if (set == null || !set.contains(skillId)) return ForgetResult.fail("not learned");
        return ForgetResult.removed(cascadeRemove(set, skillId));
    }

    /** Remove the skill and then, to a fixpoint, any learned skill whose prerequisites are no longer met. */
    private static Set<ResourceLocation> cascadeRemove(Set<ResourceLocation> set, ResourceLocation skillId) {
        Set<ResourceLocation> removed = new LinkedHashSet<>();
        set.remove(skillId);
        removed.add(skillId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ResourceLocation learnedId : new LinkedHashSet<>(set)) {
                SkillDef learnedSkill = SkillDefs.byId(learnedId);
                if (learnedSkill != null && !set.containsAll(learnedSkill.requires())) {
                    set.remove(learnedId);
                    removed.add(learnedId);
                    changed = true;
                }
            }
        }
        return removed;
    }

    @Nullable
    private static ResourceLocation exclusivityConflict(SkillDef skill, Set<ResourceLocation> learned) {
        for (ResourceLocation other : skill.exclusiveWith()) {
            if (learned.contains(other)) return other;
        }
        for (ResourceLocation learnedId : learned) {
            SkillDef learnedSkill = SkillDefs.byId(learnedId);
            if (learnedSkill != null && learnedSkill.exclusiveWith().contains(skill.id())) return learnedId;
        }
        return null;
    }

    public record Result(boolean ok, @Nullable String error) {
        static Result success() {
            return new Result(true, null);
        }

        static Result fail(String error) {
            return new Result(false, error);
        }
    }

    public record ForgetResult(boolean ok, @Nullable String error, Set<ResourceLocation> removed) {
        static ForgetResult removed(Set<ResourceLocation> removed) {
            return new ForgetResult(true, null, removed);
        }

        static ForgetResult fail(String error) {
            return new ForgetResult(false, error, Set.of());
        }
    }
}
