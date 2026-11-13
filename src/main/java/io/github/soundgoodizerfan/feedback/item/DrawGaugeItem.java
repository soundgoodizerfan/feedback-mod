// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.bench.Measured;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A spring balance with a hook: held against a bow, a crossbow, a spring or a spring gun in the other hand it reads the draw time, the
 * launch speed and the stored energy and stamps them on it, exact ({@link Measured}). It reads nothing on its own, so it is a stamping
 * tool and not a carried instrument.
 */
public class DrawGaugeItem extends Item implements Instrument {

    public DrawGaugeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return false;
    }

    @Override
    public float resolution(Quantity quantity) {
        return 1f;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Measured.apply(Measured.Kind.DRAW, level, player, hand);
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.draw_gauge");
    }
}
