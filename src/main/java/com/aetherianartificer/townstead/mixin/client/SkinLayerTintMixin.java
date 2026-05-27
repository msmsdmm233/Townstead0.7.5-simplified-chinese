package com.aetherianartificer.townstead.mixin.client;

import com.aetherianartificer.townstead.client.skin.SkinBlend;
import com.aetherianartificer.townstead.client.skin.SkinTintRegistry;
import net.conczin.mca.client.render.layer.SkinLayer;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * Tints MCA's skin colour by a Townstead origin tint when one resolves for the
 * villager ({@link SkinTintRegistry}). MCA computes the exact vanilla skin from its
 * melanin×hemoglobin colour-map ({@code SkinLayer#getColor}); we <b>multiply</b> that
 * result by the origin tint (like a coloured lens), so the full vanilla gradient is
 * preserved and merely shifted toward the race's palette (dark elf grey, orc green,
 * …). A white tint is the identity, so vanilla skin stays pixel-exact. {@code getColor}
 * is MCA's own method ({@code remap = false}); its return type differs by version —
 * a packed ARGB {@code int} on NeoForge, a {@code float[]} r/g/b/a on Forge.
 */
@Mixin(SkinLayer.class)
public abstract class SkinLayerTintMixin<T extends LivingEntity, M extends HumanoidModel<T>> {

    //? if neoforge {
    @Inject(method = "getColor", remap = false, at = @At("RETURN"), cancellable = true, require = 1)
    private void townstead$tintSkin(T entity, float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof VillagerEntityMCA villager) {
            OptionalInt tint = SkinTintRegistry.resolve(villager);
            if (tint.isPresent()) {
                int base = cir.getReturnValue();
                int v = tint.getAsInt();
                int mode = (v >>> 24) & 0xFF;
                int r = SkinBlend.channel((base >> 16) & 0xFF, (v >> 16) & 0xFF, mode);
                int g = SkinBlend.channel((base >> 8) & 0xFF, (v >> 8) & 0xFF, mode);
                int b = SkinBlend.channel(base & 0xFF, v & 0xFF, mode);
                cir.setReturnValue((base & 0xFF000000) | (r << 16) | (g << 8) | b);
            }
        }
    }
    //?} else {
    /*@Inject(method = "getColor", remap = false, at = @At("RETURN"), cancellable = true, require = 1)
    private void townstead$tintSkin(T entity, float partialTick, CallbackInfoReturnable<float[]> cir) {
        if (entity instanceof VillagerEntityMCA villager) {
            OptionalInt tint = SkinTintRegistry.resolve(villager);
            if (tint.isPresent()) {
                float[] base = cir.getReturnValue();
                int v = tint.getAsInt();
                int mode = (v >>> 24) & 0xFF;
                cir.setReturnValue(new float[]{
                        SkinBlend.channel(Math.round(base[0] * 255f), (v >> 16) & 0xFF, mode) / 255f,
                        SkinBlend.channel(Math.round(base[1] * 255f), (v >> 8) & 0xFF, mode) / 255f,
                        SkinBlend.channel(Math.round(base[2] * 255f), v & 0xFF, mode) / 255f,
                        base.length > 3 ? base[3] : 1.0f
                });
            }
        }
    }
    *///?}
}
