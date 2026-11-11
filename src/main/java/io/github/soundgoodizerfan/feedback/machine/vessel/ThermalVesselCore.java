// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Vapour;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.geology.CrudeGrade;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
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
import io.github.soundgoodizerfan.feedback.process.ProcessRun;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Mixture;
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
public abstract class ThermalVesselCore extends io.github.soundgoodizerfan.feedback.core.rotation.RotationNode implements ThermalBody, MoltenVessel, Fittable {

    /** One slot per face, a sensor or an adapter, never both. Indexed by {@link Direction#get3DDataValue()}. */
    protected final SidedFitting[] fittings = new SidedFitting[Direction.values().length];
    /** At most one of each kind; the only concrete one so far is a {@link RefractoryLiningFitting}. */
    protected final List<UpgradeFitting> upgrades = new ArrayList<>();

    protected final io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank tank =
            new io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank(tankCapacity(), this::tankTakes, this::sync);
    /** Every fluid poured in as a melt, by amount; {@link AlloyMix#resolve} is what the tank shows and a mold can drain. */
    protected final AlloyMix alloy = new AlloyMix();
    private boolean ledgerOwned;

    /** Held as a primitive and wrapped at the accessor: a {@link Tu} field would outlive its method and could not be scalar-replaced. */
    protected float temperature = FTuning.AMBIENT_TU.value();
    /** How fast it moved last tick. A process may care about that as well as how hot it got. */
    protected float lastDelta;
    private float temperatureLastTick = Float.NaN;
    protected float currentTpu;
    /** Progress of each held stack's own one-input row (cooking, a single melt), alongside the whole contents' {@link #currentTpu}. */
    private float[] slotTpu = new float[0];
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

    /** Whether the vessel hides its glow (insulation round it); a vessel with a wall of its own overrides this. */
    protected boolean darkened() {
        return false;
    }

    /**
     * A vessel hot enough glows, empty or full, (spec incandescence): the band of its temperature sets a real block light level, moved only
     * when the band changes. Call at the end of a tick, after any state change of its own.
     */
    protected void glow() {
        if (level == null)
            return;
        io.github.soundgoodizerfan.feedback.core.thermal.Incandescence.apply(level, worldPosition,
                !darkened() ? io.github.soundgoodizerfan.feedback.core.thermal.Incandescence.light(temperature) : 0);
    }

    /**
     * How fast the rabble turns, in RPM: zero for a vessel with none. A row that names a rabble band only advances while this is inside
     * it, so such a row runs in a vessel with a shaft and nowhere else.
     */
    protected float rabbleRpm() {
        return 0f;
    }

    /** Whether the process is held in a sealed chamber with no air blowing in (charcoal, sintering). */
    protected boolean airlessRun() {
        return false;
    }

    /** The tank's size, in mB. Read while the base is constructed, so it names a constant and no field. */
    protected int tankCapacity() {
        return FTuning.VESSEL_TANK_CAPACITY_MB;
    }

    /** Whether the tank takes this fluid at all (a boiler takes water); every fluid by default. */
    protected boolean tankTakes(FluidStack stack) {
        return true;
    }

    /** How many mB of {@code cut} to boil this tick, given the share-of-the-tank figure {@code wanted}; a boiler spends the heat it has instead. */
    protected int boilAmount(PhaseChange cut, float wanted) {
        return (int) wanted;
    }

    /** What boiling {@code mB} of {@code cut} takes out of the vessel, in Tu. */
    protected float boilCost(PhaseChange cut, int mB) {
        return mB * cut.latent() / FTuning.VESSEL_BOIL_MASS_WORK_PER_TU;
    }

    /** How much smoke the vessel gives a cooking food (a smoker's); none by default. */
    protected float smoke() {
        return 0f;
    }

    private float[] soakFrom = new float[0];
    private long[] soakAt = new long[0];

