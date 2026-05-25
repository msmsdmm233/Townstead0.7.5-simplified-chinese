package com.aetherianartificer.townstead.origin.gene;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A loaded, self-contained gene: shared metadata, genetics flags, and the parsed
 * type {@link GeneInstance}. Races grant genes by id; the picker groups them by
 * {@link #category} and renders each via {@link #display}.
 *
 * <p>{@link #locus} (when set) is the slot this gene is an allele of — genes
 * sharing a locus are mutually exclusive, resolved by {@link #dominance} then
 * {@link #weight} (a deferred runtime concern). Loaded from
 * {@code data/<ns>/gene/<path>.json}.</p>
 */
public record Gene(
        ResourceLocation id,
        Component displayName,
        @Nullable Component description,
        String category,
        Dominance dominance,
        @Nullable ResourceLocation locus,
        int weight,
        GeneInstance instance
) {
    public GeneDisplay display() {
        return instance.display();
    }
}
