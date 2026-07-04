package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.root.gene.Gene;
import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneRegistry;
import com.aetherianartificer.townstead.root.gene.InheritedGene;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side: flattens the loaded origin + gene registries into wire/UI data so
 * a remote client (whose datapack-driven registries are empty) can render and
 * label origins and their inherited genes. Builds an origin list plus a gene
 * dictionary covering only the genes some origin actually inherits.
 */
public final class RootCatalog {

    private RootCatalog() {}

    public record Snapshot(List<RootCatalogEntry> origins, List<GeneCatalogEntry> genes,
                           List<TraitCatalogEntry> traits,
                           List<com.aetherianartificer.townstead.root.rig.RigDefinition> rigs) {}

    public static Snapshot build() {
        List<RootCatalogEntry> origins = new ArrayList<>();
        Map<ResourceLocation, GeneCatalogEntry> genes = new LinkedHashMap<>();
        List<TraitCatalogEntry> traits = new ArrayList<>();
        for (com.aetherianartificer.townstead.root.trait.DataTrait t
                : com.aetherianartificer.townstead.root.trait.TraitRegistry.all()) {
            traits.add(new TraitCatalogEntry(t.id(), t.chance(), t.inherit(), t.usableOnPlayer(), t.hidden()));
        }

        for (Root origin : RootRegistry.all()) {
            Component nameC = origin.displayName();
            String name = nameC.getString();
            Demonym demonym = RootRegistry.resolveDemonym(origin);
            Component singC = demonym != null ? demonym.singular() : nameC;
            Component plurC = demonym != null ? demonym.plural() : nameC;
            String singular = singC.getString();
            String plural = plurC.getString();
            Component backstory = RootRegistry.resolveBackstory(origin);
            Species spc = SpeciesRegistry.byId(origin.species());
            Ancestry anc = AncestryRegistry.byId(origin.ancestry());
            Lineage lin = LineageRegistry.byId(origin.lineage());

            List<InheritedGene> inherited = RootRegistry.effectiveInheritedGenes(origin.id());
            List<RootCatalogEntry.Inherited> views = new ArrayList<>(inherited.size());
            for (InheritedGene gene : inherited) {
                Gene def = GeneRegistry.byId(gene.geneId());
                // Body metrics render as ranges in the Body tab, not as viewer chips.
                if (def != null && def.instance() instanceof
                        com.aetherianartificer.townstead.root.gene.types.BodyMetricGeneType.Instance) {
                    continue;
                }
                views.add(new RootCatalogEntry.Inherited(gene.geneId().toString(), gene.occurrence()));
                genes.computeIfAbsent(gene.geneId(), RootCatalog::toGeneEntry);
            }

            Map<String, GeneRange> metrics = RootGenes.resolveBodyMetrics(inherited);
            List<RootCatalogEntry.GeneRangeView> ranges = new ArrayList<>(metrics.size());
            for (Map.Entry<String, GeneRange> r : metrics.entrySet()) {
                ranges.add(new RootCatalogEntry.GeneRangeView(r.getKey(), r.getValue().min(), r.getValue().max()));
            }

            origins.add(new RootCatalogEntry(
                    origin.id().toString(), name, singular, plural,
                    backstory != null ? backstory.getString() : "",
                    name(spc), name(anc), name(lin),
                    views, ranges,
                    keyOf(nameC), keyOf(singC), keyOf(plurC), keyOf(backstory),
                    keyOf(spc != null ? spc.displayName() : null),
                    keyOf(anc != null ? anc.displayName() : null),
                    keyOf(lin != null ? lin.displayName() : null),
                    spc != null ? spc.rig().base() : Rig.VILLAGER.base(),
                    spc != null ? spc.rig().scale() : Rig.VILLAGER.scale(),
                    spc != null ? spc.animations() : Animations.DEFAULT,
                    spc == null || spc.breasts(),
                    stageRigsFor(origin.id()),
                    spc != null ? spc.characterEditor() : null));
        }
        return new Snapshot(origins, new ArrayList<>(genes.values()), traits,
                com.aetherianartificer.townstead.root.rig.RigRegistry.all());
    }

