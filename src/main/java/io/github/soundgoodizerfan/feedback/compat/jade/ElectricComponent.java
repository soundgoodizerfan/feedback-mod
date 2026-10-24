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
import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;

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
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.blocked").copy().withStyle(ChatFormatting.GOLD),
                    "feedback.gloss.electric.blocked"));
        else if (volts < NONE)
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.dead"), "feedback.gloss.electric.dead"));
        else if (role == ElectricMember.Role.SINK && wanted > NONE && amps < wanted - NONE)
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.starved").copy().withStyle(ChatFormatting.GOLD),
                    "feedback.gloss.electric.starved"));
        else
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.live"), "feedback.gloss.electric.live"));

        // A wire's coat is printed on it, so it needs no instrument: the band it holds off, or that it is bare.
        if (accessor.getBlockEntity() instanceof CableBlockEntity wire)
            tooltip.add(data.contains(ElectricServerData.INSULATION)
                    ? Component.translatable("feedback.readout.wire.insulated",
                            Component.translatable(VoltageBand.values()[data.getByte(ElectricServerData.INSULATION)].key()))
                    : Component.translatable("feedback.readout.wire.bare").withStyle(ChatFormatting.GOLD));

        // A Leyden jar on a run takes current from it while it charges, so a sink sharing the run starves
        // beside it: say what the jar is doing, so the mistake can be found (ruled 2026-10-23).
        if (data.contains(ElectricServerData.JAR)) {
            byte jar = data.getByte(ElectricServerData.JAR);
            tooltip.add(Readout.gloss(adjective(jar == 2 ? "feedback.readout.jar.discharging"
                    : jar == 1 ? "feedback.readout.jar.charging" : "feedback.readout.jar.holding"),
                    jar == 2 ? "feedback.gloss.jar.discharging" : jar == 1 ? "feedback.gloss.jar.charging" : "feedback.gloss.jar.holding"));
            if (Readout.instrumented(Quantity.VOLTAGE))
                tooltip.add(Readout.reading(Quantity.VOLTAGE, "feedback.readout.jar.stored",
                        Readout.figure(Quantity.VOLTAGE, data.getFloat(ElectricServerData.JAR_VOLTS))));
        }
        if (Readout.instrumented(Quantity.VOLTAGE))
            tooltip.add(Readout.reading(Quantity.VOLTAGE, "feedback.readout.volts", Readout.figure(Quantity.VOLTAGE, volts)));
        if (Readout.instrumented(Quantity.CURRENT)) {
            if (role == ElectricMember.Role.SINK)
                tooltip.add(Readout.reading(Quantity.CURRENT, "feedback.readout.amps.of",
                        Readout.figure(Quantity.CURRENT, amps), Readout.figure(Quantity.CURRENT, wanted)));
            else
                tooltip.add(Readout.reading(Quantity.CURRENT, "feedback.readout.amps", Readout.figure(Quantity.CURRENT, amps)));
        }
    }

    private static Component adjective(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("electric");
    }
}
