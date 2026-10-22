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
package io.github.soundgoodizerfan.feedback.combat.state;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.roll.RollTuning;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The clock of {@link CombatState}, the only thing that ends a phase: counts {@code ticksLeft} down and
 * moves on (parry active, parry recovery, idle; roll, roll recovery, idle with the cooldown). Server
 * side, on any living entity that has a state; an entity never touched by combat has none and costs a
 * lookup. Stagger's slowdown is a modifier that lives exactly as long as the phase.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class CombatTicker {

    public static final ResourceLocation STAGGER_ID = Feedback.id("stagger");

    private CombatTicker() {
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity who) || who.level().isClientSide || !who.hasData(FCombat.STATE))
            return;
        CombatState s = CombatState.of(who);
        if (s.equals(CombatState.IDLE))
            return;
        int left = s.ticksLeft() - 1;
        int frames = Math.max(0, s.iFrames() - 1);
        CombatState next;
        if (left > 0)
            next = new CombatState(s.phase(), left, s.then(), frames);
        else
            next = switch (s.phase()) {
                case PARRY_ACTIVE -> new CombatState(CombatPhase.PARRY_RECOVERY, s.then(), 0, frames);
                case ROLL -> new CombatState(CombatPhase.ROLL_RECOVERY, s.then(), 0, frames);
                case ROLL_RECOVERY -> new CombatState(CombatPhase.IDLE, RollTuning.COOLDOWN, 0, frames);
                case PARRY_RECOVERY, STAGGER, IDLE -> new CombatState(CombatPhase.IDLE, 0, 0, frames);
            };
        if (s.phase() == CombatPhase.STAGGER && next.phase() != CombatPhase.STAGGER)
            who.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(STAGGER_ID);
        CombatState.set(who, next);
    }

    /** Knocks {@code who} off whatever it was doing for {@code ticks}, slowed by {@code slow} (a fraction of its speed lost). A rolling fighter is out of reach. */
    public static void stagger(LivingEntity who, int ticks, double slow) {
        CombatState s = CombatState.of(who);
        if (s.phase() == CombatPhase.ROLL || ticks <= 0)
            return;
        var speed = who.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(STAGGER_ID);
        speed.addTransientModifier(new AttributeModifier(STAGGER_ID, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        CombatState.set(who, new CombatState(CombatPhase.STAGGER, ticks, 0, s.iFrames()));
    }
}
