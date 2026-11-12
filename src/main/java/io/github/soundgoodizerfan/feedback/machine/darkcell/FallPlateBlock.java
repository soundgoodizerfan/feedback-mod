// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
            io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, state.getBlock().getName(), java.util.List.of(Component.literal("1"), Component.literal("2"), Component.literal("3")),
                    next - 1, Component.translatable("feedback.darkcell.fall_height", next));
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
