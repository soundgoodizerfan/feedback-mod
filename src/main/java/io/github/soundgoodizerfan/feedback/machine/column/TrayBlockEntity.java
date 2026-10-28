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
package io.github.soundgoodizerfan.feedback.machine.column;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.multiblock.Multiblocks;
import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One cut point (spec 6.4). The reboiler hands it the liquid that condensed at its height; it holds
 * that in a draw tank and offers it to any fluid handler beside it (the draw port is a pipe). Coke
 * builds on it only from over-temperature, and takes both its efficiency and its flow with it.
 */
public class TrayBlockEntity extends BlockEntity implements Part {

    @Nullable
    private Structure structure;
    private final HeatedTank draw = new HeatedTank(FTuning.TRAY_DRAW_TANK_MB, this::setChanged);
    private float foul;

    public TrayBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.TRAY.get(), pos, state);
    }

    public TrayType type() {
        return getBlockState().getBlock() instanceof TrayBlock block ? block.type() : TrayType.SIEVE;
    }

    /** Fraction of the tray still clean, 0 to 1 (shown as machine condition). */
    public float condition() {
        return 1f - foul;
    }

    public float efficiency() {
        return type().efficiency * condition();
    }

    public HeatedTank getDraw() {
        return draw;
    }

    /** What a pipe sees: the draw can be taken from but never filled from outside, or a pipe would hand it straight back. */
    public IFluidHandler getDrawPort() {
        return drawPort;
    }

    private final IFluidHandler drawPort = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return draw.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return draw.getCapacity();
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
            return draw.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return draw.drain(maxDrain, action);
        }
    };

    /** Takes up to a clean tray's flow of {@code stack} (the liquid that condensed here); returns how much it took. */
    public int take(FluidStack stack, float tu) {
        int amount = Math.min(Math.min(Math.round(type().flow * condition()), draw.getSpace()), stack.getAmount());
        if (amount <= 0)
            return 0;
        return draw.fill(FluidHeat.stamp(stack.copyWithAmount(amount), tu), IFluidHandler.FluidAction.EXECUTE);
    }

    /** Over-temperature coats the tray, faster with a heavier feed; misuse wear only, never uptime. */
    public void coke(float tu, float heaviness) {
        if (tu <= FTuning.COLUMN_COKE_TU)
            return;
        foul = Math.min(1f, foul + FTuning.COLUMN_FOUL_PER_TICK * type().foul * heaviness
                * (tu - FTuning.COLUMN_COKE_TU) / FTuning.COLUMN_COKE_TU);
        setChanged();
    }

    /** Steam through a cold column lifts the coke. */
    public void decoke(float amount) {
        foul = Math.max(0f, foul - amount);
        setChanged();
    }

    public void tickServer() {
        if (level == null || draw.isEmpty())
            return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d),
                    d.getOpposite());
            if (target == null)
                continue;
            int fits = target.fill(draw.drain(FTuning.TRAY_DRAW_PUSH_MB, IFluidHandler.FluidAction.SIMULATE),
                    IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                target.fill(draw.drain(fits, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // --- Part ------------------------------------------------------------------------------

    @Override
    public String role() {
        return ColumnStructure.TRAY;
    }

    @Override
    public boolean connects(Direction face) {
        return face.getAxis() == Direction.Axis.Y;
    }

    @Override
    public RoleRule rule() {
        return ColumnStructure.RULE;
    }

    @Nullable
    @Override
    public Structure structure() {
        return structure;
    }

    @Override
    public void setStructure(@Nullable Structure structure) {
        this.structure = structure;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Multiblocks.load(this);
    }

    @Override
    public void setRemoved() {
        Multiblocks.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Foul", foul);
        tag.put("Draw", draw.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        foul = tag.getFloat("Foul");
        if (tag.contains("Draw"))
            draw.readFromNBT(registries, tag.getCompound("Draw"));
    }
}
