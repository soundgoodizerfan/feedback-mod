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

import io.github.soundgoodizerfan.feedback.registry.FTags;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
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
import io.github.soundgoodizerfan.feedback.fitting.upgrade.RefractoryLiningFitting;
import io.github.soundgoodizerfan.feedback.machine.damper.DamperBlockEntity;
import io.github.soundgoodizerfan.feedback.process.AlloyMix;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

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
public class CrucibleBlockEntity extends io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselCore implements net.minecraft.world.MenuProvider {

    /** How often the vessel looks around to see what has been built against it. */
    private static final int INSULATION_RECHECK_INTERVAL = 20;

    private final NonNullList<ItemStack> contents =
            NonNullList.withSize(FTuning.CRUCIBLE_SLOTS, ItemStack.EMPTY);

    private int insulation;
    private int dampers;
    private int sinceInsulationCheck;

    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRUCIBLE.get(), pos, state);
    }

    @Override
    protected java.util.List<ItemStack> processSlots() {
        return contents;
    }

    // --- thermal ----------------------------------------------------------------------------

    @Override
    public ThermalMass getThermalMass() {
        ThermalMass base = getBlockState().getBlock() instanceof CrucibleBlock crucible
                ? crucible.getThermalMass()
                : FTuning.CRUCIBLE_SMALL_MASS;
        if (upgrades.isEmpty())
            return base;
        float mass = base.workPerTu();
        for (UpgradeFitting upgrade : upgrades)
            mass += upgrade.addedThermalMass().workPerTu();
        return new ThermalMass(mass);
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
        sync();
        return taken;
    }

    /** Take up to {@code count} from a slot, at the vessel's heat. */
    public ItemStack takeFrom(int slot, int count) {
        ItemStack held = contents.get(slot);
        if (held.isEmpty())
            return ItemStack.EMPTY;
        if (count >= held.getCount())
            return removeItem(slot);
        ItemStack taken = held.split(count);
        if (level != null)
            ItemHeat.set(taken, new Tu(temperature), level);
        sync();
        return taken;
    }

    /** Put a stack into a slot by hand; a cold item arrives cold, as {@link #insert} has it. */
    public void putItem(int slot, ItemStack stack) {
        if (level != null)
            ItemHeat.settle(stack, level);
        contents.set(slot, stack);
        sync();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                                          net.minecraft.world.entity.player.Player player) {
        return new CrucibleMenu(id, inventory, this);
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
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanBelow(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        measureDelta();

        boilOff();
        runProcess();
        setChanged();
    }

    private int countInsulation() {
        if (level == null)
            return 0;
        int found = 0;
        for (Direction face : Direction.values())
            if (level.getBlockState(worldPosition.relative(face)).is(FTags.INSULATION))
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, contents, true, registries);
        saveCore(tag);
        tag.putInt("Insulation", insulation);
        tag.putInt("Dampers", dampers);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        ContainerHelper.loadAllItems(tag, contents, registries);
        loadCore(tag);
        insulation = tag.getInt("Insulation");
        dampers = tag.getInt("Dampers");
        offline.load(tag);
    }
}
