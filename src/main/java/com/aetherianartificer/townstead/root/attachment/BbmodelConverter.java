package com.aetherianartificer.townstead.root.attachment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.GsonHelper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Converts a Blockbench project file ({@code .bbmodel}) into the Bedrock-dialect
 * geometry and animation JSON the attachment pipeline already speaks, so packs can
 * ship Blockbench projects directly ({@code attachment/bbmodel/<name>.bbmodel}) and
 * the converted bytes ride the existing content-addressed blob sync.
 *
 * <p>Transform: X mirrored (cube min-corner from the mirrored max); rotations build
 * the full matrix {@code Rx(-x)·Ry(-y)·Rz(-z)} from the Blockbench angles and
 * decompose it into the loader's ZYX convention — a plain per-axis sign map cannot
 * express the composition-order change, and this matrix path was verified by
 * rendering converted models through the exact loader math against Blockbench
 * previews. East/west faces swap with per-face UVs flipped horizontally; {@code
 * box_uv} projects emit native box UV. Coordinates keep the project's own origin
 * (feet, for an avatar) — the definition's {@code offset} places the model.</p>
 *
 * <p>A geometry reference may name an embedded animation as a static pose:
 * {@code "ns:file#poseName"} bakes that animation's first rotation keyframe per bone
 * additively over the rest pose (a Figura-style state pose baked at load). Embedded
 * animations also convert wholesale, keyed {@code animation.<file>.<name>}, so
 * {@code "file#clip"} animation references work against a bbmodel file too.</p>
 *
 * <p>Blockbench 5.0 flipped the X/Y sign convention of animation rotation keyframes
 * to match group rotations (its project codec inverts them when loading older files).
 * Pre-5.0 values are therefore already in the Bedrock convention our dialect uses and
 * pass verbatim; 5.0+ values take the same {@code (-x, -y, z)} transform as groups.</p>
 */
public final class BbmodelConverter {

    private BbmodelConverter() {}

