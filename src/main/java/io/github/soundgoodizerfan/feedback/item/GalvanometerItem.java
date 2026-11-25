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
    public float floor(Quantity quantity) {
        return ItemTuning.GALVANOMETER_FLOOR_AMPS;
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
            // The needle's lead is a hand on the conductor: a bare live one shocks the hand that reads it (a glove rated to the band stops it),
            // and the reading is taken all the same. An insulated probe would be a part of its own.
            io.github.soundgoodizerfan.feedback.core.hazard.Shock.apply(player, node.touchBand());
            Instrument self = Instruments.fitted(stack, this, level);
            // A trickle fainter than the needle can feel, for the gain it has, gives no reading at all (not a reading of zero).
            if (!Instruments.readable(self, Quantity.CURRENT, node.getAmps(), io.github.soundgoodizerfan.feedback.process.Circuit.of(stack, level).gain())) {
                io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, context.getClickedPos(), Component.translatable("feedback.readout.faint"));
                return InteractionResult.sidedSuccess(false);
            }
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, read(self, node), Quantity.CURRENT,
                    self.shown(Quantity.CURRENT, node.getAmps()));
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
