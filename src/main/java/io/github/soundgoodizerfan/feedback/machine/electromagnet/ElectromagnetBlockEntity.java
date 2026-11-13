// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electromagnet;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.OreLadder;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.modular.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The first consumer, and the first thing to show what a sink does with whatever it is given.
 *
 * <h2>It runs on what arrives, and never refuses a line</h2>
 * Its field strength is {@link ElectricRules#rate}: the fraction of its rated voltage it sees
 * times the fraction of its wanted current it gets, times its own condition. A long run or a weak
 * source makes it weak, not dead -- the floor is zero and nothing about the line is refused. At
 * zero it does nothing and is none the worse for it: under-voltage never wears.
 *
 * <h2>Over-voltage goes into the machine</h2>
 * Volts past its rating are force that cannot go into the work, so they go into the magnet:
 * condition falls, and heat goes into its own {@link ThermalBody} through the same {@link
 * Heat#warm} a wire's overload uses. It degrades to {@link ElectricMachineTuning#ELECTROMAGNET_CONDITION_FLOOR}
 * and is never destroyed and never explodes; a worn magnet is evidence of one wiring mistake, and
 * how far it wore says how wrong and for how long. (No band above LV is built, so this is reached
 * only by a test or by a future source.)
 *
 * <h2>What it does</h2>
 * Pulls dropped items toward itself, for any item whose material says it is magnetic
 * ({@code separation.magnetic} in the material file). No identity check: iron, steel, cast iron
 * and netherite are pulled because they share a property, and a pack that adds a magnetic
 * material gets the same behaviour with no code. A workpiece hotter than its material's Curie point
 * is not pulled (it passes the magnet, as it passes the drum).
 */
public class ElectromagnetBlockEntity extends ElectricNode implements ThermalBody, Wearing {

    private float temperature = ThermalTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private float field;
    private int sinceLastPull;

    // Every item whose material is magnetic, rebuilt when the material table reloads.
    private static int magneticGeneration = -1;
    private static Set<Item> magneticItems = Set.of();

    private final FigureStore figures = new FigureStore();

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    public ElectromagnetBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTROMAGNET.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return figures.get("Amps", ElectricMachineTuning.ELECTROMAGNET_AMPS);
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        field = ElectricRules.rate(volts, amps, wanted, ratedBand()) * condition;

        float excess = ElectricRules.overVoltage(volts, ratedBand());
        if (excess > 0f) {
            condition = Math.max(ElectricMachineTuning.ELECTROMAGNET_CONDITION_FLOOR,
                    condition - ElectricMachineTuning.ELECTROMAGNET_WEAR_PER_EXCESS * excess);
            Heat.warm(this, ElectricMachineTuning.ELECTROMAGNET_HEAT_PER_EXCESS * excess);
            setChanged();
        }
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, ambient, ambient, false);
    }

    /** 0 to 1: how hard it is pulling right now. Condition has already multiplied it. */
    public float getFieldStrength() {
        return field;
    }

    // --- what it does -------------------------------------------------------------------------

    public void tickServer() {
        if (field <= 0f || level == null || ++sinceLastPull < ElectricMachineTuning.ELECTROMAGNET_PULL_INTERVAL_TICKS)
            return;
        sinceLastPull = 0;

        double range = figures.get("Range", ElectricMachineTuning.ELECTROMAGNET_RANGE) * field;
        Vec3 centre = Vec3.atCenterOf(worldPosition);
        AABB reach = new AABB(worldPosition).inflate(range);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, reach)) {
            boolean blank = item.getItem().is(io.github.soundgoodizerfan.feedback.registry.FItems.RARE_EARTH_MAGNET_BLANK.get());
            if (!blank && (!isMagnetic(item.getItem()) || aboveCurie(item.getItem())))
                continue;
            Vec3 toward = centre.subtract(item.position());
            if (blank && field >= ElectricMachineTuning.MAGNETISE_FIELD && toward.lengthSqr() < SEPARATE_REACH_SQ)
                magnetise(item);
            if (toward.lengthSqr() < SEPARATE_REACH_SQ && OreLadder.feed(item.getItem()).isPresent())
                separate(item);
            if (toward.lengthSqr() < 0.25 || toward.length() > range)
                continue;
            item.setDeltaMovement(item.getDeltaMovement()
                    .add(toward.normalize().scale(figures.get("Pull", ElectricMachineTuning.ELECTROMAGNET_PULL) * field)));
            item.hasImpulse = true;
        }
    }

    /**
     * Magnetising (grid spec 3): a sintered blank held close in a field at full strength becomes a magnet, one unit a pull. A
     * weaker field does nothing; the blank is only pulled.
     */
    private void magnetise(ItemEntity item) {
        net.minecraft.world.item.ItemStack stack = item.getItem();
        stack.shrink(1);
        if (stack.isEmpty())
            item.discard();
        level.addFreshEntity(new ItemEntity(level, item.getX(), item.getY(), item.getZ(),
                new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        io.github.soundgoodizerfan.feedback.Feedback.id("rare_earth_magnet_ingot")))));
    }

    private static final double SEPARATE_REACH_SQ = 1.5 * 1.5;
    private static final int SEPARATE_PER_PULL = 8;

    /**
     * Magnetic separation: ground ore of a magnetic bearer that the magnet has drawn close sheds its gangue. Each unit the magnet can tell from
     * its rock (the bearer's magnetism against its gangue's) becomes a concentrate, which clings, and a tailings, which is not pulled and
     * falls away; the rest stays to be pulled again. At most {@link #SEPARATE_PER_PULL} units a pull.
     */
    private void separate(ItemEntity item) {
        net.minecraft.world.item.ItemStack stack = item.getItem();
        net.minecraft.world.item.ItemStack concentrate = net.minecraft.world.item.ItemStack.EMPTY, tailings = net.minecraft.world.item.ItemStack.EMPTY;
        for (int i = 0; i < SEPARATE_PER_PULL && stack.getCount() > 0; i++) {
            var split = OreLadder.attempt(stack, OreLadder.Mode.MAGNETIC, level.random);
            if (split.isEmpty())
                continue;
            stack.shrink(1);
            concentrate = concentrate.isEmpty() ? split.get().concentrate() : concentrate.copyWithCount(concentrate.getCount() + 1);
            tailings = tailings.isEmpty() ? split.get().tailings() : tailings.copyWithCount(tailings.getCount() + 1);
        }
        if (concentrate.isEmpty())
            return;
        if (stack.isEmpty())
            item.discard();
        level.addFreshEntity(new ItemEntity(level, item.getX(), item.getY(), item.getZ(), concentrate));
        level.addFreshEntity(new ItemEntity(level, item.getX(), item.getY() - 0.5, item.getZ(), tailings));
    }

    /** Whether a stack is hotter than its material's Curie point: a hot workpiece is not pulled (user ruling, 2026-10-23). */
    private boolean aboveCurie(net.minecraft.world.item.ItemStack stack) {
        io.github.soundgoodizerfan.feedback.process.MadeOf made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        Material m = made != null ? MaterialTable.get().materials().get(made.material())
                : Material.find(MaterialTable.get().materials(), stack).map(Material.Located::material).orElse(null);
        return m != null && m.separation().curieTemperature().filter(curie ->
                io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(stack, level).value() > curie.value()).isPresent();
    }

    /** Whether a magnet pulls this stack: a shaped part by its {@code MadeOf}, anything else by its item. */
    public static boolean isMagnetic(net.minecraft.world.item.ItemStack stack) {
        io.github.soundgoodizerfan.feedback.process.MadeOf made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        if (made == null)
            return isMagnetic(stack.getItem());
        Material m = MaterialTable.get().materials().get(made.material());
        return m != null && m.separation().magnetic();
    }

    /** Whether a magnet pulls this item: its material says it is magnetic. */
    public static boolean isMagnetic(Item item) {
        if (magneticGeneration != MaterialTable.generation()) {
            magneticGeneration = MaterialTable.generation();
            Set<Item> found = new HashSet<>();
            for (Map.Entry<ResourceLocation, Material> entry : MaterialTable.get().materials().entrySet())
                if (entry.getValue().separation().magnetic())
                    // Tailings are the gangue left over: the same material, but not what the magnet takes.
                    entry.getValue().items(entry.getKey()).entrySet().stream().filter(e -> !e.getKey().equals("tailings")).map(Map.Entry::getValue)
                            .filter(i -> !(i instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem)).forEach(found::add);
            magneticItems = found;
        }
        return magneticItems.contains(item);
    }

    // --- Wearing ------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    /** A magnet that is pulling is working and cannot be measured, like any machine (philosophy 10). */
    @Override
    public boolean isRunning() {
        return field > 0f;
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
        return ElectricMachineTuning.ELECTROMAGNET_MASS;
    }

    @Override
    public Conductance getLeak() {
        return ElectricMachineTuning.ELECTROMAGNET_LEAK;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
