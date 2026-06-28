package com.aetherianartificer.townstead.root;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A named, selectable assignment profile in the Roots system. It is not a
 * biological tier after lineage: it points to a species and either an ancestry
 * or lineage, then supplies founder defaults and optional presentation/genome
 * overrides. The individual's realised inherited identity is {@link Heritage}.
 * Its effective founder genome is resolved by
 * {@link RootRegistry#effectiveGenome}.
 *
 * <p>Loaded from {@code data/<ns>/origin/<path>.json}. The built-in
 * {@code townstead_roots:overworlder} is Humanoid / Human with default ranges.</p>
 */
public record Root(
        ResourceLocation id,
        Component displayName,
        @Nullable ResourceLocation species,
        @Nullable ResourceLocation ancestry,
        @Nullable ResourceLocation lineage,
        @Nullable Demonym demonym,
        @Nullable Component backstory,
        Genome genome,
        SpawnBias spawnBias
) {
    public Root {
        genome = genome == null ? Genome.EMPTY : genome;
        spawnBias = spawnBias == null ? SpawnBias.EMPTY : spawnBias;
    }
}
