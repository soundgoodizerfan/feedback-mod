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
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Shadow Step and its Ambush rider. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ShadowStep {

    private ShadowStep() {
    }

    /** The game tick until which the first blow after a Shadow Step is an Ambush. */
    private static final java.util.Map<java.util.UUID, Long> AMBUSH = new java.util.HashMap<>();

    /** Vanish from where you stand and step in behind the nearest thing in front: its own back is the way in. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 9, 0.5);
        if (t == null)
            return false;
        Vec3 facing = t.getLookAngle().multiply(1, 0, 1);
        facing = facing.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, t.getYRot()) : facing.normalize();
        Vec3 behind = t.position().subtract(facing.scale(1.3));
        var box = p.getBoundingBox().move(behind.subtract(p.position()));
        if (!p.level().noCollision(p, box))
            return false;
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position().add(0, 1, 0), arrive = behind.add(0, 1, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 14, 0.3, 0.5, 0.3, 0.02);
        CastFx.spray(level, ParticleTypes.PORTAL, from, 40, 1.2);
        CastFx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.5f);
        p.teleportTo(behind.x, behind.y, behind.z);
        Vec3 to = t.position().subtract(behind);
        p.setYRot((float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG));
        level.sendParticles(ParticleTypes.LARGE_SMOKE, arrive.x, arrive.y, arrive.z, 14, 0.3, 0.5, 0.3, 0.02);
        // the streak between the two points, and the cut at the end of it
        CastFx.line(level, CastFx.fade(CastFx.SHADOW, 0x000000, 1.6f), from, arrive, 0.25);
        CastFx.line(level, ParticleTypes.SMOKE, from, arrive, 0.6);
        CastFx.spray(level, ParticleTypes.REVERSE_PORTAL, arrive, 30, 0.8);
        Vec3 chest = t.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, chest.x, chest.y, chest.z, 1, 0, 0, 0, 0);
        CastFx.spray(level, CastFx.dust(CastFx.SHADOW, 1.2f), chest, 20, 0.3);
        CastFx.sound(level, arrive, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.8f);
        CastFx.sound(level, arrive, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
        AMBUSH.put(p.getUUID(), p.level().getGameTime() + 60);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.6f);
        return true;
    }

    /** Ambush (rider): the first blow within three seconds of a Shadow step staggers its target, whatever it faces. */
    @SubscribeEvent
    public static void onAmbush(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        var src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || event.getNewDamage() <= 0f
                || !Skills.unlocked(p, "ambush"))
            return;
        Long until = AMBUSH.remove(p.getUUID());
        if (until == null || p.level().getGameTime() > until)
            return;
        stagger(p, event.getEntity(), event.getNewDamage() * ParryTuning.JOULES_PER_DAMAGE * 3f);
    }
}
