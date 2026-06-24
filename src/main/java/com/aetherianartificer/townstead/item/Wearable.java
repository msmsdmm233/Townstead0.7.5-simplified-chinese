package com.aetherianartificer.townstead.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * An item that renders a worn 3D model on the wearer (villager or player), in addition to its flat
 * inventory icon. {@code WornItemLayer} draws it: the {@link #wornGeo() geo} mesh, bound to the
 * {@link #wornTexture() texture} directly (no item atlas, so a real entity texture works), tinted by
 * {@link #wornColor}, anchored to the host {@link #anchorChannel() head or body} bone. That bone is
 * re-posed onto a non-humanoid rig's real head/back by {@code RigWearables}, so the wearable follows
 * any body; on a humanoid it uses {@link #humanoidSeat()}, and a rig may add a per-item delta via its
 * {@code wearables.<channel>.items.<item id>}.
 */
public interface Wearable {

    /** The Bedrock {@code .geo.json} resource for the worn mesh (e.g. {@code townstead:geo/scarf.geo.json}). */
    ResourceLocation wornGeo();

    /** The texture bound directly when drawing the worn mesh (e.g. {@code townstead:textures/entity/scarf.png}). */
    ResourceLocation wornTexture();

    /** The dye tint applied to the (greyscale) worn mesh, {@code 0xRRGGBB}. */
    int wornColor(ItemStack stack);

    /** Which host bone the mesh rides: {@code "head"} or {@code "body"}. */
    String anchorChannel();

    /** Default seat ({@code offset} in pixels, {@code rotation} in degrees) on a humanoid wearer. */
    float[][] humanoidSeat();

    /** Uniform scale of the worn mesh. */
    default float wornScale() {
        return 1f;
    }
}
