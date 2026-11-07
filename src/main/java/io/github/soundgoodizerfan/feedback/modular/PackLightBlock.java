// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light a lantern on a pack gives its wearer. It has no shape and no model, and it removes itself on a scheduled tick unless the
 * wearer it follows still holds it ({@link ModularBackpack#holds}); scheduled ticks are saved with the chunk, so a crash leaves none behind.
 */
public class PackLightBlock extends Block {

    public PackLightBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        level.scheduleTick(pos, this, ModularTuning.PACK_LIGHT_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (ModularBackpack.holds(level, pos))
            level.scheduleTick(pos, this, ModularTuning.PACK_LIGHT_TICKS);
        else
            level.removeBlock(pos, false);
    }
}
