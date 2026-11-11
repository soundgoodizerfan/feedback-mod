// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.ModularItem;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.item.ItemStack;

/**
 * A gun in hand: a modular item of the gun type like a pick or a sword, so its name, parts, adjectives, traits and the Shift glosses are the core's. It carries a
 * {@link io.github.soundgoodizerfan.feedback.modular.ModularDesign} and the figures baked from it
 * ({@link GunFigures#of} is the shot logic's view of them); firing is driven from the client (prediction,
 * guns spec 7) and resolved on the server ({@link GunShots}), so the item itself does nothing on use.
 *
 * <p>A gun made at the Engineering Table arrives with its figures. The one that does not is the creative tab's
 * starter gun, baked by the core the first time it is ticked ({@link ModularStacks#tick}).
 */
public abstract class GunItem extends ModularItem implements io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.Cooling {

    public GunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** What differs between the families of gun: where a shot's energy and projectile come from. */
    public abstract GunMechanism mechanism();

    /** The mechanism of {@code stack}'s item if it is a gun, else null. */
    public static GunMechanism mechanismOf(ItemStack stack) {
        return stack.getItem() instanceof GunItem gun ? gun.mechanism() : null;
    }

    /** The stack's gun figures at the reference load, or null if it has none yet. */
    public static GunFigures figures(ItemStack stack) {
        return GunFigures.of(ModularStacks.figures(stack));
    }

    /** A gun cools at its own rate: its mass over the leak, from the figures it carries. */
    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.TuRate coolingRate(ItemStack stack) {
        GunFigures f = figures(stack);
        if (f == null)
            return io.github.soundgoodizerfan.feedback.core.FTuning.ITEM_COOLING_TU_PER_TICK;
        return new io.github.soundgoodizerfan.feedback.core.unit.TuRate(f.coolingRate().tuPerTick()
                * io.github.soundgoodizerfan.feedback.modular.attunement.HeatKinds.gunCooling(stack));
    }

    /** Every shot and reload rewrites the stack; only a different item or slot lowers the gun to raise it again. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** Whether {@code stack} is a gun made to be fired one-handed: the {@code off_hand} trait (a handgun's receiver). */
    public static boolean oneHanded(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem))
            return false;
        // A handgun's receiver says so; any other gun is one-handed if its baked mass and recoil allow it.
        GunFigures f = GunFigures.of(io.github.soundgoodizerfan.feedback.modular.ModularStacks.figures(stack));
        return GunSpec.of(stack).map(g -> g.has("off_hand")).orElse(false) || f != null && f.oneHandable();
    }

    /** A gun in each hand: each is fired one-handed, and {@link GunFigures#oneHanded} is what that costs. */
    public static boolean dualWielding(net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem().getItem() instanceof GunItem && player.getOffhandItem().getItem() instanceof GunItem;
    }

    /** Whether the off-hand gun fires on the use key: it is one-handed, and the main hand holds no gun or a one-handed one. */
    public static boolean offHandFires(net.minecraft.world.entity.player.Player player) {
        ItemStack main = player.getMainHandItem();
        return oneHanded(player.getOffhandItem()) && (!(main.getItem() instanceof GunItem) || oneHanded(main));
    }

    /** Whether the off hand holds a gun at all. */
    public static boolean hasOffHandGun(net.minecraft.world.entity.player.Player player) {
        return player.getOffhandItem().getItem() instanceof GunItem;
    }

    /** The fire mode the gun is set to, as far as its action can fire it (spec 12). */
    public static FireMode mode(ItemStack stack) {
        FireMode set = stack.getOrDefault(FWeapons.FIRE_MODE.get(), FireMode.AUTO);
        GunMechanism m = mechanismOf(stack);
        return m == null ? set : m.allow(stack, set);
    }

}
