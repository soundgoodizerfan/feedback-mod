// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.hazard.RadiationEmitter;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.heatexchanger.HeatExchangerBlock;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The reactor core (fission spec): the controller block at the top centre of a 7 x 7 x 7 shell. The lattice is the 5 x 5 x 5 cells
 * under it; the walls are the shell's six faces (edges free), any block whose material is dense enough (or a heat exchanger or a
 * boiler built into the lid, which is where the coolant loop leaves). The core never refuses to run: holes in the shell and poor layouts
 * are reported as flaws and felt as dose and hot spots.
 *
 * <h2>How it runs</h2>
 * Once a second the core re-reads the lattice (fuel channels, control assemblies, moderator blocks, the shell) and caches each fuel
 * cell's multiplication from its six neighbours: moderator raises it, live fuel beside it adds a little, absorber next to it is taken off
 * as the rod moves. Each tick it sums the cached figure less what the control rods soak up, turns it into a target power, and lets the
 * real power chase the target through a spring, so it coasts and can overshoot. A hotter core is slightly less reactive. Fission and
 * decay heat go into the core's own temperature ({@link Heat#warm}); a heat exchanger beside it carries heat to a boiler, and the
 * boiler line makes the steam. The core makes heat only.
 *
 * <h2>Stopping and failing</h2>
 * A SCRAM (a trip block in the shell over its temperature, or any control rod set in) ends the chain but decay heat keeps coming,
 * a share of the last power that falls over half an hour. Past {@code REACTOR_FUEL_DAMAGE_TU} (or overpower) fuel loses condition;
 * past the vessel's onset the pressure rises through {@link PressureBurst}, and a burst scatters radioactive slag. Reached only by
 * lost cooling, withdrawn rods or overpower.
 * shortcut: a core in an unloaded chunk makes no new fission (heat and decay catch up, power does not).
 */
public class ReactorCoreBlockEntity extends BlockEntity implements ThermalBody, PressureHolder, RadiationEmitter {

    private float temperature = FTuning.AMBIENT_TU.value();
    private float power;
    private float velocity;
    private float decayWork;
    private long last = Long.MIN_VALUE;
    private int scanIn;

