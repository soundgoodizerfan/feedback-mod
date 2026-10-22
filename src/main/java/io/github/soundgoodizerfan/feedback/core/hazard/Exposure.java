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
package io.github.soundgoodizerfan.feedback.core.hazard;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How much of a shock a victim takes: one multiplier on {@link Shock#damage}, read off the world.
 *
 * <h2>Not a body's resistance</h2>
 * Resistance is still not modelled (the {@link Shock} javadoc, philosophy 17). This is the
 * shock-side twin of {@link Protection}: three things anyone can see and change about a victim --
 * whether it is wet, what its armour is made of, and what it stands on -- each a property of a
 * material's file or of the weather, never of what the victim is. A dry, bare thing on stone
 * reads {@code 1.0}, which is the bare wire's old figure unchanged.
 *
 * <h2>One method per factor</h2>
 * Each factor is pure, so the arithmetic is checkable without a block; {@link #of} is the one
 * method that touches the world.
 */
public final class Exposure {

    private Exposure() {
    }

    /** What a body of some material does to a shock passing through it. */
    public enum Conduction { CONDUCTS, NEUTRAL, INSULATES }

    /** Wet raises it; dry leaves it. */
    public static float wet(boolean wet) {
        return wet ? FTuning.FENCE_WET_FACTOR : 1f;
    }

    /** Conductive armour slots raise it in proportion to their share of the four, insulating ones lower it. */
    public static float armour(int conductive, int insulating, int slots) {
        if (slots <= 0)
            return 1f;
        return 1f + FTuning.FENCE_ARMOUR_GAIN * conductive / slots - FTuning.FENCE_ARMOUR_LOSS * insulating / slots;
    }

    /** Off the ground it is less; on a conductive floor more, an insulating floor less. */
    public static float ground(boolean onGround, Conduction floor) {
        if (!onGround)
            return FTuning.FENCE_AIRBORNE_FACTOR;
        return switch (floor) {
            case CONDUCTS -> FTuning.FENCE_GROUND_CONDUCTIVE;
            case INSULATES -> FTuning.FENCE_GROUND_INSULATING;
            case NEUTRAL -> 1f;
        };
    }

    /** The factors multiplied, held between nothing at all and {@link FTuning#FENCE_EXPOSURE_MAX}. */
    public static float clamp(float product) {
        return Math.max(0f, Math.min(FTuning.FENCE_EXPOSURE_MAX, product));
    }

    /**
     * A figure for conductivity decides when the file has one; where nobody measured, a metal is
     * taken to conduct and an organic to insulate. A property again, never a name (the same call
     * as {@link Grip#insulates}).
     */
    public static Conduction conduction(Material material) {
        return material.electrical().conductivity()
                .map(c -> c >= FTuning.FENCE_CONDUCTIVE_MIN ? Conduction.CONDUCTS : Conduction.INSULATES)
                .orElseGet(() -> material.kind().map(k -> switch (k) {
                    case METAL -> Conduction.CONDUCTS;
                    case ORGANIC -> Conduction.INSULATES;
                    default -> Conduction.NEUTRAL;
                }).orElse(Conduction.NEUTRAL));
    }

    private static Conduction conduction(Map<ResourceLocation, Material> materials, Item item) {
        return Material.find(materials, item).map(l -> conduction(l.material())).orElse(Conduction.NEUTRAL);
    }

    /** The multiplier for {@code victim} right now. Server side: reads the server's material table. */
    public static float of(Entity victim) {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        int conductive = 0, insulating = 0;
        if (victim instanceof LivingEntity living)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                    continue;
                ItemStack worn = living.getItemBySlot(slot);
                if (worn.isEmpty())
                    continue;
                Conduction c = Material.find(materials, worn).map(l -> conduction(l.material())).orElse(Conduction.NEUTRAL);
                if (c == Conduction.CONDUCTS)
                    conductive++;
                else if (c == Conduction.INSULATES)
                    insulating++;
            }
        BlockState below = victim.level().getBlockState(victim.getBlockPosBelowThatAffectsMyMovement());
        Conduction floor = conduction(materials, below.getBlock().asItem());
        return clamp(wet(victim.isInWaterOrRain())
                * armour(conductive, insulating, 4)
                * ground(victim.onGround(), floor));
    }
}
