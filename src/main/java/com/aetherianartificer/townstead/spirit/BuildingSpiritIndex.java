package com.aetherianartificer.townstead.spirit;

import com.aetherianartificer.townstead.Townstead;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Static map: building-type id → spirit-id → point contribution.
 *
 * Populated by {@code CatalogDataLoader} as it scans both the inline
 * {@code townsteadSpirit} field on Townstead-authored building_type JSONs
 * and the companion {@code data/townstead/spirit/<type>.json} files that
 * annotate vanilla MCA building types without clobbering them.
 *
 * Readers ({@link VillageSpiritAggregator}, and the client Spirit page)
 * treat the map as immutable. Writers go through {@link #put} / {@link #clear}
 * during reload.
 */
public final class BuildingSpiritIndex {
    private static final Map<String, Map<String, Integer>> CONTRIBUTIONS = new ConcurrentHashMap<>();

    private BuildingSpiritIndex() {}

    /** Returns an immutable map of spirit id → points for the given building type, or an empty map. */
    public static Map<String, Integer> contributionsFor(String buildingType) {
        if (buildingType == null) return Map.of();
        return CONTRIBUTIONS.computeIfAbsent(buildingType, BuildingSpiritIndex::scanForContributions);
    }

    /**
     * Store a contribution map for a building type. Later calls for the same
     * building type overwrite — the loader is responsible for merge semantics
     * (last writer wins, which matches how other `CatalogDataLoader` fields
     * behave).
     */
    public static void put(String buildingType, Map<String, Integer> contributions) {
        if (buildingType == null || contributions == null || contributions.isEmpty()) return;
        CONTRIBUTIONS.put(buildingType, Map.copyOf(contributions));
    }

    public static void clear() {
        CONTRIBUTIONS.clear();
    }

    /**
     * Copy of every non-empty contribution map, for the catalog sync packet.
     * Cached scan misses (empty maps from {@link #contributionsFor}) are skipped.
     */
    public static Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> out = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : CONTRIBUTIONS.entrySet()) {
            if (!e.getValue().isEmpty()) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    /** Client-side entry point for the catalog sync packet: replace the whole index. */
    public static void replaceAll(Map<String, Map<String, Integer>> contributions) {
        CONTRIBUTIONS.clear();
        for (Map.Entry<String, Map<String, Integer>> e : contributions.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public static int size() {
        return CONTRIBUTIONS.size();
    }

    private static final AtomicBoolean ASYNC_PREWARM_RUNNING = new AtomicBoolean(false);

    /**
     * Synchronous prewarm. Use only when the caller can tolerate a multi-ms
     * stall — most callers should use {@link #prewarmAsync} instead.
     */
    public static void prewarm(Iterable<String> buildingTypes) {
        if (buildingTypes == null) return;
        for (String buildingType : buildingTypes) {
            contributionsFor(buildingType);
        }
    }

    /**
     * Dispatch the spirit-companion JSON scan onto a worker thread. Returns
     * immediately. Subsequent {@link #contributionsFor} calls hit the cache
     * once the worker completes; callers that race ahead just take the
     * synchronous miss path. Concurrent calls coalesce.
     */
    public static void prewarmAsync(Iterable<String> buildingTypes) {
        if (buildingTypes == null) return;
        List<String> snapshot = new ArrayList<>();
        for (String t : buildingTypes) {
            if (t != null && !CONTRIBUTIONS.containsKey(t)) snapshot.add(t);
        }
        if (snapshot.isEmpty()) return;
        if (!ASYNC_PREWARM_RUNNING.compareAndSet(false, true)) return;
        ForkJoinPool.commonPool().execute(() -> {
            try {
                for (String buildingType : snapshot) {
                    contributionsFor(buildingType);
                }
            } catch (Throwable t) {
                Townstead.LOGGER.warn("BuildingSpiritIndex prewarm failed", t);
            } finally {
                ASYNC_PREWARM_RUNNING.set(false);
            }
        });
    }

    private static Map<String, Integer> scanForContributions(String buildingType) {
        Map<String, Integer> scanned = scanClasspathCompanion(buildingType);
        if (scanned.isEmpty()) {
            scanned = scanInlineBuildingType(buildingType);
        }
        return scanned.isEmpty() ? Map.of() : Map.copyOf(scanned);
    }

    private static Map<String, Integer> scanClasspathCompanion(String buildingType) {
        try {
            ClassLoader cl = BuildingSpiritIndex.class.getClassLoader();
            if (cl == null) return Map.of();
            String relPath = "data/townstead/spirit/" + buildingType + ".json";
            Enumeration<URL> urls = cl.getResources(relPath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream in = url.openStream();
                        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    if (obj == null || !obj.has("townsteadSpirit")) continue;
                    return parseSpiritMap(obj.getAsJsonObject("townsteadSpirit"));
                }
            }
        } catch (Exception ignored) {}
        return Map.of();
    }

    private static Map<String, Integer> scanInlineBuildingType(String buildingType) {
        String relPath = buildingType.startsWith("compat/")
                ? "townstead_compat/building_types/" + buildingType + ".json"
                : "data/mca/building_types/" + buildingType + ".json";
        return scanInlineSpiritJson(relPath);
    }

    private static Map<String, Integer> scanInlineSpiritJson(String relPath) {
        try {
            ClassLoader cl = BuildingSpiritIndex.class.getClassLoader();
            if (cl == null) return Map.of();
            Enumeration<URL> urls = cl.getResources(relPath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream in = url.openStream();
                        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    if (obj == null || !obj.has("townsteadSpirit")) continue;
                    return parseSpiritMap(obj.getAsJsonObject("townsteadSpirit"));
                }
            }
        } catch (Exception ignored) {}
        return Map.of();
    }

    private static Map<String, Integer> parseSpiritMap(JsonObject spirit) {
        if (spirit == null || spirit.size() == 0) return Map.of();
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : spirit.entrySet()) {
            if (!SpiritRegistry.contains(e.getKey())) continue;
            try {
                int pts = e.getValue().getAsInt();
                if (pts > 0) out.put(e.getKey(), pts);
            } catch (Exception ignored) {}
        }
        return out;
    }
}
