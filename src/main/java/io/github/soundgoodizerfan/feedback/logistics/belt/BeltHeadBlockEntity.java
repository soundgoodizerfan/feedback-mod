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
package io.github.soundgoodizerfan.feedback.logistics.belt;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.registry.FLogistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A run's drive. Its shaft network's {@code Rpm} times a roller constant is the belt speed, and its
 * {@code Su} load is a bare roller's plus a term per Mu of cargo on the run: a loaded belt is heavy to
 * turn, and an undersized shaft slows the whole run (the rotation ledger does that); the belt never fails from load.
 */
public class BeltHeadBlockEntity extends RotationNode implements CargoSave.Holder {

    private List<Object[]> pending = List.of();

    public BeltHeadBlockEntity(BlockPos pos, BlockState state) {
        super(FLogistics.HEAD_ENTITY.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        BeltRun run = level == null ? null : BeltRuns.of(level).runAt(worldPosition);
        float carried = run == null ? 0f : run.mass() * run.length();
        return new Su(LogisticsTuning.BELT_BASE_LOAD_SU + LogisticsTuning.BELT_LOAD_SU_PER_MU * carried);
    }

    @Override
    public List<Object[]> takePending() {
        List<Object[]> out = pending;
        pending = List.of();
        return out;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).queue(worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide)
            BeltRuns.of(level).queue(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        List<Object[]> over = level == null || level.isClientSide ? pending : BeltRuns.of(level).savedOver(worldPosition);
        if (over.isEmpty())
            over = pending;
        if (!over.isEmpty())
            tag.put("Cargo", CargoSave.write(over, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cargo"))
            pending = CargoSave.read(tag.getList("Cargo", 10), registries);
    }
}
