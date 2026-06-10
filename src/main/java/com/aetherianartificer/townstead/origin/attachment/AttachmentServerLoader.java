package com.aetherianartificer.townstead.origin.attachment;

import com.aetherianartificer.townstead.Townstead;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server data loader for attachments. Reads {@code data/<ns>/attachments/<id>.json}
 * definitions plus their referenced geometry ({@code attachments/geo/<name>.geo.json})
 * and texture ({@code attachments/textures/<name>.png}) bytes, hashes the blobs
 * (SHA-1), and fills {@link AttachmentServerData}. Slots come from
 * {@code data/<ns>/attachment_slots/<id>.json}. The bytes are then synced + cached
 * client-side, so a pack needs no resource pack.
 */
public final class AttachmentServerLoader implements ResourceManagerReloadListener {

    private static final int MAX_TEXTURE_BYTES = 8 * 1024 * 1024;
    private static final String DIR = "attachments";
    private static final String SLOT_DIR = "attachment_slots";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<AttachmentDef> defs = new ArrayList<>();
        List<AttachmentSlotDef> slots = new ArrayList<>();
        Map<String, AttachmentServerData.Blob> blobs = new LinkedHashMap<>();

        manager.listResources(DIR, rl -> isTopLevelJson(rl.getPath())).forEach((file, resource) -> {
            String id = idFrom(file.getNamespace(), file.getPath(), DIR);
            JsonObject json = readJson(resource);
            if (id == null || json == null) return;
            try {
                AttachmentDef def = parseDef(manager, file.getNamespace(), id, json, blobs);
                if (def != null) defs.add(def);
            } catch (Exception e) {
                Townstead.LOGGER.error("Failed to load attachment {}", id, e);
            }
        });

        manager.listResources(SLOT_DIR, rl -> rl.getPath().endsWith(".json")).forEach((file, resource) -> {
            String id = idFrom(file.getNamespace(), file.getPath(), SLOT_DIR);
            JsonObject json = readJson(resource);
            if (id == null || json == null) return;
            slots.add(new AttachmentSlotDef(id, GsonHelper.getAsString(json, "bone", "body"), readVec(json, "offset")));
        });

        AttachmentServerData.set(defs, slots, blobs);
        Townstead.LOGGER.info("Loaded {} attachment definitions, {} slots, {} blobs",
                defs.size(), slots.size(), blobs.size());
    }

    private static AttachmentDef parseDef(ResourceManager manager, String ns, String id, JsonObject json,
                                          Map<String, AttachmentServerData.Blob> blobs) throws Exception {
        ResourceLocation geoFile = resolve(ns, GsonHelper.getAsString(json, "geometry", ""), "geo", ".geo.json");
        ResourceLocation texFile = resolve(ns, GsonHelper.getAsString(json, "texture", ""), "textures", ".png");
        if (geoFile == null || texFile == null) {
            Townstead.LOGGER.warn("Attachment {} missing geometry/texture reference", id);
            return null;
        }
        byte[] geo = readBytes(manager, geoFile, Integer.MAX_VALUE);
        byte[] tex = readBytes(manager, texFile, MAX_TEXTURE_BYTES);
        if (geo == null || tex == null) return null;

        String geoSha = sha1(geo);
        String texSha = sha1(tex);
        blobs.put(geoSha, new AttachmentServerData.Blob(geo, AttachmentServerData.KIND_GEO));
        blobs.put(texSha, new AttachmentServerData.Blob(tex, AttachmentServerData.KIND_TEXTURE));

        String slot = json.has("slot") ? GsonHelper.getAsString(json, "slot", "") : null;
        String bone = GsonHelper.getAsString(json, "bone", "body");
        float scale = GsonHelper.getAsFloat(json, "scale", 1f);
        int tint = parseHex(GsonHelper.getAsString(json, "tint", "#FFFFFF"));
        return new AttachmentDef(id, geoSha, texSha, slot == null || slot.isEmpty() ? null : slot, bone,
                readVec(json, "offset"), readVec(json, "rotation"), scale, tint);
    }

    private static ResourceLocation resolve(String defNs, String ref, String subdir, String suffix) {
        if (ref == null || ref.isEmpty()) return null;
        int colon = ref.indexOf(':');
        String ns = colon < 0 ? defNs : ref.substring(0, colon);
        String name = colon < 0 ? ref : ref.substring(colon + 1);
        return ResourceLocation.tryParse(ns + ":" + DIR + "/" + subdir + "/" + name + suffix);
    }

    private static byte[] readBytes(ResourceManager manager, ResourceLocation file, int cap) {
        Optional<Resource> resource = manager.getResource(file);
        if (resource.isEmpty()) {
            Townstead.LOGGER.warn("Attachment blob not found: {}", file);
            return null;
        }
        try (InputStream in = resource.get().open()) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length > cap) {
                Townstead.LOGGER.warn("Attachment blob {} is {} bytes, over the {} cap; skipped",
                        file, bytes.length, cap);
                return null;
            }
            return bytes;
        } catch (Exception e) {
            Townstead.LOGGER.error("Failed to read attachment blob {}", file, e);
            return null;
        }
    }

    private static boolean isTopLevelJson(String path) {
        return path.startsWith(DIR + "/")
                && path.endsWith(".json")
                && path.indexOf('/', DIR.length() + 1) < 0;
    }

    private static String idFrom(String ns, String path, String dir) {
        String name = path.substring(dir.length() + 1, path.length() - ".json".length());
        return ns + ":" + name;
    }

    private static JsonObject readJson(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static float[] readVec(JsonObject json, String key) {
        float[] out = new float[3];
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < 3 && i < array.size(); i++) out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static int parseHex(String raw) {
        String s = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    private static String sha1(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
    }
}
