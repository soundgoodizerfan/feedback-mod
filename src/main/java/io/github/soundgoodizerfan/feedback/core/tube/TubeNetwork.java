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
package io.github.soundgoodizerfan.feedback.core.tube;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.machine.tube.TubeBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One connected run of tube blocks with one shared air ledger -- the gas half of {@link FluidNetwork}
 * applied to a run that also carries capsules. The air belongs to the run; its pressure is what
 * drives every capsule in it. The capsules themselves live in the tube blocks (so they survive a
 * rebuild and a chunk unload); the run keeps only a time-ordered queue of when each next needs
 * attention, so a tick with nothing due does constant work (spec 3.2, 7).
 * <p>
 * Air spent by hops is replaced by whatever pressurises the run: a compressor or a creative air source.
 */
public class TubeNetwork {

    private record Due(long at, TubeBlockEntity tube) {
    }

    public final Set<TubeBlockEntity> members = new HashSet<>();
    public final HeatedTank ledger;
    private final int volume;
    private boolean changed;
    private final PriorityQueue<Due> queue = new PriorityQueue<>((a, b) -> Long.compare(a.at, b.at));
    /** Tubes holding a pod with a rider: bounded by the player count, moved every tick. */
    private final Set<TubeBlockEntity> riders = new HashSet<>();

    TubeNetwork(int size) {
        volume = size * FTuning.PIPE_VOLUME_MB;
        ledger = new HeatedTank(volume * FTuning.PIPE_COMPRESSION_HEADROOM, () -> changed = true);
    }

    public int volume() {
        return volume;
    }

    public Pu pressure() {
        return new Pu(ledger.isEmpty() || !FluidNetwork.isGas(ledger.getFluid()) ? 0f
                : (float) ledger.getFluidAmount() / volume * FTuning.PIPE_PU_PER_VOLUME);
    }

    /** Takes gas up to one volume, or up to the headroom when {@code compressed}; a liquid is refused (a tube holds air). */
    public int accept(FluidStack resource, boolean compressed, IFluidHandler.FluidAction action) {
        if (!FluidNetwork.isGas(resource))
            return 0;
        int limit = compressed ? ledger.getCapacity() : volume;
        int room = Math.max(0, limit - ledger.getFluidAmount());
        return room <= 0 ? 0 : ledger.fill(resource.copyWithAmount(Math.min(room, resource.getAmount())), action);
    }

    /** Spends air for a hop (the capsule's push); the run may be left dry. */
    public void spendAir(int mb) {
        if (!ledger.isEmpty())
            ledger.drain(mb, IFluidHandler.FluidAction.EXECUTE);
    }

    /** Puts {@code tube}'s capsule on the schedule at its ready time. */
    public void schedule(TubeBlockEntity tube) {
        Capsule c = tube.capsule();
        if (c == null)
            return;
        queue.add(new Due(c.wakeAt, tube));
        if (c.rider != null)
            riders.add(tube);
    }

    void tick(Level level) {
        if (members.isEmpty())
            return;
        long now = level.getGameTime();
        if (now % 10 == 0 && hazard(level))
            return;
        if (now % 20 == 0)
            leak();
        if (!riders.isEmpty())
            for (TubeBlockEntity t : new ArrayList<>(riders))
                if (!t.moveRider(now) || t.isRemoved() || t.network() != this)
                    riders.remove(t);
        while (!queue.isEmpty() && queue.peek().at <= now) {
            Due due = queue.poll();
            TubeBlockEntity t = due.tube;
            Capsule c = t.capsule();
            if (t.isRemoved() || t.network() != this || c == null || c.wakeAt != due.at)
                continue;
            t.advance(level, now);
        }
    }

    /** Evacuated tube blocks let a little air in through their seals; a pump keeps the vacuum. */
    private void leak() {
        long evacuated = members.stream().filter(TubeBlockEntity::evacuated).count();
        if (evacuated > 0)
            ledger.fill(new FluidStack(FFluids.AIR.get(), (int) (evacuated * FTuning.TUBE_EVAC_LEAK_MB_PER_SECOND)),
                    IFluidHandler.FluidAction.EXECUTE);
    }

    private boolean hazard(Level level) {
        if (!PressureBurst.fails(pressure(), FTuning.PRESSURE_VESSEL_DANGER_PU, FTuning.PRESSURE_VESSEL_CRITICAL_PU, level.random))
            return false;
        List<TubeBlockEntity> list = new ArrayList<>(members);
        TubeBlockEntity weak = list.get(level.random.nextInt(list.size()));
        ledger.setFluid(FluidStack.EMPTY);
        PressureBurst.burst(level, weak.getBlockPos(), FTuning.PIPE_BURST_POWER);
        return true;
    }
}
