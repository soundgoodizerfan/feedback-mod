// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.process.PhaseChange;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.RefractoryLiningFitting;
import io.github.soundgoodizerfan.feedback.fitting.sensor.TemperatureSensorFitting;
import io.github.soundgoodizerfan.feedback.machine.bellows.Blown;
import io.github.soundgoodizerfan.feedback.process.Fuel;
import io.github.soundgoodizerfan.feedback.process.MoltenVessel;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.VanillaFallback;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A self-fired thermal vessel with nine generic workpiece slots and one fuel slot -- all six
 * vessels in {@link VesselKind}, one class, differing only by kind (§15).
 *
 * <h2>Why this replaced mixing into the vanilla furnace family</h2>
 * The first pass at §15 mixed into {@code AbstractFurnaceBlockEntity} and its menu/screen, and it
 * worked right up until it had to disagree with vanilla about what a slot was for. Every vanilla
 * assumption this mod needed to change -- what slot 2 means, whether a click there is even
 * accepted, whether the container will let an item sit there, what the GUI draws -- turned out to
 * be hardcoded somewhere else in the vanilla class hierarchy, and each one only surfaced by
 * actually crashing. An independent block, block entity, menu and screen has none of that: every
 * assumption here is one this class actually makes.
 *
 * <h2>Nine slots, and why they are interchangeable</h2>
 * Unlike the constrained three-slot vanilla shape, there is always a free slot to place a result
 * in, which is what lets this go back to the crucible's own model rather than the whole-stack
 * workaround the mixin pass needed: {@link ThermalProcessTable} is checked against the whole
 * inventory first (a multi-ingredient process, like steel, can be satisfied by any two slots),
 * and whatever it doesn't touch falls through to {@link VanillaFallback} independently, per slot.
 * No slot is the input, the reagent or the output; a chest of nine identical stacks is what the
 * vessel actually is, physically.
 */
