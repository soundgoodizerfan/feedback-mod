// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import java.util.Map;
import java.util.Optional;


import net.minecraft.resources.ResourceLocation;

/**
 * Joint quality, graded from Tu against an alloy's liquidus. Pure: the hand iron
 * and the reflow oven both reduce a joint to a peak Tu and a time above liquidus and ask here, so
 * there is one rule and the two routes differ only in what it costs to reach it.
 *
 * <h2>Where the figures come from</h2>
 * The liquidus is the solder material's {@code melting_point}. The window above it, inside which a
 * joint is good, is {@link ProcessTuning#SOLDER_WINDOW_NUMERATOR_TU} over its {@code molten.viscosity}:
 * a thinner melt wets better, so lead solder forgives more than lead-free. The limit past which a
 * trace lifts is the lowest {@code service_temperature} of the substrate and of the trace metal,
 * or {@link ProcessTuning#PRINTED_TRACE_LIMIT_TU} for a printed trace. Nothing here names a material.
 */
public final class Soldering {

    private Soldering() {
    }

    /** What a joint came to. A lift is the one outcome that costs the board its traces. */
    public enum Outcome { COLD, GOOD, LIFTED }

    /** A solder's liquidus and the width of its good window above it, in Tu. */
    public record Alloy(float liquidus, float window) {}

    /** The alloy a material is, if it is a solder with a melting point. */
    public static Optional<Alloy> alloy(Material material) {
        if (!material.traits().contains(Material.Trait.SOLDER) || material.thermal().meltingPoint().isEmpty())
            return Optional.empty();
        float viscosity = material.molten().viscosity().orElse(4f);
        return Optional.of(new Alloy(material.thermal().meltingPoint().get().value(),
                ProcessTuning.SOLDER_WINDOW_NUMERATOR_TU / Math.max(0.1f, viscosity)));
    }

