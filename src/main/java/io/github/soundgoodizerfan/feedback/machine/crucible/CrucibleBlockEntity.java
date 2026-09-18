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
package io.github.soundgoodizerfan.feedback.machine.crucible;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.sensor.TemperatureSensorFitting;
import io.github.soundgoodizerfan.feedback.machine.damper.DamperBlockEntity;
import io.github.soundgoodizerfan.feedback.process.AlloyMix;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A vessel that was designed to be measured.
 *
 * <h2>Why it exists, and why it is worse</h2>
 * The slice's best thermal vessel is the sealed blast furnace, and everything good about it
 * follows from being closed. A carried thermometer can still be dipped through the door for one
 * manual reading -- see {@code ThermometerItem#onItemUseFirst} -- so a sealed vessel was never
 * truly unmeasurable. What it refuses is being <em>watched</em>: no ambient reading, no HUD card,
 * no sensor fitting ever auto-attaching itself to it ({@link ThermalBody#hasThermowell()}). That is
 * exactly what a controller needs and a manual spot check cannot give it, so the player's best
 * vessel and their first automated loop are still mutually exclusive, and no amount of iron fixes
 * it. The way out is not a better furnace; it is a vessel with a hole in it.
 * <p>
 * So the crucible is <b>not an upgrade</b>. It is slower and clumsier than the thing it does not
 * replace, and it is the only one of the two that can ever be part of a loop. What the player
 * buys with it is the option to walk away, which is philosophy 7's possible/reliable/economical
 * distinction arriving as something they feel rather than read.
 *
 * <h2>It knows nothing about steel</h2>
 * The crucible holds items at whatever temperature its fire and its own mass produce. What
 * happens to them is a property of the materials, looked up in {@link ThermalProcessTable}, and
 * the crucible never asks what it can make -- it has no answer. Leave iron in past its window and
 * the vessel does not stop, so the iron burns: the same machine-does-not-know-when-to-stop as the
 * hammer, in the second energy type.
 *
 * <h2>Mass is the whole of the upgrade</h2>
 * The two sizes differ by one number, {@link #getThermalMass()}, and everything the player
 * notices follows from it -- how fast it climbs, how far a twenty-tick-old reading has drifted,
 * whether it can satisfy a process's heating-rate limit at all. Insulation is a second number
 * arrived at by stacking blocks against it. Neither is "+50% anything"; both are things you could
 * point at.
 */
public class CrucibleBlockEntity extends BlockEntity implements ThermalBody, MoltenVessel, Fittable {

    /** How often the vessel looks around to see what has been built against it. */
    private static final int INSULATION_RECHECK_INTERVAL = 20;

    /** One slot per face -- a sensor or an adapter, never both, the same one-per-side rule a
     * cover uses. Indexed by {@link Direction#get3DDataValue()}. */
    private final SidedFitting[] fittings = new SidedFitting[Direction.values().length];
    /** Empty for now: no concrete {@link UpgradeFitting} exists yet (see {@code fitting/}). */
    private final List<UpgradeFitting> upgrades = new ArrayList<>();

    private final NonNullList<ItemStack> contents =
            NonNullList.withSize(FTuning.CRUCIBLE_SLOTS, ItemStack.EMPTY);

    private final FluidTank tank = new FluidTank(FTuning.VESSEL_TANK_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            sync();
        }
    };

    /**
     * The tank above is a <em>view</em> onto this, not the source of truth. Melting a second
     * metal into an already-occupied tank used to simply fail (a {@link FluidTank} refuses a
     * fluid that does not match what it already holds) -- which was an accidental, unintentional
     * block on alloying rather than a designed one. This ledger tracks every metal actually
     * poured in; {@link AlloyMix#resolve} decides what the tank shows and what a mold can
     * therefore drain. See {@link AlloyMix}'s own doc for why a bad ratio makes the tank go
     * empty rather than refusing the pour outright.
     */
    private final AlloyMix alloy = new AlloyMix();

    /**
     * Stored as a primitive, and wrapped only at the accessor. A {@link Tu} in a field would
     * outlive the method that made it, which is exactly the case the JIT's escape analysis cannot
     * eliminate -- so it would be a real allocation, held for the lifetime of the block entity,
     * buying nothing the accessor's return type does not already buy.
     */
    private float temperature = FTuning.AMBIENT_TU.value();
    /** How fast it moved last tick. A process may care about that as well as how hot it got. */
    private float lastDelta;
    private float currentTpu;

    private int insulation;
    private int dampers;
    private int sinceInsulationCheck;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRUCIBLE.get(), pos, state);
    }

    // --- thermal ----------------------------------------------------------------------------

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
        return getBlockState().getBlock() == FBlocks.LARGE_CRUCIBLE.get()
                ? FTuning.CRUCIBLE_LARGE_MASS
                : FTuning.CRUCIBLE_SMALL_MASS;
    }

    @Override
    public Conductance getLeak() {
        return new Conductance(FTuning.VESSEL_LEAK.workPerTickPerTu()
                * (float) Math.pow(FTuning.INSULATION_LEAK_FACTOR, insulation)
                + dampers * FTuning.DAMPER_LEAK_BONUS.workPerTickPerTu());
    }

    /** How many insulating blocks are packed against it. Shown as a spec, not as a reading. */
    public int getInsulation() {
        return insulation;
    }

    /** How many open Dampers are packed against it. Shown as a spec, not as a reading. */
    public int getOpenDampers() {
        return dampers;
    }

    public TuRate getHeatingRate() {
        return new TuRate(lastDelta);
    }

    public float getCurrentTpu() {
        return currentTpu;
    }

    @Override
    public FluidTank getTank() {
        return tank;
    }

    /** Keeps the alloy ledger in step with what a mold just took out of the tank -- see the
     * ledger field's own doc for why the tank cannot be trusted to update it on its own. */
    @Override
    public void onDrained(FluidStack drained) {
        alloy.remove(drained.getAmount());
    }

    // --- fitting ------------------------------------------------------------------------------

    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return fittings[side.get3DDataValue()];
    }

    @Override
    public void setSidedFitting(Direction side, SidedFitting fitting) {
        fittings[side.get3DDataValue()] = fitting;
        sync();
    }

    /**
     * {@code hasThermowell()} widened, not replaced (see {@code TODO.md} §4d): a crucible is
     * always thermowelled by default, so this always says yes for a sensor today, but the check
     * lives here rather than being silently true so the day a sealed {@code Fittable} holder
     * exists, it inherits the real rule instead of a copy of it.
     */
    @Override
    public boolean canMount(SidedFitting fitting, Direction side) {
        return !(fitting instanceof SensorFitting) || hasThermowell();
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

    // --- contents ---------------------------------------------------------------------------

    public List<ItemStack> getContents() {
        return contents;
    }

    public ItemStack getItem(int slot) {
        return contents.get(slot);
    }

    public boolean insert(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        for (int slot = 0; slot < contents.size(); slot++) {
            if (!contents.get(slot).isEmpty())
                continue;
            ItemStack taken = stack.split(1);
            // A cold item put into a hot vessel is still cold; it is the vessel that will heat it.
            // Clearing any stale stamp here is what stops an ingot that was hot an hour ago
            // arriving with a temperature it no longer has.
            if (level != null)
                ItemHeat.settle(taken, level);
            contents.set(slot, taken);
            sync();
            return true;
        }
        return false;
    }

    /**
     * Take something out, at the temperature the vessel is at.
     *
     * <h3>Where a workpiece picks up its heat</h3>
     * Contents are not stamped while they sit here -- they are at the vessel's temperature by
     * definition, and re-stamping every tick would rewrite a data component sixty times a second
     * to say something already known. The stamp is applied at the moment the item leaves, which
     * is the moment it starts cooling on its own and the moment anybody could ask.
     */
    public ItemStack removeItem(int slot) {
        ItemStack taken = contents.get(slot);
        if (taken.isEmpty())
            return ItemStack.EMPTY;
        contents.set(slot, ItemStack.EMPTY);
        if (level != null)
            ItemHeat.set(taken, new Tu(temperature), level);
        currentTpu = 0f;
        sync();
        return taken;
    }

    public ItemStack removeFirst() {
        for (int slot = contents.size() - 1; slot >= 0; slot--)
            if (!contents.get(slot).isEmpty())
                return removeItem(slot);
        return ItemStack.EMPTY;
    }

    // --- the tick ---------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;

        if (++sinceInsulationCheck >= INSULATION_RECHECK_INTERVAL) {
            sinceInsulationCheck = 0;
            insulation = countInsulation();
            dampers = countOpenDampers();
        }

        Tu fireTu = HeatSource.below(level, worldPosition);
        lastDelta = Heat.tick(this, fireTu, fireTu.value() > FTuning.AMBIENT_TU.value()).tuPerTick();

        advanceProcess();
    }

    private void advanceProcess() {
        Optional<ThermalProcess> maybe = ThermalProcessTable.get().find(contents);
        if (maybe.isEmpty()) {
            currentTpu = 0f;
            return;
        }
        ThermalProcess process = maybe.get();
        Tu tu = new Tu(temperature);

        // Overrun. The vessel did not stop, so the batch kept getting hotter, so it is ruined --
        // and it is ruined the instant it passes the line rather than after a grace period,
        // because a grace period would be the machine noticing.
        if (process.spoilsAt(tu)) {
            spoil(process);
            return;
        }

        // Tempering's shape: in-band is not enough, the body has to be cooling to count. A flat
        // or rising tick pauses the hold rather than advancing or decaying it -- see
        // ThermalProcess's class doc.
        if (process.requireCooling() && lastDelta >= 0)
            return;

        if (process.requiredTpu() <= 0f) {
            // A melt: min_temperature is the whole requirement, in band completes it outright.
            if (process.inBand(tu))
                complete(process);
            return;
        }

        // Outside the band, or changing too fast to trust in either direction (the one failure
        // with no visible cause, and therefore the one an instrument genuinely fixes -- also what
        // makes a bigger vessel necessary rather than merely nicer, since a small crucible on a
        // full fire cannot satisfy a 25 Tu/t limit at all), TPu decays rather than resetting
        // outright: the safe direction is meant to be genuinely safe (§6), so a player who drifts
        // a little pays for how long they drifted rather than for the whole batch's progress at
        // once. Symmetric with cooling now too -- see ThermalProcess#maxRateTuPerTick -- so
        // annealing's slow-cool requirement is this same check, not a separate one.
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

        complete(process);
    }

    private void complete(ThermalProcess process) {
        int[] assignment = ThermalProcessTable.match(process.inputs(), contents);
        if (assignment == null)
            return;
        for (int slot : assignment)
            contents.get(slot).shrink(1);
        cleanEmpties();

        // A melt: the input is gone, and what it was carries into the alloy ledger rather than a
        // slot. Overflow past the tank's capacity is silently lost, the same as an item result
        // dropped in the world when every slot is full -- see #place. Worth a second look once
        // anything can actually overflow this by more than a sliver.
        if (process.hasFluidResult()) {
            FluidStack result = process.resultFluid();
            alloy.add(result.getFluid(), result.getAmount());
            tank.setFluid(alloy.resolve());
        } else {
            ItemStack result = process.result().copy();
            if (level != null)
                ItemHeat.set(result, new Tu(temperature), level);
            place(result);
        }

        currentTpu = 0f;
        sync();
    }

    private void spoil(ThermalProcess process) {
        int[] assignment = ThermalProcessTable.match(process.inputs(), contents);
        if (assignment == null)
            return;
        for (int slot : assignment)
            contents.get(slot).shrink(1);
        cleanEmpties();

        ItemStack ruined = process.spoiled().copy();
        if (!ruined.isEmpty()) {
            if (level != null)
                ItemHeat.set(ruined, new Tu(temperature), level);
            place(ruined);
        }

        currentTpu = 0f;
        sync();
    }

    /** Put a stack into the first free slot, or drop it if there is somehow none. */
    private void place(ItemStack stack) {
        for (int slot = 0; slot < contents.size(); slot++) {
            if (contents.get(slot).isEmpty()) {
                contents.set(slot, stack);
                return;
            }
        }
        if (level != null)
            net.minecraft.world.Containers.dropItemStack(level,
                    worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
    }

    private void cleanEmpties() {
        for (int slot = 0; slot < contents.size(); slot++)
            if (contents.get(slot).isEmpty())
                contents.set(slot, ItemStack.EMPTY);
    }

    private int countInsulation() {
        if (level == null)
            return 0;
        int found = 0;
        for (Direction face : Direction.values())
            if (level.getBlockState(worldPosition.relative(face)).is(FBlocks.INSULATION.get()))
                found++;
        return Math.min(FTuning.INSULATION_MAX_BLOCKS, found);
    }

    private int countOpenDampers() {
        if (level == null)
            return 0;
        int found = 0;
        for (Direction face : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(face)) instanceof DamperBlockEntity damper
                    && damper.isEngaged())
                found++;
        return Math.min(FTuning.DAMPER_MAX_BLOCKS, found);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, contents, true, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("CurrentTpu", currentTpu);
        tag.putInt("Insulation", insulation);
        tag.putInt("Dampers", dampers);
        alloy.writeNbt(tag);

        // Only one concrete SidedFitting exists yet, so this stays a direct check rather than a
        // registry lookup -- see fitting/UpgradeFitting's own doc on why generalizing early is
        // guessing at a shape instead of finding one.
        for (Direction side : Direction.values()) {
            if (fittings[side.get3DDataValue()] instanceof TemperatureSensorFitting sensor) {
                CompoundTag sensorTag = new CompoundTag();
                sensor.writeNbt(sensorTag);
                tag.put("Sensor" + side.get3DDataValue(), sensorTag);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        ContainerHelper.loadAllItems(tag, contents, registries);
        temperature = tag.contains("Temperature")
                ? tag.getFloat("Temperature")
                : FTuning.AMBIENT_TU.value();
        currentTpu = tag.getFloat("CurrentTpu");
        insulation = tag.getInt("Insulation");
        dampers = tag.getInt("Dampers");
        alloy.readNbt(tag);
        tank.setFluid(alloy.resolve());

        for (Direction side : Direction.values()) {
            String key = "Sensor" + side.get3DDataValue();
            if (tag.contains(key)) {
                TemperatureSensorFitting sensor = new TemperatureSensorFitting(this, this, side);
                sensor.readNbt(tag.getCompound(key));
                fittings[side.get3DDataValue()] = sensor;
            }
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
