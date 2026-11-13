// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.Tags;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.Protection;
import io.github.soundgoodizerfan.feedback.modular.ElectricGunSpec;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

/**
 * A pulse gun's shot on the server: instant, no projectile. A beam is traced from the muzzle to the gun's cutoff range in one go, through
 * glass (not tinted glass), hurting up to the gun's pierce count of mobs on the line (every one for a capacitor bank). It meets armour as
 * a bullet does, but metal armour takes the shock bonus in place of resisting; a wet target or a metal-armoured one takes extra and is
 * staggered by the energy. An arc fork carries the shock on to the nearest conductive body, then any. A coin of the shooter's in the air
 * redirects the beam, and a redirect costs it nothing.
 */
public final class PulseShot {

    /**
     * What one trigger pull sends.
     *
     * @param joules   joules that leave the barrel (one pulse; a splitter shares it among its beams).
     * @param cutoff   blocks the pulse holds its damage out to.
     * @param pierce   mobs beyond the first (a large number for every mob in line).
     * @param colour   the beam's colour, an RGB the blueprint chose.
     * @param beams    beams of a splitter (0 for one plain beam).
     * @param widen    what a pump charge multiplies the splitter cone by (1 for none; a plain beam's spread arrives already widened).
     * @param fork     whether an arc fork carries the shock on.
     * @param cylinder whether a coin redirect adds to the pulse.
     * @param volts    the cell's volts: what the wielder's insulation is tested against.
     * @param spreadDeg the cone one beam leaves in.
     */
    public record Plan(float joules, float cutoff, int pierce, int colour, int beams, boolean fork, boolean cylinder, float volts, float spreadDeg, float widen) {
    }

    private PulseShot() {
    }

    /** Sends the pulse(s) of {@code plan} from {@code shooter}'s eye along {@code yaw}/{@code pitch}. */
    public static void fire(ServerLevel level, LivingEntity shooter, ItemStack gun, Plan plan, float yaw, float pitch) {
        int beams = Math.max(1, plan.beams());
        float each = beams > 1 ? plan.joules() * ModularTuning.EGUN_SPLIT_TOTAL / beams : plan.joules();
        float cone = beams > 1 ? ModularTuning.EGUN_SPLIT_DEG_PER_BEAM * beams * plan.widen() : plan.spreadDeg();
        Vec3 origin = shooter.getEyePosition();
        Set<Integer> seen = new HashSet<>();
        for (int k = 0; k < beams; k++) {
            float r = cone * Mth.sqrt(level.random.nextFloat());
            float theta = level.random.nextFloat() * Mth.TWO_PI;
            float aimPitch = Mth.clamp(pitch + r * Mth.sin(theta), -90f, 90f);
            float aimYaw = yaw + r * Mth.cos(theta);
            // Beams of one splitter shot hurt a body once between them at the nearest beam's strength; a plain beam has its own set.
            trace(level, shooter, gun, plan, origin.add(Vec3.directionFromRotation(aimPitch, aimYaw).scale(0.4)),
                    Vec3.directionFromRotation(aimPitch, aimYaw), each, beams > 1 ? new HashSet<>() : seen);
        }
        if (plan.fork() && shooter.isInWaterOrRain())
            wetArc(level, shooter, gun, plan);
    }

    // --- the beam ----------------------------------------------------------------------------

    private record Hit(Entity entity, Vec3 at, double dist) {
    }

