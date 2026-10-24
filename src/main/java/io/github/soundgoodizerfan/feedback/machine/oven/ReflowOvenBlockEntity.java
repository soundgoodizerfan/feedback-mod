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
package io.github.soundgoodizerfan.feedback.machine.oven;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Fumes;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Soldering;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The capital route to a good joint (mechanics 6.20): a chamber on a fire holding a batch of
 * pasted boards.
 *
 * <h2>A batch is thermal mass</h2>
 * The oven is a {@link ThermalBody}; every board in it adds {@link FTuning#REFLOW_BOARD_MASS} to its
 * mass, so a full cold batch drags the chamber down and a full hot one holds it up. Each board
 * follows the chamber with a lag of its own ({@link FTuning#REFLOW_BOARD_GRIP} of the gap per
 * tick), so a board is never at the oven's Tu the instant it goes in. For each board the oven
 * tracks the peak Tu it reached and the ticks it spent at or above its solder's liquidus; both
 * are graded by {@link Soldering#finish} when the batch is taken out, which is when the joint is
 * actually set.
 *
 * <h2>Overrun</h2>
 * Left in a hot oven a batch overshoots: past the alloy's window the flux burns (cold joints), past
 * what the substrate and trace survive the traces lift and the board comes back bare. Cooking a
 * batch is a mistake with a cost, never a refusal, and the solder is spent either way. A hot
 * chamber also burns a bare hand like any hot body ({@code core/hazard}).
 *
 * <h2>Sense and stop</h2>
 * Sense: a thermometer on the oven (it is a {@link ThermalBody}). Stop: take the batch out; the
 * fire below is the firebox's to cut. Toxic solder gives off fumes while it is above liquidus.
 */
public class ReflowOvenBlockEntity extends BlockEntity implements ThermalBody {

    private float temperature = FTuning.AMBIENT_TU.value();
    private final Catchup offline = new Catchup();
    private final ItemStack[] boards = new ItemStack[FTuning.REFLOW_SLOTS];
    private final float[] boardTu = new float[FTuning.REFLOW_SLOTS];
    private final float[] peak = new float[FTuning.REFLOW_SLOTS];
    private final int[] dwell = new int[FTuning.REFLOW_SLOTS];

    public ReflowOvenBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REFLOW_OVEN.get(), pos, state);
        java.util.Arrays.fill(boards, ItemStack.EMPTY);
    }

    private int count() {
        int n = 0;
        for (ItemStack b : boards)
            if (!b.isEmpty())
                n++;
        return n;
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return new ThermalMass(FTuning.REFLOW_OVEN_MASS.workPerTu() + count() * FTuning.REFLOW_BOARD_MASS);
    }

    @Override
    public Conductance getLeak() {
        return FTuning.REFLOW_OVEN_LEAK;
    }

    /** Put one pasted board in. False if there is no room or it has no solder to flow. */
    public boolean insert(ItemStack stack) {
        Board board = stack.get(FDataComponents.BOARD.get());
        // A finished circuit may be baked again (it lifts past its traces' limit); a board needs solder to flow.
        if (!stack.has(FDataComponents.CIRCUIT.get()) && (board == null || board.solder().isEmpty()))
            return false;
        for (int i = 0; i < boards.length; i++)
            if (boards[i].isEmpty()) {
                boards[i] = stack.copyWithCount(1);
                boardTu[i] = AmbientTable.at(level).value();
                peak[i] = boardTu[i];
                dwell[i] = 0;
                setChanged();
                return true;
            }
        return false;
    }

    /** Take the whole batch out, each board graded now. A lifted board comes back as its substrate. */
    public java.util.List<ItemStack> removeAll() {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < boards.length; i++) {
            if (boards[i].isEmpty())
                continue;
            ItemStack done = Soldering.finish(boards[i], materials, peak[i], dwell[i]);
            if (!done.isEmpty())
                out.add(done);
            boards[i] = ItemStack.EMPTY;
        }
        setChanged();
        return out;
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.below(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanBelow(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        setChanged();

        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        int toxic = 0;
        for (int i = 0; i < boards.length; i++) {
            if (boards[i].isEmpty())
                continue;
            boardTu[i] += FTuning.REFLOW_BOARD_GRIP * (temperature - boardTu[i]);
            peak[i] = Math.max(peak[i], boardTu[i]);
            Board held = boards[i].get(FDataComponents.BOARD.get());
            Optional<Material> solder = held == null ? Optional.empty() : held.solder().map(materials::get);
            Optional<Soldering.Alloy> alloy = solder.flatMap(Soldering::alloy);
            if (alloy.isPresent() && boardTu[i] >= alloy.get().liquidus()) {
                dwell[i]++;
                toxic = Math.max(toxic, Fumes.level(solder.get()));
            }
        }
        if (toxic > 0 && level.getGameTime() % FTuning.FUME_INTERVAL_TICKS == 0)
            Fumes.release(level, Vec3.atCenterOf(worldPosition), toxic);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        offline.save(tag, level);
        for (int i = 0; i < boards.length; i++)
            if (!boards[i].isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.put("Board", boards[i].save(registries));
                slot.putFloat("Tu", boardTu[i]);
                slot.putFloat("Peak", peak[i]);
                slot.putInt("Dwell", dwell[i]);
                tag.put("Slot" + i, slot);
            }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        offline.load(tag);
        for (int i = 0; i < boards.length; i++) {
            CompoundTag slot = tag.getCompound("Slot" + i);
            boards[i] = slot.contains("Board") ? ItemStack.parseOptional(registries, slot.getCompound("Board")) : ItemStack.EMPTY;
            boardTu[i] = slot.getFloat("Tu");
            peak[i] = slot.getFloat("Peak");
            dwell[i] = slot.getInt("Dwell");
        }
    }
}
