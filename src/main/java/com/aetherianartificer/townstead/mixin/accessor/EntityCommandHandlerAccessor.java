package com.aetherianartificer.townstead.mixin.accessor;

import net.conczin.mca.entity.interaction.EntityCommandHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads MCA's {@code EntityCommandHandler#entity} (the handled villager). The field is
 * {@code protected final T entity}, a direct member of this generic class (erasing to
 * {@link Entity}), so an {@code @Accessor} here resolves cleanly — unlike a subclass
 * {@code @Shadow}, which Mixin can't bind to an inherited field.
 *
 * <p>neoforge only: the older 1.20.1-forge Mixin annotation processor can't match an
 * {@code Entity}-typed accessor against the type-variable field, so the accessor is omitted
 * there (this interface is then an inert no-op mixin, and {@link com.aetherianartificer.townstead.mixin.ProcreateDialogueAckMixin}
 * is likewise inactive on forge).</p>
 */
@Mixin(EntityCommandHandler.class)
public interface EntityCommandHandlerAccessor {
    //? if neoforge {
    @Accessor(value = "entity", remap = false)
    Entity townstead$entity();
    //?}
}
