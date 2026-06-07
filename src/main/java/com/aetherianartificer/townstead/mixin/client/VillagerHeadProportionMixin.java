package com.aetherianartificer.townstead.mixin.client;

import net.conczin.mca.client.model.VillagerEntityBaseModelMCA;
import net.conczin.mca.entity.VillagerLike;
import net.conczin.mca.entity.ai.Genetics;
import net.conczin.mca.entity.ai.Traits;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a villager's head undistorted when its body is scaled non-uniformly.
 *
 * <p>MCA renders the whole model with one scale {@code (width, height, width)}, applied to every
 * part including the head — so a villager that's wider than it is tall (any short, stocky build)
 * gets a flattened, squashed head. This <b>neutralizes</b> the body scale on the head part by
 * counter-scaling it: X/Z by {@code 1/horizontalScale} and Y by {@code 1/verticalScale}, so the
 * head renders at its natural, undistorted size. The body keeps the full stocky scale; only the
 * head is corrected — a normal head on a short, broad body, rather than a squashed (or tiny) one.</p>
 *
 * <p>Entirely derived from the entity's own genetics × traits scale factors — never from an origin,
 * trait, or gene: it's a no-op {@code (1,1,1)} when width equals height, and corrects in either
 * direction otherwise. Clamped so an extreme build can't balloon or vanish the head. Re-applied
 * every frame (the renderer reuses one model instance per layer, so a stale scale would leak to the
 * next villager) and propagated to the skin/hair/clothing layers, which copy this parent model's
 * part transforms — scale included — via {@code VillagerLayer.copyPropertiesTo}.</p>
 */
@Mixin(VillagerEntityBaseModelMCA.class)
public abstract class VillagerHeadProportionMixin<T extends LivingEntity & VillagerLike<T>> {

    //? if neoforge {
    @Inject(method = "setupAnim", remap = false, at = @At("TAIL"), require = 1)
    //?} else {
    /*@Inject(method = "m_6973_", remap = false, at = @At("TAIL"), require = 1)
    *///?}
    private void townstead$proportionHead(T entity, float limbAngle, float limbDistance,
            float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        Genetics genetics = entity.getGenetics();
        Traits traits = entity.getTraits();
        if (genetics == null || traits == null) return;
        // The genetics (size/width) × traits (e.g. dwarfism) factors are what MCA's renderer
        // scales the body by on both versions; gender/dimensions are skipped (minor, and absent
        // on the 1.20.1 entity API).
        float horiz = genetics.getHorizontalScaleFactor() * traits.getHorizontalScaleFactor();
        float vert = genetics.getVerticalScaleFactor() * traits.getVerticalScaleFactor();
        if (horiz <= 1.0e-4f || vert <= 1.0e-4f) return;
        // Counter the body scale on the head so its net scale is ~(1,1,1) — natural size, no squash.
        float xz = Math.max(0.5f, Math.min(2.0f, 1.0f / horiz));
        float y = Math.max(0.5f, Math.min(2.0f, 1.0f / vert));
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        model.head.xScale = xz;
        model.head.yScale = y;
        model.head.zScale = xz;
        model.hat.xScale = xz;
        model.hat.yScale = y;
        model.hat.zScale = xz;
    }
}
