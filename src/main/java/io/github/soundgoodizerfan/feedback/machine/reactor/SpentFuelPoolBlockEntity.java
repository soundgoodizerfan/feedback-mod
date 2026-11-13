// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.core.hazard.RadiationEmitter;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Where spent rods go to cool. A freshly discharged rod keeps making decay heat ({@link ReactorTuning#REACTOR_SPENT_HEAT} work a tick, falling
 * over days); the pool is a body of water that takes it. Over the boiling point the water boils away and the pool needs topping up. A
 * pool left dry heats the rods until they slump into slag. Water over the rods also shields them: a dry pool is a source of dose.
 */
public class SpentFuelPoolBlockEntity extends BlockEntity implements ThermalBody, RadiationEmitter {

    private final NonNullList<ItemStack> rods = NonNullList.withSize(ReactorTuning.REACTOR_POOL_SLOTS, ItemStack.EMPTY);
    private final FluidTank water = new FluidTank(ReactorTuning.REACTOR_POOL_TANK_MB, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private float temperature = ThermalTuning.AMBIENT_TU.value();
    private float boilAcc;

    public SpentFuelPoolBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SPENT_FUEL_POOL.get(), pos, state);
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
        return ReactorTuning.REACTOR_POOL_MASS;
    }

    public FluidTank water() {
        return water;
    }

    public IFluidHandler handler() {
        return water;
    }

    public int count() {
        int n = 0;
        for (ItemStack r : rods)
            if (!r.isEmpty())
                n++;
        return n;
    }

    public boolean put(ItemStack stack) {
        if (!stack.is(FItems.SPENT_FUEL_ROD.get()))
            return false;
        for (int i = 0; i < rods.size(); i++)
            if (rods.get(i).isEmpty()) {
                rods.set(i, stack.copyWithCount(1));
                setChanged();
                return true;
            }
        return false;
    }

    public ItemStack take() {
        for (int i = rods.size() - 1; i >= 0; i--)
            if (!rods.get(i).isEmpty()) {
                ItemStack out = rods.get(i);
                rods.set(i, ItemStack.EMPTY);
                setChanged();
                return out;
            }
        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> contents() {
        return rods;
    }

    /** A rod's decay heat this tick, in work: falls with the time since it was discharged; a rod with no discharge stamp is old. */
    static float decayHeat(ItemStack rod, long now) {
        CompoundTag tag = rod.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("Discharged"))
            return 0f;
        long age = Math.max(0L, now - tag.getLong("Discharged"));
        return ReactorTuning.REACTOR_SPENT_HEAT * (float) Math.exp(-age / ReactorTuning.REACTOR_SPENT_TAU_TICKS);
    }

    public void tickServer() {
        Level lvl = level;
        if (lvl == null || count() == 0 && temperature <= AmbientTable.at(lvl).value() + 0.01f)
            return;
        float work = 0f;
        for (ItemStack r : rods)
            if (!r.isEmpty())
                work += decayHeat(r, lvl.getGameTime());
        Heat.warm(this, work);
        Tu ambient = AmbientTable.at(lvl);
        Heat.tick(this, ambient, ambient, false);
        // Over the boiling point the water boils off: the heat it takes with it is what holds the pool there.
        var cut = ThermalProcessTable.get().boiling(new FluidStack(Fluids.WATER, 1)).orElse(null);
        if (cut != null && water.getFluidAmount() > 0 && temperature > cut.boil().value()) {
            boilAcc += (temperature - cut.boil().value()) * ReactorTuning.REACTOR_POOL_MASS.workPerTu() / cut.latent();
            int mb = Math.min((int) boilAcc, water.getFluidAmount());
            if (mb > 0) {
                water.drain(mb, IFluidHandler.FluidAction.EXECUTE);
                boilAcc -= mb;
                Heat.warm(this, -mb * cut.latent());
            }
        }
        if (count() > 0 && water.isEmpty() && temperature > ReactorTuning.REACTOR_POOL_MELT_TU)
            slump();
        setChanged();
    }

    /** A dry pool past the melting line: the block and the rods in it become slag. */
    private void slump() {
        if (level == null)
            return;
        level.setBlock(worldPosition, FBlocks.CORE_SLAG.get().defaultBlockState(), 3);
        if (level.getBlockEntity(worldPosition) instanceof CoreSlagBlockEntity slag)
            slag.stamp(ReactorTuning.REACTOR_SLAG_RU, level.getGameTime());
    }

    @Override
    public float radiationRu() {
        float dry = 0f;
        for (ItemStack r : rods)
            if (!r.isEmpty())
                dry += ReactorTuning.REACTOR_POOL_DRY_RU;
        return water.isEmpty() ? dry : dry * ReactorTuning.REACTOR_POOL_WET_SHARE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, rods, registries);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rods.clear();
        ContainerHelper.loadAllItems(tag, rods, registries);
        water.readFromNBT(registries, tag.getCompound("Water"));
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : ThermalTuning.AMBIENT_TU.value();
    }
}
