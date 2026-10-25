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

import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Forge Slam: a hot held item slammed into the ground. */
final class ForgeSlam {

    private ForgeSlam() {
    }

    /** Slams a hot held item into the ground: a ring of searing around the feet, and the item gives up its heat to it. */
    static boolean act(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty() || held.getItem() instanceof GunItem || !ItemHeat.isHot(held, p.level()))
            return false;
        Tu tu = ItemHeat.get(held, p.level());
        ServerLevel level = p.serverLevel();
        int burned = 0;
        for (LivingEntity e : near(p, p.position(), 3.5)) {
            e.invulnerableTime = 0;
            if (HotContact.burn(e, tu))
                burned++;
        }
        float lost = 150f + burned * 40f; // the more it sears, the more the iron gives up
        ItemHeat.set(held, new Tu(Math.max(AmbientTable.at(p.level()).value(), tu.value() - lost)), p.level());
        level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 0.2, p.getZ(), 20, 1.5, 0.1, 1.5, 0);
        level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.1, p.getZ(), 40, 1.8, 0.05, 1.8, 0.05);
        // heat reads in the colour: a molten ring spreading out, cracks glowing in the floor, sparks and smoke rising
        Vec3 land = p.position().add(0, 0.1, 0);
        double heat = Math.min(1.0, tu.value() / 1200.0);
        CastFx.shockwave(p, land, 3.5, 5, CastFx.fade(0xFFE08A, CastFx.EMBER, 1.6f), 0);
        CastFx.shockwave(p, land, 3.5, 5, ParticleTypes.FLAME, 0.18);
        CastFx.spokes(level, CastFx.fade(0xFFE08A, 0x802000, 1.2f), land, 0.4, 3.5, 8, 0.2);
        CastFx.spray(level, ParticleTypes.LAVA, land.add(0, 0.3, 0), 10 + (int) (20 * heat), 0.35);
        CastFx.spray(level, ParticleTypes.ELECTRIC_SPARK, land.add(0, 0.3, 0), 20, 0.7);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, land.x, land.y + 0.5, land.z, 10, 0.5, 0.3, 0.5, 0.05);
        CastFx.smash(level, land, 750, false);
        CastFx.sound(level, land, SoundEvents.LAVA_EXTINGUISH, 0.8f, 0.7f);
        CastFx.sound(level, land, SoundEvents.BLAZE_SHOOT, 0.6f, 0.6f);
        level.playSound(null, p.blockPosition(), FSounds.FEEL_HEAVY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.5f);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "one_handed_slam_instant", CastFx.MAIN, 14, 4);
        return true;
    }
}
