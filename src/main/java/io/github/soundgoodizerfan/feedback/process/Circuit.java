// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
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
 * The circuit: a part that carries properties, not a tier. It is the data component on the circuit
 * item, read off a soldered {@link Board} when its joints are graded, and it is what hosts eat as a
 * quality input. One item, one record: a board's elements decide which figures are non-zero (a
 * relay, coherer or crystal board has no gain; a board with a tube does), and a circuit has no
 * failure mode of its own beyond the two overrun paths below.
 *
 * <h2>The accessor</h2>
 * A machine or a modular-item role that wants a circuit's quality calls {@link #of} on the stack it
 * holds and reads the figures it cares about; no circuit is {@link #NONE}, which reads as zero
 * elements, zero speed and full noise, so "works without one, just worse" is a number and not a
 * branch. Whether a host refuses to run for lack of a circuit is that host's own rule, stated in its
 * dev doc: hosts that take a recipe data stick may gate, every other host works without it, only worse.
 *
 * <h2>The figures</h2>
 * <ul>
 * <li>{@code elements}: switching elements on the board (relays, coherers, rectifying crystals,
 * tubes). Zero is a dead part.</li>
 * <li>{@code speed} (0-1): the fastest element's, at working warmth. A crystal is fast in proportion to its
 * {@code rectifying}; a relay's contacts and a coherer's grains are slow; a tube's follows its geometry.</li>
 * <li>{@code noise} (0-1, never below {@link ProcessTuning#CIRCUIT_NOISE_FLOOR}): the elements' own, the
 * trace route's, and a cold joint's, cut by a ringing clock plate in proportion to its
 * {@code piezo_coupling}. Lower is better; it multiplies the spread of whatever the circuit
 * conditions, and is the read jitter of a Controller's sensors.</li>
 * <li>{@code joint}: how the joints came out ({@link Board.Joint#GOOD} or {@code COLD}).</li>
 * <li>{@code limitTu}, {@code ratingAmps}: where the traces lift and what they carry, taken from
 * the board's materials and route when built, so a consumer needs no material lookup.</li>
 * <li>{@code gain}: what the tubes add as amplifiers, summed; 0 without a tube.</li>
 * <li>{@code power}: the standing heater power of the tubes, Eu a tick, summed; the host pays it
 * (a Controller draws it from its line), the circuit has no load of its own.</li>
 * <li>{@code warmTu}, {@code warmRate}: the bulb temperature the tubes work at (0 with none: nothing
 * to warm) and how fast one Eu a tick of heater raises it. A tubed circuit is read through its
 * stack's {@link ItemHeat}: cold it is slow and noisy ({@link #at}), never refused; a host that
 * powers its heater warms it ({@link #warm}); fire warms it too.</li>
 * <li>{@code clockRate}: program steps a second a host takes from it at working warmth
 * ({@link #clockRateOf}); a Controller steps at this rate.</li>
 * </ul>
 *
 * <h2>Overrun, sense, stop</h2>
 * Heat past {@code limitTu} lifts the traces (the circuit goes dead, {@link #scorch}); current past
 * {@code ratingAmps} through a consumer fuses an element ({@link #fused}). A heater stops short of
 * the limit, so a tube on a poor board runs under its working warmth instead of lifting it. Sense: the
 * component tester (this is a {@link Testable}) and a thermometer on whatever heats it. Stop: take it out of
 * the heat or off the current. Neither is wear by uptime.
 */
public record Circuit(int elements, float speed, float noise, Board.Joint joint, float limitTu, float ratingAmps,
                      float gain, float power, float warmTu, float warmRate, float clockRate, Line line, java.util.Optional<Board> board) implements Testable {

    /**
     * The figures of the amethyst and sculk lines, all zero on a board with neither: one record on the one item, the elements decide which are
     * non-zero.
     *
     * @param stability   0-1, a resonator's: how steady a host's clock and sampling run (less step-timing jitter, less read noise).
     * @param selectivity channels a resonator can separate, 0 with none; the endernet upgrade's {@code channels()} reads it.
     * @param bandwidth   0-1, a resonator's: what a Data Port's link carries over its base bandwidth.
     * @param memory      extra program variables a lattice holds on top of the Controller's Memory Drums, 0 to {@link ProcessTuning#CIRCUIT_MEMORY_MAX}.
     * @param retention   0-1, a lattice's: at {@link ProcessTuning#LATTICE_HOLDS_AT} and over it holds its memory with the host unpowered; under it needs a standing supply.
     * @param adaptation  0-1, a lattice's: how firmly a host holds a sensor reading it has seen repeat (it filters read noise).
     */
    public record Line(float stability, int selectivity, float bandwidth, int memory, float retention, float adaptation) {
        public static final Line NONE = new Line(0f, 0, 0f, 0, 0f, 0f);

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("stability").forGetter(Line::stability),
                Codec.INT.fieldOf("selectivity").forGetter(Line::selectivity),
                Codec.FLOAT.fieldOf("bandwidth").forGetter(Line::bandwidth),
                Codec.INT.fieldOf("memory").forGetter(Line::memory),
                Codec.FLOAT.fieldOf("retention").forGetter(Line::retention),
                Codec.FLOAT.fieldOf("adaptation").forGetter(Line::adaptation)).apply(i, Line::new));

        /** Whether a lattice's memory survives the host losing power. */
        public boolean holds() {
            return memory > 0 && retention >= ProcessTuning.LATTICE_HOLDS_AT;
        }

        public boolean any() {
            return !equals(NONE);
        }
    }

    /** A circuit with no amethyst or sculk element on it. */
    public Circuit(int elements, float speed, float noise, Board.Joint joint, float limitTu, float ratingAmps,
                   float gain, float power, float warmTu, float warmRate, float clockRate) {
        this(elements, speed, noise, joint, limitTu, ratingAmps, gain, power, warmTu, warmRate, clockRate, Line.NONE);
    }

    /** A circuit that does not remember the board it was soldered from (a bare figure set: tests, JEI examples). */
    public Circuit(int elements, float speed, float noise, Board.Joint joint, float limitTu, float ratingAmps,
                   float gain, float power, float warmTu, float warmRate, float clockRate, Line line) {
        this(elements, speed, noise, joint, limitTu, ratingAmps, gain, power, warmTu, warmRate, clockRate, line, java.util.Optional.empty());
    }

    public float stability() {
        return line.stability();
    }

    public int selectivity() {
        return line.selectivity();
    }

    public float bandwidth() {
        return line.bandwidth();
    }

    public int memory() {
        return line.memory();
    }

    public float retention() {
        return line.retention();
    }

    public float adaptation() {
        return line.adaptation();
    }

    /** No circuit at all: what a machine reads when none is fitted. */
    public static final Circuit NONE = new Circuit(0, 0f, 1f, Board.Joint.NONE, 0f, 0f, 0f, 0f, 0f, 0f, clockRateOf(0f));

    public static final Codec<Circuit> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("elements").forGetter(Circuit::elements),
            Codec.FLOAT.fieldOf("speed").forGetter(Circuit::speed),
            Codec.FLOAT.fieldOf("noise").forGetter(Circuit::noise),
            Board.Joint.CODEC.fieldOf("joint").forGetter(Circuit::joint),
            Codec.FLOAT.fieldOf("limit_tu").forGetter(Circuit::limitTu),
            Codec.FLOAT.fieldOf("rating_amps").forGetter(Circuit::ratingAmps),
            Codec.FLOAT.fieldOf("gain").forGetter(Circuit::gain),
            Codec.FLOAT.fieldOf("power").forGetter(Circuit::power),
            Codec.FLOAT.fieldOf("warm_tu").forGetter(Circuit::warmTu),
            Codec.FLOAT.fieldOf("warm_rate").forGetter(Circuit::warmRate),
            Codec.FLOAT.fieldOf("clock_rate").forGetter(Circuit::clockRate),
            Line.CODEC.optionalFieldOf("line", Line.NONE).forGetter(Circuit::line),
            Board.CODEC.optionalFieldOf("board").forGetter(Circuit::board)).apply(i, Circuit::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Circuit> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** What a stack carries: its circuit, or {@link #NONE}. This is the read helper a consumer calls. */
    public static Circuit of(ItemStack stack) {
        Circuit c = stack.get(FDataComponents.CIRCUIT.get());
        return c == null ? NONE : c;
    }

    /** What a stack carries, read at the temperature it is at now: a tubed circuit's speed, noise and clock follow how warm its bulb is ({@link #at}). */
    public static Circuit of(ItemStack stack, Level level) {
        Circuit c = of(stack);
        return c.tubed() && c.live() ? c.at(c.warmth(stack, level)) : c;
    }

    /** A circuit whose traces lifted or whose elements all fused: kept as an item, reads as nothing. */
    public static Circuit dead(Board.Joint joint) {
        return new Circuit(0, 0f, 1f, joint, 0f, 0f, 0f, 0f, 0f, 0f, clockRateOf(0f));
    }

    /** Program steps a second a clock of this speed gives: the base rate with none, one a tick at speed 1. */
    public static float clockRateOf(float speed) {
        return ProcessTuning.CIRCUIT_BASE_STEPS_PER_SECOND
                + Math.max(0f, Math.min(1f, speed)) * (ProcessTuning.CIRCUIT_MAX_STEPS_PER_SECOND - ProcessTuning.CIRCUIT_BASE_STEPS_PER_SECOND);
    }

    /** Whether the board carries a tube, so its figures wait on a heater. */
    public boolean tubed() {
        return warmTu > 0f;
    }

    /** How far this circuit's bulb has come toward working warmth, 0 to 1: its stack's {@link ItemHeat}, read where the stack is. 1 for a circuit with no tube. */
    public float warmth(ItemStack stack, Level level) {
        if (!tubed())
            return 1f;
        return Tube.warmth(ItemHeat.get(stack, level).value(), warmTu, ThermalTuning.AMBIENT_TU.value());
    }

    /** These figures at {@code warmth}: a tubed circuit that is cold is slow and noisy, in proportion; a warm one is as built. Never a refusal. */
    public Circuit at(float warmth) {
        if (!tubed() || !live() || warmth >= 1f)
            return this;
        float w = Math.max(0f, warmth);
        float slow = speed * (ProcessTuning.CIRCUIT_COLD_SPEED + (1f - ProcessTuning.CIRCUIT_COLD_SPEED) * w);
        float noisy = Math.min(1f, noise + (1f - w) * ProcessTuning.CIRCUIT_COLD_NOISE);
        // a cold tube amplifies in proportion to its warmth, none at all at ambient
        return new Circuit(elements, slow, noisy, joint, limitTu, ratingAmps, gain * w, power, warmTu, warmRate, clockRateOf(slow), line, board);
    }

    /**
     * A host's heater warms the circuit it holds for {@code ticks} at {@code share} of its full power (1 when the line delivers it
     * all): the bulb rises toward working warmth at {@code power x warmRate}, stopping at {@link ProcessTuning#CIRCUIT_HEATER_LIMIT_SHARE}
     * of the traces' limit. The stack's temperature is its {@link ItemHeat}, so the same warmth comes from a fire, and cools the same way.
     * (Heater temperature as the emission input is PowerGrid's, design only: see {@link Tube}.)
     */
    public static void warm(ItemStack stack, Level level, int ticks, float share) {
        Circuit c = of(stack);
        if (!c.live() || c.power <= 0f || share <= 0f)
            return;
        float target = Math.min(c.warmTu, c.limitTu * ProcessTuning.CIRCUIT_HEATER_LIMIT_SHARE);
        float now = ItemHeat.get(stack, level).value();
        if (now >= target)
            return;
        float next = Math.min(target, now + ticks * share * c.warmRate * c.power);
        // Stamped directly: ItemHeat.set drops anything under the warm line, and a heater has to climb through it.
        stack.set(FDataComponents.TEMPERATURE.get(), next);
        stack.set(FDataComponents.HEATED_AT.get(), level.getGameTime());
    }

    /** The standing heater power, Eu a tick, this circuit costs its host while the host heats it. */
    public float heaterPower() {
        return live() ? power : 0f;
    }

    /** What a fitted circuit does to an instrument's resolution step: 1 for none or dead, down to about half for a quiet one. */
    public float stepFactor() {
        return live() ? 1f - ProcessTuning.CIRCUIT_INSTRUMENT_GAIN * (1f - noise) : 1f;
    }

    public boolean live() {
        return elements > 0;
    }

    /** One element fused by over-current; the last one makes it a dead part. */
    public Circuit fused() {
        return elements <= 1 ? dead(joint) : new Circuit(elements - 1, speed, noise, joint, limitTu, ratingAmps, gain, power, warmTu, warmRate, clockRate, line, board);
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
        Board.Part made = partOf(stack, found);
        return made == null ? null : made.withSource(stack);
    }

    private static Board.Part partOf(ItemStack stack, Material.Located found) {
        // A sealed tube carries its own finished figures; an unsealed one is a bulb of air and mounts as nothing.
        Tube sealed = stack.get(FDataComponents.TUBE.get());
        if (sealed != null)
            return new Board.Part(Board.Kind.TUBE, sealed.vacuum(), java.util.Optional.of(sealed));
        // A resonator or lattice carries the figures its build gave it.
        Element element = stack.get(FDataComponents.ELEMENT.get());
        if (element != null)
            return new Board.Part(element.kind(), 1f, java.util.Optional.empty(), java.util.Optional.of(element.line()));
        if (stack.is(FTags.CIRCUIT_RELAYS))
            return new Board.Part(Board.Kind.RELAY, 1f);
        if (stack.is(FTags.CIRCUIT_COHERERS))
            return new Board.Part(Board.Kind.COHERER, 1f);
        if (found == null)
            return null;
        float piezo = found.material().electrical().piezoCoupling().orElse(0f);
        if (found.form().equals("crystal_plate") && piezo >= ProcessTuning.CIRCUIT_CLOCK_MIN_PIEZO)
            return new Board.Part(Board.Kind.CLOCK, piezo);
        float rectifying = found.material().electrical().rectifying().orElse(0f);
        return rectifying > 0f ? new Board.Part(Board.Kind.SWITCH, rectifying) : null;
    }

    /** Read a soldered board into a circuit. A board with no switching element comes out dead. */
    public static Circuit assemble(Board board, Board.Joint joint, Map<ResourceLocation, Material> materials) {
        return assemble(board, joint, materials, 0);
    }

    /** As above, with {@code defects} cold or bridged pads each adding {@link ProcessTuning#CIRCUIT_DEFECT_NOISE}. */
    public static Circuit assemble(Board board, Board.Joint joint, Map<ResourceLocation, Material> materials, int defects) {
        int elements = 0;
        float speed = 0f, noise = 0f, steady = 0f, gain = 0f, power = 0f, warmTu = 0f, warmRate = Float.MAX_VALUE;
        float stability = 0f, bandwidth = 0f, retention = -1f, adaptation = 0f;
        int selectivity = 0, memory = 0;
        for (Board.Part p : board.parts()) {
            // A cooked part is still on the board but is no element.
            if (p.dead())
                continue;
            switch (p.kind()) {
                case TUBE -> {
                    Tube t = p.tube().orElse(null);
                    if (t != null) {
                        elements++;
                        speed = Math.max(speed, t.speed());
                        noise += t.noise();
                        gain += t.gain();
                        power += t.power();
                        warmTu = Math.max(warmTu, t.warmTu());
                        warmRate = Math.min(warmRate, t.warmRate());
                    }
                }
                case SWITCH -> {
                    elements++;
                    speed = Math.max(speed, ProcessTuning.CIRCUIT_CRYSTAL_SPEED * p.figure());
                    noise += ProcessTuning.CIRCUIT_CRYSTAL_NOISE + (1f - p.figure()) * ProcessTuning.CIRCUIT_CRYSTAL_POOR_NOISE;
                }
                case RELAY -> {
                    elements++;
                    speed = Math.max(speed, ProcessTuning.CIRCUIT_RELAY_SPEED);
                    noise += ProcessTuning.CIRCUIT_RELAY_NOISE;
                }
                case COHERER -> {
                    elements++;
                    speed = Math.max(speed, ProcessTuning.CIRCUIT_COHERER_SPEED);
                    noise += ProcessTuning.CIRCUIT_COHERER_NOISE;
                }
                case CLOCK -> steady = Math.max(steady, p.figure());
                case RESONATOR, LATTICE -> {
                    Line l = p.line().orElse(Line.NONE);
                    elements++;
                    noise += ProcessTuning.ELEMENT_NOISE;
                    steady = Math.max(steady, l.stability());
                    stability = Math.max(stability, l.stability());
                    selectivity = Math.max(selectivity, l.selectivity());
                    bandwidth = Math.max(bandwidth, l.bandwidth());
                    memory += l.memory();
                    if (l.memory() > 0)
                        retention = retention < 0f ? l.retention() : Math.min(retention, l.retention());
                    adaptation = Math.max(adaptation, l.adaptation());
                }
            }
        }
        if (elements == 0)
            return dead(joint);
        noise = noise / elements
                + (board.trace() == Board.Trace.PRINTED ? ProcessTuning.CIRCUIT_PRINTED_NOISE : ProcessTuning.CIRCUIT_ETCHED_NOISE)
                + (joint == Board.Joint.COLD ? ProcessTuning.CIRCUIT_COLD_JOINT_NOISE : 0f)
                + defects * ProcessTuning.CIRCUIT_DEFECT_NOISE;
        noise *= 1f - ProcessTuning.CIRCUIT_CLOCK_STEADY * steady;
        float rating = board.trace() == Board.Trace.PRINTED ? ProcessTuning.PRINTED_RATING_AMPS : ProcessTuning.ETCHED_RATING_AMPS;
        return new Circuit(elements, speed, Math.max(ProcessTuning.CIRCUIT_NOISE_FLOOR, Math.min(1f, noise)), joint,
                Soldering.limit(board, materials), rating, gain, power, warmTu, warmTu > 0f ? warmRate : 0f, clockRateOf(speed),
                new Line(stability, selectivity, bandwidth, Math.min(ProcessTuning.CIRCUIT_MEMORY_MAX, memory), Math.max(0f, retention), adaptation),
                java.util.Optional.of(board));
    }

    private static String fmt(Instrument tester, float value) {
        return io.github.soundgoodizerfan.feedback.instrument.Instruments.format(value, tester.resolution(io.github.soundgoodizerfan.feedback.instrument.Quantity.RESPONSE));
    }

    @Override
    public void test(Instrument tester, Consumer<Component> out) {
        if (!live()) {
            out.accept(Component.translatable("feedback.circuit.dead"));
            return;
        }
        out.accept(Component.translatable("feedback.circuit.elements", elements));
        long cooked = board.map(b -> b.parts().stream().filter(Board.Part::dead).count()).orElse(0L);
        if (cooked > 0)
            out.accept(Component.translatable("feedback.circuit.cooked", cooked));
        out.accept(Testable.response(tester, speed));
        out.accept(Testable.noise(tester, noise));
        if (line.selectivity() > 0 || line.bandwidth() > 0f || line.stability() > 0f) {
            out.accept(Component.translatable("feedback.test.stability", fmt(tester, line.stability())));
            out.accept(Component.translatable("feedback.test.selectivity", fmt(tester, line.selectivity())));
            out.accept(Component.translatable("feedback.test.bandwidth", fmt(tester, line.bandwidth())));
        }
        if (line.memory() > 0 || line.adaptation() > 0f) {
            out.accept(Component.translatable("feedback.test.memory", fmt(tester, line.memory())));
            out.accept(Component.translatable("feedback.test.adaptation", fmt(tester, line.adaptation())));
            out.accept(Component.translatable(line.holds() ? "feedback.circuit.holds" : "feedback.circuit.needs_supply"));
        }
        if (tubed()) {
            out.accept(Component.translatable("feedback.test.gain",
                    io.github.soundgoodizerfan.feedback.instrument.Instruments.format(gain, tester.resolution(io.github.soundgoodizerfan.feedback.instrument.Quantity.RESPONSE))));
            out.accept(Component.translatable("feedback.circuit.heater"));
        }
        out.accept(Component.translatable("feedback.board.joint", Component.translatable(joint.translationKey())));
    }
}
