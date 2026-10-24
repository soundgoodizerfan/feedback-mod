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
package io.github.soundgoodizerfan.feedback.machine.detector;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wire probing a crystal for a rectifying spot. The crystal has {@link FTuning#WHISKER_SPOTS}
 * spots; each answers sparks with a sensitivity of the crystal's own {@code electrical.rectifying}
 * times a random share fixed when it was seated. The whisker rests on one spot. A spark inside
 * {@code range x sensitivity} closes the line for {@link FTuning#WHISKER_HOLD_TICKS} ticks and then
 * it opens again by itself: a momentary detector, where the coherer latches.
 *
 * <h2>The hunt</h2>
 * The player steps the whisker round the spots and is told only a noisy band for each, so the best
 * spot is found by sampling and comparing, not read off. A better spot hears further and so also
 * hears every stray arc further; nothing is free. The crystal is picked by the property, not the
 * id: any material with {@code rectifying} works.
 *
 * <h2>Overrun</h2>
 * Current through the closed contact past {@link FTuning#WHISKER_FUSE_AMPS} fuses the spot the
 * whisker rests on: it hears nothing from then on, and the line opens. The other spots are
 * untouched; re-seating the crystal draws fresh ones. Wear only from that misuse.
 *
 * <h2>A circuit on the contact</h2>
 * Seat a Basic Electronic Circuit and the whisker reads its figures ({@code process/Circuit}), none of
 * which gates anything: <b>noise</b> multiplies the spread of the hunt band (a quiet stage makes the
 * best spot easier to tell), <b>elements</b> add reach per element (and hear stray arcs further),
 * <b>speed</b> shortens the hold (a crisper pulse). Over its trace rating the closed contact fuses an
 * element now and then, the last making it a dead part; a printed board's rating is under the
 * whisker's own fuse, an etched one's is over it, so only a printed stage is at risk.
 *
 * <h2>Sense and stop</h2>
 * Sense: a spark. Stop: it opens by itself when the hold runs out; nothing else is needed.
 */
public class WhiskerBlockEntity extends SparkDetectorBlockEntity implements io.github.soundgoodizerfan.feedback.instrument.Testable {

    private ItemStack crystal = ItemStack.EMPTY;
    /** An optional circuit on the contact (phase 4): conditions the line. The detector works without one, just worse. */
    private ItemStack stage = ItemStack.EMPTY;
    private int seed;
    private int spot;
    private int fused;
    private int hold;

    public WhiskerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.WHISKER.get(), pos, state);
    }

    /** The material's rectifying figure for {@code stack}, or 0 if it is not a rectifying crystal. */
    public static float rectifying(ItemStack stack) {
        return Material.find(MaterialTable.get().materials(), stack.getItem())
                .map(f -> f.material().electrical().rectifying().orElse(0f)).orElse(0f);
    }

    public Circuit stage() {
        return Circuit.of(stage);
    }

    public boolean hasStage() {
        return !stage.isEmpty();
    }

    public void fit(ItemStack one) {
        stage = one.copyWithCount(1);
        if (level != null)
            Circuit.scorch(stage, level);
        setChanged();
    }

    public ItemStack unfit() {
        ItemStack out = stage;
        stage = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    /** The spread of the hunt band: the base, times the stage's noise if one is fitted. */
    public float bandSpread() {
        return FTuning.WHISKER_BAND_SPREAD * (stage().live() ? stage().noise() : 1f);
    }

    private float reach() {
        return 1f + (stage().live() ? FTuning.WHISKER_STAGE_GAIN * stage().elements() : 0f);
    }

    private int holdTicks() {
        return stage().live() ? Math.max(2, Math.round(FTuning.WHISKER_HOLD_TICKS * (1f - FTuning.WHISKER_STAGE_SNAP * stage().speed())))
                : FTuning.WHISKER_HOLD_TICKS;
    }

    public boolean hasCrystal() {
        return !crystal.isEmpty();
    }

    public void seat(ItemStack one) {
        crystal = one.copyWithCount(1);
        seed = level == null ? 1 : level.random.nextInt();
        spot = 0;
        fused = 0;
        setClosed(false);
        setChanged();
    }

    public ItemStack unseat() {
        ItemStack out = crystal;
        crystal = ItemStack.EMPTY;
        setClosed(false);
        setChanged();
        return out;
    }

    public int step() {
        spot = (spot + 1) % FTuning.WHISKER_SPOTS;
        setChanged();
        return spot;
    }

    public boolean spotFused() {
        return (fused >> spot & 1) != 0;
    }

    /** Sensitivity of the spot the whisker rests on, 0-1: the crystal's figure times this spot's share. */
    public float sensitivity() {
        if (crystal.isEmpty() || spotFused())
            return 0f;
        int h = (seed + spot * 0x9E3779B1) * 0x85EBCA6B;
        h ^= h >>> 15;
        return rectifying(crystal) * ((h & 0xFFFF) / 65535f);
    }


    @Override
    public void test(io.github.soundgoodizerfan.feedback.instrument.Instrument tester, java.util.function.Consumer<net.minecraft.network.chat.Component> out) {
        if (crystal.isEmpty())
            out.accept(net.minecraft.network.chat.Component.translatable("feedback.whisker.no_crystal"));
        else if (spotFused())
            out.accept(net.minecraft.network.chat.Component.translatable("feedback.whisker.burnt"));
        else
            out.accept(io.github.soundgoodizerfan.feedback.instrument.Testable.response(tester, sensitivity()));
        if (hasStage())
            stage().test(tester, out);
    }

    @Override
    public boolean conducting() {
        return hold > 0;
    }

    @Override
    protected void hear(double distance) {
        float s = sensitivity();
        if (s < FTuning.WHISKER_MIN_SENSITIVITY || distance > FTuning.SPARK_RANGE_BLOCKS * s * reach())
            return;
        boolean was = hold > 0;
        hold = holdTicks();
        if (!was)
            reconnectNow();
    }

    public void tickServer() {
        if (hold > 0 && --hold == 0)
            reconnectNow();
    }

    private void setClosed(boolean closed) {
        boolean was = hold > 0;
        hold = closed ? holdTicks() : 0;
        if (was != closed)
            reconnectNow();
    }

    private void reconnectNow() {
        if (level != null && !level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (hold > 0 && amps > FTuning.WHISKER_FUSE_AMPS) {
            fused |= 1 << spot;
            setChanged();
            setClosed(false);
        }
        Circuit c = stage();
        if (c.live() && hold > 0 && amps > c.ratingAmps() && level.random.nextFloat() < FTuning.CIRCUIT_FUSE_CHANCE) {
            stage.set(FDataComponents.CIRCUIT.get(), c.fused());
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!crystal.isEmpty())
            tag.put("Crystal", crystal.save(registries));
        if (!stage.isEmpty())
            tag.put("Stage", stage.save(registries));
        tag.putInt("Seed", seed);
        tag.putInt("Spot", spot);
        tag.putInt("Fused", fused);
        tag.putInt("Hold", hold);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crystal = tag.contains("Crystal") ? ItemStack.parseOptional(registries, tag.getCompound("Crystal")) : ItemStack.EMPTY;
        stage = tag.contains("Stage") ? ItemStack.parseOptional(registries, tag.getCompound("Stage")) : ItemStack.EMPTY;
        seed = tag.getInt("Seed");
        spot = tag.getInt("Spot");
        fused = tag.getInt("Fused");
        hold = tag.getInt("Hold");
    }
}
