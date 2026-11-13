// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.breaker;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.machine.Weldable;
import io.github.soundgoodizerfan.feedback.machine.cable.CableGauge;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electricity's stop (philosophy 11): it cuts nearly instantly, and the contacts arc and wear
 * when they open under load. A wire in the run while closed; open, it joins nothing, and the line
 * through it is two runs.
 *
 * <h2>Wear from one act</h2>
 * Opening while current flows draws an arc, and the arc pits the contacts: condition falls by the
 * current broken, never by time closed. Pitted contacts lose more volts per amp, so a breaker
 * flapped under load by a controller with no deadband (philosophy 14) shows it as a sag on the
 * far side -- sloppy logic destroying downstream hardware, by a gradient. Never broken; it keeps
 * switching at its floor.
 *
 * <h2>It trips</h2>
 * Over {@link ElectricMachineTuning#BREAKER_TRIP_AMPS} times its gauge's area -- the rating of the copper cable
 * it was built from, the thing it is there to protect -- it opens itself and stays open: that is an opening under load too, and it is charged
 * like one. A hand or a controller closes it again; closing it back onto the fault trips it again,
 * and wears it again.
 */
public class BreakerBlockEntity extends ElectricNode implements Switchable, Weldable {

    private boolean closed = true;
    private float condition = 1f;
    private float maxCondition = 1f;
    /** The cable gauge it was built from: contacts as thick as that cable, so it trips at that cable's rating. */
    private CableGauge gauge = CableGauge.STANDARD;

    public BreakerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BREAKER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.WIRE;
    }

    public CableGauge gauge() {
        return gauge;
    }

    /** Placing the item takes the gauge from the stack. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        gauge = input.getOrDefault(FDataComponents.CABLE_GAUGE.get(), CableGauge.STANDARD);
    }

    /** Breaking it, or picking it, keeps the gauge on the stack. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (gauge != CableGauge.STANDARD)
            components.set(FDataComponents.CABLE_GAUGE.get(), gauge);
    }

    @Override
    public boolean conducting() {
        return closed;
    }

    @Override
    public float dropPerAmp() {
        return ElectricMachineTuning.BREAKER_DROP_PER_AMP / gauge.area() / condition;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        float trip = ElectricMachineTuning.BREAKER_TRIP_AMPS * gauge.area();
        if (closed && amps > trip)
            setEngaged(false);
        else if (closed && level != null && !level.isClientSide && volts >= ElectricMachineTuning.BREAKER_FLASH_MIN_BAND_VOLTS
                && amps > trip * ElectricMachineTuning.BREAKER_FLASH_WARN_LOAD && level.getGameTime() % ElectricMachineTuning.BREAKER_FLASH_WARN_TICKS == 0)
            warn();
    }

    /** A joint nearing its limit at line voltage hums and sparks before it lets go (grid spec 4.1). */
    private void warn() {
        var level = this.level;
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BEEHIVE_WORK, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 0.5f);
        ((net.minecraft.server.level.ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
    }

    /** Opening under heavy current at line voltage throws an arc: it burns what stands close and is not insulated from it. */
    private void flash(float volts, float amps) {
        float damage = Math.min(ElectricMachineTuning.BREAKER_FLASH_DAMAGE_CAP, amps * ElectricMachineTuning.BREAKER_FLASH_DAMAGE_PER_AMP);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.6f);
        ((net.minecraft.server.level.ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1, 0, 0, 0, 0);
        for (var victim : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(ElectricMachineTuning.BREAKER_FLASH_RADIUS))) {
            if (victim instanceof net.minecraft.world.entity.player.Player player
                    && io.github.soundgoodizerfan.feedback.core.hazard.Protection.shockProofVolts(player) >= volts)
                continue;
            if (victim.hurt(io.github.soundgoodizerfan.feedback.core.hazard.Hazards.source(level,
                    io.github.soundgoodizerfan.feedback.core.hazard.Hazards.HOT_CONTACT), damage))
                victim.igniteForTicks(ElectricMachineTuning.BREAKER_FLASH_FIRE_TICKS);
        }
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (engaged == closed || level == null || level.isClientSide)
            return;
        if (!engaged && getAmps() > 0f)
            io.github.soundgoodizerfan.feedback.core.electric.Sparks.emit(level, worldPosition, getAmps());
        if (!engaged && getAmps() > 0f && getVolts() >= ElectricMachineTuning.BREAKER_FLASH_MIN_BAND_VOLTS)
            flash(getVolts(), getAmps());
        if (!engaged && getAmps() > 0f)
            condition = Math.max(ElectricMachineTuning.BREAKER_CONDITION_FLOOR, condition - ElectricMachineTuning.BREAKER_ARC_WEAR_PER_AMP * getAmps());
        closed = engaged;
        setChanged();
        ElectricPropagator.reconnect(level, worldPosition);
    }

    @Override
    public boolean isEngaged() {
        return closed;
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public float maxCondition() {
        return maxCondition;
    }

    @Override
    public void weld(float newCondition, float newMax) {
        condition = newCondition;
        maxCondition = newMax;
        setChanged();
    }

    @Override
    public boolean isRunning() {
        return closed && getAmps() > 0f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Closed", closed);
        tag.putFloat("Condition", condition);
        tag.putFloat("MaxCondition", maxCondition);
        tag.putString("Gauge", gauge.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        closed = !tag.contains("Closed") || tag.getBoolean("Closed");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        maxCondition = tag.contains("MaxCondition") ? tag.getFloat("MaxCondition") : 1f;
        gauge = CableGauge.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(tag.getString("Gauge"))).result().orElse(CableGauge.STANDARD);
    }
}
