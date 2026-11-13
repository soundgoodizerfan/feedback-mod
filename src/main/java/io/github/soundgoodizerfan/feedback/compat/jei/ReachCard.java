// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.LinkedMachine;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.Reachable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The linked machine's reach set under a card whose recipe asks for a temperature, pressure or speed (philosophy 6): the machine's own
 * figure, an equipment spec printed exactly, and how finely the player's best carried instrument reads what the card asks (words only
 * when none is carried). Never a verdict. {@link #HEIGHT} is the room a card leaves for it; it draws nothing when no machine is linked.
 */
final class ReachCard {

    /** Two rows: the machine's figure, then the player's reading of the requirement. */
    static final int HEIGHT = 20;

    private ReachCard() {
    }

    static void draw(GuiGraphics graphics, int width, int y, Quantity quantity, int labelColour, int valueColour) {
        LinkedMachine.Reach machine = LinkedMachine.current();
        if (machine == null || machine.of(quantity) >= Reachable.Reach.UNSTATED)
            return;
        Font font = Minecraft.getInstance().font;
        String unit = switch (quantity) {
            case PRESSURE -> "Pu";
            case SPEED -> "RPM";
            default -> "Tu";
        };
        CardText.row(graphics, font, width, 1, y, machine.name(), Component.translatable("feedback.jei.reach." + unit.toLowerCase(Locale.ROOT),
                Readout.number(machine.of(quantity))).getString(), labelColour, valueColour, false);
        var instrument = Instruments.reading(Minecraft.getInstance().player, quantity);
        String reading = instrument == null ? Component.translatable("feedback.jei.reach.by_eye").getString()
                : instrument.resolution(quantity) <= 0f ? Component.translatable("feedback.jei.reach.exact", instrument.label().getString()).getString()
                : Component.translatable("feedback.jei.reach.step", Readout.number(instrument.resolution(quantity)), unit, instrument.label().getString()).getString();
        CardText.row(graphics, font, width, 1, y + 10, Component.translatable("feedback.jei.reach.reading"), reading, labelColour, valueColour, false);
    }
}
