package com.aetherianartificer.townstead.origin;

import com.aetherianartificer.townstead.origin.gene.GeneDisplay;

/**
 * One gene's display data for the catalog sync / picker, flattened to primitives
 * so the client renders it without the server-only {@code GeneRegistry} or Java
 * gene types.
 *
 * <p>{@code displayKind} is {@link GeneDisplay.Kind#ordinal()}
 * (0 = RANGE, 1 = BOOLEAN, 2 = INFLUENCE, 3 = COLOR). {@code min}/{@code max} apply to
 * RANGE; {@code targetId} to INFLUENCE (target gene) and COLOR (two hex endpoints
 * {@code "rrggbb-rrggbb"}, read via {@link #colorFrom()}/{@link #colorTo()});
 * {@code amount} to INFLUENCE. {@code dominanceOrdinal} is
 * {@link com.aetherianartificer.townstead.origin.gene.Dominance#ordinal()}
 * (0 = DOMINANT, 1 = RECESSIVE); {@code locus} is empty when none.</p>
 */
public record GeneCatalogEntry(
        String id,
        String name,
        String description,
        String category,
        int displayKind,
        float min,
        float max,
        String targetId,
        float amount,
        int dominanceOrdinal,
        String locus,
        int weight
) {
    public boolean isRange() {
        return displayKind == GeneDisplay.Kind.RANGE.ordinal();
    }

    public boolean isInfluence() {
        return displayKind == GeneDisplay.Kind.INFLUENCE.ordinal();
    }

    public boolean isColor() {
        return displayKind == GeneDisplay.Kind.COLOR.ordinal();
    }

    public boolean isAttachment() {
        return displayKind == GeneDisplay.Kind.ATTACHMENT.ordinal();
    }

    /** For ATTACHMENT genes, the attachment id (rides in {@code targetId}). */
    public String attachmentId() { return targetId; }

    /** COLOR gradient endpoints (RGB), parsed from {@code targetId} {@code "rrggbb-rrggbb"}. */
    public int colorFrom() { return colorPart(0, 0xCFCFCF); }
    public int colorTo()   { return colorPart(1, 0x4A4A4A); }

    private int colorPart(int idx, int fallback) {
        if (targetId == null) return fallback;
        int dash = targetId.indexOf('-');
        if (dash < 0) return fallback;
        String part = idx == 0 ? targetId.substring(0, dash) : targetId.substring(dash + 1);
        try {
            return Integer.parseInt(part.trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean isRecessive() {
        return dominanceOrdinal == com.aetherianartificer.townstead.origin.gene.Dominance.RECESSIVE.ordinal();
    }
}
