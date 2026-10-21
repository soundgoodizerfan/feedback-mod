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
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The mace's smash, shared: the damage a fall is worth and the ring of shove, sound and dust it
 * leaves. The mace, sabatons and the sledge all land the same slam; only what sets it off differs.
 * Figures are vanilla {@code MaceItem}'s own, so the three never drift apart.
 */
public final class Smash {

    private Smash() {
    }

    /** A fall at or past this many blocks is heavy: louder, and shoves twice as hard (vanilla). */
    public static final float HEAVY_FALL = 5f;

    /** How far the vanilla ring reaches. */
    public static final float RADIUS = 3.5f;

    private static final float SHOVE = 0.7f;

    /** Damage a fall of {@code fall} blocks adds to a smash: 4 a block to 3, 2 a block to 8, then 1. */
    public static float fallDamage(float fall) {
        if (fall <= 3f)
            return 4f * fall;
        if (fall <= 8f)
            return 12f + 2f * (fall - 3f);
        return 22f + fall - 8f;
    }

    /**
     * The ring: dust at {@code ground}, a thud at {@code centre}, and everything alive within
     * {@code radius} of it (but not {@code source}, spectators, allies, the source's own tame pets
     * or marker stands) pushed outward and up, harder the nearer it stands. {@code power} scales
     * vanilla's shove; a heavy slam doubles it.
     */
    public static void shockwave(Level level, Entity source, Vec3 centre, BlockPos ground, float radius, float power, boolean heavy) {
        level.levelEvent(2013, ground, 750);
        SoundEvent sound = heavy ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
        level.playSound(null, centre.x, centre.y, centre.z, sound, source.getSoundSource(), 1f, 1f);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(centre, centre).inflate(radius),
                e -> struck(source, e, centre, radius))) {
            Vec3 away = e.position().subtract(centre);
            double push = (radius - away.length()) * SHOVE * power * (heavy ? 2 : 1) * (1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (push <= 0)
                continue;
            Vec3 shove = away.normalize().scale(push);
            e.push(shove.x, SHOVE, shove.z);
            if (e instanceof ServerPlayer p)
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
        }
    }

    private static boolean struck(Entity source, LivingEntity e, Vec3 centre, float radius) {
        if (e == source || e.isSpectator() || source.isAlliedTo(e) || e.position().distanceToSqr(centre) > radius * radius)
            return false;
        if (e instanceof TamableAnimal pet && pet.isTame() && source.getUUID().equals(pet.getOwnerUUID()))
            return false;
        return !(e instanceof ArmorStand stand && stand.isMarker());
    }
}
