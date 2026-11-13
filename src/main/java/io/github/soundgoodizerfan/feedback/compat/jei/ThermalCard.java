// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
import io.github.soundgoodizerfan.feedback.process.Dip;
import io.github.soundgoodizerfan.feedback.process.FormType;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The one layout of the Thermal Process category: inputs down the left, outputs down the right,
 * a title, and label/value rows beneath. Melts, reactions, boils, quenches, dips, electrolysis,
 * freezes and fired coats are all rows of {@code thermal_process}; they differ in which slots fill
 * and which rows print, never in the shape of the card.
 */
final class ThermalCard {

    static final int HEIGHT = 168;
    private static final int WIDTH = 168, MAX_ROWS = 3, TOP = 14, ROWS_Y = TOP + 18 * MAX_ROWS + 6, VALUE_X = 72;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626, SPOIL_COLOUR = 0xFF8A2B12;

    private ThermalCard() {
    }

    /** Fills slots column by column: {@code index} counts slots already placed on that side. */
    private static final class Side {
        final IRecipeLayoutBuilder builder;
        final RecipeIngredientRole role;
        final boolean right;
        int n;

        Side(IRecipeLayoutBuilder builder, RecipeIngredientRole role, boolean right) {
            this.builder = builder;
            this.role = role;
            this.right = right;
        }

        private mezz.jei.api.gui.builder.IRecipeSlotBuilder next() {
            int col = n / MAX_ROWS, row = n++ % MAX_ROWS;
            int x = right ? WIDTH - 19 - 18 * col : 1 + 18 * col;
            var slot = builder.addSlot(role, x, TOP + 18 * row);
            return role == RecipeIngredientRole.OUTPUT ? slot.setOutputSlotBackground() : slot.setStandardSlotBackground();
        }

        void item(Ingredient ingredient) {
            next().addIngredients(ingredient);
        }

        void item(ItemStack stack) {
            if (!stack.isEmpty())
                next().addItemStack(stack);
        }

        void fluid(net.minecraft.world.level.material.Fluid fluid, int mb) {
            next().addFluidStack(fluid, mb).setFluidRenderer(Math.max(1, mb), false, 16, 16);
        }

        void fluid(FluidStack stack) {
            if (!stack.isEmpty())
                fluid(stack.getFluid(), stack.getAmount());
        }

        void fluid(ResourceLocation id, int mb) {
            fluid(BuiltInRegistries.FLUID.get(id), mb);
        }
    }

