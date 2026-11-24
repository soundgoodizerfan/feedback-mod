// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import io.github.soundgoodizerfan.feedback.core.hazard.Corrosion;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.item.ModularCaster;
import io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.net.AimPayloads;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.CasterTuning;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.FluidStream;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;

/**
 * A fluid caster's mechanism. The valve is a switch: held left click is a fire-mode AUTO pull every tick, each one a tick of flow
 * ({@link FluidStream}); a pull with nothing to throw is the dry click. Two correct drives: a <b>pressurised tank</b> (the gauge
 * pressure is what throws the stream, and it falls as the tank empties: the gas has more room) and a <b>pump</b> (a hand pump's stroke
 * primes the stream for a while at a steady head, whatever the tank's gauge says). The reload key is the stroke of whichever it has:
 * a pump's, or the tank's own air pump, which raises the gauge. Everything that goes wrong is misuse: a stroke over what the walls hold
 * weeps, then lets go ({@link PressureBurst}); a pump stroked dry wears; a wrong fluid eats seals and walls; a burning volatile stream
 * through a wide nozzle flashes back into the tank.
 */
public final class CasterMechanism implements GunMechanism {

    public static final CasterMechanism INSTANCE = new CasterMechanism();

    private CasterMechanism() {
    }

    /** The next tick of flow, worked out and not yet spent. */
    private record Prepared(CasterSpec spec, FluidStack fluid, float pressure, float speed, int flowMb, GunFigures figures, float eu) {
    }

    /** {@code predicting}: the client asking whether a pull would throw anything; a motor is taken at its full head then, since the run-up starts with the pull the server gets. */
    private static Prepared prepare(ItemStack gun, long now, boolean predicting) {
        CasterSpec spec = ModularCaster.spec(gun).orElse(null);
        if (spec == null || ModularStacks.broken(gun))
            return null;
        FluidStack fluid = ModularCaster.content(gun);
        if (fluid.isEmpty())
            return null;
        float pressure = spec.electric() ? motorHead(spec, gun, now, predicting)
                : spec.pump() ? (now < ModularCaster.primeUntil(gun) ? spec.pumpPu() : 0f) : ModularCaster.pressure(gun);
        Material m = FluidStream.materialOf(fluid).orElse(null);
        float speed = spec.speed(pressure, m == null ? 1f : (float) m.density().muPerMb());
        if (speed <= 0f)
            return null;
        GunFigures figures = GunItem.figures(gun);
        if (figures == null)
            return null;
        // A worn tank leaks a little of its push; the condition is the share the walls keep.
        speed *= GunCondition.condition(gun);
        int flow = Math.min(fluid.getAmount(), Math.max(1, Math.round(spec.flow(speed))));
        // A motor pays for the work it does: the mB it moves against the head it holds. A cell that cannot pay this tick gives the dry click.
        float eu = spec.electric() ? flow * pressure * CasterTuning.EU_PER_MB_PU : 0f;
        if (eu > 0f && BatteryStack.charge(BatteryStack.state(ModularElectricGun.cell(gun))) < eu) // read only when the motor is running
            return null;
        return new Prepared(spec, fluid, pressure, speed, flow, figures, eu);
    }

    /** An electric pump's head now: its full head scaled by how far the motor has run up, nothing without a charged cell. */
    private static float motorHead(CasterSpec spec, ItemStack gun, long now, boolean predicting) {
        ItemStack cell = ModularElectricGun.cell(gun);
        if (cell.isEmpty() || !BatteryStack.is(cell) || !BatteryStack.ready(BatteryStack.state(cell)) || BatteryStack.charge(BatteryStack.state(cell)) <= 0f)
            return 0f;
        return spec.pumpPu() * (predicting ? 1f : ModularCaster.spin(gun, now));
    }

    @Override
    public boolean built(ItemStack gun) {
        return ModularCaster.spec(gun).isPresent();
    }

    @Override
    public java.util.List<io.github.soundgoodizerfan.feedback.weapon.GunSwitch> switches(ItemStack gun) {
        CasterSpec spec = ModularCaster.spec(gun).orElse(null);
        java.util.List<io.github.soundgoodizerfan.feedback.weapon.GunSwitch> out = new java.util.ArrayList<>();
        if (spec != null && spec.piloted())
            out.add(io.github.soundgoodizerfan.feedback.weapon.GunSwitch.twoState("pilot", "out", "lit", ModularCaster.pilotOn(gun)));
        out.addAll(GunMechanism.super.switches(gun));
        return out;
    }