    private void soakSized(int n) {
        if (soakFrom.length < n) {
            soakFrom = java.util.Arrays.copyOf(soakFrom, n);
            soakAt = java.util.Arrays.copyOf(soakAt, n);
        }
    }

    /** Remember what a piece laid in this slot is at right now, then drop its stale stamp so it stacks like an unheated one. */
    protected final void soakStart(int slot, ItemStack stack) {
        if (level == null || slot < 0 || stack.isEmpty())
            return;
        soakSized(slot + 1);
        soakFrom[slot] = ItemHeat.get(stack, level).value();
        soakAt[slot] = level.getGameTime();
        ItemHeat.clear(stack);
    }

    /** What the item in a slot is at: the vessel's temperature, less the part of the gap it has not yet soaked up since it was laid in. */
    @Override
    public Tu slotTemperature(int slot) {
        if (level == null || slot < 0 || slot >= soakFrom.length)
            return new Tu(temperature);
        float dt = Math.max(0L, level.getGameTime() - soakAt[slot]);
        return new Tu(temperature + (soakFrom[slot] - temperature) * (float) Math.exp(-FTuning.CRUCIBLE_SOAK_PER_TICK * dt));
    }

    /** Each held stack's name and what it is at now, for a readout. */
    public List<java.util.Map.Entry<net.minecraft.network.chat.Component, Tu>> heldReadings() {
        List<java.util.Map.Entry<net.minecraft.network.chat.Component, Tu>> out = new java.util.ArrayList<>();
        List<ItemStack> held = processSlots();
        for (int i = 0; i < held.size(); i++)
            if (!held.get(i).isEmpty())
                out.add(java.util.Map.entry(held.get(i).getHoverName(), slotTemperature(i)));
        return out;
    }

    /** A slot's progress belongs to what was in it, not to whatever arrives next. */
    protected final void resetSlot(int slot) {
        if (slot >= 0 && slot < slotTpu.length)
            slotTpu[slot] = 0f;
    }

