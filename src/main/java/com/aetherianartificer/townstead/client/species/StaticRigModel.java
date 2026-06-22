package com.aetherianartificer.townstead.client.species;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * A custom-geometry rig body: a baked Bedrock {@code .geo.json} model with no built-in gait. Unlike the
 * vanilla-model generic rigs (spider, etc.), a custom model has no {@code setupAnim} to animate it, so this
 * is a static body — posed only by the rig's data poses ({@code applyRigPose}) and, later, the animation
 * bridge. {@link HierarchicalModel} provides {@code renderToBuffer} (it draws {@link #root()}); we only hold
 * the baked root and make {@code setupAnim} a no-op.
 */
public class StaticRigModel<T extends LivingEntity> extends HierarchicalModel<T> {

    private final ModelPart root;

    public StaticRigModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // No built-in animation; data poses and the bridge drive the bones.
    }
}
