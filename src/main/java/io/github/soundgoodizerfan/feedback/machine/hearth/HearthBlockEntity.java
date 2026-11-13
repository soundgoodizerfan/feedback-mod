// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hearth;

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
import io.github.soundgoodizerfan.feedback.modular.Station;
import io.github.soundgoodizerfan.feedback.modular.StationBlock;
import io.github.soundgoodizerfan.feedback.modular.PartBenchBlockEntity;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

/**
 * A pan of brick that sits on a fire and keeps the Anvil beside it hot. Only the anvil forges, so only the anvil is reheated.
 *
 * <h2>A pan with a temperature, and nothing else</h2>
 * It is a {@link ThermalBody} like a crucible: the firebox below it ({@link HeatSource#at}) is
 * the fire, and {@link Heat#tick} drives it on the difference, so it approaches its flame and never
 * passes it (about nine tenths of it, open as it is). A hot hearth is a hot block, so
 * {@code HotContact} burns a bare hand on it with no code here.
 *
 * <h2>It reheats stock; it never makes a part hotter than itself</h2>
 * Each tick, every piece of stock in the cells of a Tool Table touching it moves
 * {@link FTuning#HEARTH_STOCK_GRIP} of the way to the pan's temperature, if the pan is hotter. That
 * is the same law as the fire on a crucible, so a cold ingot is reheated quickly and one nearly
 * at the pan's heat barely at all, and it can never overshoot the pan. {@link ItemHeat} still does
 * the cooling (1.5 Tu/t, linear), so stock beside a pan settles a fixed 50 Tu under it, and a
 * pan too cool to hold a part in its window just slows the cooling down.
 * <p>
 * Only a material with a hot-working window is reheated: the table stamps a part with heat only for
 * those, so planks and cobblestone beside a red-hot pan stay cold. That is a property read off the
 * material, not an item.
 * <p>
 * Too hot is the player's to notice. Stock left in a pan hotter than its material's window is
 * refused by the table until it cools. Raw stock (not a shaped part) held above its material's
 * oxidation temperature scales: now and then one piece of the stack is lost, more often the further
 * over it sits ({@link FTuning#HEARTH_SCALE_RATE}).
 *
 * <h2>What a pan does not do</h2>
 * Stock does not cool the pan (a few ingots against {@link FTuning#HEARTH_MASS} would not change
 * its temperature by a figure anyone could read), and stock is not reheated across a gap the chunk
 * was unloaded for; the pan itself is caught up ({@link Catchup}).
 * <p>
 * The Tool Table does not ask the pan anything: the pan reaches into the table's cells, so the
 * table, which is a bench and not a machine, has no thermal code. This is philosophy 5's shape
 * again -- a source publishes how hot it is; whoever sits beside it uses that.
 */
public class HearthBlockEntity extends BlockEntity implements ThermalBody, HeatSource {

    private float temperature = FTuning.AMBIENT_TU.value();
    private final Catchup offline = new Catchup();

    public HearthBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.HEARTH.get(), pos, state);
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
        return FTuning.HEARTH_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.HEARTH_LEAK;
    }

    /** How hot the pan is, as a flame a thing on it would be heated by (a hot plate under a crucible). */
    @Override
    public Tu getFireTu() {
        return getTemperature();
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.at(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanAt(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        setChanged();

        if (temperature > FTuning.WARM_TU.value())
            reheatBeside();
    }

    /**
     * The heat a hot Hearth beside the Anvil at {@code table} holds its stock at: the pan's heat less the gap the item cooling keeps
     * (cooling / {@link FTuning#HEARTH_STOCK_GRIP}), or 0 for none. The forge's heat clock reads this so a part on a kept-hot anvil does not run out of window.
     */
    public static float holdBeside(Level level, BlockPos table) {
        float best = 0f;
        for (Direction side : Direction.Plane.HORIZONTAL)
            if (level.getBlockEntity(table.relative(side)) instanceof HearthBlockEntity hearth && hearth.temperature > FTuning.WARM_TU.value())
                best = Math.max(best, hearth.temperature - FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick() / FTuning.HEARTH_STOCK_GRIP);
        return best;
    }

    private void reheatBeside() {
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        if (materials.isEmpty())
            return;
        for (Direction side : Direction.Plane.HORIZONTAL)
            if (level.getBlockEntity(worldPosition.relative(side)) instanceof PartBenchBlockEntity table
                    && table.getBlockState().getBlock() instanceof StationBlock b && b.station == Station.ANVIL) {
                // The centre slot (TOOL) is the stock the anvil forges; the side stock cells are the rest.
                reheat(table.getItem(PartBenchBlockEntity.TOOL), materials);
                for (int i = 0; i < PartBenchBlockEntity.STOCK_SLOTS; i++)
                    reheat(table.getItem(PartBenchBlockEntity.STOCK + i), materials);
            }
    }

    private void reheat(ItemStack stock, Map<ResourceLocation, Material> materials) {
        if (stock.isEmpty())
            return;
        Optional<Material.Located> what = Material.find(materials, stock);
        if (what.isEmpty() || what.get().material().minTemperature().value() <= 0)
            return;
        float now = ItemHeat.get(stock, level).value();
        if (temperature > now)
            ItemHeat.set(stock, new Tu(now + FTuning.HEARTH_STOCK_GRIP * (temperature - now)), level);
        scale(stock, what.get().material());
    }

    /** Raw stock held over its oxidation temperature loses a piece now and then (a shaped part is left alone). */
    private void scale(ItemStack stock, Material material) {
        if (stock.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get()))
            return;
        var oxidises = material.thermal().oxidationTemperature();
        if (oxidises.isEmpty() || oxidises.get().value() <= 0f)
            return;
        float over = (ItemHeat.get(stock, level).value() - oxidises.get().value()) / oxidises.get().value();
        if (over > 0f && level.random.nextFloat() < FTuning.HEARTH_SCALE_RATE * over)
            stock.shrink(1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        offline.load(tag);
    }
}
