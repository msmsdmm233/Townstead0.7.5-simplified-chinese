package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.Townstead;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.resources.Dialogues;
import net.conczin.mca.resources.data.dialogue.Question;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCA's {@code selectAnswer} dereferences the question and answer looked up from the ids a
 * client packet supplied, NPE-ing on any id the server no longer knows (datapack reload
 * mid-conversation, dialogue packs merging/overriding question files). Bail with a log line
 * naming the ids instead, and release the interaction so the dialogue screen doesn't hang.
 */
@Mixin(value = Dialogues.class, remap = false)
public abstract class DialogueSelectAnswerGuardMixin {

    @Unique
    private static final Logger TOWNSTEAD$LOG = LoggerFactory.getLogger(Townstead.MOD_ID + "/DialogueGuard");

    @Inject(method = "selectAnswer", at = @At("HEAD"), cancellable = true)
    private void townstead$guardUnknownIds(VillagerEntityMCA villager, ServerPlayer player,
            String questionId, String answerId, CallbackInfo ci) {
        Dialogues self = (Dialogues) (Object) this;
        Question question = self.getQuestion(questionId);
        if (question != null && question.getAnswer(answerId) != null) return;
        TOWNSTEAD$LOG.warn("Ignored dialogue selection from {}: {} (question='{}', answer='{}')",
                player.getGameProfile().getName(),
                question == null ? "unknown question" : "unknown answer",
                questionId, answerId);
        villager.getInteractions().stopInteracting();
        ci.cancel();
    }
}
