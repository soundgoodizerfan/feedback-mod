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

import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * One tick of cutting on one workpiece -- {@link Deforming}'s own shape, for the removal family.
 *
 * <h2>Progress is the accumulated-Fu component, not {@code ItemHeat}</h2>
 * Deliberately the same {@link FDataComponents#WORK}/{@link FDataComponents#WORK_REQUIRED} pair
 * {@link Deforming} already writes -- removal only changes when work is actually applied, exactly
 * like a hammer's blow, and nothing about it is a function of elapsed time the way a workpiece's
 * temperature is. Reusing {@code ItemHeat}'s stamp-and-tick pattern here would be a correctness
 * bug, not a style choice: a drilled hole does not get deeper while the part sits in a chest.
 * {@link FDataComponents#WORK_OPERATION} is tagged {@link Operation#REMOVE} so switching a
 * workpiece between a deformation process and a removal process forfeits whatever progress was
 * pending, the same rule {@link Deforming} already enforces between {@code BLOW}/{@code DRAW}/
 * {@code ROLL}.
 */
public final class Removing {

    private static final int MAX_CASCADE = 8;

    /** What became of a cut. Only {@link #WORKED} changed the workpiece. */
    public enum Outcome {
        WORKED,
        /** Nothing in {@link RemovalTable} answers for this item. */
        NO_PROCESS,
        /** The tool's hardness does not exceed this material's -- a hard gate (§7), not a slow route. */
        WRONG_TOOL;

        public boolean landed() {
            return this == WORKED;
        }
    }

    public record Cut(ItemStack result, Outcome outcome) {
        public boolean landed() {
            return outcome.landed();
        }
    }

    private Removing() {
    }

    /**
     * Apply one tick of {@code driveForce} through a tool of {@code toolHardness} to {@code
     * workpiece}. The input stack is never mutated.
     */
    public static Cut apply(ItemStack workpiece, St driveForce, St toolHardness) {
        if (workpiece.isEmpty())
            return new Cut(workpiece, Outcome.NO_PROCESS);

        Optional<Removal> maybe = RemovalTable.get().find(workpiece);
        if (maybe.isEmpty())
            return new Cut(workpiece, Outcome.NO_PROCESS);

        Removal removal = maybe.get();
        if (!removal.canBeCutBy(toolHardness))
            return new Cut(workpiece, Outcome.WRONG_TOOL);

        int delivered = removal.cutFrom(driveForce);

        ItemStack result = workpiece.copy();

        int priorOperation = result.getOrDefault(FDataComponents.WORK_OPERATION.get(), Operation.REMOVE.ordinal());
        int carriedWork = priorOperation == Operation.REMOVE.ordinal()
                ? result.getOrDefault(FDataComponents.WORK.get(), 0) : 0;
        int worked = carriedWork + delivered;

        Removal stage = removal;
        for (int guard = 0; guard < MAX_CASCADE && worked >= stage.work(); guard++) {
            worked -= stage.work();
            result = stage.result().copy();

            Optional<Removal> next = RemovalTable.get().find(result);
            if (next.isEmpty()) {
                worked = 0;
                break;
            }
            stage = next.get();
        }

        if (worked > 0) {
            result.set(FDataComponents.WORK.get(), worked);
            result.set(FDataComponents.WORK_REQUIRED.get(), stage.work());
            result.set(FDataComponents.WORK_OPERATION.get(), Operation.REMOVE.ordinal());
        } else {
            result.remove(FDataComponents.WORK.get());
            result.remove(FDataComponents.WORK_REQUIRED.get());
            result.remove(FDataComponents.WORK_OPERATION.get());
        }

        return new Cut(result, Outcome.WORKED);
    }
}
