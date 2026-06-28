package com.aetherianartificer.townstead.root;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Back-compat shim for the pre-rebrand {@code townstead_origins} namespace, now
 * {@link RootRegistry#NAMESPACE townstead_roots}. Built-in data moved to the new
 * namespace, so old saves ({@code Life.rootId}, MCA-stored gene ids) and old data
 * packs still reference {@code townstead_origins:*}. Registry lookups try the id
 * as-given first (so a legacy pack that still ships under the old namespace keeps
 * resolving its own entries) and only fall back to the remapped id.
 */
public final class LegacyNamespace {

    public static final String LEGACY = "townstead_origins";

    private LegacyNamespace() {}

    /** The {@code townstead_roots} equivalent of a {@code townstead_origins} id, else {@code null}. */
    @Nullable
    public static ResourceLocation remap(@Nullable ResourceLocation id) {
        if (id == null || !LEGACY.equals(id.getNamespace())) return null;
        return com.aetherianartificer.townstead.data.DataPackLang.parseId(
                RootRegistry.NAMESPACE + ":" + id.getPath());
    }

    /** The {@code townstead_roots:} form of a {@code townstead_origins:} key string, else {@code null}. */
    @Nullable
    public static String remapKey(@Nullable String key) {
        if (key == null) return null;
        String prefix = LEGACY + ":";
        if (!key.startsWith(prefix)) return null;
        return RootRegistry.NAMESPACE + ":" + key.substring(prefix.length());
    }
}