    /**
     * What a board becomes after a joint at {@code peakTu} for {@code dwellTicks}: the board with
     * its new grade, or its bare substrate if the traces lifted (rework, not refusal; the solder is
     * spent either way). A board with no solder pasted is returned as it was.
     */
    public static net.minecraft.world.item.ItemStack finish(net.minecraft.world.item.ItemStack stack,
            Map<ResourceLocation, Material> materials, float peakTu, int dwellTicks) {
        Circuit baked = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get());
        if (baked != null) {
            // A finished circuit put back through heat: past its traces' limit it lifts, otherwise nothing happens.
            net.minecraft.world.item.ItemStack out = stack.copyWithCount(1);
            if (baked.live() && peakTu > baked.limitTu())
                out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get(), Circuit.dead(baked.joint()));
            return out;
        }
        Board board = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get());
        if (board == null || board.solder().isEmpty() || !materials.containsKey(board.solder().get()))
            return stack;
        Optional<Alloy> alloy = alloy(materials.get(board.solder().get()));
        if (alloy.isEmpty())
            return stack;
        return build(stack, board, grade(peakTu, dwellTicks, alloy.get(), limit(board, materials)), 0, materials, cook(board, peakTu));
    }

    /** The hand route: a board comes out of a drag {@link Pass} as its worst pad says, its cold and bridged pads counted. */
    public static net.minecraft.world.item.ItemStack finish(net.minecraft.world.item.ItemStack stack,
            Map<ResourceLocation, Material> materials, Pass pass) {
        Board board = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get());
        if (board == null || board.solder().isEmpty())
            return stack;
        return build(stack, board, pass.worst(), pass.defects(), materials, cook(board, pass.peaks()));
    }

    private static net.minecraft.world.item.ItemStack build(net.minecraft.world.item.ItemStack stack, Board board,
            Outcome outcome, int defects, Map<ResourceLocation, Material> materials, Board cooked) {
        if (!board.parts().isEmpty()) {
            // Mounted parts: the board becomes a circuit. A lift takes the parts with it (a dead part); a cold joint is a noisy one.
            net.minecraft.world.item.ItemStack circuit = new net.minecraft.world.item.ItemStack(
                    io.github.soundgoodizerfan.feedback.registry.FItems.CIRCUIT.get());
            Board.Joint joint = outcome == Outcome.GOOD ? Board.Joint.GOOD : Board.Joint.COLD;
            circuit.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get(),
                    outcome == Outcome.LIFTED ? Circuit.dead(joint) : Circuit.assemble(cooked, joint, materials, defects));
            return circuit;
        }
        if (outcome == Outcome.LIFTED) {
            Material substrate = materials.get(board.substrate());
            return substrate == null ? net.minecraft.world.item.ItemStack.EMPTY
                    : substrate.stack(board.substrate(), "substrate").orElse(net.minecraft.world.item.ItemStack.EMPTY);
        }
        net.minecraft.world.item.ItemStack out = stack.copyWithCount(1);
        out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get(),
                board.withJoint(outcome == Outcome.GOOD && defects == 0 ? Board.Joint.GOOD : Board.Joint.COLD));
        return out;
    }

    /** The Tu a part of this kind can sit at before it is cooked. */
    public static float partLimit(Board.Kind kind) {
        return switch (kind) {
            case TUBE -> ProcessTuning.PART_LIMIT_TUBE_TU;
            case CLOCK -> ProcessTuning.PART_LIMIT_CLOCK_TU;
            case RESONATOR -> ProcessTuning.PART_LIMIT_RESONATOR_TU;
            case SWITCH -> ProcessTuning.PART_LIMIT_SWITCH_TU;
            case LATTICE -> ProcessTuning.PART_LIMIT_LATTICE_TU;
            case RELAY -> ProcessTuning.PART_LIMIT_RELAY_TU;
            case COHERER -> ProcessTuning.PART_LIMIT_COHERER_TU;
        };
    }

    /** The board with every part cooked whose pair of pads ({@code 2i} and {@code 2i + 1}) peaked over its kind's limit: it stays on the board, no longer an element. */
    public static Board cook(Board board, float[] padPeakTu) {
        java.util.List<Board.Part> parts = new java.util.ArrayList<>(board.parts());
        for (int i = 0; i < parts.size(); i++) {
            float peak = 0f;
            for (int pad = 2 * i; pad <= 2 * i + 1 && pad < padPeakTu.length; pad++)
                peak = Math.max(peak, padPeakTu[pad]);
            if (!parts.get(i).dead() && peak > partLimit(parts.get(i).kind()))
                parts.set(i, parts.get(i).ruined());
        }
        return board.withParts(parts);
    }

    /** As {@link #cook(Board, float[])} when the whole board sat at one temperature (the reflow oven). */
    public static Board cook(Board board, float peakTu) {
        float[] all = new float[Math.max(2, 2 * board.parts().size())];
        java.util.Arrays.fill(all, peakTu);
        return cook(board, all);
    }

    /**
     * What a rework drag leaves: {@code board} is the bare trace board (parts that stayed on it, solder taken up) when at least one part
     * lifted, the bare substrate when the traces lifted, empty when nothing moved; {@code parts} are the lifted items that survived the heat.
     */
    public record Lift(net.minecraft.world.item.ItemStack board, java.util.List<net.minecraft.world.item.ItemStack> parts, int lifted, boolean traceLost) {}

    /**
     * Rework a finished circuit's parts: a part whose two pads both came to wet solder lifts off the board with the wick. It comes back
     * as the item that was mounted when its pads stayed under its kind's limit; a cooked one is lifted too but comes back as nothing.
     */
    public static Lift lift(Circuit circuit, Map<ResourceLocation, Material> materials, Pass pass) {
        Board board = circuit.board().orElse(null);
        net.minecraft.world.item.ItemStack none = net.minecraft.world.item.ItemStack.EMPTY;
        if (board == null)
            return new Lift(none, java.util.List.of(), 0, false);
        if (pass.worst() == Outcome.LIFTED) {
            Material substrate = materials.get(board.substrate());
            return new Lift(substrate == null ? none : substrate.stack(board.substrate(), "substrate").orElse(none), java.util.List.of(), 0, true);
        }
        float[] peaks = pass.peaks();
        java.util.List<Board.Part> staying = new java.util.ArrayList<>();
        java.util.List<net.minecraft.world.item.ItemStack> back = new java.util.ArrayList<>();
        int lifted = 0;
        for (int i = 0; i < board.parts().size(); i++) {
            Board.Part part = board.parts().get(i);
            boolean free = 2 * i + 1 < pass.pads() && pass.outcome(2 * i) == Outcome.GOOD && pass.outcome(2 * i + 1) == Outcome.GOOD;
            if (!free) {
                staying.add(part);
                continue;
            }
            lifted++;
            float peak = Math.max(peaks[2 * i], peaks[2 * i + 1]);
            if (!part.dead() && peak <= partLimit(part.kind()))
                part.source().ifPresent(s -> back.add(s.copyWithCount(1)));
        }
        if (lifted == 0)
            return new Lift(none, java.util.List.of(), 0, false);
        net.minecraft.world.item.ItemStack out = new net.minecraft.world.item.ItemStack(
                io.github.soundgoodizerfan.feedback.registry.FItems.TRACE_BOARD.get());
        out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get(), board.unsoldered(staying));
        return new Lift(out, java.util.List.copyOf(back), lifted, false);
    }

    /** Pads in a row: one pair per mounted part, else two. */
    public static int pads(Board board) {
        return Math.max(2, Math.min(ProcessTuning.SOLDER_MAX_PADS, 2 * board.parts().size()));
    }

    /** The Tu past which this board's traces lift. */
    public static float limit(Board board, Map<ResourceLocation, Material> materials) {
        float limit = Float.MAX_VALUE;
        Material substrate = materials.get(board.substrate());
        if (substrate != null)
            limit = substrate.thermal().serviceTemperature().map(t -> t.value()).orElse(limit);
        if (board.trace() == Board.Trace.PRINTED)
            return Math.min(limit, ProcessTuning.PRINTED_TRACE_LIMIT_TU);
        Material trace = materials.get(board.traceMaterial());
        if (trace != null)
            limit = Math.min(limit, trace.thermal().serviceTemperature().map(t -> t.value()).orElse(limit));
        return limit;
    }

    /**
     * Grade a joint. Below the liquidus, or not held above it long enough, the solder never flowed;
     * above the window the flux burnt (cold again); above the limit the trace lifted.
     */
    public static Outcome grade(float peakTu, int dwellTicks, Alloy alloy, float limit) {
        return grade(peakTu, dwellTicks, ProcessTuning.SOLDER_DWELL_TICKS, alloy, limit);
    }

    public static Outcome grade(float peakTu, int dwellTicks, int minDwell, Alloy alloy, float limit) {
        if (peakTu > limit)
            return Outcome.LIFTED;
        if (peakTu < alloy.liquidus() || dwellTicks < minDwell)
            return Outcome.COLD;
        return peakTu <= alloy.liquidus() + alloy.window() ? Outcome.GOOD : Outcome.COLD;
    }

    /**
     * One drag along the pad row, the single rule the screen draws and the server
     * judges. Each tick the tip sits on a pad the pad closes {@link ProcessTuning#SOLDER_PAD_HEAT_FRACTION}
     * of its gap to the iron and the iron gives up {@link ProcessTuning#IRON_JOINT_DRAW_TU}, so a pace that
     * was right at the first pad is too fast at the last. Pure: resistance and instruments never enter.
     */
    public static final class Pass {
        private final Alloy alloy;
        private final float limit;
        private final float[] pad;
        private final int[] dwell;
        private final boolean[] bridged;
        private final float[] peak;
        private float iron;

        public Pass(int pads, float ironTu, Alloy alloy, float limit) {
            this.alloy = alloy;
            this.limit = limit;
            this.iron = ironTu;
            pad = new float[pads];
            dwell = new int[pads];
            bridged = new boolean[pads];
            peak = new float[pads];
            java.util.Arrays.fill(pad, ThermalTuning.AMBIENT_TU.value());
            java.util.Arrays.fill(peak, ThermalTuning.AMBIENT_TU.value());
        }

        /** The tip sat on pad {@code p} for {@code ticks}. */
        public void touch(int p, int ticks) {
            for (int i = 0; i < ticks; i++) {
                pad[p] += (iron - pad[p]) * ProcessTuning.SOLDER_PAD_HEAT_FRACTION;
                iron = Math.max(0f, iron - ProcessTuning.IRON_JOINT_DRAW_TU);
                peak[p] = Math.max(peak[p], pad[p]);
            }
            dwell[p] += ticks;
        }

        /** Solder reached the neighbour from pad {@code p}. */
        public void bridge(int p) {
            bridged[p] = true;
        }

        public Outcome outcome(int p) {
            return dwell[p] == 0 ? Outcome.COLD : grade(pad[p], dwell[p], ProcessTuning.SOLDER_PAD_MIN_DWELL_TICKS, alloy, limit);
        }

        public float padTu(int p) {
            return pad[p];
        }

        /** The hottest each pad has been, which is what cooks a part (a pad cools toward a falling iron but a part does not un-cook). */
        public float[] peaks() {
            return peak.clone();
        }

        public boolean bridged(int p) {
            return bridged[p];
        }

        public float ironTu() {
            return iron;
        }

        public int pads() {
            return pad.length;
        }

        /** Any lifted pad lifts the board (whole board; per-pad loss is [OPEN] in 6.22); else any cold pad; else good. */
        public Outcome worst() {
            Outcome w = Outcome.GOOD;
            for (int p = 0; p < pad.length; p++) {
                Outcome o = outcome(p);
                if (o == Outcome.LIFTED)
                    return o;
                if (o == Outcome.COLD)
                    w = o;
            }
            return w;
        }

        /** Cold plus bridged pads: what feeds a circuit's noise. */
        public int defects() {
            int n = 0;
            for (int p = 0; p < pad.length; p++)
                if (outcome(p) == Outcome.COLD || bridged[p])
                    n++;
            return n;
        }
    }
}
