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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.parry;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * What one weapon makes of a parry: window, recovery and cone, read off its baked figures (no item ids;
 * the family decides who may parry). Pure, so the server, the debug command and a later client pose all
 * ask the same thing.
 *
 * @param window       ticks of the active window, before the ping pad
 * @param recovery     ticks of lockout after it
 * @param coneHalfDeg  half-angle of the frontal cone, degrees
 * @param mass         the weapon's baked mass, Mu
 * @param hand         which hand holds it
 * @param stack        the weapon
 */
public record ParryStats(int window, int recovery, float coneHalfDeg, float mass, InteractionHand hand, ItemStack stack) {

    /** The weapon {@code who} would parry with: the main hand's if it can, else the off hand's. */
    public static Optional<ParryStats> of(LivingEntity who) {
        Optional<ParryStats> main = of(who.getMainHandItem(), InteractionHand.MAIN_HAND);
        return (main.isPresent() ? main : of(who.getOffhandItem(), InteractionHand.OFF_HAND))
                .map(st -> io.github.soundgoodizerfan.feedback.combat.skill.Skills.tune(who, st));
    }

    /**
     * A blade, mace, spear or shield that is not broken parries. A bow, a tool and a gun do not.
     * (Where the buckler's {@code Parry} trait and the kite's {@code Deflect} fold in: a shield trait
     * would say "this shield parries" as data here, and its own window would replace this curve's.)
     */
    public static Optional<ParryStats> of(ItemStack stack, InteractionHand hand) {
        var design = ModularStacks.design(stack);
        var figures = ModularStacks.figures(stack);
        if (design == null || figures == null || ModularStacks.broken(stack))
            return Optional.empty();
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null)
            return Optional.empty();
        ModularType.Family family = type.family();
        if (family != ModularType.Family.BLADE && family != ModularType.Family.MACE
                && family != ModularType.Family.SPEAR && family != ModularType.Family.SHIELD)
            return Optional.empty();
        float mass = figures.get(Fig.MASS);
        float heaviness = ParryTuning.heaviness(mass, figures.get(Fig.REACH));
        float cone = family == ModularType.Family.SHIELD ? ParryTuning.CONE_SHIELD_DEG
                : ParryTuning.coneHalfDegrees(figures.get(Fig.BALANCE));
        return Optional.of(new ParryStats(ParryTuning.window(heaviness), ParryTuning.recovery(heaviness), cone, mass, hand, stack));
    }
}
