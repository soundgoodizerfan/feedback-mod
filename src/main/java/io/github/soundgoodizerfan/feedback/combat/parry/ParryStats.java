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

import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
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
 * @param mass         the weapon's baked mass, Mu (for a wearer of a bracer, scaled by the bracer's joint share: the mass the parry rates the weapon against)
 * @param hand         which hand holds it
 * @param stack        the weapon
 * @param projectilesOnly a shield that deflects: it turns projectiles back and does not parry a melee blow
 * @param redirect     a turned projectile's speed over its incoming speed
 * @param knock        how hard a parried melee attacker is thrown back (0 for none)
 */
public record ParryStats(int window, int recovery, float coneHalfDeg, float mass, InteractionHand hand, ItemStack stack,
                         boolean projectilesOnly, float redirect, float knock) {

    /** The weapon {@code who} would parry with: the main hand's if it can, else the off hand's. */
    public static Optional<ParryStats> of(LivingEntity who) {
        Optional<ParryStats> main = of(who.getMainHandItem(), InteractionHand.MAIN_HAND);
        return (main.isPresent() ? main : of(who.getOffhandItem(), InteractionHand.OFF_HAND)).map(s -> s.worn(who));
    }

    /**
     * What the wearer's gear adds ({@link CombatGear}, already capped): whole ticks on the window,
     * and the bracer's share of the blow, which raises the mass the weapon's joint is rated against.
     * Recovery and cone are untouched, so gear widens the window and never makes a parry free.
     */
    private ParryStats worn(LivingEntity who) {
        CombatGear.Figures gear = CombatGear.of(who);
        return new ParryStats(window + gear.windowTicks(), recovery, coneHalfDeg, mass * (1f + gear.joint()), hand, stack,
                projectilesOnly, redirect, knock);
    }

    /**
     * A blade, mace or spear that is not broken parries; so does a shield whose trait says it does (data in
     * {@code modular_type/shield.json}): {@code parry} (the buckler: its {@code parry_ticks} replace the curve's
     * window, its {@code cone} the shield default, its {@code knock} throws the attacker back) or {@code deflect}
     * (the kite: projectiles only, turned back at {@code return_speed}). A bow, a tool, a gun and a shield with
     * neither trait do not.
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
        ModularTraits.Active trait = family == ModularType.Family.SHIELD ? ModularTraits.of(stack).orElse(null) : null;
        boolean parries = trait != null && trait.id().equals("parry"), deflects = trait != null && trait.id().equals("deflect");
        if (family == ModularType.Family.SHIELD && !parries && !deflects)
            return Optional.empty();
        float mass = figures.get(Fig.MASS);
        float heaviness = ParryTuning.heaviness(mass, figures.get(Fig.REACH));
        float cone = family == ModularType.Family.SHIELD ? ParryTuning.CONE_SHIELD_DEG
                : ParryTuning.coneHalfDegrees(figures.get(Fig.BALANCE));
        int window = ParryTuning.window(heaviness);
        float redirect = ParryTuning.REDIRECT_SPEED, knock = 0;
        if (parries) {
            window = Math.round(trait.params().get("parry_ticks", window));
            cone = trait.params().get("cone", cone * 2) / 2;
            knock = trait.params().get("knock", 0f);
        } else if (deflects)
            redirect = trait.params().get("return_speed", 0.8f);
        return Optional.of(new ParryStats(window, ParryTuning.recovery(heaviness), cone, mass, hand, stack, deflects, redirect, knock));
    }
}