    private final List<FuelChannelBlockEntity> fuel = new ArrayList<>();
    private final List<ControlAssemblyBlockEntity> controls = new ArrayList<>();
    private final List<float[]> controlFuel = new ArrayList<>();
    private final List<String> flaws = new ArrayList<>();
    private float sumBase;
    private int trips;
    private boolean formed;
    private float health = 1f;
    private float meanK;
    private boolean scrammed;

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REACTOR_CORE.get(), pos, state);
    }

    // --- ThermalBody -----------------------------------------------------------------------

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
        return FTuning.FISSION_CORE_MASS;
    }

    // --- readings --------------------------------------------------------------------------

    public float power() {
        return power;
    }

    public float decayWork() {
        return decayWork;
    }

    public int fuelCount() {
        return fuel.size();
    }

    public boolean formed() {
        return formed;
    }

    public List<String> flaws() {
        return flaws;
    }

    public float health() {
        return health;
    }

    public float meanK() {
        return meanK;
    }

    public boolean scrammed() {
        return scrammed;
    }

    public List<ControlAssemblyBlockEntity> controls() {
        return controls;
    }

    /** Pressure the hot core holds: nothing under the onset, rising with temperature over it. */
    @Override
    public Pu getPressure() {
        return new Pu(Math.max(0f, temperature - FTuning.REACTOR_PRESSURE_ONSET_TU) * FTuning.REACTOR_PU_PER_TU);
    }

    /** The dose a running core gives off at point blank: its power plus what its fuel holds is the rods' own business. */
    @Override
    public float radiationRu() {
        return power * FTuning.REACTOR_RUNNING_RU;
    }

    // --- the lattice -----------------------------------------------------------------------

    /** Re-reads the shell and the lattice and caches each fuel cell's multiplication. Once a second, not every tick. */
    private void scan(ServerLevel level) {
        flaws.clear();
        fuel.clear();
        controls.clear();
        controlFuel.clear();
        trips = 0;
        sumBase = 0f;
        int bad = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = FTuning.REACTOR_SHELL_RADIUS;
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -2 * r; dy <= 0; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    int edge = (Math.abs(dx) == r ? 1 : 0) + (Math.abs(dz) == r ? 1 : 0) + (dy == -2 * r || dy == 0 ? 1 : 0);
                    if (edge != 1 || (dx == 0 && dy == 0 && dz == 0))
                        continue;
                    pos.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(FBlocks.SCRAM_TRIP.get())) {
                        trips++;
                        continue;
                    }
                    if (state.getBlock() instanceof HeatExchangerBlock || level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlockEntity)
                        continue;
                    if (Neutrons.density(state) < FTuning.REACTOR_WALL_MIN_DENSITY) {
                        bad++;
                        if (flaws.size() < 8)
                            flaws.add("wall " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                    }
                }
        if (bad > flaws.size())
            flaws.add("and " + (bad - flaws.size()) + " more");
        formed = bad == 0;

        int in = r - 1;
        for (int dx = -in; dx <= in; dx++)
            for (int dy = -2 * r + 1; dy <= -1; dy++)
                for (int dz = -in; dz <= in; dz++) {
                    pos.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof FuelChannelBlockEntity ch && ch.live())
                        fuel.add(ch);
                    else if (be instanceof ControlAssemblyBlockEntity ca)
                        controls.add(ca);
                }
        float healthSum = 0f;
        for (FuelChannelBlockEntity ch : fuel) {
            float k = ch.fissile();
            BlockPos at = ch.getBlockPos();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos n = at.relative(d);
                BlockState ns = level.getBlockState(n);
                if (ns.isAir())
                    continue;
                if (level.getBlockEntity(n) instanceof FuelChannelBlockEntity other) {
                    if (other.live())
                        k += FTuning.REACTOR_FUEL_NEIGHBOUR_GAIN;
                    continue;
                }
                k += Neutrons.of(ns).map(m -> m.moderation() * FTuning.REACTOR_MODERATOR_GAIN).orElse(0f);
            }
            sumBase += k;
            healthSum += ch.condition();
        }
        health = fuel.isEmpty() ? 1f : healthSum / fuel.size();
        for (ControlAssemblyBlockEntity ca : controls) {
            int adjacent = 0;
            for (FuelChannelBlockEntity ch : fuel)
                if (ch.getBlockPos().distManhattan(ca.getBlockPos()) == 1)
                    adjacent++;
            controlFuel.add(new float[]{adjacent});
        }
    }

    // --- tick ------------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        long now = server.getGameTime();
        if (last != Long.MIN_VALUE && now - last > 1) {
            long gap = now - last;
            float tau = FTuning.REACTOR_DECAY_TAU_TICKS;
            float decayed = decayWork * (float) Math.exp(-gap / tau);
            Heat.warm(this, decayWork * tau * (1f - (float) Math.exp(-gap / tau)));
            Tu ambient = AmbientTable.at(server);
            Heat.relax(this, ambient, ambient, false, gap);
            decayWork = decayed;
            power = 0f;
            velocity = 0f;
        }
        last = now;
        if (scanIn-- <= 0) {
            scanIn = 20;
            scan(server);
        }

        // SCRAM: a trip block in the shell over its temperature drops every rod, unpowered.
        if (trips > 0 && temperature > FTuning.REACTOR_TRIP_TU && !scrammed) {
            scrammed = true;
            for (ControlAssemblyBlockEntity ca : controls)
                ca.scram();
        }
        if (scrammed && temperature < FTuning.REACTOR_TRIP_TU - FTuning.REACTOR_TRIP_RESET_TU)
            scrammed = false;

        float absorbed = 0f;
        int f = fuel.size();
        for (int i = 0; i < controls.size(); i++) {
            ControlAssemblyBlockEntity ca = controls.get(i);
            absorbed += ca.strength() * ca.insertion()
                    * (FTuning.REACTOR_ABSORB_ADJACENT_GAIN * controlFuel.get(i)[0] + FTuning.REACTOR_ABSORB_GLOBAL_GAIN * f);
        }
        meanK = f == 0 ? 0f : Math.max(0f, sumBase - absorbed) / f;
        float cycle = Math.max(0f, Math.min(FTuning.REACTOR_POWER_CEILING,
                (meanK - FTuning.REACTOR_K_CRITICAL) / (FTuning.REACTOR_K_FULL - FTuning.REACTOR_K_CRITICAL)));
        float feedback = Math.max(0f, 1f - FTuning.REACTOR_TEMPERATURE_FEEDBACK * Math.max(0f, temperature - FTuning.REACTOR_FEEDBACK_ONSET_TU));
        float target = cycle * feedback * health;
        velocity = (velocity + (target - power) * FTuning.REACTOR_SPRING) * FTuning.REACTOR_DAMP;
        power = Math.max(0f, Math.min(FTuning.REACTOR_POWER_CEILING, power + velocity));
        if (f == 0 || power < 0.0005f) {
            power = f == 0 ? 0f : power;
            if (power < 0.0005f) {
                power = 0f;
                velocity = 0f;
            }
        }

        float work = power * f * FTuning.REACTOR_CELL_WORK;
        decayWork = Math.max(decayWork * (1f - 1f / FTuning.REACTOR_DECAY_TAU_TICKS), FTuning.REACTOR_DECAY_SHARE * work);
        Heat.warm(this, work + decayWork);
        Tu ambient = AmbientTable.at(server);
        Heat.tick(this, ambient, ambient, false);

        // Fuel damage: too hot, or far over power. Condition only falls; the rods never refuse to run.
        float over = Math.max(0f, temperature - FTuning.REACTOR_FUEL_DAMAGE_TU) * FTuning.REACTOR_FUEL_DAMAGE_PER_TU
                + Math.max(0f, power - FTuning.REACTOR_OVERPOWER) * FTuning.REACTOR_OVERPOWER_DAMAGE;
        for (FuelChannelBlockEntity ch : fuel) {
            ch.advance(power);
            ch.damage(over);
        }

        hazard(server);
        if (!formed && power > 0.05f && now % 40 == 0)
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.01);
        setChanged();
    }

    /** Warning, then the burst ladder over the core's own pressure. */
    private void hazard(ServerLevel server) {
        float p = getPressure().value();
        if (p > FTuning.REACTOR_DANGER_PU.value() * FTuning.REACTOR_WARN_FRACTION && server.getGameTime() % 20 == 0) {
            PressureBurst.hiss(server, worldPosition);
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
        }
        if (server.getGameTime() % 10 == 0
                && PressureBurst.fails(new Pu(p), FTuning.REACTOR_DANGER_PU, FTuning.REACTOR_CRITICAL_PU, server.random))
            breach(server);
    }

    /** The vessel lets go: a steam blast, then slag around the hole that stays radioactive. */
    private void breach(ServerLevel server) {
        BlockPos pos = worldPosition;
        PressureBurst.burst(server, pos, FTuning.REACTOR_BURST_POWER);
        server.setBlock(pos, FBlocks.CORE_SLAG.get().defaultBlockState(), 3);
        if (server.getBlockEntity(pos) instanceof CoreSlagBlockEntity slag)
            slag.stamp(FTuning.REACTOR_SLAG_RU, server.getGameTime());
        int reach = FTuning.REACTOR_SLAG_REACH;
        for (int i = 0; i < FTuning.REACTOR_SLAG_BLOCKS; i++) {
            BlockPos at = pos.offset(server.random.nextIntBetweenInclusive(-reach, reach),
                    server.random.nextIntBetweenInclusive(-reach * 2, 0), server.random.nextIntBetweenInclusive(-reach, reach));
            BlockState here = server.getBlockState(at);
            if (here.isAir() || here.canBeReplaced()) {
                server.setBlock(at, FBlocks.CORE_SLAG.get().defaultBlockState(), 3);
                if (server.getBlockEntity(at) instanceof CoreSlagBlockEntity s)
                    s.stamp(FTuning.REACTOR_SLAG_RU, server.getGameTime());
            }
        }
    }

    /** What the player reads with an empty hand. */
    public Component describe() {
        return Component.translatable("message.feedback.reactor_core", Math.round(power * 100f), Math.round(temperature), fuel.size(),
                formed ? Component.translatable("message.feedback.reactor_core.sealed")
                        : Component.translatable("message.feedback.reactor_core.holes", flaws.size()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Power", power);
        tag.putFloat("Velocity", velocity);
        tag.putFloat("Decay", decayWork);
        tag.putLong("Last", last);
        tag.putBoolean("Scrammed", scrammed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        power = tag.getFloat("Power");
        velocity = tag.getFloat("Velocity");
        decayWork = tag.getFloat("Decay");
        last = tag.contains("Last") ? tag.getLong("Last") : Long.MIN_VALUE;
        scrammed = tag.getBoolean("Scrammed");
    }
}
