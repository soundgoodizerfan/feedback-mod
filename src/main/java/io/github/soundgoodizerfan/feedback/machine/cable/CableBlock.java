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
package io.github.soundgoodizerfan.feedback.machine.cable;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A length of wire: one block, connecting on every face to anything electrical it touches.
 *
 * <h2>Made of a material, not of a name</h2>
 * The block is given the material it is a conductor of, and every figure that matters -- how many
 * amps it carries, what it loses per amp, how hot it may get -- is read from that material's
 * file (mechanics 6.4). A second wire is one more registration line and no new class.
 *
 * <h2>The shape is a core, and you can walk through it</h2>
 * A wire run along a floor is not an obstacle. It is also not a full block, because a cube that
 * looks like a block and carries current is a trap that teaches nothing. A bare conductor shocks
 * (mechanics 6.4 stage 1) -- {@link #entityInside} -- which is why it has to be passable: standing
 * in one is how you find out.
 */
public class CableBlock extends Block implements EntityBlock {

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);

    private final ResourceLocation material;

    public CableBlock(Properties properties, ResourceLocation material) {
        super(properties);
        this.material = material;
    }

    /** The material file this wire's figures come from. */
    public ResourceLocation material() {
        return material;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE;
    }

    /**
     * A bare conductor under load hurts whatever is touching it. Throttled to twice a second so
     * it is a hazard and not a death trap; the entity is hurt only while insulation is gone and
     * current is actually flowing.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity.tickCount % 10 != 0)
            return;
        if (level.getBlockEntity(pos) instanceof CableBlockEntity wire && wire.isShocking())
            entity.hurt(level.damageSources().generic(), FTuning.BARE_WIRE_SHOCK_DAMAGE);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CableBlockEntity cable)
            Fittable.dropAll(cable, level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }
}
