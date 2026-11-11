// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.rotation;

import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroRotor;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where one run feeds several others that the propagator keeps apart: a differential's two axles and
 * a Rotation Adapter's four micro shafts. Each fed run has no source of its own, so it is driven the way
 * a belt drives one ({@link RotationNetwork#driveByBelt}) at the speed the splitter asks of the member
 * beside it, with the splitter's run's capacity, and the Su it carries is charged back to that run
 * ({@link RotationNetwork#loadByBelt}). The propagator never joins them, so each has its own speed,
 * its own load and its own overstress.
 */
public final class RotationSplit {

    private RotationSplit() {
    }

    /**
     * The member across {@code face} of {@code member} that presents a shaft back at it, or null:
     * the full-size node in the next block, or the voxel in the next place on the quarter grid.
     */
    @Nullable
    public static RotationMember across(Level level, RotationMember member, Direction face) {
        if (member instanceof MicroRotor rotor) {
            int x = rotor.index() & 3, y = rotor.index() >> 2 & 3, z = rotor.index() >> 4;
            int n = MicroCellBlockEntity.N;
            int nx = x + face.getStepX(), ny = y + face.getStepY(), nz = z + face.getStepZ();
            BlockPos at = rotor.getBlockPos().offset(Math.floorDiv(nx, n), Math.floorDiv(ny, n), Math.floorDiv(nz, n));
            BlockEntity be = at.equals(rotor.getBlockPos()) ? rotor.cell() : level.isLoaded(at) ? level.getBlockEntity(at) : null;
            if (!(be instanceof MicroCellBlockEntity cell))
                return null;
            MicroRotor other = cell.rotor(MicroCellBlockEntity.index(Math.floorMod(nx, n), Math.floorMod(ny, n), Math.floorMod(nz, n)));
            return other != null && faces(level, other.getBlockPos(), other.state(), face.getOpposite()) ? other : null;
        }
        BlockPos next = member.getBlockPos().relative(face);
        if (!level.isLoaded(next) || !(level.getBlockEntity(next) instanceof RotationNode other))
            return null;
        return faces(level, next, other.getBlockState(), face.getOpposite()) ? other : null;
    }

    private static boolean faces(Level level, BlockPos pos, BlockState state, Direction face) {
        return state.getBlock() instanceof Rotatable rotatable && rotatable.hasShaftTowards(level, pos, state, face);
    }

    /**
     * Drives each of {@code outs} to its figure in {@code wants} (the RPM that member itself should
     * turn at), once per distinct run, and charges {@code in} for what they carry. A member whose run
     * has a source of its own, or a zero ratio, is left alone.
     */
    public static void drive(RotationNetwork in, List<RotationMember> outs, float[] wants, long now) {
        Set<RotationNetwork> done = new HashSet<>();
        List<RotationNetwork> runs = new ArrayList<>(outs.size());
        float capacity = in.getSettledCapacitySu().value();
        float carried = 0;
        for (int i = 0; i < outs.size(); i++) {
            RotationMember out = outs.get(i);
            RotationNetwork run = out.getNetwork();
            if (run == null || run == in || out.getRatio() == 0 || !done.add(run) || run.getTargetRpm().value() != 0)
                continue;
            run.driveByBelt(new Rpm(wants[i] / out.getRatio()), new Su(capacity));
            run.markFed(in, now);
            carried += Math.min(run.getSettledLoadSu().value(), capacity);
            runs.add(run);
        }
        if (carried > 0)
            in.loadByBelt(new Su(carried));
    }
}
