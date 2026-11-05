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
package io.github.soundgoodizerfan.feedback.logistics.sort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularBackpack;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The one property reader. Belt modules, the tube loading station and any later separator ask it
 * what a stack is, by physical figure, resolved from the material table ({@code Material.find},
 * the path {@code Instruments} uses). It never reads an item id: a stack the table cannot place is
 * {@link Reading#unknown()}.
 * <p>
 * The material part of a reading is cached per item and dropped when the table reloads; heat and
 * mass are read fresh.
 */
public final class SortReader {

    private SortReader() {
    }

    private record Fixed(boolean unknown, boolean magnetic, float curie, float reflectivity, float density, float hardness,
                         float friction, Band kind, float unitMass, boolean brittle, boolean sticky) {
    }

    private static final Map<Item, Fixed> CACHE = new HashMap<>();
    private static int generation = -1;

    private static final Fixed NOTHING = new Fixed(true, false, Float.MAX_VALUE, 0.5f, 1f, 1f, 0.5f, Band.OTHER, 1f, false, false);

    /** Reads {@code stack} at the present moment (its carried heat is worked out against {@code level}). */
    public static Reading read(ItemStack stack, Level level) {
        if (generation != MaterialTable.generation()) {
            CACHE.clear();
            generation = MaterialTable.generation();
        }
        // A shaped part is told by its MadeOf, not its item, so it is never cached.
        Fixed f = stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem
                ? fixed(stack) : CACHE.computeIfAbsent(stack.getItem(), i -> fixed(new ItemStack(i)));
        float temp = ItemHeat.get(stack, level).value();
        return new Reading(f.unknown, f.magnetic, f.curie, f.reflectivity, f.density, f.hardness, f.friction, f.kind, temp,
                f.unitMass * stack.getCount(), f.unitMass, f.brittle, f.sticky);
    }

    /** The reading a material gives at room temperature, for a table that lists materials rather than stacks. */
    public static Reading of(Material m) {
        float hardness = m.hardness().value();
        return new Reading(false, m.separation().magnetic(), m.separation().curieTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE),
                m.separation().reflectivity().orElse(0.5f), m.density().muPerMb(), hardness, m.mechanical().friction().orElse(0.5f), kindBand(m),
                0f, 1f, 1f, false, m.traits().contains(Material.Trait.STICKY));
    }

    /** Mass in Mu of one item of {@code stack}'s kind. */
    public static float unitMass(ItemStack stack) {
        return ModularBackpack.massOf(stack.getItem());
    }

    private static Fixed fixed(ItemStack stack) {
        float mass = ModularBackpack.massOf(stack.getItem());
        Optional<Material.Located> found = Material.find(MaterialTable.get().materials(), stack);
        if (found.isEmpty())
            return new Fixed(true, false, Float.MAX_VALUE, 0.5f, 1f, 1f, 0.5f, Band.OTHER, mass, false, false);
        Material m = found.get().material();
        float hardness = m.hardness().value();
        boolean brittle = m.mechanical().toughness().map(t -> t.value() < LogisticsTuning.FRAGILE_TOUGHNESS_SHARE * hardness).orElse(false);
        return new Fixed(false, m.separation().magnetic(), m.separation().curieTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE),
                m.separation().reflectivity().orElse(0.5f), m.density().muPerMb(), hardness,
                m.mechanical().friction().orElse(0.5f), kindBand(m), mass, brittle, m.traits().contains(Material.Trait.STICKY));
    }

    static Band kindBand(Material m) {
        Material.Kind k = m.kind().orElse(null);
        if (k == null)
            return Band.OTHER;
        return switch (k) {
            case METAL, METALLOID -> Band.METALLIC;
            case MINERAL, CRYSTAL, CERAMIC, GLASS -> Band.STONY;
            case GRANULAR -> Band.GRANULAR;
            case POLYMER -> Band.POLYMER;
            case ORGANIC, SCULK, GRAIN, VEGETABLE, FRUIT, FUNGUS, ANIMAL, SEAFOOD, DOUGH, DISH, FLOWER, HERB -> Band.ORGANIC;
            default -> Band.OTHER;
        };
    }

    /** The band of {@code r} in the property of {@code like}, as the world holds it (no detector weakness). */
    public static Band bandOf(Reading r, SortProperty p) {
        return switch (p) {
            case MAGNETIC -> r.magnetic() && r.temperature() < r.curie() ? Band.PULLED : Band.IGNORED;
            case REFLECTIVITY -> r.reflectivity() < LogisticsTuning.REFLECT_DULL_BELOW ? Band.DULL
                    : r.reflectivity() >= LogisticsTuning.REFLECT_BRIGHT_FROM ? Band.BRIGHT : Band.MID_SHEEN;
            case DENSITY -> r.density() < LogisticsTuning.DENSITY_LIGHT_BELOW ? Band.LIGHT
                    : r.density() >= LogisticsTuning.DENSITY_HEAVY_FROM ? Band.HEAVY : Band.MID_DENSITY;
            case HARDNESS -> r.hardness() < LogisticsTuning.HARDNESS_SOFT_BELOW ? Band.SOFT
                    : r.hardness() >= LogisticsTuning.HARDNESS_HARD_FROM ? Band.HARD : Band.MID_HARD;
            case FRICTION -> r.friction() < LogisticsTuning.FRICTION_SLICK_BELOW ? Band.SLICK
                    : r.friction() >= LogisticsTuning.FRICTION_GRIPPY_FROM ? Band.GRIPPY : Band.MID_GRIP;
            case KIND -> r.kind();
            case TEMPERATURE -> r.temperature() >= LogisticsTuning.TEMP_HOT_FROM ? Band.HOT
                    : r.temperature() > io.github.soundgoodizerfan.feedback.core.FTuning.WARM_TU.value() ? Band.WARM : Band.COLD;
            case SIZE -> r.mass() < LogisticsTuning.SIZE_SMALL_BELOW ? Band.SMALL
                    : r.mass() >= LogisticsTuning.SIZE_LARGE_FROM ? Band.LARGE : Band.MID_SIZE;
        };
    }
}
