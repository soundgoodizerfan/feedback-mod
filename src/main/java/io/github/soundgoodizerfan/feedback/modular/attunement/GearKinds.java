// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a creature wears for the worn-kind readings: its armour and, for a player, the pack on its back. A pack's shell and
 * hard liner carry the armour kinds of a plate and a lining (the attunement rows say which), so every effect that reads worn
 * parts reads the pack through here.
 */
public final class GearKinds {

    private GearKinds() {
    }

    /** Armour pieces and the worn pack. Allocates; call gated. */
    public static List<ItemStack> worn(LivingEntity wearer) {
        List<ItemStack> out = new ArrayList<>(5);
        wearer.getArmorSlots().forEach(out::add);
        if (wearer instanceof Player p) {
            ItemStack pack = ModularBackpack.wornStack(p);
            if (!pack.isEmpty())
                out.add(pack);
        }
        return out;
    }

    /** Share of a pack's carry rating its strap parts leave: clamp holds more, slip lets the load ride and carries less. */
    private static final float CARRY_PER_LEVEL = 0.08f;
    /** Share of misuse wear one level of slip turns into slipping instead. */
    private static final float SLIP_WEAR_PER_LEVEL = 0.5f;

    public static float carryShare(ItemStack bag) {
        return Math.max(0.3f, 1f + CARRY_PER_LEVEL * (TideKinds.level(bag, "clamp") - TideKinds.level(bag, "slip")));
    }

    /** The wear a pack takes from {@code amount} of misuse: soak turns whole points away, slip turns a share into slipping. */
    public static int packWear(ItemStack bag, int amount) {
        float share = 1f / (1f + SLIP_WEAR_PER_LEVEL * TideKinds.level(bag, "slip"));
        float want = WardKinds.soak(bag, amount) * share;
        int whole = (int) want;
        return java.util.concurrent.ThreadLocalRandom.current().nextFloat() < want - whole ? whole + 1 : whole;
    }
}
