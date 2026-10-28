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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.process.Operation;
import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;

import io.github.soundgoodizerfan.feedback.process.Plating;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Feedback.MOD_ID);

    /**
     * Cumulative mechanical work beaten into this stack, in Fu.
     * <p>
     * Lives on the item rather than in the machine, so a half-worked ingot stays half-worked when
     * it is carried out, dropped, or put in a chest. Philosophy 9: material carries its own
     * history between machines, and the gap between two machines is part of the process.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WORK =
            COMPONENTS.register("work", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * A fluid stack's temperature in Tu (mechanics 5.9): the stamp a tank, pipe or handler carries
     * with the fluid, so steam arrives as hot as it left. Absent reads as ambient. Not an energy
     * and not a quantity of heat: a state, like an item's.
     */
    /** A crude's grade, riding on the fluid stack like its temperature: see {@code CrudeGrade}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.geology.CrudeGrade>> CRUDE_GRADE =
            COMPONENTS.register("crude_grade", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.geology.CrudeGrade>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.geology.CrudeGrade.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.geology.CrudeGrade.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> FLUID_TEMPERATURE =
            COMPONENTS.register("fluid_temperature", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /** Charge a Charge-attuned tool has built from its blows, in blows' worth (roster 16). Never above what its level holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> CHARGE_STORED =
            COMPONENTS.register("charge_stored", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /** Experience points a Reservoir-attuned tool holds as polarization (roster 15). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RESERVOIR_XP =
            COMPONENTS.register("reservoir_xp", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Volts a jar baton holds, drawn from a Leyden jar and spent whole on one hit. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> BATON_VOLTS =
            COMPONENTS.register("baton_volts", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /**
     * Fu this workpiece needs before it becomes the next thing.
     * <p>
     * Stored on the stack next to {@link #WORK} so the two travel together. That is what lets a
     * tooltip say "nearly there" without the client knowing the deformation table, and it is the
     * same pair the calipers will eventually report as {@code 16 / 20 Fu}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WORK_REQUIRED =
            COMPONENTS.register("work_required", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * Which {@link io.github.soundgoodizerfan.feedback.process.Operation} {@link #WORK} was
     * accrued under.
     * <p>
     * A blow and a draw are physically different actions on the material, so progress does not
     * carry between them. If a workpiece switches operation mid-way, {@code Deforming} forfeits
     * whatever {@link #WORK} it was holding rather than crediting it toward the new operation's
     * entry -- the honest reading of "you cannot half-hammer something, then finish it by
     * drawing."
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Operation>> WORK_OPERATION =
            COMPONENTS.register("work_operation", () -> DataComponentType.<Operation>builder()
                    // Persisted by name: ordinals shift whenever the enum does (REMOVE's split
                    // into TURN/DRILL/GRIND already did once). Older saves held an int; those read
                    // as BLOW, which at worst forfeits one workpiece's partial progress.
                    .persistent(Codec.withAlternative(Operation.CODEC, Codec.INT.xmap(i -> Operation.BLOW, op -> 0)))
                    .networkSynchronized(Operation.STREAM_CODEC)
                    .build());

    /**
     * How sound a physical tool bit still is, 1 down to whatever floor the tool defines --
     * {@code Wearing}'s condition figure, but living on the bit item itself rather than on a
     * machine block. The Lathe's tool bit is the second thing in the mod that wears (the Hammer's
     * head is the first, and lives as a plain field since a Hammer is not a swappable component);
     * the bit is a physical, replaceable part per philosophy 4, so its condition has to travel
     * with the item, not with whichever Lathe it happens to be mounted in.
     */
    // Also carried by a mined machine block (hammer, pulley, shop machines), so its wear survives
    // being picked up -- the same figure, on the item that is the machine.
    /** How much of a galvanic cell's anode is left, 0 to 1: a mined cell keeps what it has eaten. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ANODE_REMAINING =
            COMPONENTS.register("anode_remaining", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> TOOL_CONDITION =
            COMPONENTS.register("tool_condition", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /**
     * The temperature this stack was last set to, in Tu -- not its temperature now.
     *
     * <h3>Why a stamp and not a reading</h3>
     * Paired with {@link #HEATED_AT}, and the pair is the whole of how a workpiece cools in a
     * chest nobody is ticking. See {@link io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat}: the
     * current figure is computed from these two whenever it is asked for, so no code anywhere has
     * to remember to cool anything. Read this field directly and you will get a number that was
     * true some minutes ago.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> TEMPERATURE =
            COMPONENTS.register("temperature", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /** How hard a hand tool is being swung; chosen by sneak-scroll. Absent is an ordinary blow. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.item.HandSwing>> HAND_SWING =
            COMPONENTS.register("hand_swing", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.item.HandSwing>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.item.HandSwing.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.item.HandSwing.STREAM_CODEC)
                    .build());

    /** The game tick {@link #TEMPERATURE} was stamped on. Meaningless without it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HEATED_AT =
            COMPONENTS.register("heated_at", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    /**
     * What a mold is currently holding -- see {@code MoldItem}. Absent or empty means the mold
     * is empty; the mold's own {@link #TEMPERATURE}/{@link #HEATED_AT} pair (it is a workpiece
     * like any other) says whether its contents have cooled enough to cast.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> MOLTEN_CONTENT =
            COMPONENTS.register("molten_content", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .build());

    /** What a copper bucket holds -- see {@code CopperBucketItem}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> BUCKET_CONTENT =
            COMPONENTS.register("bucket_content", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .build());

    /**
     * A printed Punch Card's whole program -- see {@code control/program}. Empty on a blank
     * card; printing writes the finished graph here once (spec's hard-rewrite tier).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProgramGraph>> PROGRAM_GRAPH =
            COMPONENTS.register("program_graph", () -> DataComponentType.<ProgramGraph>builder()
                    .persistent(ProgramGraph.CODEC)
                    .networkSynchronized(ProgramGraph.STREAM_CODEC)
                    .build());

    /** A coat electroplated onto the item -- see {@code process/Plating}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Plating>> PLATING =
            COMPONENTS.register("plating", () -> DataComponentType.<Plating>builder()
                    .persistent(Plating.CODEC)
                    .networkSynchronized(Plating.STREAM_CODEC)
                    .build());

    /** A substrate with traces and joints. See {@code process/Board}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.Board>> BOARD =
            COMPONENTS.register("board", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.Board>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.Board.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.Board.STREAM_CODEC)
                    .build());

    /** The properties of a soldered circuit. See {@code process/Circuit}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.Circuit>> CIRCUIT =
            COMPONENTS.register("circuit", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.Circuit>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.Circuit.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.Circuit.STREAM_CODEC)
                    .build());

    /** How far a crystal plate is ground; absent is the first cut. See {@code process/Cut}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.Cut>> CUT =
            COMPONENTS.register("cut", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.Cut>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.Cut.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.Cut.STREAM_CODEC)
                    .build());

    /** How sound a mass probe's crystal still is, 0-1; absent is as new. See {@code item/MassProbeItem}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> PROBE_CONDITION =
            COMPONENTS.register("probe_condition", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /** What a shaped part is made of -- see {@code process/MadeOf}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.MadeOf>> MADE_OF =
            COMPONENTS.register("made_of", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.MadeOf>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.MadeOf.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.MadeOf.STREAM_CODEC)
                    .build());

    /** The voltage band the insulation on a cable holds off; absent on a bare one -- see {@code machine/cable/CableBlockEntity}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.core.unit.VoltageBand>> INSULATION =
            COMPONENTS.register("insulation", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.core.unit.VoltageBand>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.STREAM_CODEC)
                    .build());

    /** What a pair of tongs is made of: arms and grip, both materials -- see {@code item/Tongs}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.item.Tongs>> TONGS =
            COMPONENTS.register("tongs", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.item.Tongs>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.item.Tongs.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.item.Tongs.STREAM_CODEC)
                    .build());

    /** What a piece of combat gear is made of and what that made of it -- see {@code combat/gear/GearStats}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.combat.gear.GearStats>> GEAR =
            COMPONENTS.register("gear", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.combat.gear.GearStats>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.combat.gear.GearStats.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.combat.gear.GearStats.STREAM_CODEC)
                    .build());

    /** Technique ids a worn curio grants; queried through {@code CombatGear.grants}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<net.minecraft.resources.ResourceLocation>>> GRANTS =
            COMPONENTS.register("grants", () -> DataComponentType.<java.util.List<net.minecraft.resources.ResourceLocation>>builder()
                    .persistent(net.minecraft.resources.ResourceLocation.CODEC.listOf())
                    .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC
                            .apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .build());

    /** A cooked, cooking or spoiling food's state -- see {@code process/FoodState}. Absent is fresh and raw by its material. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.FoodState>> FOOD_STATE =
            COMPONENTS.register("food_state", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.FoodState>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.FoodState.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.FoodState.STREAM_CODEC)
                    .build());

    /** How a food was cut at the Prep Table -- see {@code process/FoodCut}. Absent is whole. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.FoodCut>> FOOD_CUT =
            COMPONENTS.register("food_cut", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.FoodCut>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.FoodCut.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.FoodCut.STREAM_CODEC)
                    .build());

    /** What a dish was baked to before spoilage -- see {@code process/DishFigures}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.DishFigures>> DISH_FIGURES =
            COMPONENTS.register("dish_figures", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.DishFigures>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.DishFigures.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.DishFigures.STREAM_CODEC)
                    .build());

    /** How a dough was kneaded at the Prep Table -- see {@code process/FoodKnead}. Absent is unkneaded. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.process.FoodKnead>> FOOD_KNEAD =
            COMPONENTS.register("food_knead", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.process.FoodKnead>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.process.FoodKnead.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.process.FoodKnead.STREAM_CODEC)
                    .build());

    /** A ladled meal's dish and what is live in it -- see {@code item/Meal}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.item.Meal>> MEAL =
            COMPONENTS.register("meal", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.item.Meal>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.item.Meal.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.item.Meal.STREAM_CODEC)
                    .build());

    /** What a jar holds and how far canning has taken it -- see {@code item/Canned}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.item.Canned>> CANNED =
            COMPONENTS.register("canned", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.item.Canned>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.item.Canned.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.item.Canned.STREAM_CODEC)
                    .build());

    private FDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