    /** Converted geometry JSON bytes, or null when the file doesn't parse. */
    public static byte[] geometry(byte[] bbmodel, String fileName, String pose) {
        try {
            JsonObject project = JsonParser.parseString(new String(bbmodel, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, JsonObject> elements = elementsById(project);
            Map<String, float[]> poseDeltas = pose == null || pose.isEmpty()
                    ? Map.of() : poseDeltas(project, pose);

            JsonObject resolution = GsonHelper.getAsJsonObject(project, "resolution", new JsonObject());
            JsonObject description = new JsonObject();
            description.addProperty("identifier", "geometry." + fileName + (pose == null || pose.isEmpty() ? "" : "_" + pose));
            description.addProperty("texture_width", GsonHelper.getAsInt(resolution, "width", 16));
            description.addProperty("texture_height", GsonHelper.getAsInt(resolution, "height", 16));
            description.addProperty("visible_bounds_width", 4);
            description.addProperty("visible_bounds_height", 4);

            boolean boxUv = GsonHelper.getAsBoolean(
                    GsonHelper.getAsJsonObject(project, "meta", new JsonObject()), "box_uv", false);
            JsonArray bones = new JsonArray();
            for (JsonElement root : GsonHelper.getAsJsonArray(project, "outliner", new JsonArray())) {
                if (root.isJsonObject()) {
                    convertBone(root.getAsJsonObject(), null, elements, poseDeltas, boxUv, bones);
                }
            }

            JsonObject geometry = new JsonObject();
            geometry.add("description", description);
            geometry.add("bones", bones);
            JsonArray geometries = new JsonArray();
            geometries.add(geometry);
            JsonObject out = new JsonObject();
            out.addProperty("format_version", "1.12.0");
            out.add("minecraft:geometry", geometries);
            return out.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            com.aetherianartificer.townstead.Townstead.LOGGER.error("Failed to convert bbmodel {}", fileName, e);
            return null;
        }
    }

    /** Every embedded animation as a clip file ({@code animation.<file>.<name>}), or null. */
    public static byte[] animations(byte[] bbmodel, String fileName) {
        try {
            JsonObject project = JsonParser.parseString(new String(bbmodel, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject clips = new JsonObject();
            for (JsonElement element : GsonHelper.getAsJsonArray(project, "animations", new JsonArray())) {
                JsonObject anim = element.getAsJsonObject();
                String name = GsonHelper.getAsString(anim, "name", "");
                if (name.isEmpty()) continue;
                JsonObject bones = new JsonObject();
                boolean legacy = legacyAnimations(project);
                for (var animator : GsonHelper.getAsJsonObject(anim, "animators", new JsonObject()).entrySet()) {
                    if (!animator.getValue().isJsonObject()) continue;
                    JsonObject channel = animator.getValue().getAsJsonObject();
                    String boneName = GsonHelper.getAsString(channel, "name", "");
                    JsonObject rotation = new JsonObject();
                    for (JsonElement kfElement : GsonHelper.getAsJsonArray(channel, "keyframes", new JsonArray())) {
                        JsonObject kf = kfElement.getAsJsonObject();
                        if (!"rotation".equals(GsonHelper.getAsString(kf, "channel", ""))) continue;
                        float[] v = dataPoint(kf);
                        if (v == null) continue;
                        // Legacy files store animation values with X/Y inverted vs display.
                        float[] display = legacy ? new float[]{-v[0], -v[1], v[2]} : v;
                        float[] geo = toGeoRotation(display);
                        JsonArray value = new JsonArray();
                        value.add(geo[0]);
                        value.add(geo[1]);
                        value.add(geo[2]);
                        rotation.add(trimFloat(GsonHelper.getAsFloat(kf, "time", 0f)), value);
                    }
                    if (!rotation.entrySet().isEmpty() && !boneName.isEmpty()) {
                        JsonObject track = new JsonObject();
                        track.add("rotation", rotation);
                        bones.add(boneName, track);
                    }
                }
                if (bones.entrySet().isEmpty()) continue;
                JsonObject clip = new JsonObject();
                clip.addProperty("animation_length", GsonHelper.getAsFloat(anim, "length", 1f));
                String loop = GsonHelper.getAsString(anim, "loop", "once");
                if (loop.equals("loop")) clip.addProperty("loop", true);
                else if (loop.equals("hold")) clip.addProperty("loop", "hold_on_last_frame");
                clip.add("bones", bones);
                clips.add("animation." + fileName + "." + name, clip);
            }
            if (clips.entrySet().isEmpty()) return null;
            JsonObject out = new JsonObject();
            out.addProperty("format_version", "1.8.0");
            out.add("animations", clips);
            return out.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            com.aetherianartificer.townstead.Townstead.LOGGER.error("Failed to convert bbmodel animations {}", fileName, e);
            return null;
        }
    }

    // --- geometry internals ---

    private static void convertBone(JsonObject node, String parent, Map<String, JsonObject> elements,
                                    Map<String, float[]> poseDeltas, boolean boxUv, JsonArray out) {
        String name = GsonHelper.getAsString(node, "name", "");
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        if (parent != null) bone.addProperty("parent", parent);
        float[] origin = vec(node, "origin");
        bone.add("pivot", array(-origin[0], origin[1], origin[2]));
        float[] rot = vec(node, "rotation");
        float[] delta = poseDeltas.get(name);
        if (delta != null) {
            rot = new float[]{rot[0] + delta[0], rot[1] + delta[1], rot[2] + delta[2]};
        }
        if (rot[0] != 0f || rot[1] != 0f || rot[2] != 0f) {
            float[] geo = toGeoRotation(rot);
            bone.add("rotation", array(geo[0], geo[1], geo[2]));
        }
        JsonArray cubes = new JsonArray();
        for (JsonElement child : GsonHelper.getAsJsonArray(node, "children", new JsonArray())) {
            if (child.isJsonPrimitive()) {
                JsonObject element = elements.get(child.getAsString());
                if (element != null && "cube".equals(GsonHelper.getAsString(element, "type", "cube"))) {
                    cubes.add(convertCube(element, boxUv));
                }
            } else if (child.isJsonObject()) {
                convertBone(child.getAsJsonObject(), name, elements, poseDeltas, boxUv, out);
            }
        }
        if (!cubes.isEmpty()) bone.add("cubes", cubes);
        out.add(bone);
    }

    private static JsonObject convertCube(JsonObject element, boolean boxUv) {
        float[] from = vec(element, "from");
        float[] to = vec(element, "to");
        JsonObject cube = new JsonObject();
        // X mirror: the Java min corner comes from the mirrored max.
        cube.add("origin", array(-Math.max(from[0], to[0]),
                Math.min(from[1], to[1]), Math.min(from[2], to[2])));
        cube.add("size", array(Math.abs(to[0] - from[0]),
                Math.abs(to[1] - from[1]), Math.abs(to[2] - from[2])));
        float inflate = GsonHelper.getAsFloat(element, "inflate", 0f);
        if (inflate != 0f) cube.addProperty("inflate", inflate);
        float[] rot = vec(element, "rotation");
        if (rot[0] != 0f || rot[1] != 0f || rot[2] != 0f) {
            float[] geo = toGeoRotation(rot);
            cube.add("rotation", array(geo[0], geo[1], geo[2]));
            float[] pivot = vec(element, "origin");
            cube.add("pivot", array(-pivot[0], pivot[1], pivot[2]));
        }
        // Box UV is decided PER ELEMENT (the project meta.box_uv is only the default —
        // a project can say box_uv:true while every element overrides to hand-mapped
        // per-face UVs; emitting box layout for those samples unpainted texture and
        // renders invisible). Box-UV elements emit native box UV (+ mirror flag).
        if (GsonHelper.getAsBoolean(element, "box_uv", boxUv)) {
            JsonArray offset = GsonHelper.getAsJsonArray(element, "uv_offset", null);
            JsonArray uvArray = new JsonArray();
            uvArray.add(offset != null && offset.size() > 0 ? offset.get(0).getAsFloat() : 0f);
            uvArray.add(offset != null && offset.size() > 1 ? offset.get(1).getAsFloat() : 0f);
            cube.add("uv", uvArray);
            if (GsonHelper.getAsBoolean(element, "mirror_uv", false)) {
                cube.addProperty("mirror", true);
            }
            return cube;
        }
        JsonObject faces = GsonHelper.getAsJsonObject(element, "faces", new JsonObject());
        JsonObject uv = new JsonObject();
        for (var entry : faces.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject face = entry.getValue().getAsJsonObject();
            if (face.get("texture") == null || face.get("texture").isJsonNull()) continue;
            JsonArray rect = GsonHelper.getAsJsonArray(face, "uv", null);
            if (rect == null || rect.size() < 4) continue;
            float u1 = rect.get(0).getAsFloat(), v1 = rect.get(1).getAsFloat();
            float u2 = rect.get(2).getAsFloat(), v2 = rect.get(3).getAsFloat();
            // X mirror swaps east/west and flips every face horizontally.
            String slot = switch (entry.getKey().toLowerCase(Locale.ROOT)) {
                case "east" -> "west";
                case "west" -> "east";
                default -> entry.getKey().toLowerCase(Locale.ROOT);
            };
            JsonObject faceUv = new JsonObject();
            faceUv.add("uv", array2(u2, v1));
            faceUv.add("uv_size", array2(u1 - u2, v2 - v1));
            uv.add(slot, faceUv);
        }
        if (!uv.entrySet().isEmpty()) cube.add("uv", uv);
        return cube;
    }

    /** Whether animation keyframes use the pre-5.0 (Bedrock-matching) sign convention. */
    private static boolean legacyAnimations(JsonObject project) {
        JsonObject meta = GsonHelper.getAsJsonObject(project, "meta", new JsonObject());
        String version = GsonHelper.getAsString(meta, "format_version",
                GsonHelper.getAsString(meta, "format", "4.0"));
        try {
            int dot = version.indexOf('.');
            return Integer.parseInt(dot < 0 ? version : version.substring(0, dot)) < 5;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * First rotation keyframe per bone of the named embedded animation (a static pose),
     * in GROUP convention (added to the rest rotation before the group transform).
     */
    private static Map<String, float[]> poseDeltas(JsonObject project, String pose) {
        boolean legacy = legacyAnimations(project);
        Map<String, float[]> out = new LinkedHashMap<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(project, "animations", new JsonArray())) {
            JsonObject anim = element.getAsJsonObject();
            if (!pose.equals(GsonHelper.getAsString(anim, "name", ""))) continue;
            for (var animator : GsonHelper.getAsJsonObject(anim, "animators", new JsonObject()).entrySet()) {
                if (!animator.getValue().isJsonObject()) continue;
                JsonObject channel = animator.getValue().getAsJsonObject();
                String boneName = GsonHelper.getAsString(channel, "name", "");
                JsonObject best = null;
                float bestTime = Float.MAX_VALUE;
                for (JsonElement kfElement : GsonHelper.getAsJsonArray(channel, "keyframes", new JsonArray())) {
                    JsonObject kf = kfElement.getAsJsonObject();
                    if (!"rotation".equals(GsonHelper.getAsString(kf, "channel", ""))) continue;
                    float time = GsonHelper.getAsFloat(kf, "time", 0f);
                    if (time < bestTime) {
                        bestTime = time;
                        best = kf;
                    }
                }
                float[] v = best == null ? null : dataPoint(best);
                if (v != null && !boneName.isEmpty()) {
                    out.put(boneName, legacy ? new float[]{-v[0], -v[1], v[2]} : v);
                }
            }
        }
        return out;
    }

    private static float[] dataPoint(JsonObject keyframe) {
        JsonArray points = GsonHelper.getAsJsonArray(keyframe, "data_points", null);
        if (points == null || points.isEmpty() || !points.get(0).isJsonObject()) return null;
        JsonObject dp = points.get(0).getAsJsonObject();
        try {
            return new float[]{axis(dp, "x"), axis(dp, "y"), axis(dp, "z")};
        } catch (NumberFormatException e) {
            return null;   // Molang expression; poses need numeric keyframes
        }
    }

    /** bbmodel data points store numbers as strings (and may hold Molang). */
    private static float axis(JsonObject dp, String key) {
        JsonElement value = dp.get(key);
        if (value == null || value.isJsonNull()) return 0f;
        return Float.parseFloat(value.getAsString().trim());
    }

    /**
     * Blockbench display rotation (degrees) → geo-dialect rotation: build the matrix
     * {@code Rx(-x)·Ry(-y)·Rz(-z)} and decompose it into the ZYX angles our loader
     * reconstructs ({@code render = Rz(-gz)·Ry(-gy)·Rx(gx)}).
     */
    private static float[] toGeoRotation(float[] bb) {
        double x = Math.toRadians(-bb[0]);
        double y = Math.toRadians(-bb[1]);
        double z = Math.toRadians(-bb[2]);
        double cx = Math.cos(x), sx = Math.sin(x);
        double cy = Math.cos(y), sy = Math.sin(y);
        double cz = Math.cos(z), sz = Math.sin(z);
        // M = Rx * Ry * Rz
        double m00 = cy * cz;
        double m10 = sx * sy * cz + cx * sz;
        double m20 = -cx * sy * cz + sx * sz;
        double m21 = cx * sy * sz + sx * cz;
        double m22 = cx * cy;
        // Decompose M = Rz(a)·Ry(b)·Rx(c):  b = asin(-m20), c = atan2(m21, m22), a = atan2(m10, m00)
        double b = Math.asin(Math.max(-1.0, Math.min(1.0, -m20)));
        double c = Math.atan2(m21, m22);
        double a = Math.atan2(m10, m00);
        // loader: java = (gx, -gy, -gz) → geo = (c, -b, -a)
        return new float[]{(float) Math.toDegrees(c), (float) -Math.toDegrees(b), (float) -Math.toDegrees(a)};
    }

    private static float[] vec(JsonObject json, String key) {
        float[] out = new float[3];
        JsonArray array = GsonHelper.getAsJsonArray(json, key, null);
        if (array != null) {
            for (int i = 0; i < 3 && i < array.size(); i++) out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static Map<String, JsonObject> elementsById(JsonObject project) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(project, "elements", new JsonArray())) {
            JsonObject o = element.getAsJsonObject();
            out.put(GsonHelper.getAsString(o, "uuid", ""), o);
        }
        return out;
    }

    private static String trimFloat(float f) {
        return f == (long) f ? String.valueOf((long) f) : String.valueOf(f);
    }

    private static JsonArray array(float x, float y, float z) {
        JsonArray a = new JsonArray();
        a.add(x);
        a.add(y);
        a.add(z);
        return a;
    }

    private static JsonArray array2(float x, float y) {
        JsonArray a = new JsonArray();
        a.add(x);
        a.add(y);
        return a;
    }
}
