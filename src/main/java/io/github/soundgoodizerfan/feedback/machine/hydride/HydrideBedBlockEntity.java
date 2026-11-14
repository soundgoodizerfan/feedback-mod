// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hydride;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A bed of hydrogen-absorbing metal (palladium, magnesium, titanium: any metal whose {@code mechanical.hydrogen} figure is set), loaded
 * by hand an ingot at a time. A pipe fills it with hydrogen, which it soaks at low pressure up to {@code uptake} volumes of gas
 * per volume of metal; it holds the gas while cool and gives it back when it is heated past the metals' release temperature (it is a
 * {@link ThermalBody}, warmed by any heat source it touches), faster the hotter. Taken too far past the release temperature with gas
 * in it, the metal cracks as the gas leaves too fast: condition falls, to a floor where it holds half as much and never fails outright.
 */
public class HydrideBedBlockEntity extends BlockEntity implements ThermalBody {

    public static final TagKey<Fluid> HYDROGEN = TagKey.create(net.minecraft.core.registries.Registries.FLUID,
            io.github.soundgoodizerfan.feedback.Feedback.id("hydrogen_gases"));

    private final List<ResourceLocation> ingots = new ArrayList<>();
    private float absorbed;
    private float temperature = io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private long budgetTick = -1;
    private float budgetUsed;

    public HydrideBedBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.HYDRIDE_BED.get(), pos, state);
    }

    public List<ResourceLocation> ingots() {
        return ingots;
    }

    public boolean addIngot(ResourceLocation material) {
        if (ingots.size() >= HydrideTuning.MAX_INGOTS)
            return false;
        ingots.add(material);
        setChanged();
        return true;
    }

    /** Takes the last ingot out, if what it holds fits in what is left; null when it cannot. */
    public ResourceLocation takeIngot() {
        if (ingots.isEmpty())
            return null;
        ResourceLocation last = ingots.remove(ingots.size() - 1);
        if (absorbed > capacityMb()) {
            ingots.add(last);
            return null;
        }
        setChanged();
        return last;
    }

    /** What the loaded metals can hold, mB, worn by the bed's condition. */
    public float capacityMb() {
        float total = 0f;
        for (ResourceLocation id : ingots)
            total += uptake(id) * HydrideTuning.INGOT_MB;
        return total * condition;
    }

    /** 0 to 1: how full of gas it is, 0 when it has no room. */
    public float fill() {
        float cap = capacityMb();
        return cap <= 0f ? 0f : Math.min(1f, absorbed / cap);
    }

    public float condition() {
        return condition;
    }

    /** The temperature above which it gives gas back: the metals' release temperatures weighted by what each can hold. */
    public float releaseTu() {
        float weight = 0f, sum = 0f;
        for (ResourceLocation id : ingots) {
            float u = uptake(id);
            Material.Mechanical.Hydrogen h = hydrogen(id);
            if (h != null) {
                weight += u;
                sum += u * h.release().value();
            }
        }
        return weight <= 0f ? Float.POSITIVE_INFINITY : sum / weight;
    }

    public boolean giving() {
        return absorbed > 0f && temperature > releaseTu();
    }

    private static Material.Mechanical.Hydrogen hydrogen(ResourceLocation id) {
        Material m = MaterialTable.get().materials().get(id);
        return m == null ? null : m.mechanical().hydrogen().orElse(null);
    }

    private static float uptake(ResourceLocation id) {
        Material.Mechanical.Hydrogen h = hydrogen(id);
        return h == null ? 0f : h.uptake();
    }

    /** True for a metal a bed can hold. */
    public static boolean absorbs(Material material) {
        return material.mechanical().hydrogen().isPresent();
    }

    // --- the pipe's side ------------------------------------------------------------------------

    /** What a pipe sees: it fills while cool, within a rate, and drains what heat has freed, within a rate. */
    public IFluidHandler getPort() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return absorbed < 1f ? FluidStack.EMPTY : new FluidStack(FFluids.HYDROGEN.get(), (int) absorbed);
            }

            @Override
            public int getTankCapacity(int tank) {
                return (int) capacityMb();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return stack.getFluid().is(HYDROGEN);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (level == null || !resource.getFluid().is(HYDROGEN) || temperature >= releaseTu())
                    return 0;
                int take = (int) Math.min(Math.min(resource.getAmount(), capacityMb() - absorbed), room(HydrideTuning.ABSORB_MB_PER_TICK));
                if (take <= 0)
                    return 0;
                if (action.execute()) {
                    absorbed += take;
                    spend(take);
                    setChanged();
                }
                return take;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return resource.getFluid().is(HYDROGEN) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (level == null || !giving())
                    return FluidStack.EMPTY;
                float rate = HydrideTuning.RELEASE_MB_PER_TICK_PER_100_TU * (temperature - releaseTu()) / 100f;
                int give = (int) Math.min(Math.min(maxDrain, absorbed), room(rate));
                if (give <= 0)
                    return FluidStack.EMPTY;
                if (action.execute()) {
                    absorbed -= give;
                    spend(give);
                    setChanged();
                }
                return new FluidStack(FFluids.HYDROGEN.get(), give);
            }
        };
    }

    /** What is left of this tick's rate budget: a pipe may ask many times in one tick. */
    private float room(float rate) {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            budgetUsed = 0f;
        }
        return rate - budgetUsed;
    }

    private void spend(float mb) {
        budgetUsed += mb;
    }

    // --- the tick ---------------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;
        Tu ambient = AmbientTable.at(level);
        if (ingots.isEmpty()) {
            // Nothing to hold heat: an empty bed is at the room's temperature, not at what it last was.
            temperature = ambient.value();
            return;
        }
        Tu fire = HeatSource.at(level, worldPosition);
        Heat.tick(this, fire, ambient, fire.value() > ambient.value());
        float over = temperature - releaseTu();
        // Gas let out too fast cracks the metal: misuse, not use. Wear to a floor, never a failure.
        if (absorbed > 0f && over > HydrideTuning.SAFE_OVER_TU && condition > HydrideTuning.CONDITION_FLOOR) {
            condition = Math.max(HydrideTuning.CONDITION_FLOOR,
                    condition - HydrideTuning.WEAR_PER_TICK_PER_100_TU * (over - HydrideTuning.SAFE_OVER_TU) / 100f);
            setChanged();
        }
        float cap = capacityMb();
        if (absorbed > cap)
            absorbed = cap;
    }

    // --- ThermalBody -----------------------------------------------------------------------------

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
        return new ThermalMass(HydrideTuning.MASS.workPerTu() * (1f + ingots.size() / 3f));
    }

    @Override
    public Conductance getLeak() {
        return HydrideTuning.LEAK;
    }

    // --- persistence -----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ResourceLocation id : ingots)
            list.add(StringTag.valueOf(id.toString()));
        tag.put("Ingots", list);
        tag.putFloat("Absorbed", absorbed);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingots.clear();
        for (net.minecraft.nbt.Tag t : tag.getList("Ingots", net.minecraft.nbt.Tag.TAG_STRING))
            ingots.add(ResourceLocation.parse(t.getAsString()));
        absorbed = tag.getFloat("Absorbed");
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
