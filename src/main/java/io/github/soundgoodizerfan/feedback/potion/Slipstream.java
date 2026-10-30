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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Slipstream: while it lasts, starting a sprint gives a short burst of speed (a movement-speed modifier) with a dust wake behind; the burst
 * re-arms only after a rest off the sprint, so it is a burst and not steady speed (the Speed node is steady). When it runs out the same
 * stimulant is repaid as a stumble (vanilla slowness), longer for a stronger dose. Stamina is untouched.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Slipstream {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "slipstream");
    private static final String BURST = "fb_slip_burst", COOL = "fb_slip_cool";

    private Slipstream() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide)
            return;
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        MobEffectInstance e = p.getEffect(FEffects.SLIPSTREAM);
        var tag = p.getPersistentData();
        int burst = tag.getInt(BURST), cool = tag.getInt(COOL);
        if (e != null && p.isSprinting()) {
            if (burst == 0 && cool == 0) {
                burst = PotionTuning.SLIPSTREAM_BURST;
                cool = PotionTuning.SLIPSTREAM_REST;
            }
        } else {
            burst = 0; // a burst ends with the sprint
            if (cool > 0)
                cool--;
        }
        boolean on = burst > 0;
        if (on) {
            burst--;
            if (!speed.hasModifier(ID))
                speed.addTransientModifier(new AttributeModifier(ID,
                        PotionTuning.SLIPSTREAM_SPEED * (e.getAmplifier() + 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (p.tickCount % 2 == 0 && p.level() instanceof ServerLevel sl) {
                Vec3 back = p.getLookAngle().multiply(1, 0, 1).normalize().scale(-0.6);
                sl.sendParticles(ParticleTypes.CLOUD, p.getX() + back.x, p.getY() + 0.1, p.getZ() + back.z, 2, 0.15, 0.05, 0.15, 0.01);
            }
        } else if (speed.hasModifier(ID)) {
            speed.removeModifier(ID);
        }
        tag.putInt(BURST, burst);
        tag.putInt(COOL, cool);
    }

    /** The dose runs out: the stumble, longer for a stronger one. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e == null || !e.is(FEffects.SLIPSTREAM) || event.getEntity().level().isClientSide)
            return;
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                PotionTuning.SLIPSTREAM_STUMBLE * (e.getAmplifier() + 1), 0));
    }
}
