package com.aetherianartificer.townstead.fatigue;

//? if neoforge {
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a row of lightning bolt sprites in item tooltips for energy-restoring items
 * (coffee, etc.). The energy need itself uses a battery icon; energy-*restoring* items
 * use the lightning to read as "gives energy".
 */
public class ClientEnergyTooltipComponent implements ClientTooltipComponent {

    private static final ResourceLocation LIGHTNING_ICON =
            ResourceLocation.fromNamespaceAndPath("townstead_icons", "lightning.png");
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 0;

    private final int amount;

    public ClientEnergyTooltipComponent(EnergyTooltipComponent data) {
        this.amount = data.amount();
    }

    @Override
    public int getHeight() {
        return ICON_SIZE + 2;
    }

    @Override
    public int getWidth(Font font) {
        return amount * (ICON_SIZE + ICON_SPACING);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int i = 0; i < amount; i++) {
            int iconX = x + i * (ICON_SIZE + ICON_SPACING);
            graphics.blit(LIGHTNING_ICON, iconX, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }
}
//?}
