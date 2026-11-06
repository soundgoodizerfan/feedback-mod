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
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import io.github.soundgoodizerfan.feedback.core.hazard.Fumes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A gas tank in a cell's wall. A gas is as harmful as its {@code toxic} perk and kills only what breathes. Each cycle that kills spends
 * {@link #PER_CYCLE_MB}. A tank that is full and has nothing to work on seeps into the room once a second: a leak is a hazard.
 */
public class FumePortBlockEntity extends BlockEntity implements KillMethod {

    /** Invented. Gas one working cycle spends, mB. */
    public static final int PER_CYCLE_MB = 50;
    /** Invented. Harm per point of toxicity, per cycle. */
    private static final float DAMAGE_PER_TOXIC = 8f;

    private final FluidTank tank = new FluidTank(2000, f -> toxic(f) > 0);

    public FumePortBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.FUME_PORT_ENTITY.get(), pos, state);
    }

    private static int toxic(net.neoforged.neoforge.fluids.FluidStack fluid) {
        if (fluid.isEmpty())
            return 0;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
        return m == null ? 0 : Fumes.level(m);
    }

    public IFluidHandler tank() {
        return tank;
    }

    @Override
    public KillKind kind() {
        return KillKind.FUMES;
    }

    @Override
    public boolean ready() {
        return tank.getFluidAmount() >= PER_CYCLE_MB && toxic(tank.getFluid()) > 0;
    }

    @Override
    public float damage(MobFacts facts) {
        return facts.breathes() ? toxic(tank.getFluid()) * DAMAGE_PER_TOXIC : 0f;
    }

    @Override
    public void used(int killed, float mass) {
        tank.drain(PER_CYCLE_MB, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    public void tickServer() {
        if (level != null && level.getGameTime() % 20 == 0 && tank.getFluidAmount() >= tank.getCapacity() - 100) {
            int level0 = toxic(tank.getFluid());
            if (level0 > 0) {
                tank.drain(20, IFluidHandler.FluidAction.EXECUTE);
                Fumes.release(level, Vec3.atCenterOf(worldPosition), level0);
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
