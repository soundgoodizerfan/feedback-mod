// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.capacitor;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds charge in Eu at a rated band and gives it back in a burst, and leaks. The storage
 * that makes a pulse usable: an electrostatic machine's twenty ticks go into the jar, and come out
 * as long as something draws on it.
 *
 * <h2>Rated a band, storing Eu</h2>
 * The jar's glass sets the band it is rated, and its plate area over wall thickness the packets' worth of Eu it holds
 * ({@code StoresEu}). It charges when its band or a higher one is fed to it and never from a lower one; a band over its own simply
 * fills it. What it holds is {@code charge}, in Eu, an internal quantity the jar keeps: a reading of it is how full it is.
 *
 * <h2>Reserve</h2>
 * Once it holds charge on a line at its band or lower the jar is a source marked {@code reserve}: beside a same-band generator it
 * supplies only what generation cannot carry and charges from any surplus. Only a band over its own keeps it a sink.
 *
 * <h2>One role at a time, and it changes</h2>
 * {@code ElectricMember} gives a node exactly one role, so the jar is a <em>sink</em> while its band (or more) is on its run,
 * charging at a fixed current until it is full, and a <em>source</em> once nothing on the run holds it up, offering its band at a
 * large current. It flips when the supply goes (as a sink it sees no packet at its band) or when it is outbid (as a source it is
 * blocked, the resolver's own word for it), and an empty jar always waits as a sink. Role decides which neighbours conduct to it,
 * so a flip rebuilds its run -- an event, not a per-tick cost.
 * <p>
 * It gives back packets of its band until the Eu run out, the last of them fractional: a burst, not a battery. It never refuses. A band over its rating punctures the glass: the jar discharges at once as a shock at its band to
 * anything within {@link ElectricMachineTuning#LEYDEN_JAR_CRACK_RADIUS} blocks and is destroyed (a cheap part may pop).
 */
public class LeydenJarBlockEntity extends ElectricNode {

    private static final String CHARGE = "Charge", SUPPLYING = "Supplying";

    private float charge;
    private boolean supplying;

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

    /** The band the jar is rated. */
    public VoltageBand band() {
        VoltageBand[] all = VoltageBand.values();
        return all[Math.max(0, Math.min(all.length - 1, Math.round(figures.get("Band", ElectricMachineTuning.LEYDEN_JAR_BAND.ordinal()))))];
    }

    /** Eu the jar holds when full. */
    public float capacity() {
        return figures.get("StoresEu", ElectricMachineTuning.LEYDEN_JAR_CAPACITY_PACKETS * ElectricRules.packetEu(band()));
    }

    public LeydenJarBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LEYDEN_JAR.get(), pos, state);
    }

    /** Eu held. */
    public float getStoredEu() {
        return charge;
    }

    /** How full it is, 0 to 1. */
    public float fill() {
        return Math.min(1f, charge / capacity());
    }

    /**
     * A pulse put in by hand (a piezo part's blow): {@code eu} of charge, never taking the jar past {@code capShare} of what it holds.
     * A hand is a tiny source, so what it adds is small and what it can reach is capped low.
     */
    public void inject(float eu, float capShare) {
        charge = Math.max(charge, Math.min(charge + eu, capShare * capacity()));
        setChanged();
    }

    /** Empties the jar and returns the Eu it held. */
    public float drain() {
        float held = charge;
        charge = 0;
        setChanged();
        return held;
    }

    /** A charged jar's terminals are bare: once it is rated MV or more, touching it is a shock. */
    @Override
    public VoltageBand touchBand() {
        return charge > ElectricMachineTuning.LEYDEN_JAR_EMPTY_EU && band().compareTo(ElectricMachineTuning.SHOCK_MIN_BAND) >= 0 ? band() : null;
    }

    public boolean isSupplying() {
        return supplying;
    }

    @Override
    public Role role() {
        return supplying ? Role.SOURCE : Role.SINK;
    }

    @Override
    public boolean reserve() {
        return supplying;
    }

    @Override
    public float reserveWantedAmps() {
        return charge < capacity() ? ElectricMachineTuning.LEYDEN_JAR_CHARGE_AMPS : 0;
    }

    @Override
    public VoltageBand sourceBand() {
        return supplying ? band() : null;
    }

    @Override
    public float sourceAmps() {
        return supplying && charge > 0 ? Math.min(figures.get("BurstAmps", ElectricMachineTuning.LEYDEN_JAR_BURST_AMPS),
                charge / ElectricRules.packetEu(band())) : 0;
    }

    /** Charges only while its band or more is fed to it; a full jar asks for nothing. */
    @Override
    public float wantedAmps() {
        return !supplying && charge < capacity() && ElectricRules.accepts(getBand(), getPacketEu(), band())
                ? ElectricMachineTuning.LEYDEN_JAR_CHARGE_AMPS : 0;
    }

    public void tickServer() {
        if (level == null)
            return;
        // Fed a band over what its glass is rated, the glass punctures: all it holds goes at once to whoever is near, and the jar is gone.
        if (getPacketEu() > 0f && getBand().compareTo(band()) > 0) {
            float held = charge;
            for (net.minecraft.world.entity.LivingEntity near : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(worldPosition).inflate(ElectricMachineTuning.LEYDEN_JAR_CRACK_RADIUS)))
                io.github.soundgoodizerfan.feedback.core.hazard.Shock.apply(near, band(), held);
            level.destroyBlock(worldPosition, false);
            return;
        }
        // Last tick's resolution: packets delivered in as a sink, drawn out as a source.
        float before = charge;
        charge += (supplying ? -getAmps() : getAmps()) * getPacketEu();
        charge = Math.max(0, Math.min(charge, capacity()) * (1 - figures.get("Leak", ElectricMachineTuning.LEYDEN_JAR_LEAK_PER_TICK)));
        if (charge != before)
            setChanged();

        boolean flip = supplying
                ? charge <= ElectricMachineTuning.LEYDEN_JAR_EMPTY_EU || isBlocked()
                : charge > ElectricMachineTuning.LEYDEN_JAR_EMPTY_EU && !(getPacketEu() > 0f && getBand().compareTo(band()) > 0);
        if (flip) {
            supplying = !supplying;
            setChanged();
            ElectricPropagator.rebuildFrom(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(CHARGE, charge);
        figures.save(tag, registries);
        tag.putBoolean(SUPPLYING, supplying);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        charge = tag.getFloat(CHARGE);
        supplying = tag.getBoolean(SUPPLYING);
    }
}
