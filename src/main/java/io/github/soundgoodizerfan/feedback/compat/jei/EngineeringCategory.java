// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.School;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.Treatises;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringMinigames;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringRecipe;
import io.github.soundgoodizerfan.feedback.modular.engineering.Fasteners;
import io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * The Engineering Table's cards: the 3x3 grid and what it makes, then below it the hand tools the work needs (each with its wear
 * and, for a hammer, the blow it asks), the fasteners (how many, and the quality the work is built for), the school whose treatise
 * must be held, and the hand-work game that scores the assembly.
 * <p>
 * Two sources: every {@code feedback:engineering} recipe, and one default build of each complex (blueprint) modular type, whose core
 * parts sit in the grid (attachments are fitted on the Fit tab, so they are not here). Tool slots cycle every item with the ability,
 * fastener slots every fastener form of every material at or above the floor; both are read when the card opens, since the materials
 * arrive after JEI starts.
 */
public class EngineeringCategory implements IRecipeCategory<EngineeringCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "engineering", Card.class);

    /**
     * One card. {@code type} is empty for a recipe (its {@code grid} is the nine cells, empty ones {@link Ingredient#EMPTY}, and
     * {@code output} the result) and the modular type's id for a generated build (grid and output are made when the card opens).
     * The cards of a recipe are told apart by {@code id}.
     */
    public record Card(ResourceLocation id, boolean generated, List<Ingredient> grid, ItemStack output,
                       List<EngineeringRecipe.Tool> tools, Fasteners.Need need, String school, String minigame) {

        @Override
        public boolean equals(Object o) {
            return o instanceof Card c && c.generated == generated && c.id.equals(id);
        }

        @Override
        public int hashCode() {
            return id.hashCode() * 31 + (generated ? 1 : 0);
        }
    }

    private static final int WIDTH = 168, HEIGHT = 196;
    private static final int TOOLS_Y = 76, ROW = 18, MAX_TOOLS = 4;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;
    private final IDrawable arrow;

    public EngineeringCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FModular.ENGINEERING_TABLE_ITEM.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.engineering");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    // --- cards ----------------------------------------------------------------------------------

    /** A card for a recipe. */
    static Card of(ResourceLocation id, EngineeringRecipe recipe) {
        List<Ingredient> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++)
            cells.add(Ingredient.EMPTY);
        var pattern = recipe.pattern();
        if (pattern.isPresent()) {
            int w = pattern.get().width(), h = pattern.get().height();
            for (int y = 0; y < h && y < 3; y++)
                for (int x = 0; x < w && x < 3; x++)
                    cells.set(y * 3 + x, pattern.get().ingredients().get(y * w + x));
        } else {
            int i = 0;
            for (Ingredient in : recipe.getIngredients())
                if (i < 9)
                    cells.set(i++, in);
        }
        return new Card(id, false, cells, recipe.getResultItem(null), recipe.tools(), recipe.fasteners(), recipe.school(),
                recipe.minigame().orElse(""));
    }

    /** The cards of every complex type with a build, from the client's copy of the types. */
    static List<Card> generated(Map<ResourceLocation, ModularType> types) {
        List<Card> out = new ArrayList<>();
        types.forEach((id, type) -> {
            if (!type.blueprint() || type.family() == ModularType.Family.DISH || type.patterns().isEmpty())
                return;
            List<EngineeringRecipe.Tool> tools = new ArrayList<>();
            for (String t : type.tools())
                tools.add(new EngineeringRecipe.Tool(t, 1, 0f));
            out.add(new Card(id, true, List.of(), ItemStack.EMPTY, tools, type.fasteners(), type.school(),
                    EngineeringMinigames.forType(type, type.fasteners().count())));
        });
        return out;
    }

    /** The default build of a complex type: its core parts, one to a cell (the grid holds nine), and the item they make. */
    private record Build(List<List<ItemStack>> cells, ItemStack output) {
    }

    private static Build build(ResourceLocation typeId) {
        var types = ModularTypes.known();
        ModularType type = types.get(typeId);
        Map<ResourceLocation, Material> materials = ClientMaterials.get();
        List<List<ItemStack>> cells = new ArrayList<>();
        ItemStack output = new ItemStack(BuiltInRegistries.ITEM.get(typeId));
        if (type == null)
            return new Build(cells, output);
        var design = ToolDesigns.starter(typeId, types, materials);
        if (design.isPresent()) {
            for (ItemStack s : ToolDesigns.stacks(design.get(), type, materials))
                for (int i = 0; i < s.getCount() && cells.size() < 9; i++)
                    cells.add(List.of(s.copyWithCount(1)));
            var built = ToolDesigns.build(design.get(), types, materials);
            if (built.isPresent())
                output = built.get();
        }
        return new Build(cells, output);
    }

    // --- layout ---------------------------------------------------------------------------------

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        ItemStack output = card.output();
        if (card.generated()) {
            Build b = build(card.id());
            for (int i = 0; i < 9; i++) {
                var slot = builder.addInputSlot(1 + 18 * (i % 3), 14 + 18 * (i / 3)).setStandardSlotBackground();
                if (i < b.cells().size())
                    slot.addItemStacks(b.cells().get(i));
            }
            output = b.output();
        } else {
            for (int i = 0; i < 9; i++) {
                var slot = builder.addInputSlot(1 + 18 * (i % 3), 14 + 18 * (i / 3)).setStandardSlotBackground();
                Ingredient in = card.grid().get(i);
                if (!in.isEmpty())
                    slot.addIngredients(in);
            }
        }
        builder.addOutputSlot(98, 32).setOutputSlotBackground().addItemStack(output);

        int y = TOOLS_Y;
        for (int t = 0; t < card.tools().size() && t < MAX_TOOLS; t++, y += ROW) {
            ItemAbility ability = ToolAbilities.byName(card.tools().get(t).ability());
            List<ItemStack> stacks = ability == null ? List.of() : toolStacks(ability);
            builder.addSlot(RecipeIngredientRole.CATALYST, 1, y).setStandardSlotBackground().addItemStacks(stacks);
        }
        y = TOOLS_Y + ROW * MAX_TOOLS;
        if (card.need().count() > 0) {
            List<ItemStack> fasteners = fastenerStacks(card.need().floor());
            builder.addSlot(RecipeIngredientRole.INPUT, 1, y).setStandardSlotBackground().addItemStacks(fasteners)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("feedback.jei.engineering.fastener_tip",
                            card.need().count(), card.need().floor())));
        }
    }

    /** Every item that does {@code ability}: the modular hand tools by what they are, anything answering it, and the ability's item tag. */
    static List<ItemStack> toolStacks(ItemAbility ability) {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack s = new ItemStack(item);
            boolean has;
            try {
                has = ToolAbilities.of(s) == ability || s.is(ToolAbilities.tag(ability)) || s.canPerformAction(ability);
            } catch (RuntimeException e) {
                has = false;
            }
            if (has)
                out.add(s);
        }
        return out;
    }

    /** Every fastener form of every material whose fastener quality reaches {@code floor}, best last. */
    static List<ItemStack> fastenerStacks(int floor) {
        List<ItemStack> out = new ArrayList<>();
        int[] q = {0};
        for (int quality = floor; quality <= 5; quality++) {
            q[0] = quality;
            ClientMaterials.get().forEach((id, material) -> {
                if (material.fastener().filter(v -> v == q[0]).isEmpty())
                    return;
                for (String form : new java.util.TreeSet<>(Fasteners.FORMS))
                    material.item(id, form).ifPresent(item -> out.add(new ItemStack(item)));
            });
        }
        return out;
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("feedback.jei.engineering").getString().toUpperCase(Locale.ROOT),
                1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, 0x40000000);
        arrow.draw(graphics, 66, 32);

        int y = TOOLS_Y;
        for (int t = 0; t < card.tools().size() && t < MAX_TOOLS; t++, y += ROW) {
            EngineeringRecipe.Tool tool = card.tools().get(t);
            String name = abilityName(tool.ability());
            Component line = tool.hammerStrength() > 0f
                    ? Component.translatable("feedback.jei.engineering.tool_strength", name, tool.wear(), String.format(Locale.ROOT, "%.1f", tool.hammerStrength()))
                    : Component.translatable("feedback.jei.engineering.tool", name, tool.wear());
            graphics.drawString(font, font.plainSubstrByWidth(line.getString(), WIDTH - 24), 22, y + 5, VALUE_COLOUR, false);
        }
        y = TOOLS_Y + ROW * MAX_TOOLS;
        if (card.need().count() > 0)
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("feedback.jei.engineering.fasteners",
                    Component.translatable("gui.feedback.engineering.label.fasteners"), card.need().count(), card.need().floor()).getString(), WIDTH - 24),
                    22, y + 5, VALUE_COLOUR, false);
        y += ROW + 2;
        y = note(graphics, font, y, Component.translatable("feedback.jei.engineering.school", schoolName(card.school())), VALUE_COLOUR);
        y = note(graphics, font, y, minigame(card.minigame()), VALUE_COLOUR);
        if (!card.minigame().isEmpty())
            y = note(graphics, font, y, Component.translatable("feedback.jei.engineering.blueprint"), LABEL_COLOUR);
        if (card.generated())
            note(graphics, font, y, Component.translatable("feedback.jei.engineering.default_build"), LABEL_COLOUR);
    }

    private static int note(GuiGraphics graphics, Font font, int y, Component text, int colour) {
        graphics.drawWordWrap(font, text, 1, y, WIDTH - 2, colour);
        return y + 10 * font.split(text, WIDTH - 2).size() + 1;
    }

    private static String abilityName(String ability) {
        ResourceLocation id = ResourceLocation.tryParse(ability);
        String path = id == null ? ability : id.getPath();
        return Component.translatableWithFallback("feedback.jei.engineering.ability." + path, path).getString();
    }

    /** The treatise a school field names ({@code "weapons:2"}), or "none". */
    private static Component schoolName(String field) {
        School school = School.of(field);
        if (school == null)
            return Component.translatable("feedback.jei.engineering.none");
        return Treatises.of(school, School.volumeOf(field)).getHoverName();
    }

    private static Component minigame(String id) {
        if (id.isEmpty())
            return Component.translatable("feedback.jei.engineering.minigame.none");
        ResourceLocation rl = ResourceLocation.tryParse(id);
        String path = rl == null ? id : rl.getPath();
        return Component.translatable("feedback.jei.engineering.minigame",
                Component.translatableWithFallback("feedback.jei.engineering.minigame." + path, path));
    }
}
