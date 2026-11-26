// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.fusion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A held plasma loop (roadmap F5): a vessel in which hydrogen fuses to helium. Lit only when a heat source set against it is hotter than
 * {@link MachineTuning#FUSION_IGNITION_TU} (a Star Burner is) while its magnet field is powered; once lit it burns fuel, lets no waste
 * accumulate (helium is the whole of the ash, and it leaves as a gas) and gives heat to what it faces, as a fire does.
 *
 * <h2>The field holds it, and losing it is the hazard</h2>
 * The magnets are an EHV sink. While the field is held (power share at least {@link MachineTuning#FUSION_HOLD_SHARE}) the plasma climbs toward
 * {@link MachineTuning#FUSION_PLASMA_TU}; without it, a lit plasma touches the wall and the wall wears a little every tick, and a wall worn
 * through bursts, so a plasma that was let go is a bomb on a fuse, not an instant loss. Fuel running out lets the plasma cool and go out; that
 * is safe. Gentler control than fission (roadmap 6.7): nothing runs away with the field held, the fuel feed sets the power and nothing else does.
 */
public class FusionChamberBlockEntity extends ElectricNode implements HeatSource, Wearing {

    /** The gases that burn here: the hydrogen tag. */
    private static final TagKey<Fluid> FUEL = FluidTags.create(Feedback.id("hydrogen_gases"));

    private final FluidTank fuel = new FluidTank(MachineTuning.FUSION_TANK_MB, s -> s.is(FUEL)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank ash = new FluidTank(MachineTuning.FUSION_TANK_MB) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private float plasmaTu = Float.NaN;
    private float fieldShare;
    private float condition = 1f;
    private float owedHelium;
    private boolean burning;

    /** What other blocks see: fuel can be put in, ash taken out, and nothing else crosses. */
    private final IFluidHandler port = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fuel.getFluid() : ash.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return MachineTuning.FUSION_TANK_MB;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && fuel.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fuel.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return ash.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return ash.drain(maxDrain, action);
        }
    };

    public FusionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FUSION_CHAMBER.get(), pos, state);
    }

    public IFluidHandler port() {
        return port;
    }

    // --- sink: the magnets ----------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return MachineTuning.FUSION_FIELD_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.EHV;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        fieldShare = wanted > 0f && eu > 0f ? Math.min(1f, amps / wanted) : 0f;
    }

    // --- the tick -------------------------------------------------------------------------------

    public void tickServer() {
        Level level = this.level;
        if (level == null)
            return;
        HeatSource.radiate(this);
        float ambient = AmbientTable.at(level).value();
        if (Float.isNaN(plasmaTu))
            plasmaTu = ambient;
        boolean held = fieldShare >= MachineTuning.FUSION_HOLD_SHARE;
        boolean hasFuel = fuel.getFluidAmount() > 0 && ash.getFluidAmount() < MachineTuning.FUSION_TANK_MB;
        boolean lit = plasmaTu >= MachineTuning.FUSION_IGNITION_TU.value();
        // Ignition: something hotter than the ignition temperature beside it, the field up and fuel in the tank.
        if (!lit && held && hasFuel && HeatSource.at(level, worldPosition).value() >= MachineTuning.FUSION_IGNITION_TU.value())
            plasmaTu = MachineTuning.FUSION_IGNITION_TU.value();
        lit = plasmaTu >= MachineTuning.FUSION_IGNITION_TU.value();

        float target = held && hasFuel && lit ? MachineTuning.FUSION_PLASMA_TU.value() : ambient;
        plasmaTu += (target - plasmaTu) / MachineTuning.FUSION_PLASMA_MASS;
        if (plasmaTu < ambient)
            plasmaTu = ambient;
        burning = lit && held && hasFuel;
        if (burning)
            burn();
        else if (lit && !held)
            touchWall(level);
        else if (!lit && condition < 1f)
            setChanged();
    }

    /** Fuses fuel to ash: helium out for every mB of hydrogen in, leaving no solid waste. */
    private void burn() {
        int used = fuel.drain((int) MachineTuning.FUSION_BURN_MB_PER_TICK,IFluidHandler.FluidAction.EXECUTE).getAmount();
        owedHelium += used * MachineTuning.FUSION_HELIUM_PER_HYDROGEN;
        int whole = (int) owedHelium;
        if (whole > 0) {
            owedHelium -= whole;
            ash.fill(new FluidStack(FFluids.HELIUM.get(), whole), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    /** A plasma the field no longer holds touches the wall: the wall wears, and bursts once it is gone. */
    private void touchWall(Level level) {
        condition -= MachineTuning.FUSION_WALL_WEAR_PER_TICK;
        setChanged();
        if (condition <= 0f) {
            condition = 0f;
            plasmaTu = AmbientTable.at(level).value();
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    MachineTuning.FUSION_BURST_POWER, Level.ExplosionInteraction.BLOCK);
        }
    }

    // --- HeatSource: what it gives to the vessel it faces ---------------------------------------

    @Override
    public Direction heatFacing() {
        return HeatSource.facingOf(getBlockState());
    }

    /** A held plasma gives heat to every side of the face it points at, up to the most a wall can pass on. */
    @Override
    public float sideShare() {
        return 1f;
    }

    @Override
    public Tu getFireTu() {
        if (level == null || Float.isNaN(plasmaTu))
            return level == null ? MachineTuning.FUSION_IGNITION_TU : AmbientTable.at(level);
        return new Tu(Math.min(plasmaTu, MachineTuning.FUSION_OUTPUT_TU.value()));
    }

    // --- Wearing --------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return burning;
    }

    /** Plasma temperature for the readout, ambient when out. */
    public Tu plasma() {
        return new Tu(Float.isNaN(plasmaTu) ? 0f : plasmaTu);
    }

    // --- persistence ----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel", fuel.writeToNBT(registries, new CompoundTag()));
        tag.put("Ash", ash.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Condition", condition);
        if (!Float.isNaN(plasmaTu))
            tag.putFloat("Plasma", plasmaTu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.readFromNBT(registries, tag.getCompound("Fuel"));
        ash.readFromNBT(registries, tag.getCompound("Ash"));
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        plasmaTu = tag.contains("Plasma") ? tag.getFloat("Plasma") : Float.NaN;
    }
}
