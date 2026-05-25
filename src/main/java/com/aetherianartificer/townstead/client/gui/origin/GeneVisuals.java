package com.aetherianartificer.townstead.client.gui.origin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * Shared drawing + color helpers for gene chips, reused by the picker's traits
 * list and the dedicated gene screen so the visual language stays identical:
 * per-category tints, colour-palette gradient swatches, range bars, and the
 * solid/dashed dominance border.
 */
final class GeneVisuals {

    private GeneVisuals() {}

    static int categoryTint(String cat) {
        switch (cat == null ? "" : cat.toLowerCase(Locale.ROOT)) {
            case "diet":       return 0xFF8FBF6F;
            case "hydration":  return 0xFF6FA8D8;
            case "activity":   return 0xFFD8B45A;
            case "vitality":   return 0xFFCF7070;
            case "appearance": return 0xFF6FCFC0;
            case "genetics":   return 0xFFB58CD8;
            default:           return hashTint(cat);
        }
    }

    /** Stable mid-bright tint from a string (unknown categories / per-locus colours). */
    static int hashTint(String s) {
        int h = (s == null ? 0 : s.hashCode());
        float hue = ((h & 0x7FFFFFFF) % 360) / 360f;
        return hsv(hue, 0.45f, 0.82f);
    }

    /** A gradient swatch between two RGB endpoints. */
    static void drawSwatch(GuiGraphics g, int from, int to, int x, int y, int w, int h) {
        for (int i = 0; i < w; i++) {
            float frac = w <= 1 ? 0f : i / (float) (w - 1);
            int col = lerp(from, to, frac);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | (col & 0xFFFFFF));
        }
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF101010);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF101010);
        g.fill(x - 1, y, x, y + h, 0xFF101010);
        g.fill(x + w, y, x + w + 1, y + h, 0xFF101010);
    }

    /** A 0-1 track with the [min,max] band filled. */
    static void drawRangeBar(GuiGraphics g, float min, float max, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, 0xFF0A0A0A);
        int bandL = x + Math.round(min * w);
        int bandR = x + Math.round(max * w);
        g.fill(bandL, y, Math.max(bandL + 1, bandR), y + h, fill);
    }

    static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color, boolean dashed) {
        if (!dashed) {
            g.fill(x1, y1, x2, y1 + 1, color);
            g.fill(x1, y2 - 1, x2, y2, color);
            g.fill(x1, y1, x1 + 1, y2, color);
            g.fill(x2 - 1, y1, x2, y2, color);
            return;
        }
        for (int x = x1; x < x2; x += 4) {
            int e = Math.min(x + 2, x2);
            g.fill(x, y1, e, y1 + 1, color);
            g.fill(x, y2 - 1, e, y2, color);
        }
        for (int y = y1; y < y2; y += 4) {
            int e = Math.min(y + 2, y2);
            g.fill(x1, y, x1 + 1, e, color);
            g.fill(x2 - 1, y, x2, e, color);
        }
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    static int scale(int c, float f) {
        return ((int) (((c >> 16) & 0xFF) * f) << 16)
                | ((int) (((c >> 8) & 0xFF) * f) << 8)
                | (int) ((c & 0xFF) * f);
    }

    static int hsv(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0:  r = v; g = t; b = p; break;
            case 1:  r = q; g = v; b = p; break;
            case 2:  r = p; g = v; b = t; break;
            case 3:  r = p; g = q; b = v; break;
            case 4:  r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    static String truncate(Font font, String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        int ellW = font.width("…");
        StringBuilder out = new StringBuilder();
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = font.width(String.valueOf(s.charAt(i)));
            if (w + cw + ellW > maxW) break;
            out.append(s.charAt(i));
            w += cw;
        }
        return out.append("…").toString();
    }

    static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    static String shortId(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    static String prettify(String id) {
        String s = shortId(id).replace('_', ' ');
        if (s.isEmpty()) return s;
        StringBuilder out = new StringBuilder();
        for (String word : s.split(" ")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
