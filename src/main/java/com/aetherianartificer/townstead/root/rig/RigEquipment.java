package com.aetherianartificer.townstead.root.rig;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Enforces a rig's {@link RigDefinition#disabledSlots() disabled equipment slots}: a body that can't wear
 * or hold gear in a slot (an egg wears nothing). Server backstop on equipment change, alongside the scarf
 * gate, anything placed in a disabled slot is stripped back out, returned to a player (with a message) or
 * dropped. The slot then stays empty, so nothing renders there either. No-op for the common case where the
 * slot isn't disabled, so a normal humanoid rig is untouched.
 */
public final class RigEquipment {

    /** Shown when a rig refuses gear in a slot. */
    public static final String DENY_KEY = "message.townstead.equip_slot_unavailable";

    private RigEquipment() {}

    /** Strips gear placed in a slot this entity's rig disables. Returns true if it removed something. */
    public static boolean enforce(LivingEntity entity, EquipmentSlot slot, ItemStack to) {
        if (entity.level().isClientSide || to.isEmpty()) return false;
        RigDefinition def = ServerRig.defFor(entity);
        if (def == null || !def.disabledSlots().contains(slot)) return false;

        // Setting the slot empty re-fires the change with an empty stack, which the to.isEmpty() guard skips.
        entity.setItemSlot(slot, ItemStack.EMPTY);
        ItemStack returned = to.copy();
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable(DENY_KEY), true);
            if (!player.addItem(returned)) player.drop(returned, false);
        } else {
            entity.spawnAtLocation(returned);
        }
        return true;
    }
}
