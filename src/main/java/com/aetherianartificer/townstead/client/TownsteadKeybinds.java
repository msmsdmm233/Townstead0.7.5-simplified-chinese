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
 * Townstead keybinds: the RPG dialogue key plus the pool of remappable "Root
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

    /** One Root Ability key per slot (mirrors {@code ActiveAbilities.POOL_SIZE}); default unbound. */
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
                com.aetherianartificer.townstead.root.ability.ActivateAbilityC2SPayload payload =
                        new com.aetherianartificer.townstead.root.ability.ActivateAbilityC2SPayload(slot);
                //? if neoforge {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
                //?} else {
                /*com.aetherianartificer.townstead.TownsteadNetwork.sendToServer(payload);
                *///?}
            }
        }

        // Gene-glide start: vanilla LocalPlayer only sends START_FALL_FLYING when the chest
        // slot holds an actual elytra — it never consults tryToStartFallFlying on the client —
        // so an elytra_flight bearer must send the command packet themself. The server
        // validates through PlayerElytraGeneMixin. Latched so one press sends one packet.
        townstead$tryGeneGlide(mc);

        // Observe (do not consume) the vanilla keys so press triggers can react without stealing the
        // key from movement. Edge-detected: a packet only on the press, not while held.
        boolean active = mc.player != null && mc.screen == null;
        for (int i = 0; i < PRESS_KEYS.length; i++) {
            KeyMapping mapping = pressKey(mc, PRESS_KEYS[i]);
            boolean down = active && mapping != null && mapping.isDown();
            if (down && !PRESS_PREV[i]) sendKeyPress(PRESS_KEYS[i]);
            PRESS_PREV[i] = down;
        }
    }

    private static boolean GLIDE_PREV_DOWN = false;

    private static void townstead$tryGeneGlide(Minecraft mc) {
        var player = mc.player;
        if (player == null) { GLIDE_PREV_DOWN = false; return; }
        boolean down = mc.screen == null && mc.options.keyJump.isDown();
        boolean freshPress = down && !GLIDE_PREV_DOWN;
        GLIDE_PREV_DOWN = down;
        // Deliberately stricter than vanilla's held-key check: a permanent glider would
        // deploy at the apex of every held-jump hop, so deploying takes the classic
        // double-tap — a fresh jump press while already airborne and falling.
        if (!freshPress) return;
        if (player.onGround() || player.isFallFlying() || player.isInWater()
                || player.getAbilities().flying || player.getDeltaMovement().y >= 0.0) {
            return;
        }
        if (!com.aetherianartificer.townstead.client.root.ClientAbilities.isActive(
                player, com.aetherianartificer.townstead.root.ability.Ability.ELYTRA_FLIGHT)) {
            return;
        }
        player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }

    /** Vanilla keys observable by a {@code press} trigger; those with their own server signal stay out. */
    private static final String[] PRESS_KEYS = {"jump", "sneak", "sprint"};
    private static final boolean[] PRESS_PREV = new boolean[PRESS_KEYS.length];

    private static KeyMapping pressKey(Minecraft mc, String name) {
        return switch (name) {
            case "jump" -> mc.options.keyJump;
            case "sneak" -> mc.options.keyShift;
            case "sprint" -> mc.options.keySprint;
            default -> null;
        };
    }

    private static void sendKeyPress(String key) {
        com.aetherianartificer.townstead.root.trigger.KeyPressC2SPayload payload =
                new com.aetherianartificer.townstead.root.trigger.KeyPressC2SPayload(key);
        //? if neoforge {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
        //?} else {
        /*com.aetherianartificer.townstead.TownsteadNetwork.sendToServer(payload);
        *///?}
    }
}
