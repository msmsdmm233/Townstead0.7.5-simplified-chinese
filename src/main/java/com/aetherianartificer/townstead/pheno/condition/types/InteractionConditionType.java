package com.aetherianartificer.townstead.pheno.condition.types;

import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionType;
import com.aetherianartificer.townstead.reaction.trigger.event.DialogueStateTracker;
import com.aetherianartificer.townstead.reaction.trigger.event.SocialInteractionTracker;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Social and UI-driven interaction state. */
public final class InteractionConditionType implements ConditionType {

    public static final String KEY = "pheno:interaction";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    @Nullable
    public Condition parse(JsonObject json) {
        String interaction = GsonHelper.getAsString(json, "interaction", "").toLowerCase(Locale.ROOT);
        return switch (interaction) {
            case "in_dialogue", "in_dialogue_with_player" ->
                    ctx -> DialogueStateTracker.activePartner(ctx.entity()) != null;
            case "dialogue_just_ended" ->
                    ctx -> DialogueStateTracker.dialogueJustEnded(ctx.entity(), ctx.level().getGameTime());
            case "heart_increased", "hearts_increased", "relationship_improved" ->
                    ctx -> SocialInteractionTracker.heartIncreasedRecently(ctx.entity(), ctx.level().getGameTime());
            case "heart_decreased", "hearts_decreased", "relationship_worsened" ->
                    ctx -> SocialInteractionTracker.heartDecreasedRecently(ctx.entity(), ctx.level().getGameTime());
            default -> null;
        };
    }
}
