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
package io.github.soundgoodizerfan.feedback.machine.compressor;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Moves gas from its back face to its front while turned, and forces it into a pipe run past the
 * one-volume limit passive flow stops at (spec 8.3): that is what raises a line's pressure. It
 * heats what it moves, so a hot outlet is a decision about cooling and, with naphtha vapour, a hazard.
 * Run against a line that cannot take more and it surges: condition falls to a floor and never
 * lower. Wear comes only from that misuse, never from running correctly.
 */
public class CompressorBlockEntity extends RotationNode {

    private float owed;
    private float condition = 1f;

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COMPRESSOR.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.COMPRESSOR_LOAD_SU;
    }

    public float condition() {
        return condition;
    }

    public void tickServer() {
        if (level == null)
            return;
        owed += Math.abs(getRpm().value()) * FTuning.COMPRESSOR_MB_PER_RPM * condition;
        int amount = (int) owed;
        if (amount <= 0)
            return;
        owed -= amount;

        Direction out = getBlockState().getValue(CompressorBlock.FACING);
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.relative(out.getOpposite()), out);
        if (source == null)
            return;
        FluidStack offered = source.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty() || !FluidNetwork.isGas(offered))
            return;
        FluidStack hot = FluidHeat.stamp(offered, FluidHeat.of(offered).value() + FTuning.COMPRESSOR_HEAT_TU);

        BlockPos front = worldPosition.relative(out);
        int fits;
        if (level.getBlockEntity(front) instanceof PipeBlockEntity pipe) {
            fits = pipe.compressedFill(hot, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                pipe.compressedFill(FluidHeat.stamp(source.drain(offered.copyWithAmount(fits),
                        IFluidHandler.FluidAction.EXECUTE), FluidHeat.of(hot).value()), IFluidHandler.FluidAction.EXECUTE);
        } else if (level.getBlockEntity(front) instanceof io.github.soundgoodizerfan.feedback.machine.gastank.GasTankBlockEntity tank) {
            fits = tank.compressedFill(hot, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                tank.compressedFill(FluidHeat.stamp(source.drain(offered.copyWithAmount(fits),
                        IFluidHandler.FluidAction.EXECUTE), FluidHeat.of(hot).value()), IFluidHandler.FluidAction.EXECUTE);
        } else {
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, front, out.getOpposite());
            fits = target == null ? 0 : target.fill(hot, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                target.fill(FluidHeat.stamp(source.drain(offered.copyWithAmount(fits), IFluidHandler.FluidAction.EXECUTE),
                        FluidHeat.of(hot).value()), IFluidHandler.FluidAction.EXECUTE);
        }
        if (fits <= 0) {
            condition = Math.max(FTuning.COMPRESSOR_CONDITION_FLOOR, condition - FTuning.COMPRESSOR_SURGE_WEAR_PER_TICK);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
