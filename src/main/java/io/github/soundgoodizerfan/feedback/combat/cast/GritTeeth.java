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

package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Grit teeth (Proficiency): set the body and take it. For {@link #TICKS} every blow lands lighter, by a share read off
 * the armour worn ({@link BodyMass#armour}): bare skin shrugs off {@link #BASE}, full plate up to {@link #MAX}. The
 * price is the stance: you are rooted, slowed by {@link #ROOTED}, so it is for standing in a doorway, not for chasing.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class GritTeeth {

    /** STAND-IN: four seconds; damage share taken off bare and at most; Mu of armour per extra share point. */
    static final int TICKS = 80;
    static final float BASE = 0.15f, MAX = 0.55f, MU_PER_SHARE = 125f;
    /** STAND-IN: move speed lost while gritted. */
    static final double ROOTED = -0.3;

    private static final ResourceLocation ROOT = Feedback.id("grit_teeth");
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private GritTeeth() {
    }

    static boolean act(ServerPlayer p) {
        UNTIL.put(p.getUUID(), p.level().getGameTime() + TICKS);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(ROOT);
        speed.addTransientModifier(new AttributeModifier(ROOT, ROOTED, Operation.ADD_MULTIPLIED_TOTAL));
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 0.6f);
        CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.CRIMSON, 1.2f), p.position().add(0, 0.1, 0), 0.8, 14, 0f);
        return true;
    }

    static float share(ServerPlayer p) {
        return Math.min(MAX, BASE + BodyMass.armour(p) / MU_PER_SHARE);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && UNTIL.containsKey(p.getUUID())) {
            event.setAmount(event.getAmount() * (1 - share(p)));
            Vec3 at = p.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.3, 0.4, 0.3, 0.05);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Long until = UNTIL.get(p.getUUID());
        if (until != null && p.level().getGameTime() >= until) {
            UNTIL.remove(p.getUUID());
            p.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ROOT);
        }
    }
}
