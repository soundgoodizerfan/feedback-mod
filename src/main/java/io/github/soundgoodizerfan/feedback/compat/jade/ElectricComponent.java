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

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * An electrical node on the HUD, in the vocabulary the player has paid for.
 *
 * <h2>Senses give adjectives</h2>
 * With nothing carried a line is <em>live</em> or <em>dead</em> and a sink is <em>starved</em> or
 * not -- what the needle and the machine's own slowness already say, free, so a failure shows on
 * the thing that failed (mechanics 6.2). A figure needs the instrument for that quantity:
 * an electroscope for volts, a galvanometer for amps, each at its own resolution.
 */
public class ElectricComponent implements IBlockComponentProvider {

    public static final ElectricComponent INSTANCE = new ElectricComponent();

    /** Below this a figure reads as nothing. Not a tuning figure: it is float noise. */
    private static final float NONE = 1e-3f;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ElectricNode))
            return;
        CompoundTag data = accessor.getServerData();
        if (!data.contains(ElectricServerData.ROLE))
            return;   // no Jade on the server: say nothing rather than guess

        ElectricMember.Role role = ElectricMember.Role.values()[data.getInt(ElectricServerData.ROLE)];
        float volts = data.getFloat(ElectricServerData.VOLTS);
        float amps = data.getFloat(ElectricServerData.AMPS);
        float wanted = data.getFloat(ElectricServerData.WANTED);

        if (data.getBoolean(ElectricServerData.BLOCKED))
            tooltip.add(adjective("feedback.readout.electric.blocked").copy().withStyle(ChatFormatting.GOLD));
        else if (volts < NONE)
            tooltip.add(adjective("feedback.readout.electric.dead"));
        else if (role == ElectricMember.Role.SINK && wanted > NONE && amps < wanted - NONE)
            tooltip.add(adjective("feedback.readout.electric.starved").copy().withStyle(ChatFormatting.GOLD));
        else
            tooltip.add(adjective("feedback.readout.electric.live"));

        if (Readout.instrumented(Quantity.VOLTAGE))
            tooltip.add(Readout.reading(Quantity.VOLTAGE, "feedback.readout.volts", figure(volts)));
        if (Readout.instrumented(Quantity.CURRENT)) {
            if (role == ElectricMember.Role.SINK)
                tooltip.add(Readout.reading(Quantity.CURRENT, "feedback.readout.amps.of", figure(amps), figure(wanted)));
            else
                tooltip.add(Readout.reading(Quantity.CURRENT, "feedback.readout.amps", figure(amps)));
        }
    }

    private static String figure(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static Component adjective(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("electric");
    }
}
