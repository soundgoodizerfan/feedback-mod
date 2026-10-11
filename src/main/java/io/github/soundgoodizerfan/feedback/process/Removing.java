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
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Cutting on one workpiece -- {@link Deforming}'s own shape, for the removal family.
 *
 * <h2>Progress is the accumulated-Fu component, not {@code ItemHeat}</h2>
 * Deliberately the same {@link FDataComponents#WORK}/{@link FDataComponents#WORK_REQUIRED} pair
 * {@link Deforming} already writes -- removal only changes when work is actually applied, exactly
 * like a hammer's blow, and nothing about it is a function of elapsed time the way a workpiece's
 * temperature is. Reusing {@code ItemHeat}'s stamp-and-tick pattern here would be a correctness
 * bug, not a style choice: a drilled hole does not get deeper while the part sits in a chest.
 * {@link FDataComponents#WORK_OPERATION} is tagged with the caller's own {@link Operation}
 * ({@code TURN}/{@code DRILL}/{@code GRIND}) so switching a workpiece between two different
 * operations -- including two removal operations, not only removal-versus-deformation -- forfeits
 * whatever progress was pending, the same rule {@link Deforming} already enforces between
 * {@code BLOW}/{@code DRAW}/{@code ROLL}.
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

    /**
     * @param result    the workpiece after the cut, or the untouched input when nothing landed
     * @param delivered Fu this call put into the material, fraction included -- what a caller
     *                  scales grit loss or interface heat by
     * @param carried   the fraction of a Fu not yet credited to the workpiece; hand it back in
     *                  on the next call
     * @param swarf     what the cut-away metal is as scrap, per the entry that was cut
     */
    public record Cut(ItemStack result, Outcome outcome, float delivered, float carried, ItemStack swarf) {
        public boolean landed() {
            return outcome.landed();
        }

        static Cut failed(ItemStack workpiece, Outcome outcome, float carried) {
            return new Cut(workpiece, outcome, 0f, carried, ItemStack.EMPTY);
        }
    }

    private Removing() {
    }

    /**
     * Apply {@code passes} passes of {@code driveForce} through a tool of {@code toolHardness} to
     * {@code workpiece}, for the given {@code operation}. The input stack is never mutated.
     *
     * <h3>Why a fraction is carried, and where</h3>
     * One pass of a light cut on steel is about half a Fu. The workpiece's own {@code WORK} is a
     * whole number -- it is what calipers read -- so the fraction rides on the machine between
     * calls ({@code carriedFu}) and is lost if the workpiece is taken out, which costs under one
     * Fu and is the honest price of interrupting a cut.
     *
     * @param level only for carrying the workpiece's heat across a finished stage, as
     *              {@link Deforming} does
     */
    public static Cut apply(ItemStack workpiece, St driveForce, St toolHardness, Operation operation,
                            float passes, float carriedFu, Level level) {
        if (workpiece.isEmpty())
            return Cut.failed(workpiece, Outcome.NO_PROCESS, 0f);

        Optional<Removal> maybe = RemovalTable.get().find(workpiece, operation);
        if (maybe.isEmpty())
            return Cut.failed(workpiece, Outcome.NO_PROCESS, carriedFu);

        Removal removal = maybe.get();
        if (!removal.canBeCutBy(toolHardness))
            return Cut.failed(workpiece, Outcome.WRONG_TOOL, carriedFu);

        float fu = removal.cutFrom(driveForce) * passes;
        float total = carriedFu + fu;
        int delivered = (int) total;
        float carried = total - delivered;
        if (delivered == 0)
            return new Cut(workpiece, Outcome.WORKED, fu, carried, removal.swarf());

        ItemStack result = workpiece.copy();

        Operation priorOperation = result.getOrDefault(FDataComponents.WORK_OPERATION.get(), operation);
        int carriedWork = priorOperation == operation
                ? result.getOrDefault(FDataComponents.WORK.get(), 0) : 0;
        int worked = carriedWork + delivered;

        Removal stage = removal;
        for (int guard = 0; guard < MAX_CASCADE && worked >= stage.work(); guard++) {
            worked -= stage.work();
            // Cutting does not cool the part -- see Deforming's cascade for the same rule.
            Tu heat = ItemHeat.get(result, level);
            result = stage.result().copy();
            ItemHeat.set(result, heat, level);

            Optional<Removal> next = RemovalTable.get().find(result, operation);
            if (next.isEmpty()) {
                worked = 0;
                break;
            }
            stage = next.get();
        }

        if (worked > 0) {
            result.set(FDataComponents.WORK.get(), worked);
            result.set(FDataComponents.WORK_REQUIRED.get(), stage.work());
            result.set(FDataComponents.WORK_OPERATION.get(), operation);
        } else {
            result.remove(FDataComponents.WORK.get());
            result.remove(FDataComponents.WORK_REQUIRED.get());
            result.remove(FDataComponents.WORK_OPERATION.get());
        }

        return new Cut(result, Outcome.WORKED, fu, carried, removal.swarf());
    }
}
