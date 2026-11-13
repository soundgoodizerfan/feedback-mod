// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.registry.FItems;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A steel capsule packed with graphite, made for one blast. Detonation synthesis: a shock of the right strength
 * squeezes the carbon into nanodiamond grit. Too weak a blast leaves the capsule whole to try again; too strong a
 * one vaporises the carbon and it comes back as graphite. The blast's radius is the only thing read, so a creeper,
 * TNT, a respawn anchor and a charge of the player's own design all work the same.
 */
public class ShockCapsuleBlock extends Block {

    public static final MapCodec<ShockCapsuleBlock> CODEC = simpleCodec(ShockCapsuleBlock::new);

    public ShockCapsuleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        super.onBlockExploded(state, level, pos, explosion);
        if (level.isClientSide)
            return;
        float radius = explosion.radius();
        ItemStack out;
        if (radius < GunTuning.SHOCK_MIN_RADIUS)
            out = new ItemStack(FItems.SHOCK_CAPSULE.get());
        else if (radius <= GunTuning.SHOCK_MAX_RADIUS)
            out = new ItemStack(FItems.DIAMOND_GRIT.get(), GunTuning.SHOCK_GRIT);
        else
            out = new ItemStack(FItems.GRAPHITE.get());
        popResource(level, pos, out);
    }
}
