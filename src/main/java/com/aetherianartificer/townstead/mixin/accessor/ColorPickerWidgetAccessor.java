package com.aetherianartificer.townstead.mixin.accessor;

import net.conczin.mca.client.gui.widget.ColorPickerWidget;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read/write MCA's {@code ColorPickerWidget#texture} (the gradient blitted behind the cursor).
 * The Body page's skin picker uses {@code mca:textures/colormap/villager_skin.png}; swapping that
 * field to a tinted copy makes the picker WYSIWYG with our origin skin tint. The field is
 * {@code private final}, hence {@link Mutable} on the setter.
 */
@Mixin(ColorPickerWidget.class)
public interface ColorPickerWidgetAccessor {
    @Accessor(value = "texture", remap = false)
    ResourceLocation townstead$getTexture();

    @Accessor(value = "texture", remap = false)
    @Mutable
    void townstead$setTexture(ResourceLocation texture);
}
