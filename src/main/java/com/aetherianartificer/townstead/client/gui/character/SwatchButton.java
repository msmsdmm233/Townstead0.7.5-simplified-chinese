package com.aetherianartificer.townstead.client.gui.character;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A vanilla-framed button whose face is a solid colour swatch — a preset colour
 * choice in the character editor. Drawn as a fill rather than a text glyph, so the
 * swatch is one unbroken block (block-character labels leave the font's 1px gap
 * between glyphs down the middle).
 */
public class SwatchButton extends AbstractButton {

    private final int color;
    private final Runnable onPress;

    public SwatchButton(int x, int y, int width, int height, int color, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.color = color;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(graphics, mouseX, mouseY, delta);
        graphics.fill(getX() + 3, getY() + 3, getX() + getWidth() - 3, getY() + getHeight() - 3,
                0xFF000000 | color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
