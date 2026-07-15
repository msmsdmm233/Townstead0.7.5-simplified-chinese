package com.aetherianartificer.townstead.mixin.client;

import com.aetherianartificer.townstead.client.species.InvisFade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.mca.client.render.layer.VillagerLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fades MCA's own body layers (skin/face/clothing/hair — the default {@code mca:villager}
 * rig and the genetics player) through invisibility transitions instead of vanilla's hard
 * cut. {@code render} computes {@code visible = !isInvisible()} and threads it to
 * {@code renderFinal}; while {@link InvisFade} still has alpha we keep the body drawing,
 * swap the render type to MCA's translucent one so vertex alpha blends, and scale the
 * colour's alpha by the fade. All draw passes funnel through the private
 * {@code renderModel}, and none of the four layer subclasses override the hooked methods,
 * so the base-class hooks cover every pass. Alternate-rig entities never reach these
 * hooks ({@code VillagerBodyLayerSuppressMixin} cancels {@code render}); their fade lives
 * in {@code SpeciesRigLayer}.
 */
@Mixin(VillagerLayer.class)
public abstract class VillagerLayerFadeMixin<T extends LivingEntity> {

    /** The current pass's fade, stashed for renderModel which has no entity parameter. */
    @Unique
    private static final ThreadLocal<Float> townstead$fade = ThreadLocal.withInitial(() -> 1f);

    @ModifyVariable(method = "renderFinal", remap = false, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean townstead$fadeVisible(boolean visible, PoseStack pose, MultiBufferSource buffers,
                                          int light, LivingEntity entity, float partialTick,
                                          boolean visibleArg, boolean glowing) {
        float fade = InvisFade.alpha(entity, partialTick);
        townstead$fade.set(fade);
        return visible || fade > 0f;
    }

    @Inject(method = "getRenderLayer", remap = false, at = @At("HEAD"), cancellable = true)
    private void townstead$fadeRenderLayer(ResourceLocation texture, boolean visible, boolean translucent,
                                           boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        // The cutout render type ignores vertex alpha; MCA's own translucent choice blends it.
        // Glowing keeps its outline path so a glowing entity still outlines while fading.
        if (townstead$fade.get() < 1f && visible && !glowing) {
            cir.setReturnValue(RenderType.itemEntityTranslucentCull(texture));
        }
    }

    //? if neoforge {
    @ModifyVariable(method = "renderModel", remap = false, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int townstead$fadeColor(int color) {
        float fade = townstead$fade.get();
        if (fade >= 1f) return color;
        int alpha = (color >>> 24) & 0xFF;
        // A zero-alpha pass (MCA's overlay constant) is left exactly as MCA renders it today.
        if (alpha == 0) return color;
        return (Math.round(alpha * fade) << 24) | (color & 0xFFFFFF);
    }
    //?} else {
    /*@org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderModel", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;m_7695_(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7)
    private float townstead$fadeAlpha(float alpha) {
        float fade = townstead$fade.get();
        return fade >= 1f ? alpha : alpha * fade;
    }
    *///?}
}
