package com.aetherianartificer.townstead.client;

import com.aetherianartificer.townstead.client.gui.dialogue.RpgDialogueScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.VillagerLike;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Townstead keybinds: the RPG dialogue key plus the pool of remappable "Origin
 * Ability" keys. The ability keys are real {@link KeyMapping}s (registered at
 * startup, default unbound), so they appear in the Controls screen and are picked
 * up by controller/VR rebinding layers; an active-ability gene binds to one by slot.
 */
public final class TownsteadKeybinds {
    public static final KeyMapping TALK = new KeyMapping(
            "townstead.key.talk",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "townstead.key.category"
    );

    /** One Origin Ability key per slot (mirrors {@code ActiveAbilities.POOL_SIZE}); default unbound. */
    public static final int ABILITY_KEYS = 8;
    public static final KeyMapping[] ABILITIES = new KeyMapping[ABILITY_KEYS];

    static {
        for (int i = 0; i < ABILITY_KEYS; i++) {
            ABILITIES[i] = new KeyMapping(
                    "townstead.key.ability" + (i + 1),
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    "townstead.key.category");
        }
    }

    private TownsteadKeybinds() {}

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        while (TALK.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            HitResult hit = mc.hitResult;
            if (hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                if (entity instanceof VillagerLike<?> villager) {
                    mc.setScreen(new RpgDialogueScreen(villager));
                }
            }
        }
        for (int i = 0; i < ABILITIES.length; i++) {
            int slot = i + 1;
            while (ABILITIES[i].consumeClick()) {
                if (mc.player == null || mc.screen != null) continue;
                com.aetherianartificer.townstead.origin.ability.ActivateAbilityC2SPayload payload =
                        new com.aetherianartificer.townstead.origin.ability.ActivateAbilityC2SPayload(slot);
                //? if neoforge {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
                //?} else {
                /*com.aetherianartificer.townstead.TownsteadNetwork.sendToServer(payload);
                *///?}
            }
        }
    }
}
