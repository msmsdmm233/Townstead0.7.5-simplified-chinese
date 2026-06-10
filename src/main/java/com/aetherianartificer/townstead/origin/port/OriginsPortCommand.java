package com.aetherianartificer.townstead.origin.port;

import com.aetherianartificer.townstead.Townstead;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /townstead origins port <namespace> [outDir]} (op level 2): reads a loaded
 * Origins data pack and writes a Townstead-shaped scaffold (species / ancestry /
 * origin / gene JSON plus a lang sidecar) for the author to refine and ship.
 * Convertible passive powers become genes; everything else lands in a
 * {@code port_report.txt} and as {@code _todo_professions} stubs in the origin
 * files. Nothing is loaded automatically.
 */
public final class OriginsPortCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    //? if >=1.21 {
    private static final int PACK_FORMAT = 48;
    //?} else {
    /*private static final int PACK_FORMAT = 15;
    *///?}

    private OriginsPortCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx) {
        dispatcher.register(Commands.literal("townstead").then(Commands.literal("origins").then(
                Commands.literal("port")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("namespace", StringArgumentType.string())
                                .executes(c -> port(c.getSource(), StringArgumentType.getString(c, "namespace"), null))
                                .then(Commands.argument("outDir", StringArgumentType.greedyString())
                                        .executes(c -> port(c.getSource(),
                                                StringArgumentType.getString(c, "namespace"),
                                                StringArgumentType.getString(c, "outDir"))))))));
    }

    private static int port(CommandSourceStack source, String namespace, String outDirArg) {
        MinecraftServer server = source.getServer();
        ResourceManager resources = server.getResourceManager();

        Map<ResourceLocation, JsonObject> powers = loadDir(resources, namespace, "powers");
        Map<ResourceLocation, JsonObject> origins = loadDir(resources, namespace, "origins");
        if (origins.isEmpty()) {
            source.sendFailure(Component.literal("No origins found for namespace '" + namespace
                    + "'. Make sure the Origins data pack is loaded."));
            return 0;
        }

        Path outRoot = outDirArg != null && !outDirArg.isBlank()
                ? Path.of(outDirArg.trim())
                : server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("townstead_" + namespace + "_port");
        Path dataNs = outRoot.resolve("data").resolve(namespace);

        Map<ResourceLocation, JsonObject> geneFiles = new LinkedHashMap<>();
        Map<String, String> lang = new LinkedHashMap<>();
        List<String> report = new ArrayList<>();
        int originCount = 0;
        int geneCount = 0;
        int skipCount = 0;

        try {
            for (Map.Entry<ResourceLocation, JsonObject> entry : origins.entrySet()) {
                ResourceLocation originId = entry.getKey();
                JsonObject originJson = entry.getValue();

                List<ResourceLocation> originGenes = new ArrayList<>();
                List<PowerToGeneConverter.Skip> originSkips = new ArrayList<>();
                for (JsonElement ref : GsonHelper.getAsJsonArray(originJson, "powers", new JsonArray())) {
                    if (!ref.isJsonPrimitive()) continue;
                    ResourceLocation powerId = ResourceLocation.tryParse(ref.getAsString());
                    JsonObject power = powerId == null ? null : powers.get(powerId);
                    if (power == null) {
                        originSkips.add(new PowerToGeneConverter.Skip(String.valueOf(ref.getAsString()),
                                "?", "power JSON not found in pack"));
                        continue;
                    }
                    List<PowerToGeneConverter.ConvertedGene> converted = new ArrayList<>();
                    PowerToGeneConverter.convert(namespace, powerId, power, converted, originSkips);
                    for (PowerToGeneConverter.ConvertedGene gene : converted) {
                        if (geneFiles.containsKey(gene.id())) {
                            if (!originGenes.contains(gene.id())) originGenes.add(gene.id());
                            continue;
                        }
                        String key = "gene." + gene.id().getNamespace() + "." + gene.id().getPath().replace('/', '.');
                        gene.json().add("display_name", translateComponent(key));
                        lang.put(key, gene.displayText());
                        geneFiles.put(gene.id(), gene.json());
                        originGenes.add(gene.id());
                    }
                }

                writeOrigin(dataNs, namespace, originId, originJson, originGenes, originSkips);
                writeAncestry(dataNs, namespace, originId, originJson);

                originCount++;
                geneCount += originGenes.size();
                skipCount += originSkips.size();
                report.add("Origin " + originId + ": " + originGenes.size() + " genes, "
                        + originSkips.size() + " skipped");
                for (PowerToGeneConverter.Skip skip : originSkips) {
                    report.add("    skip " + skip.power() + " [" + skip.type() + "] - " + skip.reason());
                }
            }

            for (Map.Entry<ResourceLocation, JsonObject> gene : geneFiles.entrySet()) {
                writeJson(geneFile(dataNs, gene.getKey()), gene.getValue());
            }
            writeSpecies(dataNs);
            writeJson(dataNs.resolve("lang").resolve("en_us.json"), langJson(lang));
            writePackMeta(outRoot, namespace);
            writeReport(outRoot, namespace, originCount, geneCount, skipCount, report);
        } catch (Exception e) {
            Townstead.LOGGER.error("Origins port failed", e);
            source.sendFailure(Component.literal("Port failed: " + e.getMessage()));
            return 0;
        }

        int finalGenes = geneCount;
        int finalSkips = skipCount;
        int finalOrigins = originCount;
        source.sendSuccess(() -> Component.literal("Ported " + finalOrigins + " origin(s): "
                + geneFiles.size() + " unique genes, " + finalSkips + " powers skipped. Wrote to "
                + outRoot + ". See port_report.txt; refine before loading."), true);
        return 1;
    }

    private static void writeOrigin(Path dataNs, String namespace, ResourceLocation originId,
                                    JsonObject apoli, List<ResourceLocation> genes,
                                    List<PowerToGeneConverter.Skip> skips) throws Exception {
        JsonObject out = new JsonObject();
        out.add("display_name", component(apoli.get("name"), originId.getPath()));
        if (apoli.has("description")) out.add("backstory", component(apoli.get("description"), ""));
        out.addProperty("species", namespace + ":imported");
        out.addProperty("ancestry", namespace + ":" + originId.getPath());
        JsonArray geneArray = new JsonArray();
        for (ResourceLocation gene : genes) geneArray.add(gene.toString());
        out.add("genes", geneArray);
        if (!skips.isEmpty()) {
            JsonArray todo = new JsonArray();
            for (PowerToGeneConverter.Skip skip : skips) {
                todo.add(skip.power() + " [" + skip.type() + "] - " + skip.reason());
            }
            out.add("_todo_professions", todo);
        }
        writeJson(dataNs.resolve("origin").resolve(originId.getPath() + ".json"), out);
    }

    private static void writeAncestry(Path dataNs, String namespace, ResourceLocation originId,
                                      JsonObject apoli) throws Exception {
        JsonObject out = new JsonObject();
        out.add("display_name", component(apoli.get("name"), originId.getPath()));
        out.addProperty("species", namespace + ":imported");
        writeJson(dataNs.resolve("ancestry").resolve(originId.getPath() + ".json"), out);
    }

    private static void writeSpecies(Path dataNs) throws Exception {
        JsonObject out = new JsonObject();
        out.add("display_name", literal("Imported"));
        out.addProperty("shape", "humanoid");
        out.addProperty("admixture_chance", 0.0);
        writeJson(dataNs.resolve("species").resolve("imported.json"), out);
    }

    private static Map<ResourceLocation, JsonObject> loadDir(ResourceManager resources, String namespace, String dir) {
        Map<ResourceLocation, JsonObject> out = new LinkedHashMap<>();
        resources.listResources(dir, rl -> rl.getNamespace().equals(namespace) && rl.getPath().endsWith(".json"))
                .forEach((file, resource) -> {
                    String path = file.getPath();
                    String name = path.substring(dir.length() + 1, path.length() - ".json".length());
                    ResourceLocation id = ResourceLocation.tryParse(namespace + ":" + name);
                    JsonObject json = read(resource);
                    if (id != null && json != null) out.put(id, json);
                });
        return out;
    }

    private static JsonObject read(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static Path geneFile(Path dataNs, ResourceLocation geneId) {
        return dataNs.resolve("gene").resolve(geneId.getPath() + ".json");
    }

    private static void writeJson(Path path, JsonElement json) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json));
    }

    private static JsonObject langJson(Map<String, String> lang) {
        JsonObject out = new JsonObject();
        lang.forEach(out::addProperty);
        return out;
    }

    private static void writePackMeta(Path outRoot, String namespace) throws Exception {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", "Townstead port of " + namespace + " origins (refine before use)");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        writeJson(outRoot.resolve("pack.mcmeta"), root);
    }

    private static void writeReport(Path outRoot, String namespace, int origins, int genes, int skips,
                                    List<String> lines) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Townstead Origins port report for namespace: ").append(namespace).append('\n');
        sb.append(origins).append(" origins, ").append(genes).append(" gene references, ")
                .append(skips).append(" powers skipped.\n");
        sb.append("Skipped powers are class/active content deferred to the professions system,\n");
        sb.append("or powers/conditions outside the heritable subset. Each is stubbed as\n");
        sb.append("_todo_professions in its origin file.\n\n");
        for (String line : lines) sb.append(line).append('\n');
        Files.createDirectories(outRoot);
        Files.writeString(outRoot.resolve("port_report.txt"), sb.toString());
    }

    /** Pass an Apoli text component through, or wrap a bare string / fall back to a literal. */
    private static JsonElement component(JsonElement source, String fallback) {
        if (source != null && source.isJsonObject()) return source;
        if (source != null && source.isJsonPrimitive()) return literal(source.getAsString());
        return literal(fallback);
    }

    private static JsonObject literal(String text) {
        JsonObject out = new JsonObject();
        out.add("text", new JsonPrimitive(text));
        return out;
    }

    private static JsonObject translateComponent(String key) {
        JsonObject out = new JsonObject();
        out.addProperty("translate", key);
        return out;
    }
}
