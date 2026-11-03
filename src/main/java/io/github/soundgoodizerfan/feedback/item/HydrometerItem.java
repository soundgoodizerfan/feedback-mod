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
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Samples the acid of a battery: its density falls as the cells discharge, so the sample is the state of charge
 * ({@link Quantity#CHARGE}, a share of what the cells can hold). Use it on a battery. Read-only, to a step of
 * {@link FTuning#HYDROMETER_RESOLUTION} until a finer instrument narrows it.
 */
public class HydrometerItem extends Item implements Instrument {

    public HydrometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.CHARGE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.HYDROMETER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.hydrometer");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof BatteryBlockEntity battery))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            Component line = battery.ready()
                    ? Instruments.signed(self, Component.translatable("feedback.battery.charge",
                    Instruments.format(battery.stateOfCharge() * 100f, self.resolution(Quantity.CHARGE) * 100f)))
                    : Component.translatable("feedback.battery.dry");
            player.displayClientMessage(line, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
