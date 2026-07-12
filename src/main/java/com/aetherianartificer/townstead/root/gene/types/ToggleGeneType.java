package com.aetherianartificer.townstead.root.gene.types;

import com.aetherianartificer.townstead.root.gene.GeneDisplay;
import com.aetherianartificer.townstead.root.gene.GeneInstance;
import com.aetherianartificer.townstead.root.gene.GeneType;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * A standalone on/off toggle bound to an Root Ability key (Apoli's {@code toggle}). It
 * does nothing itself; other genes gate on it with a {@code toggled} condition (Apoli's
 * {@code power_active}). State lives in {@code AbilityToggles}, flipped by the key the same
 * way a toggle-mode ability is. {@code slot} (1-8) claims a key; 0 auto-assigns.
 *
 * <p>{@code ai_trigger} (default {@code never}) lets villagers manage the toggle
 * themselves: the AI holds it ON while the trigger condition is true and releases it
 * when it stops — a mender switching on when a neighbour is hurt.</p>
 *
 * <p>JSON: {@code { "type":"pheno:toggle", "slot":1, "ai_trigger":"when_hurt_nearby" }}</p>
 */
public final class ToggleGeneType implements GeneType {

    public static final String KEY = "pheno:toggle";

    public record Instance(int slot, ActiveAbilityGeneType.AiTrigger aiTrigger) implements GeneInstance {
        @Override public String typeKey() { return KEY; }
        @Override public GeneDisplay display() { return GeneDisplay.PRESENCE; }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public GeneInstance parse(JsonObject json) {
        return new Instance(GsonHelper.getAsInt(json, "slot", 0),
                ActiveAbilityGeneType.AiTrigger.byKey(GsonHelper.getAsString(json, "ai_trigger", "never")));
    }
}