    @Override
    public void setSwitch(ItemStack gun, String id, int state, long now) {
        if ("pilot".equals(id))
            ModularCaster.setPilot(gun, state == 1);
        else
            GunMechanism.super.setSwitch(gun, id, state, now);
    }

    @Override
    public boolean trigger(ServerLevel level, Player shooter, ItemStack gun, float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        CasterSpec drive = ModularCaster.spec(gun).orElse(null);
        if (drive != null && drive.electric()) {
            // The motor runs up from the first pull; one with nothing to move wears (and costs no charge).
            ModularCaster.touchSpin(gun, level.getGameTime());
            if (ModularCaster.content(gun).isEmpty() && motorHead(drive, gun, level.getGameTime(), true) > 0f)
                GunCondition.misuse(gun, CasterTuning.WEAR_DRY_MOTOR);
        }
        Prepared p = prepare(gun, level.getGameTime(), false);
        if (p == null)
            return false;
        if (!GunShots.accept(shooter, p.figures(), nowMs, hand))
            return false;
        FluidStack before = p.fluid();
        if (p.eu() > 0f) {
            ItemStack cell = ModularElectricGun.cell(gun);
            var drawn = BatteryStack.state(cell).copy();
            BatteryStack.draw(drawn, p.eu());
            ItemStack out = cell.copy();
            out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BATTERY.get(), drawn);
            ModularElectricGun.setCell(gun, out);
        }
        // The tank gives up the tick's flow; a pressurised tank's gas has more room, so its pressure falls (isothermal).
        FluidStack left = before.copy();
        left.shrink(p.flowMb());
        ModularCaster.setContent(gun, left);
        if (!p.spec().pump()) {
            float room = Math.max(p.spec().capacityMb() - before.getAmount(), CasterTuning.HEADSPACE_FLOOR * p.spec().capacityMb());
            ModularCaster.setPressure(gun, p.pressure() * room / (room + p.flowMb()));
        }
        FluidStack streamed = before.copyWithAmount(p.flowMb());
        FluidStream.Result r = FluidStream.cast(level, shooter, yaw, pitch, streamed, p.flowMb(), p.speed(), p.spec().reach(p.speed()),
                p.spec().spreadDeg(), p.spec().atomiser(), ModularCaster.pilotOn(gun) ? p.spec().pilotReach() : 0f);
        // The pilot burns a little of the tank's fluid while lit, and a fluid hotter than its metal takes wears it.
        if (r.pilotLit()) {
            if (level.random.nextFloat() < p.spec().pilotBleed()) {
                FluidStack held = ModularCaster.content(gun);
                if (!held.isEmpty()) {
                    held.shrink(1);
                    ModularCaster.setContent(gun, held);
                }
            }
            if (FluidHeat.of(before).value() > p.spec().pilotServiceTu())
                GunCondition.misuse(gun, CasterTuning.WEAR_PILOT_HOT);
        }
        // Thrust: mass flow times exit speed, a steady push on the wielder against the stream.
        Material m = FluidStream.materialOf(before).orElse(null);
        float density = m == null ? 1f : (float) m.density().muPerMb();
        float push = Math.min(CasterTuning.SHOVE_MAX, p.flowMb() * density * p.speed() * CasterTuning.SHOVE_PER_MOMENTUM * 0.5f);
        Vec3 back = Vec3.directionFromRotation(pitch, yaw).scale(-push);
        shooter.push(back.x, back.y * 0.5, back.z);
        shooter.hurtMarked = true;
        // A burning volatile stream through a wide bore burns back along itself into the tank.
        if (r.ignited() && r.igniteBlocks() <= CasterTuning.FLASHBACK_REACH && p.spec().boreMm() >= CasterTuning.FLASHBACK_BORE_MM
                && volatileFluid(before) && (!r.pilotLit() || level.random.nextFloat() < CasterTuning.PILOT_FLASHBACK_CHANCE))
            flashback(level, shooter, gun, p.spec());
        // A fluid the parts cannot take bites them as it passes.
        if (Corrosion.corrosive(before))
            GunCondition.misuse(gun, Corrosion.wearPerSecond(p.spec().weakestResist()) / 20f);
        return true;
    }

    /** Whether the fluid gives off vapour at its own temperature (it is over its flash point): a burning stream of it can flash back. */
    private static boolean volatileFluid(FluidStack fluid) {
        return io.github.soundgoodizerfan.feedback.core.hazard.Vapour.potency(fluid) > 0f;
    }

    /** The flame runs back up the stream into the tank: the wielder is burnt, the tank half empties, the gun wears. */
    private static void flashback(ServerLevel level, Player shooter, ItemStack gun, CasterSpec spec) {
        FluidStack held = ModularCaster.content(gun);
        held.shrink(held.getAmount() / 2);
        ModularCaster.setContent(gun, held);
        ModularCaster.setPressure(gun, 0f);
        GunCondition.misuse(gun, CasterTuning.WEAR_FLASHBACK);
        shooter.hurt(level.damageSources().inFire(), CasterTuning.FLASHBACK_HP);
        shooter.igniteForSeconds(CasterTuning.IGNITE_SECONDS);
        level.sendParticles(ParticleTypes.FLAME, shooter.getX(), shooter.getEyeY() - 0.3, shooter.getZ(), 24, 0.3, 0.2, 0.3, 0.05);
        level.playSound(null, shooter.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
        AimPayloads.cue(shooter, AimPayloads.GUN, net.minecraft.network.chat.Component.translatable("message.feedback.caster.flashback"), 50);
    }

    @Override
    public boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        return false;
    }

    @Override
    public GunFigures predict(Player player, ItemStack gun, Level level, Map<ResourceLocation, Material> materials, int hand) {
        Prepared p = prepare(gun, level.getGameTime(), true);
        return p == null ? null : p.figures();
    }

    @Override
    public float sightZoom(ItemStack gun) {
        return 1f;
    }

    @Override
    public int raiseMs(ItemStack gun) {
        return 150;
    }

    /** The valve is a switch: held means open, so the only mode is automatic. */
    @Override
    public FireMode allow(ItemStack gun, FireMode set) {
        return FireMode.AUTO;
    }

    @Override
    public FireMode next(ItemStack gun, FireMode current) {
        return FireMode.AUTO;
    }

    /** A hiss, not one every tick: about one in three pulls speaks, quieter than a shot. */
    @Override
    public List<GunSound.Layer> sound(ItemStack gun, GunFigures figures, RandomSource random) {
        if (random.nextInt(3) != 0)
            return List.of();
        return List.of(new GunSound.Layer(FSounds.GUN_VENT.get(), 0.35f, 1.3f + random.nextFloat() * 0.2f));
    }

    @Override
    public SimpleParticleType muzzleParticle(ItemStack gun, GunFigures figures) {
        return null;
    }

    // --- the reload key: the pump stroke -------------------------------------------------------

    @Override
    public boolean reloadKeyWorks(ItemStack gun) {
        return !ModularCaster.spec(gun).map(CasterSpec::electric).orElse(false);
    }

    /** A hand pump's stroke primes the stream; with no pump fitted it is the tank's own air pump, which raises the gauge. */
    @Override
    public void reloadKey(ServerLevel level, Player player, ItemStack gun) {
        CasterSpec spec = ModularCaster.spec(gun).orElse(null);
        if (spec == null || ModularStacks.broken(gun) || spec.electric())
            return;
        long now = level.getGameTime();
        FluidStack held = ModularCaster.content(gun);
        if (spec.pump()) {
            // A pump with nothing to move runs dry: it heats and wears.
            if (held.isEmpty()) {
                GunCondition.misuse(gun, CasterTuning.WEAR_DRY_PUMP);
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY() - 0.4, player.getZ(), 3, 0.1, 0.05, 0.1, 0.01);
                level.playSound(null, player.blockPosition(), FSounds.GUN_EGUN_FLAT.get(), SoundSource.PLAYERS, 0.6f, 0.9f);
                return;
            }
            long until = Math.min(now + Math.round(CasterTuning.PRIME_MAX_TICKS), Math.max(now, ModularCaster.primeUntil(gun)) + Math.round(spec.strokeTicks()));
            ModularCaster.setPrimeUntil(gun, until);
        } else {
            // The tank's own air pump: each stroke adds to the gauge, faster the less room the gas has left.
            float room = Math.max(1f - held.getAmount() / spec.capacityMb(), CasterTuning.HEADSPACE_FLOOR);
            ModularCaster.setPressure(gun, ModularCaster.pressure(gun) + CasterTuning.AIR_STROKE_PU / room);
            walls(level, player, gun);
        }
        level.playSound(null, player.blockPosition(), FSounds.GUN_PULSE_PUMP.get(), SoundSource.PLAYERS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
    }

    // --- the walls: what the tank does when it holds more than it was built to --------------------

    /**
     * Checks the tank against what it holds: a fluid hotter than the metal runs it, one that corrodes the parts eats them, a pressure
     * over the danger line weeps and may let go (the ladder of {@link PressureBurst}). Server side, on a timer; a tank under its
     * line and holding a fluid it tolerates is never touched however long it sits.
     */
    public static void walls(ServerLevel level, Entity holder, ItemStack gun) {
        // Nothing in it and no pressure on it: nothing to check, and no design parsed.
        FluidStack fluid = ModularCaster.content(gun);
        float p = ModularCaster.pressure(gun);
        if (fluid.isEmpty() && p <= 0f)
            return;
        CasterSpec spec = ModularCaster.spec(gun).orElse(null);
        if (spec == null)
            return;
        if (!fluid.isEmpty()) {
            float tu = FluidHeat.of(fluid).value();
            if (tu >= spec.tankMeltTu()) {
                burst(level, holder, gun, spec);
                return;
            }
            if (tu > spec.tankServiceTu())
                GunCondition.misuse(gun, CasterTuning.WEAR_WEEP * 0.1f);
            if (Corrosion.corrosive(fluid))
                GunCondition.misuse(gun, Corrosion.wearPerSecond(spec.weakestResist()) * 0.5f);
        }
        if (p <= spec.danger())
            return;
        if (PressureBurst.fails(new Pu(p), new Pu(spec.danger()), new Pu(spec.critical()), level.random)) {
            burst(level, holder, gun, spec);
            return;
        }
        // Weeps first: a hiss, a loss of pressure and a little fluid, and the wear of a stroke that asked too much.
        ModularCaster.setPressure(gun, p * 0.9f);
        if (!fluid.isEmpty()) {
            fluid.shrink(Math.max(1, fluid.getAmount() / 50));
            ModularCaster.setContent(gun, fluid);
        }
        GunCondition.misuse(gun, CasterTuning.WEAR_WEEP);
        level.sendParticles(ParticleTypes.CLOUD, holder.getX(), holder.getY() + 1.2, holder.getZ(), 4, 0.15, 0.1, 0.15, 0.02);
        level.playSound(null, holder.blockPosition(), FSounds.GUN_VENT.get(), SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    /** The walls let go: the fluid is gone, the gun is worn and whoever holds it is hurt (scalded too, if the fluid was hot). */
    private static void burst(ServerLevel level, Entity holder, ItemStack gun, CasterSpec spec) {
        FluidStack fluid = ModularCaster.content(gun);
        float tu = fluid.isEmpty() ? 0f : FluidHeat.of(fluid).value();
        ModularCaster.setContent(gun, FluidStack.EMPTY);
        ModularCaster.setPressure(gun, 0f);
        GunCondition.misuse(gun, CasterTuning.WEAR_BURST);
        if (holder instanceof LivingEntity living) {
            living.hurt(level.damageSources().explosion(null, null), CasterTuning.BURST_HP);
            if (tu > CasterTuning.SCALD_TU)
                living.hurt(Hazards.source(level, Hazards.SCALD), Math.min(CasterTuning.SCALD_MAX, (tu - CasterTuning.SCALD_TU) * CasterTuning.SCALD_PER_TU_MB * 100f));
        }
        level.sendParticles(ParticleTypes.EXPLOSION, holder.getX(), holder.getY() + 1.0, holder.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, holder.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.3f);
        if (holder instanceof Player player)
            AimPayloads.cue(player, AimPayloads.GUN, net.minecraft.network.chat.Component.translatable("message.feedback.caster.burst"), 50);
    }

    /** A pull with nothing to throw is a short wet cough, not a hammer on an empty chamber. */
    @Override
    public net.minecraft.sounds.SoundEvent dryClick() {
        return FSounds.GUN_EGUN_FLAT.get();
    }
}
