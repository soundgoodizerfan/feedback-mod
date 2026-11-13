// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.compressor;

import io.github.soundgoodizerfan.feedback.machine.column.PetrochemTuning;
import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.AirIntakeFitting;
import io.github.soundgoodizerfan.feedback.registry.FFluids;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

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
public class CompressorBlockEntity extends RotationNode implements Fittable {

    /** At most one {@link AirIntakeFitting}; stateless, so a flag is its whole persistence. */
    private final List<UpgradeFitting> upgrades = new ArrayList<>();

    /** The open air as a source: it can always give what is asked, and takes nothing out of the world. */
    private final IFluidHandler airSource = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.copy();
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return new FluidStack(FFluids.AIR.get(), maxDrain);
        }
    };

    private float owed;
    private float condition = 1f;

    private final FigureStore figures = new FigureStore();

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COMPRESSOR.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return new Su(figures.get("Load", PetrochemTuning.COMPRESSOR_LOAD_SU.value()));
    }

    public float condition() {
        return condition;
    }

    public void tickServer() {
        if (level == null)
            return;
        owed += Math.abs(getRpm().value()) * figures.get("MbPerRpm", PetrochemTuning.COMPRESSOR_MB_PER_RPM) * condition;
        int amount = (int) owed;
        if (amount <= 0)
            return;
        owed -= amount;

        Direction out = getBlockState().getValue(CompressorBlock.FACING);
        IFluidHandler back = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.relative(out.getOpposite()), out);
        FluidStack offered = back == null ? FluidStack.EMPTY : back.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        boolean fromAir = false;
        if ((offered.isEmpty() || !FluidNetwork.isGas(offered)) && hasIntake()) {
            // The intake draws the open air: no source block behind it, as much as the air is thick.
            int thin = Math.round(amount * AirDensityTable.at(level));
            if (thin <= 0)
                return;
            offered = new FluidStack(FFluids.AIR.get(), thin);
            fromAir = true;
        } else if (back == null || offered.isEmpty() || !FluidNetwork.isGas(offered))
            return;
        IFluidHandler source = fromAir ? airSource : back;
        FluidStack hot = FluidHeat.stamp(offered, FluidHeat.of(offered).value() + figures.get("Heat", PetrochemTuning.COMPRESSOR_HEAT_TU));

        BlockPos front = worldPosition.relative(out);
        int fits;
        if (level.getBlockEntity(front) instanceof PipeBlockEntity pipe) {
            fits = pipe.compressedFill(hot, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                pipe.compressedFill(FluidHeat.stamp(source.drain(offered.copyWithAmount(fits),
                        IFluidHandler.FluidAction.EXECUTE), FluidHeat.of(hot).value()), IFluidHandler.FluidAction.EXECUTE);
        } else if (level.getBlockEntity(front) instanceof io.github.soundgoodizerfan.feedback.machine.tube.TubeBlockEntity tube) {
            fits = tube.compressedFill(hot, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                tube.compressedFill(FluidHeat.stamp(source.drain(offered.copyWithAmount(fits),
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
            condition = Math.max(PetrochemTuning.COMPRESSOR_CONDITION_FLOOR, condition - PetrochemTuning.COMPRESSOR_SURGE_WEAR_PER_TICK);
            setChanged();
        }
    }

    private boolean hasIntake() {
        return upgrades.stream().anyMatch(AirIntakeFitting.class::isInstance);
    }

    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return null;
    }

    @Override
    public void setSidedFitting(Direction side, SidedFitting fitting) {
    }

    @Override
    public boolean canMount(SidedFitting fitting, Direction side) {
        return false;
    }

    /** The one upgrade a compressor reads is an air intake, one of it. */
    @Override
    public boolean canAdd(UpgradeFitting upgrade) {
        return upgrade instanceof AirIntakeFitting && !hasIntake();
    }

    @Override
    public List<UpgradeFitting> getUpgrades() {
        return upgrades;
    }

    @Override
    public boolean addUpgrade(UpgradeFitting upgrade) {
        boolean added = upgrades.add(upgrade);
        if (added)
            setChanged();
        return added;
    }

    @Override
    public void removeUpgrade(UpgradeFitting upgrade) {
        if (upgrades.remove(upgrade))
            setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putFloat("Condition", condition);
        tag.putBoolean("AirIntake", hasIntake());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        upgrades.clear();
        if (tag.getBoolean("AirIntake"))
            upgrades.add(new AirIntakeFitting());
    }
}
