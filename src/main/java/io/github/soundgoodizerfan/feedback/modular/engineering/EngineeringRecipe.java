// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.modular.type.ModularFit;
import io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.treatise.Treatises;
import io.github.soundgoodizerfan.feedback.registry.FEngineering;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/**
 * Hand and bench work at the Engineering Table as a recipe: a second vanilla {@code RecipeType} beside {@code hand_deformation}, for the
 * same reason (the player is the one working, so hand work may be a recipe; no machine owns it).
 *
 * <p>The ingredients are shaped ({@code pattern}/{@code key}) or shapeless ({@code ingredients}) and matched against the table's 3x3 grid.
 * Beyond the grid a recipe asks for tools ({@code tool}: an item ability, the wear a craft costs, and for the hammer an optional
 * minimum strength), fasteners ({@code fasteners}: how many are spent and the quality the work is built for) and knowledge
 * ({@code school}: a treatise volume held in the binder slot or anywhere the table looks). {@code minigame} names the hand-work game
 * that scores the assembly; without it the craft is plain. {@code fit_order} (item ids) is the optional seating order for the fitting game
 * when the result is not a modular item.
 *
 * <p>{@link #attempt} is the whole pure logic. It never gates by precision: a hammer short of its strength lowers the start condition,
 * and only a shortfall past half of it is physically impossible and refuses; fasteners under the floor still build, worse.
 */
public class EngineeringRecipe implements Recipe<EngineeringInput> {

    /** One tool a recipe calls for. {@code wear} is spent per craft; {@code hammerStrength} (St) is a floor on the hammer's own blow, 0 for none. */
    public record Tool(String ability, int wear, float hammerStrength) {

        public static final Codec<Tool> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("ability").forGetter(Tool::ability),
                Codec.intRange(0, 100).optionalFieldOf("wear", 1).forGetter(Tool::wear),
                Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("hammer_strength", 0f).forGetter(Tool::hammerStrength)).apply(i, Tool::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Tool> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Tool::ability, ByteBufCodecs.VAR_INT, Tool::wear, ByteBufCodecs.FLOAT, Tool::hammerStrength, Tool::new);

