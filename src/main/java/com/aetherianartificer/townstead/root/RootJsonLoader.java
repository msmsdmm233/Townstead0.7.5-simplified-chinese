package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.data.TownsteadSchema;
import com.aetherianartificer.townstead.root.personality.Personalities;
import com.aetherianartificer.townstead.root.personality.PersonalityPolicies;
import com.aetherianartificer.townstead.root.personality.PersonalityPolicyRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Loads {@link Root} from {@code data/<ns>/origin/*.json}. */
public final class RootJsonLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Townstead.MOD_ID + "/RootJsonLoader");
    private static final Gson GSON = new Gson();

    public RootJsonLoader() {
        super(GSON, "origin");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, String> lang = DataPackLang.loadLangIndex(resourceManager);
        Map<ResourceLocation, Root> parsed = new LinkedHashMap<>();
        Map<ResourceLocation, Personalities> policies = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation file = entry.getKey();
            String ctx = file.toString();
            try {
                JsonObject obj = GsonHelper.convertToJsonObject(entry.getValue(), ctx);
                TownsteadSchema.validate(obj, "townstead:origin/v1");
                Component displayName = DataPackLang.parseComponent(obj.get("display_name"), ctx, lang);
                ResourceLocation species = RootJsonParsing.optionalId(obj, "species", ctx, LOGGER);
                ResourceLocation ancestry = RootJsonParsing.optionalId(obj, "ancestry", ctx, LOGGER);
                ResourceLocation lineage = RootJsonParsing.optionalId(obj, "lineage", ctx, LOGGER);
                Demonym demonym = RootJsonParsing.demonym(obj, ctx, lang);
                Component backstory = RootJsonParsing.backstory(obj, ctx, lang);
                Genome genome = RootJsonParsing.genes(obj, ctx, LOGGER);
                SpawnBias spawnBias = RootJsonParsing.spawnBias(obj, ctx, LOGGER);
                parsed.put(file, new Root(file, displayName, species, ancestry, lineage, demonym, backstory, genome, spawnBias));
                policies.put(file, PersonalityPolicies.parse(obj));
            } catch (Exception ex) {
                LOGGER.warn("Failed to parse origin {}: {}", file, ex.getMessage());
            }
        }
        RootRegistry.replaceAll(parsed);
        PersonalityPolicyRegistry.setRoot(policies);
        LOGGER.info("Loaded {} origins", parsed.size());
    }
}
