// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.companion.SableCompanion;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Belts between pulley voxels: the same slip-not-stall belt as {@code PulleyBlockEntity}, with each end
 * addressed by its cell and voxel. A belt links two pulleys of one body (or both in the world), so the
 * two ends share a frame and the Lift moves both; {@link MicroCellBlockEntity#moved} carries the
 * partner's address with it. One end at a time is driven: the side with no source of its own turns at the
 * other's speed across the pulleys' radii, with the belt's capacity, and the load it carries is charged to
 * the other's run. A belt wears only while it slips.
 */
public final class MicroBelts {

    /** One end of a belt: a cell's position and the pulley voxel in it. */
    public record End(BlockPos pos, int index) {
    }

    private record Pending(GlobalPos cell, int index) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private MicroBelts() {
    }

    /** A Belt item used on pulley voxel {@code index} of {@code cell}: the first click selects, the second links. */
    public static void select(Player player, MicroCellBlockEntity cell, int index) {
        Level level = cell.getLevel();
        GlobalPos here = GlobalPos.of(level.dimension(), cell.getBlockPos());
        Pending first = PENDING.remove(player.getUUID());
        if (first == null || first.cell().equals(here) && first.index() == index) {
            PENDING.put(player.getUUID(), new Pending(here, index));
            player.displayClientMessage(Component.translatable("feedback.belt.selected"), true);
            return;
        }
        boolean ok = first.cell().dimension() == level.dimension() && level.isLoaded(first.cell().pos())
                && level.getBlockEntity(first.cell().pos()) instanceof MicroCellBlockEntity a && link(a, first.index(), cell, index);
        player.displayClientMessage(Component.translatable(ok ? "feedback.belt.linked" : "feedback.belt.failed"), true);
    }

    private static boolean pulley(MicroCellBlockEntity cell, int index) {
        return cell.get(index) != null && cell.get(index).getBlock() instanceof PulleyBlock;
    }

    private static boolean link(MicroCellBlockEntity a, int ia, MicroCellBlockEntity b, int ib) {
        Level level = a.getLevel();
        if (level == null || b.getLevel() != level || !pulley(a, ia) || !pulley(b, ib) || a == b && ia == ib
                || !a.getBlockPos().closerThan(b.getBlockPos(), FTuning.BELT_MAX_LENGTH)
                || SableCompanion.INSTANCE.getContaining(level, a.getBlockPos()) != SableCompanion.INSTANCE.getContaining(level, b.getBlockPos()))
            return false;
        a.unlinkBelt(ia);
        b.unlinkBelt(ib);
        a.setBelt(ia, new End(b.getBlockPos(), ib));
        b.setBelt(ib, new End(a.getBlockPos(), ia));
        return true;
    }

    /** One game tick of pulley voxel {@code index}: drives its own run from its partner's, if it has none of its own. */
    static void step(Level level, MicroCellBlockEntity cell, int index) {
        End far = cell.belt(index);
        MicroRotor own = cell.rotor(index);
        if (far == null || own == null || !level.isLoaded(far.pos()))
            return;
        if (!(level.getBlockEntity(far.pos()) instanceof MicroCellBlockEntity other) || !pulley(other, far.index())) {
            cell.unlinkBelt(index);
            return;
        }
        MicroRotor theirs = other.rotor(far.index());
        RotationNetwork mine = own.getNetwork(), yours = theirs == null ? null : theirs.getNetwork();
        if (mine == null || yours == null || mine == yours || own.getRatio() == 0 || mine.getCapacitySu().value() > 0)
            return;
        float desired = theirs.getRpm().value() * radius(other, far.index()) / radius(cell, index) / own.getRatio();
        float demand = mine.getLoadSu().value();
        float condition = cell.beltCondition(index);
        float capacity = FTuning.BELT_CAPACITY_SU.value() * condition;
        float carried = Math.min(demand, capacity);
        if (demand > capacity) {
            desired *= capacity / demand;
            cell.wearBelt(index, Math.max(FTuning.BELT_CONDITION_FLOOR, condition - (demand - capacity) * FTuning.BELT_WEAR_PER_SU_SLIPPED));
        }
        mine.driveByBelt(new Rpm(desired), new Su(Math.max(demand, capacity)));
        yours.loadByBelt(new Su(carried));
    }

    private static int radius(MicroCellBlockEntity cell, int index) {
        return cell.get(index).getBlock() instanceof PulleyBlock p ? p.getRadius() : 1;
    }
}
