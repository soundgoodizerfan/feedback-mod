// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
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
 * A needle that moves when current goes through it: the carried galvanometer.
 *
 * <h2>What it reads, and what it cannot do</h2>
 * Current at the block it is put to, finely enough to tell a loaded line from an idle one and
 * coarsely enough to be a real instrument ({@link ItemTuning#GALVANOMETER_RESOLUTION_AMPS}). On a
 * sink it also says what was wanted, so a starved sink reads as a figure against a figure -- the
 * visible failure of §6.2 -- without the instrument knowing why. It is an {@link Instrument}, which
 * has no way to act: nothing here changes the line.
 *
 * <h2>Pointed, not carried</h2>
 * A wire is a block you walk up to and touch, so the reading is an act in the §8 sense. Unlike a
 * machine it costs nothing to take (a live wire does not stop for it), so there is no price to
 * charge.
 */
public class GalvanometerItem extends Item implements Instrument {

    public GalvanometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.CURRENT;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.GALVANOMETER_RESOLUTION_AMPS;
    }

    @Override
    public float limit(Quantity quantity) {
        return ItemTuning.GALVANOMETER_LIMIT_AMPS;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.galvanometer");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof ElectricNode node))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            // A dead line carries nothing: set against it the needle is back on zero. Past its band the needle is slammed and sits off.
            if (node.getAmps() == 0f && node.getWantedAmps() == 0f) {
                Instruments.recalibrate(stack, Quantity.CURRENT);
                self = Instruments.fitted(stack, this);
            }
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, read(self, node), Quantity.CURRENT,
                    self.shown(Quantity.CURRENT, node.getAmps()));
            Instruments.strain(stack, this, Quantity.CURRENT, node.getAmps());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component read(Instrument self, ElectricNode node) {
        float delivered = self.shown(Quantity.CURRENT, node.getAmps());
        if (node.role() == ElectricMember.Role.SINK) {
            float wanted = self.shown(Quantity.CURRENT, node.getWantedAmps());
            return Component.translatable("feedback.readout.amps.of", format(delivered), format(wanted));
        }
        return Component.translatable("feedback.readout.amps", format(delivered));
    }

    private static String format(float amps) {
        return String.format(java.util.Locale.ROOT, "%.2f", amps);
    }
}
