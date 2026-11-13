// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.instrument.Testable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A carried tester for parts: point it at a placed {@link Testable} to be told how
 * it responds, or use it with a board in the other hand. Condition and response are read at
 * {@link ItemTuning#TESTER_RESOLUTION}, and the debug helmet reads the same at the truth tier.
 *
 * <h2>Yield, not a key</h2>
 * Nothing needs it: the whisker's hunt, the coherer's tap and the relay's late release all work
 * without. It buys knowing which spot to keep and when a part is worn. It acts on nothing; it
 * senses, and has no stop because it holds nothing to stop. It does not enter the joint grade or
 * any part's behaviour (philosophy 10).
 */
public class ComponentTesterItem extends Item implements Instrument {

    public ComponentTesterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.RESPONSE || quantity == Quantity.CONDITION;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.TESTER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.component_tester");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof Testable part))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            part.test(this, line -> player.displayClientMessage(Instruments.signed(this, line), false));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        var circuit = other.get(FDataComponents.CIRCUIT.get());
        if (circuit != null) {
            if (!level.isClientSide)
                circuit.test(this, line -> player.displayClientMessage(Instruments.signed(this, line), false));
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }
        var board = other.get(FDataComponents.BOARD.get());
        if (board == null)
            return InteractionResultHolder.pass(held);
        if (!level.isClientSide)
            BoardItem.describe(board, line -> player.displayClientMessage(Instruments.signed(this, line), false));
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