    static void layout(IRecipeLayoutBuilder builder, ThermalProcess p) {
        Side in = new Side(builder, RecipeIngredientRole.INPUT, false);
        Side out = new Side(builder, RecipeIngredientRole.OUTPUT, true);
        if (p.dip().isPresent()) {
            Dip d = p.dip().get();
            in.item(d.input());
            if (d.imbue().isPresent())
                in.item(new ItemStack(net.minecraft.world.item.Items.POTION));
            else
                for (var f : d.fluid().getFluids().length > 0 ? new FluidStack[]{d.fluid().getFluids()[0]} : new FluidStack[0])
                    in.fluid(f);
            if (d.hasFluidResult())
                out.fluid(d.resultFluid());
            else if (d.coat().isPresent())
                out.item(d.input());
            else
                out.item(d.result());
            return;
        }
        if (p.electrolysis().isPresent()) {
            var e = p.electrolysis().get();
            var feeds = e.feed().getStacks();
            if (feeds.length > 0)
                in.fluid(feeds[0].getFluid(), 1000);
            e.item().ifPresent(in::item);
            // The input is drawn as 1000 mB of feed, so a no-solute row's per-unit figures are shown for that litre.
            int scale = e.item().isPresent() ? 1 : 1000;
            e.residue().ifPresent(r -> out.fluid(r.fluid(), Math.max(1, r.mb() * scale / r.per())));
            e.products().forEach(pr -> out.fluid(pr.fluid(), pr.mb() * scale));
            e.deposit().ifPresent(d -> out.item(d.stack()));
            return;
        }
        if (p.freeze().isPresent()) {
            var c = p.freeze().get();
            var fluids = c.fluid().getFluids();
            if (fluids.length > 0)
                in.fluid(fluids[0]);
            out.item(c.result());
            return;
        }
        if (p.phase().isPresent()) {
            var phase = p.phase().get();
            var liquids = phase.liquid().getStacks();
            if (liquids.length > 0)
                in.fluid(liquids[0].getFluid(), 1000);
            // The gas is an input too, so a lookup of its uses finds the condensing; the condensate is always an output.
            var gases = phase.gas().getStacks();
            if (gases.length > 0)
                in.fluid(gases[0].getFluid(), 1000);
            out.fluid(phase.gasFluid(), 1000);
            out.fluid(phase.liquidFluid(), 1000);
            return;
        }
        for (Ingredient input : p.inputs())
            in.item(input);
        var reaction = p.reaction();
        for (var a : reaction.map(ThermalProcess.Reaction::fluidInputs).orElse(List.of()))
            in.fluid(a.fluid(), a.mb());
        reaction.flatMap(ThermalProcess.Reaction::catalyst).ifPresent(tag ->
                builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH / 2 - 9, TOP).setStandardSlotBackground()
                        .addItemStacks(java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false)
                                .map(h -> new ItemStack(h.value())).toList()));
        if (p.fire().isPresent() && !p.inputs().isEmpty())
            out.item(p.inputs().get(0)); // the part comes out carrying the coat; the slot shows the parts that can
        else
            out.item(p.result());
        for (var extra : reaction.map(ThermalProcess.Reaction::byproducts).orElse(List.of()))
            out.item(extra.stack());
        for (var a : reaction.map(ThermalProcess.Reaction::fluidOutputs).orElse(List.of()))
            out.fluid(a.fluid(), a.mb());
        out.fluid(p.resultFluid());
    }

    private record Row(String label, String value, int colour) {
    }

    /**
     * The form on display in a form-agnostic row: of the input slots whose ingredient spans more than one form (the others are fuel
     * or reagent), the displayed item of the coarsest, which is the one that sets the hold. Null for any other row.
     */
    private static FormType.Shown shownForm(ThermalProcess p, IRecipeSlotsView slots) {
        if (!p.agnostic() || !p.hasHold())
            return null;
        FormType.Shown best = null;
        for (var slot : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            long forms = slot.getItemStacks().map(s -> FormType.shown(s).form()).filter(f -> !f.isEmpty()).distinct().count();
            if (forms < 2)
                continue;
            var shown = slot.getDisplayedItemStack().map(FormType::shown);
            if (shown.isPresent() && !shown.get().form().isEmpty() && (best == null || shown.get().grain() > best.grain()))
                best = shown.get();
        }
        return best;
    }

    static void draw(ThermalProcess p, IRecipeSlotsView slots, GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, title(p).toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        int y = ROWS_Y;
        for (Row r : rows(p, shownForm(p, slots))) {
            graphics.drawString(font, r.label, 1, y, LABEL_COLOUR, false);
            if (!r.value.isEmpty())
                graphics.drawString(font, r.value, VALUE_X, y, r.colour, false);
            y += 10;
        }
    }

    private static String t(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String fluidName(ResourceLocation id) {
        return BuiltInRegistries.FLUID.get(id).getFluidType().getDescription().getString();
    }

    private static String title(ThermalProcess p) {
        if (p.dip().isPresent()) {
            Dip d = p.dip().get();
            if (d.imbue().isPresent())
                return t("feedback.jei.dip.imbue");
            if (d.coat().isPresent())
                return t("feedback.jei.dip.coat", Readout.number(d.coat().get().thickness()));
            return (d.hasFluidResult() ? d.resultFluid().getHoverName() : d.result().getHoverName()).getString();
        }
        if (p.electrolysis().isPresent())
            return t("feedback.jei.electrolysis");
        if (p.freeze().isPresent())
            return p.freeze().get().result().getHoverName().getString();
        if (p.phase().isPresent())
            return fluidName(p.phase().get().gasFluid());
        if (p.fire().isPresent())
            return t("feedback.jei.fire.title", Component.translatable("material." + p.fire().get().metal().getNamespace() + "." + p.fire().get().metal().getPath()));
        var outs = p.reaction().map(ThermalProcess.Reaction::fluidOutputs).orElse(List.of());
        if (!outs.isEmpty())
            return new FluidStack(BuiltInRegistries.FLUID.get(outs.get(0).fluid()), 1).getHoverName().getString();
        if (!p.resultFluid().isEmpty())
            return p.resultFluid().getHoverName().getString();
        return p.result().isEmpty() ? t("feedback.jei.reaction") : p.result().getHoverName().getString();
    }

    private static List<Row> rows(ThermalProcess p, FormType.Shown shown) {
        List<Row> rows = new ArrayList<>();
        if (p.dip().isPresent()) {
            Dip d = p.dip().get();
            if (d.imbue().isPresent()) {
                rows.add(new Row(t("feedback.jei.dip.imbue.potion"), "", VALUE_COLOUR));
                rows.add(new Row(t("feedback.jei.dip.imbue.hits", d.imbue().get().hits()), "", VALUE_COLOUR));
                return rows;
            }
            rows.add(new Row(t("feedback.jei.dip.into"), d.fluid().amount() + " mB", VALUE_COLOUR));
            rows.add(new Row(t("feedback.jei.dip.vessel"), dipBand(d), VALUE_COLOUR));
            if (d.coat().isPresent())
                rows.add(new Row(t("feedback.jei.dip.overrun",
                        Readout.number(io.github.soundgoodizerfan.feedback.machine.MachineTuning.PLATING_SPALLS_ABOVE_QU)), "", SPOIL_COLOUR));
            return rows;
        }
        if (p.electrolysis().isPresent()) {
            var e = p.electrolysis().get();
            rows.add(new Row(t("feedback.jei.electrolysis.band"), net.minecraft.network.chat.Component.translatable(e.band().key()).getString(), VALUE_COLOUR));
            if (e.minTemperature() > 0f)
                rows.add(new Row(t("feedback.jei.electrolysis.hot", Readout.number(e.minTemperature())), "", VALUE_COLOUR));
            for (var side : io.github.soundgoodizerfan.feedback.process.Electrolysis.Electrode.values()) {
                String names = e.products().stream().filter(pr -> pr.electrode() == side)
                        .map(pr -> fluidName(pr.fluid())).distinct().collect(java.util.stream.Collectors.joining(", "));
                if (!names.isEmpty())
                    rows.add(new Row(t("feedback.jei.electrolysis." + side.getSerializedName()), names, VALUE_COLOUR));
            }
            e.residue().ifPresent(r -> rows.add(new Row(t("feedback.jei.electrolysis.residue"), fluidName(r.fluid()), VALUE_COLOUR)));
            e.deposit().ifPresent(d -> rows.add(new Row(t("feedback.jei.electrolysis.plates"),
                    d.stack().getHoverName().getString() + " / " + d.units() + " mB", VALUE_COLOUR)));
            return rows;
        }
        if (p.freeze().isPresent()) {
            var c = p.freeze().get();
            rows.add(new Row(t("feedback.jei.mold"), t("feedback.jei.mold." + c.mold()), VALUE_COLOUR));
            rows.add(new Row(t("feedback.jei.solid_at"), "<= " + Readout.number(c.solidifyTemperature().value()) + " Tu", VALUE_COLOUR));
            rows.add(new Row(t("feedback.jei.amount"), c.fluid().amount() + " mB", VALUE_COLOUR));
            return rows;
        }
        if (p.phase().isPresent()) {
            rows.add(new Row(t("feedback.jei.phase_change.boil"), Readout.number(p.minTemperature().value()) + " Tu", VALUE_COLOUR));
            rows.add(new Row(t("feedback.jei.phase_change.condense"), fluidName(p.phase().get().liquidFluid()), VALUE_COLOUR));
            return rows;
        }
        if (p.quench()) {
            rows.add(new Row(t("feedback.jei.quench.from"), Readout.number(p.minTemperature().value()) + "+ Tu", VALUE_COLOUR));
            rows.add(new Row(t("feedback.jei.quench.into"), p.medium().map(ThermalCard::fluidName).orElseGet(() -> t("feedback.jei.quench.water")), VALUE_COLOUR));
            return rows;
        }
        if (p.fire().isPresent())
            rows.add(new Row(t("feedback.jei.fire.output", Readout.number(p.fire().get().thickness())), "", VALUE_COLOUR));
        float max = p.maxTemperature().value();
        rows.add(new Row(t("feedback.jei.reaction.temperature"), max < Float.MAX_VALUE
                ? Readout.number(p.minTemperature().value()) + "-" + Readout.number(max) + " Tu"
                : Readout.number(p.minTemperature().value()) + "+ Tu", VALUE_COLOUR));
        rows.add(new Row(t("feedback.jei.optimal"), Readout.number(p.optimalTemperature().value()) + " Tu", VALUE_COLOUR));
        // Time to complete held exactly at the optimum (ThermalProcess#requiredTpu); a pooled recipe has no hold and prints none.
        // A form-agnostic row reads the form on display: the slot cycles through them and the hold follows.
        if (p.hasHold()) {
            int ticks = Math.round(shown == null ? p.requiredTpu() : p.requiredAt(shown.grain()));
            rows.add(new Row(t("feedback.jei.hold"), ticks + " t (" + Readout.number(ticks / 20f) + " s)"
                    + (shown == null ? "" : " " + shown.form().replace('_', ' ')), VALUE_COLOUR));
        }
        if (p.maxRateTuPerTick().tuPerTick() < Float.MAX_VALUE)
            rows.add(new Row(t("feedback.jei.max_rate"), Readout.number(p.maxRateTuPerTick().tuPerTick()) + " Tu/t", VALUE_COLOUR));
        var r = p.reaction();
        if (r.isPresent() && r.get().minPu().isPresent())
            rows.add(new Row(t("feedback.jei.reaction.pressure"), Readout.number(r.get().minPu().get().value())
                    + (r.get().optimalPu().isPresent() ? " (best " + Readout.number(r.get().optimalPu().get().value()) + ")" : "") + " Pu", VALUE_COLOUR));
        if (r.isPresent() && r.get().catalyst().isPresent())
            rows.add(new Row(t("feedback.jei.reaction.catalyst"), t("feedback.jei.reaction.bed"), VALUE_COLOUR));
        if (r.isPresent() && r.get().heat().isPresent())
            rows.add(new Row(t("feedback.jei.reaction.heat"), Readout.number(r.get().heat().get()) + " Tu/t", VALUE_COLOUR));
        if (p.spoilTemperature().value() < Float.MAX_VALUE)
            rows.add(new Row(t("feedback.jei.spoils"), "> " + Readout.number(p.spoilTemperature().value()) + " Tu", SPOIL_COLOUR));
        p.rabble().ifPresent(b -> rows.add(new Row(t("feedback.jei.rabble"), Readout.number(b.minRpm()) + " to " + Readout.number(b.maxRpm()) + " RPM", VALUE_COLOUR)));
        if (p.airless())
            rows.add(new Row(t("feedback.jei.airless"), "Sealed: no air", VALUE_COLOUR));
        p.atmosphere().ifPresent(a -> rows.add(new Row(t("feedback.jei.atmosphere"),
                new FluidStack(BuiltInRegistries.FLUID.get(a.fluid()), a.mb()).getHoverName().getString() + " " + a.mb() + " mB", VALUE_COLOUR)));
        return rows;
    }

    private static String dipBand(Dip d) {
        boolean floor = d.minTemperature().value() > -Float.MAX_VALUE, ceiling = d.maxTemperature().value() < Float.MAX_VALUE;
        String min = Readout.number(d.minTemperature().value()), max = Readout.number(d.maxTemperature().value());
        if (floor && ceiling)
            return min + "-" + max + " Tu";
        if (floor)
            return ">= " + min + " Tu";
        return ceiling ? "<= " + max + " Tu" : t("feedback.jei.dip.any");
    }
}
