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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

import org.jetbrains.annotations.Nullable;

/**
 * Turns a placed block a quarter turn about the face clicked; sneaking turns it the other way.
 * Create's wrench is the shape (MIT, design only, nothing read for it): the clicked face is the
 * axle the block turns on, which is the only rule that needs no explaining.
 * <p>
 * It asks what a block <em>has</em>, never what it is: a facing turns, an axis swings, and a turn
 * a block cannot take -- a horizontal facing turned onto its back -- passes on to the next quarter
 * that it can. A block with neither is left alone. A turned shaft or wire is taken out of its run
 * and put back, so the networks see the new layout at once.
 */
public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    /** Before the block's own use: a wrench on an engine turns it, it does not switch it on. */
    @Override
    public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        boolean back = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        BlockState turned = turned(state, context.getClickedFace(), back);
        if (turned == null)
            return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, turned, 3);
            relink(server, pos);
            server.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** {@code state} a quarter turn about {@code face}'s axis, or null if it has nothing that turns. */
    @Nullable
    static BlockState turned(BlockState state, Direction face, boolean back) {
        Direction.Axis about = face.getAxis();
        // Clockwise as seen looking at the clicked face.
        boolean clockwise = (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) != back;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty facing) {
                Direction was = state.getValue(facing), d = was;
                for (int i = 0; i < 3; i++) {
                    d = clockwise ? d.getClockWise(about) : d.getCounterClockWise(about);
                    if (d == was)
                        break; // facing along the axle: a turn about it changes nothing
                    if (facing.getPossibleValues().contains(d))
                        return state.setValue(facing, d);
                }
            } else if (property.getValueClass() == Direction.Axis.class) {
                @SuppressWarnings("unchecked")
                EnumProperty<Direction.Axis> axis = (EnumProperty<Direction.Axis>) property;
                Direction.Axis was = state.getValue(axis);
                if (was == about)
                    continue;
                Direction.Axis other = third(was, about);
                if (axis.getPossibleValues().contains(other))
                    return state.setValue(axis, other);
            }
        }
        return null;
    }

    private static Direction.Axis third(Direction.Axis a, Direction.Axis b) {
        for (Direction.Axis c : Direction.Axis.values())
            if (c != a && c != b)
                return c;
        return a;
    }

    /** The block's runs, and its neighbours', rebuilt round the new orientation. */
    private static void relink(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RotationNode) {
            RotationPropagator.rebuildFrom(level, pos);
            for (Direction d : Direction.values())
                if (level.getBlockEntity(pos.relative(d)) instanceof RotationNode)
                    RotationPropagator.rebuildFrom(level, pos.relative(d));
        }
        if (level.getBlockEntity(pos) instanceof ElectricNode)
            ElectricPropagator.reconnect(level, pos);
    }
}