    /**
     * The per-stage rig overrides for an origin's effective life cycle (one entry per stage, empty string
     * = species rig). Returns an empty list when no stage overrides the rig, so only origins that actually
     * use a per-stage model (e.g. an egg-laying species) ship the array.
     */
    private static List<String> stageRigsFor(net.minecraft.resources.ResourceLocation rootId) {
        LifeCycle cycle = RootRegistry.effectiveLifeCycle(rootId);
        if (cycle == null || cycle.isEmpty()) return List.of();
        List<String> rigs = new ArrayList<>(cycle.size());
        boolean any = false;
        for (LifeStage stage : cycle.stages()) {
            String r = stage.rig() == null ? "" : stage.rig();
            rigs.add(r);
            if (!r.isEmpty()) any = true;
        }
        return any ? rigs : List.of();
    }

    private static GeneCatalogEntry toGeneEntry(ResourceLocation geneId) {
        Gene gene = GeneRegistry.byId(geneId);
        if (gene == null) {
            return new GeneCatalogEntry(geneId.toString(), geneId.getPath(), "", "general",
                    GeneDisplay.Kind.BOOLEAN.ordinal(), 0f, 1f, "", 0f, 0, "", 1, List.of(), "", "", "");
        }
        GeneDisplay display = gene.display();
        List<GeneCatalogEntry.Variant> variants = new ArrayList<>();
        if (gene.hasVariants()) {
            for (com.aetherianartificer.townstead.root.gene.GeneVariant v : gene.variants()) {
                variants.add(new GeneCatalogEntry.Variant(
                        v.id(), v.displayName().getString(), v.weight(), keyOf(v.displayName()),
                        variantTint(v.instance()), variantTexture(v.instance()), variantGlow(v.instance())));
            }
        }
        return new GeneCatalogEntry(
                geneId.toString(),
                gene.displayName().getString(),
                gene.description() != null ? gene.description().getString() : "",
                gene.category(),
                display.kind().ordinal(),
                display.min(), display.max(),
                display.targetId(), display.amount(),
                gene.dominance().ordinal(),
                gene.locus() != null ? gene.locus().toString() : "",
                gene.weight(),
                variants,
                keyOf(gene.displayName()),
                keyOf(gene.description()),
                faceSlotOf(gene.instance()));
    }

    /** The colour tint a variant carries (skin tone or eye colour), or {@code -1} for none. */
    private static int variantTint(com.aetherianartificer.townstead.root.gene.GeneInstance instance) {
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.SkinToneGeneType.Instance st) return st.tint();
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.EyeColorGeneType.Instance ec) return ec.tint();
        return -1;
    }

    /** The face sprite-strip texture a variant carries (eyes/mouth), or {@code ""}. */
    private static String variantTexture(com.aetherianartificer.townstead.root.gene.GeneInstance instance) {
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.EyesGeneType.Instance e) return e.texture();
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.MouthGeneType.Instance m) return m.texture();
        return "";
    }

    /** Whether an eyes variant is emissive (glowing). */
    private static boolean variantGlow(com.aetherianartificer.townstead.root.gene.GeneInstance instance) {
        return instance instanceof com.aetherianartificer.townstead.root.gene.types.EyesGeneType.Instance e && e.glow();
    }

    /** The face slot a gene occupies ("eyes"/"mouth"/"eye_color"), else "". */
    private static String faceSlotOf(com.aetherianartificer.townstead.root.gene.GeneInstance instance) {
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.EyesGeneType.Instance) return "eyes";
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.MouthGeneType.Instance) return "mouth";
        if (instance instanceof com.aetherianartificer.townstead.root.gene.types.EyeColorGeneType.Instance) return "eye_color";
        return "";
    }

    /** Translate key of a Component when it is a {@code translatable}, else "" (literal). */
    private static String keyOf(Component c) {
        if (c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
            return tc.getKey();
        }
        return "";
    }

    private static String name(Species species) {
        return species != null ? species.displayName().getString() : "";
    }

    private static String name(Ancestry ancestry) {
        return ancestry != null ? ancestry.displayName().getString() : "";
    }

    private static String name(Lineage lineage) {
        return lineage != null ? lineage.displayName().getString() : "";
    }
}
