package com.aetherianartificer.townstead.root.condition.types;

import com.aetherianartificer.townstead.data.DataPackLang;
import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionType;
import com.aetherianartificer.townstead.root.ability.AbilityToggles;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * True while a {@code toggle} gene is switched on (Apoli's {@code power_active}). Lets one
 * gene gate on another toggle gene's state, e.g. a phasing gene active only while a
 * "phantomize" toggle is on. Genetics-specific (reads {@code AbilityToggles}), so it lives
 * in {@code origin} rather than the shared layer.
 *
 * <p>JSON: {@code { "type":"pheno:toggled", "gene":"my_pack:imported/origins/phantomize" }}</p>
 */
public final class ToggledConditionType implements ConditionType {

    public static final String KEY = "pheno:toggled";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Condition parse(JsonObject json) {
        ResourceLocation geneId = DataPackLang.parseId(GsonHelper.getAsString(json, "gene", ""));
        if (geneId == null) return null;
        // Side-aware: the live map is server-side (read default-aware, so a default-on
        // toggle tests true untouched); clients (attachment gates, pose conditions) read
        // the synced per-entity toggle set, which already carries effective state.
        return ctx -> ctx.entity().level().isClientSide()
                ? ClientToggleState.isOn(ctx.entity(), geneId)
                : AbilityToggles.isOnEffective(ctx.entity(), geneId);
    }

    /** Client-only store access, isolated so dedicated servers never load it. */
    private static final class ClientToggleState {
        static boolean isOn(net.minecraft.world.entity.Entity entity, ResourceLocation geneId) {
            return com.aetherianartificer.townstead.client.root.RootClientStore.isToggled(
                    entity.getId(), geneId.toString());
        }
    }
}
