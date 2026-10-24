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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.resources.ResourceLocation;

/**
 * Joint quality, graded from Tu against an alloy's liquidus (mechanics 6.20). Pure: the hand iron
 * and the reflow oven both reduce a joint to a peak Tu and a time above liquidus and ask here, so
 * there is one rule and the two routes differ only in what it costs to reach it.
 *
 * <h2>Where the figures come from</h2>
 * The liquidus is the solder material's {@code melting_point}. The window above it, inside which a
 * joint is good, is {@link FTuning#SOLDER_WINDOW_NUMERATOR_TU} over its {@code molten.viscosity}:
 * a thinner melt wets better, so lead solder forgives more than lead-free. The limit past which a
 * trace lifts is the lowest {@code service_temperature} of the substrate and of the trace metal,
 * or {@link FTuning#PRINTED_TRACE_LIMIT_TU} for a printed trace. Nothing here names a material.
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
                FTuning.SOLDER_WINDOW_NUMERATOR_TU / Math.max(0.1f, viscosity)));
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
        return build(stack, board, grade(peakTu, dwellTicks, alloy.get(), limit(board, materials)), 0, materials);
    }

    /** The hand route (mechanics 6.22): a board comes out of a drag {@link Pass} as its worst pad says, its cold and bridged pads counted. */
    public static net.minecraft.world.item.ItemStack finish(net.minecraft.world.item.ItemStack stack,
            Map<ResourceLocation, Material> materials, Pass pass) {
        Board board = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BOARD.get());
        if (board == null || board.solder().isEmpty())
            return stack;
        return build(stack, board, pass.worst(), pass.defects(), materials);
    }

    private static net.minecraft.world.item.ItemStack build(net.minecraft.world.item.ItemStack stack, Board board,
            Outcome outcome, int defects, Map<ResourceLocation, Material> materials) {
        if (!board.parts().isEmpty()) {
            // Mounted parts: the board becomes a circuit. A lift takes the parts with it (a dead part); a cold joint is a noisy one.
            net.minecraft.world.item.ItemStack circuit = new net.minecraft.world.item.ItemStack(
                    io.github.soundgoodizerfan.feedback.registry.FItems.CIRCUIT.get());
            Board.Joint joint = outcome == Outcome.GOOD ? Board.Joint.GOOD : Board.Joint.COLD;
            circuit.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get(),
                    outcome == Outcome.LIFTED ? Circuit.dead(joint) : Circuit.assemble(board, joint, materials, defects));
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

    /** Pads in a row: one pair per mounted part, else two. */
    public static int pads(Board board) {
        return Math.max(2, Math.min(FTuning.SOLDER_MAX_PADS, 2 * board.parts().size()));
    }

    /** The Tu past which this board's traces lift. */
    public static float limit(Board board, Map<ResourceLocation, Material> materials) {
        float limit = Float.MAX_VALUE;
        Material substrate = materials.get(board.substrate());
        if (substrate != null)
            limit = substrate.thermal().serviceTemperature().map(t -> t.value()).orElse(limit);
        if (board.trace() == Board.Trace.PRINTED)
            return Math.min(limit, FTuning.PRINTED_TRACE_LIMIT_TU);
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
        return grade(peakTu, dwellTicks, FTuning.SOLDER_DWELL_TICKS, alloy, limit);
    }

    public static Outcome grade(float peakTu, int dwellTicks, int minDwell, Alloy alloy, float limit) {
        if (peakTu > limit)
            return Outcome.LIFTED;
        if (peakTu < alloy.liquidus() || dwellTicks < minDwell)
            return Outcome.COLD;
        return peakTu <= alloy.liquidus() + alloy.window() ? Outcome.GOOD : Outcome.COLD;
    }

    /**
     * One drag along the pad row (mechanics 6.22), the single rule the screen draws and the server
     * judges. Each tick the tip sits on a pad the pad closes {@link FTuning#SOLDER_PAD_HEAT_FRACTION}
     * of its gap to the iron and the iron gives up {@link FTuning#IRON_JOINT_DRAW_TU}, so a pace that
     * was right at the first pad is too fast at the last. Pure: resistance and instruments never enter.
     */
    public static final class Pass {
        private final Alloy alloy;
        private final float limit;
        private final float[] pad;
        private final int[] dwell;
        private final boolean[] bridged;
        private float iron;

        public Pass(int pads, float ironTu, Alloy alloy, float limit) {
            this.alloy = alloy;
            this.limit = limit;
            this.iron = ironTu;
            pad = new float[pads];
            dwell = new int[pads];
            bridged = new boolean[pads];
            java.util.Arrays.fill(pad, FTuning.AMBIENT_TU.value());
        }

        /** The tip sat on pad {@code p} for {@code ticks}. */
        public void touch(int p, int ticks) {
            for (int i = 0; i < ticks; i++) {
                pad[p] += (iron - pad[p]) * FTuning.SOLDER_PAD_HEAT_FRACTION;
                iron = Math.max(0f, iron - FTuning.IRON_JOINT_DRAW_TU);
            }
            dwell[p] += ticks;
        }

        /** Solder reached the neighbour from pad {@code p}. */
        public void bridge(int p) {
            bridged[p] = true;
        }

        public Outcome outcome(int p) {
            return dwell[p] == 0 ? Outcome.COLD : grade(pad[p], dwell[p], FTuning.SOLDER_PAD_MIN_DWELL_TICKS, alloy, limit);
        }

        public float padTu(int p) {
            return pad[p];
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
