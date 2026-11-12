// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.note;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The note block, built from a noisemaker. It makes no sound of its own: the sound comes from the {@link NoisemakerItem} fitted into it (one per
 * instrument, each made of a different material), and a block with none fitted is silent. Pitch is the note, 0 to 24. Use with a noisemaker
 * fits it (a fitted one is returned); sneak with an empty hand takes it out; an empty hand otherwise steps the pitch and plays it; a rising signal
 * or a punch plays it. Vanilla takes the instrument from the block below; here it is the part. Vanilla's block is swapped for it, keeping its pitch.
 */
public class FeedbackNoteBlock extends Block {

    public static final IntegerProperty NOTE = BlockStateProperties.NOTE;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Which instrument the fitted noisemaker plays; meaningful only while {@link #FITTED}. */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
    public static final BooleanProperty FITTED = BooleanProperty.create("fitted");

    public FeedbackNoteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NOTE, 0).setValue(POWERED, false).setValue(INSTRUMENT, NoteBlockInstrument.HARP).setValue(FITTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NOTE, POWERED, INSTRUMENT, FITTED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof NoisemakerItem maker) {
            if (!level.isClientSide) {
                if (state.getValue(FITTED))
                    popOut(level, pos, state);
                level.setBlock(pos, state.setValue(INSTRUMENT, maker.instrument()).setValue(FITTED, true), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                play(level, pos, level.getBlockState(pos));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (!state.getValue(FITTED))
                return InteractionResult.PASS;
            popOut(level, pos, state);
            level.setBlock(pos, state.setValue(FITTED, false), Block.UPDATE_ALL);
            return InteractionResult.CONSUME;
        }
        BlockState next = state.cycle(NOTE);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        play(level, pos, next);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide)
            play(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            if (powered && !level.isClientSide)
                play(level, pos, state);
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    private static void popOut(Level level, BlockPos pos, BlockState state) {
        Block.popResource(level, pos, new ItemStack(NoisemakerItem.of(state.getValue(INSTRUMENT))));
    }

    /** One note of the fitted noisemaker: nothing at all if none is fitted. */
    public static void play(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(FITTED) || !level.getBlockState(pos.above()).isAir())
            return;
        int note = state.getValue(NOTE);
        float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
        level.playSeededSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, state.getValue(INSTRUMENT).getSoundEvent(), SoundSource.RECORDS, 3.0f, pitch, level.random.nextLong());
        if (level instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, note / 24.0, 0, 0, 1);
    }
}
