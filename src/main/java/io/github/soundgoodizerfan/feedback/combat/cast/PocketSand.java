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

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Pocket Sand: a handful of whatever is underfoot, thrown at the eyes. */
final class PocketSand {

    private PocketSand() {
    }

    /** Pocket sand: a fistful of the ground in your face's direction blinds a cone; loose sand blinds longest, dirt less, and bare stone gives you nothing to throw. */
    static boolean act(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        BlockState ground = lv.getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.5, p.getZ()));
        int blind = ground.is(BlockTags.SAND) ? 120 : ground.is(BlockTags.DIRT) ? 60
                : Skills.unlocked(p, "grit") && ground.is(BlockTags.BASE_STONE_OVERWORLD) ? 40 : 0;
        if (blind == 0)
            return false;
        if (Skills.unlocked(p, "grit"))
            blind += 40;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity e : near(p, eye.add(look.scale(2)), 3.2)) {
            if (e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.5)
                continue;
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blind));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            if (e instanceof Mob m)
                m.setTarget(null);
            stagger(p, e, 40f);
        }
        Vec3 hand = eye.add(look.scale(0.6)).add(0, -0.4, 0);
        for (int i = 0; i < 40; i++) {
            Vec3 d = look.add((lv.random.nextDouble() - 0.5) * 0.7, (lv.random.nextDouble() - 0.3) * 0.5, (lv.random.nextDouble() - 0.5) * 0.7).normalize();
            lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), hand.x, hand.y, hand.z, 0, d.x, d.y, d.z, 0.5 + lv.random.nextDouble() * 0.4);
        }
        CastFx.sound(lv, hand, SoundEvents.SAND_BREAK, 1f, 0.8f);
        return true;
    }
}
