// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import java.util.List;

import com.mojang.serialization.MapCodec;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A framed {@link ModularBed} placed as a two-block bed. It is a vanilla {@link BedBlock} (so sleeping, the night skip, the respawn point and
 * every mod that asks {@code isBed} work as for a vanilla bed) whose halves carry the bed stack ({@link ModularBedBlockEntity}). The wake-up
 * heals by the bed's comfort and warmth and wears it ({@link ModularBed#soothe}); breaking either half drops the head's stack, design intact.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class ModularBedBlock extends BedBlock {

    public ModularBedBlock(Properties properties) {
        super(DyeColor.WHITE, properties);
    }

    @Override
    public MapCodec<BedBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModularBedBlockEntity(pos, state);
    }

    /** Drawn as a model (tinted by the design), not by the vanilla bed's entity renderer. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos head = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
        if (level.getBlockEntity(head) instanceof ModularBedBlockEntity be && ModularStacks.broken(be.bed())) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.feedback.bed.worn_out"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    /** The head's bed stack is the drop: one bed per two blocks, whichever half is broken. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(PART) != BedPart.HEAD || !(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ModularBedBlockEntity be)
                || be.bed().isEmpty())
            return List.of();
        // Recall: a magnetic frame comes back to the hand that broke it, as a loyal weapon does.
        if (params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player breaker
                && io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.recalls(be.bed())) {
            breaker.getInventory().placeItemBackInInventory(be.bed().copy());
            return List.of();
        }
        return List.of(be.bed().copy());
    }

    /** Damp: a mattress takes a share off a fall onto the bed (on top of a bed's own half). */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        float damp = level.getBlockEntity(pos) instanceof ModularBedBlockEntity be
                ? io.github.soundgoodizerfan.feedback.modular.attunement.RestKinds.dampShare(be.bed()) : 0f;
        super.fallOn(level, state, pos, entity, fallDistance * (1f - damp));
    }

    /** How much slower each level of anchor makes the planted bed to break. */
    private static final float ANCHOR_SLOW_PER_LEVEL = 1.5f;

    /** Anchor: a planted bed is slow to pull up. */
    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getState().getBlock() instanceof ModularBedBlock))
            return;
        event.getPosition().ifPresent(pos -> {
            if (event.getEntity().level().getBlockEntity(pos) instanceof ModularBedBlockEntity be) {
                int anchor = io.github.soundgoodizerfan.feedback.modular.attunement.TideKinds.level(be.bed(), "bed_anchor");
                if (anchor > 0)
                    event.setNewSpeed(event.getNewSpeed() / (1f + ANCHOR_SLOW_PER_LEVEL * anchor));
            }
        });
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;
        for (BlockPos half : List.of(pos, pos.relative(state.getValue(FACING))))
            if (level.getBlockEntity(half) instanceof ModularBedBlockEntity be)
                be.setBed(stack);
    }

    /** The night is over for whoever slept in a placed bed: they heal by its comfort and warmth, and it wears. */
    @SubscribeEvent
    public static void slept(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        for (ServerPlayer player : level.players())
            player.getSleepingPos().ifPresent(pos -> {
                BlockState bed = level.getBlockState(pos);
                if (!(bed.getBlock() instanceof ModularBedBlock))
                    return;
                BlockPos head = bed.getValue(PART) == BedPart.HEAD ? pos : pos.relative(bed.getValue(FACING));
                if (level.getBlockEntity(head) instanceof ModularBedBlockEntity be) {
                    ModularBed.soothe(player, level, be.bed());
                    be.changed();
                    if (level.getBlockEntity(pos) instanceof ModularBedBlockEntity foot && foot != be)
                        foot.setBed(be.bed());
                }
            });
    }
}
