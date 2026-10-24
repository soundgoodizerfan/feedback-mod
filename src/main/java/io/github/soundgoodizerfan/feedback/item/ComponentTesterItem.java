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

import io.github.soundgoodizerfan.feedback.core.FTuning;
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
 * A carried tester for parts (mechanics 6.20): point it at a placed {@link Testable} to be told how
 * it responds, or use it with a board in the other hand. Condition and response are read at
 * {@link FTuning#TESTER_RESOLUTION}, and the debug helmet reads the same at the truth tier.
 *
 * <h2>Yield, not a key</h2>
 * Nothing needs it: the whisker's hunt, the coherer's tap and the relay's late release all work
 * without. It buys knowing which spot to keep and when a part is worn. It acts on nothing; it
 * senses, and has no stop because it holds nothing to stop. It does not enter the joint grade or
 * any part's behaviour (philosophy 8).
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
        return FTuning.TESTER_RESOLUTION;
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
