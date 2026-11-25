// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.optical;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.control.data.Amplifying;
import io.github.soundgoodizerfan.feedback.control.data.OpticalEnd;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.CircuitSlot;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One end of a fibre link: the light source and detector. A crystal seated in it (any item whose material has a {@code rectifying} figure)
 * is the emitter and the detector, and sets how much loss the line may carry; a circuit seated in it gives gain, which lengthens that
 * clean reach as it does at a Data Port. Nodes join it with ordinary wire; two terminals are joined only by fibre
 * ({@code DataLinkManager}). It is a sink on an LV line drawing a small standing current while a crystal is seated; with no power or no
 * crystal the fibre it ends is dark. A band over its rating burns the crystal out (a cheap, consumable part): the line goes dark until a
 * new one is seated, and the housing is never harmed.
 */
public class OpticalTerminalBlockEntity extends ElectricNode implements OpticalEnd, Amplifying {

    private ItemStack crystal = ItemStack.EMPTY;
    private final CircuitSlot circuit = new CircuitSlot();

    public OpticalTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.OPTICAL_TERMINAL.get(), pos, state);
    }

    // --- sink -----------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return crystal.isEmpty() ? 0f : ControlTuning.OPTICAL_TERMINAL_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    // --- the crystal and the circuit ------------------------------------------------------------

    /** The {@code rectifying} figure of the material of {@code stack}, 0 when it has none (so no crystal). */
    public static float rectifying(ItemStack stack) {
        if (stack.isEmpty())
            return 0f;
        return Material.find(MaterialTable.get().materials(), stack)
                .flatMap(l -> l.material().electrical().rectifying()).orElse(0f);
    }

    /** Whether a hand's stack can be seated as the crystal. */
    public static boolean isCrystal(ItemStack stack) {
        return rectifying(stack) > 0f;
    }

    /** Seats one of {@code in} as the crystal; the one it replaces comes back. */
    public ItemStack seatCrystal(ItemStack in) {
        ItemStack old = crystal;
        crystal = in.split(1);
        setChanged();
        return old;
    }

    public ItemStack takeCrystal() {
        ItemStack old = crystal;
        crystal = ItemStack.EMPTY;
        setChanged();
        return old;
    }

    public ItemStack crystal() {
        return crystal;
    }

    public CircuitSlot circuit() {
        return circuit;
    }

    // --- the link ---------------------------------------------------------------------------------

    @Override
    public boolean lit() {
        return !crystal.isEmpty() && !isRemoved() && ElectricRules.rate(getPacketEu(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    @Override
    public float budgetShare() {
        float r = rectifying(crystal);
        return r <= 0f ? 0f : ControlTuning.FIBRE_CRYSTAL_FLOOR + ControlTuning.FIBRE_CRYSTAL_SLOPE * Math.min(1f, r);
    }

    @Override
    public float gain() {
        return level == null ? 0f : circuit.gain(level);
    }

    @Override
    public float amplifierGain() {
        return gain();
    }

    /** Burns the crystal out when the line feeds a band over the rating: misuse, never uptime. */
    public void tickServer() {
        if (level == null || crystal.isEmpty() || getBand().compareTo(ratedBand()) <= 0)
            return;
        crystal = ItemStack.EMPTY;
        level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6f, 1.4f);
        setChanged();
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!crystal.isEmpty())
            tag.put("Crystal", crystal.save(registries));
        circuit.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crystal = tag.contains("Crystal") ? ItemStack.parseOptional(registries, tag.getCompound("Crystal")) : ItemStack.EMPTY;
        circuit.load(tag, registries);
    }
}
