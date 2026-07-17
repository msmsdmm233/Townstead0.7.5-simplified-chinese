package com.aetherianartificer.townstead.mixin.accessor;

import net.conczin.mca.client.gui.VillagerEditorScreen;
import net.conczin.mca.entity.VillagerEntityMCA;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerEditorScreen.class)
public interface VillagerEditorScreenAccessor {
    /**
     * The editor's second preview entity, used for the preset-compare panel and the
     * clothing/hair/skin selection grids. {@code getVillager()} only exposes the main
     * preview entity, but both render with the editor's wall-clock preview time.
     */
    @Accessor(value = "villagerVisualization", remap = false)
    VillagerEntityMCA townstead$getVillagerVisualization();
}
