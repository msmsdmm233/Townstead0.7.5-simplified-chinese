package com.aetherianartificer.townstead.mixin.client;

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
 * Overrides MCA's skin-layer colour with a Townstead skin tint when one resolves
 * for the villager ({@link SkinTintRegistry}). MCA multiplies a neutral base skin
 * texture by {@code SkinLayer#getColor}, so replacing that return value recolours
 * the skin to an arbitrary RGB (orc green, undead grey, …) without touching the
 * clothing/hair layers, which carry their own colour. {@code getColor} is MCA's
 * own method ({@code remap = false}); its return type differs by version —
 * a packed ARGB {@code int} on NeoForge, a {@code float[]} r/g/b/a on Forge.
 */
@Mixin(SkinLayer.class)
public abstract class SkinLayerTintMixin<T extends LivingEntity, M extends HumanoidModel<T>> {

    //? if neoforge {
    @Inject(method = "getColor", remap = false, at = @At("HEAD"), cancellable = true)
    private void townstead$tintSkin(T entity, float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof VillagerEntityMCA villager) {
            OptionalInt tint = SkinTintRegistry.resolve(villager);
            if (tint.isPresent()) cir.setReturnValue(0xFF000000 | (tint.getAsInt() & 0xFFFFFF));
        }
    }
    //?} else {
    /*@Inject(method = "getColor", remap = false, at = @At("HEAD"), cancellable = true)
    private void townstead$tintSkin(T entity, float partialTick, CallbackInfoReturnable<float[]> cir) {
        if (entity instanceof VillagerEntityMCA villager) {
            OptionalInt tint = SkinTintRegistry.resolve(villager);
            if (tint.isPresent()) {
                int rgb = tint.getAsInt();
                cir.setReturnValue(new float[]{
                        ((rgb >> 16) & 0xFF) / 255f,
                        ((rgb >> 8) & 0xFF) / 255f,
                        (rgb & 0xFF) / 255f,
                        1.0f
                });
            }
        }
    }
    *///?}
}
