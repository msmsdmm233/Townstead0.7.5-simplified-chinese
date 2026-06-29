package com.aetherianartificer.townstead.mixin;

//? if neoforge {
import com.aetherianartificer.townstead.mixin.accessor.EntityCommandHandlerAccessor;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}
import net.conczin.mca.entity.interaction.VillagerCommandHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Unsticks the "try for a baby" confirmation dialogue. MCA's procreate answer is a bare
 * {@code command:procreate} with no {@code next}, so on success it sends the client no phrase. Townstead's
 * {@code RpgDialogueScreen} sits in AWAITING_RESPONSE for its full timeout (every other answer advances by
 * sending a phrase, so only command-only answers hang). We send a confirming {@code VillagerMessage} on the
 * success branch, exactly as the lowhearts/toosoon failures already do, so the screen advances and closes.
 *
 * <p>{@code remap=false}: the target lives in MCA (stable names across both stonecutter branches). The
 * villager is reached through {@link EntityCommandHandlerAccessor} (the {@code entity} field is inherited
 * from the generic {@code EntityCommandHandler} and can't be {@code @Shadow}ed from this subclass).
 * neoforge only — see the accessor for why; on forge this mixin is inert.</p>
 */
@Mixin(value = VillagerCommandHandler.class, remap = false)
public abstract class ProcreateDialogueAckMixin {

    //? if neoforge {
    @Inject(method = "handle", at = @At("RETURN"))
    private void townstead$ackProcreate(ServerPlayer player, String command, CallbackInfoReturnable<Boolean> cir) {
        if (!"procreate".equals(command)) return;
        Entity entity = ((EntityCommandHandlerAccessor) this).townstead$entity();
        if (entity instanceof VillagerEntityMCA villager && villager.getRelationships().isProcreating()) {
            villager.sendChatMessage(Component.translatable("message.townstead.procreate_confirmed"), player);
        }
    }
    //?}
}
