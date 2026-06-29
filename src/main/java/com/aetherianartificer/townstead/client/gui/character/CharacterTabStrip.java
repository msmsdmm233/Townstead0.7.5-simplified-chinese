package com.aetherianartificer.townstead.client.gui.character;

import com.aetherianartificer.townstead.client.accessibility.Accessibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Horizontally-scrollable Character-editor tab strip. Lays out one button per resolved tab in a fixed
 * band; when the buttons are wider than the band it scrolls natively (mouse wheel), clipped by scissor.
 * The button matching the current page renders pressed, mirroring how MCA greys its active subpage tab.
 * Drawn with {@code fill} (not GUI sprites) so it compiles on both stonecutter targets; this feature
 * only renders on the 1.21 MCA anyway.
 */
public class CharacterTabStrip extends AbstractWidget {
    private static final int PAD = 8;
    private static final int GAP = 1;
    private static final int MIN_TAB = 30;
    private static final int BORDER = 0xFF101010;
    private static final int BG_NORMAL = 0xFF555555;
    private static final int BG_HOVER = 0xFF707070;
    private static final int BG_SELECTED = 0xFF2D2D2D;

    public record Entry(String pageId, Component label) {}

    private final List<Entry> entries;
    private final Consumer<String> onSelect;
    private final int[] tabX;     // content-space left of each tab
    private final int[] tabW;     // width of each tab
    private final int contentW;   // total laid-out width
    private String selected;
    private int scrollX;

    public CharacterTabStrip(int x, int y, int width, int height, List<Entry> entries,
                             String selected, Consumer<String> onSelect) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.entries = List.copyOf(entries);
        this.onSelect = onSelect;
        this.selected = selected;
        var font = Minecraft.getInstance().font;
        this.tabX = new int[this.entries.size()];
        this.tabW = new int[this.entries.size()];
        int cx = 0;
        for (int i = 0; i < this.entries.size(); i++) {
            int w = Math.max(MIN_TAB, font.width(this.entries.get(i).label()) + PAD * 2);
            tabX[i] = cx;
            tabW[i] = w;
            cx += w + GAP;
        }
        this.contentW = Math.max(0, cx - GAP);
    }

    public void setSelected(String pageId) {
        this.selected = pageId;
    }

    private int maxScroll() {
        return Math.max(0, contentW - width);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        scrollX = Math.max(0, Math.min(scrollX, maxScroll()));
        g.enableScissor(getX(), getY(), getX() + width, getY() + height);
        var font = Minecraft.getInstance().font;
        boolean inBand = mouseY >= getY() && mouseY < getY() + height && mouseX >= getX() && mouseX < getX() + width;
        for (int i = 0; i < entries.size(); i++) {
            int sx = getX() + tabX[i] - scrollX;
            int w = tabW[i];
            if (sx + w <= getX() || sx >= getX() + width) continue; // fully clipped
            boolean isSel = entries.get(i).pageId().equals(selected);
            boolean hover = inBand && mouseX >= sx && mouseX < sx + w;
            int bg = isSel ? BG_SELECTED : (hover ? BG_HOVER : BG_NORMAL);
            g.fill(sx, getY(), sx + w, getY() + height, bg);
            g.fill(sx, getY(), sx + w, getY() + 1, BORDER);
            g.fill(sx, getY() + height - 1, sx + w, getY() + height, BORDER);
            g.fill(sx, getY(), sx + 1, getY() + height, BORDER);
            g.fill(sx + w - 1, getY(), sx + w, getY() + height, BORDER);
            int color = isSel ? 0xFFAAAAAA : 0xFFFFFFFF;
            g.drawCenteredString(font, entries.get(i).label(), sx + w / 2, getY() + (height - 8) / 2, color);
        }
        g.disableScissor();
        if (scrollX > 0) g.drawString(font, "‹", getX() + 1, getY() + (height - 8) / 2, 0xFFFFFFFF, true);
        if (scrollX < maxScroll())
            g.drawString(font, "›", getX() + width - 6, getY() + (height - 8) / 2, 0xFFFFFFFF, true);
    }

    private boolean doScroll(double mouseX, double mouseY, double dy) {
        if (maxScroll() == 0) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;
        int step = Accessibility.isReduceMotion() ? 40 : 24;
        scrollX = Math.max(0, Math.min(scrollX - (int) Math.signum(dy) * step, maxScroll()));
        return true;
    }

    //? if >=1.21 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return doScroll(mouseX, mouseY, dy);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dy) {
        return doScroll(mouseX, mouseY, dy);
    }
    *///?}

    @Override
    public void onClick(double mouseX, double mouseY) {
        for (int i = 0; i < entries.size(); i++) {
            int sx = getX() + tabX[i] - scrollX;
            int w = tabW[i];
            if (mouseX >= sx && mouseX < sx + w && mouseX >= getX() && mouseX < getX() + width) {
                String id = entries.get(i).pageId();
                if (!id.equals(selected) && onSelect != null) onSelect.accept(id);
                return;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.translatable("townstead.character.tabs"));
    }

    /** Convenience: build entries from resolved tabs. */
    public static List<Entry> entriesOf(CharacterEditorResolver.Resolved resolved) {
        List<Entry> out = new ArrayList<>();
        for (CharacterEditorResolver.Tab t : resolved.tabs()) out.add(new Entry(t.pageId(), t.label()));
        return out;
    }
}
