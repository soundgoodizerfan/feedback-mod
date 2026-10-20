/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
 * ({@code feedback_mechanics.md} 5.7). A pack that wants coke to burn hotter still writes one
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
public record Fuel(int duration, Tu temperature, float spread) {

    /** A bucket of a melt: lava is molten stone, a body at a fixed temperature, not a burn. */
    public static final String MOLTEN_BUCKET = "molten_bucket";

    /** What {@code stack} does in a fire, by the server's material table. */
    public static Optional<Fuel> find(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();
        return Material.find(MaterialTable.get().materials(), stack.getItem()).flatMap(Fuel::of);
    }

    /**
     * A bucket of a melt holds its melting point while it freezes (a fixed-temperature body, so
     * no spread); anything else burns by its material's combustion, scaled by how much stuff the
     * form is.
     */
    static Optional<Fuel> of(Material.Located at) {
        Material m = at.material();
        if (at.form().equals(MOLTEN_BUCKET))
            return m.thermal().meltingPoint().flatMap(t -> m.molten().freezeTicks().map(n -> new Fuel(n, t, 0f)));
        return m.combustion().flatMap(c -> c.flameTemperature().flatMap(t -> c.burnTicks().flatMap(n -> {
            int ticks = Math.round(n * pieces(at.form()));
            return ticks > 0 ? Optional.of(new Fuel(ticks, t, c.spread())) : Optional.empty();
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
