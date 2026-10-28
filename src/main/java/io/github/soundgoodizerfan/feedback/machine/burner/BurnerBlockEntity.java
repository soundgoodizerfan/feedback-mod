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
package io.github.soundgoodizerfan.feedback.machine.burner;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Burns a liquid or gas fuel, pipe-fed, and gives its heat to whatever sits on top of it, the way the
 * firebox does (it is a {@link HeatSource}); the firebox stays the item-fuel one. It names no fuel:
 * whatever arrives, its material's {@code combustion} sets the flame temperature and {@code
 * oxygen_balance} how much oxidiser it brings, a fuel carrying its own burning as if blown. It
 * reaches for fuel from its neighbours and never filters; a fluid that does not burn just sits there.
 * shortcut: no catch-up across unloaded time, and the air an ordinary fuel burns in is assumed.
 */
public class BurnerBlockEntity extends BlockEntity implements HeatSource {

    private final HeatedTank fuel = new HeatedTank(FTuning.BURNER_TANK_MB, this::setChanged);
    private float owed;
    private float flameTu;

    public BurnerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BURNER.get(), pos, state);
    }

    public HeatedTank getFuel() {
        return fuel;
    }

    private Material.Combustion combustion() {
        if (fuel.isEmpty())
            return null;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(fuel.getFluid().getFluid()));
        return m == null ? null : m.combustion().filter(c -> c.flameTemperature().isPresent()).orElse(null);
    }

    @Override
    public Tu getFireTu() {
        return flameTu > 0f ? new Tu(flameTu) : AmbientTable.at(level);
    }

    public void tickServer() {
        if (level == null)
            return;
        pull();
        Material.Combustion c = combustion();
        float before = flameTu;
        if (c == null) {
            flameTu = 0f;
        } else {
            float blown = Mth.clamp(c.oxygenBalance(), 0f, 1f);
            flameTu = c.flameTemperature().get().value() * Mth.lerp(blown, 1f, FTuning.FULL_AIR_TEMPERATURE_FACTOR);
            owed += FTuning.BURNER_MB_PER_TICK;
            int drawn = (int) owed;
            owed -= drawn;
            if (drawn > 0)
                fuel.drain(drawn, IFluidHandler.FluidAction.EXECUTE);
        }
        if ((before > 0f) != (flameTu > 0f) && getBlockState().hasProperty(BurnerBlock.LIT))
            level.setBlock(worldPosition, getBlockState().setValue(BurnerBlock.LIT, flameTu > 0f), 3);
    }

    private void pull() {
        int needed = fuel.getSpace();
        for (Direction face : Direction.values()) {
            if (needed <= 0)
                return;
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face),
                    face.getOpposite());
            if (neighbour == null)
                continue;
            FluidStack offered = neighbour.drain(needed, IFluidHandler.FluidAction.SIMULATE);
            int fits = offered.isEmpty() ? 0 : fuel.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                needed -= fuel.fill(neighbour.drain(offered.copyWithAmount(fits), IFluidHandler.FluidAction.EXECUTE),
                        IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel", fuel.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fuel"))
            fuel.readFromNBT(registries, tag.getCompound("Fuel"));
    }
}
