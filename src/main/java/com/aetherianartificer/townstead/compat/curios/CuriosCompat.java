package com.aetherianartificer.townstead.compat.curios;

import com.aetherianartificer.townstead.Townstead;
import com.aetherianartificer.townstead.compat.ModCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reflection soft-dep bridge to the Curios API (no compile-time dependency). Lets the worn-cosmetic layer
 * render items held in an entity's Curios slots, and the equip gate police them, the same as the vanilla
 * armor slots. Curios is optional: when it's absent, or its API shifts under us, every call is a graceful
 * no-op (the bridge disables itself on the first failure, never crashes). Class/method names live only
 * here as strings; the version-specific {@code IItemHandler} type is resolved off the live handler instance
 * so no Forge/NeoForge class split is needed.
 */
public final class CuriosCompat {

    private static final boolean PRESENT = ModCompat.isLoaded("curios");
    private static boolean disabled;
    private static Method getCuriosInventory; // CuriosApi.getCuriosInventory(LivingEntity) -> Optional/LazyOptional
    private static Method orElse;             // (Optional|LazyOptional).orElse(Object)
    private static Method getCurios;          // ICuriosItemHandler.getCurios() -> Map<String, ICurioStacksHandler>
    private static Method getStacks;          // ICurioStacksHandler.getStacks() -> IItemHandlerModifiable
    private static Method getSlots;           // IItemHandler.getSlots() -> int
    private static Method getStackInSlot;     // IItemHandler.getStackInSlot(int) -> ItemStack
    private static Method setStackInSlot;     // IItemHandlerModifiable.setStackInSlot(int, ItemStack)

    private CuriosCompat() {}

    /** Whether Curios is installed (so callers can gate work that's pointless without it). */
    public static boolean present() {
        return PRESENT && !disabled;
    }

    /** Feeds every non-empty Curios-slot stack the entity wears to {@code out}. No-op without Curios. */
    public static void forEachWorn(LivingEntity entity, Consumer<ItemStack> out) {
        walk(entity, (handler, slot, stack) -> out.accept(stack));
    }

    /**
     * Server-side: strips every worn stack matching {@code test} out of its Curios slot, handing each
     * removed copy to {@code onRemoved} (to return it to the player, message, etc.). No-op without Curios.
     */
    public static void removeWhere(LivingEntity entity, Predicate<ItemStack> test, Consumer<ItemStack> onRemoved) {
        if (entity.level().isClientSide) return;
        walk(entity, (handler, slot, stack) -> {
            if (!test.test(stack)) return;
            ItemStack removed = stack.copy();
            setStackInSlot.invoke(handler, slot, ItemStack.EMPTY);
            onRemoved.accept(removed);
        });
    }

    @FunctionalInterface
    private interface SlotVisitor {
        void visit(Object stacksHandler, int slot, ItemStack stack) throws Exception;
    }

    private static void walk(LivingEntity entity, SlotVisitor visitor) {
        if (!PRESENT || disabled) return;
        try {
            Object inventory = handlerFor(entity);
            if (inventory == null) return;
            Map<?, ?> curios = (Map<?, ?>) getCurios.invoke(inventory);
            for (Object stacksHandler : curios.values()) {
                Object stacks = getStacks.invoke(stacksHandler);
                ensureStackMethods(stacks);
                int slots = (int) getSlots.invoke(stacks);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlot.invoke(stacks, i);
                    if (stack != null && !stack.isEmpty()) visitor.visit(stacks, i, stack);
                }
            }
        } catch (Throwable t) {
            disabled = true;
            Townstead.LOGGER.warn("Curios bridge disabled (API mismatch?); Curios-slot wearables won't render or be gated", t);
        }
    }

    private static Object handlerFor(LivingEntity entity) throws Exception {
        if (getCuriosInventory == null) resolveApi();
        Object optional = getCuriosInventory.invoke(null, entity);
        if (optional == null) return null;
        if (orElse == null) orElse = optional.getClass().getMethod("orElse", Object.class);
        return orElse.invoke(optional, (Object) null);
    }

    private static void resolveApi() throws Exception {
        Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
        getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
        getCurios = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                .getMethod("getCurios");
        getStacks = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler")
                .getMethod("getStacks");
    }

    private static void ensureStackMethods(Object stacks) throws Exception {
        if (getSlots != null) return;
        Class<?> c = stacks.getClass();
        getSlots = c.getMethod("getSlots");
        getStackInSlot = c.getMethod("getStackInSlot", int.class);
        setStackInSlot = c.getMethod("setStackInSlot", int.class, ItemStack.class);
    }
}
