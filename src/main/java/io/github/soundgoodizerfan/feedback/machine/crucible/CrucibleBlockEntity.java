// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.crucible;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import io.github.soundgoodizerfan.feedback.registry.FMenus;

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
 * buys with it is the option to walk away, which is philosophy 8's possible/reliable/economical
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
public class CrucibleBlockEntity extends io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselCore implements net.minecraft.world.MenuProvider, io.github.soundgoodizerfan.feedback.machine.Wearing {

    /** How often the vessel looks around to see what has been built against it. */
    private static final int INSULATION_RECHECK_INTERVAL = 20;

    /** What a built crucible keeps of its design: its slots, tank, heat mass and the hottest its wall holds ({@code BlockFigures}). */
    private final io.github.soundgoodizerfan.feedback.modular.bake.FigureStore figures = new io.github.soundgoodizerfan.feedback.modular.bake.FigureStore();
    private NonNullList<ItemStack> contents = NonNullList.withSize(slotCount(), ItemStack.EMPTY);

    private int insulation;
    /** An arc hearth's wall: 1 as built, worn by a bath hotter than its lining holds. Other crucibles never wear. */
    private float wall = 1f;
    private int dampers;
    private int sinceInsulationCheck;

    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRUCIBLE.get(), pos, state);
    }

    /** The slots and tank a built crucible's figures give, applied once they are known: the charge list is resized, keeping what it held. */
    private void sizeToFigures() {
        int slots = Math.max(1, Math.round(figures.get("Slots", contents.size())));
        if (slots != contents.size()) {
            NonNullList<ItemStack> resized = NonNullList.withSize(slots, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(slots, contents.size()); i++)
                resized.set(i, contents.get(i));
            contents = resized;
        }
        tank.setCapacity(Math.round(figures.get("Tank", tank.getCapacity())));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BLOCK_FIGURES.get()),
                input.get(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get()), input.get(io.github.soundgoodizerfan.feedback.registry.FModular.FIGURES.get()));
        sizeToFigures();
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    /** Read off the block, so a bigger hearth is the same entity with more room. Used while the fields are built, so it names no field. */
    private int slotCount() {
        return getBlockState().getBlock() instanceof CrucibleBlock block ? block.slots() : VesselTuning.CRUCIBLE_SLOTS;
    }

    @Override
    protected int tankCapacity() {
        return getBlockState().getBlock() instanceof CrucibleBlock block ? block.tankMb() : VesselTuning.VESSEL_TANK_CAPACITY_MB;
    }

    @Override
    protected java.util.List<ItemStack> processSlots() {
        return contents;
    }

    // --- thermal ----------------------------------------------------------------------------

    /**
     * Thermal shock: a melt poured into a wall far cooler than it is (glass and ceramic walls are rated by a {@code shock_limit}; metal walls are
     * not) wears the wall by how many times the limit the jump overshoots, and a wall worn through gives way, as when it is held too hot.
     */
    @Override
    protected void onPour(net.minecraft.world.level.material.Fluid fluid, float pourTu) {
        float limit = figures.get("ShockTu", 0f);
        if (limit <= 0f || level == null || level.isClientSide || isArcHearth())
            return;
        float over = Math.max(0f, (Math.abs(pourTu - temperature) - limit) / limit);
        if (over <= 0f)
            return;
        wall = Math.max(0f, wall - VesselTuning.CRUCIBLE_SHOCK_WEAR * over);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.2f);
        if (wall <= VesselTuning.CRUCIBLE_WALL_BREAK)
            level.destroyBlock(worldPosition, false);
        setChanged();
    }

    @Override
    public ThermalMass getThermalMass() {
        ThermalMass base = getBlockState().getBlock() instanceof CrucibleBlock crucible
                ? new ThermalMass(figures.get("Mass", crucible.getThermalMass().workPerTu()))
                : VesselTuning.CRUCIBLE_SMALL_MASS;
        float mass = base.workPerTu() + chargeWorkPerTu();
        for (UpgradeFitting upgrade : upgrades)
            mass += upgrade.addedThermalMass().workPerTu();
        return new ThermalMass(mass);
    }

    @Override
    public Conductance getLeak() {
        return new Conductance(ThermalTuning.VESSEL_LEAK.workPerTickPerTu()
                * (float) Math.pow(VesselTuning.INSULATION_LEAK_FACTOR, insulation) / wall
                + dampers * VesselTuning.DAMPER_LEAK_BONUS.workPerTickPerTu());
    }

    /** The hollow this vessel holds, mB: what a picture of it is sized by. */
    public float shownTankMb() {
        return tank.getCapacity();
    }

    /** The wall's mass as designed (the charge not counted): what a picture of it is thickened by. */
    public float shownWallMass() {
        return getBlockState().getBlock() instanceof CrucibleBlock crucible
                ? figures.get("Mass", crucible.getThermalMass().workPerTu()) : VesselTuning.CRUCIBLE_SMALL_MASS.workPerTu();
    }

    /** Game time until which the attuned wall's experience-fed heat holds, and the Tu above ambient it lifts the heat source to. Not saved: a read renews it. */
    private long wallHeatUntil;
    private float wallHeatTu;

    /** Whether the crucible holds any stack or fluid (an attuned wall draws experience only then). */
    public boolean holdsAnything() {
        return processSlots().stream().anyMatch(s -> !s.isEmpty()) || alloy.amount() > 0 || !tank.isEmpty();
    }

    /** One aura read of the wall's heat attunement: when {@code fed} the heat source holds until the next read. */
    public void wallHeat(int level, boolean fed, long now) {
        if (fed) {
            wallHeatUntil = now + 45;
            wallHeatTu = level * VesselTuning.WALL_HEAT_TU_PER_LEVEL;
        }
    }

    public boolean isArcHearth() {
        return getBlockState().is(io.github.soundgoodizerfan.feedback.registry.FBlocks.ARC_HEARTH.get());
    }

    /** The wall's condition: a worn hearth leaks more heat. Always 1 for a plain crucible. */
    @Override
    public float getCondition() {
        return wall;
    }

    @Override
    public boolean isRunning() {
        return currentProcess != null;
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
            // The temperature it arrives at is remembered per slot and soaks toward the vessel's.
            soakStart(slot, taken);
            contents.set(slot, taken);
            resetSlot(slot);
            sync();
            return true;
        }
        return false;
    }

    /**
     * Take something out, at the temperature the vessel is at.
     *
     * <h3>Where a workpiece picks up its heat</h3>
     * Contents are not stamped while they sit here: each slot remembers the temperature its item arrived at
     * and when, and {@link #slotTemperature} soaks that toward the vessel's, closed-form, so a cold piece laid
     * in a hot crucible reads as warming rather than as already hot. The stamp is applied at the moment the item
     * leaves, which is the moment it starts cooling on its own and the moment anybody could ask.
     */
    public ItemStack removeItem(int slot) {
        ItemStack taken = contents.get(slot);
        if (taken.isEmpty())
            return ItemStack.EMPTY;
        Tu now = slotTemperature(slot);
        contents.set(slot, ItemStack.EMPTY);
        resetSlot(slot);
        if (level != null)
            ItemHeat.set(taken, now, level);
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
        Tu now = slotTemperature(slot);
        ItemStack taken = held.split(count);
        if (level != null)
            ItemHeat.set(taken, now, level);
        sync();
        return taken;
    }

    /** Put a stack into a slot by hand; a cold item arrives cold, as {@link #insert} has it. */
    public void putItem(int slot, ItemStack stack) {
        soakStart(slot, stack);
        contents.set(slot, stack);
        resetSlot(slot);
        sync();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                                          net.minecraft.world.entity.player.Player player) {
        return new CrucibleMenu(isArcHearth() ? FMenus.ARC_HEARTH.get() : FMenus.CRUCIBLE.get(), id, inventory, this);
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

        Tu fireTu = HeatSource.at(level, this);
        Tu ambient = AmbientTable.at(level);
        // An attuned wall fed experience is a small heat source of its own, never above what the wall's level lifts it to.
        if (level.getGameTime() < wallHeatUntil)
            fireTu = new Tu(Math.max(fireTu.value(), ambient.value() + wallHeatTu));
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanAt(level, this, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        measureDelta();
        float wallTu = figures.get("ServiceTu", 0f);
        if (!isArcHearth() && wallTu > 0f) {
            // A built crucible's wall holds as hot as its own material does: past that it wears (as the arc hearth's does), and a spent wall gives way.
            float excess = temperature - wallTu;
            if (excess > 0f)
                wall = Math.max(0f, wall - VesselTuning.ARC_HEARTH_WEAR_PER_TU_EXCESS * excess);
            if (wall <= VesselTuning.CRUCIBLE_WALL_BREAK) {
                level.destroyBlock(worldPosition, false);
                return;
            }
        }
        if (isArcHearth()) {
            boolean lined = upgrades.stream().anyMatch(u -> u instanceof RefractoryLiningFitting);
            float excess = temperature - (lined ? VesselTuning.ARC_HEARTH_LINED_TU : VesselTuning.ARC_HEARTH_UNLINED_TU);
            if (excess > 0f)
                wall = Math.max(VesselTuning.ARC_HEARTH_CONDITION_FLOOR, wall - VesselTuning.ARC_HEARTH_WEAR_PER_TU_EXCESS * excess);
        }

        boilOff();
        runProcess();
        glow();
        setChanged();
    }

    @Override
    protected boolean darkened() {
        return insulation > 0;
    }

    private int countInsulation() {
        if (level == null)
            return 0;
        int found = 0;
        for (Direction face : Direction.values())
            if (level.getBlockState(worldPosition.relative(face)).is(FTags.INSULATION))
                found++;
        return Math.min(VesselTuning.INSULATION_MAX_BLOCKS, found);
    }

    private int countOpenDampers() {
        if (level == null)
            return 0;
        int found = 0;
        for (Direction face : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(face)) instanceof DamperBlockEntity damper
                    && damper.isEngaged())
                found++;
        return Math.min(VesselTuning.DAMPER_MAX_BLOCKS, found);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, contents, true, registries);
        figures.save(tag, registries);
        saveCore(tag);
        tag.putInt("Insulation", insulation);
        tag.putInt("Dampers", dampers);
        tag.putFloat("HearthWear", wall);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        sizeToFigures();
        contents.clear();
        ContainerHelper.loadAllItems(tag, contents, registries);
        loadCore(tag);
        insulation = tag.getInt("Insulation");
        dampers = tag.getInt("Dampers");
        wall = tag.contains("HearthWear") ? tag.getFloat("HearthWear") : 1f;
        offline.load(tag);
    }
}
