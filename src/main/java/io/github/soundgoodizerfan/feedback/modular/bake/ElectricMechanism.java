// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

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

import io.github.soundgoodizerfan.feedback.core.Haste;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec.Governor;
import io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec.Seat;
import io.github.soundgoodizerfan.feedback.modular.attunement.MomentumKinds;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.weapon.GunStance;
import io.github.soundgoodizerfan.feedback.weapon.HeatPellets;
import io.github.soundgoodizerfan.feedback.weapon.PulseShot;
import io.github.soundgoodizerfan.feedback.weapon.SteamCloud;
import io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits;

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
    private record Prepared(ElectricGunSpec spec, ModularElectricGun.Source source, CompoundTag drawn, io.github.soundgoodizerfan.feedback.core.unit.VoltageBand band, float joules,
                            float holdShare, float zoomShare, float power, float heatAfter, GunFigures figures) {
    }

    /**
     * The next shot of {@code gun} for {@code shooter} at game tick {@code now}, or null if it cannot fire: no spec, no battery or a flat
     * one, a seat with nothing charged, a governor holding it back. Changes nothing.
     */
    /** The heat a pulse of {@code power} leaves in this gun: its figure, times what a fitted {@code vent_hot} part does to a shot's heat (a powder gun's too). */
    private static float shotHeat(ElectricGunSpec spec, ItemStack gun, float power) {
        return spec.heatPerShot(power) * io.github.soundgoodizerfan.feedback.modular.attunement.HeatKinds.gunShotHeat(gun);
    }

    private static Prepared prepare(LivingEntity shooter, ItemStack gun, long now, FireMode mode) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null)
            return null;
        lastNow = now;
        ModularElectricGun.Source source = ModularElectricGun.source(shooter, gun, spec);
        if (source.battery().isEmpty())
            return null;
        CompoundTag drawn = BatteryStack.state(source.battery()).copy();
        io.github.soundgoodizerfan.feedback.core.unit.VoltageBand band = BatteryStack.caseBand(drawn);
        if (!BatteryStack.ready(drawn) || BatteryStack.charge(drawn) <= 0f)
            return null;
        int cells = Math.max(1, drawn.getInt("Cells"));
        float heat = ModularElectricGun.heat(gun, shooter.level());
        float holdShare = spec.holdCharge() ? ModularElectricGun.holdShare(gun, now) : 0f;
        int pumps = spec.pump() ? ModularElectricGun.pumps(gun) : 0;
        float zoomShare = spec.zoom() ? ModularElectricGun.zoomShare(gun, now) : 0f;
        float power = 1f + (spec.holdCharge() ? ModularTuning.EGUN_HOLD_POWER * holdShare : 0f) + ModularTuning.EGUN_PUMP_POWER * pumps;
        // What the seat needs before it can fire.
        float spin = 1f;
        switch (spec.seat()) {
            case CYLINDER -> {
                if (ModularElectricGun.chambers(gun, now, spec, band, cells) < 1f)
                    return null;
            }
            case CAPACITOR_BANK -> {
                if (ModularElectricGun.pips(gun, now, 1) < 1)
                    return null;
            }
            case BURST_BANK -> {
                if (ModularElectricGun.pips(gun, now, ModularTuning.GUN_BURST_ROUNDS) < 1)
                    return null;
            }
            case CORE_BREECH -> {
                if (ModularElectricGun.hot(gun))
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
        float gain = shotHeat(spec, gun, power) + ModularTuning.EGUN_PUMP_HEAT * pumps;
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
        // The draw: the pulse's energy in Eu; a weak cell gives the share it has.
        float needed = joules / ModularTuning.EGUN_J_PER_EU;
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
        // A worn receiver delivers less and sprays wider, as a worn bore does.
        GunFigures figures = spec.figures(joules, gunKg, ticks, zoomShare, spreadMult).aged(GunCondition.condition(gun), 0f);
        if (shooter instanceof Player player) {
            figures = ModularTraits.gunAimed(player, gun, figures);
            if (GunItem.dualWielding(player))
                figures = figures.oneHanded();
        }
        figures = figures.withCycle(1f / Haste.rate(shooter));
        return new Prepared(spec, source, drawn, band, joules, holdShare, zoomShare, power, Math.min(1.5f, heat + gain), figures);
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
                    && ModularElectricGun.heat(gun, level) + shotHeat(spec, gun, 1f) > 1f)
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
        io.github.soundgoodizerfan.feedback.core.unit.VoltageBand band = p.band();
        int cells = Math.max(1, p.drawn().getInt("Cells"));
        switch (spec.seat()) {
            case CYLINDER -> ModularElectricGun.setChambers(gun, ModularElectricGun.chambers(gun, now, spec, band, cells) - 1f, now);
            case CAPACITOR_BANK -> ModularElectricGun.spendPip(gun, now, 1, spec.bankRechargeTicks(cells));
            case BURST_BANK -> ModularElectricGun.spendPip(gun, now, ModularTuning.GUN_BURST_ROUNDS, spec.bankRechargeTicks(cells));
            case RECOIL_DYNAMO -> BatteryStack.store(p.drawn(), p.joules() * ModularTuning.EGUN_DYNAMO_RETURN / ModularTuning.EGUN_J_PER_EU);
            default -> {
            }
        }
        p.source().write().accept(rewritten(p.source().battery(), p.drawn()));
        if (shooter instanceof net.minecraft.server.level.ServerPlayer server)
            ModularElectricGun.mistakes(server, gun, spec, band, hand);
        if (spec.holdCharge())
            ModularElectricGun.clearHold(gun, now);
        if (spec.zoom())
            ModularElectricGun.resetZoom(gun, now);

        GunFigures figures = p.figures();
        if (shooter instanceof Player player)
            figures = figures.withSpread(GunStance.of(player, aimed) * MomentumKinds.braceSpread(player, gun));
        int pumps = spec.pump() ? ModularElectricGun.pumps(gun) : 0;
        if (pumps >= ModularTuning.EGUN_PUMP_MAX)
            pumpBlast(level, shooter, ModularTuning.EGUN_PUMP_BLAST_MULT * spec.damage(spec.delivered(p.joules())));
        else if (spec.seat() == Seat.CORE_BREECH) {
            // The one seat whose shot is a slow gout of heat, not a beam: pellets with a splitter, else one big one.
            float widen = 1f + ModularTuning.EGUN_PUMP_SPREAD * pumps;
            int beams = spec.beams() > 0 ? spec.beams() + Math.round(spec.beams() * ModularTuning.EGUN_PUMP_BEAMS * pumps) : 1;
            float cone = beams > 1 ? ModularTuning.EGUN_SPLIT_DEG_PER_BEAM * beams * widen : figures.spreadDeg() * widen;
            HeatPellets.launch(level, shooter, spec.delivered(p.joules()), spec.cutoff(p.zoomShare()) * ModularTuning.EGUN_CORE_RANGE, beams, cone, yaw, pitch, ModularElectricGun.colourOf(gun));
        } else {
            // Each pump widens the spread; a splitter also throws more beams, the way buckshot throws more pellets.
            float widen = 1f + ModularTuning.EGUN_PUMP_SPREAD * pumps;
            int beams = spec.beams() > 0 ? spec.beams() + Math.round(spec.beams() * ModularTuning.EGUN_PUMP_BEAMS * pumps) : 0;
            PulseShot.fire(level, shooter, gun, new PulseShot.Plan(spec.delivered(p.joules()), spec.cutoff(p.zoomShare()), spec.pierce(p.holdShare()),
                    ModularElectricGun.colourOf(gun), beams, spec.fork(), spec.seat() == Seat.CYLINDER, band, figures.spreadDeg() * widen, widen, spec.style()), yaw, pitch);
        }
        Vec3 muzzle = shooter.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(0.4));
        GunShots.report(level, shooter, figures, gun, muzzle);
        io.github.soundgoodizerfan.feedback.weapon.GunImpacts.energyMuzzle(level, muzzle, Vec3.directionFromRotation(pitch, yaw), Math.min(1f, figures.report()),
                ModularElectricGun.colourOf(gun), spec.style());
        // The muzzle glows brighter with the barrel's heat.
        int tint = ModularElectricGun.colourOf(gun);
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f), 0.6f),
                muzzle.x, muzzle.y, muzzle.z, 1 + Math.round(p.heatAfter() * 6f), 0.04, 0.04, 0.04, 0.01);

        // Heat, the pumps' share of it, and what the governor does with it.
        float heat = ModularElectricGun.heat(gun, level) + shotHeat(spec, gun, p.power()) + ModularTuning.EGUN_PUMP_HEAT * (spec.pump() ? ModularElectricGun.pumps(gun) : 0);
        if (spec.pump())
            ModularElectricGun.setPumps(gun, 0);
        if (spec.seat() == Seat.CORE_BREECH)
            ModularElectricGun.setHot(gun, true);
        switch (spec.governor()) {
            case THROTTLE -> heat = Math.min(heat, 0.95f);
            case VENT -> {
                if (heat >= 1f) {
                    float over = Mth.clamp((heat - 1f) / Math.max(1e-3f, shotHeat(spec, gun, p.power())), 0f, 1f);
                    ModularElectricGun.setLock(gun, now + Math.round(Mth.lerp(over, ModularTuning.EGUN_VENT_LOCK_MIN_TICKS, ModularTuning.EGUN_VENT_LOCK_MAX_TICKS)));
                    level.playSound(null, shooter.blockPosition(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_HEAT_TICK.get(), SoundSource.PLAYERS, 0.8f, 0.7f);
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
        ModularElectricGun.setHeat(gun, heat, level);
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
                spec.beams(), spec.fork(), spec.seat() == Seat.CYLINDER, ModularTuning.EGUN_REF_BAND, figures.spreadDeg(), 1f, spec.style()), yaw, pitch);
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
        if (spec == null)
            return 1f;
        return (spec.zoom() ? 1f / (1f + ModularTuning.EGUN_ZOOM_RANGE * ModularElectricGun.zoomShare(gun, lastNow)) : 1f) * spec.sightFov();
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
        return spec != null && spec.seat() == Seat.CORE_BREECH;
    }

    /** Players whose core breech is mid-vent: the stroke takes {@link #VENT_TICKS}, and a second press during it does nothing. */
    private static final java.util.Set<java.util.UUID> VENTING = new java.util.HashSet<>();

    /** The client's vent stroke ({@code GunMotion.VENT_MS}, 900 ms) in ticks; its beats are the sounds below. */
    private static final int VENT_TICKS = 18;

    @Override
    public boolean reloadKeyReady(ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        return spec != null && spec.seat() == Seat.CORE_BREECH && ModularElectricGun.hot(gun);
    }

    /**
     * The reload key breaks a core breech open: the hand slams the barrels down, the cores vent in a puff of steam, the hand
     * throws them shut. The gun stays hot, and cannot fire, until the whole stroke is done.
     */
    @Override
    public void reloadKey(ServerLevel level, Player player, ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        if (spec == null || spec.seat() != Seat.CORE_BREECH || !ModularElectricGun.hot(gun) || !VENTING.add(player.getUUID()))
            return;
        // The slap on the barrels, the steam, the forend taken from below, the slam shut and its latch.
        io.github.soundgoodizerfan.feedback.core.Later.in(3, () -> level.playSound(null, player.blockPosition(),
                io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_BUTT.get(), SoundSource.PLAYERS, 0.9f, 0.8f));
        io.github.soundgoodizerfan.feedback.core.Later.in(5, () -> {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getEyeY() - 0.2, player.getZ(), 14, 0.2, 0.1, 0.2, 0.06);
            level.playSound(null, player.blockPosition(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_VENT.get(), SoundSource.PLAYERS, 0.7f, 1.1f);
        });
        io.github.soundgoodizerfan.feedback.core.Later.in(9, () -> level.playSound(null, player.blockPosition(),
                io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_HEAT_TICK.get(), SoundSource.PLAYERS, 0.7f, 0.9f));
        io.github.soundgoodizerfan.feedback.core.Later.in(13, () -> {
            level.playSound(null, player.blockPosition(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_ARMED.get(), SoundSource.PLAYERS, 0.9f, 1f);
            level.playSound(null, player.blockPosition(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_HEAT_TICK.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
        });
        io.github.soundgoodizerfan.feedback.core.Later.in(VENT_TICKS, () -> {
            VENTING.remove(player.getUUID());
            long now = level.getGameTime();
            ModularElectricGun.setHot(gun, false);
            ModularElectricGun.setHeat(gun, ModularElectricGun.heat(gun, level) * ModularTuning.EGUN_CORE_VENT_KEEP, level);
        });
    }

    @Override
    public boolean canPump(ItemStack gun) {
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        return spec != null && spec.pump() && !(spec.seat() == Seat.CORE_BREECH && ModularElectricGun.hot(gun))
                && ModularElectricGun.pumps(gun) < ModularTuning.EGUN_PUMP_MAX;
    }

    /** A pump: two widen and strengthen the next shot, the third arms a blast for it; more do nothing. */
    @Override
    public void pumpKey(ServerLevel level, Player player, ItemStack gun) {
        if (!canPump(gun))
            return;
        int pumps = ModularElectricGun.pumps(gun);
        ModularElectricGun.setPumps(gun, pumps + 1);
        boolean armed = pumps + 1 >= ModularTuning.EGUN_PUMP_MAX;
        // The click lands as the slider runs home, which is where the client's stroke ends.
        var at = player.blockPosition();
        io.github.soundgoodizerfan.feedback.core.Later.in(ModularTuning.EGUN_PUMP_SOUND_TICKS, () -> level.playSound(null, at, armed ? io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_ARMED.get() : io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_PUMP.get(), SoundSource.PLAYERS,
                armed ? 1f : 0.9f, 0.96f + level.random.nextFloat() * 0.08f));
    }

    /**
     * The armed shot: no pulse, a blast on the wielder that hurts everything near, the wielder too. {@code damage} is 1.5 times what the
     * three-pump shot would have done in all (a splitter's beams added up); the wielder takes the same, up to three quarters of their health.
     */
    private static void pumpBlast(ServerLevel level, LivingEntity shooter, float damage) {
        float r = ModularTuning.EGUN_PUMP_BLAST_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, shooter.getBoundingBox().inflate(r),
                e -> e != shooter && e.isAlive() && e.distanceToSqr(shooter) <= r * r))
            e.hurt(level.damageSources().explosion(shooter, shooter), damage);
        shooter.hurt(level.damageSources().explosion(shooter, shooter), Math.min(damage, shooter.getMaxHealth() * ModularTuning.EGUN_PUMP_BLAST_SELF_CAP));
        level.sendParticles(ParticleTypes.EXPLOSION, shooter.getX(), shooter.getY(0.5), shooter.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, shooter.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 1.2f);
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
            return List.of(new GunSound.Layer(io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_SHOT.get(), volume, jitter));
        SoundEvent crack = io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_PULSE_SHOT.get();
        float pitch = 1.1f - 0.06f * spec.tier();
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
