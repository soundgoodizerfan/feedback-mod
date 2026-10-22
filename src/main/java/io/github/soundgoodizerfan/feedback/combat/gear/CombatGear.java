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
package io.github.soundgoodizerfan.feedback.combat.gear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * The one place the combat mechanics ask what an entity wears (implementation plan 2.8): the summed,
 * capped figures of its gear curios, and the techniques its gear grants. The parry, the roll and the
 * stagger call this; none of them reads Curios.
 *
 * <p>Read per use, never per tick: a parry press, a roll start, a stagger. The inventory walk is
 * a handful of slots.
 */
public final class CombatGear {

    /** What worn gear adds, already capped by {@link GearTuning}. */
    public record Figures(float window, float joint, float rollDistance, float recoveryShave, float iFrames, float mass) {
        public static final Figures NONE = new Figures(0, 0, 0, 0, 0, 0);

        /** Whole ticks added to the parry window. */
        public int windowTicks() {
            return Math.round(window);
        }

        /** Whole ticks off the roll's recovery. */
        public int recoveryTicks() {
            return Math.round(recoveryShave);
        }

        /** Whole ticks added to the roll's i-frames. */
        public int iFrameTicks() {
            return Math.round(iFrames);
        }
    }

    private CombatGear() {
    }

    /** Sum of every gear curio {@code who} wears, each figure capped. */
    public static Figures of(LivingEntity who) {
        float window = 0, joint = 0, distance = 0, shave = 0, frames = 0, mass = 0;
        for (ItemStack stack : worn(who)) {
            GearStats g = stack.get(FDataComponents.GEAR.get());
            if (g == null)
                continue;
            window += g.window();
            joint += g.joint();
            distance += g.rollDistance();
            shave += g.recoveryShave();
            frames += g.iFrames();
            mass += g.mass();
        }
        return new Figures(Math.min(window, GearTuning.MAX_WINDOW), Math.min(joint, GearTuning.MAX_JOINT),
                Math.min(distance, GearTuning.MAX_ROLL_DISTANCE), Math.min(shave, GearTuning.MAX_RECOVERY_SHAVE),
                Math.min(frames, GearTuning.MAX_I_FRAMES), Math.min(mass, GearTuning.MAX_MASS));
    }

    /** Every technique id any worn curio grants (its {@code feedback:grants} component). Only the skill system to come reads it. */
    public static Set<ResourceLocation> grants(LivingEntity who) {
        Set<ResourceLocation> out = new HashSet<>();
        for (ItemStack stack : worn(who)) {
            List<ResourceLocation> g = stack.get(FDataComponents.GRANTS.get());
            if (g != null)
                out.addAll(g);
        }
        return out;
    }

    public static boolean grants(LivingEntity who, ResourceLocation technique) {
        return grants(who).contains(technique);
    }

    private static List<ItemStack> worn(LivingEntity who) {
        List<ItemStack> out = new ArrayList<>(4);
        CuriosApi.getCuriosInventory(who).ifPresent(inv ->
                inv.findCurios(s -> s.has(FDataComponents.GEAR.get()) || s.has(FDataComponents.GRANTS.get()))
                        .forEach(r -> out.add(r.stack())));
        return out;
    }
}
