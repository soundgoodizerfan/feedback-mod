// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;


import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
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
 * the thing that failed. A figure needs the instrument for that quantity:
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
        float packet = data.getFloat(ElectricServerData.PACKET);
        VoltageBand band = VoltageBand.values()[data.getByte(ElectricServerData.BAND)];
        float amps = data.getFloat(ElectricServerData.AMPS);
        float wanted = data.getFloat(ElectricServerData.WANTED);

        if (data.getBoolean(ElectricServerData.BLOCKED))
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.blocked").copy().withStyle(ChatFormatting.GOLD),
                    "feedback.gloss.electric.blocked"));
        else if (packet < NONE)
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.dead"), "feedback.gloss.electric.dead"));
        else if (role == ElectricMember.Role.SINK && wanted > NONE && amps < wanted - NONE)
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.starved").copy().withStyle(ChatFormatting.GOLD),
                    "feedback.gloss.electric.starved"));
        else
            tooltip.add(Readout.gloss(adjective("feedback.readout.electric.live"), "feedback.gloss.electric.live"));

        // A lamp's step is its block state, which the client has: the glow is seen, so it needs no instrument.
        if (accessor.getBlockState().getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.lamp.LampBlock) {
            var glow = accessor.getBlockState().getValue(io.github.soundgoodizerfan.feedback.machine.lamp.LampBlock.GLOW);
            tooltip.add(Readout.gloss(adjective("feedback.readout.lamp." + glow.getSerializedName()),
                    glow == io.github.soundgoodizerfan.feedback.machine.lamp.LampBlock.Glow.BURNT ? "feedback.gloss.lamp.burnt" : "feedback.gloss.lamp"));
        }

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
            if (Readout.instrumented(Quantity.BAND))
                tooltip.add(Readout.reading(Quantity.BAND, "feedback.readout.jar.stored",
                        Component.translatable(VoltageBand.values()[data.getByte(ElectricServerData.JAR_BAND)].key())));
        }
        if (data.contains(ElectricServerData.BAT)) {
            byte bat = data.getByte(ElectricServerData.BAT);
            float soc = data.getFloat(ElectricServerData.BAT_SOC);
            if (!data.getBoolean(ElectricServerData.BAT_READY))
                tooltip.add(Readout.gloss(Component.translatable("feedback.readout.battery.dry").withStyle(ChatFormatting.GOLD), "feedback.gloss.battery.dry"));
            else {
                tooltip.add(Readout.gloss(adjective(bat == 2 ? "feedback.readout.battery.supplying"
                        : bat == 1 ? "feedback.readout.battery.charging" : "feedback.readout.battery.holding"),
                        bat == 2 ? "feedback.gloss.battery.supplying" : bat == 1 ? "feedback.gloss.battery.charging" : "feedback.gloss.battery.holding"));
                String word = soc < FTuning.BATTERY_SOC_LOW ? "low" : soc < FTuning.BATTERY_SOC_PART ? "part"
                        : soc < FTuning.BATTERY_SOC_NEARLY ? "nearly" : "full";
                tooltip.add(Readout.gloss(adjective("feedback.readout.battery.soc." + word), "feedback.gloss.battery.soc." + word,
                        Math.round(FTuning.BATTERY_SOC_LOW * 100), Math.round(FTuning.BATTERY_SOC_PART * 100), Math.round(FTuning.BATTERY_SOC_NEARLY * 100)));
                if (Readout.instrumented(Quantity.CHARGE))
                    tooltip.add(Readout.reading(Quantity.CHARGE, "feedback.readout.battery.charge", Readout.figure(Quantity.CHARGE, soc * 100f)));
            }
            if (data.getBoolean(ElectricServerData.BAT_UNDER))
                tooltip.add(Readout.gloss(Component.translatable("feedback.readout.battery.under").withStyle(ChatFormatting.GOLD), "feedback.gloss.battery.under",
                        Readout.instrumented(Quantity.BAND) ? Component.translatable(VoltageBand.values()[data.getByte(ElectricServerData.BAT_NEED)].key()).getString() : "?"));
            if (data.getBoolean(ElectricServerData.BAT_VENT))
                tooltip.add(Readout.gloss(Component.translatable("feedback.readout.battery.weeping").withStyle(ChatFormatting.GOLD), "feedback.gloss.battery.weeping"));
            else if (data.getFloat(ElectricServerData.BAT_WATER) < ElectricMachineTuning.BATTERY_WATER_LOW_SHARE)
                tooltip.add(Readout.gloss(Component.translatable("feedback.readout.battery.low_water").withStyle(ChatFormatting.GOLD), "feedback.gloss.battery.low_water"));
        }
        if (Readout.instrumented(Quantity.BAND) && packet >= NONE)
            tooltip.add(Readout.reading(Quantity.BAND, "feedback.readout.band", Component.translatable(band.key())));
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