    private static void trace(ServerLevel level, LivingEntity shooter, ItemStack gun, Plan plan, Vec3 from, Vec3 dir, float joules,
                              Set<Integer> seen) {
        Vec3 pos = from;
        double travelled = 0;
        int left = plan.pierce() == Integer.MAX_VALUE ? Integer.MAX_VALUE : plan.pierce() + 1;
        for (int guard = 0; guard < 12 && travelled < plan.cutoff() && left > 0; guard++) {
            Vec3 end = pos.add(dir.scale(plan.cutoff() - travelled));
            // A coin the shooter flipped: the beam turns on it, whole.
            CoinShot.Result coin = CoinShot.resolve(level, shooter, pos, end, 0.001f, FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS);
            if (coin != null) {
                line(level, plan.colour(), pos, coin.point());
                travelled += pos.distanceTo(coin.point());
                pos = coin.point();
                if (coin.direction() != null) {
                    dir = coin.direction();
                    if (plan.cylinder())
                        joules *= ModularTuning.EGUN_COIN_CYLINDER;
                }
                continue;
            }
            BlockHitResult block = level.clip(new ClipContext(pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            List<Hit> hits = new ArrayList<>();
            for (Entity e : level.getEntities(shooter, new AABB(pos, stop).inflate(1.0),
                    e -> e.isPickable() && !e.isSpectator() && e.isAlive() && !seen.contains(e.getId()))) {
                Optional<Vec3> at = e.getBoundingBox().inflate(e.getPickRadius() + 0.05).clip(pos, stop);
                if (at.isPresent())
                    hits.add(new Hit(e, at.get(), at.get().distanceTo(pos)));
            }
            hits.sort(Comparator.comparingDouble(Hit::dist));
            boolean ended = false;
            for (Hit h : hits) {
                seen.add(h.entity().getId());
                strike(level, shooter, gun, plan, h.entity(), h.at(), joules);
                if (--left <= 0) {
                    stop = h.at();
                    ended = true;
                    break;
                }
            }
            line(level, plan.colour(), pos, stop);
            travelled += pos.distanceTo(stop);
            if (ended)
                return;
            if (block.getType() == HitResult.Type.MISS)
                return;
            BlockState state = level.getBlockState(block.getBlockPos());
            if (!passes(state)) {
                sparks(level, plan.colour(), stop, 5);
                return;
            }
            pos = stop.add(dir.scale(0.02));
        }
    }

    /** Glass and glass panes let a pulse through; tinted glass does not. */
    private static boolean passes(BlockState state) {
        return (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) && !state.is(Tags.Blocks.GLASS_BLOCKS_TINTED);
    }

    // --- a hit -------------------------------------------------------------------------------

    private static void strike(ServerLevel level, LivingEntity shooter, ItemStack gun, Plan plan, Entity target, Vec3 at, float joules) {
        float damage = joules * ModularTuning.EGUN_DAMAGE_PER_J;
        hurt(level, shooter, target, damage, joules, plan.volts());
        sparks(level, plan.colour(), at, 8);
        if (shooter instanceof Player player) {
            ModularTraits.gunRoundLanded(level, player, gun, at, target, damage / FTuning.GUN_DAMAGE_PER_WORK);
            GunFu.bulletHit(level, shooter, target, ElectricGunSpec.impulse(joules));
        }
        if (plan.fork() && target instanceof LivingEntity struck)
            arc(level, shooter, plan, struck, joules);
    }

    /** One body's hit: a wet or metal-armoured target takes the shock bonus (and the armour no longer resists), then the stagger. */
    private static void hurt(ServerLevel level, LivingEntity shooter, Entity target, float damage, float joules, float volts) {
        boolean metal = target instanceof LivingEntity l && conductive(l);
        boolean wet = target.isInWaterOrRain();
        float dealt = damage * (wet || metal ? 1f + ModularTuning.EGUN_SHOCK_BONUS : 1f);
        target.invulnerableTime = 0;
        float through = metal || !(target instanceof LivingEntity l) ? 1f : GunShots.pierce(FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS, GunShots.armourHardness(l));
        float dealtThrough = dealt * through, dealtRest = dealt - dealtThrough;
        GunShots.asBullet(() -> {
            if (dealtThrough > 0f)
                target.hurt(Hazards.source(level, GunShots.BULLET_PIERCE, shooter), dealtThrough);
            if (dealtRest > 0f) {
                target.invulnerableTime = 0;
                target.hurt(GunShots.attack(level, shooter), dealtRest);
            }
        });
        if (target instanceof LivingEntity living && living.isAlive())
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 + Math.round(joules * ModularTuning.EGUN_STAGGER_TICKS_PER_J), 3,
                    false, false, false));
    }

