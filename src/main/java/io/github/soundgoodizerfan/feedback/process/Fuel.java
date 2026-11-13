// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.world.item.ItemStack;

/**
 * How hot one item burns in a fire, and for how long -- read off what it is made of, never off
 * what it is.
 *
 * <h2>There is no fuel table</h2>
 * Fuel used to be its own datapack table, {@code fuel/*.json}, keyed by an item ingredient. That
 * was an identity lookup wearing a datapack, and it stated a second time what the material table
 * already knew: coal is coal whether it is in a firebox or a chamber. The figures now live in the
 * material's {@link Material.Combustion} block, and an item burns because its material does
 * A pack that wants coke to burn hotter still writes one
 * line of JSON, now in coke's material file.
 *
 * <h2>Nothing burns unless its material says so</h2>
 * There is deliberately no fallback to vanilla's furnace burn times: a vanilla burn time counts
 * items smelted, a fact about a furnace that carries no temperature, and a fire whose temperature
 * was guessed would make every hard gate in beat 2 negotiable.
 *
 * @param duration    ticks this one item lasts.
 * @param temperature flame temperature, in Tu: a hard ceiling on anything it heats.
 * @param spread      how much the flame varies, as a fraction, rolled once per item and held for
 *                    its whole burn, so it is learnable in aggregate rather than jittery.
 */
public record Fuel(int duration, Tu temperature, float spread, boolean poor) {

    /** What an oxygen-poor fuel does with no oxidiser: cool and short. Any other fuel is unchanged. */
    public Fuel smouldering() {
        if (!poor)
            return this;
        return new Fuel(Math.max(1, Math.round(duration * io.github.soundgoodizerfan.feedback.process.ProcessTuning.SMOULDER_DURATION)),
                new Tu(temperature.value() * io.github.soundgoodizerfan.feedback.process.ProcessTuning.SMOULDER_TEMPERATURE), spread, true);
    }

    /** What {@code stack} does in a fire fed only air: an oxygen-poor fuel smoulders. */
    public static Optional<Fuel> findInAir(ItemStack stack) {
        return find(stack).map(Fuel::smouldering);
    }

    /** What {@code stack} does in a fire, by the server's material table. */
    public static Optional<Fuel> find(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();
        return Material.find(MaterialTable.get().materials(), stack.getItem()).flatMap(Fuel::of);
    }

    /** Burns by the material's combustion, scaled by how much stuff the form is. A melt (a lava bucket) has none: it is hot, not fuel. */
    static Optional<Fuel> of(Material.Located at) {
        Material m = at.material();
        return m.combustion().flatMap(c -> c.flameTemperature().flatMap(t -> c.burnTicks().flatMap(n -> {
            int ticks = Math.round(n * pieces(at.form()));
            return ticks > 0 ? Optional.of(new Fuel(ticks, t, c.spread(), c.oxygenBalance() < 0f)) : Optional.empty();
        })));
    }

    /**
     * How many pieces of the base form one item of {@code form} is. A block is nine (its crafting
     * recipe); a plank a quarter of a log and a stick half a plank (theirs). An ore is mostly
     * rock and does not burn as itself.
     */
    static float pieces(String form) {
        if (form.equals("block"))
            return 9f;
        if (form.endsWith("planks"))
            return 0.25f;
        if (form.equals("stick"))
            return 0.125f;
        if (form.equals("ore") || form.endsWith("_ore"))
            return 0f;
        return 1f;
    }
}
