// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.converter;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The output side of a solid-state converter: a SOURCE that offers whatever its {@link ConverterBlockEntity} tells it to and
 * knows nothing else (digest: an actuator knows nothing). It holds nothing across a save; the converter sets it again on its
 * first resolve. It never joins the converter's own run ({@link #joins}).
 */
public class ConverterTapBlockEntity extends ElectricNode {

    private float offerVolts;
    private float offerAmps;

    public ConverterTapBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CONVERTER_TAP.get(), pos, state);
    }

    void offer(float volts, float amps) {
        offerVolts = volts;
        offerAmps = amps;
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public float sourceVolts() {
        return offerVolts;
    }

    @Override
    public float sourceAmps() {
        return offerAmps;
    }

    @Override
    public boolean joins(ElectricMember other) {
        return !(other instanceof ConverterBlockEntity);
    }
}
