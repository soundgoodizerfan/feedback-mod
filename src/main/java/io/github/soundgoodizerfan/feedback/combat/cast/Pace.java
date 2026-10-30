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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Observation's pace limb: Light Foot (a light-clad scout moves faster, by {@link BodyMass#armour}), Running Start
 * (a straight sprint builds speed), and Fleet (a short burst whose gain is set by how little is worn). All three
 * are one transient speed modifier, rewritten only when its rounded value changes.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Pace {

    private Pace() {
    }

    private static final ResourceLocation ID = Feedback.id("pace");

    /** [OPEN] stand-ins: armour Mu at which Light Foot is spent, Running Start's top gain, ramp ticks and the turn (degrees a tick) that breaks it. */
    private static final float LIGHT_CAP_MU = 40f, RUN_MAX = 0.25f;
    private static final int RUN_RAMP = 60;
    private static final float TURN_DEG = 12f;
    /** [OPEN] Fleet: top gain bare, the least gain in full plate (share of the top), the burst's ticks, and the armour Mu at which only the least is left. */
    private static final float FLEET_MAX = 0.6f, FLEET_LEAST = 0.2f, FLEET_CAP_MU = 50f;
    private static final int FLEET_TICKS = 50;

    private static final class State {
        float yaw, light, applied;
        boolean runner;
        int run;
        long fleetUntil;
        float fleetGain;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Fleet: a burst of speed for two and a half seconds; bare it is +60%, and every Mu of armour worn takes from the gain. */
    static boolean act(ServerPlayer p) {
        if (p.isPassenger() || p.isInWater())
            return false;
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.fleetGain = FLEET_MAX * Mth.clamp(1f - BodyMass.armour(p) / FLEET_CAP_MU, FLEET_LEAST, 1f);
        s.fleetUntil = p.level().getGameTime() + FLEET_TICKS;
        ServerLevel lv = p.serverLevel();
        CastFx.ring(lv, CastFx.dust(CastFx.STEEL, 1.1f), p.position().add(0, 0.2, 0), 0.8, 14, 0.2);
        CastFx.debris(lv, p.position(), 10, 0.4);
        CastFx.sound(lv, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
        CastFx.sound(lv, p.position(), FSounds.ROLL_WHOOSH.get(), 0.8f, 1.3f);
        return true;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        long now = p.level().getGameTime();
        if ((now + p.getId()) % 20 == 0) { // the skill lookups walk every tree: twice a second is plenty
            s.light = Skills.modifier(p, Figure.LIGHT_FOOT);
            s.runner = Skills.unlocked(p, "running_start");
        }
        float lightGain = s.light > 0f ? s.light * Mth.clamp(1f - BodyMass.armour(p) / LIGHT_CAP_MU, 0f, 1f) : 0f;
        if (s.runner && p.isSprinting() && !p.isPassenger()) {
            s.run = Math.abs(Mth.wrapDegrees(p.getYRot() - s.yaw)) > TURN_DEG ? 0 : Math.min(RUN_RAMP, s.run + 1);
        } else
            s.run = 0;
        s.yaw = p.getYRot();
        boolean fleet = now < s.fleetUntil;
        float want = Math.round((lightGain + RUN_MAX * s.run / RUN_RAMP + (fleet ? s.fleetGain : 0f)) * 100f) / 100f;
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (want != s.applied || speed.hasModifier(ID) != (want != 0f)) {
            speed.removeModifier(ID);
            if (want != 0f)
                speed.addTransientModifier(new AttributeModifier(ID, want, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            s.applied = want;
        }
        if (fleet && now % 2 == 0) {
            ServerLevel lv = p.serverLevel();
            lv.sendParticles(CastFx.dust(CastFx.STEEL, 0.9f), p.getX(), p.getY() + 0.15, p.getZ(), 3, 0.2, 0.05, 0.2, 0.0);
            if (p.onGround())
                lv.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.05, p.getZ(), 1, 0.15, 0.0, 0.15, 0.01);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
