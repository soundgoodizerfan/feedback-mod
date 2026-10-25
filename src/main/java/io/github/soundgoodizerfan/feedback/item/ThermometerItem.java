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
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Copper foil, a sealed tube, and something that expands.
 *
 * <h2>An instrument that reveals rather than refines</h2>
 * This is the larger of the slice's two instruments and it arrives second, which is the right way
 * round. Calipers refine a thing the player could already see -- a workpiece was visibly
 * flattening before they owned any -- and they exist to let the player <em>aim</em>. The
 * thermometer reveals a quantity the player cannot perceive at all. The word "hot" is the entire
 * vocabulary they had, and steel's window is 60 Tu wide.
 * <p>
 * The order matters because calipers already taught what an instrument is for. Arriving first,
 * this would have looked like a key.
 *
 * <h2>It is immediately, obviously insufficient</h2>
 * 25 Tu of resolution against a 60 Tu window means the player can be at 1425 or 1450 or 1475 and
 * cannot tell which. And it is still enough to work by hand -- heat, look, wait, look, adjust --
 * miserable, slow, and it produces steel reliably enough to be worth doing.
 * <p>
 * That is the progression moment, and nothing was unlocked. The recipe was never locked; what the
 * player lacked was never heat (§7). They gained <em>information</em>, and information became
 * capability. A better thermometer later buys significant figures, not access.
 *
 * <h2>Why it is carried rather than attached</h2>
 * Same argument as the calipers: reading is passive and costs no hands. The slice's wall -- that
 * the player's best vessel cannot be <em>automated</em> against -- is not enforced by making this
 * a cover, but by the vessels themselves, through {@link ThermalBody#hasThermowell()}. It gates
 * the ambient, continuous reading a HUD card or a future cover would give for free; it is a fact
 * about the vessel rather than a rule about this item, and it stays true for every instrument we
 * ever add.
 *
 * <h2>Sealed is not the same as unreadable</h2>
 * {@link #onItemUseFirst} lets a carried thermometer take one manual reading of any {@link
 * ThermalBody} at all, sealed or not -- the same shape as {@code CalipersItem} reading a wearing
 * machine: a real action, taken by walking up and touching the thing, priced by costing a click
 * rather than by being refused outright. What a seal still buys the vessel is that nobody is
 * watching it for free; it says nothing about whether the player standing in front of it right
 * now can dip a probe through the door.
 */
public class ThermometerItem extends Item implements Instrument {

    public ThermometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.TEMPERATURE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.THERMOMETER_RESOLUTION_TU.value();
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.thermometer");
    }

    /**
     * Puts the thermometer on whatever was clicked, sealed or not. See the class doc for why this
     * bypasses {@code hasThermowell} rather than checking it -- that flag is about who gets to
     * watch for free, not about whether one deliberate dip is allowed.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof ThermalBody body)) {
            // A plant has no heat of its own: what the probe reads is the weather it stands in and how that suits it.
            BlockPos pos = context.getClickedPos();
            if (!io.github.soundgoodizerfan.feedback.process.CropClimate.covers(level.getBlockState(pos)))
                return InteractionResult.PASS;
            if (!level.isClientSide) {
                float rate = io.github.soundgoodizerfan.feedback.process.CropClimate.rate(level, pos, level.getBlockState(pos));
                float shown = Instruments.quantise(io.github.soundgoodizerfan.feedback.process.CropClimate.weather(level, pos), resolution(Quantity.TEMPERATURE));
                player.displayClientMessage(Instruments.signed(this, Component.translatable("feedback.readout.crop", Math.round(shown),
                        Component.translatable("feedback.readout.crop." + (rate <= 0f ? "halted" : rate < 0.5f ? "poor" : rate < 1f ? "fair" : "thriving")))), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide)
            player.displayClientMessage(read(body), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private Component read(ThermalBody body) {
        float shown = Instruments.quantise(body.getTemperature().value(), resolution(Quantity.TEMPERATURE));
        return Instruments.signed(this,
                Component.translatable("feedback.readout.tu", Math.round(shown)));
    }
}
