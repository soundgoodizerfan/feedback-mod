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
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a set of materials becomes when held in a temperature band for long enough.
 *
 * <h2>The same claim {@link Deformation} makes</h2>
 * This is a property of the materials, not a recipe belonging to a machine. The crucible never
 * consults a list of things it can make; it holds items at whatever temperature its fire and its
 * mass produce, and what happens to them is their own business. A crucible asked what it makes
 * has no answer, which is correct.
 *
 * <h2>Completion is TPu, not elapsed time</h2>
 * {@code tpu_spec_doc.md} replaces a flat {@code holdTicks} with {@code requiredTpu}: a process
 * needs a quantity of progress, not a duration, and how fast that progress accrues is a property
 * of the current thermal conditions rather than the recipe. {@code requiredTpu} is expressed in
 * the same units the old field was -- ticks-equivalent, at perfect conditions -- so a process
 * held exactly at {@link #optimalTemperature} completes in exactly {@code requiredTpu} ticks, the
 * same number that used to be a flat countdown. Drift off the optimum and it takes longer; drift
 * out of the band entirely and it starts giving ground back (see {@link #suitability}). This is
 * what lets a better vessel, a hotter fire, or simply better aim finish the same transformation
 * faster, which a flat tick count could never express.
 *
 * <h2>Four numbers, four different kinds of failure</h2>
 * Philosophy 6 wants overrun to have a character rather than to be a timer running out, and the
 * shape of that is in these fields:
 * <ul>
 *   <li><b>Below {@code minTemperature}</b> -- no TPu accumulates, and what has already been
 *       gained decays gently ({@code FTuning.TPU_DECAY_PER_TICK}) rather than resetting outright.
 *       The player may fail safely in this direction all day; they only pay for how long they
 *       stayed there.</li>
 *   <li><b>Above {@code maxTemperature}</b> -- the same decay, but now they are climbing towards
 *       the expensive direction and have no way of knowing how close they are.</li>
 *   <li><b>Above {@code spoilTemperature}</b> -- the inputs are destroyed, immediately, and turn
 *       into {@code spoiled}. This is the overrun, and it is the same idea as a plate being
 *       hammered into foil: the machine did not stop, so it kept doing what it does.</li>
 *   <li><b>Changing faster than {@code maxRateTuPerTick}, in either direction</b> -- the process
 *       will not take, and accumulated TPu decays the same as being out of band. Nothing is
 *       destroyed; the player simply cannot work out why it is not working, which is the one
 *       failure that instruments genuinely fix. It is also what makes a large vessel
 *       <em>necessary</em> rather than merely nicer: a small crucible on a full fire climbs at
 *       29 Tu/t and can never satisfy a 25 Tu/t limit. Symmetrically, annealing's slow-cool
 *       requirement is this same bound applied to the way down.</li>
 * </ul>
 *
 * <h2>Tempering: a hold that only counts on the way down</h2>
 * {@code requireCooling} is the one thing tempering needed that carburizing did not. Philosophy 13
 * says an actuator is a switch, never a dial, so there is no rate to store here -- the only lever
 * a Damper gives the player is on/off, timed by hand or by a controller, exactly like the Bellows/
 * Bimetallic-Strip thermostat. So the process asks for a direction instead of a rate: while this is
 * true, a tick where the body is flat or heating neither accumulates nor decays TPu -- paused, not
 * lost, unlike the out-of-band case above -- until the body is actually cooling in-band. The
 * player still has to get it there by reheating past the band first and then shutting the fire off
 * <p>
 * That "no rate to store" is about the <em>actuator</em> -- a Damper stays on/off regardless.
 * The <em>recipe</em> is a different contract: annealing pairs {@code requireCooling} with a tight
 * {@link #maxRateTuPerTick}, so a process can still demand a slow cool without the mod ever
 * inventing a rate the player dials. They get there by timing the same on/off switch more
 * patiently -- a thicker vessel or a slower damper cycle, not a new kind of control.
 * or opening a Damper; nothing new is needed for that half, it falls out of the existing fire/leak
 * model.
 *
 * @param inputs              everything that must be present. Ingredients, so tags work and no
 *                            identity is ever named.
 * @param minTemperature      bottom of the band, in Tu. For a melt, this is the melt point and
 *                            the only number that matters -- see the class doc.
 * @param optimalTemperature  where TPu accumulates fastest (suitability 1.0), in Tu.
 * @param maxTemperature      top of the band, in Tu.
 * @param requiredTpu         how much progress the transformation needs, in ticks-equivalent at
 *                            {@link #optimalTemperature}. {@link #NO_HOLD} for a pooled
 *                            vanilla-fallback card with no honest figure to publish; {@code 0}
 *                            (a melt) completes the instant it is in band, regardless of
 *                            suitability.
 * @param maxRateTuPerTick    fastest the temperature may be changing, in either direction, while
 *                            TPu accumulates. Originally heating-only ({@code max_heating}),
 *                            widened when annealing turned up needing the same bound on the way
 *                            down: a cool that outruns this is a quench, not an anneal, and the
 *                            physical mistake is the same shape either direction -- one field
 *                            covers both rather than a second one covering the mirror case. See
 *                            §3.3's own note on {@code CrucibleBlockEntity}/{@code
 *                            ThermalVesselBlockEntity} disagreeing about a shared rule; this was a
 *                            second instance the audit that widened this field turned up (the
 *                            crucible checked heating only, the vessel already checked both ways
 *                            by accident) rather than a new design decision.
 * @param result              what the inputs become, if it is an item. Empty for a melt, where
 *                            {@link #resultFluid} is the real output instead.
 * @param spoilTemperature    above this the batch is ruined. Same as {@code maxTemperature} would
 *                            make the safe direction unsafe, so it always sits well above it.
 * @param spoiled             what is left when it is.
 * @param resultFluid         what the inputs become, if it is a fluid -- a melt. Empty for every
 *                            ordinary process. Never both this and {@link #result} at once.
 * @param requireCooling      tempering's flag -- see above. False for every ordinary process.
 *                            Annealing is this same flag plus a tight {@link #maxRateTuPerTick}:
 *                            no new mechanic, just both existing knobs turned at once.
 */
public record ThermalProcess(List<Ingredient> inputs,
                             Tu minTemperature,
                             Tu optimalTemperature,
                             Tu maxTemperature,
                             float requiredTpu,
                             TuRate maxRateTuPerTick,
                             ItemStack result,
                             Tu spoilTemperature,
                             ItemStack spoiled,
                             FluidStack resultFluid,
                             boolean requireCooling) {

    public static final Codec<ThermalProcess> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(ThermalProcess::inputs),
            Units.codec(Tu::new).fieldOf("min_temperature").forGetter(ThermalProcess::minTemperature),
            Units.codec(Tu::new).fieldOf("optimal_temperature").forGetter(ThermalProcess::optimalTemperature),
            // Open by default -- a melt has a floor and nothing else, the same open-ended shape
            // the pooled vanilla-fallback cards already draw as "800+ Tu" (see
            // ThermalProcessCategory). An ordinary band-and-hold process still states both ends.
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(ThermalProcess::maxTemperature),
            Codec.FLOAT.fieldOf("required_tpu").forGetter(ThermalProcess::requiredTpu),
            Units.codec(TuRate::new).optionalFieldOf("max_rate", new TuRate(Float.MAX_VALUE)).forGetter(ThermalProcess::maxRateTuPerTick),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(ThermalProcess::result),
            Units.codec(Tu::new).optionalFieldOf("spoil_temperature", new Tu(Float.MAX_VALUE)).forGetter(ThermalProcess::spoilTemperature),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("spoiled", ItemStack.EMPTY).forGetter(ThermalProcess::spoiled),
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("result_fluid", FluidStack.EMPTY).forGetter(ThermalProcess::resultFluid),
            Codec.BOOL.optionalFieldOf("require_cooling", false).forGetter(ThermalProcess::requireCooling)
    ).apply(instance, ThermalProcess::new));

    /**
     * For printing on the client. Philosophy 8: a requirement is published data and costs nothing.
     * <p>
     * Written out rather than composed, because {@code StreamCodec.composite} stops at six fields
     * and this record has eight. Splitting the record to fit a helper would be letting the wire
     * format dictate the design.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ThermalProcess> STREAM_CODEC =
            new StreamCodec<>() {

                private static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INPUTS =
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list());

                @Override
                public ThermalProcess decode(RegistryFriendlyByteBuf buffer) {
                    return new ThermalProcess(
                            INPUTS.decode(buffer),
                            new Tu(buffer.readFloat()),
                            new Tu(buffer.readFloat()),
                            new Tu(buffer.readFloat()),
                            buffer.readFloat(),
                            new TuRate(buffer.readFloat()),
                            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                            new Tu(buffer.readFloat()),
                            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                            FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                            buffer.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ThermalProcess process) {
                    INPUTS.encode(buffer, process.inputs());
                    buffer.writeFloat(process.minTemperature().value());
                    buffer.writeFloat(process.optimalTemperature().value());
                    buffer.writeFloat(process.maxTemperature().value());
                    buffer.writeFloat(process.requiredTpu());
                    buffer.writeFloat(process.maxRateTuPerTick().tuPerTick());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, process.result());
                    buffer.writeFloat(process.spoilTemperature().value());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, process.spoiled());
                    FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, process.resultFluid());
                    buffer.writeBoolean(process.requireCooling());
                }
            };

    public boolean inBand(Tu tu) {
        return tu.value() >= minTemperature.value() && tu.value() <= maxTemperature.value();
    }

    public boolean spoilsAt(Tu tu) {
        return tu.value() > spoilTemperature.value();
    }

    /**
     * How fast TPu accumulates at {@code tu}, as a fraction of the rate at {@link
     * #optimalTemperature} -- 0 outside the band, ramping up to 1.0 at the optimum and back down
     * to 0 at the far edge. Philosophy 8's "quality should fall off gracefully, not switch off":
     * the same triangular shape as the worked example in {@code tpu_spec_doc.md}, kept to three
     * points (min/optimal/max) rather than the doc's illustrative six -- the exact curve is
     * explicitly a tuning decision there, and three points is the minimum that has a peak at all.
     */
    public static float suitability(float tu, float min, float optimal, float max) {
        if (tu <= min || tu >= max)
            return 0f;
        return tu <= optimal ? (tu - min) / (optimal - min) : (max - tu) / (max - optimal);
    }

    public float suitability(Tu tu) {
        return suitability(tu.value(), minTemperature.value(), optimalTemperature.value(), maxTemperature.value());
    }

    /**
     * {@code requiredTpu} for a process the JEI plugin synthesises from a pooled vanilla/modded
     * cooking recipe rather than loading from a file -- see {@code FeedbackJeiPlugin.pooled}.
     * Neither food's plain-tick baseline nor metal's is "stay in this band for N ticks" once
     * either is scaled by a live suitability curve the display can't animate, so there is no
     * honest single number to put here and the card knows to leave the row off instead of
     * printing one that was never true.
     */
    public static final float NO_HOLD = -1f;

    public boolean hasHold() {
        return requiredTpu >= 0f;
    }

    /** Whether this entry is a melt -- its output is a fluid rather than an item. */
    public boolean hasFluidResult() {
        return !resultFluid.isEmpty();
    }
}
