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
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.core.hazard.Shielded;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * What a block gives off, in Ru at point blank (mechanics 9.8) -- a figure only with a Geiger
 * counter carried, because a block that glows in the HUD for free would be the survey meter again.
 * A lined container says so, plainly: the lining is a spec printed on it, not a reading.
 */
public class RadiationComponent implements IBlockComponentProvider {

    public static final RadiationComponent INSTANCE = new RadiationComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof Shielded)
            tooltip.add(Component.translatable("feedback.jade.shielded"));
        if (!Readout.instrumented(Quantity.RADIATION))
            return;
        float ru = Radiation.blockActivity(accessor.getBlock(), ClientMaterials.get());
        if (ru > 0f)
            tooltip.add(Readout.reading(Quantity.RADIATION, "feedback.jade.radioactive", Readout.figure(Quantity.RADIATION, ru)));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("radiation");
    }
}