    /** The arc fork: the shock goes on from the body struck to the nearest conductive one within reach, else the nearest of any. */
    private static void arc(ServerLevel level, LivingEntity shooter, Plan plan, LivingEntity from, float joules) {
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(ModularTuning.EGUN_FORK_RANGE),
                e -> e != from && e != shooter && e.isAlive() && !e.isSpectator() && !shooter.isAlliedTo(e));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        LivingEntity to = near.stream().filter(e -> conductive(e) || e.isInWaterOrRain()).findFirst().orElse(near.isEmpty() ? null : near.get(0));
        if (to == null)
            return;
        Vec3 a = from.getBoundingBox().getCenter(), b = to.getBoundingBox().getCenter();
        line(level, plan.colour(), a, b);
        float damage = joules * ModularTuning.EGUN_DAMAGE_PER_J * ModularTuning.EGUN_FORK_SHARE;
        hurt(level, shooter, to, damage, joules * ModularTuning.EGUN_FORK_SHARE, plan.volts());
        sparks(level, plan.colour(), b, 6);
    }

    /** An arc fork fired in water or rain: everything wet within reach takes the shock, the wielder too unless insulated. */
    private static void wetArc(ServerLevel level, LivingEntity shooter, ItemStack gun, Plan plan) {
        float joules = plan.joules() * ModularTuning.EGUN_WET_FORK_SHARE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, shooter.getBoundingBox().inflate(ModularTuning.EGUN_WET_FORK_RANGE),
                e -> e.isAlive() && !e.isSpectator() && e.isInWaterOrRain())) {
            if (e == shooter && insulated(shooter, plan.volts()))
                continue;
            sparks(level, plan.colour(), e.getBoundingBox().getCenter(), 6);
            hurt(level, shooter, e, joules * ModularTuning.EGUN_DAMAGE_PER_J, joules, plan.volts());
        }
    }

    /** Whether {@code who} is insulated from a cell of {@code volts}: the existing rule, insulating gloves held or worn. */
    public static boolean insulated(LivingEntity who, float volts) {
        return who instanceof Player player && Protection.shockProofVolts(player) >= volts;
    }

    /** Whether a body is armoured in metal: a worn piece whose material (or, for a modular piece, any part's) conducts. */
    public static boolean conductive(LivingEntity body) {
        var materials = MaterialTable.get().materials();
        for (ItemStack piece : body.getArmorSlots()) {
            if (piece.isEmpty())
                continue;
            ModularDesign design = ModularStacks.design(piece);
            if (design != null) {
                for (ModularDesign.Part p : design.parts()) {
                    Material m = materials.get(p.material());
                    if (m != null && m.ampacity().isPresent() && m.kind().orElse(null) == Material.Kind.METAL)
                        return true;
                }
                continue;
            }
            Optional<Material.Located> found = Material.find(materials, piece);
            if (found.isPresent() && found.get().material().ampacity().isPresent() && found.get().material().kind().orElse(null) == Material.Kind.METAL)
                return true;
        }
        return false;
    }

    // --- the look ----------------------------------------------------------------------------

    /** The beam: a line of coloured dust, never more than forty points however long. */
    private static void line(ServerLevel level, int colour, Vec3 a, Vec3 b) {
        double len = a.distanceTo(b);
        if (len < 1e-3)
            return;
        DustParticleOptions dust = dust(colour, 0.8f);
        double step = Math.max(0.5, len / 40.0);
        Vec3 d = b.subtract(a).normalize().scale(step);
        Vec3 p = a;
        for (double t = 0; t < len; t += step, p = p.add(d))
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    private static void sparks(ServerLevel level, int colour, Vec3 at, int n) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, n, 0.15, 0.15, 0.15, 0.3);
        level.sendParticles(dust(colour, 1.2f), at.x, at.y, at.z, n, 0.2, 0.2, 0.2, 0.05);
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }
}
