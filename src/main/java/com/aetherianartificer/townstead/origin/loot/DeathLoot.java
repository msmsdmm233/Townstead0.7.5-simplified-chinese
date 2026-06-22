package com.aetherianartificer.townstead.origin.loot;

import com.aetherianartificer.townstead.origin.LifeStage;
import com.aetherianartificer.townstead.origin.LifeStageProgression;
import com.aetherianartificer.townstead.origin.PlayerOrigin;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Drops a dying entity's per-origin, per-stage death loot IN ADDITION to its normal drops. Called from
 * the server-side {@code LivingDeathEvent} for both MCA villagers (origin + current life stage) and
 * players (origin; treated as the {@code adult} stage). No-op when the entity has no origin or no
 * death-loot table, so it costs nothing for everyone else.
 */
public final class DeathLoot {

    private DeathLoot() {}

    public static void onDeath(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || DeathLootRegistry.isEmpty()) return;
        String originStr = originIdOf(entity);
        if (originStr == null || originStr.isEmpty()) return;
        DeathLootDef def = DeathLootRegistry.get(ResourceLocation.tryParse(originStr));
        if (def == null) return;
        List<DeathLootDef.Drop> drops = def.dropsForStage(stageIdOf(entity));
        if (drops == null || drops.isEmpty()) return;

        RandomSource rng = entity.getRandom();
        for (DeathLootDef.Drop d : drops) {
            if (d.chance() < 1f && rng.nextFloat() >= d.chance()) continue;
            int count = d.min() + (d.max() > d.min() ? rng.nextInt(d.max() - d.min() + 1) : 0);
            if (count <= 0) continue;
            Item item = BuiltInRegistries.ITEM.get(d.item());
            if (item == Items.AIR) continue;
            ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    new ItemStack(item, count));
            drop.setDefaultPickUpDelay();
            entity.level().addFreshEntity(drop);
        }
    }

    private static String originIdOf(LivingEntity entity) {
        if (entity instanceof Player player) return PlayerOrigin.getOriginId(player);
        if (entity instanceof VillagerEntityMCA villager) return TownsteadVillagers.get(villager).life().originId();
        return null;
    }

    /** Villager → its current stage id; players (and unresolved villagers) → {@code adult} (falls back to default). */
    private static String stageIdOf(LivingEntity entity) {
        if (entity instanceof VillagerEntityMCA villager) {
            LifeStage stage = LifeStageProgression.currentStage(villager);
            if (stage != null) return stage.id();
        }
        return "adult";
    }
}
