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
package io.github.soundgoodizerfan.feedback.machine.dynamo;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The motor and the dynamo as one placed machine (roadmap E5), built at the Tool Table from a frame, a winding, a core and a
 * housing ({@code ModularBake#machine}); the figures are copied into the block at build and ride the item and the block alike
 * ({@link FDataComponents#ELECTRIC_MACHINE}).
 *
 * <h2>One winding figure, used twice</h2>
 * A generator's voltage rises with the speed it is turned at, and the torque it takes rises with the current drawn from it, by
 * the <em>same</em> constant -- the winding's (in a real machine {@code k_e = k_t}). So {@code voltsPerRpm} sets the voltage
 * from the shaft's live RPM, and the same number times the current delivered is the Su it charges back to that shaft run. That
 * is conservation stated once, not an exchange rate: a dynamo that is not drawn from costs the shaft nothing. Run as a motor
 * the figure is read backwards: the voltage it sees sets the speed it drives toward, and the current it is delivered sets the
 * torque. A generator feeding a motor, or a motor turning a generator, passes power through unchanged apart from the shaft's
 * friction and the machines' own heat -- which is what makes the pair a voltage converter (the motor-generator set).
 *
 * <h2>The torque-speed line</h2>
 * A real DC motor's back-EMF rises with speed until it cancels the supply, so the current it draws falls linearly from stall to
 * nothing at its no-load speed, {@code volts / voltsPerRpm}. The rated amps stand in for the stall current. An idle motor draws
 * next to nothing, a loaded one slows until the current it pulls carries the load, an under-voltage motor runs slow rather than
 * not at all, and an over-voltage one runs fast.
 *
 * <h2>Heat and wear</h2>
 * The core's loss (a share of the power through it) and the winding's (amps squared times the conductor's drop over its
 * length) both go into the body as heat; the housing sheds it to air at its conductivity over the frame's wall area, and a
 * jacket with water in it, while the body is above ambient, sheds at {@link FTuning#JACKET_SHED_MULTIPLE} times that and
 * spends a millibucket every {@link FTuning#JACKET_TICKS_PER_MB} ticks. A draw over the rated current (a generator) or volts
 * past the rated band (a motor) are force that cannot go into the work, so they go into the body as more heat and as lost
 * condition, and so does running past the housing's service temperature. Condition stops at
 * {@link FTuning#MACHINE_CONDITION_FLOOR}; it never refuses a line and is never destroyed.
 *
 * <h2>Why it is not a {@link RotationNode}</h2>
 * It is an {@link ElectricNode}, and a block entity can extend only one of the two. It couples to the shaft run on its facing
 * face the way a Pulley's belt couples to a far run, through {@code RotationNetwork.loadByBelt} and {@code driveByBelt}, every
 * tick, so neither propagator learns that the other exists. The load is flat, not referred through the shaft's gear ratio, the
 * choice {@code RotationNetwork.recalculate} makes for every machine's working draw. It does not spin on screen yet:
 * placeholder art.
 */
public class ElectricMachineBlockEntity extends ElectricNode implements ThermalBody, Wearing {

    // Figures baked at the Tool Table; the defaults are the smallest, coarsest machine, for a block placed with none.
    private float frameSu = FTuning.FRAME_SU_SMALL;
    private float voltsPerRpm = FTuning.WINDING_VOLTS_PER_RPM_COARSE;
    private float ratedAmps = Math.min(FTuning.FRAME_SU_SMALL / FTuning.WINDING_VOLTS_PER_RPM_COARSE, FTuning.WINDING_AMPS_PER_AMPACITY);
    private VoltageBand band = VoltageBand.HV;
    private float mass = FTuning.FRAME_MASS_SMALL.workPerTu();
    private float leak = 0.04f;
    private float serviceTu = FTuning.MACHINE_SERVICE_TU;
    private float windingDrop;
    private float coreLoss = FTuning.CORE_LOSS_DEFAULT;
    private boolean jacketed;

    // State.
    private float condition = 1f;
    private float temperature = FTuning.AMBIENT_TU.value();
    private int water;
    private int coolTicks;
    private boolean cooling;
    /** The shaft's speed last tick. Not persisted: one tick after loading it is read again. */
    private float shaftRpm;

    public ElectricMachineBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTRIC_MACHINE.get(), pos, state);
    }

    private boolean generator() {
        return getBlockState().getValue(ElectricMachineBlock.MODE) == ElectricMachineBlock.Mode.GENERATOR;
    }

    // --- figures ------------------------------------------------------------------------------

    public float getFrameSu() {
        return frameSu;
    }

    public float getVoltsPerRpm() {
        return voltsPerRpm;
    }

    /** The current it carries before it is overdrawn, A. */
    public float getRatedAmps() {
        return ratedAmps;
    }

    public boolean isJacketed() {
        return jacketed;
    }

    /** Whether the jacket is shedding heat right now: water in it and the body above ambient. */
    public boolean isCooling() {
        return cooling;
    }

    public int getWater() {
        return water;
    }

    /** The speed of the shaft it sits on, for display and tests. */
    public float getShaftRpm() {
        return shaftRpm;
    }

    // --- electric -----------------------------------------------------------------------------

    @Override
    public Role role() {
        return generator() ? Role.SOURCE : Role.SINK;
    }

    @Override
    public VoltageBand ratedBand() {
        return band;
    }

    @Override
    public float sourceVolts() {
        return shaftRpm * voltsPerRpm;
    }

    @Override
    public float sourceAmps() {
        return shaftRpm > 0 ? ratedAmps * condition : 0;
    }

    /** The speed the voltage it sees would carry a motor to with no load at all. */
    public float noLoadRpm() {
        return getVolts() / voltsPerRpm;
    }

    @Override
    public float wantedAmps() {
        if (generator())
            return 0f;
        float free = noLoadRpm();
        // Seeing no voltage yet reads as stalled, so it asks for the stall current and is told.
        float back = free > 0 ? Math.min(1f, shaftRpm / free) : 0f;
        return ratedAmps * (1f - back);
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (level == null)
            return;
        float power = volts * amps;
        float excess = generator() ? (ratedAmps > 0f ? Math.max(0f, amps / ratedAmps - 1f) : 0f) : ElectricRules.overVoltage(volts, band);
        if (power > 0f)
            Heat.warm(this, power * coreLoss + amps * amps * windingDrop + FTuning.MACHINE_HEAT_PER_EXCESS * power * excess);
        float lost = FTuning.MACHINE_WEAR_PER_EXCESS * excess;
        if (temperature > serviceTu)
            lost += (temperature - serviceTu) * FTuning.MACHINE_HEAT_WEAR_PER_TU;
        if (lost > 0f) {
            condition = Math.max(FTuning.MACHINE_CONDITION_FLOOR, condition - lost);
            setChanged();
        }
        Tu ambient = AmbientTable.at(level);
        cooling = jacketed && water > 0 && temperature > ambient.value();
        if (cooling && ++coolTicks >= FTuning.JACKET_TICKS_PER_MB) {
            coolTicks = 0;
            water--;
            setChanged();
        }
        Heat.tick(this, ambient, ambient, false);
    }

    // --- the shaft ------------------------------------------------------------------------------

    /** The torque the current drawn last tick costs the shaft as a generator, in Su. */
    public Su getShaftLoad() {
        return new Su(getAmps() * voltsPerRpm);
    }

    /** The torque the current delivered last tick puts on the shaft as a motor, in Su, as much as the winding's condition lets through. */
    public Su getShaftTorque() {
        return new Su(getAmps() * voltsPerRpm * condition);
    }

    public void tickServer() {
        RotationNode shaft = shaft();
        shaftRpm = shaft == null ? 0 : Math.abs(shaft.getRpm().value());
        if (shaft == null || shaft.getNetwork() == null)
            return;
        if (generator()) {
            if (getAmps() > 0)
                shaft.getNetwork().loadByBelt(getShaftLoad());
        } else if (shaft.getRatio() != 0 && getAmps() > 0) {
            // Into the run's own frame through the coupling node's ratio, as a pulley does.
            shaft.getNetwork().driveByBelt(new Rpm(noLoadRpm() / shaft.getRatio()), getShaftTorque());
        }
    }

    /** The rotation node on the shaft face whose own axle points at this block, or null. */
    @Nullable
    private RotationNode shaft() {
        if (level == null)
            return null;
        Direction facing = getBlockState().getValue(ElectricMachineBlock.FACING);
        BlockPos at = worldPosition.relative(facing);
        if (!level.isLoaded(at))
            return null;
        BlockState state = level.getBlockState(at);
        return state.getBlock() instanceof Rotatable rotatable
                && rotatable.hasShaftTowards(level, at, state, facing.getOpposite())
                && level.getBlockEntity(at) instanceof RotationNode node ? node : null;
    }

    // --- Wearing, ThermalBody -------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    /** A machine on a shaft is working and cannot be measured, like any machine (philosophy 8). */
    @Override
    public boolean isRunning() {
        return getAmps() > 0f;
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
        return new ThermalMass(mass);
    }

    @Override
    public Conductance getLeak() {
        return new Conductance(leak * (cooling ? FTuning.JACKET_SHED_MULTIPLE : 1f));
    }

    // --- the jacket -------------------------------------------------------------------------------

    /** The jacket's fluid face: water fills, nothing drains. Only a jacketed housing has one. */
    public final IFluidHandler fluids = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return new FluidStack(Fluids.WATER, water);
        }

        @Override
        public int getTankCapacity(int tank) {
            return jacketed ? FTuning.JACKET_TANK_MB : 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return jacketed && stack.is(FluidTags.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!jacketed || !resource.is(FluidTags.WATER))
                return 0;
            int take = Math.min(resource.getAmount(), FTuning.JACKET_TANK_MB - water);
            if (take > 0 && action.execute()) {
                water += take;
                setChanged();
            }
            return Math.max(0, take);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    // --- persistence ----------------------------------------------------------------------------

    /** The design the machine was built from, kept so the broken block drops the same item. */
    private ModularDesign design;
    private ModularFigures figures;

    /** The figures and the state, as the item carries them ({@code FDataComponents.ELECTRIC_MACHINE}) and the block saves them. */
    private CompoundTag writeState(CompoundTag tag) {
        tag.putFloat("FrameSu", frameSu);
        tag.putFloat("VoltsPerRpm", voltsPerRpm);
        tag.putFloat("RatedAmps", ratedAmps);
        tag.putString("Band", band.getSerializedName());
        tag.putFloat("Mass", mass);
        tag.putFloat("Leak", leak);
        tag.putFloat("ServiceTu", serviceTu);
        tag.putFloat("WindingDrop", windingDrop);
        tag.putFloat("CoreLoss", coreLoss);
        tag.putBoolean("Jacketed", jacketed);
        tag.putFloat("Condition", condition);
        tag.putFloat("Temperature", temperature);
        tag.putInt("Water", water);
        tag.putInt("CoolTicks", coolTicks);
        return tag;
    }

    private void readState(CompoundTag tag) {
        frameSu = tag.contains("FrameSu") ? tag.getFloat("FrameSu") : frameSu;
        voltsPerRpm = tag.contains("VoltsPerRpm") ? Math.max(1e-3f, tag.getFloat("VoltsPerRpm")) : voltsPerRpm;
        ratedAmps = tag.contains("RatedAmps") ? tag.getFloat("RatedAmps") : ratedAmps;
        if (tag.contains("Band"))
            band = VoltageBand.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(tag.getString("Band"))).result().orElse(VoltageBand.HV);
        mass = tag.contains("Mass") ? tag.getFloat("Mass") : mass;
        leak = tag.contains("Leak") ? tag.getFloat("Leak") : leak;
        serviceTu = tag.contains("ServiceTu") ? tag.getFloat("ServiceTu") : serviceTu;
        windingDrop = tag.contains("WindingDrop") ? tag.getFloat("WindingDrop") : windingDrop;
        coreLoss = tag.contains("CoreLoss") ? tag.getFloat("CoreLoss") : coreLoss;
        jacketed = tag.getBoolean("Jacketed");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        water = Math.max(0, tag.getInt("Water"));
        coolTicks = tag.getInt("CoolTicks");
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag state = input.get(FDataComponents.ELECTRIC_MACHINE.get());
        if (state != null)
            readState(state);
        design = input.get(FModular.DESIGN.get());
        figures = input.get(FModular.FIGURES.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(FDataComponents.ELECTRIC_MACHINE.get(), writeState(new CompoundTag()));
        if (design != null)
            components.set(FModular.DESIGN.get(), design);
        if (figures != null)
            components.set(FModular.FIGURES.get(), figures);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeState(tag);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (design != null)
            ModularDesign.CODEC.encodeStart(ops, design).result().ifPresent(t -> tag.put("Design", t));
        if (figures != null)
            ModularFigures.CODEC.encodeStart(ops, figures).result().ifPresent(t -> tag.put("Figures", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readState(tag);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        design = tag.contains("Design") ? ModularDesign.CODEC.parse(ops, tag.get("Design")).result().orElse(null) : null;
        figures = tag.contains("Figures") ? ModularFigures.CODEC.parse(ops, tag.get("Figures")).result().orElse(null) : null;
    }
}
