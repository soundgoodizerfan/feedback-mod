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
package io.github.soundgoodizerfan.feedback.compat.jei;

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

        Example(List<List<ItemStack>> inputs, int width, int height, List<ItemStack> outputs) {
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
    }

    /** Re-types the one extension for each recipe class JEI asks about. */
    private record ExtensionOf<R extends CustomRecipe>(Extension inner) implements ICraftingCategoryExtension<R> {
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

        // Boards. The substrate is the first one loaded; the lists cycle the thing that varies.
        ResourceLocation substrate = null;
        List<ResourceLocation> wires = new ArrayList<>(), pastes = new ArrayList<>(), solders = new ArrayList<>();
        for (var e : materials.entrySet()) {
            Material m = e.getValue();
            ResourceLocation id = e.getKey();
            if (substrate == null && m.item(id, "substrate").isPresent())
                substrate = id;
            if (m.item(id, "fine_wire").isPresent())
                wires.add(id);
            if (m.traits().contains(Material.Trait.PASTE) && m.item(id, "dust").isPresent())
                pastes.add(id);
            if (m.traits().contains(Material.Trait.SOLDER) && m.item(id, "ingot").isPresent())
                solders.add(id);
        }
        if (substrate == null)
            return out;
        ItemStack base = of(materials, substrate, "substrate");
        List<ItemStack> wireStacks = new ArrayList<>(), wireBoards = new ArrayList<>();
        for (ResourceLocation w : wires) {
            wireStacks.add(of(materials, w, "fine_wire"));
            wireBoards.add(board(new Board(Board.Trace.ETCHED, w, substrate, Optional.empty(), Board.Joint.NONE, List.of())));
        }
        if (!wires.isEmpty())
            out.add(holder("board_etched", new Example(List.of(List.of(base), wireStacks, wireStacks), 3, 1, wireBoards)));
        List<ItemStack> pasteStacks = new ArrayList<>(), pasteBoards = new ArrayList<>();
        for (ResourceLocation p : pastes) {
            pasteStacks.add(of(materials, p, "dust"));
            pasteBoards.add(board(new Board(Board.Trace.PRINTED, p, substrate, Optional.empty(), Board.Joint.NONE, List.of())));
        }
        if (!pastes.isEmpty())
            out.add(holder("board_printed", new Example(List.of(List.of(base), pasteStacks), 2, 1, pasteBoards)));

        Board bare = !wires.isEmpty()
                ? new Board(Board.Trace.ETCHED, wires.get(0), substrate, Optional.empty(), Board.Joint.NONE, List.of())
                : !pastes.isEmpty() ? new Board(Board.Trace.PRINTED, pastes.get(0), substrate, Optional.empty(), Board.Joint.NONE, List.of()) : null;
        if (bare == null)
            return out;
        // Mount: one part on an unpasted board (up to FTuning.CIRCUIT_MAX_PARTS of them, one per craft).
        List<ItemStack> parts = new ArrayList<>(), mounted = new ArrayList<>();
        List<ItemStack> candidates = new ArrayList<>(List.of(new ItemStack(FItems.RELAY.get()), new ItemStack(FItems.COHERER.get()),
                new ItemStack(FItems.GALENA.get()), new ItemStack(FItems.PYRITE.get())));
        materials.forEach((id, m) -> {
            ItemStack plate = of(materials, id, "crystal_plate");
            if (plate != null)
                candidates.add(plate);
        });
        for (ItemStack part : candidates) {
            Board.Part kind = Circuit.part(part, Material.find(materials, part).orElse(null));
            if (kind != null) {
                parts.add(part);
                mounted.add(board(bare.mounted(kind)));
            }
        }
        if (!parts.isEmpty())
            out.add(holder("board_mount", new Example(List.of(List.of(board(bare)), parts), 2, 1, mounted)));
        // Paste: a board and a solder ingot.
        List<ItemStack> solderStacks = new ArrayList<>(), pasted = new ArrayList<>();
        for (ResourceLocation s : solders) {
            solderStacks.add(of(materials, s, "ingot"));
            pasted.add(board(bare.pasted(s)));
        }
        if (!solders.isEmpty())
            out.add(holder("board_paste", new Example(List.of(List.of(board(bare)), solderStacks), 2, 1, pasted)));
        return out;
    }
}