    /** Where {@code mB} of {@code change}'s vapour goes when a liquid boils: out of the open top by default. */
    protected void vent(PhaseChange change, int mB, float at) {
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

    /** Every molten fluid held: the ledger's metals, else what the tank shows. */
    public java.util.Set<net.minecraft.world.level.material.Fluid> moltenFluids() {
        if (alloy.amount() > 0)
            return alloy.content().keySet();
        FluidStack shown = tank.getFluid();
        return shown.isEmpty() ? java.util.Set.of() : java.util.Set.of(shown.getFluid());
    }

    /** Whether the tank's contents are the ledger's. */
    protected boolean ledgerHeld() {
        return alloy.amount() > 0;
    }

    // --- the process ----------------------------------------------------------------------------

    /**
     * Runs everything held: the one row the whole contents satisfy (a steel needing two inputs), then each stack the row does not use on its
     * own, in parallel: a food through its cook, char and dry ladder, any other as the one-input row it takes (an authored row, or a cooking
     * recipe as a row). Returns the whole-contents row, if any.
     */
    protected final Optional<ThermalProcess> runProcess() {
        List<ItemStack> contents = processSlots();
        if (slotTpu.length != contents.size())
            slotTpu = new float[contents.size()];
        Optional<ThermalProcess> maybe = ThermalProcessTable.get().find(contents, airlessRun());
        int[] used = null;
        if (maybe.isEmpty()) {
            currentTpu = 0f;
            currentProcess = null;
        } else {
            ThermalProcess process = maybe.get();
            if (currentProcess != null && !currentProcess.equals(process))
                currentTpu = 0f;
            currentProcess = process;
            used = ThermalProcessTable.match(process.inputs(), contents);
            advance(process, contents, -1);
        }
        for (int slot = 0; slot < contents.size() && level != null; slot++) {
            boolean taken = false;
            if (used != null)
                for (int u : used)
                    taken |= u == slot;
            if (!taken)
                runSlot(contents, slot);
        }
        return maybe;
    }

    private void runSlot(List<ItemStack> contents, int slot) {
        ItemStack input = contents.get(slot);
        if (input.isEmpty()) {
            slotTpu[slot] = 0f;
            return;
        }
        // A fine combustible dust in an open vessel past its ignition point flashes: the stack is gone and the air above it catches.
        if (io.github.soundgoodizerfan.feedback.process.DustHazard.ignites(input, temperature, airlessRun())) {
            input.setCount(0);
            contents.set(slot, ItemStack.EMPTY);
            net.minecraft.core.BlockPos above = worldPosition.above();
            if (level.getBlockState(above).isAir())
                level.setBlockAndUpdate(above, net.minecraft.world.level.block.BaseFireBlock.getState(level, above));
            sync();
            return;
        }
        // Food is a material, not a recipe: the ladder takes it past done to dried and charred (food spec 4.3).
        var food = io.github.soundgoodizerfan.feedback.process.Cooking.findCookable(level, input);
        if (food.isPresent()) {
            contents.set(slot, io.github.soundgoodizerfan.feedback.process.Cooking.tick(input, food.get(), new Tu(temperature), smoke(), level));
            slotTpu[slot] = 0f;
            return;
        }
        // A plain dip (wash dirt to clay) works on a stack sitting in the vessel the same as one held to it; a coat or imbue needs a hand.
        var dip = ThermalProcessTable.get().findDip(tank.getFluid(), input, new Tu(temperature));
        if (dip.isPresent() && dip.get().coat().isEmpty() && (!dip.get().hasFluidResult() || takesMelt())) {
            FluidStack drawn = tank.drain(dip.get().fluid().amount(), IFluidHandler.FluidAction.EXECUTE);
            onDrained(drawn);
            input.shrink(1);
            if (dip.get().hasFluidResult())
                addMolten(dip.get().resultFluid().copy());
            else
                placeHot(dip.get().result().copy());
            cleanEmpties(contents);
            return;
        }
        var row = ThermalProcessTable.get().findSingle(level, input, new Tu(temperature), airlessRun());
        if (row.isEmpty()) {
            slotTpu[slot] = 0f;
            return;
        }
        advance(row.get(), contents.subList(slot, slot + 1), slot);
    }

    private float tpuOf(int slot) {
        return slot < 0 ? currentTpu : slotTpu[slot];
    }

    private void setTpu(int slot, float value) {
        if (slot < 0)
            currentTpu = value;
        else
            slotTpu[slot] = value;
    }

    private void advance(ThermalProcess process, List<ItemStack> contents, int slot) {
        Tu tu = new Tu(temperature);

        // Overrun. The vessel did not stop, so the batch kept getting hotter, so it is ruined the instant it passes the line.
        if (process.spoilsAt(tu)) {
            spoil(process, contents, slot);
            return;
        }

        // Tempering's shape: in-band is not enough, the body has to be cooling to count. A flat or rising tick pauses the hold.
        if (process.requireCooling() && lastDelta >= 0)
            return;

        if (process.requiredTpu() <= 0f) {
            // A melt: min_temperature is the whole requirement, in band completes it outright.
            if (process.inBand(tu))
                complete(process, contents, slot);
            return;
        }

        // Outside the band, or changing too fast to trust in either direction, TPu decays rather than resetting outright.
        float suitability = ProcessRun.limited(process, process.suitability(tu), lastDelta);
        // A charge that wants stirring crusts when the rabble stalls or races: nothing advances, and the hold decays.
        if (process.rabble().isPresent() && !process.rabble().get().holds(rabbleRpm()))
            suitability = 0f;
        setTpu(slot, ProcessRun.hold(tpuOf(slot), suitability));
        if (suitability <= 0f)
            return;
        // A form-agnostic row asks less of a finer form: the hold is read from what is actually held.
        if (tpuOf(slot) < process.requiredFor(contents))
            return;

        complete(process, contents, slot);
    }

    private void complete(ThermalProcess process, List<ItemStack> contents, int slot) {
        int[] assignment = ThermalProcessTable.match(process.inputs(), contents);
        if (assignment == null)
            return;
        // A melt into a tank holding a foreign fluid has nowhere to go: it stays solid rather than being destroyed.
        if (process.hasFluidResult() && !takesMelt())
            return;
        ItemStack first = assignment.length > 0 ? contents.get(assignment[0]).copy() : ItemStack.EMPTY;
        for (int held : assignment)
            contents.get(held).shrink(1);
        cleanEmpties(contents);

        // A fine form is worse to hold: a toxic dust finishing open to the air throws up its fume.
        int dustFume = io.github.soundgoodizerfan.feedback.process.DustHazard.fume(first, airlessRun());
        if (dustFume > 0)
            io.github.soundgoodizerfan.feedback.core.hazard.FumeCloud.release(level,
                    net.minecraft.world.phys.Vec3.atCenterOf(worldPosition), dustFume);

        // A row that wants a gas spends it when it is there; without it the batch comes out as the row's ruin.
        // A sibling row (same result and hold, another gas: sintering in nitrogen or in argon) is the same batch under another atmosphere.
        boolean gassed = process.atmosphere().map(own -> {
            FluidStack held = tank.getFluid();
            var candidates = new ArrayList<ThermalProcess.Amount>();
            candidates.add(own);
            ThermalProcessTable.get().entries().stream()
                    .filter(p -> p != process && p.atmosphere().isPresent() && ItemStack.isSameItem(p.result(), process.result())
                            && p.requiredTpu() == process.requiredTpu())
                    .forEach(p -> candidates.add(p.atmosphere().get()));
            for (ThermalProcess.Amount a : candidates)
                if (!held.isEmpty() && BuiltInRegistries.FLUID.getKey(held.getFluid()).equals(a.fluid()) && held.getAmount() >= a.mb()) {
                    tank.drain(a.mb(), IFluidHandler.FluidAction.EXECUTE);
                    return true;
                }
            return false;
        }).orElse(true);

        if (!gassed) {
            placeHot(Failure.ruined(process, first));
            // A toxic material heated without its gas gives off its fume (osmium powder oxidising to the tetroxide in air).
            Material.find(MaterialTable.get().materials(), first.getItem()).ifPresent(l ->
                    io.github.soundgoodizerfan.feedback.core.hazard.FumeCloud.release(level,
                            net.minecraft.world.phys.Vec3.atCenterOf(worldPosition), io.github.soundgoodizerfan.feedback.core.hazard.Fumes.level(l.material())));
        }
        else if (process.hasFluidResult()) {
            // Whatever will not fit is lost: a full vessel melting more metal spills it. A full vessel stuck on a bad ratio
            // spills its own mix in proportion to make room, so the metal that corrects the ratio always gets in.
            // A metal dust melting in air above its oxidation point loses a share to oxide.
            float burnt = io.github.soundgoodizerfan.feedback.process.DustHazard.burnOff(first, temperature, airlessRun());
            int poured = process.resultFluid().getAmount();
            // A whole made part pours the volume it holds (its cells of stock scaled by its axes), less what a remelt loses.
            if (first.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem
                    && first.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get()))
                poured = Math.round(io.github.soundgoodizerfan.feedback.modular.Filing.volumeMb(first, level == null
                        ? java.util.Map.of() : io.github.soundgoodizerfan.feedback.modular.ModularTypes.on(level))
                        * (1f - io.github.soundgoodizerfan.feedback.modular.ModularTuning.PART_REMELT_LOSS));
            FluidStack result = process.resultFluid().copyWithAmount(Math.round(poured * (1f - burnt)));
            int overflow = alloy.amount() + result.getAmount() - tank.getCapacity();
            if (overflow > 0 && alloy.resolve().isEmpty())
                alloy.remove(Math.min(overflow, alloy.amount()));
            alloy.add(result.getFluid(), Math.min(result.getAmount(), tank.getCapacity() - alloy.amount()));
            refreshTank();
            // A melt may also leave a solid behind (the slag of smelting an ore): its item result is that byproduct.
            placeHot(process.result().copy());
        } else if (process.fire().isPresent())
            placeHot(io.github.soundgoodizerfan.feedback.modular.ModularStacks.fired(first, process.fire().get()));
        else
            placeHot(process.result().copy());

        bankedXp = Math.min(FTuning.VESSEL_XP_BANK_MAX, bankedXp
                + io.github.soundgoodizerfan.feedback.process.VanillaFallback.xpOf(process));
        setTpu(slot, 0f);
        sync();
    }

