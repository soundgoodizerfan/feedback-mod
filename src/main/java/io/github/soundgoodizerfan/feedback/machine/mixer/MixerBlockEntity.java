// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.mixer;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.OreLadder;
import io.github.soundgoodizerfan.feedback.process.Mixture;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Blast;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A drum of loose powders, turned by the shaft (guns spec 13.4b). The Reloading Press seats the
 * ratio it is given; this is what makes a ratio a blend.
 *
 * <p>Uniformity climbs with turns until {@link FTuning#MIXER_BLEND_TURNS}. Draw the batch early and
 * only the uniform share of it is usable (lumps and dead pockets do not burn): {@link #draw} keeps
 * that share of every component and the rest is lost, so a failed batch consumes its inputs.
 *
 * <p><b>The machine does not know it is done.</b> Keep turning and the drum grinds the finished
 * blend: grains break down and segregate by density, so uniformity falls toward its floor, and each
 * turn adds friction heat. When the heat above ambient reaches the lowest ignition temperature in
 * the load, the load goes up with {@link Blast}'s rule. Nothing here wears the body: the drum's
 * only victim is its own contents.
 *
 * <p><b>Items and fluids.</b> Input slots pour one item at a time into the drum while there is room
 * (the rule of {@link #pour}); a fluid with a material is a part like an item, {@link FTuning#MIXER_MB_PER_PART}
 * mB to the part, and counts toward the same cap. Fluid parts stay out of {@link Cartridge.Blend}, so a
 * mixture with one is never a gun charge. {@link #draw} asks the mixture table what the drawn parts
 * resolve into: an item result comes out in the output slot, a fluid result is held as the product and
 * leaves through the fluid port; no row, and the generic mixture item comes out. Hoppers and pipes reach
 * the inputs and the drawn output, never the drum.
 *
 * <p><b>Hot drum.</b> The drum is a {@link ThermalBody} that reads {@link HeatSource#at} as the
 * crucible does; with no fire it sits at ambient. Friction heat rides on top of the body's temperature, and
 * {@link #trueTemperature()} (body plus friction) is what ignition and the mixture table's Tu window see.
 * A molten fluid part in a drum under its material's melting point is a solid lump: it stays a part, but the
 * drum will not draw until it is hot enough to stir (see {@link #frozen()}).
 *
 * <p><b>[OPEN]</b> Numbers are placeholders; whether the drum should cool when stopped.
 */
public class MixerBlockEntity extends RotationNode implements MenuProvider, ThermalBody {

    public static final int OUTPUT = FTuning.MIXER_INPUT_SLOTS;

    private final List<Cartridge.Charge> contents = new ArrayList<>();
    private final Map<ResourceLocation, Integer> fluids = new LinkedHashMap<>();
    /** Input slots, then the drawn item. */
    private final SimpleContainer slots = new SimpleContainer(FTuning.MIXER_INPUT_SLOTS + 1);
    private FluidStack product = FluidStack.EMPTY;
    private float turns;
    private float heat;
    /** Water in the drum, mB: the medium a turning drum washes ore in (gravity separation, {@link OreLadder}); not a mixture part. */
    private int water;
    /** Body temperature, Tu; primitive field wrapped at the accessor. */
    private float temperature = FTuning.AMBIENT_TU.value();
    private final Catchup offline = new Catchup();

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.MIXER_ENTITY.get(), pos, state);
        slots.addListener(c -> setChanged());
    }

    @Override
    public Su getLoadSu() {
        return FTuning.MIXER_LOAD_SU;
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
        return FTuning.MIXER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.MIXER_LEAK;
    }

    /** What the load actually sits at: the drum's body plus friction heat. */
    public Tu trueTemperature() {
        return new Tu(temperature + heat);
    }

    /** Whether a molten fluid part is below its material's melting point, so the drum cannot stir it. */
    public boolean frozen() {
        var materials = MaterialTable.get().materials();
        for (var id : fluids.keySet()) {
            Material m = materials.get(id);
            if (m != null && m.thermal().meltingPoint().isPresent() && trueTemperature().value() < m.thermal().meltingPoint().get().value())
                return true;
        }
        return false;
    }

    /** The drum's load in parts: items one each, fluid by {@link FTuning#MIXER_MB_PER_PART}. */
    private float load() {
        return contents.stream().mapToInt(Cartridge.Charge::count).sum() + fluidMb() / (float) FTuning.MIXER_MB_PER_PART;
    }

    /** Fluid in the drum, mB. */
    public int fluidMb() {
        return fluids.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The drawn fluid waiting at the port (empty when none). */
    public FluidStack product() {
        return product;
    }

    public SimpleContainer slots() {
        return slots;
    }

    /** The drum's own fluid parts, mB by material: the drum is open, so anyone may look. */
    public Map<ResourceLocation, Integer> fluidParts() {
        return Map.copyOf(fluids);
    }

    /** Whether an item could be poured at all: it is a material, in any form. */
    public static boolean pourable(ItemStack stack, Map<ResourceLocation, Material> materials) {
        return !stack.isEmpty() && Material.find(materials, stack.getItem()).isPresent();
    }

    /** Pours one item in; false if the drum is full or the load would be a coil's drive mixed with a gas. */
    public boolean pour(ItemStack stack) {
        var materials = MaterialTable.get().materials();
        var found = Material.find(materials, stack.getItem());
        if (found.isEmpty() || load() + 1 > FTuning.GUN_CHARGE_MAX)
            return false;
        ResourceLocation id = found.get().id();
        List<Cartridge.Charge> next = new ArrayList<>(contents);
        int at = -1;
        for (int k = 0; k < next.size(); k++)
            if (next.get(k).material().equals(id))
                at = k;
        if (at >= 0)
            next.set(at, new Cartridge.Charge(id, next.get(at).count() + 1));
        else
            next.add(new Cartridge.Charge(id, 1));
        // Only a drum of nothing but propellants is a charge, and only that can be refused (a coil's drive with a gas).
        boolean allPropellant = next.stream().allMatch(c -> materials.get(c.material()) != null && materials.get(c.material()).propellant().isPresent());
        if (allPropellant && Cartridge.Blend.of(materials, next).isEmpty())
            return false;
        // Fresh powder on top of a blend is unmixed: the old turns spread over the larger load.
        turns *= load() / (load() + 1);
        contents.clear();
        contents.addAll(next);
        stack.shrink(1);
        sync();
        return true;
    }

    /** Takes fluid into the drum as a part; returns how much went in. Zero if it has no material, a product is waiting, or the drum is full. */
    public int fill(FluidStack stack, boolean simulate) {
        if (stack.getFluid() == net.minecraft.world.level.material.Fluids.WATER) {
            int taken = Math.max(0, Math.min(stack.getAmount(), OreLadder.WASH_WATER_MB - water));
            if (taken > 0 && !simulate) {
                water += taken;
                sync();
            }
            return taken;
        }
        if (stack.isEmpty() || !product.isEmpty())
            return 0;
        var material = Mixture.fluidMaterial(MaterialTable.get().materials(), stack.getFluid());
        if (material.isEmpty())
            return 0;
        int room = (int) ((FTuning.GUN_CHARGE_MAX - load()) * FTuning.MIXER_MB_PER_PART);
        int taken = Math.min(stack.getAmount(), room);
        if (taken <= 0 || simulate)
            return Math.max(taken, 0);
        float before = load();
        fluids.merge(material.get(), taken, Integer::sum);
        turns *= before / load();
        sync();
        return taken;
    }

    /** What is in the drum, as parts for the mixture table: items, then fluids as whole parts. */
    private List<Cartridge.Charge> parts() {
        List<Cartridge.Charge> all = new ArrayList<>(contents);
        fluids.forEach((material, mb) -> all.add(new Mixture.FluidPart(material, mb).asCharge()));
        return all;
    }

    /** What is in the drum: the drum is open, so anyone may look. */
    public List<Cartridge.Charge> contents() {
        return List.copyOf(contents);
    }

    /** Friction heat above ambient, Tu; state, so a HUD reads it through the instrument seam. */
    public float heat() {
        return heat;
    }

    /** Whether the drum has turned past the blend point and is grinding the finished mix. */
    public boolean overmixed() {
        return turns > FTuning.MIXER_BLEND_TURNS;
    }

    /** Fraction of the load that is uniformly mixed now. */
    public float uniformity() {
        if (turns <= FTuning.MIXER_BLEND_TURNS)
            return turns / FTuning.MIXER_BLEND_TURNS;
        float over = (turns - FTuning.MIXER_BLEND_TURNS) / FTuning.MIXER_SEGREGATE_TURNS;
        return Math.max(FTuning.MIXER_UNIFORMITY_FLOOR, 1f - over * (1f - FTuning.MIXER_UNIFORMITY_FLOOR));
    }

    public boolean isEmpty() {
        return contents.isEmpty() && fluids.isEmpty();
    }

    /**
     * Empties the drum, keeping only the uniform share of each component; the rest is lost. What the
     * kept parts resolve into under the mixture table (at the drum's heat) is the result: an item
     * comes back, a fluid result is held as the product (and nothing comes back). No row leaves the
     * generic mixture item. Empty if nothing usable.
     */
    public ItemStack draw() {
        float u = uniformity();
        Tu temperature = trueTemperature();
        List<Cartridge.Charge> usable = new ArrayList<>();
        for (var c : contents) {
            int n = Math.round(c.count() * u);
            if (n > 0)
                usable.add(new Cartridge.Charge(c.material(), n));
        }
        List<Mixture.FluidPart> usableFluids = new ArrayList<>();
        fluids.forEach((material, mb) -> {
            int n = Math.round(mb * u);
            if (n > 0)
                usableFluids.add(new Mixture.FluidPart(material, n));
        });
        contents.clear();
        fluids.clear();
        turns = 0;
        heat = 0;
        sync();
        List<Cartridge.Charge> table = new ArrayList<>(usable);
        usableFluids.forEach(f -> table.add(f.asCharge()));
        if (table.isEmpty())
            return ItemStack.EMPTY;
        int parts = table.stream().mapToInt(Cartridge.Charge::count).sum();
        var row = Mixture.resolve(Mixture.table(level), table, temperature);
        if (row.isPresent()) {
            // A row's result is per part of the mixture.
            if (row.get().hasFluidResult()) {
                product = new FluidStack(row.get().resultFluid().getFluid(), row.get().resultFluid().getAmount() * parts);
                sync();
                return ItemStack.EMPTY;
            }
            if (!row.get().result().isEmpty()) {
                ItemStack made = row.get().result().copy();
                made.setCount(Math.min(made.getMaxStackSize(), made.getCount() * parts));
                return made;
            }
        }
        ItemStack out = new ItemStack(FWeapons.MIXTURE_ITEM.get());
        out.set(FWeapons.MIXTURE.get(), usable);
        if (!usableFluids.isEmpty())
            out.set(FWeapons.MIXTURE_FLUID.get(), usableFluids);
        return out;
    }

    /** The draw button: the batch goes to the output slot. False (nothing drawn) while the output or port is still occupied. */
    public boolean drawToOutput() {
        if (isEmpty() || frozen() || !slots.getItem(OUTPUT).isEmpty() || !product.isEmpty())
            return false;
        slots.setItem(OUTPUT, draw());
        return true;
    }

    /** Takes some of the product at the fluid port. */
    public FluidStack drainProduct(int max, boolean simulate) {
        if (product.isEmpty() || max <= 0)
            return FluidStack.EMPTY;
        FluidStack out = new FluidStack(product.getFluid(), Math.min(max, product.getAmount()));
        if (!simulate) {
            product.shrink(out.getAmount());
            if (product.isEmpty())
                product = FluidStack.EMPTY;
            sync();
        }
        return out;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        // Items go in one at a time, as a hand pours.
        for (int i = 0; i < FTuning.MIXER_INPUT_SLOTS; i++) {
            ItemStack in = slots.getItem(i);
            if (!in.isEmpty() && water > 0 && OreLadder.feed(in).isPresent()) {
                wash(in, server);
                continue;
            }
            if (!in.isEmpty() && pour(in))
                slots.setChanged();
        }
        Tu fire = HeatSource.at(level, worldPosition), ambient = AmbientTable.at(server);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanAt(level, worldPosition, away), ambient, away);
        Heat.tick(this, fire, ambient, fire.value() > ambient.value());
        if (isEmpty())
            return;
        float rpm = Math.abs(getRpm().value());
        if (rpm <= FTuning.STOPPED_RPM_THRESHOLD.value())
            return;
        float revs = rpm * FTuning.STROKES_PER_RPM_PER_TICK;
        turns += revs;
        if (turns > FTuning.MIXER_BLEND_TURNS)
            heat += revs * FTuning.MIXER_FRICTION_TU;
        setChanged();
        Optional<Cartridge.Blend> blend = Cartridge.Blend.of(MaterialTable.get().materials(), contents);
        if (blend.isPresent() && trueTemperature().value() >= blend.get().ignitionTu())
            detonate(server, blend.get());
    }

    /**
     * Ore feed (a bearer's crushed or dust) in a drum with water does not pour as a powder: while the shaft turns, the drum washes it
     * one unit at a time. A unit whose grains the water tells from their rock (density against the gangue) leaves as a concentrate and
     * its tailings, spilling over the rim; the rest stays in the slot to go round again. Nothing is lost on a miss.
     */
    private void wash(ItemStack in, ServerLevel server) {
        if (Math.abs(getRpm().value()) <= FTuning.STOPPED_RPM_THRESHOLD.value() || server.getGameTime() % OreLadder.WASH_INTERVAL_TICKS != 0)
            return;
        OreLadder.attempt(in, OreLadder.Mode.GRAVITY, server.random).ifPresent(split -> {
            in.shrink(1);
            slots.setChanged();
            net.minecraft.world.level.block.Block.popResource(server, worldPosition.above(), split.concentrate());
            net.minecraft.world.level.block.Block.popResource(server, worldPosition.above(), split.tailings());
        });
    }

    private void detonate(ServerLevel server, Cartridge.Blend blend) {
        float power = Blast.power(blend, null);
        boolean fire = Blast.fire(blend);
        BlockPos at = worldPosition;
        server.removeBlockEntity(at);
        server.removeBlock(at, false);
        server.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, Math.max(power, FTuning.DEMO_POWER_MIN), fire,
                Level.ExplosionInteraction.BLOCK);
    }

    // --- menu and ports ---------------------------------------------------------------------

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MixerMenu(id, inventory, this);
    }

    /** Hopper face: pours into the input slots, gives up only the drawn item. */
    public IItemHandler itemPort() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return slots.getContainerSize();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slots.getItem(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot >= OUTPUT || !isItemValid(slot, stack))
                    return stack;
                ItemStack held = slots.getItem(slot);
                if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, stack))
                    return stack;
                int room = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - held.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved <= 0)
                    return stack;
                if (!simulate) {
                    if (held.isEmpty())
                        slots.setItem(slot, stack.copyWithCount(moved));
                    else {
                        held.grow(moved);
                        slots.setChanged();
                    }
                }
                return stack.copyWithCount(stack.getCount() - moved);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != OUTPUT || amount <= 0)
                    return ItemStack.EMPTY;
                ItemStack held = slots.getItem(slot);
                int n = Math.min(amount, held.getCount());
                if (n <= 0)
                    return ItemStack.EMPTY;
                return simulate ? held.copyWithCount(n) : slots.removeItem(slot, n);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < OUTPUT && pourable(stack, MaterialTable.get().materials());
            }
        };
    }

    /** Pipe face: fluid in as parts, the fluid product out; the drum's own fluid is never shown. */
    public IFluidHandler fluidPort() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return product;
            }

            @Override
            public int getTankCapacity(int tank) {
                return FTuning.GUN_CHARGE_MAX * FTuning.MIXER_MB_PER_PART;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return stack.getFluid() == net.minecraft.world.level.material.Fluids.WATER || Mixture.fluidMaterial(MaterialTable.get().materials(), stack.getFluid()).isPresent();
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return MixerBlockEntity.this.fill(resource, action.simulate());
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.isSameFluidSameComponents(resource, product)
                        ? drainProduct(resource.getAmount(), action.simulate()) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return drainProduct(maxDrain, action.simulate());
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (var c : contents) {
            CompoundTag e = new CompoundTag();
            e.putString("material", c.material().toString());
            e.putInt("count", c.count());
            list.add(e);
        }
        tag.put("Contents", list);
        ListTag fluidList = new ListTag();
        fluids.forEach((material, mb) -> {
            CompoundTag e = new CompoundTag();
            e.putString("material", material.toString());
            e.putInt("mb", mb);
            fluidList.add(e);
        });
        tag.put("Fluids", fluidList);
        tag.put("Slots", slots.createTag(registries));
        tag.put("Product", product.saveOptional(registries));
        tag.putInt("Water", water);
        tag.putFloat("Turns", turns);
        tag.putFloat("Heat", heat);
        tag.putFloat("Temperature", temperature);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        for (Tag t : tag.getList("Contents", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            contents.add(new Cartridge.Charge(ResourceLocation.parse(e.getString("material")), e.getInt("count")));
        }
        fluids.clear();
        for (Tag t : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            fluids.put(ResourceLocation.parse(e.getString("material")), e.getInt("mb"));
        }
        slots.fromTag(tag.getList("Slots", Tag.TAG_COMPOUND), registries);
        product = FluidStack.parseOptional(registries, tag.getCompound("Product"));
        water = tag.getInt("Water");
        turns = tag.getFloat("Turns");
        heat = tag.getFloat("Heat");
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        offline.load(tag);
    }
}
