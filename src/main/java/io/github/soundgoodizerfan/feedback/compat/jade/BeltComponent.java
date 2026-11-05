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
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltModule;
import io.github.soundgoodizerfan.feedback.registry.FLogistics;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Names the module in a belt cell and what it needs; the load on the run is a plain adjective. */
public class BeltComponent implements IBlockComponentProvider {

    public static final BeltComponent INSTANCE = new BeltComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean("Belt"))
            return;
        if (data.contains(BeltServerData.MODULE)) {
            BeltModule m = BeltModule.byOrdinal(data.getInt(BeltServerData.MODULE));
            tooltip.add(Component.translatable(FLogistics.MODULE_ITEMS.get(m).get().getDescriptionId()).withStyle(ChatFormatting.GRAY));
            if (data.getBoolean(BeltServerData.SOILED))
                tooltip.add(Component.translatable("jade.feedback.belt.soiled").withStyle(ChatFormatting.YELLOW));
            if (m == BeltModule.COUNTER)
                tooltip.add(Component.translatable("jade.feedback.belt.count", data.getLong(BeltServerData.COUNT)).withStyle(ChatFormatting.GRAY));
            if (m == BeltModule.GATE || m == BeltModule.DIVERTER || m == BeltModule.ELECTROMAGNET || m == BeltModule.AIR_JET)
                tooltip.add(Component.translatable(data.getBoolean(BeltServerData.ENGAGED) ? "message.feedback.belt.engaged" : "message.feedback.belt.released").withStyle(ChatFormatting.GRAY));
        }
        float mass = data.getFloat(BeltServerData.MASS);
        tooltip.add(Component.translatable(mass <= 0f ? "jade.feedback.belt.empty" : mass < 64f ? "jade.feedback.belt.light" : "jade.feedback.belt.loaded")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("belt");
    }
}
