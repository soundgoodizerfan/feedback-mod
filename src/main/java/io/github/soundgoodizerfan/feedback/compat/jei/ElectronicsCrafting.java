// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.item.ItemTuning;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.BoardRecipe;
import io.github.soundgoodizerfan.feedback.item.MassProbeRecipe;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.process.Cut;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The mass probe and the trace board are crafted by code, not by a pattern, so JEI has nothing to
 * draw for them. Each is shown through a handful of {@link Example}s built from the loaded
 * materials (so a pack's own crystals and solders appear), published from the runtime like every
 * other table (see {@link FeedbackJeiPlugin}). The real recipes are marked unhandled so JEI does
 * not draw an empty card for them.
 */
public final class ElectronicsCrafting {

    private ElectronicsCrafting() {
    }

    /** A display-only crafting recipe: a grid of cycling inputs and an output list that cycles in step. */
    public static final class Example extends CustomRecipe {
        final List<List<ItemStack>> inputs;
        final int width, height;
        final List<ItemStack> outputs;

        public Example(List<List<ItemStack>> inputs, int width, int height, List<ItemStack> outputs) {
            super(CraftingBookCategory.MISC);
            this.inputs = inputs;
            this.width = width;
            this.height = height;
            this.outputs = outputs;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return false;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return false;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return FRecipes.BOARD.get();
        }
    }

    /** Draws an {@link Example}; for the two real recipe classes it says it handles nothing. */
    public static final class Extension implements ICraftingCategoryExtension<CustomRecipe> {
        @Override
        public boolean isHandled(RecipeHolder<CustomRecipe> holder) {
            return holder.value() instanceof Example;
        }

        @Override
        public void setRecipe(RecipeHolder<CustomRecipe> holder, IRecipeLayoutBuilder builder,
                              ICraftingGridHelper grid, IFocusGroup focuses) {
            Example e = (Example) holder.value();
            grid.createAndSetInputs(builder, e.inputs, e.width, e.height);
            grid.createAndSetOutputs(builder, e.outputs);
        }

        @Override
        public int getWidth(RecipeHolder<CustomRecipe> holder) {
            return ((Example) holder.value()).width;
        }

        @Override
        public int getHeight(RecipeHolder<CustomRecipe> holder) {
            return ((Example) holder.value()).height;
        }
    }

    static void register(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        var crafting = registration.getCraftingCategory();
        Extension ext = new Extension();
        crafting.addExtension(Example.class, new ExtensionOf<>(ext));
        crafting.addExtension(MassProbeRecipe.class, new ExtensionOf<>(ext));
        crafting.addExtension(BoardRecipe.class, new ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.item.InstrumentFitRecipe.class, new ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.item.RodGrindRecipe.class, new ExtensionOf<>(ext));
    }

    /** Re-types the one extension for each recipe class JEI asks about. */
    record ExtensionOf<R extends CustomRecipe>(Extension inner) implements ICraftingCategoryExtension<R> {
        @Override
        @SuppressWarnings("unchecked")
        public boolean isHandled(RecipeHolder<R> holder) {
            return inner.isHandled((RecipeHolder<CustomRecipe>) (RecipeHolder<?>) holder);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setRecipe(RecipeHolder<R> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
            inner.setRecipe((RecipeHolder<CustomRecipe>) (RecipeHolder<?>) holder, builder, grid, focuses);
        }

        @Override
        @SuppressWarnings("unchecked")
        public int getWidth(RecipeHolder<R> holder) {
            return inner.getWidth((RecipeHolder<CustomRecipe>) (RecipeHolder<?>) holder);
        }

        @Override
        @SuppressWarnings("unchecked")
        public int getHeight(RecipeHolder<R> holder) {
            return inner.getHeight((RecipeHolder<CustomRecipe>) (RecipeHolder<?>) holder);
        }
    }

    /** A stack of {@code form} of material {@code id}, or null. */
    private static ItemStack of(Map<ResourceLocation, Material> materials, ResourceLocation id, String form) {
        return materials.get(id).stack(id, form).orElse(null);
    }

    private static ItemStack withCut(ItemStack stack, Cut cut) {
        if (cut != Cut.COARSE)
            stack.set(FDataComponents.CUT.get(), cut);
        return stack;
    }

    private static ItemStack board(Board board) {
        ItemStack stack = new ItemStack(FItems.TRACE_BOARD.get());
        stack.set(FDataComponents.BOARD.get(), board);
        return stack;
    }

    private static RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> holder(String path, Example e) {
        return new RecipeHolder<>(Feedback.id("jei/" + path), e);
    }

    /** The examples for the current material table. Anything the table cannot supply is simply left out. */
    public static List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> examples(Map<ResourceLocation, Material> materials) {
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> out = new ArrayList<>();

        // Instruments: each fittable one with a circuit (a quiet one, so the fitted result is not a dead stack).
        ItemStack circuit = new ItemStack(FItems.CIRCUIT.get());
        circuit.set(FDataComponents.CIRCUIT.get(), new Circuit(1, 1f, ProcessTuning.CIRCUIT_NOISE_FLOOR, Board.Joint.GOOD, 0f, 0f, 0f, 0f, 0f, 0f, Circuit.clockRateOf(1f)));
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(FTags.CIRCUIT_FITTABLE)) {
            ItemStack fitted = new ItemStack(item.value());
            fitted.set(FDataComponents.CIRCUIT.get(), circuit.get(FDataComponents.CIRCUIT.get()));
            out.add(holder("fit_" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.value()).getPath(),
                    new Example(List.of(List.of(new ItemStack(item.value())), List.of(circuit)), 2, 1, List.of(fitted))));
        }

        // Mass probe: every ringing crystal plate in every cut, with any metal plate.
        List<ItemStack> crystals = new ArrayList<>(), probes = new ArrayList<>(), backings = new ArrayList<>();
        materials.forEach((id, m) -> {
            ItemStack plate = of(materials, id, "plate");
            if (plate != null)
                backings.add(plate);
            ItemStack crystal = of(materials, id, "crystal_plate");
            if (crystal == null || m.electrical().piezoCoupling().orElse(0f) < 0.5f)
                return;
            for (Cut cut : Cut.values()) {
                crystals.add(withCut(crystal.copy(), cut));
                ItemStack probe = new ItemStack(FItems.MASS_PROBE.get());
                probe.set(FDataComponents.CUT.get(), cut);
                probes.add(probe);
            }
        });
        if (!crystals.isEmpty() && !backings.isEmpty())
            out.add(holder("mass_probe", new Example(List.of(crystals, backings), 2, 1, probes)));

        // Boards: one set of cards per substrate loaded; the lists cycle the thing that varies (wire, paste, part, solder).
        List<ResourceLocation> substrates = new ArrayList<>(), wires = new ArrayList<>(), pastes = new ArrayList<>(), solders = new ArrayList<>();
        for (var e : materials.entrySet()) {
            Material m = e.getValue();
            ResourceLocation id = e.getKey();
            if (m.item(id, "substrate").isPresent())
                substrates.add(id);
            if (m.item(id, "fine_wire").isPresent())
                wires.add(id);
            if (m.traits().contains(Material.Trait.PASTE) && m.item(id, "dust").isPresent())
                pastes.add(id);
            if (m.traits().contains(Material.Trait.SOLDER) && m.item(id, "ingot").isPresent())
                solders.add(id);
        }
        List<ItemStack> wireStacks = new ArrayList<>(), pasteStacks = new ArrayList<>(), solderStacks = new ArrayList<>();
        wires.forEach(w -> wireStacks.add(of(materials, w, "fine_wire")));
        pastes.forEach(p -> pasteStacks.add(of(materials, p, "dust")));
        solders.forEach(sd -> solderStacks.add(of(materials, sd, "ingot")));
        List<ItemStack> candidates = new ArrayList<>(List.of(new ItemStack(FItems.RELAY.get()), new ItemStack(FItems.COHERER.get()),
                new ItemStack(FItems.GALENA.get()), new ItemStack(FItems.PYRITE.get())));
        // A sealed tube (an example: a triode at a good vacuum; the real ones come from a vacuum dryer).
        ItemStack tube = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FModular.VACUUM_TUBE.get());
        tube.set(FDataComponents.TUBE.get(), new io.github.soundgoodizerfan.feedback.process.Tube(
                io.github.soundgoodizerfan.feedback.process.Tube.Kind.TRIODE, 0.9f, 1f, 0.8f, 0.1f, 0.5f, 200f, 0.7f));
        candidates.add(tube);
        // A sample resonator and lattice (the real ones come from the part bench; their figures are from their parts).
        for (var e : List.of(
                new io.github.soundgoodizerfan.feedback.process.Element(Board.Kind.RESONATOR, io.github.soundgoodizerfan.feedback.process.Element.Character.BAND,
                        new Circuit.Line(0.5f, 3, 0.5f, 0, 0f, 0f)),
                new io.github.soundgoodizerfan.feedback.process.Element(Board.Kind.LATTICE, io.github.soundgoodizerfan.feedback.process.Element.Character.MIXED,
                        new Circuit.Line(0f, 0, 0f, 3, 0.5f, 0.6f)))) {
            ItemStack sample = new ItemStack(e.kind() == Board.Kind.RESONATOR ? io.github.soundgoodizerfan.feedback.registry.FModular.RESONATOR.get()
                    : io.github.soundgoodizerfan.feedback.registry.FModular.LATTICE.get());
            sample.set(FDataComponents.ELEMENT.get(), e);
            candidates.add(sample);
        }
        materials.forEach((id, m) -> {
            ItemStack plate = of(materials, id, "crystal_plate");
            if (plate != null)
                candidates.add(plate);
        });
        for (ResourceLocation substrate : substrates) {
            ItemStack base = of(materials, substrate, "substrate");
            String tag = substrates.size() > 1 ? "_" + substrate.getPath() : "";
            List<ItemStack> wireBoards = new ArrayList<>(), pasteBoards = new ArrayList<>();
            for (ResourceLocation w : wires)
                wireBoards.add(board(new Board(Board.Trace.ETCHED, w, substrate, Optional.empty(), Board.Joint.NONE, List.of())));
            for (ResourceLocation p : pastes)
                pasteBoards.add(board(new Board(Board.Trace.PRINTED, p, substrate, Optional.empty(), Board.Joint.NONE, List.of())));
            if (!wires.isEmpty())
                out.add(holder("board_etched" + tag, new Example(List.of(List.of(base), wireStacks, wireStacks), 3, 1, wireBoards)));
            if (!pastes.isEmpty())
                out.add(holder("board_printed" + tag, new Example(List.of(List.of(base), pasteStacks), 2, 1, pasteBoards)));

            Board bare = !wires.isEmpty()
                    ? new Board(Board.Trace.ETCHED, wires.get(0), substrate, Optional.empty(), Board.Joint.NONE, List.of())
                    : !pastes.isEmpty() ? new Board(Board.Trace.PRINTED, pastes.get(0), substrate, Optional.empty(), Board.Joint.NONE, List.of()) : null;
            if (bare == null)
                continue;
            // Mount: one part on an unpasted board (up to ItemTuning.CIRCUIT_MAX_PARTS of them, one per craft).
            List<ItemStack> parts = new ArrayList<>(), mounted = new ArrayList<>();
            for (ItemStack part : candidates) {
                Board.Part kind = Circuit.part(part, Material.find(materials, part).orElse(null));
                if (kind != null) {
                    parts.add(part);
                    mounted.add(board(bare.mounted(kind)));
                }
            }
            if (!parts.isEmpty())
                out.add(holder("board_mount" + tag, new Example(List.of(List.of(board(bare)), parts), 2, 1, mounted)));
            // Paste: a board and a solder ingot.
            List<ItemStack> pasted = new ArrayList<>();
            for (ResourceLocation sd : solders)
                pasted.add(board(bare.pasted(sd)));
            if (!solders.isEmpty())
                out.add(holder("board_paste" + tag, new Example(List.of(List.of(board(bare)), solderStacks), 2, 1, pasted)));
        }
        return out;
    }
}
