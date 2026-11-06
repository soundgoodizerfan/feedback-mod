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
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A plate that drops what stands on it: three heights, stepped by hand. Harm is the fall, so a mob that does not take fall damage is untouched. */
public class FallPlateBlock extends Block implements KillMethodBlock {

    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 1, 3);
    private static final float DAMAGE_PER_STEP = 6f;

    public FallPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEIGHT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEIGHT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int next = state.getValue(HEIGHT) % 3 + 1;
            level.setBlock(pos, state.setValue(HEIGHT, next), 3);
            player.displayClientMessage(Component.translatable("feedback.darkcell.fall_height", next), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public KillMethod method(Level level, BlockPos pos, BlockState state) {
        float damage = DAMAGE_PER_STEP * state.getValue(HEIGHT);
        return new KillMethod() {
            @Override
            public KillKind kind() {
                return KillKind.FALL;
            }

            @Override
            public float damage(MobFacts facts) {
                return facts.fallImmune() ? 0f : damage;
            }
        };
    }
}
