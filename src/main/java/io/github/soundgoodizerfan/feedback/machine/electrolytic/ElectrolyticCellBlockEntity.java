// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electrolytic;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.hazard.Corrosion;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.item.MassProbeItem;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Electrorefining: a metal anode dissolves into the water, and the current plates metal back out
 * onto a cathode (roadmap slice 5). The first thing in the mod that turns
 * electricity into chemistry, and the first reader of {@link Qu}.
 *
 * <h2>Voltage is a threshold, current is the rate</h2>
 * A process asks for "at least this voltage; this much current is how fast it
 * goes", never a bare Eu cost. So this sink does not run on {@link ElectricRules#rate}. Every amp
 * delivered moves {@link ElectricMachineTuning#QU_PER_AMP_TICK} of metal a tick, and the voltage it sees decides
 * only <em>which</em> metal: below a metal's deposition potential no amount of current plates it,
 * which is one of 6.5's genuine physical thresholds and so a hard gate in the §7 sense.
 *
 * <h2>The chemistry selects the metal</h2>
 * The noblest metal in solution (highest {@code electrode_potential}) always plates; a less noble one plates only if it sits
 * within the electrolyte's window behind it ({@link ElectricMachineTuning#ELECTROLYSIS_WATER_WINDOW} for water,
 * {@link ElectricMachineTuning#ELECTROLYSIS_ACID_WINDOW} for an acid or a base). The band only has to be enough to run the
 * cell: it never picks a metal. Brass dissolves as copper and zinc (its {@code composition}); zinc sits 1.1 behind copper, so
 * in water the cell plates copper out of brass and leaves the zinc in solution, while an acid bath plates both. Bronze's tin
 * (0.48 behind) stays in water too. The electrolyte and the anode's material are the choice; nothing here names a metal.
 *
 * <h2>Overrun: the finished deposit is what gets spoiled</h2>
 * While the anode lasts it keeps the noble metal coming. Once it is gone the anode can only split
 * water, which needs {@link ElectricMachineTuning#ELECTROLYSIS_WATER_BAND}: below that the cell simply stops,
 * and a low-band refinery is safe to forget. At it or above the current strips what is left in the
 * water -- the noble metal first, then whatever was waiting behind it, which plates onto the
 * cathode the player has not yet taken. Purity is judged on the whole deposit when it comes off
 * ({@link ElectricMachineTuning#ELECTROLYTIC_PURE_FRACTION}), so a few minutes too long turns a cathode of copper
 * into copper scrap. The faster, hotter supply is also the one that punishes a late hand.
 *
 * <h2>The water remembers (philosophy 9)</h2>
 * Metal that did not plate stays dissolved. A brass anode refined at low voltage leaves its zinc
 * behind, and the next anode put in that water inherits it. Draining the tank takes the dissolved
 * metal with it, in proportion.
 *
 * <h2>Electroplating</h2>
 * A metal piece hung as the cathode takes the plating instead of the cell's own deposit, by the
 * same chemistry: the electrolyte picks the metal, current is the rate. What is new is only what the piece
 * can take -- a coat that is too thin, burnt by too much current for its size, or so thick it
 * flakes off -- see {@link #takeWork} and {@link #burnAmps}.
 *
 * <h2>Over-voltage goes into the cell</h2>
 * The electromagnet's rule: a band over its LV rating is condition lost and heat into its own
 * body, never a refusal and never destruction. Condition multiplies the metal moved per amp.
 */
public class ElectrolyticCellBlockEntity extends ElectricNode implements ThermalBody, Wearing {

    private static final float EPS = 1e-5f;

    private float temperature = ThermalTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private boolean working;

    private ItemStack anode = ItemStack.EMPTY;
    /** The piece of anode currently dissolving: its material, and Qu of it left. */
    @Nullable
    private ResourceLocation dissolving;
    private float dissolvingLeft;

    /** Qu of each metal dissolved in the water, and plated on the cathode. */
    private final Map<ResourceLocation, Float> solution = new LinkedHashMap<>();
    private final Map<ResourceLocation, Float> deposit = new LinkedHashMap<>();

    /**
     * A piece hung as the cathode to be plated, its coat so far, and how much of
     * that coat grew at a current its surface could not take. While a piece hangs, metal plates
     * onto it rather than onto the cathode's own deposit.
     */
    private ItemStack work = ItemStack.EMPTY;
    private final Map<ResourceLocation, Float> coat = new LinkedHashMap<>();
    private float burntQu;
    /** Set each tick before {@link #electrolyse}: the current this tick is over the piece's limit. */
    private boolean burning;

    private final FluidTank water = new FluidTank(ElectricMachineTuning.ELECTROLYTIC_CELL_TANK_MB,
            stack -> stack.is(FluidTags.WATER) || Corrosion.corrosive(stack)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public ElectrolyticCellBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTROLYTIC_CELL.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    /** Asks for current while there is water to carry it and metal to move. */
    @Override
    public float wantedAmps() {
        boolean metal = !anode.isEmpty() || dissolvingLeft > EPS || !solution.isEmpty();
        return conducts() && metal ? ElectricMachineTuning.ELECTROLYTIC_CELL_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        burning = !work.isEmpty() && amps > burnAmps(work);
        working = amps > 0f && conducts() && electrolyse(eu > 0f && band.compareTo(ElectricMachineTuning.ELECTROLYSIS_WATER_BAND) >= 0, amps * ElectricMachineTuning.QU_PER_AMP_TICK * condition);

        float excess = ElectricRules.overVoltage(band, eu, ratedBand());
        if (excess > 0f) {
            condition = Math.max(ElectricMachineTuning.ELECTROLYTIC_CELL_CONDITION_FLOOR,
                    condition - ElectricMachineTuning.ELECTROLYTIC_CELL_WEAR_PER_EXCESS * excess);
            Heat.warm(this, ElectricMachineTuning.ELECTROLYTIC_CELL_HEAT_PER_EXCESS * excess);
            // Over-drive cracks a hung probe's crystal too: condition lost, never refusal.
            if (work.getItem() instanceof MassProbeItem)
                work.set(FDataComponents.PROBE_CONDITION.get(), Math.max(FTuning.MASS_PROBE_CONDITION_FLOOR,
                        MassProbeItem.condition(work) - ElectricMachineTuning.MASS_PROBE_WEAR_PER_EXCESS * excess));
            setChanged();
        }
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, ambient, ambient, false);
        // An acid or a base in a glass-and-wire cell bites it a little each second (core/hazard/Corrosion).
        if (level.getGameTime() % 20 == 0 && Corrosion.corrosive(water.getFluid())) {
            float bite = Corrosion.wearPerSecond(HazardTuning.ELECTROLYSER_LINING);
            if (bite > 0f) {
                condition = Math.max(ElectricMachineTuning.ELECTROLYTIC_CELL_CONDITION_FLOOR, condition - bite);
                setChanged();
            }
        }
    }

    /** Whether the tank holds an acid or a base, which is what lets a bearer of metal (an ore) go into solution. */
    private boolean leaching() {
        return Corrosion.corrosive(water.getFluid());
    }

    /** What a piece dissolves into: an alloy's composition, or, in an acid or base, the metals an ore bears. */
    private Map<ResourceLocation, Float> shares(Material material, ResourceLocation id) {
        if (material.composition().isEmpty() && !material.bears().isEmpty() && leaching()) {
            float total = 0f;
            for (float b : material.bears().values())
                total += b;
            Map<ResourceLocation, Float> out = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, Float> e : material.bears().entrySet())
                out.put(e.getKey(), e.getValue() / total);
            return out;
        }
        return material.constituents(id);
    }

    private boolean conducts() {
        return water.getFluidAmount() >= ElectricMachineTuning.ELECTROLYTIC_CELL_MIN_WATER_MB;
    }

    // --- the chemistry ------------------------------------------------------------------------

    /**
     * One tick of {@code charge} Qu through the cell, fed a band that {@code splitsWater} or not. Returns whether anything
     * happened. Package-visible arithmetic, so a test can drive it with any band.
     */
    boolean electrolyse(boolean splitsWater, float charge) {
        if (charge <= 0f)
            return false;
        float window = leaching() ? ElectricMachineTuning.ELECTROLYSIS_ACID_WINDOW : ElectricMachineTuning.ELECTROLYSIS_WATER_WINDOW;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();

        // The anode: dissolve metal, else split water, else nothing can carry the charge.
        float carried = dissolve(materials, charge);
        if (splitsWater)
            carried = charge;
        if (carried <= EPS)
            return false;

        // The cathode: noblest first, each only if it sits within the electrolyte's window behind the noblest.
        List<Map.Entry<ResourceLocation, Float>> present = new ArrayList<>();
        float noblest = -Float.MAX_VALUE;
        for (Map.Entry<ResourceLocation, Float> e : solution.entrySet()) {
            Optional<Float> potential = potential(materials, e.getKey());
            if (potential.isEmpty())
                continue;
            present.add(Map.entry(e.getKey(), potential.get()));
            noblest = Math.max(noblest, potential.get());
        }
        present.sort(Collections.reverseOrder(Map.Entry.comparingByValue()));
        for (Map.Entry<ResourceLocation, Float> s : present) {
            if (carried <= EPS
                    || noblest - s.getValue() > window)
                break;
            float plated = Math.min(carried, solution.get(s.getKey()));
            move(solution, work.isEmpty() ? deposit : coat, s.getKey(), plated);
            if (burning)
                burntQu += plated;
            carried -= plated;
        }
        setChanged();
        return true;
    }

    /** Dissolves up to {@code charge} Qu of anode into the water. Returns how much went. */
    private float dissolve(Map<ResourceLocation, Material> materials, float charge) {
        float done = 0f;
        while (done < charge - EPS) {
            if (dissolvingLeft <= EPS && !nextPiece(materials))
                break;
            float d = Math.min(charge - done, dissolvingLeft);
            dissolvingLeft -= d;
            done += d;
            Material material = materials.get(dissolving);
            if (material != null)
                shares(material, dissolving).forEach((m, share) -> solution.merge(m, share * d, Float::sum));
        }
        return done;
    }

    /** Takes one item off the anode stack to dissolve next. */
    private boolean nextPiece(Map<ResourceLocation, Material> materials) {
        while (!anode.isEmpty()) {
            Optional<Material.Located> located = Material.find(materials, anode.getItem());
            anode.shrink(1);
            if (located.isPresent() && dissolvable(materials, located.get())) {
                dissolving = located.get().id();
                dissolvingLeft = quPerItem(located.get().form()).value()
                        * (located.get().material().bears().isEmpty() || !leaching() ? 1f
                        : located.get().material().bears().values().stream().reduce(0f, Float::sum)
                        * io.github.soundgoodizerfan.feedback.process.OreLadder.ORE_CONTENT
                        * io.github.soundgoodizerfan.feedback.process.OreLadder.dissolve(located.get()));
                return true;
            }
        }
        return false;
    }

    private static Optional<Float> potential(Map<ResourceLocation, Material> materials, ResourceLocation id) {
        Material material = materials.get(id);
        return material == null ? Optional.empty() : material.electrical().electrodePotential();
    }

    /** A metal, or an alloy of them: everything it dissolves into has an electrode potential. */
    private boolean dissolvable(Map<ResourceLocation, Material> materials, Material.Located located) {
        for (ResourceLocation part : shares(located.material(), located.id()).keySet())
            if (potential(materials, part).isEmpty())
                return false;
        return true;
    }

    /** How much metal one item of a form is. A nugget is a ninth of an ingot, a block nine. */
    static Qu quPerItem(String form) {
        float ingot = FTuning.QU_PER_INGOT.value();
        return new Qu(switch (form) {
            case "nugget" -> ingot / 9f;
            case "block" -> ingot * 9f;
            default -> ingot;
        });
    }

    private static void move(Map<ResourceLocation, Float> from, Map<ResourceLocation, Float> to,
                             ResourceLocation id, float amount) {
        float left = from.getOrDefault(id, 0f) - amount;
        if (left <= EPS)
            from.remove(id);
        else
            from.put(id, left);
        to.merge(id, amount, Float::sum);
    }

    private static void scale(Map<ResourceLocation, Float> map, float factor) {
        map.replaceAll((id, qu) -> qu * factor);
        map.values().removeIf(qu -> qu <= EPS);
    }

    // --- electroplating -------------------------------------------------------

    /**
     * The current a piece takes before its coat burns: current density, with a piece's surface
     * going as its size to the two-thirds, its size read off its form as the cell reads an anode -- or,
     * for a modular piece, off its baked mass over its metal's density, in ingots of 144 mB.
     */
    static float burnAmps(ItemStack piece) {
        float size;
        if (ModularStacks.platable(piece))
            size = Math.max(ModularStacks.volumeMb(piece), 1f) / ModularTuning.CELL_MB;
        else
            size = quPerItem(Material.find(MaterialTable.get().materials(), piece.getItem())
                    .map(Material.Located::form).orElse("ingot")).value() / FTuning.QU_PER_INGOT.value();
        return ElectricMachineTuning.PLATING_BURN_AMPS_PER_INGOT * (float) Math.pow(size, 2.0 / 3.0);
    }

    /**
     * Hangs one of {@code stack} as the cathode, to be plated. Only a metal conducts, so only a
     * metal can be a cathode. A piece already plated carries its coat back in, to grow further.
     */
    public boolean hangWork(ItemStack stack) {
        if (!work.isEmpty() || !hangable(stack))
            return false;
        work = stack.split(1);
        Plating plating = work.remove(FDataComponents.PLATING.get());
        if (plating != null) {
            coat.put(plating.metal(), plating.thickness());
            burntQu = plating.quality() == Plating.Quality.BURNT ? plating.thickness() : 0f;
        }
        setChanged();
        return true;
    }

    /**
     * Takes the hung piece out with whatever grew on it. A coat past
     * {@link MachineTuning#PLATING_SPALLS_ABOVE_QU} is too thick to hold and flakes off as the piece
     * comes out -- the overrun, spoiling the finished coat -- leaving the piece bare and the metal
     * as nuggets, which hang as an anode again: the loss is time and current, as a scrap cathode's.
     */
    public List<ItemStack> takeWork() {
        if (work.isEmpty())
            return List.of();
        ItemStack piece = work;
        work = ItemStack.EMPTY;
        float total = 0f;
        ResourceLocation dominant = null;
        for (Map.Entry<ResourceLocation, Float> e : coat.entrySet()) {
            total += e.getValue();
            if (dominant == null || e.getValue() > coat.get(dominant))
                dominant = e.getKey();
        }
        float burnt = burntQu;
        Map<ResourceLocation, Float> grown = new LinkedHashMap<>(coat);
        coat.clear();
        burntQu = 0f;
        setChanged();
        if (dominant == null || total <= EPS) {
            ModularStacks.rebake(piece);
            return List.of(piece);
        }
        if (total > MachineTuning.PLATING_SPALLS_ABOVE_QU) {
            List<ItemStack> out = new ArrayList<>(List.of(piece));
            Material material = MaterialTable.get().materials().get(dominant);
            int nuggets = (int) Math.floor(total / quPerItem("nugget").value() + 1e-4f);
            if (material != null && nuggets > 0)
                material.item(dominant, "nugget").ifPresent(n -> out.add(new ItemStack(n, nuggets)));
            ModularStacks.rebake(piece);
            return out;
        }
        Plating.Quality quality = burnt / total > ElectricMachineTuning.PLATING_BURNT_SHARE ? Plating.Quality.BURNT
                : grown.get(dominant) / total < ElectricMachineTuning.ELECTROLYTIC_PURE_FRACTION ? Plating.Quality.MIXED
                : total < FTuning.PLATING_POROUS_BELOW_QU ? Plating.Quality.POROUS
                : Plating.Quality.SOUND;
        piece.set(FDataComponents.PLATING.get(), new Plating(dominant, total, quality));
        ModularStacks.rebake(piece);
        return List.of(piece);
    }

    public ItemStack getWork() {
        return work;
    }

    /** Total Qu of coat on the hung piece, all metals. */
    public float coatTotal() {
        float total = 0f;
        for (float qu : coat.values())
            total += qu;
        return total;
    }

    /** Qu of each metal on the hung piece. A view; for instruments and tests. */
    public Map<ResourceLocation, Float> getCoat() {
        return Collections.unmodifiableMap(coat);
    }

    // --- the player's side --------------------------------------------------------------------

    /** Whether {@code stack} can hang as an anode: a metal or an alloy of metals, in any form. */
    /** A piece that can hang as a cathode: an anode's metal, or a modular tool or plate whose wearing part is one. */
    public static boolean hangable(ItemStack stack) {
        return isAnode(stack) || ModularStacks.platable(stack) || stack.getItem() instanceof MassProbeItem;
    }

    public static boolean isAnode(ItemStack stack) {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        return Material.find(materials, stack.getItem()).filter(l -> metalsOnly(materials, l)).isPresent();
    }

    /** A metal or alloy whose every part has an electrode potential, whatever is in the tank. */
    private static boolean metalsOnly(Map<ResourceLocation, Material> materials, Material.Located located) {
        for (ResourceLocation part : located.material().constituents(located.id()).keySet())
            if (potential(materials, part).isEmpty())
                return false;
        return true;
    }

    /** Hangs as much of {@code stack} as fits; returns what did not. */
    public ItemStack insertAnode(ItemStack stack) {
        if (!isAnode(stack) && !(leaching() && Material.find(MaterialTable.get().materials(), stack.getItem())
                .filter(l -> dissolvable(MaterialTable.get().materials(), l)).isPresent()))
            return stack;
        if (anode.isEmpty()) {
            anode = stack.split(stack.getMaxStackSize());
        } else if (ItemStack.isSameItemSameComponents(anode, stack)) {
            int moved = Math.min(stack.getCount(), anode.getMaxStackSize() - anode.getCount());
            anode.grow(moved);
            stack.shrink(moved);
        }
        setChanged();
        return stack;
    }

    public ItemStack removeAnode() {
        ItemStack out = anode;
        anode = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    /**
     * Strips every whole ingot's worth of deposit off the cathode. Ingots of the dominant metal if
     * the deposit is pure enough, that metal's scrap if it is not; a part-ingot stays plated.
     */
    public List<ItemStack> takeCathode() {
        float total = 0f;
        ResourceLocation dominant = null;
        for (Map.Entry<ResourceLocation, Float> e : deposit.entrySet()) {
            total += e.getValue();
            if (dominant == null || e.getValue() > deposit.get(dominant))
                dominant = e.getKey();
        }
        int whole = (int) Math.floor(total / FTuning.QU_PER_INGOT.value() + 1e-4f);
        Material material = dominant == null ? null : MaterialTable.get().materials().get(dominant);
        if (whole <= 0 || material == null)
            return List.of();
        boolean pure = deposit.get(dominant) / total >= ElectricMachineTuning.ELECTROLYTIC_PURE_FRACTION;
        Optional<Item> item = material.item(dominant, pure ? "ingot" : "scrap");
        if (item.isEmpty())
            return List.of();

        scale(deposit, 1f - whole * FTuning.QU_PER_INGOT.value() / total);
        setChanged();
        List<ItemStack> out = new ArrayList<>();
        for (int left = whole; left > 0; ) {
            ItemStack stack = new ItemStack(item.get(), Math.min(left, item.get().getDefaultMaxStackSize()));
            left -= stack.getCount();
            out.add(stack);
        }
        return out;
    }

    public ItemStack getAnode() {
        return anode;
    }

    public FluidTank getWater() {
        return water;
    }

    /** Qu of each metal in the water. A view; for instruments and tests. */
    public Map<ResourceLocation, Float> getSolution() {
        return Collections.unmodifiableMap(solution);
    }

    /** Qu of each metal on the cathode. A view; for instruments and tests. */
    public Map<ResourceLocation, Float> getDeposit() {
        return Collections.unmodifiableMap(deposit);
    }

    /** Water in, water out; what drains takes its share of the dissolved metal with it. */
    public IFluidHandler getFluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return water.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return water.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return water.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return water.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return drained(water.getFluidAmount(), water.drain(resource, action), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return drained(water.getFluidAmount(), water.drain(maxDrain, action), action);
            }
        };
    }

    private FluidStack drained(int before, FluidStack out, IFluidHandler.FluidAction action) {
        if (action.execute() && before > 0 && !out.isEmpty())
            scale(solution, (float) water.getFluidAmount() / before);
        return out;
    }

    // --- Wearing ------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return working;
    }

    // --- ThermalBody --------------------------------------------------------------------------

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
        return ElectricMachineTuning.ELECTROLYTIC_CELL_MASS;
    }

    @Override
    public Conductance getLeak() {
        return ElectricMachineTuning.ELECTROLYTIC_CELL_LEAK;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
        if (!anode.isEmpty())
            tag.put("Anode", anode.save(registries));
        if (dissolving != null) {
            tag.putString("Dissolving", dissolving.toString());
            tag.putFloat("DissolvingLeft", dissolvingLeft);
        }
        tag.put("Solution", write(solution));
        tag.put("Deposit", write(deposit));
        if (!work.isEmpty())
            tag.put("Work", work.save(registries));
        tag.put("Coat", write(coat));
        tag.putFloat("BurntQu", burntQu);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        anode = ItemStack.parseOptional(registries, tag.getCompound("Anode"));
        dissolving = tag.contains("Dissolving") ? ResourceLocation.tryParse(tag.getString("Dissolving")) : null;
        dissolvingLeft = tag.getFloat("DissolvingLeft");
        read(tag.getCompound("Solution"), solution);
        read(tag.getCompound("Deposit"), deposit);
        work = ItemStack.parseOptional(registries, tag.getCompound("Work"));
        read(tag.getCompound("Coat"), coat);
        burntQu = tag.getFloat("BurntQu");
        water.readFromNBT(registries, tag.getCompound("Water"));
    }

    private static CompoundTag write(Map<ResourceLocation, Float> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach((id, qu) -> tag.putFloat(id.toString(), qu));
        return tag;
    }

    private static void read(CompoundTag tag, Map<ResourceLocation, Float> into) {
        into.clear();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null)
                into.put(id, tag.getFloat(key));
        }
    }
}
