// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.fitting.RadiatorFitting;
import io.github.soundgoodizerfan.feedback.fitting.StarterFitting;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The design a placed Engine was built to. It comes off the item when placed and goes back on it
 * when broken (the loot table copies the component), so an engine is never re-designed by moving.
 *
 * <h2>It drives the rotation network</h2>
 * A source like the Steam Engine, with one difference: its Su capacity is not a constant. It is
 * the design's torque curve read at the engine's own current speed ({@code docs/specs/feedback_vehicles_spec.md}
 * section 2) -- weak from rest, strongest at the peak RPM -- so a run settles wherever the curve,
 * the load and the friction meet, and a load past the curve stalls it. The network only re-reads
 * capacity when told to, so the tick tells it whenever the curve has moved by {@link #RECALC_SU}.
 * <p>
 * Target speed is the design's peak-torque RPM scaled by the throttle. It burns fuel from its tank
 * (spec 9.1): power scales with the fuel's flame temperature, and the compression ratio decides
 * whether the fuel's ignition point suits it (see {@link Compression}).
 * <p>
 * <b>Heat.</b> The engine is a {@link ThermalBody}: a running engine warms its own body ({@link VehicleTuning#ENGINE_HEAT_WORK_PER_MB}
 * of Work a tick per mB of displacement, scaled by opening and the fuel) and sheds heat through the air like any body, plus through a
 * fitted {@link RadiatorFitting} (more with coolant in its tank). Above {@link VehicleTuning#ENGINE_COMFORT_TU} power falls, to a floor;
 * above its metal's service temperature the body takes misuse wear into {@link #condition}, with a warning smoke; near the metal's melting
 * point it seizes and will not run until it has cooled. It steps only while it is warm or running; a cold idle engine is skipped.
 * <p>
 * <b>Off when placed.</b> With no fuel there is nothing else to make starting it an act, and a
 * source that ran from placement would spin whatever it was set against before the build was
 * finished. Use toggles it; a controller does the same through {@link Switchable}. Off means no
 * capacity at all, and the run coasts down under its own load like any cut-off network. Its
 * throttle ({@link Analog}) takes a number from a controller wherever the engine sits, on a
 * stand as in a car (philosophy 14).
 *
 * <h2>Its weight rides on a box</h2>
 * Sable weighs a block by its state alone, so every Engine block would weigh the same. The block is held light
 * ({@code physics_block_properties/engine.json}) and the rest of its design's mass, the figure the Blueprint Table prints, rides on a
 * box ({@link io.github.soundgoodizerfan.feedback.vehicle.CarriedMass}).
 */
public class EngineBlockEntity extends RotationNode implements Switchable, Analog, DataNode, io.github.soundgoodizerfan.feedback.vehicle.VehiclePart,
        io.github.soundgoodizerfan.feedback.fitting.StoredFittable, io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody {

    private final io.github.soundgoodizerfan.feedback.fitting.FittingStore fittings = new io.github.soundgoodizerfan.feedback.fitting.FittingStore();

    @Override
    public io.github.soundgoodizerfan.feedback.fitting.FittingStore fittings() {
        return fittings;
    }

    /** Only a starter and a radiator fit an engine, one to a face. */
    @Override
    public boolean canMount(io.github.soundgoodizerfan.feedback.fitting.SidedFitting fitting, Direction side) {
        return fitting instanceof StarterFitting || fitting instanceof RadiatorFitting;
    }

    /** Heat of the body in Tu; stepped by {@link #heat}. */
    private float temperature = ThermalTuning.AMBIENT_TU.value();
    /** Set when the body got too hot; the engine will not run until it has cooled below its metal's service temperature. */
    private boolean seized;
    private final Catchup catchup = new Catchup();
    /** The room's temperature seen on the last heat step, and whether the first step after a load has run (it settles what a closed chunk cooled). */
    private float ambientTu = ThermalTuning.AMBIENT_TU.value();
    private boolean heatLoaded;
    /** The design metal's service temperature and melting point, read once and dropped whenever the design is (re)loaded; NaN until read. */
    private float serviceTu = Float.NaN, meltTu = Float.NaN;
    /** Game time of the last flat-battery click, so a held key or a controller does not click every tick. */
    private long lastClickAt = -1000L;
    private static final long CLICK_GAP_TICKS = 20L;
    /** The radiator's coolant: any liquid; it adds thermal mass and carries heat to the fins while a radiator is fitted. */
    private final FluidTank coolant = new FluidTank(VehicleTuning.ENGINE_COOLANT_MB, fluid -> !fluid.getFluidType().isLighterThanAir()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** How far the curve may move, in Su, before the network is told. */
    private static final float RECALC_SU = 1f;

    @Nullable
    private EngineDesign design;
    private final io.github.soundgoodizerfan.feedback.vehicle.CarriedMass carried = new io.github.soundgoodizerfan.feedback.vehicle.CarriedMass();

    private final FuelBlend fuel = new FuelBlend(this::setChanged);
    /** Fraction of as-built power left: knock wears it, to a floor. */
    private float condition = 1f;
    /** The plug's wear, 0 new to 1 spent; running in a vehicle wears it ({@link #wearPlug}). */
    private float plugWear;
    /** Ticks of a stumble left: a worn plug misses and gives no torque. */
    private int missTicks;
    /** The modular build this engine was made from, kept so it can be reworked at the Engineering Table after it is picked up. */
    @Nullable
    private io.github.soundgoodizerfan.feedback.modular.type.ModularDesign build;
    /** What the fuel gives this tick, as a share of a reference fuel; 0 when starved or misfiring. */
    private float power;
    private float fuelOwed;
    private boolean running;
    /**
     * How far open, 0 to 1 -- the vehicle's analog input ({@link Analog}, spec section 5,
     * <em>Controls</em>). Shut unless a controller says otherwise, so an engine with no controller
     * idles instead of running flat out. Torque and the speed it reaches both scale with
     * {@code IDLE + (1 - IDLE) x throttle}: shut, a running engine idles rather than dies.
     */
    private float throttle = 0;
    /** Capacity as the network last read it, so the tick knows when the curve has moved. */
    private float reportedSu;
    /** Required by {@link DataNode}; nothing links to an engine, a card targets it. Not saved. */

    public EngineBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.ENGINE_ENTITY.get(), pos, state);
    }

    public FuelBlend getFuel() {
        return fuel;
    }

    public float condition() {
        return condition;
    }

    /** Closes {@code by} of the engine's wear (an attuned part's mend, {@code MachineAura}). */
    public void mend(float by) {
        condition = Math.min(1f, condition + by);
        setChanged();
    }

    /** The spark plug's condition, 1 new falling with running time in a vehicle. */
    public float plugCondition() {
        return 1f - plugWear;
    }

    @Nullable
    public EngineDesign design() {
        return design;
    }

    @Nullable
    private Material material() {
        return design == null ? null : MaterialTable.get().materials().get(design.material());
    }

    // --- rotation -----------------------------------------------------------------------------

    @Override
    public Rpm getGeneratedRpm() {
        Material m = material();
        return running && power > 0f && m != null ? new Rpm(design.peakRpm(m).value() * opening()) : ZERO_RPM;
    }

    /** The torque curve at this engine's own current speed. */
    @Override
    public Su getCapacitySu() {
        Material m = material();
        return running && power > 0f && missTicks == 0 && m != null ? new Su(design.torqueAt(new Rpm(Math.abs(getRpm().value())), m).value() * opening() * power * condition * heatFactor()) : NO_SU;
    }

    // --- heat ---------------------------------------------------------------------------------

    /** The share of full power left to a body this hot: 1 up to the comfortable band, falling to a floor, never to nothing. */
    private float heatFactor() {
        return Math.max(VehicleTuning.ENGINE_DERATE_FLOOR,
                1f - VehicleTuning.ENGINE_DERATE_PER_TU * Math.max(0f, temperature - VehicleTuning.ENGINE_COMFORT_TU.value()));
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    /** The design's metal: its specific heat times the design's mass, plus the coolant held while a radiator is fitted. */
    @Override
    public ThermalMass getThermalMass() {
        Material m = material();
        float metal = design == null || m == null ? 50f
                : design.mass().value() * m.thermal().specificHeat().map(s -> s.workPerMuPerTu()).orElse(0.5f);
        return new ThermalMass(metal + (hasRadiator() ? coolant.getFluidAmount() * VehicleTuning.COOLANT_WORK_PER_MB_PER_TU : 0f));
    }

    /** The air leak every body has, plus a radiator's fins: the full figure with coolant in the tank, a share of it dry. */
    @Override
    public Conductance getLeak() {
        float radiator = !hasRadiator() ? 0f
                : VehicleTuning.RADIATOR_CONDUCTANCE.workPerTickPerTu() * (coolant.isEmpty() ? VehicleTuning.RADIATOR_COOLANT_SHARE : 1f);
        return new Conductance(VehicleTuning.ENGINE_AIR_LEAK.workPerTickPerTu() + radiator);
    }

    private boolean hasRadiator() {
        for (Direction d : Direction.values())
            if (fittings.get(d) instanceof RadiatorFitting)
                return true;
        return false;
    }

    @Nullable
    private StarterFitting starter() {
        for (Direction d : Direction.values())
            if (fittings.get(d) instanceof StarterFitting s)
                return s;
        return null;
    }

    /** The coolant tank as a fluid handler on a face that carries a radiator, which is what the pipe system reaches; nothing on any other face. */
    @Nullable
    public IFluidHandler coolantHandler(@Nullable Direction side) {
        return side != null && fittings.get(side) instanceof RadiatorFitting ? coolant : null;
    }

    /** Whether the body got too hot and will not run until it has cooled. */
    public boolean isSeized() {
        return seized;
    }

    /**
     * One tick of the engine's heat: catch up what a closed chunk cooled, warm the body from the burn, shed heat to the air and the
     * radiator, then the consequences of how hot it is. A cold, idle engine is skipped.
     */
    private void heat(Level level, BlockPos pos) {
        // An engine that is off, not seized and settled at the room's temperature has nothing to step: no lookups. The ambient seen on the
        // last step is reused, and a step is forced now and then (and on the first tick after a load, which also settles what a closed chunk cooled).
        if (heatLoaded && !running && !seized && Math.abs(temperature - ambientTu) < 0.5f && level.getGameTime() % 200 != 0)
            return;
        Tu ambient = AmbientTable.at(level);
        ambientTu = ambient.value();
        long gap = catchup.take(level);
        heatLoaded = true;
        if (gap > 0)
            Catchup.settle(this, FireSpan.NONE, ambient, gap);
        boolean active = running && power > 0f && design != null;
        if (active)
            Heat.warm(this, VehicleTuning.ENGINE_HEAT_WORK_PER_MB * design.displacementMb() * opening() * power);
        if (active || Math.abs(temperature - ambientTu) > 0.5f)
            Heat.tick(this, ambient, ambient, false);
        if (Float.isNaN(serviceTu)) {
            Material m = material();
            if (m == null)
                return;
            serviceTu = m.thermal().serviceTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE);
            meltTu = m.thermal().meltingPoint().map(t -> t.value()).orElse(Float.MAX_VALUE);
        }
        float service = serviceTu;
        if (seized && temperature < service) {
            seized = false;
            setChanged();
        }
        float seizeAt = meltTu * VehicleTuning.ENGINE_SEIZE_SHARE;
        if (temperature >= seizeAt) {
            // Seizing locks the engine; what it costs is how far past the seize point the body stays and for how long, a tick at a time.
            if (!seized && running)
                seize(level, pos);
            condition = Math.max(VehicleTuning.ENGINE_CONDITION_FLOOR, condition - VehicleTuning.ENGINE_SEIZE_WEAR_PER_TICK
                    * (temperature - seizeAt) / VehicleTuning.ENGINE_HEAT_WEAR_SCALE_TU.value()
                    * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(build));
            setChanged();
        }
        if (temperature > service) {
            condition = Math.max(VehicleTuning.ENGINE_CONDITION_FLOOR, condition - VehicleTuning.ENGINE_HEAT_WEAR_PER_TICK
                    * (temperature - service) / VehicleTuning.ENGINE_HEAT_WEAR_SCALE_TU.value()
                    * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(build));
            setChanged();
        }
        if (temperature > service * VehicleTuning.ENGINE_WARN_SHARE && level.getGameTime() % 10 == 0 && level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.4f);
        }
    }

    /** Too hot to run: it stops and stays stopped until it has cooled below its metal's service temperature. */
    private void seize(Level level, BlockPos pos) {
        seized = true;
        running = false;
        power = 0f;
        setChanged();
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 0.6f);
        RotationPropagator.rebuildFrom(level, pos);
    }

    private float opening() {
        return VehicleTuning.ENGINE_IDLE_FRACTION + (1 - VehicleTuning.ENGINE_IDLE_FRACTION) * throttle;
    }

    @Override
    protected void startSound() {
        io.github.soundgoodizerfan.feedback.client.BlockLoops.engine(this);
    }

    // --- Analog -------------------------------------------------------------------------------

    @Override
    public void setLevel(int channel, float level) {
        float next = Math.clamp(level, 0, 1);
        if (Math.abs(next - throttle) < 1e-3)
            return;
        throttle = next;
        setChanged();
        // The target speed moved with it, not only the curve: recalculate() re-reads capacity but
        // the target is set by a rebuild, so without one throttle changed only the Su.
        if (getLevel() != null && !getLevel().isClientSide)
            RotationPropagator.rebuildFrom(getLevel(), worldPosition);
    }

    @Override
    public String[] channelKeys() {
        return new String[]{"feedback.analog.throttle"};
    }

    // --- Switchable ---------------------------------------------------------------------------

    @Override
    public boolean isEngaged() {
        return running;
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || running == engaged)
            return;
        if (engaged) {
            // A seized engine will not turn; a fitted starter turns it over on the battery it touches, and a flat battery gives no start.
            if (seized)
                return;
            StarterFitting starter = starter();
            if (starter != null) {
                if (!starter.crank()) {
                    // A flat battery clicks, but not on every call: a held switch or a controller asks every tick.
                    if (level.getGameTime() - lastClickAt >= CLICK_GAP_TICKS) {
                        lastClickAt = level.getGameTime();
                        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.COMPARATOR_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 0.6f);
                    }
                    return;
                }
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.PISTON_EXTEND, net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.8f);
            }
        }
        running = engaged;
        setChanged();
        // Becoming a source or ceasing to be one changes who drives the run: rebuild it, as the
        // Steam Engine does when its steam comes and goes.
        RotationPropagator.rebuildFrom(level, worldPosition);
    }

    // --- the tick -----------------------------------------------------------------------------

    static void serverTick(Level level, BlockPos pos, BlockState state, EngineBlockEntity be) {
        be.burn(level, pos);
        be.heat(level, pos);
        be.wearPlug(level);
        be.followTorqueCurve();
        be.carryMass(level, pos);
    }

    /** Draws fuel for this tick, works out what it gives, and wears the engine if it knocks. */
    private void burn(Level level, BlockPos pos) {
        float before = power;
        Material m = material();
        float flame = fuel.isEmpty() ? 0f : fuel.flame();
        if (!running || design == null || m == null || flame <= 0f) {
            power = 0f;
        } else {
            float factor = flame / FTuning.ENGINE_REFERENCE_FLAME_TU;
            fuelOwed += FTuning.ENGINE_FUEL_MB_PER_TICK_PER_L * design.displacementMb() * opening() / factor;
            int drawn = (int) fuelOwed;
            fuelOwed -= drawn;
            if (drawn > 0)
                fuel.burn(drawn);
            float ignition = fuel.ignition();
            boolean misfire = ignition > design.compression().misfireAbove;
            power = misfire ? 0f : factor;
            if (!misfire && ignition < design.compression().knockBelow) {
                condition = Math.max(VehicleTuning.ENGINE_CONDITION_FLOOR, condition - VehicleTuning.ENGINE_KNOCK_WEAR_PER_TICK * opening() * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(build));
                setChanged();
                if (level.getGameTime() % 10 == 0) {
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_HIT,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.6f);
                    // The pistons under knock are a working face under strain: a piezo part charges a jar beside the engine, a sensing one says how hard.
                    io.github.soundgoodizerfan.feedback.modular.attunement.PiezoKinds.strain(level, pos,
                            io.github.soundgoodizerfan.feedback.modular.attunement.MachineAura.levelsOf(this), opening(), 1f);
                }
            }
        }
        // Starting or stopping burning changes the target speed, not only the curve.
        if ((before > 0f) != (power > 0f))
            RotationPropagator.rebuildFrom(level, pos);
    }

    /**
     * Running in a vehicle wears the plug by time; on a stand the engine is a machine and wears only from misuse. A worn plug
     * stumbles: some ticks give no torque ({@code PLUG_MISS_*}), never a stop. Only the torque is cut, so the run is not rebuilt.
     */
    private void wearPlug(Level level) {
        if (missTicks > 0)
            missTicks--;
        if (!running || power <= 0f || design == null
                || dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(this) == null)
            return;
        float before = plugWear;
        plugWear = Math.min(1f, plugWear + 1f / Math.max(1f, design.plugLife()));
        if ((int) (before * 100) != (int) (plugWear * 100))
            setChanged();
        if (missTicks == 0 && level.random.nextFloat() < VehicleTuning.PLUG_MISS_CHANCE * plugWear * plugWear * design.plugFoul())
            missTicks = VehicleTuning.PLUG_MISS_TICKS;
    }

    private void followTorqueCurve() {
        float su = getCapacitySu().value();
        if (Math.abs(su - reportedSu) < RECALC_SU)
            return;
        reportedSu = su;
        if (network != null)
            network.recalculate();
    }

    /** Puts the design's mass on the vehicle this engine is part of, once its box is in Sable. */
    private void carryMass(Level level, BlockPos pos) {
        if (design != null)
            carried.tick(this, design.mass());
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    // --- lifecycle ----------------------------------------------------------------------------

    @Override
    public void setRemoved() {
        super.setRemoved();
        carried.release(this);
    }

    /** The mass this engine puts on its vehicle, block and box, in kpg, for tests; the block alone until the box exists. */
    public double carriedKpg() {
        return carried.kpg();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        design = input.get(FVehicles.ENGINE_DESIGN.get());
        serviceTu = Float.NaN;
        build = input.get(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get());
        plugWear = input.getOrDefault(FVehicles.PLUG_WEAR.get(), 0f);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (design != null)
            components.set(FVehicles.ENGINE_DESIGN.get(), design);
        if (build != null)
            components.set(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get(), build);
        if (plugWear > 0f)
            components.set(FVehicles.PLUG_WEAR.get(), plugWear);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (design != null)
            EngineDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
        if (build != null)
            io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.CODEC.encodeStart(NbtOps.INSTANCE, build).ifSuccess(t -> tag.put("Build", t));
        tag.putFloat("PlugWear", plugWear);
        tag.putBoolean("Running", running);
        fittings.save(tag);
        tag.putFloat("Throttle", throttle);
        tag.putFloat("Condition", condition);
        tag.putFloat("Temperature", temperature);
        tag.putBoolean("Seized", seized);
        tag.put("Coolant", coolant.writeToNBT(registries, new CompoundTag()));
        catchup.save(tag, level);
        CompoundTag fuelTag = new CompoundTag();
        fuel.save(fuelTag);
        tag.put("Fuel", fuelTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("Design") ? EngineDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().orElse(null) : null;
        serviceTu = Float.NaN;
        build = tag.contains("Build") ? io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Build")).result().orElse(null) : null;
        plugWear = tag.getFloat("PlugWear");
        running = tag.getBoolean("Running");
        fittings.load(tag, this);
        throttle = tag.contains("Throttle") ? tag.getFloat("Throttle") : 0;
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : ThermalTuning.AMBIENT_TU.value();
        seized = tag.getBoolean("Seized");
        if (tag.contains("Coolant"))
            coolant.readFromNBT(registries, tag.getCompound("Coolant"));
        catchup.load(tag);
        if (tag.contains("Fuel"))
            fuel.load(tag.getCompound("Fuel"));
    }
}
