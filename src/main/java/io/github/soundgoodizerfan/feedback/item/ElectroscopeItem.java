// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Two leaves that stand apart when something is charged: the first electrical instrument, and a
 * deliberately poor one.
 *
 * <h2>Potential, coarsely, and never current</h2>
 * It reads potential at the block it is touched to, to {@link
 * FTuning#ELECTROSCOPE_RESOLUTION_VOLTS}. It can say that a line is live and roughly how hard; it
 * cannot say how much is flowing, because a charged leaf does not draw current. That is the whole
 * reason the galvanometer is a separate purchase: the electroscope is the instrument that
 * reveals ({@code philosophy 10}), the galvanometer the one that makes the reading useful.
 *
 * <p>It is what makes static electricity observable long before it is a power network, which is
 * the point of slice 4: a pulse from the electrostatic machine is visible on a leaf before
 * anything is wired to anything.
 */
public class ElectroscopeItem extends Item implements Instrument {

    public ElectroscopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.VOLTAGE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.ELECTROSCOPE_RESOLUTION_VOLTS;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.electroscope");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof ElectricNode node))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            float shown = Instruments.quantise(node.getVolts(), self.resolution(Quantity.VOLTAGE));
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, Component.translatable("feedback.readout.volts",
                    String.format(java.util.Locale.ROOT, "%.2f", shown)), Quantity.VOLTAGE, shown);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
