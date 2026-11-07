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
