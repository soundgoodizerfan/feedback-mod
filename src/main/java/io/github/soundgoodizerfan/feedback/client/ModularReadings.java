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
import io.github.soundgoodizerfan.feedback.modular.ModularCause;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularModifiers;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
 * says what each modifier means, in the figure and threshold that decide it, and which part's
 * material it is down to; the Tool Table's result over a tool in its tool slot lists what the work changes.
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
        compare(stack, f, tooltip);
    }

    /**
     * On the Tool Table's result when a finished tool is in its tool slot (a part swapped, a tip
     * added, a piece mended), what the work would change, at the truth tier like the figures.
     */
    private static void compare(ItemStack result, ModularFigures after, List<Component> tooltip) {
        if (!Readout.exact(Quantity.FIGURE) || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                || !(screen.getMenu() instanceof ToolTableMenu menu)
                || screen.getSlotUnderMouse() != menu.getSlot(ToolTableMenu.OUT)
                || !ItemStack.isSameItemSameComponents(menu.getSlot(ToolTableMenu.OUT).getItem(), result))
            return;
        ModularFigures before = ModularStacks.figures(menu.tool());
        if (before == null)
            return;
        List<Component> lines = FigureCompare.lines(before, after);
        if (!lines.isEmpty())
            tooltip.add(Component.translatable("feedback.compare.heading").withStyle(ChatFormatting.GRAY));
        tooltip.addAll(lines);
    }

    /** {@code line} with the parts it is down to while Shift is held: {@code Mass - from Iron head}. */
    private static Component caused(Component line, ItemStack stack, ModularFigures.Fig fig) {
        Component cause = cause(stack, fig);
        return cause == null ? line : Readout.gloss(line, "feedback.gloss.modular.cause", cause);
    }

    /** The parts {@code fig} comes from on this item, or null when it is not down to any. */
    private static Component cause(ItemStack stack, ModularFigures.Fig fig) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return null;
        List<ModularDesign.Part> parts = ModularCause.of(fig, design, type, ClientMaterials.get());
        return parts.isEmpty() ? null : ModularTooltip.parts(parts, type);
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
            out.add(caused(Readout.reading(Quantity.MASS, "feedback.readout.mass", Readout.figure(Quantity.MASS, f.mass().value())),
                    stack, ModularFigures.Fig.MASS));
        if (Readout.instrumented(Quantity.HARDNESS)) {
            float hardness = f.get(ModularFigures.Fig.HARDNESS);
            out.add(caused(Readout.reading(Quantity.HARDNESS, "feedback.readout.hardness", Readout.exact(Quantity.HARDNESS)
                    ? Component.literal(Readout.figure(Quantity.HARDNESS, hardness)) : ladder(hardness)),
                    stack, ModularFigures.Fig.HARDNESS));
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
            out.add(caused(Readout.reading(Quantity.FIGURE, "feedback.readout.attributed",
                    Component.translatableWithFallback("modular.feedback.fig." + name, name),
                    Readout.figure(Quantity.FIGURE, value)), stack, fig));
        }
        out.add(Readout.reading(Quantity.FIGURE, "feedback.readout.modular_wear",
                Readout.figure(Quantity.FIGURE, ModularStacks.worn(stack) * 100), f.step()));
        shieldHeat(stack, f, out);
    }

    /** A shield's face temperature and the capacity it holds at that heat, at the truth tier. */
    private static void shieldHeat(ItemStack stack, ModularFigures f, List<Component> out) {
        net.minecraft.world.level.Level level = Minecraft.getInstance().level;
        if (level == null || !(stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularShield))
            return;
        out.add(Readout.reading(Quantity.FIGURE, "feedback.readout.shield_face", Readout.figure(Quantity.FIGURE,
                io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(stack, level).value())));
        Component hold = Readout.reading(Quantity.FIGURE, "feedback.readout.shield_hold", Readout.figure(Quantity.FIGURE,
                f.get(ModularFigures.Fig.CAPACITY) * io.github.soundgoodizerfan.feedback.modular.ShieldHeat.hold(level, stack)));
        var stands = io.github.soundgoodizerfan.feedback.modular.ShieldHeat.stands(level, stack);
        out.add(stands.isEmpty() ? hold : Readout.gloss(hold, "feedback.gloss.shield_soften",
                Readout.number(io.github.soundgoodizerfan.feedback.core.FTuning.SHIELD_SOFTEN_ONSET * 100),
                Readout.number(stands.get()), Readout.number(io.github.soundgoodizerfan.feedback.core.FTuning.SHIELD_SOFTEN_FLOOR * 100)));
    }

    /** What each modifier on this item means; a hint to hold Shift when there are any. */
    private static void glossary(ItemStack stack, ModularFigures f, List<Component> tooltip) {
        List<ModularModifiers.Modifier> mods = ModularModifiers.of(stack);
        int step = ModularStacks.broken(stack) ? 0 : Math.min(f.step(), ModularTuning.WEAR_STEPS.length);
        if (mods.isEmpty() && step == 0)
            return;
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("feedback.gloss.hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (ModularModifiers.Modifier m : mods) {
            Component line = Component.literal("  ").append(m.line());
            Component cause = m.fig() == null ? null : cause(stack, m.fig());
            tooltip.add(cause == null ? line : Readout.gloss(line, "feedback.gloss.modular.cause", cause));
        }
        if (step > 0)
            tooltip.add(gloss("feedback.gloss.modular.wear",
                    Component.translatable("tooltip.feedback.modular.wear." + step),
                    Readout.number(ModularTuning.WEAR_STEPS[step - 1] * 100),
                    Readout.number(ModularTuning.WEAR_FACTOR[step] * 100)));
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
