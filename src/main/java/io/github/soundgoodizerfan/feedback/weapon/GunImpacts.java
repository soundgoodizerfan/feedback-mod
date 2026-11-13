// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.net.GunHitMarkPayload;
import io.github.soundgoodizerfan.feedback.net.GunImpactPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What a bullet hit looks and sounds like, sent to the clients to dress ({@code client/gun/GunFx}). Purely visual and
 * audible: nothing here changes a hit. A body throws what it is made of (blood, bone dust, a golem's sparks), a block
 * what its surface is (chips, dust, a hole by its sound type), never an id.
 */
final class GunImpacts {

    private GunImpacts() {
    }

    /** A bullet in a living (or other) body: its stuff flies, the body answers, the shooter hears the tick. */
    static void body(ServerLevel level, LivingEntity shooter, Entity target, Vec3 at, Vec3 travel, float damage, boolean killed) {
        float power = Math.min(1f, damage / 12f);
        Vec3 dir = travel.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : travel.normalize();
        boolean metal = target instanceof IronGolem || (target instanceof LivingEntity l && l.getArmorValue() >= 8);
        byte kind;
        int colour = 0x8A0303;
        if (metal) {
            kind = GunImpactPayload.ARMOUR;
        } else if (target.getType().is(EntityTypeTags.SKELETONS)) {
            kind = GunImpactPayload.BONE;
        } else {
            kind = GunImpactPayload.FLESH;
            if (target instanceof Slime)
                colour = 0x4FBF4F;
            else if (target instanceof EnderMan)
                colour = 0x9A3FD0;
        }
        GunImpactPayload.send(level, kind, at, dir, power, colour, -1);
        level.playSound(null, at.x, at.y, at.z, (metal ? FSounds.GUN_HIT_ARMOUR : FSounds.GUN_HIT_FLESH).get(),
                SoundSource.NEUTRAL, 0.6f + 0.4f * power, 0.92f + level.random.nextFloat() * 0.16f);
        if (shooter instanceof ServerPlayer player && target instanceof LivingEntity && target != shooter)
            GunHitMarkPayload.send(player, killed);
    }

    /** A bullet stopped by a block face: chips of that block, dust, sparks off hard stone and metal, a hole where one can sit. */
    static void block(ServerLevel level, BlockPos at, Direction face, Vec3 p, float power) {
        BlockState state = level.getBlockState(at);
        GunImpactPayload.send(level, GunImpactPayload.BLOCK, p, Vec3.atLowerCornerOf(face.getNormal()), power,
                net.minecraft.world.level.block.Block.getId(state), decal(level, at, face, state));
    }

    /** A bullet glancing off: a streak of sparks along the reflected line. */
    static void ricochet(ServerLevel level, Vec3 p, Vec3 reflected) {
        GunImpactPayload.send(level, GunImpactPayload.RICOCHET, p, reflected.normalize(), 1f, 0, -1);
    }

    /** Flash, smoke and (for a gun that works its action) a case, at the muzzle. */
    static void muzzle(ServerLevel level, Vec3 muzzle, Vec3 aim, float report, boolean ejects) {
        GunImpactPayload.send(level, GunImpactPayload.MUZZLE, muzzle, aim, report, ejects ? 1 : 0, -1);
    }

    /** The hole a surface keeps, by its own character; none where nothing can sit (leaves, a part-face, a plant). */
    private static int decal(ServerLevel level, BlockPos at, Direction face, BlockState state) {
        if (state.is(BlockTags.LEAVES) || !state.isFaceSturdy(level, at, face))
            return -1;
        SoundType t = state.getSoundType();
        if (t == SoundType.GLASS || t == SoundType.AMETHYST || t == SoundType.AMETHYST_CLUSTER)
            return 2;
        if (t == SoundType.WOOD || t == SoundType.NETHER_WOOD || t == SoundType.BAMBOO_WOOD || t == SoundType.CHERRY_WOOD
                || t == SoundType.BAMBOO)
            return 1;
        if (t == SoundType.GRAVEL || t == SoundType.GRASS || t == SoundType.SAND || t == SoundType.SNOW || t == SoundType.WOOL
                || t == SoundType.MUD || t == SoundType.SOUL_SAND || t == SoundType.SOUL_SOIL || t == SoundType.MOSS
                || t == SoundType.ROOTED_DIRT)
            return 3;
        return 0;
    }
}
