// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.instrument.Instruments;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A rig that puts a number on a made tool. The Impact Pendulum swings into whatever is held when it is used and reports the
 * edge's toughness (and an armour piece's) as the rig can read it; the Strike Post takes a swing at it and reports what the
 * blow carried (joules) and the damage it dealt. Neither marks the specimen: precision is the apparatus's, quantised to its own
 * step ({@link ModularTuning#PENDULUM_STEP}, {@link ModularTuning#POST_JOULE_STEP}, {@link ModularTuning#POST_DAMAGE_STEP}),
 * never finer than a rig of that kind can stand behind (philosophy 8).
 */
public class TestRigBlock extends Block {

    public enum Kind {
        PENDULUM, POST
    }

    public static final MapCodec<TestRigBlock> CODEC = simpleCodec(p -> new TestRigBlock(p, Kind.PENDULUM));

    private final Kind kind;

    public TestRigBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** The pendulum: right-click with the specimen in hand. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (kind != Kind.PENDULUM)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide)
            report(player, pos, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The post: take a swing at it. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (kind == Kind.POST && !level.isClientSide)
            report(player, pos, player.getMainHandItem());
    }

    /**
     * A Better Combat swing at a block never reaches {@link #attack} (the click is ours, not vanilla's), so the swing's strike
     * tick lands here: if the player is looking at a Strike Post within reach, it reports as a left click would.
     */
    public static void swing(Player player) {
        if (player.level().isClientSide)
            return;
        if (player.pick(player.blockInteractionRange(), 1f, false) instanceof BlockHitResult hit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && player.level().getBlockState(hit.getBlockPos()).getBlock() instanceof TestRigBlock rig && rig.kind == Kind.POST)
            rig.report(player, hit.getBlockPos(), player.getMainHandItem());
    }

    private void report(Player player, BlockPos pos, ItemStack held) {
        ModularFigures f = ModularStacks.figures(held);
        Component line;
        if (f == null) {
            line = Component.translatable("feedback.rig.nothing");
        } else if (kind == Kind.PENDULUM) {
            float edge = f.get(ModularFigures.Fig.EDGE_TOUGHNESS), armour = f.get(ModularFigures.Fig.ARMOUR_TOUGHNESS);
            if (edge <= 0f && armour <= 0f)
                line = Component.translatable("feedback.rig.nothing");
            else if (edge > 0f) {
                float off = f.get(ModularFigures.Fig.EDGE_TOUGHNESS_OFF);
                line = off > 0f
                        ? Component.translatable("feedback.rig.pendulum_twin", Instruments.format(edge, ModularTuning.PENDULUM_STEP),
                                Instruments.format(off, ModularTuning.PENDULUM_STEP))
                        : Component.translatable("feedback.rig.pendulum", Instruments.format(edge, ModularTuning.PENDULUM_STEP));
            } else
                line = Component.translatable("feedback.rig.pendulum_armour", Instruments.format(armour, ModularTuning.PENDULUM_STEP));
        } else {
            float joules = f.get(ModularFigures.Fig.IMPACT), damage = f.get(ModularFigures.Fig.DAMAGE);
            line = joules <= 0f && damage <= 0f ? Component.translatable("feedback.rig.nothing")
                    : Component.translatable("feedback.rig.post", Instruments.format(joules, ModularTuning.POST_JOULE_STEP),
                            Instruments.format(damage, ModularTuning.POST_DAMAGE_STEP));
        }
        io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, line);
    }
}
