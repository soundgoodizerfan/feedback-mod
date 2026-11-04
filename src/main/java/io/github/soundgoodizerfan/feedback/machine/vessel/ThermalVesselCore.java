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
package io.github.soundgoodizerfan.feedback.machine.vessel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.sensor.TemperatureSensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.RefractoryLiningFitting;
import io.github.soundgoodizerfan.feedback.process.AlloyMix;
import io.github.soundgoodizerfan.feedback.process.Failure;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.process.PhaseChange;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The thermal vessel: everything that carries heat and holds things, once. A furnace, a kiln, a crucible and anything built like them
 * are this class with their own stats and their own way of being heated; none of them is "the one that melts metal". A vessel melts
 * metal because it carries heat and has a tank, so every vessel does, however well.
 *
 * <p>Here: temperature and its rate of change, the held items' thermal process (the band, the hold, the melt, the ruin, an atmosphere),
 * the fluid tank over an {@link AlloyMix} ledger (what a melt pours into, so metals alloy in any vessel), the boil-off of anything liquid
 * the phase rows boil, sensors and upgrades, and their persistence. A subclass says which stacks are the process's
 * ({@link #processSlots}), whether the air is shut out ({@link #airlessRun}), what its vapour does ({@link #vent}), and how it is
 * heated (its own tick, calling {@link #runProcess} and {@link #boilOff}).
 *
 * <p>The tank is a view of the ledger only while a melt is in it. A vessel's tank may hold a foreign fluid too (a crude, a gas for an
 * atmosphere); a melt then stays solid rather than mixing into it.
 */
public abstract class ThermalVesselCore extends BlockEntity implements ThermalBody, MoltenVessel, Fittable {

    /** One slot per face, a sensor or an adapter, never both. Indexed by {@link Direction#get3DDataValue()}. */
    protected final SidedFitting[] fittings = new SidedFitting[Direction.values().length];
    /** At most one of each kind; the only concrete one so far is a {@link RefractoryLiningFitting}. */
    protected final List<UpgradeFitting> upgrades = new ArrayList<>();

    protected final FluidTank tank = new FluidTank(FTuning.VESSEL_TANK_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            sync();
        }
    };
    /** Every fluid poured in as a melt, by amount; {@link AlloyMix#resolve} is what the tank shows and a mold can drain. */
    protected final AlloyMix alloy = new AlloyMix();
    private boolean ledgerOwned;

    /** Held as a primitive and wrapped at the accessor: a {@link Tu} field would outlive its method and could not be scalar-replaced. */
    protected float temperature = FTuning.AMBIENT_TU.value();
    /** How fast it moved last tick. A process may care about that as well as how hot it got. */
    protected float lastDelta;
    private float temperatureLastTick = Float.NaN;
    protected float currentTpu;
    /** What {@link #currentTpu} is progress towards; a different match starts from zero. Null after a load. */
    @Nullable
    protected ThermalProcess currentProcess;
    private float boilOwed;

    protected ThermalVesselCore(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- what a subclass says -------------------------------------------------------------------

    /** The stacks the thermal process works on, in order; results are placed back into them. */
    protected abstract List<ItemStack> processSlots();

    /** Whether the process is held in a sealed chamber with no air blowing in (charcoal, sintering). */
    protected boolean airlessRun() {
        return false;
    }

    /** Where {@code mB} of {@code change}'s vapour goes when a liquid boils: out of the open top by default. */
    protected void vent(PhaseChange change, int mB) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 1, 0.15, 0.05, 0.15, 0.02);
            if (server.getGameTime() % 10 == 0)
                server.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.2f, 1.6f);
        }
    }

    // --- thermal ----------------------------------------------------------------------------------

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    public float getCurrentTpu() {
        return currentTpu;
    }

    /**
     * The rate is the temperature's actual change since last tick, not {@code Heat.tick}'s own figure: a heat exchanger moves heat in
     * its own tick, and that cooling has to count toward the rate limit and toward a cooling-only process like any other.
     */
    protected void measureDelta() {
        lastDelta = Float.isNaN(temperatureLastTick) ? 0f : temperature - temperatureLastTick;
        temperatureLastTick = temperature;
    }

    // --- the tank -------------------------------------------------------------------------------

    @Override
    public FluidTank getTank() {
        return tank;
    }

    /** Keeps the ledger in step with what a mold just took out of the tank. */
    @Override
    public void onDrained(FluidStack drained) {
        alloy.remove(drained.getAmount());
        refreshTank();
    }

    /** Millibuckets the ledger can still take. */
    public int freeSpace() {
        return tank.getCapacity() - alloy.amount();
    }

    /** Whether a melt can pour in: the tank is empty or already the ledger's, not a foreign fluid. */
    protected boolean takesMelt() {
        return alloy.amount() > 0 || tank.isEmpty();
    }

    @Override
    public int addMolten(FluidStack stack) {
        if (!takesMelt())
            return 0;
        int amount = Math.min(stack.getAmount(), tank.getCapacity() - alloy.amount());
        alloy.add(stack.getFluid(), amount);
        refreshTank();
        return amount;
    }

    /** The tank shows what the ledger resolves to; a vessel whose ledger is empty leaves a foreign fluid in the tank alone. */
    protected void refreshTank() {
        if (alloy.amount() > 0) {
            tank.setFluid(alloy.resolve());
            ledgerOwned = true;
        } else if (ledgerOwned) {
            tank.setFluid(FluidStack.EMPTY);
            ledgerOwned = false;
        }
    }

    /** Whether the tank's contents are the ledger's. */
    protected boolean ledgerHeld() {
        return alloy.amount() > 0;
    }

    // --- the process ----------------------------------------------------------------------------

    /** Finds and advances the thermal process the held stacks satisfy; the one found, if any. */
    protected final Optional<ThermalProcess> runProcess() {
        List<ItemStack> contents = processSlots();
        Optional<ThermalProcess> maybe = ThermalProcessTable.get().find(contents, airlessRun());
        if (maybe.isEmpty()) {
            currentTpu = 0f;
            currentProcess = null;
            return maybe;
        }
        ThermalProcess process = maybe.get();
        if (currentProcess != null && !currentProcess.equals(process))
            currentTpu = 0f;
        currentProcess = process;
        advance(process, contents);
        return maybe;
    }

    private void advance(ThermalProcess process, List<ItemStack> contents) {
        Tu tu = new Tu(temperature);

        // Overrun. The vessel did not stop, so the batch kept getting hotter, so it is ruined the instant it passes the line.
        if (process.spoilsAt(tu)) {
            spoil(process, contents);
            return;
        }

        // Tempering's shape: in-band is not enough, the body has to be cooling to count. A flat or rising tick pauses the hold.
        if (process.requireCooling() && lastDelta >= 0)
            return;

        if (process.requiredTpu() <= 0f) {
            // A melt: min_temperature is the whole requirement, in band completes it outright.
            if (process.inBand(tu))
                complete(process, contents);
            return;
        }

        // Outside the band, or changing too fast to trust in either direction, TPu decays rather than resetting outright.
        float suitability = process.suitability(tu);
        if (Math.abs(lastDelta) > process.maxRateTuPerTick().tuPerTick())
            suitability = 0f;
        if (suitability <= 0f) {
            currentTpu = Math.max(0f, currentTpu - FTuning.TPU_DECAY_PER_TICK);
            return;
        }

        currentTpu += suitability;
        if (currentTpu < process.requiredTpu())
            return;

        complete(process, contents);
    }

    private void complete(ThermalProcess process, List<ItemStack> contents) {
        int[] assignment = ThermalProcessTable.match(process.inputs(), contents);
        if (assignment == null)
            return;
        // A melt into a tank holding a foreign fluid has nowhere to go: it stays solid rather than being destroyed.
        if (process.hasFluidResult() && !takesMelt())
            return;
        ItemStack first = assignment.length > 0 ? contents.get(assignment[0]).copy() : ItemStack.EMPTY;
        for (int slot : assignment)
            contents.get(slot).shrink(1);
        cleanEmpties(contents);

        // A row that wants a gas spends it when it is there; without it the batch comes out as the row's ruin.
        boolean gassed = process.atmosphere().map(a -> {
            FluidStack held = tank.getFluid();
            boolean there = !held.isEmpty() && BuiltInRegistries.FLUID.getKey(held.getFluid()).equals(a.fluid())
                    && held.getAmount() >= a.mb();
            if (there)
                tank.drain(a.mb(), IFluidHandler.FluidAction.EXECUTE);
            return there;
        }).orElse(true);

        if (!gassed)
            placeHot(Failure.ruined(process, first));
        else if (process.hasFluidResult()) {
            // Whatever will not fit is lost: a full vessel melting more metal spills it. A full vessel stuck on a bad ratio
            // spills its own mix in proportion to make room, so the metal that corrects the ratio always gets in.
            FluidStack result = process.resultFluid();
            int overflow = alloy.amount() + result.getAmount() - tank.getCapacity();
            if (overflow > 0 && alloy.resolve().isEmpty())
                alloy.remove(Math.min(overflow, alloy.amount()));
            alloy.add(result.getFluid(), Math.min(result.getAmount(), tank.getCapacity() - alloy.amount()));
            refreshTank();
        } else
            placeHot(process.result().copy());

        currentTpu = 0f;
        sync();
    }

    private void spoil(ThermalProcess process, List<ItemStack> contents) {
        int[] assignment = ThermalProcessTable.match(process.inputs(), contents);
        if (assignment == null)
            return;
        ItemStack first = assignment.length > 0 ? contents.get(assignment[0]).copy() : ItemStack.EMPTY;
        for (int slot : assignment)
            contents.get(slot).shrink(1);
        cleanEmpties(contents);
        placeHot(Failure.ruined(process, first));
        currentTpu = 0f;
        sync();
    }

    private void placeHot(ItemStack stack) {
        if (stack.isEmpty())
            return;
        if (level != null)
            ItemHeat.set(stack, new Tu(temperature), level);
        place(stack);
    }

    /** Into the first free or stackable slot, or dropped if somehow none. */
    protected void place(ItemStack stack) {
        List<ItemStack> contents = processSlots();
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack existing = contents.get(slot);
            if (existing.isEmpty()) {
                contents.set(slot, stack);
                return;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                existing.grow(stack.getCount());
                return;
            }
        }
        if (level != null)
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
    }

    private static void cleanEmpties(List<ItemStack> contents) {
        for (int slot = 0; slot < contents.size(); slot++)
            if (contents.get(slot).isEmpty())
                contents.set(slot, ItemStack.EMPTY);
    }

    /**
     * Anything liquid in the ledger that the phase rows boil, held at or past its boiling point, boils: a share of it a tick leaves as
     * vapour ({@link #vent}) and each mB takes its latent heat out of the vessel. Names no fluid; a melt boils nowhere near here.
     */
    protected final void boilOff() {
        if (alloy.amount() <= 0)
            return;
        for (var e : new ArrayList<>(alloy.content().entrySet())) {
            var change = ThermalProcessTable.get().boiling(new FluidStack(e.getKey(), e.getValue()));
            if (change.isEmpty() || change.get().lightShare().isPresent() || temperature < change.get().boil().value())
                continue;
            float want = e.getValue() * FTuning.VESSEL_BOIL_PER_TICK + boilOwed;
            int amount = alloy.take(e.getKey(), Math.max(1, (int) want));
            boilOwed = want - (int) want;
            if (amount <= 0)
                continue;
            temperature -= amount * change.get().latent() / FTuning.VESSEL_BOIL_MASS_WORK_PER_TU;
            refreshTank();
            vent(change.get(), amount);
            sync();
            break;
        }
    }

    // --- fitting --------------------------------------------------------------------------------

    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return fittings[side.get3DDataValue()];
    }

    @Override
    public void setSidedFitting(Direction side, SidedFitting fitting) {
        fittings[side.get3DDataValue()] = fitting;
        sync();
    }

    /** A sensor needs a thermowell; whether this vessel has one is {@link ThermalBody#hasThermowell()}. */
    @Override
    public boolean canMount(SidedFitting fitting, Direction side) {
        return !(fitting instanceof SensorFitting) || hasThermowell();
    }

    /** A vessel folds an upgrade's added mass into its thermal mass, one of each kind. */
    @Override
    public boolean canAdd(UpgradeFitting upgrade) {
        return upgrades.stream().noneMatch(held -> held.getClass() == upgrade.getClass());
    }

    @Override
    public List<UpgradeFitting> getUpgrades() {
        return upgrades;
    }

    @Override
    public boolean addUpgrade(UpgradeFitting upgrade) {
        boolean added = upgrades.add(upgrade);
        if (added)
            sync();
        return added;
    }

    @Override
    public void removeUpgrade(UpgradeFitting upgrade) {
        if (upgrades.remove(upgrade))
            sync();
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- persistence ----------------------------------------------------------------------------

    /** Temperature, process progress, the ledger, the lining and the sensors: what every vessel keeps. */
    protected void saveCore(CompoundTag tag) {
        tag.putFloat("Temperature", temperature);
        tag.putFloat("CurrentTpu", currentTpu);
        alloy.writeNbt(tag);
        // One concrete UpgradeFitting, stateless: a flag is its whole persistence.
        tag.putBoolean("Lining", upgrades.stream().anyMatch(RefractoryLiningFitting.class::isInstance));
        // Only one concrete SidedFitting exists yet, so this stays a direct check.
        for (Direction side : Direction.values())
            if (fittings[side.get3DDataValue()] instanceof TemperatureSensorFitting sensor) {
                CompoundTag sensorTag = new CompoundTag();
                sensor.writeNbt(sensorTag);
                tag.put("Sensor" + side.get3DDataValue(), sensorTag);
            }
    }

    protected void loadCore(CompoundTag tag) {
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        currentTpu = tag.getFloat("CurrentTpu");
        alloy.readNbt(tag);
        ledgerOwned = false;
        refreshTank();
        upgrades.clear();
        if (tag.getBoolean("Lining"))
            upgrades.add(new RefractoryLiningFitting());
        for (Direction side : Direction.values()) {
            String key = "Sensor" + side.get3DDataValue();
            int i = side.get3DDataValue();
            if (!tag.contains(key)) {
                fittings[i] = null;   // removed since the last sync
                continue;
            }
            // Reuse the fitting already there, so a sync does not replace it with a fresh object.
            TemperatureSensorFitting sensor = fittings[i] instanceof TemperatureSensorFitting existing
                    ? existing : new TemperatureSensorFitting(this, this, side);
            sensor.readNbt(tag.getCompound(key));
            fittings[i] = sensor;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
