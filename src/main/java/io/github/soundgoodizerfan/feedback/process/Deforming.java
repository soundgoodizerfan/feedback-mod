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

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.core.unit.St;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One blow on one workpiece, and the only place the rule is written.
 *
 * <h2>Why this is not a method on the machine any more</h2>
 * It was, and it had to stop being one the moment a player could swing a hammer by hand. Philosophy
 * 7 promises manual production stays theoretically possible for a surprising share of the game, and
 * a hand route that reached the same plate by different arithmetic would be two rules wearing one
 * name -- the sort of divergence that is invisible until a table changes and only one of them
 * follows. A blow is a property of the material and the force, and neither of those knows whether
 * a machine or a player delivered it.
 *
 * <h2>What a blow does not decide</h2>
 * Nothing here consults an item id, and nothing here knows what a copper plate is. Everything
 * comes out of {@link DeformationTable}: what a material becomes, how much work it takes, how hard
 * it is, and whether it has to be hot. The chain is data (philosophy 1, 4).
 *
 * <h2>The outcome is not a boolean</h2>
 * A blow that accomplishes nothing accomplishes nothing in four genuinely different ways, and the
 * two callers want different things from that. The machine absorbs the force and wears, because
 * force that cannot go into the work goes into the machine. The crafting grid does not even offer
 * the craft, because the swing never happened -- there is no output slot to take from and so
 * nothing is spent. Both are the same rule seen from the two ends of a handle.
 */
public final class Deforming {

    /** Stages one blow may cascade through. A guard against a table that loops back on itself. */
    private static final int MAX_CASCADE = 8;

    /** What became of a blow. Only {@link #WORKED} changed the workpiece. */
    public enum Outcome {
        /** The blow landed and the stack in {@link Blow#result()} is what is left. */
        WORKED,
        /** This material does nothing under a hammer. Scrap is already scrap. */
        NO_PROCESS,
        /** A hot-working material outside its window -- too cold to move, or too hot to survive. */
        WRONG_TEMPERATURE,
        /** Below the material's hardness floor. A genuine impossibility, not a slower route (§7). */
        TOO_SOFT;

        public boolean landed() {
            return this == WORKED;
        }
    }

    /**
     * @param result  the workpiece after the blow, or the untouched input when nothing landed
     * @param outcome why, which is the half the caller cannot recompute
     */
    public record Blow(ItemStack result, Outcome outcome) {

        public boolean landed() {
            return outcome.landed();
        }
    }

    private Deforming() {
    }

    /** Land one blow of {@code strength} St on {@code workpiece}. See {@link #apply}. */
    public static Blow strike(ItemStack workpiece, St strength, Level level) {
        return apply(workpiece, strength, level, Operation.BLOW);
    }

    /** Draw {@code workpiece} through a die with {@code strength} St of pull. See {@link #apply}. */
    public static Blow draw(ItemStack workpiece, St strength, Level level) {
        return apply(workpiece, strength, level, Operation.DRAW);
    }

    /**
     * Apply {@code strength} St of {@code operation} to {@code workpiece}.
     *
     * <p>The input stack is never mutated; the result is a fresh stack. Callers that hold the
     * workpiece in a slot assign it, and callers that are previewing a craft can throw it away.
     *
     * @param level needed only to resolve the workpiece's own temperature, which is computed on
     *              demand from a stamp and a tick rather than stored (philosophy 9)
     */
    public static Blow apply(ItemStack workpiece, St strength, Level level, Operation operation) {
        if (workpiece.isEmpty())
            return new Blow(workpiece, Outcome.NO_PROCESS);

        Optional<Deformation> maybe = DeformationTable.get().find(workpiece, operation);
        if (maybe.isEmpty())
            return new Blow(workpiece, Outcome.NO_PROCESS);

        Deformation deformation = maybe.get();

        // Nothing heats the anvil. The workpiece carries its own heat (§9), and if it has fallen
        // out of its working range the blow lands to no effect whatsoever -- which is the honest
        // answer, because a hammer has no way to tell. The heat is wasted and the material is
        // intact, matching beat 2's asymmetry: the player loses a trip to the fire, not the steel.
        if (deformation.isHotWorking()
                && !deformation.worksAt(ItemHeat.get(workpiece, level)))
            return new Blow(workpiece, Outcome.WRONG_TEMPERATURE);

        // How much a blow accomplishes is the material's business, not the machine's. Below the
        // hardness threshold nothing lands at all -- philosophy 7's hard gate, a genuine
        // impossibility rather than a slower version of the process. It is also what keeps a hand
        // hammer out of steel without anyone writing a rule about hand hammers: 3 St against
        // hardness 15 is not a long afternoon, it is nothing at all.
        int delivered = deformation.workFrom(strength);
        if (delivered <= 0)
            return new Blow(workpiece, Outcome.TOO_SOFT);

        ItemStack result = workpiece.copy();

        // Progress does not carry across a change of operation -- a blow and a draw are
        // physically different actions, so a partial dent is not partial credit toward a draw.
        // Switching forfeits whatever WORK was pending rather than crediting it to this entry.
        int priorOperation = result.getOrDefault(FDataComponents.WORK_OPERATION.get(), operation.ordinal());
        int carriedWork = priorOperation == operation.ordinal()
                ? result.getOrDefault(FDataComponents.WORK.get(), 0) : 0;
        int worked = carriedWork + delivered;

        // Surplus carries, and carries through a finished stage into the next one. A blow does not
        // politely stop at the finish line, which is the whole point: a hard enough blow on a soft
        // enough material runs straight past what you wanted. Overshoot is the mechanic, not an
        // edge case.
        Deformation stage = deformation;
        for (int guard = 0; guard < MAX_CASCADE && worked >= stage.work(); guard++) {
            worked -= stage.work();

            // The new stage inherits the old one's heat. Beating a hot ingot into a plate does not
            // cool it, and losing the stamp here would have made every hot-working chain a single
            // step by accident.
            Tu carried = ItemHeat.get(result, level);
            result = stage.result().copy();
            ItemHeat.set(result, carried, level);

            Optional<Deformation> next = DeformationTable.get().find(result, operation);
            if (next.isEmpty()) {
                worked = 0;   // nothing further to become; the work has nowhere to go
                break;
            }
            stage = next.get();
        }

        if (worked > 0) {
            result.set(FDataComponents.WORK.get(), worked);
            // Required work rides along so the workpiece can describe its own progress wherever it
            // goes, without anything having to look the material up.
            result.set(FDataComponents.WORK_REQUIRED.get(), stage.work());
            result.set(FDataComponents.WORK_OPERATION.get(), operation.ordinal());
        } else {
            result.remove(FDataComponents.WORK.get());
            result.remove(FDataComponents.WORK_REQUIRED.get());
            result.remove(FDataComponents.WORK_OPERATION.get());
        }

        return new Blow(result, Outcome.WORKED);
    }
}
