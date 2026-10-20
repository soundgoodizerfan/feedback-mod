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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.Edge;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The figures behind a modular item's adjectives, for a player who carries the instrument that
 * reads them (spec 6): mass from a balance scale, an edge's hardness as a rung of a file set's
 * ladder. What was made is what costs an instrument; the blueprint's target is free paper.
 * <p>
 * At the truth tier (the debug helmet) every baked figure prints, exact, iterated off
 * {@link ModularFigures.Fig} so a figure added to the bake shows with no edit here. Holding Shift
 * says what each adjective means, in the figure and threshold that decide it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class ModularReadings {

    private ModularReadings() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ModularFigures f = ModularStacks.figures(stack);
        if (f == null)
            return;
        List<Component> tooltip = event.getToolTip();
        glossary(stack, f, tooltip);
        if (stack != hovered) {
            hovered = stack;
            scroll = 0;
        }
        window(readings(stack, f), tooltip);
    }

    /** How many reading lines a tooltip lists at once; more than this scroll with the wheel. */
    static final int VISIBLE = 3;

    /** First listed reading. Set by {@link ReadingScroll}, clamped where it is read. */
    static int scroll;
    private static ItemStack hovered = ItemStack.EMPTY;

    /** Every reading this player's instruments give for the item, one entry per line. */
    static List<Component> readings(ItemStack stack, ModularFigures f) {
        List<Component> out = new ArrayList<>();
        if (Readout.exact(Quantity.FIGURE)) {
            truthFigures(stack, f, out);
            return out;
        }
        if (Readout.instrumented(Quantity.MASS))
            out.add(Readout.reading(Quantity.MASS, "feedback.readout.mass", Readout.figure(Quantity.MASS, f.mass().value())));
        if (Readout.instrumented(Quantity.HARDNESS)) {
            float hardness = f.get(ModularFigures.Fig.HARDNESS);
            out.add(Readout.reading(Quantity.HARDNESS, "feedback.readout.hardness", Readout.exact(Quantity.HARDNESS)
                    ? Component.literal(Readout.figure(Quantity.HARDNESS, hardness)) : ladder(hardness)));
            if (Screen.hasShiftDown())
                out.add(gloss("feedback.gloss.scratch_files", files()));
        }
        return out;
    }

    /** Lists {@link #VISIBLE} readings from {@link #scroll}, with a marker for what is out of sight. */
    private static void window(List<Component> lines, List<Component> tooltip) {
        if (lines.size() <= VISIBLE) {
            tooltip.addAll(lines);
            return;
        }
        int first = Math.max(0, Math.min(scroll, lines.size() - VISIBLE));
        int last = first + VISIBLE;
        tooltip.addAll(lines.subList(first, last));
        tooltip.add(Component.translatable("feedback.readout.more", first, lines.size() - last)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Every baked figure, exact, one per line. */
    private static void truthFigures(ItemStack stack, ModularFigures f, List<Component> out) {
        for (ModularFigures.Fig fig : ModularFigures.Fig.values()) {
            float value = f.get(fig);
            if (value == 0)
                continue;
            String name = fig.name().toLowerCase(Locale.ROOT);
            out.add(Readout.reading(Quantity.FIGURE, "feedback.readout.attributed",
                    Component.translatableWithFallback("modular.feedback.fig." + name, name),
                    Readout.figure(Quantity.FIGURE, value)));
        }
        out.add(Readout.reading(Quantity.FIGURE, "feedback.readout.modular_wear",
                Readout.figure(Quantity.FIGURE, ModularStacks.worn(stack) * 100), f.step()));
    }

    /** What each adjective on this item means; a hint to hold Shift when there are any. */
    private static void glossary(ItemStack stack, ModularFigures f, List<Component> tooltip) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return;
        List<String> traits = ModularTooltip.traits(design, ModularTypes.known().get(design.type()), f);
        int step = ModularStacks.broken(stack) ? 0 : Math.min(f.step(), ModularTuning.WEAR_STEPS.length);
        if (traits.isEmpty() && step == 0)
            return;
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("feedback.gloss.hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (String t : traits)
            tooltip.add(gloss("feedback.gloss.modular." + t, args(t)));
        if (step > 0)
            tooltip.add(gloss("feedback.gloss.modular.wear",
                    Component.translatable("tooltip.feedback.modular.wear." + step),
                    Readout.number(ModularTuning.WEAR_STEPS[step - 1] * 100),
                    Readout.number(ModularTuning.WEAR_FACTOR[step] * 100)));
    }

    /** Each gloss's thresholds, read from the constants {@link ModularTooltip} judges by. */
    private static Object[] args(String trait) {
        return switch (trait) {
            case "head_heavy" -> new Object[]{n(ModularTooltip.HEAD_HEAVY)};
            case "balanced" -> new Object[]{n(ModularTooltip.BALANCED_BELOW)};
            case "heavy" -> new Object[]{n(ModularTooltip.HEAVY_SWING)};
            case "light" -> new Object[]{n(ModularTooltip.LIGHT_SWING)};
            case "keen" -> new Object[]{n(Edge.KEEN.bite()), n(Edge.KEEN.chip())};
            case "blunt" -> new Object[]{n(Edge.BLUNT.bite()), n(Edge.BLUNT.chip())};
            case "brittle" -> new Object[]{n(ModularTooltip.BRITTLE_SHARE)};
            case "soft" -> new Object[]{n(ModularTooltip.SOFT_HARDNESS)};
            default -> new Object[0];
        };
    }

    private static String n(float value) {
        return Instruments.format(value, Instrument.TRUTH);
    }

    private static Component gloss(String key, Object... args) {
        return Component.literal("  ").append(Component.translatable(key, args)).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static String files() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ModularTuning.SCRATCH_FILES.length; i++)
            out.append(i == 0 ? "" : ", ").append(i + 1).append(": ").append(n(ModularTuning.SCRATCH_FILES[i]));
        return out.toString();
    }

    /** Which two files the edge falls between: the hardest it is not scratched by, and the softest that scratches it. */
    private static Component ladder(float hardness) {
        float[] files = ModularTuning.SCRATCH_FILES;
        if (hardness < files[0])
            return Component.translatable("feedback.readout.hardness.softer", Integer.toString(1));
        for (int i = files.length - 1; i >= 0; i--)
            if (hardness >= files[i])
                return i == files.length - 1
                        ? Component.translatable("feedback.readout.hardness.harder", i + 1)
                        : Component.translatable("feedback.readout.hardness.between", i + 1, i + 2);
        return Component.empty();
    }
}
