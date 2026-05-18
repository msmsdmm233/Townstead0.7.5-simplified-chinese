package com.aetherianartificer.townstead.client.gui.calendar;

import com.aetherianartificer.townstead.calendar.CalendarClientStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Stardew-style month-grid calendar. Reads the active profile shape from
 * {@link CalendarClientStore} (months list, days_per_week, weekdays, year
 * suffix / eras, epoch) so it renders any month/year locally without
 * server round-trips per navigation.
 *
 * <h2>Layout</h2>
 *
 * Panel dimensions are computed at {@link #init()} time from the current
 * month's grid shape (columns = {@code days_per_week}, rows derived from
 * the start-of-month day-of-week offset plus the month's day count), with
 * screen-edge caps for very long weeks / months. Horizontal scrolling
 * engages when the week exceeds the cap (e.g., Mayan 13-day week).
 *
 * <h2>Visual style</h2>
 *
 * Wooden bevel frame around a parchment field — all drawn with layered
 * {@code g.fill} primitives so no textures need to ship. Cells render as
 * pale parchment squares with sepia text; today is a warm gold marker.
 */
public class CalendarScreen extends Screen {

    // ── Layout constants ───────────────────────────────────────────────────
    private static final int FRAME_THICKNESS = 6;
    private static final int INNER_PADDING = 8;
    private static final int HEADER_H = 22;
    private static final int WEEKDAY_H = 16;
    private static final int CELL_SIZE = 22;
    private static final int CELL_GAP = 2;
    private static final int MAX_PANEL_W_MARGIN = 40; // px to leave around panel
    private static final int MAX_PANEL_H_MARGIN = 60;

    // ── Color palette ──────────────────────────────────────────────────────
    // Wood frame: outer dark walnut, inner lighter highlight, inset shadow
    private static final int FRAME_OUTER     = 0xFF3E2510;
    private static final int FRAME_MID       = 0xFF6B4422;
    private static final int FRAME_HIGHLIGHT = 0xFF9B7140;
    private static final int FRAME_INSET     = 0xFF2A1808;
    // Parchment field
    private static final int PARCHMENT       = 0xFFE8D5A8;
    private static final int PARCHMENT_EDGE  = 0xFFC9B07C;
    // Cells
    private static final int CELL_FILL       = 0xFFF0DDA8;
    private static final int CELL_BORDER     = 0xFFB8985C;
    private static final int CELL_HOVER      = 0xFFFFF0C0;
    private static final int TODAY_FILL      = 0xFFFFB347;
    private static final int TODAY_BORDER    = 0xFF8C4A0E;
    // Text
    private static final int TEXT_HEADER     = 0xFF1F1305;
    private static final int TEXT_WEEKDAY    = 0xFF5C3A1E;
    private static final int TEXT_DAY        = 0xFF3A2410;
    private static final int TEXT_TODAY      = 0xFF1F1305;

    // ── State ──────────────────────────────────────────────────────────────
    private int viewYear;
    private int viewMonth;
    private double scrollX;
    private double maxScrollX;
    private int panelX, panelY, panelW, panelH;
    private int contentX, contentY, contentW, contentH;

    @Nullable private Button prevMonthBtn, nextMonthBtn, prevYearBtn, nextYearBtn;

    public CalendarScreen() {
        super(Component.translatable("gui.townstead_calendar.title"));
    }

    @Override
    protected void init() {
        CalendarClientStore.Snapshot snap = CalendarClientStore.get();
        if (snap == null) {
            viewYear = 1;
            viewMonth = 1;
        } else {
            viewYear = snap.year();
            viewMonth = Math.max(1, snap.monthIndex());
        }
        relayout();
        addNavButtons();
    }

    /** Compute panel + content dimensions for the current viewed month. */
    private void relayout() {
        CalendarClientStore.Snapshot snap = CalendarClientStore.get();
        int dpw = (snap != null) ? Math.max(1, snap.daysPerWeek()) : 7;
        int rows = computeRowCount(snap);

        int gridW = dpw * CELL_SIZE + (dpw - 1) * CELL_GAP;
        int gridH = rows * CELL_SIZE + (rows - 1) * CELL_GAP;

        contentW = gridW;
        contentH = HEADER_H + WEEKDAY_H + gridH;

        // Cap to screen
        int maxContentW = Math.max(120, width - MAX_PANEL_W_MARGIN - 2 * (FRAME_THICKNESS + INNER_PADDING));
        int maxContentH = Math.max(120, height - MAX_PANEL_H_MARGIN - 2 * (FRAME_THICKNESS + INNER_PADDING));
        contentW = Math.min(contentW, maxContentW);
        contentH = Math.min(contentH, maxContentH);

        panelW = contentW + 2 * (FRAME_THICKNESS + INNER_PADDING);
        panelH = contentH + 2 * (FRAME_THICKNESS + INNER_PADDING);

        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + FRAME_THICKNESS + INNER_PADDING;
        contentY = panelY + FRAME_THICKNESS + INNER_PADDING;

        maxScrollX = Math.max(0, gridW - contentW);
        if (scrollX > maxScrollX) scrollX = maxScrollX;
        if (scrollX < 0) scrollX = 0;
    }

    private int computeRowCount(@Nullable CalendarClientStore.Snapshot snap) {
        if (snap == null || snap.months().isEmpty()) return 1;
        int monthIdx = Math.max(0, Math.min(viewMonth - 1, snap.months().size() - 1));
        int monthDays = snap.months().get(monthIdx).days();
        int dpw = Math.max(1, snap.daysPerWeek());
        int startDow = startDayOfWeek(snap, viewYear, monthIdx);
        int totalCells = startDow + monthDays;
        return Math.max(1, (totalCells + dpw - 1) / dpw);
    }

    private void addNavButtons() {
        clearWidgets();
        int btnY = panelY + FRAME_THICKNESS + INNER_PADDING - 1;
        int btnW = 14;
        int btnH = 16;
        prevYearBtn = addRenderableWidget(Button.builder(
                Component.literal("«"),
                b -> { viewYear = Math.max(1, viewYear - 1); relayout(); addNavButtons(); })
                .bounds(contentX, btnY, btnW, btnH).build());
        prevMonthBtn = addRenderableWidget(Button.builder(
                Component.literal("‹"),
                b -> { navigateMonth(-1); addNavButtons(); })
                .bounds(contentX + btnW + 2, btnY, btnW, btnH).build());
        nextMonthBtn = addRenderableWidget(Button.builder(
                Component.literal("›"),
                b -> { navigateMonth(1); addNavButtons(); })
                .bounds(contentX + contentW - 2 * btnW - 2, btnY, btnW, btnH).build());
        nextYearBtn = addRenderableWidget(Button.builder(
                Component.literal("»"),
                b -> { viewYear = viewYear + 1; relayout(); addNavButtons(); })
                .bounds(contentX + contentW - btnW, btnY, btnW, btnH).build());
    }

    private void navigateMonth(int delta) {
        CalendarClientStore.Snapshot snap = CalendarClientStore.get();
        if (snap == null || snap.months().isEmpty()) return;
        int monthCount = snap.months().size();
        int next = viewMonth + delta;
        while (next < 1) { next += monthCount; viewYear = Math.max(1, viewYear - 1); }
        while (next > monthCount) { next -= monthCount; viewYear = viewYear + 1; }
        viewMonth = next;
        relayout();
    }

    //? if >=1.21 {
    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (maxScrollX > 0 && isMouseOverGrid(mx, my)) {
            scrollX = clampScroll(scrollX - dy * 16.0);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScrollX > 0 && isMouseOverGrid(mx, my)) {
            scrollX = clampScroll(scrollX - delta * 16.0);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
    *///?}

    private double clampScroll(double v) {
        if (v < 0) return 0;
        if (v > maxScrollX) return maxScrollX;
        return v;
    }

    private boolean isMouseOverGrid(double mx, double my) {
        int gridY = contentY + HEADER_H + WEEKDAY_H;
        return mx >= contentX && mx < contentX + contentW
                && my >= gridY && my < contentY + contentH;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderFrame(g);
        CalendarClientStore.Snapshot snap = CalendarClientStore.get();
        if (snap == null) {
            g.drawCenteredString(font, Component.translatable("gui.townstead_calendar.no_data"),
                    panelX + panelW / 2, panelY + panelH / 2 - 4, TEXT_DAY);
            return;
        }
        renderHeader(g, snap);
        renderWeekdayLabels(g, snap);
        renderGrid(g, snap, mouseX, mouseY);
    }

    /**
     * Wooden bevel frame around a parchment field. Order of layers (outer →
     * inner): dark walnut outline, mid-tone wood, light highlight inner edge,
     * inset shadow, parchment fill, parchment edge shadow.
     */
    private void renderFrame(GuiGraphics g) {
        int x0 = panelX, y0 = panelY, x1 = panelX + panelW, y1 = panelY + panelH;
        // Outer dark line
        g.fill(x0, y0, x1, y0 + 1, FRAME_OUTER);
        g.fill(x0, y1 - 1, x1, y1, FRAME_OUTER);
        g.fill(x0, y0, x0 + 1, y1, FRAME_OUTER);
        g.fill(x1 - 1, y0, x1, y1, FRAME_OUTER);
        // Mid wood body
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, FRAME_MID);
        // Highlight inner edge (one row of lighter wood)
        int hx0 = x0 + FRAME_THICKNESS - 2;
        int hy0 = y0 + FRAME_THICKNESS - 2;
        int hx1 = x1 - FRAME_THICKNESS + 2;
        int hy1 = y1 - FRAME_THICKNESS + 2;
        g.fill(hx0, hy0, hx1, hy0 + 1, FRAME_HIGHLIGHT);
        g.fill(hx0, hy0, hx0 + 1, hy1, FRAME_HIGHLIGHT);
        g.fill(hx0, hy1 - 1, hx1, hy1, FRAME_INSET);
        g.fill(hx1 - 1, hy0, hx1, hy1, FRAME_INSET);
        // Inset shadow inside parchment area
        int px0 = x0 + FRAME_THICKNESS;
        int py0 = y0 + FRAME_THICKNESS;
        int px1 = x1 - FRAME_THICKNESS;
        int py1 = y1 - FRAME_THICKNESS;
        g.fill(px0, py0, px1, py1, FRAME_INSET);
        // Parchment field
        g.fill(px0 + 1, py0 + 1, px1 - 1, py1 - 1, PARCHMENT);
        // Subtle edge shadow (a single line of darker parchment around the inside)
        g.fill(px0 + 1, py0 + 1, px1 - 1, py0 + 2, PARCHMENT_EDGE);
        g.fill(px0 + 1, py0 + 1, px0 + 2, py1 - 1, PARCHMENT_EDGE);
    }

    private void renderHeader(GuiGraphics g, CalendarClientStore.Snapshot snap) {
        Component monthName = monthNameFor(snap, viewMonth - 1);
        // Resolve display year through era when defined; otherwise use absolute year + suffix
        int displayYear = viewYear;
        Component yearLabel = Component.empty();
        CalendarClientStore.EraResolved era = snap.resolveEra(viewYear);
        if (era != null) {
            displayYear = era.displayedYear();
            yearLabel = era.nameComponent();
        } else if (snap.hasYearSuffix()) {
            yearLabel = snap.yearSuffixComponent();
        }
        String suffixStr = yearLabel.getString();
        String headerStr = monthName.getString() + "  " + displayYear
                + (suffixStr.isEmpty() ? "" : " " + suffixStr);
        g.drawCenteredString(font, Component.literal(headerStr),
                contentX + contentW / 2, contentY + (HEADER_H - font.lineHeight) / 2 + 1, TEXT_HEADER);
    }

    private void renderWeekdayLabels(GuiGraphics g, CalendarClientStore.Snapshot snap) {
        int dpw = Math.max(1, snap.daysPerWeek());
        int labelY = contentY + HEADER_H;
        g.enableScissor(contentX, labelY, contentX + contentW, labelY + WEEKDAY_H);
        boolean named = snap.hasWeekdays() && snap.weekdays().size() == dpw;
        for (int col = 0; col < dpw; col++) {
            int cellX = contentX + col * (CELL_SIZE + CELL_GAP) - (int) scrollX;
            String label = named
                    ? snap.weekdays().get(col).shortComponent().getString()
                    : Integer.toString(col + 1);
            g.drawCenteredString(font, label,
                    cellX + CELL_SIZE / 2, labelY + (WEEKDAY_H - font.lineHeight) / 2, TEXT_WEEKDAY);
        }
        g.disableScissor();
    }

    private void renderGrid(GuiGraphics g, CalendarClientStore.Snapshot snap, int mouseX, int mouseY) {
        int dpw = Math.max(1, snap.daysPerWeek());
        if (snap.months().isEmpty()) return;
        int safeMonthIdx = Math.max(0, Math.min(viewMonth - 1, snap.months().size() - 1));
        int monthDays = snap.months().get(safeMonthIdx).days();
        int startDow = startDayOfWeek(snap, viewYear, safeMonthIdx);

        int gridTop = contentY + HEADER_H + WEEKDAY_H;
        int gridBottom = contentY + contentH;
        g.enableScissor(contentX, gridTop, contentX + contentW, gridBottom);

        boolean isCurrentMonth = (viewYear == snap.year() && viewMonth == snap.monthIndex());
        int todayDom = snap.dayOfMonth();

        for (int d = 1; d <= monthDays; d++) {
            int cellIndex = startDow + d - 1;
            int col = cellIndex % dpw;
            int row = cellIndex / dpw;
            int cellX = contentX + col * (CELL_SIZE + CELL_GAP) - (int) scrollX;
            int cellY = gridTop + row * (CELL_SIZE + CELL_GAP);

            boolean today = isCurrentMonth && d == todayDom;
            boolean hover = !today && mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE
                    && mouseY >= gridTop && mouseY < gridBottom;

            int fillColor   = today ? TODAY_FILL   : (hover ? CELL_HOVER : CELL_FILL);
            int borderColor = today ? TODAY_BORDER : CELL_BORDER;
            int textColor   = today ? TEXT_TODAY   : TEXT_DAY;

            g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, fillColor);
            // 1px border
            g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, borderColor);
            g.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, borderColor);
            g.fill(cellX, cellY, cellX + 1, cellY + CELL_SIZE, borderColor);
            g.fill(cellX + CELL_SIZE - 1, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, borderColor);

            String num = Integer.toString(d);
            int textW = font.width(num);
            g.drawString(font, num,
                    cellX + (CELL_SIZE - textW) / 2,
                    cellY + (CELL_SIZE - font.lineHeight) / 2 + 1,
                    textColor, false);
        }
        g.disableScissor();
    }

    private int startDayOfWeek(CalendarClientStore.Snapshot snap, int displayYear, int monthIdx) {
        int dpw = Math.max(1, snap.daysPerWeek());
        int dpy = Math.max(1, snap.daysPerYear());
        long yearsElapsed = (long) displayYear - snap.epochYearOffset();
        long startOfYear = yearsElapsed * dpy;
        int daysBefore = 0;
        for (int i = 0; i < monthIdx && i < snap.months().size(); i++) {
            daysBefore += snap.months().get(i).days();
        }
        long startWorldDay = startOfYear + daysBefore;
        return (int) Math.floorMod(startWorldDay, (long) dpw);
    }

    private Component monthNameFor(CalendarClientStore.Snapshot snap, int monthIdxZeroBased) {
        if (monthIdxZeroBased < 0 || monthIdxZeroBased >= snap.months().size()) {
            return Component.literal("?");
        }
        return snap.months().get(monthIdxZeroBased).nameComponent();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
