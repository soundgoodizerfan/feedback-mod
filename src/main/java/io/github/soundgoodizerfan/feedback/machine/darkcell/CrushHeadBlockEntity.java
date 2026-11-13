// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.HeadSlot;
import io.github.soundgoodizerfan.feedback.machine.Headed;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** The crush head's state: the seated head and the shaft that turns it. Harm per cycle is half the head's ceiling in St, scaled by how fast it turns, up to a full 16 RPM. */
public class CrushHeadBlockEntity extends RotationNode implements KillMethod, Headed {

    private final HeadSlot head = new HeadSlot();

    public CrushHeadBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.CRUSH_HEAD_ENTITY.get(), pos, state);
    }

    @Override
    public HeadSlot getHead() {
        return head;
    }

    @Override
    public Su getLoadSu() {
        return new Su(10f);
    }

    @Override
    public KillKind kind() {
        return KillKind.CRUSH;
    }

    @Override
    public boolean ready() {
        return !head.isEmpty() && Math.abs(getRpm().value()) > 0.5f;
    }

    @Override
    public float damage(MobFacts facts) {
        return head.ceiling().value() * 0.5f * Math.min(1f, Math.abs(getRpm().value()) / 16f);
    }

    /** A head too light for the mass it crushed absorbs the shortfall: force that cannot go into the work goes into the machine. */
    @Override
    public void used(int killed, float mass) {
        float need = mass * 2f;
        float ceiling = head.ceiling().value();
        head.wear(Math.max(0.5f, need - ceiling) * killed, ElectricMachineTuning.PIVOT_HEAD_WEAR_PER_ST, ElectricMachineTuning.PIVOT_HEAD_FLOOR);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        head.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        head.load(tag, registries);
    }
}
