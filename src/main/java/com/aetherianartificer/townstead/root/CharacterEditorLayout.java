package com.aetherianartificer.townstead.root;

import java.util.List;

/**
 * Data-pack-driven layout for a species' Character editor tab. Authored as
 * {@code "character_editor": { "native": [...], "tabs": [...] }} on a species JSON.
 *
 * <p><b>Opt-out:</b> a species that declares no block leaves this {@code null}, and the editor
 * keeps MCA's full native Character tab unchanged (so humanoid origins need no data). When a block
 * IS present, only the listed {@link #nativeGroups MCA-native groups} survive, and the tabs are
 * either the explicit {@link #tabs} list or, when that's empty, auto-derived client-side: the kept
 * native groups become tabs, and the species' visually-editable genes bucket into tabs by their
 * {@code category}. Resolution lives client-side in the editor (it needs the synced gene catalog);
 * this record is just the authored intent, carried on {@link RootCatalogEntry}.</p>
 */
public record CharacterEditorLayout(List<String> nativeGroups, List<Tab> tabs) {
    public CharacterEditorLayout {
        nativeGroups = nativeGroups == null ? List.of() : List.copyOf(nativeGroups);
        tabs = tabs == null ? List.of() : List.copyOf(tabs);
    }

    /**
     * An explicit tab. {@code fields} are ordered refs, each either a native-group id (see the
     * {@code NATIVE_*} constants, delegating to MCA's own content) or a gene id (rendered from the
     * gene's display kind). {@code label}/{@code labelKey} are the server-resolved display string and
     * its translate key (empty when omitted, so the client falls back to the tab id / category).
     */
    public record Tab(String id, String label, String labelKey, List<String> fields) {
        public Tab {
            fields = fields == null ? List.of() : List.copyOf(fields);
        }
    }

    // MCA-native field-group ids a species may keep; each maps to one of MCA's Character subpages and
    // delegates to MCA's own rendering (with Townstead's existing per-rig trims, e.g. hiding the skin
    // texture picker / breast slider on a custom-textured "body").
    public static final String NATIVE_BODY = "body";       // MCA body subpage: size/width/skin/breast
    public static final String NATIVE_CLOTHES = "clothes";  // MCA clothing_style subpage
    public static final String NATIVE_HAIR = "hair";        // MCA hair_style (+ advanced) subpage
    public static final String NATIVE_EYES = "mca_eyes";    // MCA eyes subpage: FACE + eye color

    /** True for a recognized native-group id (vs a gene id) in a tab's field list. */
    public static boolean isNativeGroup(String id) {
        return NATIVE_BODY.equals(id) || NATIVE_CLOTHES.equals(id)
                || NATIVE_HAIR.equals(id) || NATIVE_EYES.equals(id);
    }
}
