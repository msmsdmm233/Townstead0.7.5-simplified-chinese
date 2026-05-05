package com.aetherianartificer.townstead.client.catalog;

import com.aetherianartificer.townstead.Townstead;
import net.conczin.mca.resources.BuildingTypes;
import net.conczin.mca.resources.data.BuildingType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves a human-readable name for a building requirement (block id, item id,
 * or block/item tag). Translatable resolution is amortized via a shared static
 * cache so the catalog detail panel doesn't pay {@code Component.translatable}
 * overhead per requirement on every selection change.
 *
 * Safe to call from any thread: registries and the {@code Language} manager
 * are read-only once the game is initialized.
 */
public final class RequirementNameResolver {
    private static final ConcurrentHashMap<ResourceLocation, String> CACHE = new ConcurrentHashMap<>();
    private static final AtomicBoolean ASYNC_WARM_RUNNING = new AtomicBoolean(false);

    private RequirementNameResolver() {}

    public static String displayName(ResourceLocation id) {
        String cached = CACHE.get(id);
        if (cached != null) return cached;
        String result = resolve(id);
        CACHE.put(id, result);
        return result;
    }

    public static int cacheSize() {
        return CACHE.size();
    }

    public static void clear() {
        CACHE.clear();
    }

    /**
     * Pre-resolve every requirement of every building type on a worker
     * thread. Result lands in the shared cache; first catalog-detail-panel
     * selection then sees pure cache hits instead of paying the translatable
     * resolution chain (which can be ~5 ms for tag-fallback chains).
     */
    public static void prewarmAsync() {
        if (!ASYNC_WARM_RUNNING.compareAndSet(false, true)) return;
        ForkJoinPool.commonPool().execute(() -> {
            try {
                long t0 = System.nanoTime();
                int seen = 0;
                int hitsBefore = CACHE.size();
                for (BuildingType bt : BuildingTypes.getInstance()) {
                    Set<ResourceLocation> reqs = bt.getGroups().keySet();
                    for (ResourceLocation id : reqs) {
                        if (!CACHE.containsKey(id)) {
                            CACHE.put(id, resolve(id));
                        }
                        seen++;
                    }
                }
                long elapsed = System.nanoTime() - t0;
                Townstead.LOGGER.info("[TS-Diag/ReqName] prewarmAsync done seen={} added={} cacheSize={} elapsedUs={} thread={}",
                        seen, CACHE.size() - hitsBefore, CACHE.size(),
                        elapsed / 1_000L, Thread.currentThread().getName());
            } catch (Throwable t) {
                Townstead.LOGGER.warn("[TS-Diag/ReqName] prewarmAsync failed", t);
            } finally {
                ASYNC_WARM_RUNNING.set(false);
            }
        });
    }

    private static String resolve(ResourceLocation id) {
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            return Component.translatable(block.getDescriptionId()).getString();
        }
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            Item item = BuiltInRegistries.ITEM.get(id);
            return Component.translatable(item.getDescriptionId()).getString();
        }
        String tagPath = id.toString().replace(':', '.').replace('/', '.');
        String slashKey = "tag.block." + tagPath;
        String dottedKey = "tag.item." + tagPath;
        String slash = Component.translatable(slashKey).getString();
        if (!slash.equals(slashKey)) return slash;
        String dotted = Component.translatable(dottedKey).getString();
        if (!dotted.equals(dottedKey)) return dotted;
        String fallback = id.getPath().replace('_', ' ');
        if (fallback.endsWith("s") && fallback.length() > 3) {
            fallback = fallback.substring(0, fallback.length() - 1);
        }
        String[] words = fallback.split(" ");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(w.substring(0, 1).toUpperCase(Locale.ROOT)).append(w.substring(1));
        }
        return out.toString();
    }
}
