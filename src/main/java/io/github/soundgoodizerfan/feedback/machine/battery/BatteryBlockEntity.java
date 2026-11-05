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
package io.github.soundgoodizerfan.feedback.machine.battery;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.hazard.HotIgnition;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Weldable;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;

/**
 * A lead-acid battery: {@code cells} secondary cells in series, built at the Tool Table (roadmap E4). It charges from any
 * line above its terminal voltage and supplies when the line drops, holding a flat voltage until nearly empty.
 *
 * <h2>One role at a time, like the Leyden Jar</h2>
 * A sink while charging, a source while supplying; a flip rebuilds its run ({@code ElectricPropagator.rebuildFrom}), an
 * event and not a per-tick cost. {@link #onResolved} integrates the amps the run delivered or drew (a few float
 * operations); self-discharge is caught up from the last game time every ten seconds, and gas and ignition are checked
 * once or twice a second and only while there is gas.
 *
 * <h2>What the figures are</h2>
 * {@code capacity} is one cell's amp-ticks (series cells add volts, not capacity). The plates' volts, knee and capacity per
 * ingot are the plate material's {@code electrical.secondary_cell}, copied into the block at build; the case's insulation
 * band is the highest voltage it holds off. Capacity falls with condition, with cold (body temperature below ambient) and
 * with exposed plates (low water). Misuse only: overcharge vents water and releases hydrogen, overdraw heats the case and
 * costs condition, heat over {@link FTuning#BATTERY_HOT_TU} costs condition, volts over the case's band cost condition.
 */
public class BatteryBlockEntity extends ElectricNode implements ThermalBody, Weldable {

    public static final TagKey<Fluid> ACID = TagKey.create(net.minecraft.core.registries.Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(io.github.soundgoodizerfan.feedback.Feedback.MOD_ID, "battery_acid"));

    private int cells = 1;
    private float capacity = 24000f * FTuning.BATTERY_INGOTS_PER_CELL;
    private float voltsFull = 2.1f, voltsLow = 1.95f, voltsEmpty = 1.6f, coldFactor = 0.5f;
    private VoltageBand caseBand = VoltageBand.LV;

