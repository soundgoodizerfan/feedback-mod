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

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Counter's catchment readout: the player tunes the layout by what this says, not by guessing. Counts of things in the world, free to read. */
public class CounterComponent implements IBlockComponentProvider {

    public static final CounterComponent INSTANCE = new CounterComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag r = accessor.getServerData().getCompound(CounterServerData.READOUT);
        if (r.isEmpty() || !r.getBoolean("Stocked"))
            return;
        tooltip.add(Component.translatable("feedback.jade.counter.catchment", r.getInt("InReach"), r.getInt("Wanting")));
        tooltip.add(Component.translatable("feedback.jade.counter.errands", r.getInt("Errands")));
        if (r.getInt("Sealed") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.sealed", r.getInt("Sealed")));
        if (r.getInt("TooDear") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.too_dear", r.getInt("TooDear")));
        if (r.getInt("Stalled") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.stalled", r.getInt("Stalled")));
        if (r.getInt("Road") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.road", r.getInt("Road")));
        tooltip.add(Component.translatable("feedback.jade.counter.sales", r.getInt("WalkIn"), r.getInt("Hand")));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("counter");
    }
}