    private void spoil(ThermalProcess process, List<ItemStack> contents, int slot) {
        int[] assignment = ThermalProcessTable.match(process.inputs(), contents);
        if (assignment == null)
            return;
        ItemStack first = assignment.length > 0 ? contents.get(assignment[0]).copy() : ItemStack.EMPTY;
        for (int held : assignment)
            contents.get(held).shrink(1);
        cleanEmpties(contents);
        placeHot(Failure.ruined(process, first));
        setTpu(slot, 0f);
        sync();
        io.github.soundgoodizerfan.feedback.core.Awards.near(level, worldPosition, "progress/joke/overdone");
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

    /** How much vapour a boil can send on right now, in mB; a vessel with a vapour tank says what is left of it. Open ones vent without limit. */
    protected int vapourRoom() {
        return Integer.MAX_VALUE;
    }

    /**
     * Whatever liquid the phase rows boil, held at or past its boiling point, boils: a share of it a tick leaves as vapour
     * ({@link #vent}) and each mB takes its latent heat out of the vessel. A pure liquid is the plain case; a crude, whose rows carry a
     * {@code light_share}, boils that share of its grade's light fraction and is left heavier. Names no fluid. A vessel holding anything
     * that gives off fumes lets them out whether or not anything boils.
     */
    protected final void boilOff() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server))
            return;
        FluidStack shown = tank.getFluid();
        if (!shown.isEmpty()) {
            FluidStack hot = FluidHeat.stamp(shown, temperature);
            Vapour.source(server, worldPosition, Vapour.potency(hot), 20, Vapour.burnTicks(hot) / 4, false);
        }
        if (alloy.amount() > 0) {
            for (var e : new ArrayList<>(alloy.content().entrySet())) {
                var change = ThermalProcessTable.get().boiling(new FluidStack(e.getKey(), e.getValue()));
                if (change.isEmpty()) {
                    if (boilMetal(server, e.getKey(), e.getValue()))
                        return;
                    continue;
                }
                if (change.get().lightShare().isPresent() || temperature < change.get().boil().value())
                    continue;
                float want = e.getValue() * FTuning.VESSEL_BOIL_PER_TICK + boilOwed;
                int amount = alloy.take(e.getKey(), Math.min(Math.max(1, boilAmount(change.get(), want)), vapourRoom()));
                boilOwed = want - (int) want;
                if (amount <= 0)
                    continue;
                float at = temperature;
                temperature -= boilCost(change.get(), amount);
                refreshTank();
                vent(change.get(), amount, at);
                sync();
                return;
            }
            return;
        }
        // A fluid the ledger does not hold (a crude in a furnace's tank): its grade rides on the stack.
        FluidStack held = tank.getFluid();
        if (held.isEmpty())
            return;
        for (PhaseChange cut : ThermalProcessTable.get().phases()) {
            if (!cut.boils(held) || temperature < cut.boil().value())
                continue;
            var grade = CrudeGrade.of(held);
            float light = cut.lightShare().isPresent() ? grade.lightFraction() : 1f;
            float want = held.getAmount() * light * cut.lightShare().orElse(1f) * FTuning.VESSEL_BOIL_PER_TICK + boilOwed;
            int amount = Math.min(Math.min(boilAmount(cut, want), tank.getFluidAmount()), vapourRoom());
            boilOwed = want - (int) want;
            if (amount <= 0)
                continue;
            int before = tank.getFluidAmount();
            float at = temperature;
            tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            temperature -= boilCost(cut, amount);
            // The lights drawn off are no longer in the feed.
            if (cut.lightShare().isPresent() && !tank.isEmpty() && before > amount)
                tank.getFluid().set(FDataComponents.CRUDE_GRADE.get(), grade.withLightsDrawn(light, before, amount));
            vent(cut, amount, at);
            return;
        }
    }

    /**
     * A molten metal held past its material's {@code boiling_point} boils off the bath: a share of it a tick leaves as a fume, so a
     * volatile metal (zinc, lead, tin) is lost and the alloy's ratio drifts. Read off the material, so any metal with a boiling point
     * does it, and the fume dose is the material's {@code toxic} perk with a floor of one (metal fume is a hazard). Returns whether
     * anything boiled.
     */
    private boolean boilMetal(net.minecraft.server.level.ServerLevel server, net.minecraft.world.level.material.Fluid fluid, int held) {
        var materials = MaterialTable.get().materials();
        var id = Mixture.fluidMaterial(materials, fluid);
        if (id.isEmpty())
            return false;
        var material = materials.get(id.get());
        var boiling = material.thermal().boilingPoint();
        if (boiling.isEmpty() || temperature < boiling.get().value())
            return false;
        float want = held * FTuning.VESSEL_BOIL_PER_TICK + boilOwed;
        int amount = alloy.take(fluid, Math.max(1, (int) want));
        boilOwed = want - (int) want;
        if (amount <= 0)
            return false;
        temperature -= amount * FTuning.METAL_BOIL_COOLING_TU_PER_MB;
        refreshTank();
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 1, 0.15, 0.05, 0.15, 0.02);
        io.github.soundgoodizerfan.feedback.core.hazard.Fumes.release(server, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition),
                Math.max(1, io.github.soundgoodizerfan.feedback.core.hazard.Fumes.level(material)));
        sync();
        return true;
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

    /** Experience a vanilla cooking recipe banked here, in points; a Smelter Tap drains it, a broken vessel spills it. */
    private float bankedXp;

    /** Points banked, whole. */
    public int bankedXp() {
        return (int) bankedXp;
    }

    /** Takes up to {@code wanted} whole points out of the bank; the points that came. */
    public int drainXp(int wanted) {
        int took = Math.min(wanted, (int) bankedXp);
        if (took > 0) {
            bankedXp -= took;
            setChanged();
        }
        return took;
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
        tag.putFloat("BankedXp", bankedXp);
        for (int i = 0; i < slotTpu.length; i++)
            tag.putFloat("SlotTpu" + i, slotTpu[i]);
        int[] bits = new int[soakFrom.length];
        for (int i = 0; i < bits.length; i++)
            bits[i] = Float.floatToIntBits(soakFrom[i]);
        tag.putIntArray("SoakFrom", bits);
        tag.putLongArray("SoakAt", soakAt);
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
        bankedXp = tag.getFloat("BankedXp");
        List<ItemStack> held = processSlots();
        slotTpu = new float[held.size()];
        for (int i = 0; i < slotTpu.length; i++)
            slotTpu[i] = tag.getFloat("SlotTpu" + i);
        int[] bits = tag.getIntArray("SoakFrom");
        long[] at = tag.getLongArray("SoakAt");
        int n = Math.min(bits.length, at.length);
        soakFrom = new float[n];
        soakAt = Arrays.copyOf(at, n);
        for (int i = 0; i < n; i++)
            soakFrom[i] = Float.intBitsToFloat(bits[i]);
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
