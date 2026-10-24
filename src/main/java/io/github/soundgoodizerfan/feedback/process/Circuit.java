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

import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Testable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Basic Electronic Circuit (mechanics 6.21): a part that carries properties, not a tier. It is
 * the data component on the circuit item, read off a soldered {@link Board} when its joints are
 * graded, and it is what later machines eat as a quality input.
 *
 * <h2>The accessor</h2>
 * A machine or a modular-item role that wants a circuit's quality calls {@link #of} on the stack it
 * holds and reads the figures it cares about; no circuit is {@link #NONE}, which reads as zero
 * elements, zero speed and full noise, so "works without one, just worse" is a number and not a
 * branch. Nothing may ask whether a circuit is present in order to refuse to run.
 *
 * <h2>The figures</h2>
 * <ul>
 * <li>{@code elements}: switching elements on the board (relays, coherers, rectifying crystals).
 * Zero is a dead part.</li>
 * <li>{@code speed} (0-1): the fastest element's. A crystal is fast in proportion to its
 * {@code rectifying}; a relay's contacts and a coherer's grains are slow.</li>
 * <li>{@code noise} (0-1, never below {@link FTuning#CIRCUIT_NOISE_FLOOR}): the elements' own, the
 * trace route's, and a cold joint's, cut by a ringing clock plate in proportion to its
 * {@code piezo_coupling}. Lower is better; it multiplies the spread of whatever the circuit
 * conditions.</li>
 * <li>{@code joint}: how the joints came out ({@link Board.Joint#GOOD} or {@code COLD}).</li>
 * <li>{@code limitTu}, {@code ratingAmps}: where the traces lift and what they carry, taken from
 * the board's materials and route when built, so a consumer needs no material lookup.</li>
 * </ul>
 *
 * <h2>Overrun, sense, stop</h2>
 * Heat past {@code limitTu} lifts the traces (the circuit goes dead, {@link #scorch}); current past
 * {@code ratingAmps} through a consumer fuses an element ({@link #fused}). Sense: the component
 * tester (this is a {@link Testable}) and a thermometer on whatever heats it. Stop: take it out of
 * the heat or off the current. Neither is wear by uptime.
 */
public record Circuit(int elements, float speed, float noise, Board.Joint joint, float limitTu, float ratingAmps)
        implements Testable {

    /** No circuit at all: what a machine reads when none is fitted. */
    public static final Circuit NONE = new Circuit(0, 0f, 1f, Board.Joint.NONE, 0f, 0f);

    public static final Codec<Circuit> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("elements").forGetter(Circuit::elements),
            Codec.FLOAT.fieldOf("speed").forGetter(Circuit::speed),
            Codec.FLOAT.fieldOf("noise").forGetter(Circuit::noise),
            Board.Joint.CODEC.fieldOf("joint").forGetter(Circuit::joint),
            Codec.FLOAT.fieldOf("limit_tu").forGetter(Circuit::limitTu),
            Codec.FLOAT.fieldOf("rating_amps").forGetter(Circuit::ratingAmps)).apply(i, Circuit::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Circuit> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** What a stack carries: its circuit, or {@link #NONE}. This is the read helper a consumer calls. */
    public static Circuit of(ItemStack stack) {
        Circuit c = stack.get(FDataComponents.CIRCUIT.get());
        return c == null ? NONE : c;
    }

    /** A circuit whose traces lifted or whose elements all fused: kept as an item, reads as nothing. */
    public static Circuit dead(Board.Joint joint) {
        return new Circuit(0, 0f, 1f, joint, 0f, 0f);
    }

    public boolean live() {
        return elements > 0;
    }

    /** One element fused by over-current; the last one makes it a dead part. */
    public Circuit fused() {
        return elements <= 1 ? dead(joint) : new Circuit(elements - 1, speed, noise, joint, limitTu, ratingAmps);
    }

    /**
     * Heat the traces cannot take lifts them: the stack's circuit goes dead. Call wherever a
     * circuit is handled (an inventory tick, a machine taking it in); the temperature is the
     * stack's own {@link ItemHeat}, so any route that heats it counts.
     */
    public static void scorch(ItemStack stack, Level level) {
        Circuit c = of(stack);
        if (c.live() && ItemHeat.get(stack, level).value() > c.limitTu)
            stack.set(FDataComponents.CIRCUIT.get(), dead(c.joint));
    }

    /** What kind of part a stack is when mounted on a board, if any. By tag and material property, never by id. */
    public static Board.Part part(ItemStack stack, Material.Located found) {
        if (stack.is(FTags.CIRCUIT_RELAYS))
            return new Board.Part(Board.Kind.RELAY, 1f);
        if (stack.is(FTags.CIRCUIT_COHERERS))
            return new Board.Part(Board.Kind.COHERER, 1f);
        if (found == null)
            return null;
        float piezo = found.material().electrical().piezoCoupling().orElse(0f);
        if (found.form().equals("crystal_plate") && piezo >= FTuning.CIRCUIT_CLOCK_MIN_PIEZO)
            return new Board.Part(Board.Kind.CLOCK, piezo);
        float rectifying = found.material().electrical().rectifying().orElse(0f);
        return rectifying > 0f ? new Board.Part(Board.Kind.SWITCH, rectifying) : null;
    }

    /** Read a soldered board into a circuit. A board with no switching element comes out dead. */
    public static Circuit assemble(Board board, Board.Joint joint, Map<ResourceLocation, Material> materials) {
        return assemble(board, joint, materials, 0);
    }

    /** As above, with {@code defects} cold or bridged pads each adding {@link FTuning#CIRCUIT_DEFECT_NOISE} (mechanics 6.22). */
    public static Circuit assemble(Board board, Board.Joint joint, Map<ResourceLocation, Material> materials, int defects) {
        int elements = 0;
        float speed = 0f, noise = 0f, steady = 0f;
        for (Board.Part p : board.parts()) {
            switch (p.kind()) {
                case SWITCH -> {
                    elements++;
                    speed = Math.max(speed, FTuning.CIRCUIT_CRYSTAL_SPEED * p.figure());
                    noise += FTuning.CIRCUIT_CRYSTAL_NOISE + (1f - p.figure()) * FTuning.CIRCUIT_CRYSTAL_POOR_NOISE;
                }
                case RELAY -> {
                    elements++;
                    speed = Math.max(speed, FTuning.CIRCUIT_RELAY_SPEED);
                    noise += FTuning.CIRCUIT_RELAY_NOISE;
                }
                case COHERER -> {
                    elements++;
                    speed = Math.max(speed, FTuning.CIRCUIT_COHERER_SPEED);
                    noise += FTuning.CIRCUIT_COHERER_NOISE;
                }
                case CLOCK -> steady = Math.max(steady, p.figure());
            }
        }
        if (elements == 0)
            return dead(joint);
        noise = noise / elements
                + (board.trace() == Board.Trace.PRINTED ? FTuning.CIRCUIT_PRINTED_NOISE : FTuning.CIRCUIT_ETCHED_NOISE)
                + (joint == Board.Joint.COLD ? FTuning.CIRCUIT_COLD_JOINT_NOISE : 0f)
                + defects * FTuning.CIRCUIT_DEFECT_NOISE;
        noise *= 1f - FTuning.CIRCUIT_CLOCK_STEADY * steady;
        float rating = board.trace() == Board.Trace.PRINTED ? FTuning.PRINTED_RATING_AMPS : FTuning.ETCHED_RATING_AMPS;
        return new Circuit(elements, speed, Math.max(FTuning.CIRCUIT_NOISE_FLOOR, Math.min(1f, noise)), joint,
                Soldering.limit(board, materials), rating);
    }

    @Override
    public void test(Instrument tester, Consumer<Component> out) {
        if (!live()) {
            out.accept(Component.translatable("feedback.circuit.dead"));
            return;
        }
        out.accept(Component.translatable("feedback.circuit.elements", elements));
        out.accept(Testable.response(tester, speed));
        out.accept(Testable.noise(tester, noise));
        out.accept(Component.translatable("feedback.board.joint", Component.translatable(joint.translationKey())));
    }
}