public class ThermalVesselBlockEntity extends ThermalVesselCore
        implements Container, MenuProvider, HeatSource, Blown {

    public static final int SLOTS = 9;
    public static final int SLOT_FUEL = SLOTS;
    private static final int TOTAL_SLOTS = SLOTS + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    /** What a boiling crude gives off (petrochemistry spec 3.4); a vessel with a melt in it never fills it. */
    private final FluidTank vapourTank = new FluidTank(FTuning.VESSEL_TANK_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int fuelTicksLeft;
    private int fuelDuration;
    private float flameRollTu = FTuning.AMBIENT_TU.value();

    /** Banked by a bellows; see {@code FireboxBlockEntity} for why draught, not the bank itself,
     * sets the flame temperature. This vessel is its own fire, so it takes air directly rather
     * than through a separate firebox. */
    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();

    private float air;
    private float draught;

    private final VesselKind kind;
    /** Fuel-left, fuel-duration, temperature (whole Tu) -- what the screen actually needs. */
    private final ContainerData data = new SimpleContainerData(3);

    public ThermalVesselBlockEntity(BlockPos pos, BlockState state, VesselKind kind) {
        super(FBlockEntities.THERMAL_VESSEL.get(), pos, state);
        this.kind = kind;
    }

    public VesselKind getKind() {
        return kind;
    }

    public ContainerData getContainerData() {
        return data;
    }

    public FluidTank getVapourTank() {
        return vapourTank;
    }

    // --- thermal --------------------------------------------------------------------------

    @Override
    public ThermalMass getThermalMass() {
        if (upgrades.isEmpty())
            return kind.mass();
        float mass = kind.mass().workPerTu();
        for (UpgradeFitting upgrade : upgrades)
            mass += upgrade.addedThermalMass().workPerTu();
        return new ThermalMass(mass);
    }

    @Override
    public Conductance getLeak() {
        return kind.leak();
    }

    @Override
    public boolean hasThermowell() {
        return kind.hasThermowell();
    }

    /** A vessel with a rabble takes its shaft's turning off the network and works it against the charge. */
    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.Su getLoadSu() {
        return kind.rotates() ? FTuning.PUDDLING_FURNACE_LOAD_SU : NO_SU;
    }

    @Override
    protected float rabbleRpm() {
        return kind.rotates() ? Math.abs(getRpm().value()) : 0f;
    }

    @Override
    public Tu getFireTu() {
        if (fuelTicksLeft <= 0)
            return AmbientTable.at(level);
        // Air multiplies the fuel's own flame temperature rather than replacing it -- see
        // FireboxBlockEntity for why a cool fuel blown hard is still a cool fire.
        float blown = Mth.clamp(draught / FTuning.FIREBOX_AIR_PER_TICK.value(), 0f, 1f);
        return new Tu(flameRollTu * Mth.lerp(blown, 1f, FTuning.FULL_AIR_TEMPERATURE_FACTOR));
    }

    /**
     * A vessel takes air directly rather than through a separate firebox -- it is already its
     * own fire (§15), so a bellows aimed at it plays exactly the role it plays for a crucible's
     * firebox, just without the middleman.
     */
    @Override
    public void addAir(Mb amount) {
        air = Math.min(FTuning.FIREBOX_MAX_AIR.value(), air + amount.value());
    }

    // --- the tick ---------------------------------------------------------------------------

    @Override
    protected List<ItemStack> processSlots() {
        return items.subList(0, SLOTS);
    }

    @Override
    protected float smoke() {
        return kind.smoke();
    }

    @Override
    protected boolean airlessRun() {
        return kind.sealed() && draught <= 0f;
    }

    /** What boils off a vessel's tank goes to its vapour tank, and past a full one out of the chamber. */
    @Override
    protected int vapourRoom() {
        return vapourTank.getSpace();
    }

    @Override
    protected void vent(PhaseChange change, int mB, float at) {
        var gas = BuiltInRegistries.FLUID.getOptional(change.gasFluid());
        int stored = gas.isPresent() ? vapourTank.fill(FluidHeat.stamp(new FluidStack(gas.get(), mB), at),
                IFluidHandler.FluidAction.EXECUTE) : 0;
        if (stored < mB)
            super.vent(change, mB - stored, at);
    }

    public void tickServer() {
        if (level == null)
            return;

        long away = offline.take(level);
        if (away > 0L) {
            // Back from an unloaded chunk: burn what the fuel slot could have and relax across the
            // gap in one step (see Catchup). Nobody was blowing the bellows, and nothing here
            // advances a process -- the contents resume where they were, at the temperature they
            // now have.
            air = 0f;
            Catchup.settle(this, burnAcross(away), AmbientTable.at(level), away);
            if (temperature > kind.ceilingTu())
                temperature = kind.ceilingTu();
        }

        draught = Math.min(air, FTuning.FIREBOX_AIR_PER_TICK.value());
        air -= draught;

        if (fuelTicksLeft <= 0)
            light();
        boolean lit = fuelTicksLeft > 0;
        Tu fireTu = getFireTu();
        if (lit)
            fuelTicksLeft--;

        Heat.tick(this, fireTu, AmbientTable.at(level), lit);
        // A clamp, not a leak, on purpose: the smoker's low ceiling is the vessel's material limit
        // (philosophy §19, "built as one wall"), and heat past it is lost through a box that
        // cannot hold more. See FTuning.SMOKER_CEILING_TU.
        if (temperature > kind.ceilingTu())
            temperature = kind.ceilingTu();
        measureDelta();
        boilOff();
        pushVapour();

        runProcess();

        data.set(0, fuelTicksLeft);
        data.set(1, fuelDuration);
        data.set(2, Math.round(temperature));

        BlockState state = getBlockState();
        if (state.hasProperty(ThermalVesselBlock.LIT) && state.getValue(ThermalVesselBlock.LIT) != lit)
            level.setBlock(worldPosition, state.setValue(ThermalVesselBlock.LIT, lit), 3);
        glow();

        setChanged();
    }

    private FireSpan burnAcross(long gap) {
        long left = gap;
        long lit = 0L;
        double weighted = 0.0;

        if (fuelTicksLeft > 0) {
            long t = Math.min(fuelTicksLeft, left);
            fuelTicksLeft -= (int) t;
            left -= t;
            lit += t;
            weighted += t * (double) flameRollTu;
        }
        ItemStack stack = items.get(SLOT_FUEL);
        while (left > 0L && !stack.isEmpty()) {
            Optional<Fuel> found = Fuel.findInAir(stack);
            if (found.isEmpty())
                break;
            Fuel burning = found.get();
            long t = Math.min(burning.duration(), left);
            lit += t;
            left -= t;
            weighted += t * (double) burning.temperature().value();

            ItemStack remainder = stack.getCraftingRemainingItem();
            stack.shrink(1);
            if (stack.isEmpty() && !remainder.isEmpty()) {
                items.set(SLOT_FUEL, remainder);
                stack = remainder;
            }
            if (t < burning.duration()) {
                fuelTicksLeft = (int) (burning.duration() - t);
                fuelDuration = burning.duration();
                flameRollTu = burning.temperature().value();
            }
        }
        return lit <= 0L ? FireSpan.NONE : new FireSpan(lit, (float) (weighted / lit));
    }

    private void light() {
        ItemStack fuelStack = items.get(SLOT_FUEL);
        Optional<Fuel> found = Fuel.findInAir(fuelStack);
        if (found.isEmpty())
            return;

        Fuel burning = found.get();
        fuelTicksLeft = burning.duration();
        fuelDuration = burning.duration();
        // Rolled once and held for the whole burn -- see FireboxBlockEntity for why per-tick
        // noise would be the same variance with nothing in it to learn.
        float roll = 1f + (level.random.nextFloat() * 2f - 1f) * burning.spread();
        flameRollTu = burning.temperature().value() * roll;

        // Read before shrinking: an emptied stack has no remainder left to ask for.
        ItemStack remainder = fuelStack.getCraftingRemainingItem();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty() && !remainder.isEmpty())
            items.set(SLOT_FUEL, remainder);
    }

    // --- Container ----------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        resetSlotProgress(slot);
        return stampLeaving(slot, ContainerHelper.removeItem(items, slot, count));
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        resetSlotProgress(slot);
        return stampLeaving(slot, ContainerHelper.takeItem(items, slot));
    }

    /** Anything leaving a working slot -- by hand, hopper or pipe -- leaves at the vessel's temperature (§9). */
    private ItemStack stampLeaving(int slot, ItemStack taken) {
        if (slot < SLOTS && level != null && !taken.isEmpty())
            ItemHeat.set(taken, slotTemperature(slot), level);
        return taken;
    }

    private void resetSlotProgress(int slot) {
        if (slot < SLOTS)
            resetSlot(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != SLOT_FUEL || Fuel.find(stack).isPresent();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!ItemStack.isSameItemSameComponents(items.get(slot), stack))
            resetSlotProgress(slot);
        if (slot < SLOTS && !ItemStack.isSameItemSameComponents(items.get(slot), stack))
            soakStart(slot, stack);
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // --- MenuProvider ---------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ThermalVesselMenu(id, playerInventory, this);
    }

    // --- persistence ----------------------------------------------------------------------------

    /**
     * What boiled off into the vapour tank is pushed on to whatever fluid handler sits beside (a condenser). The boiling itself, hand
     * distillation of a crude included, is {@link ThermalVesselCore#boilOff}.
     */
    private void pushVapour() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel))
            return;
        if (vapourTank.isEmpty())
            return;
        for (Direction d : Direction.values()) {
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d),
                    d.getOpposite());
            if (target == null)
                continue;
            int fits = target.fill(vapourTank.getFluid(), IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                vapourTank.drain(target.fill(vapourTank.drain(fits, IFluidHandler.FluidAction.SIMULATE),
                        IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        saveCore(tag);
        tag.putInt("FuelTicksLeft", fuelTicksLeft);
        tag.putInt("FuelDuration", fuelDuration);
        tag.putFloat("FlameRoll", flameRollTu);
        tag.putFloat("Air", air);
        offline.save(tag, level);
        // A crude or a gas held in the tank; a melt is the ledger's, written by saveCore.
        if (!ledgerHeld())
            tank.writeToNBT(registries, tag);
        tag.put("Vapour", vapourTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        loadCore(tag);
        fuelTicksLeft = tag.getInt("FuelTicksLeft");
        fuelDuration = tag.getInt("FuelDuration");
        flameRollTu = tag.getFloat("FlameRoll");
        air = tag.getFloat("Air");
        offline.load(tag);
        if (!ledgerHeld())
            tank.readFromNBT(registries, tag);
        vapourTank.readFromNBT(registries, tag.getCompound("Vapour"));
    }
}
