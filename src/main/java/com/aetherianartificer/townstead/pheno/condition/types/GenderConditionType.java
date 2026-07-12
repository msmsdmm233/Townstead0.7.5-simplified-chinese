package com.aetherianartificer.townstead.pheno.condition.types;

import com.aetherianartificer.townstead.pheno.condition.Condition;
import com.aetherianartificer.townstead.pheno.condition.ConditionContext;
import com.aetherianartificer.townstead.pheno.condition.ConditionType;
import com.google.gson.JsonObject;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.util.GsonHelper;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * {@code pheno:gender} — the bearer's MCA gender ({@code is}: one name or a list of
 * {@code male}/{@code female}/{@code neutral}; {@code masculine}/{@code feminine} are
 * accepted aliases). Villagers read the entity-tracked genetics gender; players read
 * MCA's player data (PlayerSaveData on the server, the synced playerData shadow on the
 * client), so gates evaluate identically on both sides. Entities with no MCA gender
 * (mobs, players without MCA data) never match — gate with {@code pheno:not} around
 * this when unknown bearers should keep the attachment.
 */
public final class GenderConditionType implements ConditionType {

    @Override
    public String key() {
        return "pheno:gender";
    }

    @Override
    public Condition parse(JsonObject json) {
        Set<String> names = new HashSet<>();
        if (json.has("is")) {
            if (json.get("is").isJsonArray()) {
                for (var element : json.getAsJsonArray("is")) {
                    names.add(canonical(element.getAsString()));
                }
            } else {
                names.add(canonical(GsonHelper.getAsString(json, "is", "")));
            }
        }
        if (names.isEmpty()) return null;
        return ctx -> test(ctx, names);
    }

    private static String canonical(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "masculine" -> "male";
            case "feminine" -> "female";
            default -> raw.toLowerCase(Locale.ROOT);
        };
    }

    private static boolean test(ConditionContext ctx, Set<String> names) {
        Gender gender = genderOf(ctx);
        return gender != null && gender != Gender.UNASSIGNED
                && names.contains(gender.name().toLowerCase(Locale.ROOT));
    }

    private static Gender genderOf(ConditionContext ctx) {
        if (ctx.entity() instanceof VillagerEntityMCA villager) {
            return villager.getGenetics().getGender();
        }
        if (ctx.entity() instanceof net.minecraft.world.entity.player.Player player) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                return net.conczin.mca.server.world.data.PlayerSaveData.get(serverPlayer).getGender();
            }
            if (player.level().isClientSide()) {
                return ClientPlayerGender.of(player);
            }
        }
        return null;
    }

    /** Client-only MCAClient access, isolated so dedicated servers never load it. */
    private static final class ClientPlayerGender {
        static Gender of(net.minecraft.world.entity.player.Player player) {
            var data = net.conczin.mca.MCAClient.playerData.get(player.getUUID());
            return data == null ? null : data.getGenetics().getGender();
        }
    }
}
