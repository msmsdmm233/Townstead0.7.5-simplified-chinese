package com.aetherianartificer.townstead.client.attachment;

import com.aetherianartificer.townstead.client.animation.AnimationTargetMap;
import com.aetherianartificer.townstead.client.root.RootCatalogClient;
import com.aetherianartificer.townstead.client.root.RootClientStore;
import com.aetherianartificer.townstead.client.species.RigSkinTone;
import com.aetherianartificer.townstead.root.GeneCatalogEntry;
import com.aetherianartificer.townstead.root.RootCatalogEntry;
import com.aetherianartificer.townstead.root.attachment.AttachmentDef;
import com.aetherianartificer.townstead.root.attachment.AttachmentPointDef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Renders an entity's expressed cosmetic attachments (tails, ears, tusks). Resolves
 * each {@code attachment} gene the individual expresses (per-entity from the synced
 * expressed set, falling back to the origin-typical grant list) to a synced
 * {@link AttachmentDef}, anchors its baked geometry to the named model bone via
 * {@link ModelPart#translateAndRotate} so it follows the live pose, then applies the
 * definition's offset/rotation/scale and tint. Geometry and texture come from
 * {@code AttachmentClient} (datapack-synced + cached), so no resource pack is needed.
 */
public class AttachmentRenderLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public AttachmentRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        List<Expressed> attachments = resolve(entity);
        if (attachments.isEmpty()) return;

        AnimationTargetMap<T> bones = AnimationTargetMap.forMcaModel(getParentModel());
        for (Expressed expressed : attachments) {
            AttachmentDef def = expressed.def();
            ModelPart geometry = AttachmentClient.geometry(def.geoSha1());
            ResourceLocation texture = AttachmentClient.texture(def.textureSha1());
            if (geometry == null || texture == null) continue;

            int tint = def.skinTint() ? RigSkinTone.forEntity(entity) & 0xFFFFFF : def.tint();
            float[] morph = morphScales(entity, expressed);
            // Morph with named bones scales each about its own pivot (an ear shrinks toward the
            // head surface it grows from); the geometry is shared across entities, so set and
            // restore around the draw. Without bones the whole attachment scales about its anchor.
            boolean morphBones = morph != null && !def.morphBones().isEmpty();
            if (morphBones) setBoneScale(geometry, def.morphBones(), morph[0], morph[1], morph[2]);

            // One attachment can fill several points (a tag matching both ears), so render an
            // instance at each resolved anchor.
            for (Anchor anchor : anchorsFor(def)) {
                ModelPart bone = bones.resolve(anchor.bone()).orElse(null);
                if (bone == null) continue;

                float[] base = anchor.offset();
                pose.pushPose();
                bone.translateAndRotate(pose);
                pose.translate((base[0] + def.offset()[0]) / 16f, (base[1] + def.offset()[1]) / 16f,
                        (base[2] + def.offset()[2]) / 16f);
                float[] rotation = def.rotation();
                if (rotation[2] != 0f) pose.mulPose(Axis.ZP.rotationDegrees(rotation[2]));
                if (rotation[1] != 0f) pose.mulPose(Axis.YP.rotationDegrees(rotation[1]));
                if (rotation[0] != 0f) pose.mulPose(Axis.XP.rotationDegrees(rotation[0]));
                if (def.scale() != 1f) pose.scale(def.scale(), def.scale(), def.scale());
                if (morph != null && !morphBones) pose.scale(morph[0], morph[1], morph[2]);

                VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
                renderPart(geometry, pose, buffer, light, tint);
                pose.popPose();
            }

            if (morphBones) setBoneScale(geometry, def.morphBones(), 1f, 1f, 1f);
        }
    }

    /**
     * The per-axis morph scales for this attachment on this entity: the size value
     * rolled on the allele of the gene that granted it, weighted per axis
     * ({@code 1 + (value - 1) * axis}). Null when there is nothing to morph.
     */
    private static float[] morphScales(LivingEntity entity, Expressed expressed) {
        float[] axes = expressed.def().morphAxes();
        if (axes == null) return null;
        String rolled = RootClientStore.resolveCarriedVariant(entity, expressed.geneId());
        if (rolled == null || rolled.isEmpty()) return null;
        float value;
        try {
            value = Float.parseFloat(rolled);
        } catch (NumberFormatException e) {
            return null;
        }
        if (value == 1f) return null;
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) out[i] = Math.max(0.05f, 1f + (value - 1f) * axes[i]);
        return out;
    }

    private static void setBoneScale(ModelPart root, List<String> boneNames, float sx, float sy, float sz) {
        for (String name : boneNames) {
            ModelPart bone;
            try {
                bone = root.getChild(name);
            } catch (RuntimeException e) {
                continue;
            }
            bone.xScale = sx;
            bone.yScale = sy;
            bone.zScale = sz;
        }
    }

    /** A resolved anchor: a model bone and the point's base offset (pixels). */
    private record Anchor(String bone, float[] offset) {}

    private static final float[] NO_OFFSET = {0, 0, 0};

    /** Tag (every matching point) -> explicit point -> direct bone. */
    private static List<Anchor> anchorsFor(AttachmentDef def) {
        if (def.targetTag() != null) {
            List<Anchor> out = new ArrayList<>();
            for (AttachmentPointDef point : AttachmentClient.pointsWithTag(def.targetTag())) {
                out.add(new Anchor(point.bone(), point.offset()));
            }
            return out;
        }
        if (def.targetPoint() != null) {
            AttachmentPointDef point = AttachmentClient.slot(def.targetPoint());
            if (point != null) return List.of(new Anchor(point.bone(), point.offset()));
        }
        return List.of(new Anchor(def.bone(), NO_OFFSET));
    }

    private static void renderPart(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, int tint) {
        int color = 0xFF000000 | tint;
        //? if neoforge {
        part.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        //?} else {
        /*float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        part.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, r, g, b, a);
        *///?}
    }

    /** An expressed attachment paired with the gene that granted it (the gene's allele carries the size roll). */
    private record Expressed(String geneId, AttachmentDef def) {}

    /** The attachment defs the entity expresses (per-entity set first, origin grant list as fallback). */
    private static List<Expressed> resolve(LivingEntity entity) {
        List<Expressed> out = new ArrayList<>();
        Set<String> expressed = RootClientStore.expressedGenes(entity);
        if (!expressed.isEmpty()) {
            for (String geneId : expressed) collect(geneId, out);
            return out;
        }
        String rootId = RootClientStore.resolve(entity);
        if (rootId.isEmpty()) return out;
        RootCatalogEntry origin = RootCatalogClient.origin(rootId);
        if (origin == null) return out;
        for (RootCatalogEntry.Inherited inherited : origin.inheritedGenes()) collect(inherited.geneId(), out);
        return out;
    }

    private static void collect(String geneId, List<Expressed> out) {
        GeneCatalogEntry gene = RootCatalogClient.gene(geneId);
        if (gene == null || !gene.isAttachment()) return;
        AttachmentDef def = AttachmentClient.def(gene.attachmentId());
        if (def != null) out.add(new Expressed(geneId, def));
    }
}
