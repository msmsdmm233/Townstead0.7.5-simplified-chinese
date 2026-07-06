package com.aetherianartificer.townstead.client.gui.character;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * A vertical scroll viewport for editor controls. Children are built at their
 * natural absolute positions and added here instead of to the screen; when the
 * content overflows the viewport, the mouse wheel shifts every child's real Y
 * (so hit-testing and dragging keep working untouched) and rendering is
 * scissored to the viewport, with a slim scrollbar on the right edge. A column
 * that fits renders identically to screen-level widgets.
 */
public class ControlColumn extends AbstractContainerEventHandler implements Renderable, NarratableEntry {

    private static final int WHEEL_STEP = 12;
    private static final int BAR_WIDTH = 6;

    private final int x, y, width, height;
    private final List<AbstractWidget> controls = new ArrayList<>();
    private int contentTop = Integer.MAX_VALUE;
    private int contentBottom = Integer.MIN_VALUE;
    private int scrolled;
    private boolean draggingBar;

    public ControlColumn(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Adds a control at its already-set position (content bounds grow to include it). */
    public void add(AbstractWidget widget) {
        controls.add(widget);
        contentTop = Math.min(contentTop, widget.getY());
        contentBottom = Math.max(contentBottom, widget.getY() + widget.getHeight());
    }

    private int contentHeight() {
        return controls.isEmpty() ? 0 : contentBottom - contentTop;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private void scrollTo(int target) {
        int clamped = Math.max(0, Math.min(maxScroll(), target));
        int shift = scrolled - clamped;
        if (shift == 0) return;
        for (AbstractWidget widget : controls) widget.setY(widget.getY() + shift);
        scrolled = clamped;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return controls;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.enableScissor(x, y, x + width, y + height);
        for (AbstractWidget widget : controls) {
            if (widget.getY() + widget.getHeight() >= y && widget.getY() <= y + height) {
                widget.render(graphics, mouseX, mouseY, delta);
            }
        }
        graphics.disableScissor();
        // The vanilla list-scrollbar look: black track, grey knob with a lighter face.
        int max = maxScroll();
        if (max > 0) {
            int barX = x + width - BAR_WIDTH;
            int knob = knobSize();
            int knobY = y + (height - knob) * scrolled / max;
            graphics.fill(barX, y, barX + BAR_WIDTH, y + height, 0xFF000000);
            graphics.fill(barX, knobY, barX + BAR_WIDTH, knobY + knob, 0xFF808080);
            graphics.fill(barX, knobY, barX + BAR_WIDTH - 1, knobY + knob - 1, 0xFFC0C0C0);
        }
    }

    private int knobSize() {
        return Math.max(8, Math.min(height, height * height / Math.max(1, contentHeight())));
    }

    private boolean inBar(double mouseX, double mouseY) {
        return maxScroll() > 0 && mouseX >= x + width - BAR_WIDTH && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    /** Centers the knob on the mouse and scrolls proportionally (vanilla list behavior). */
    private void dragBarTo(double mouseY) {
        int knob = knobSize();
        double denom = Math.max(1, height - knob);
        scrollTo((int) Math.round((mouseY - y - knob / 2.0) / denom * maxScroll()));
    }

    private boolean inViewport(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return inViewport(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!inViewport(mouseX, mouseY)) return false;
        if (button == 0 && inBar(mouseX, mouseY)) {
            draggingBar = true;
            dragBarTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar && button == 0) {
            dragBarTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    //? if neoforge {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!inViewport(mouseX, mouseY)) return false;
        scrollTo(scrolled - (int) Math.round(scrollY * WHEEL_STEP));
        return true;
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inViewport(mouseX, mouseY)) return false;
        scrollTo(scrolled - (int) Math.round(delta * WHEEL_STEP));
        return true;
    }
    *///?}

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }
}
