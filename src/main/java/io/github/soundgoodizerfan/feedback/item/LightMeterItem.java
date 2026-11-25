// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * A photocell behind a pinhole: the light at the face of the block it is put to, block light and sky light apart, to the nearest
 * {@link ItemTuning#LIGHT_METER_RESOLUTION} levels.
 */
public class LightMeterItem extends Item implements Instrument {

    public LightMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.LIGHT;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.LIGHT_METER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.light_meter");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        BlockPos at = pos.relative(context.getClickedFace());
        float block = level.getBrightness(LightLayer.BLOCK, at);
        float sky = Math.max(0, level.getBrightness(LightLayer.SKY, at) - level.getSkyDarken());
        Instrument self = Instruments.fitted(stack, this, level);
        float res = self.resolution(Quantity.LIGHT);
        float shownBlock = Math.max(0f, self.shown(Quantity.LIGHT, block)), shownSky = Math.max(0f, self.shown(Quantity.LIGHT, sky));
        io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, pos, self, Component.translatable("feedback.readout.light",
                Instruments.format(shownBlock, res), Instruments.format(shownSky, res)), Quantity.LIGHT, Math.max(shownBlock, shownSky));
        return InteractionResult.SUCCESS;
    }
}
