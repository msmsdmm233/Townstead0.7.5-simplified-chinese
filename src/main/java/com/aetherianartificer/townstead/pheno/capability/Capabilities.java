package com.aetherianartificer.townstead.pheno.capability;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry and front door for the capability layer. Mirrors {@code Powers}: any number of
 * {@link CapabilitySource}s register here, and callers resolve an entity's effective
 * capabilities ({@link #resolve}) or read the raw contributions ({@link #collect}, used by
 * {@code /pheno explain}). Source-agnostic: Townstead systems query the {@link CapabilityView},
 * never a particular source.
 */
public final class Capabilities {

    private static final List<CapabilitySource> SOURCES = new CopyOnWriteArrayList<>();

    private Capabilities() {}

    public static void register(CapabilitySource source) {
        SOURCES.add(source);
    }

    public static List<CapabilityContribution> collect(LivingEntity entity) {
        CapabilityCollector collector = new CapabilityCollector();
        for (CapabilitySource source : SOURCES) {
            source.contribute(entity, collector);
        }
        return new ArrayList<>(collector.contributions());
    }

    public static CapabilityView resolve(LivingEntity entity) {
        return CapabilityResolver.resolve(collect(entity));
    }
}
