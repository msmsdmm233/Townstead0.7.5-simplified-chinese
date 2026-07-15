package com.aetherianartificer.townstead.root;

import com.aetherianartificer.townstead.TownsteadConfig;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-config blocklist over the loaded root content. An admin can disable
 * individual roots, or whole species / ancestries / lineages (which blocks every
 * root resolving through them). Blocked roots are dropped from the founder spawn
 * roll, rejected by the picker apply, and flagged in the synced catalog so picker
 * lists hide them. Not retroactive: an entity already carrying a blocked root
 * keeps it, and inheritance/breeding is untouched.
 */
public final class RootBlocklist {

    private static volatile Rules rules = Rules.EMPTY;

    private RootBlocklist() {}

    public static boolean isBlocked(@Nullable ResourceLocation rootId) {
        if (rootId == null) return false;
        Rules r = rules();
        if (r.isEmpty()) return false;
        if (r.roots().contains(LegacyNamespace.canonical(rootId))) return true;
        Root root = RootRegistry.byId(rootId);
        if (root == null) return false;
        if (root.lineage() != null) {
            if (r.lineages().contains(LegacyNamespace.canonical(root.lineage()))) return true;
            Lineage lineage = LineageRegistry.byId(root.lineage());
            if (lineage != null && lineage.ancestry() != null
                    && r.ancestries().contains(LegacyNamespace.canonical(lineage.ancestry()))) {
                return true;
            }
        }
        if (root.ancestry() != null
                && r.ancestries().contains(LegacyNamespace.canonical(root.ancestry()))) {
            return true;
        }
        if (!r.species().isEmpty()) {
            ResourceLocation species = RootRegistry.effectiveSpecies(rootId);
            return species != null && r.species().contains(LegacyNamespace.canonical(species));
        }
        return false;
    }

    private static Rules rules() {
        List<? extends String> roots = TownsteadConfig.blockedRootIds();
        List<? extends String> species = TownsteadConfig.blockedSpeciesIds();
        List<? extends String> ancestries = TownsteadConfig.blockedAncestryIds();
        List<? extends String> lineages = TownsteadConfig.blockedLineageIds();
        Rules current = rules;
        if (current.matchesInputs(roots, species, ancestries, lineages)) return current;
        synchronized (RootBlocklist.class) {
            current = rules;
            if (current.matchesInputs(roots, species, ancestries, lineages)) return current;
            current = Rules.compile(roots, species, ancestries, lineages);
            rules = current;
            return current;
        }
    }

    private record Rules(List<String> rawRoots, List<String> rawSpecies,
                         List<String> rawAncestries, List<String> rawLineages,
                         Set<ResourceLocation> roots, Set<ResourceLocation> species,
                         Set<ResourceLocation> ancestries, Set<ResourceLocation> lineages) {

        static final Rules EMPTY = new Rules(List.of(), List.of(), List.of(), List.of(),
                Set.of(), Set.of(), Set.of(), Set.of());

        static Rules compile(List<? extends String> roots, List<? extends String> species,
                             List<? extends String> ancestries, List<? extends String> lineages) {
            return new Rules(List.copyOf(roots), List.copyOf(species),
                    List.copyOf(ancestries), List.copyOf(lineages),
                    parse(roots), parse(species), parse(ancestries), parse(lineages));
        }

        boolean matchesInputs(List<? extends String> roots, List<? extends String> species,
                              List<? extends String> ancestries, List<? extends String> lineages) {
            return rawRoots.equals(roots) && rawSpecies.equals(species)
                    && rawAncestries.equals(ancestries) && rawLineages.equals(lineages);
        }

        boolean isEmpty() {
            return roots.isEmpty() && species.isEmpty() && ancestries.isEmpty() && lineages.isEmpty();
        }

        /** Entries without a namespace default to {@code townstead_roots}; legacy ids fold onto it. */
        private static Set<ResourceLocation> parse(List<? extends String> raw) {
            if (raw.isEmpty()) return Set.of();
            Set<ResourceLocation> out = new HashSet<>();
            for (String entry : raw) {
                String id = entry.indexOf(':') < 0 ? RootRegistry.NAMESPACE + ":" + entry : entry;
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) out.add(LegacyNamespace.canonical(rl));
            }
            return Set.copyOf(out);
        }
    }
}
