// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

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
 * <h2>The band, and never current</h2>
 * It reads the band at the block it is touched to, to a whole band ({@link
 * ItemTuning#ELECTROSCOPE_RESOLUTION_BANDS}). It can say that a line is live and in which band; it
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
        return quantity == Quantity.BAND;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.ELECTROSCOPE_RESOLUTION_BANDS;
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
            if (node.isLive()) {
                float shown = Instruments.quantise(node.getBand().ordinal(), self.resolution(Quantity.BAND));
                io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self,
                        Component.translatable("feedback.readout.band", Component.translatable(node.getBand().key())), Quantity.BAND, shown);
            } else
                io.github.soundgoodizerfan.feedback.net.AimPayloads.read(player, context.getClickedPos(), self,
                        Component.translatable("feedback.readout.band.none"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