        public ItemAbility abilityOf() {
            return ToolAbilities.byName(ability);
        }
    }

    /** Why a craft is not offered. */
    public enum Refusal {
        /** The grid does not hold the ingredients. */
        GRID,
        /** The school's volume is not in the binder or anywhere the table looks. */
        SCHOOL,
        /** No tool in the row has a needed ability. */
        NO_TOOL,
        /** The tool with a needed ability is broken: it stays where it is until reworked or swapped. */
        BROKEN_TOOL,
        /** The hammer's blow is under half of what the work asks: physically impossible. */
        HAMMER_TOO_WEAK,
        /** Fewer fasteners than the work spends. */
        NO_FASTENERS
    }

    /**
     * What {@link #attempt} found.
     *
     * @param refusal        why not, or null when the craft is offered.
     * @param result         the item made (empty when refused); a copy, safe to hand out.
     * @param start          the start-condition inputs for {@link ModularFit#fit}: fastener levels (less the hammer's shortfall) and the
     *                       minigame score slot left at none, for the caller to fill after a play. A blueprint is the caller's.
     * @param toolSlot       per recipe tool, the index in the input's tool list that does the work (parallel to {@link #tools()}).
     * @param toolWear       the points each of those wears, parallel to {@code toolSlot}.
     * @param fasteners      what the fastener slots supply (call {@link Fasteners.Supply#spend} to take them).
     * @param hammerShortfall St the hammer falls short by (0 when it reaches, or no strength is asked).
     */
    public record Attempt(Refusal refusal, ItemStack result, ModularFit.Start start, int[] toolSlot, int[] toolWear,
                          Fasteners.Supply fasteners, float hammerShortfall) {

        public boolean ok() {
            return refusal == null;
        }

        /** Wears the tools of {@code input} by what this attempt found and spends its fasteners. Server side, once the craft is taken. */
        public void apply(EngineeringInput input) {
            for (int i = 0; i < toolSlot.length; i++)
                if (toolSlot[i] >= 0 && toolSlot[i] < input.tools().size())
                    ToolAbilities.wear(input.tools().get(toolSlot[i]), toolWear[i]);
            if (fasteners != null)
                fasteners.spend(input.fasteners());
        }
    }

    private final Either<ShapedRecipePattern, List<Ingredient>> grid;
    private final ItemStack result;
    private final List<Tool> tools;
    private final Fasteners.Need need;
    private final String school;
    private final Optional<String> minigame;
    private final List<String> fitOrder;

    public EngineeringRecipe(Either<ShapedRecipePattern, List<Ingredient>> grid, ItemStack result, List<Tool> tools, Fasteners.Need need,
                             String school, Optional<String> minigame, List<String> fitOrder) {
        this.fitOrder = List.copyOf(fitOrder);
        this.grid = grid;
        this.result = result;
        this.tools = List.copyOf(tools);
        this.need = need;
        this.school = school;
        this.minigame = minigame;
    }

    public List<Tool> tools() {
        return tools;
    }

    public Fasteners.Need fasteners() {
        return need;
    }

    /** The school field ({@code "weapons:2"}), empty for none. */
    public String school() {
        return school;
    }

    public Optional<String> minigame() {
        return minigame;
    }

    /** Item ids that seat first, in this order, in the fitting game when the result is not a modular item (the rest follow in grid order); empty for none. */
    public List<String> fitOrder() {
        return fitOrder;
    }

    /** Whether the ingredients are laid in a pattern (else any order). */
    public boolean shaped() {
        return grid.left().isPresent();
    }

    /** The pattern, for a shaped recipe. */
    public Optional<ShapedRecipePattern> pattern() {
        return grid.left();
    }

    private boolean gridMatches(EngineeringInput input) {
        if (grid.left().isPresent())
            return grid.left().get().matches(input.crafting());
        List<Ingredient> wanted = grid.right().orElseThrow();
        List<ItemStack> have = input.gridStacks();
        return have.size() == wanted.size() && RecipeMatcher.findMatches(have, wanted) != null;
    }

    /** Everything a craft of this input would do, or why it is refused. Pure: nothing is worn or spent until {@link Attempt#apply}. */
    public Attempt attempt(EngineeringInput input) {
        if (!gridMatches(input))
            return refuse(Refusal.GRID);
        School s = School.of(school);
        if (s != null && ((Treatises.mask(input.binder()) | input.known()) & (1 << s.bit(School.volumeOf(school)))) == 0)
            return refuse(Refusal.SCHOOL);
        int[] slot = new int[tools.size()], wear = new int[tools.size()];
        boolean[] used = new boolean[input.tools().size()];
        float shortfall = 0f, shortShare = 0f;
        for (int t = 0; t < tools.size(); t++) {
            Tool need = tools.get(t);
            ItemAbility ability = need.abilityOf();
            int found = -1, broken = -1;
            for (int i = 0; i < input.tools().size() && ability != null; i++) {
                ItemStack stack = input.tools().get(i);
                if (used[i] || !ToolAbilities.can(stack, ability))
                    continue;
                if (!ModularStacks.broken(stack)) {
                    found = i;
                    break;
                }
                if (broken < 0)
                    broken = i;
            }
            if (found < 0)
                return refuse(broken >= 0 ? Refusal.BROKEN_TOOL : Refusal.NO_TOOL);
            used[found] = true;
            slot[t] = found;
            wear[t] = need.wear();
            if (need.hammerStrength() > 0f) {
                float short_ = Math.max(0f, need.hammerStrength() - ModularHandTool.blow(input.tools().get(found)));
                if (short_ > need.hammerStrength() * ModularTuning.HAMMER_SHORT_REFUSE)
                    return refuse(Refusal.HAMMER_TOO_WEAK);
                if (short_ / need.hammerStrength() > shortShare) {
                    shortShare = short_ / need.hammerStrength();
                    shortfall = short_;
                }
            }
        }
        Fasteners.Supply supply = Fasteners.supply(input.fasteners(), need);
        if (!supply.complete())
            return new Attempt(Refusal.NO_FASTENERS, ItemStack.EMPTY, ModularFit.Start.NONE, slot, wear, supply, shortfall);
        int lost = Math.round(shortShare / ModularTuning.HAMMER_SHORT_REFUSE * ModularTuning.START_HAMMER_SHORT_LEVELS);
        return new Attempt(null, result.copy(), new ModularFit.Start(supply.levels() - lost, -1f), slot, wear, supply, shortfall);
    }

    private static Attempt refuse(Refusal why) {
        return new Attempt(why, ItemStack.EMPTY, ModularFit.Start.NONE, new int[0], new int[0], null, 0f);
    }

    @Override
    public boolean matches(EngineeringInput input, Level level) {
        return attempt(input).ok();
    }

    @Override
    public ItemStack assemble(EngineeringInput input, HolderLookup.Provider registries) {
        return attempt(input).result();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> out = NonNullList.create();
        grid.ifLeft(p -> out.addAll(p.ingredients())).ifRight(out::addAll);
        return out;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FEngineering.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FEngineering.TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<EngineeringRecipe> {

        private static final MapCodec<EngineeringRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.mapEither(ShapedRecipePattern.MAP_CODEC, Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")).forGetter(r -> r.grid),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Tool.CODEC.listOf().optionalFieldOf("tool", List.of()).forGetter(r -> r.tools),
                Fasteners.Need.CODEC.optionalFieldOf("fasteners", Fasteners.Need.DEFAULT).forGetter(r -> r.need),
                Codec.STRING.optionalFieldOf("school", "").forGetter(r -> r.school),
                Codec.STRING.optionalFieldOf("minigame").forGetter(r -> r.minigame),
                Codec.STRING.listOf().optionalFieldOf("fit_order", List.of()).forGetter(r -> r.fitOrder)).apply(i, EngineeringRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, EngineeringRecipe> STREAM_CODEC = StreamCodec.of(Serializer::write, Serializer::read);

        @Override
        public MapCodec<EngineeringRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EngineeringRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static EngineeringRecipe read(RegistryFriendlyByteBuf buf) {
            Either<ShapedRecipePattern, List<Ingredient>> grid;
            if (buf.readBoolean())
                grid = Either.left(ShapedRecipePattern.STREAM_CODEC.decode(buf));
            else
                grid = Either.right(Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf));
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            List<Tool> tools = Tool.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            Fasteners.Need need = Fasteners.Need.STREAM_CODEC.decode(buf);
            String school = buf.readUtf();
            Optional<String> minigame = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
            List<String> fitOrder = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);
            return new EngineeringRecipe(grid, result, new ArrayList<>(tools), need, school, minigame, fitOrder);
        }

        private static void write(RegistryFriendlyByteBuf buf, EngineeringRecipe r) {
            buf.writeBoolean(r.grid.left().isPresent());
            r.grid.ifLeft(p -> ShapedRecipePattern.STREAM_CODEC.encode(buf, p))
                    .ifRight(l -> Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, l));
            ItemStack.STREAM_CODEC.encode(buf, r.result);
            Tool.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.tools);
            Fasteners.Need.STREAM_CODEC.encode(buf, r.need);
            buf.writeUtf(r.school);
            buf.writeBoolean(r.minigame.isPresent());
            r.minigame.ifPresent(buf::writeUtf);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, r.fitOrder);
        }
    }
}