    private float charge;
    private float condition = 1f;
    private float maxCondition = 1f;
    private float water;
    private float acid;
    private float gas;
    private float temperature = FTuning.AMBIENT_TU.value();
    private boolean supplying;
    private boolean venting;
    private long lastTime = -1;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BATTERY.get(), pos, state);
    }

    // --- figures ------------------------------------------------------------------------------

    public int cells() {
        return cells;
    }

    private float coldShare() {
        float t = temperature;
        float room = FTuning.AMBIENT_TU.value();
        if (t >= room)
            return 1f;
        return Mth.lerp(Mth.clamp((t - FTuning.BATTERY_DEEP_COLD_TU) / (room - FTuning.BATTERY_DEEP_COLD_TU), 0f, 1f), coldFactor, 1f);
    }

    private float waterShare() {
        float low = FTuning.BATTERY_WATER_LOW_SHARE * FTuning.BATTERY_WATER_TANK_MB;
        return water >= low ? 1f : Mth.lerp(water / low, FTuning.BATTERY_DRY_CAPACITY, 1f);
    }

    /** What the cells can hold right now, in amp-ticks. */
    public float effectiveCapacity() {
        return capacity * condition * coldShare() * waterShare();
    }

    /** State of charge, 0 to 1 of what it can hold now. */
    public float stateOfCharge() {
        float eff = effectiveCapacity();
        return eff <= 0f ? 0f : Mth.clamp(charge / eff, 0f, 1f);
    }

    /** Terminal volts of the whole stack: flat near nominal, then the cliff. */
    public float terminalVolts() {
        float soc = stateOfCharge();
        float cell = soc > 0.1f ? Mth.lerp((soc - 0.1f) / 0.9f, voltsLow, voltsFull) : Mth.lerp(soc / 0.1f, voltsEmpty, voltsLow);
        return cells * cell;
    }

    /** Acid filled to the cells' need: without it nothing happens. */
    public boolean ready() {
        return acid >= cells * FTuning.BATTERY_ACID_MB_PER_CELL - 0.5f;
    }

    public float water() {
        return water;
    }

    public float acid() {
        return acid;
    }

    public float gas() {
        return gas;
    }

    public boolean isSupplying() {
        return supplying;
    }

    public boolean isVenting() {
        return venting;
    }

    public VoltageBand caseBand() {
        return caseBand;
    }

    private boolean full() {
        return charge >= effectiveCapacity() - 1e-3f;
    }

    // --- electric -----------------------------------------------------------------------------

    @Override
    public Role role() {
        return supplying ? Role.SOURCE : Role.SINK;
    }

    @Override
    public VoltageBand ratedBand() {
        return caseBand;
    }

    @Override
    public float touchVolts() {
        float held = terminalVolts();
        return held >= FTuning.SHOCK_MIN_VOLTS && charge > 0f ? held : 0f;
    }

    @Override
    public float sourceVolts() {
        return supplying && ready() && charge > 0f ? terminalVolts() : 0f;
    }

    @Override
    public float sourceAmps() {
        return supplying && ready() && charge > 0f ? effectiveCapacity() / FTuning.BATTERY_DISCHARGE_TICKS : 0f;
    }

    /** Charges while the line is above the terminal; a full cell asks only a trickle, and only when the line is over its gassing voltage. */
    @Override
    public float wantedAmps() {
        if (supplying || !ready())
            return 0f;
        float chargeAmps = effectiveCapacity() / FTuning.BATTERY_CHARGE_TICKS;
        if (getVolts() <= terminalVolts() + FTuning.BATTERY_SWITCH_VOLTS)
            return 0f;
        if (!full())
            return chargeAmps;
        return getVolts() > cells * voltsFull * FTuning.BATTERY_GASSING_FACTOR ? chargeAmps * FTuning.BATTERY_TRICKLE_SHARE : 0f;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (level == null)
            return;
        boolean dirty = false;
        float heat = 0f;
        if (supplying) {
            if (amps > 0f) {
                charge = Math.max(0f, charge - amps);
                float limit = effectiveCapacity() / FTuning.BATTERY_DISCHARGE_TICKS;
                float over = limit <= 0f ? 0f : amps / limit - 1f;
                if (over > 0f) {
                    condition = Math.max(FTuning.BATTERY_CONDITION_FLOOR, condition - FTuning.BATTERY_OVERDRAW_WEAR * over);
                    heat += FTuning.BATTERY_OVERDRAW_HEAT * over;
                }
                dirty = true;
            }
        } else if (amps > 0f) {
            boolean wasFull = full();
            float ventAmps = 0f;
            if (wasFull)
                ventAmps = amps;
            else {
                charge = Math.min(effectiveCapacity(), charge + amps * FTuning.BATTERY_CHARGE_EFFICIENCY);
                heat += amps * volts * (1f - FTuning.BATTERY_CHARGE_EFFICIENCY);
                float gassing = cells * voltsFull * FTuning.BATTERY_GASSING_FACTOR;
                if (volts > gassing)
                    ventAmps = amps * Math.min(1f, volts / gassing - 1f);
            }
            if (ventAmps > 0f) {
                float vented = Math.min(water, ventAmps * FTuning.BATTERY_VENT_WATER_PER_AMP_TICK);
                water -= vented;
                gas += ventAmps * FTuning.BATTERY_VENT_GAS_PER_AMP_TICK;
                venting = true;
            }
            dirty = true;
        }
        float excess = ElectricRules.overVoltage(volts, caseBand);
        if (excess > 0f && !supplying && amps > 0f) {
            condition = Math.max(FTuning.BATTERY_CONDITION_FLOOR, condition - FTuning.BATTERY_OVERVOLT_WEAR * excess);
            dirty = true;
        }
        if (heat > 0f)
            Heat.warm(this, heat);
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, ambient, ambient, false);
        if (temperature > FTuning.BATTERY_HOT_TU) {
            condition = Math.max(FTuning.BATTERY_CONDITION_FLOOR,
                    condition - (temperature - FTuning.BATTERY_HOT_TU) * FTuning.BATTERY_HEAT_WEAR_PER_TU);
            dirty = true;
        }
        if (dirty)
            setChanged();
    }

    // --- server tick: role flip, lazy catch-up, gas ---------------------------------------------

    public void tickServer() {
        if (level == null)
            return;
        long now = level.getGameTime();
        if (lastTime < 0)
            lastTime = now;
        if (now - lastTime >= 200) {
            if (charge > 0f)
                charge *= (float) Math.pow(1.0 - FTuning.BATTERY_SELF_DISCHARGE_PER_DAY, (now - lastTime) / 24000.0);
            lastTime = now;
            setChanged();
        }
        float held = terminalVolts();
        boolean flip = supplying
                ? charge <= 0f || !ready() || isBlocked()
                : ready() && charge > 0f && getVolts() <= held + FTuning.BATTERY_SWITCH_VOLTS;
        if (flip) {
            supplying = !supplying;
            setChanged();
            ElectricPropagator.rebuildFrom(level, worldPosition);
        }
        if (gas > 0f) {
            if (now % 10 == 0)
                ignite();
            if (now % 20 == 0)
                dispose();
        } else
            venting = false;
    }

    /** A flame or lit block beside the vent sets the loose hydrogen off once there is enough of it. */
    private void ignite() {
        if (gas < FTuning.BATTERY_GAS_BLAST_MB || !(level instanceof ServerLevel server))
            return;
        boolean lit = HotIgnition.isSource(getBlockState());
        for (Direction d : Direction.values())
            lit |= HotIgnition.isSource(level.getBlockState(worldPosition.relative(d)));
        if (!lit)
            return;
        float power = Math.min(FTuning.BATTERY_GAS_BLAST_MAX, gas * FTuning.BATTERY_GAS_BLAST_PER_MB);
        gas = 0f;
        server.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                power, Level.ExplosionInteraction.BLOCK);
    }

    /** Loose hydrogen goes into a neighbouring gas holder if one takes it, else drifts off; a weep shows while it hangs. */
    private void dispose() {
        for (Direction d : Direction.values()) {
            if (gas < 1f)
                break;
            IFluidHandler holder = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (holder == null)
                continue;
            int took = holder.fill(new FluidStack(FFluids.HYDROGEN.get(), (int) gas), IFluidHandler.FluidAction.EXECUTE);
            gas -= took;
        }
        gas = Math.max(0f, gas * (1f - FTuning.BATTERY_GAS_LEAK_PER_SECOND));
        if (gas < 0.5f)
            gas = 0f;
        if (gas >= FTuning.BATTERY_GAS_WEEP_MB && level instanceof ServerLevel server)
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    2, 0.15, 0.05, 0.15, 0.01);
        setChanged();
    }

    // --- filling: the acid, then water -----------------------------------------------------------

    /** The fluid face: acid fills to the cells' need, water refills the tank; nothing drains. */
    public final IFluidHandler fluids = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? new FluidStack(FFluids.SULFURIC_ACID.get(), (int) acid)
                    : new FluidStack(net.minecraft.world.level.material.Fluids.WATER, (int) water);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? cells * FTuning.BATTERY_ACID_MB_PER_CELL : FTuning.BATTERY_WATER_TANK_MB;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? stack.is(ACID) : stack.is(FluidTags.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            boolean isAcid = resource.is(ACID);
            if (!isAcid && !resource.is(FluidTags.WATER))
                return 0;
            float have = isAcid ? acid : water;
            int cap = getTankCapacity(isAcid ? 0 : 1);
            int take = Math.min(resource.getAmount(), Math.max(0, cap - (int) Math.ceil(have)));
            if (take > 0 && action.execute()) {
                if (isAcid)
                    acid += take;
                else
                    water += take;
                setChanged();
            }
            return take;
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

    // --- Wearing, ThermalBody -------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public float maxCondition() {
        return maxCondition;
    }

    @Override
    public void weld(float newCondition, float newMax) {
        condition = newCondition;
        maxCondition = newMax;
        setChanged();
    }

    @Override
    public boolean isRunning() {
        return supplying && getAmps() > 0f;
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
        return new ThermalMass(FTuning.BATTERY_MASS_PER_CELL * cells);
    }

    @Override
    public Conductance getLeak() {
        return new Conductance(FTuning.BATTERY_LEAK_PER_CELL * cells);
    }

    // --- persistence ----------------------------------------------------------------------------

    /** The design the battery was built from, kept so the broken block drops the same item. */
    private ModularDesign design;
    private ModularFigures figures;

    /** The physical state, as the item carries it ({@code FDataComponents.BATTERY}) and the block saves it. */
    private CompoundTag writeState(CompoundTag tag) {
        tag.putInt("Cells", cells);
        tag.putFloat("Capacity", capacity);
        tag.putFloat("VoltsFull", voltsFull);
        tag.putFloat("VoltsLow", voltsLow);
        tag.putFloat("VoltsEmpty", voltsEmpty);
        tag.putFloat("ColdFactor", coldFactor);
        tag.putString("CaseBand", caseBand.getSerializedName());
        tag.putFloat("Charge", charge);
        tag.putFloat("Condition", condition);
        tag.putFloat("MaxCondition", maxCondition);
        tag.putFloat("Water", water);
        tag.putFloat("Acid", acid);
        tag.putFloat("Gas", gas);
        tag.putFloat("Temperature", temperature);
        tag.putBoolean("Supplying", supplying);
        tag.putLong("LastTime", lastTime);
        return tag;
    }

    private void readState(CompoundTag tag) {
        cells = Math.max(1, tag.getInt("Cells"));
        capacity = tag.contains("Capacity") ? tag.getFloat("Capacity") : capacity;
        voltsFull = tag.contains("VoltsFull") ? tag.getFloat("VoltsFull") : voltsFull;
        voltsLow = tag.contains("VoltsLow") ? tag.getFloat("VoltsLow") : voltsLow;
        voltsEmpty = tag.contains("VoltsEmpty") ? tag.getFloat("VoltsEmpty") : voltsEmpty;
        coldFactor = tag.contains("ColdFactor") ? tag.getFloat("ColdFactor") : coldFactor;
        caseBand = VoltageBand.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(tag.getString("CaseBand"))).result().orElse(VoltageBand.LV);
        charge = tag.getFloat("Charge");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        maxCondition = tag.contains("MaxCondition") ? tag.getFloat("MaxCondition") : 1f;
        water = tag.getFloat("Water");
        acid = tag.getFloat("Acid");
        gas = tag.getFloat("Gas");
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        supplying = tag.getBoolean("Supplying");
        lastTime = tag.contains("LastTime") ? tag.getLong("LastTime") : -1;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag state = input.get(FDataComponents.BATTERY.get());
        if (state != null)
            readState(state);
        design = input.get(FModular.DESIGN.get());
        figures = input.get(FModular.FIGURES.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(FDataComponents.BATTERY.get(), writeState(new CompoundTag()));
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
