// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Map;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.Haste;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Governor;
import io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec.Seat;
import io.github.soundgoodizerfan.feedback.modular.attunement.MomentumKinds;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.weapon.GunStance;
import io.github.soundgoodizerfan.feedback.weapon.PulseShot;
import io.github.soundgoodizerfan.feedback.weapon.SteamCloud;

/**
 * A pulse gun's mechanism: the seat decides what a pull needs and what it leaves, the battery gives the energy, the barrel hands the
 * pulse on ({@link PulseShot}). {@link #prepare} works the next shot out once, for the client's prediction and the server's pull alike,
 * so the two agree; a pull spends the draw and the seat's charge, books the mistakes and the heat, and sends the pulse. Nothing is
 * consumed but battery charge.
 */
public final class ElectricMechanism implements GunMechanism {

    public static final ElectricMechanism INSTANCE = new ElectricMechanism();

    private ElectricMechanism() {
    }

    /** The next shot, worked out and not yet spent. */
    private record Prepared(ElectricGunSpec spec, ModularElectricGun.Source source, CompoundTag drawn, float volts, float joules,
                            float holdShare, float zoomShare, float power, float heatAfter, GunFigures figures) {
    }

    /**
     * The next shot of {@code gun} for {@code shooter} at game tick {@code now}, or null if it cannot fire: no spec, no battery or a flat
     * one, a seat with nothing charged, a governor holding it back. Changes nothing.
     */
    private static Prepared prepare(LivingEntity shooter, ItemStack gun, long now, FireMode mode) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null)
            return null;
        lastNow = now;
        ModularElectricGun.Source source = ModularElectricGun.source(shooter, gun, spec);
        if (source.battery().isEmpty())
            return null;
        CompoundTag drawn = BatteryStack.state(source.battery()).copy();
        float volts = BatteryStack.terminalVolts(drawn);
        if (!BatteryStack.ready(drawn) || BatteryStack.charge(drawn) <= 0f || volts <= 0f)
            return null;
        int cells = Math.max(1, drawn.getInt("Cells"));
        float heat = ModularElectricGun.heat(gun, now, spec.shed());
        float holdShare = spec.holdCharge() ? ModularElectricGun.holdShare(gun, now) : 0f;
        int pumps = spec.pump() ? ModularElectricGun.pumps(gun) : 0;
        float zoomShare = spec.zoom() ? ModularElectricGun.zoomShare(gun, now) : 0f;
        float power = 1f + (spec.holdCharge() ? ModularTuning.EGUN_HOLD_POWER * holdShare : 0f) + ModularTuning.EGUN_PUMP_POWER * pumps;
        // What the seat needs before it can fire.
        float spin = 1f;
        switch (spec.seat()) {
            case CYLINDER -> {
                if (ModularElectricGun.chambers(gun, now, spec, volts, cells) < 1f)
                    return null;
            }
            case CAPACITOR_BANK -> {
                if (ModularElectricGun.pips(gun, now, 1) < 1)
                    return null;
            }
            case BURST_BANK -> {
                if (ModularElectricGun.pips(gun, now, FTuning.GUN_BURST_ROUNDS) < 1)
                    return null;
            }
            case FLYWHEEL -> {
                spin = ModularElectricGun.spin(gun, now, spec);
                if (spin < ModularTuning.EGUN_FLY_FIRST_SPIN)
                    return null;
            }
            default -> {
            }
        }
        // What the governor says.
        float gain = spec.heatPerShot(power) + ModularTuning.EGUN_PUMP_HEAT * pumps;
        switch (spec.governor()) {
            case VENT -> {
                if (now < ModularElectricGun.lockUntil(gun))
                    return null;
            }
            case CUTOFF -> {
                if (heat + gain > 1f || ModularElectricGun.tripped(gun) && heat > ModularTuning.EGUN_CUTOFF_RESUME)
                    return null;
            }
            default -> {
            }
        }
        float joules = spec.pulseJ() * spec.seatMult() * power * spin;
        // The draw: the energy over the volts; a weak cell gives the share it has.
        float needed = joules / (volts * ModularTuning.EGUN_J_PER_VAMPTICK);
        float took = BatteryStack.draw(drawn, needed);
        float fraction = needed <= 0f ? 0f : took / needed;
        joules *= fraction;
        ModularFigures baked = ModularStacks.figures(gun);
        float gunKg = baked == null || baked.get(ModularFigures.Fig.MASS) <= 0f ? spec.barrelMass() : baked.get(ModularFigures.Fig.MASS);
        float rate = spec.governor() == Governor.THROTTLE ? 1f - Mth.clamp(heat, 0f, 1f) * (1f - ModularTuning.EGUN_THROTTLE_FLOOR) : 1f;
        int ticks = Math.max(1, Math.round(spec.cycleTicks(mode == FireMode.BURST) / rate));
        // A lit reticle helps where it is dark.
        float spreadMult = spec.darkAt() > 0f && shooter.level().getMaxLocalRawBrightness(shooter.blockPosition()) <= spec.darkAt()
                ? 1f - spec.darkCut() : 1f;
        GunFigures figures = spec.figures(joules, gunKg, ticks, zoomShare, spreadMult);
        if (shooter instanceof Player player) {
            figures = ModularTraits.gunAimed(player, gun, figures);
            if (GunItem.dualWielding(player))
                figures = figures.oneHanded();
        }
        figures = figures.withCycle(1f / Haste.rate(shooter));
        return new Prepared(spec, source, drawn, volts, joules, holdShare, zoomShare, power, Math.min(1.5f, heat + gain), figures);
    }

    @Override
    public boolean built(ItemStack gun) {
        return ModularElectricGun.spec(gun).isPresent();
    }

    @Override
    public boolean trigger(ServerLevel level, Player shooter, ItemStack gun, float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        if (ModularStacks.broken(gun))
            return false;
        long now = level.getGameTime();
        Prepared p = prepare(shooter, gun, now, GunItem.mode(gun));
        if (p == null) {
            ElectricGunSpec spec = ModularElectricGun.specOf(gun);
            // A dead-man cutoff that stopped the fire holds until the barrel has cooled.
            if (spec != null && spec.governor() == Governor.CUTOFF
                    && ModularElectricGun.heat(gun, now, spec.shed()) + spec.heatPerShot(1f) > 1f)
                ModularElectricGun.setTripped(gun, true);
            return false;
        }
        if (!GunShots.accept(shooter, p.figures(), nowMs, hand))
            return false;
        fire(level, shooter, gun, p, now, yaw, pitch, aimed, hand);
        return true;
    }

    /** Spends the draw and the seat's charge, sends the pulse, books the heat and the mistakes. */
    private static void fire(ServerLevel level, LivingEntity shooter, ItemStack gun, Prepared p, long now, float yaw, float pitch,
                             boolean aimed, InteractionHand hand) {
        ElectricGunSpec spec = p.spec();
        // The seat.
        float volts = p.volts();
        int cells = Math.max(1, p.drawn().getInt("Cells"));
        switch (spec.seat()) {
            case CYLINDER -> ModularElectricGun.setChambers(gun, ModularElectricGun.chambers(gun, now, spec, volts, cells) - 1f, now);
            case CAPACITOR_BANK -> ModularElectricGun.spendPip(gun, now, 1, spec.bankRechargeTicks(cells));
            case BURST_BANK -> ModularElectricGun.spendPip(gun, now, FTuning.GUN_BURST_ROUNDS, spec.bankRechargeTicks(cells));
            case RECOIL_DYNAMO -> BatteryStack.charge(p.drawn(), p.joules() * ModularTuning.EGUN_DYNAMO_RETURN
                    / (volts * ModularTuning.EGUN_J_PER_VAMPTICK), volts);
            default -> {
            }
        }
        p.source().write().accept(rewritten(p.source().battery(), p.drawn()));
        if (shooter instanceof net.minecraft.server.level.ServerPlayer server)
            ModularElectricGun.mistakes(server, gun, spec, volts, hand);
        if (spec.holdCharge())
            ModularElectricGun.clearHold(gun, now);
        if (spec.zoom())
            ModularElectricGun.resetZoom(gun, now);

        GunFigures figures = p.figures();
        if (shooter instanceof Player player)
            figures = figures.withSpread(GunStance.of(player, aimed) * MomentumKinds.braceSpread(player, gun));
        PulseShot.fire(level, shooter, gun, new PulseShot.Plan(spec.delivered(p.joules()), spec.cutoff(p.zoomShare()), spec.pierce(p.holdShare()),
                ModularElectricGun.colourOf(gun), spec.beams(), spec.fork(), spec.seat() == Seat.CYLINDER, volts, figures.spreadDeg()), yaw, pitch);
        Vec3 muzzle = shooter.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(0.4));
        GunShots.report(level, shooter, figures, gun, muzzle);
        // The muzzle glows brighter with the barrel's heat.
        level.sendParticles(ParticleTypes.GLOW, muzzle.x, muzzle.y, muzzle.z, 1 + Math.round(p.heatAfter() * 6f), 0.04, 0.04, 0.04, 0.01);

        // Heat, the pumps' share of it, and what the governor does with it.
        float heat = ModularElectricGun.heat(gun, now, spec.shed()) + spec.heatPerShot(p.power()) + ModularTuning.EGUN_PUMP_HEAT * (spec.pump() ? ModularElectricGun.pumps(gun) : 0);
        if (spec.pump())
            ModularElectricGun.setPumps(gun, 0);
        switch (spec.governor()) {
            case THROTTLE -> heat = Math.min(heat, 0.95f);
            case VENT -> {
                if (heat >= 1f) {
                    float over = Mth.clamp((heat - 1f) / Math.max(1e-3f, spec.heatPerShot(p.power())), 0f, 1f);
                    ModularElectricGun.setLock(gun, now + Math.round(Mth.lerp(over, ModularTuning.EGUN_VENT_LOCK_MIN_TICKS, ModularTuning.EGUN_VENT_LOCK_MAX_TICKS)));
                    SteamCloud.vent(level, shooter, pitch, yaw);
                    heat = ModularTuning.EGUN_VENT_RESET_HEAT;
                }
            }
            case CUTOFF -> ModularElectricGun.setTripped(gun, false);
            default -> {
                if (heat > 1f)
                    ModularElectricGun.overheated(shooter, gun, hand);
            }
        }
        ModularElectricGun.setHeat(gun, heat, now);
        GunShots.bookShot(level, shooter, gun, figures, Math.min(1f, heat), hand, aimed);
    }

    /** {@code battery} with the state {@code drawn} the shot left in it. */
    private static ItemStack rewritten(ItemStack battery, CompoundTag drawn) {
        ItemStack out = battery.copy();
        out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BATTERY.get(), drawn);
        return out;
    }

    /** A mob's cell is always charged: it sends one ordinary pulse of its seat, and the goal keeps the cycle. */
    @Override
    public boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null)
            return false;
        float joules = spec.pulseJ() * spec.seatMult();
        GunFigures figures = spec.figures(joules, spec.barrelMass(), spec.cycleTicks(false), 0f, 1f);
        PulseShot.fire(level, mob, gun, new PulseShot.Plan(spec.delivered(joules), spec.cutoff(0f), spec.pierce(0f), ModularElectricGun.colourOf(gun),
                spec.beams(), spec.fork(), spec.seat() == Seat.CYLINDER, ModularTuning.EGUN_REF_VOLTS, figures.spreadDeg()), yaw, pitch);
        GunShots.report(level, mob, figures, gun, mob.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(0.4)));
        return true;
    }

    @Override
    public GunFigures predict(Player player, ItemStack gun, Level level, Map<ResourceLocation, Material> materials, int hand) {
        if (ModularStacks.broken(gun))
            return null;
        Prepared p = prepare(player, gun, level.getGameTime(), GunItem.mode(gun));
        return p == null ? null : p.figures();
    }

    /** A scope zoom charge closes the view in as the sight stays up. */
    @Override
    public float sightZoom(ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        return spec != null && spec.zoom() ? 1f / (1f + ModularTuning.EGUN_ZOOM_RANGE * ModularElectricGun.zoomShare(gun, lastNow)) : 1f;
    }

    @Override
    public int raiseMs(ItemStack gun) {
        return 250;
    }

    /** The fire mode the seat allows for the one set: the set one if the seat has it, else the seat's first. */
    @Override
    public FireMode allow(ItemStack gun, FireMode set) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null)
            return set;
        for (FireMode m : spec.seat().modes())
            if (m == set)
                return set;
        return spec.seat().modes()[0];
    }

    /** The V key cycles only inside the seat's modes. */
    @Override
    public FireMode next(ItemStack gun, FireMode current) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null)
            return current.next();
        FireMode[] modes = spec.seat().modes();
        for (int i = 0; i < modes.length; i++)
            if (modes[i] == current)
                return modes[(i + 1) % modes.length];
        return modes[0];
    }

    @Override
    public boolean tracksTrigger(ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        return spec != null && (spec.seat() == Seat.FLYWHEEL || spec.holdCharge());
    }

    @Override
    public boolean fireOnRelease(ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        return spec != null && spec.holdCharge();
    }

    @Override
    public boolean reloadKeyWorks(ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        return spec != null && spec.pump();
    }

    /** A pump: three raise the next shot's power and heat; the fourth bursts the capacitor and hurts the wielder. */
    @Override
    public void reloadKey(ServerLevel level, Player player, ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null || !spec.pump())
            return;
        int pumps = ModularElectricGun.pumps(gun);
        long now = level.getGameTime();
        if (pumps >= ModularTuning.EGUN_PUMP_MAX) {
            ModularElectricGun.setPumps(gun, 0);
            player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_PUMP_BURST_DAMAGE);
            ModularElectricGun.setHeat(gun, Math.min(1f, ModularElectricGun.heat(gun, now, spec.shed()) + 0.3f), now);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getEyeY(), player.getZ(), 20, 0.3, 0.3, 0.3, 0.4);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.8f);
            return;
        }
        ModularElectricGun.setPumps(gun, pumps + 1);
        level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.5f, 1.2f + 0.2f * pumps);
    }

    @Override
    public void aimChanged(ItemStack gun, boolean aiming, long gameTime) {
        ModularElectricGun.setAim(gun, aiming, gameTime);
    }

    /** The report: a crack of the pulse, a flywheel's pitch rising with its spin, a bank's low thump under it. */
    @Override
    public List<GunSound.Layer> sound(ItemStack gun, GunFigures figures, RandomSource random) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        float jitter = 0.95f + random.nextFloat() * 0.1f;
        float volume = ModularTuning.EGUN_VOLUME * figures.report() * 2f;
        if (spec == null)
            return List.of(new GunSound.Layer(SoundEvents.FIREWORK_ROCKET_BLAST, volume, jitter));
        SoundEvent crack = SoundEvents.FIREWORK_ROCKET_BLAST;
        float pitch = 1.5f - 0.1f * spec.tier();
        return switch (spec.seat()) {
            case CAPACITOR_BANK -> List.of(new GunSound.Layer(SoundEvents.LIGHTNING_BOLT_IMPACT, volume * 0.8f, 1.6f * jitter),
                    new GunSound.Layer(crack, volume, 0.7f * jitter));
            case FLYWHEEL -> List.of(new GunSound.Layer(crack, volume, (0.8f + 1.0f * spinNow(gun, spec)) * jitter));
            default -> List.of(new GunSound.Layer(crack, volume, pitch * jitter));
        };
    }

    /** The game tick of the last shot prepared on either side: a sound reads the spin and the zoom at it. */
    private static long lastNow;

    private static float spinNow(ItemStack gun, ElectricGunSpec spec) {
        return Mth.clamp(ModularElectricGun.spin(gun, lastNow, spec), 0f, 1f);
    }

    @Override
    public SimpleParticleType muzzleParticle(ItemStack gun, GunFigures figures) {
        return ParticleTypes.ELECTRIC_SPARK;
    }
}
