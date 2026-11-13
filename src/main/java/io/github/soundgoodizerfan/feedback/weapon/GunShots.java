// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import io.github.soundgoodizerfan.feedback.combat.parry.Parrying;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server's side of a shot ({@code docs/specs/feedback_guns_spec.md} sections 7 and 7.1).
 *
 * <p><b>No entity per bullet</b> -- the spec's recommendation, and what TacZ does the other way
 * (an {@code EntityKineticBullet} per round, swept per tick, proven at its scale). A shot is a
 * position and a velocity held here and stepped once a tick as a swept segment: a block clip and
 * an entity box sweep over the whole tick's travel, so it cannot tunnel, and nothing is
 * synchronised because nothing is an entity. The cost is that a client sees no bullet but the
 * tracer it is told to draw; there is no tracer in this step.
 *
 * <p>A shot is stepped at most once per tick, so a bullet at 20 blocks a tick is one segment.
 * Hit energy falls with the square of the speed it has left, so drag and gravity cost a hit
 * damage honestly (spec 3.3).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GunShots {

    private static int NEXT_ID = 1;

    private static final class Shot {
        final LivingEntity shooter;
        Vec3 pos;
        Vec3 velocity;
        final float muzzleSpeed;
        final float muzzleEnergy;
        int age;
        /** Ticks the shot still holds where it is, ringing in a struck coin, before it goes on. */
        int hold;
        /** Entities this shot has already gone through, so it is not counted on one twice. */
        final java.util.Set<Integer> pierced = new java.util.HashSet<>();
        /** Players who have already heard this bullet go by. */
        final java.util.Set<java.util.UUID> whizzed = new java.util.HashSet<>();
        /** A luminous payload: the path is drawn bright. */
        boolean tracer;
        /** A flammable payload: what it hits is set alight. */
        boolean incendiary;
        /** Power of the explosion where it stops, or 0 (explosive filler). */
        float blast;
        /** The projectile's material hardness: what decides a glance off a block and a way through armour. */
        float hardness = GunTuning.GUN_DEFAULT_PROJECTILE_HARDNESS;
        final float massMu;
        /** Momentum of the round and gas at the muzzle, kg m/s: what Muzzle Stagger reads. */
        final float impulse;
        int bounces;
        /** A polymer projectile: a rubber round (see {@link RubberRounds}). */
        boolean rubber;
        /** The gun it came from, so its traits hear where the round lands. */
        ItemStack gun = ItemStack.EMPTY;
        /** What the round's shape and payload do to the flight and the hit. */
        Flight flight = Flight.PLAIN;
        /** A toxic payload lets its cloud go once, where it first lands. */
        boolean gassed;
        /** A vial breaks once. */
        boolean vialSpent;
        /** Barrel attunements read at fire time: signed bend toward (+) or away (-) from heavy armour (Lode); keeps speed in water (Tide). */
        int bend;
        boolean keepsSpeedWet;
        /** The streak the clients fly for this shot ({@code GunTracerPayload}). */
        final int id = NEXT_ID++;
        int argb;
        float dragMult = 1f;
        /** Where the shot ended, for the streak to stop on. */
        Vec3 stopAt;

        Shot(LivingEntity shooter, Vec3 pos, Vec3 velocity, GunFigures figures) {
            this.shooter = shooter;
            this.pos = pos;
            this.velocity = velocity;
            this.muzzleSpeed = figures.muzzleSpeed();
            this.muzzleEnergy = figures.muzzleEnergy();
            this.massMu = figures.projectileMass().value();
            this.impulse = figures.impulse();
        }

        /** Momentum now, kg m/s: velocity is blocks a tick, so twenty metres a second per unit. */
        float momentum() {
            return massMu * (float) velocity.length() * 20f;
        }

        /** Give up {@code work}: the speed falls to what the energy left says. @return whether enough is left to fly on. */
        boolean spend(float work) {
            float e = energy();
            float left = e - work;
            if (left <= muzzleEnergy * GunTuning.GUN_MIN_ENERGY_FRACTION)
                return false;
            velocity = velocity.scale(Math.sqrt(left / e));
            return true;
        }

        /** Work this projectile would deliver now: the muzzle's, scaled by speed squared. */
        float energy() {
            float v = (float) velocity.length();
            return muzzleEnergy * (v * v) / (muzzleSpeed * muzzleSpeed);
        }
    }

    /**
     * What a round's shape, jacket and payload change about its flight and its hit, as multipliers on what the
     * materials already give ({@link BulletShape}); {@link #PLAIN} is a round nose of one metal with no payload.
     *
     * @param drag   air drag against a round nose's.
     * @param damage hurt per Work against a round nose's.
     * @param ease   how readily it breaks a brittle block (momentum needed is divided by this).
     * @param glance how readily it glances off a block (the turning angle is multiplied by this).
     * @param traits what the payload material is (sticky glues, fibrous tethers); {@code seed} is a seed it plants on farmland.
     * @param vial   a potion the round carries, let go where it lands, or null.
     * @param perks  what the payload does to a living thing struck ({@link io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks#affect}).
     */
    public record Flight(float drag, float damage, float ease, float glance, java.util.Map<io.github.soundgoodizerfan.feedback.process.Material.Perk, Integer> perks,
                         io.github.soundgoodizerfan.feedback.potion.PotionState vial,
                         java.util.Set<io.github.soundgoodizerfan.feedback.process.Material.Trait> traits,
                         net.minecraft.resources.ResourceLocation seed) {
        public static final Flight PLAIN = new Flight(1f, 1f, 1f, 1f, java.util.Map.of(), null, java.util.Set.of(), null);

        /** The flight of {@code round} in {@code materials}. */
        public static Flight of(Cartridge round, java.util.Map<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials) {
            return new Flight(round.drag(materials), round.shape().damage(), round.shape().ease(), round.shape().glance(),
                    round.payloadPerks(materials), round.vial().orElse(null), round.payloadTraits(materials), round.seed().orElse(null));
        }
    }

    /** Damage armour does not reduce: the share of a hit a harder bullet puts through it. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> BULLET_PIERCE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Feedback.id("bullet_pierce"));

    private static final Map<ServerLevel, List<Shot>> LIVE = new HashMap<>();
    /** Earliest wall-clock ms each shooter may fire again. */
    private static final Map<HandKey, Long> NEXT_OK = new HashMap<>();

    /** A shooter's hand: each gun keeps its own cycle, so a pistol in each hand is two guns and not one. */
    private record HandKey(UUID id, boolean off) {
    }
    /** When each shooter last fired, wall-clock ms: what an open bolt's first shot reads. */
    private static final Map<UUID, Long> LAST_SHOT = new HashMap<>();

    /** Dodge Charge: the next pull from either hand of {@code shooter} waits out no cycle. */
    public static void clearCycle(Player shooter) {
        NEXT_OK.remove(new HandKey(shooter.getUUID(), false));
        NEXT_OK.remove(new HandKey(shooter.getUUID(), true));
    }

    /** Milliseconds since {@code shooter} last fired, or a long time if they have not (server). */
    public static long msSinceShot(Player shooter) {
        Long last = LAST_SHOT.get(shooter.getUUID());
        return last == null ? Long.MAX_VALUE / 2 : System.currentTimeMillis() - last;
    }

    private GunShots() {
    }

    /**
     * Whether {@code shooter} may fire now, and if so book the next slot. The cycle is wall-clock
     * (spec 7.1); the tolerance is for network jitter until timestamp sync replaces it.
     */
    public static boolean accept(LivingEntity shooter, GunFigures figures, long nowMs) {
        return accept(shooter, figures, nowMs, InteractionHand.MAIN_HAND);
    }

    public static boolean accept(LivingEntity shooter, GunFigures figures, long nowMs, InteractionHand hand) {
        // Early by at most a quarter of a cycle, or the jitter allowance if that is less: a
        // client hacked to fire a little early gains only that, and an honest one never trips it.
        long cycleMs = io.github.soundgoodizerfan.feedback.core.Haste.time(figures.cycleMs(), shooter);
        long tolerance = Math.min(GunTuning.GUN_FIRE_TOLERANCE_MS, cycleMs / 4);
        HandKey key = new HandKey(shooter.getUUID(), hand == InteractionHand.OFF_HAND);
        long earliest = NEXT_OK.getOrDefault(key, 0L);
        if (nowMs + tolerance < earliest)
            return false;
        NEXT_OK.put(key, Math.max(earliest, nowMs) + cycleMs);
        return true;
    }

    /**
     * A trigger pull on the server: the round the gun's own record says is next, in the figures of
     * this gun, the cycle validated, the round spent, overpressure booked, and the shot launched.
     * The client names only the aim; it never says what is chambered.
     *
     * @return whether a shot left the muzzle.
     */
    /**
     * A shot the server decided on (a cast, a free shot after a stagger), not one the shooter's client fired: the client
     * plays its own shot's sound at once and the server leaves the shooter out of it, so a server-made shot must be heard
     * by the shooter too, or it is silent to them.
     */
    public static boolean triggerHeard(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                       float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        hearSelf = true;
        try {
            return trigger(level, shooter, gun, yaw, pitch, nowMs, aimed, hand);
        } finally {
            hearSelf = false;
        }
    }

    /** Set only inside {@link #triggerHeard}; the server thread is the only one that fires, so a flag is enough. */
    private static boolean hearSelf;
    /** Set only inside {@link #triggerFromClient}: the shooter's own client fired this shot and kicked its own view, so the server sends no kick. */
    private static boolean clientShot;
    /** Set only inside {@link #triggerTurned}: the cast turned the shooter's camera to the target itself, so a view kick would fight the turn. */
    private static boolean cameraTurned;

    /**
     * A shot the shooter's own client fired (the fire payload): the client already kicked its view for it. Every other
     * player shot is the server's (a cast, a free shot after a stagger) and sends the kick, so a skill that shoots a gun
     * kicks and heats the gun as a pull of the trigger does.
     */
    public static boolean triggerFromClient(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                            float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        clientShot = true;
        try {
            return trigger(level, shooter, gun, yaw, pitch, nowMs, aimed, hand);
        } finally {
            clientShot = false;
        }
    }

    /**
     * A server-made shot, heard by the shooter, from a cast that turns the shooter's camera to the target itself
     * (Mark and fire): it sends no view kick, since the camera is already moving to the mark. Heat, wear and recoil
     * impulse apply as for any shot.
     */
    public static boolean triggerTurned(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                        float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        cameraTurned = true;
        try {
            return triggerHeard(level, shooter, gun, yaw, pitch, nowMs, aimed, hand);
        } finally {
            cameraTurned = false;
        }
    }

    public static boolean trigger(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs) {
        return trigger(level, shooter, gun, yaw, pitch, nowMs, false);
    }

    public static boolean trigger(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs, boolean aimed) {
        return trigger(level, shooter, gun, yaw, pitch, nowMs, aimed, InteractionHand.MAIN_HAND);
    }

    public static boolean trigger(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        GunMechanism mechanism = GunItem.mechanismOf(gun);
        return mechanism != null && mechanism.trigger(level, shooter, gun, yaw, pitch, nowMs, aimed, hand);
    }

    /** A firearm's pull ({@link FirearmMechanism}): see {@link #trigger}. */
    static boolean triggerFirearm(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null || GunHazard.isSpent(gun))
            return false;
        if (inStroke(shooter, gun, nowMs))
            return false;
        reloadStop(shooter); // a pull ends a reload where it is
        var next = GunAmmo.next(gun);
        if (next.isEmpty())
            return false;
        boolean both = shooter.isCrouching() && GunAmmo.count(gun) >= 2 && ModularTraits.has(gun, "double_barrel");
        boolean fired = shoot(level, shooter, gun, design, next.get(), yaw, pitch, nowMs, false, aimed, false, hand);
        // A double gun's sneak-pull fires both barrels together: the second is the same pull, so no cycle of its
        // own and no kick of its own (the client kicks once), and it is the other barrel's figures.
        if (fired && both)
            GunAmmo.next(gun).ifPresent(second -> shoot(level, shooter, gun, design, second, yaw, pitch, nowMs, false, aimed, true, hand));
        return fired;
    }

    /**
     * A mob's trigger pull: the round the gun's own record says is next, fired along the mob's own look.
     * Same {@link #shoot} as a player's -- same figures, same hazard, same swept shot -- with the cycle
     * counted in game ticks ({@code nowGameTick} * 50 as the ms the gate reads).
     *
     * @return whether a shot left the muzzle.
     */
    public static boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick) {
        return triggerMob(level, mob, gun, nowGameTick, mob.getYHeadRot(), mob.getXRot());
    }

    /** As above, along {@code yaw}/{@code pitch}: the goal's aim, which it corrects for drop. */
    public static boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        GunMechanism mechanism = GunItem.mechanismOf(gun);
        return mechanism != null && mechanism.triggerMob(level, mob, gun, nowGameTick, yaw, pitch);
    }

    /** A mob's firearm pull ({@link FirearmMechanism}): see {@link #triggerMob}. */
    static boolean triggerMobFirearm(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null || GunHazard.isSpent(gun))
            return false;
        var next = GunAmmo.next(gun);
        return next.isPresent()
                && shoot(level, mob, gun, design, next.get(), yaw, pitch, nowGameTick * 50L, false, false, false, InteractionHand.MAIN_HAND);
    }

    /**
     * The common path of a pull and a cook-off: figures for this round in this gun as it is now,
     * cycle checked, round spent, overpressure and heat and wear booked, shot launched.
     */
    private static boolean shoot(ServerLevel level, LivingEntity shooter, net.minecraft.world.item.ItemStack gun,
                                 GunSpec design, Cartridge round, float yaw, float pitch, long nowMs, boolean cookOff,
                                 boolean aimed, boolean together, InteractionHand hand) {
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var base = design.firing(GunAmmo.count(gun)).bake(materials, round);
        var load = round.load(materials);
        if (base.isEmpty() || load.isEmpty())
            return false;
        float heat = GunCondition.heatFraction(gun, level, load.get().ignitionTu());
        GunFigures figures = base.get().aged(GunCondition.condition(gun), GunFu.bloomHeat(shooter, heat));
        // Traits that read the shooter's hands and the dual-wield rule are a player's; a mob fires the bake as it stands.
        if (shooter instanceof Player player) {
            figures = GunFu.rolled(player, ModularTraits.gunAimed(player, gun, figures));
            if (GunItem.dualWielding(player))
                figures = figures.oneHanded();
        }
        if (!together && !accept(shooter, figures, nowMs, hand))
            return false;
        // A box or belt feed whose lip is bent by the recoil jams: the pull is spent and nothing fires.
        if (!cookOff && (design.layout().feed() == GunLayout.Feed.BOX || design.layout().feed() == GunLayout.Feed.BELT)
                && level.random.nextFloat() < HandlingPhysics.jamChance(
                        design.parts().feed().map(materials::get).orElse(null), figures.impulse())) {
            if (shooter instanceof Player jammed)
                jammed.displayClientMessage(net.minecraft.network.chat.Component.translatable("gun.feedback.jam"), true);
            return false;
        }
        LAST_SHOT.put(shooter.getUUID(), System.currentTimeMillis());
        GunAmmo.spend(gun);
        // A flintlock's case is not rated: the barrel holds the pressure.
        float caseRating = design.layout().action() == GunLayout.Action.FLINTLOCK ? Float.MAX_VALUE
                : round.caseRating(materials).map(p -> p.value()).orElse(Float.MAX_VALUE);
        float noise = 1 + (level.random.nextFloat() * 2 - 1) * GunTuning.GUN_HAZARD_NOISE;
        GunHazard.apply(level, shooter, gun, GunHazard.assess(figures.peakPressure().value(),
                figures.chamberRating().value(), caseRating, noise));
        GunCondition.afterShot(gun, figures, level, heat, round.boreWear(materials));
        // A fitting the barrel's heat has taken past its metal's melting point runs off the gun.
        for (GunOption lostFitting : FittingHeat.melt(gun, level))
            if (shooter instanceof Player holder)
                holder.displayClientMessage(net.minecraft.network.chat.Component.translatable("gun.feedback.fitting_melted",
                        net.minecraft.network.chat.Component.translatable(lostFitting.langKey())), true);
        if (shooter instanceof Player attuned)
            io.github.soundgoodizerfan.feedback.modular.attunement.PiezoKinds.fired(attuned, gun, figures);
        io.github.soundgoodizerfan.feedback.modular.attunement.RecallKinds.refund(gun, level, round);
        var projectile = materials.get(round.slugMaterial());
        fire(level, shooter, figures.withSpread(GunStance.of(shooter, aimed) * round.shape().spread()
                        * io.github.soundgoodizerfan.feedback.modular.attunement.MomentumKinds.braceSpread(shooter, gun)), yaw, pitch,
                round.tracer(materials), round.incendiary(materials), round.pellets(),
                round.hardness(materials), gun,
                round.blast(materials), RubberRounds.isRubber(projectile), Flight.of(round, materials));
        recoilCancel(level, shooter, round.payloadPerks(materials).get(io.github.soundgoodizerfan.feedback.process.Material.Perk.BOUNCE), yaw, pitch);
        GunFu.shotFired(level, shooter, gun, figures, heat);
        if (shooter instanceof net.minecraft.server.level.ServerPlayer server) {
            ModularTraits.gunFired(server, gun, figures);
            // A shot the server made (a cook-off, a cast, a free shot) kicks the view as the player's own pull would, unless the cast turns the camera itself.
            if (cookOff || !together && !clientShot && !cameraTurned)
                io.github.soundgoodizerfan.feedback.net.GunKickPayload.send(server, hand == InteractionHand.OFF_HAND, figures);
            String designation = GunSpec.of(gun).flatMap(spec -> GunDesignation.of(gun, spec)).orElse("");
            var built = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.design(gun);
            var grip = built == null ? null : built.part("G");
            if (GunSpec.of(gun).map(spec -> spec.layout().type().getSerializedName()).orElse("").equals("flintlock"))
                io.github.soundgoodizerfan.feedback.core.Awards.give(server, "combat/remember_remember");
            String advancement = designation.equals("big_iron") ? "combat/big_iron"
                    : designation.equals("long_ass_gun") ? "combat/long_ass_gun"
                    : designation.equals("assault_rifle") && grip != null && grip.variant().equals("pistol_grip") ? "combat/this_is_very_illegal"
                    : null;
            if (advancement != null) {
                var holder = server.server.getAdvancements().get(io.github.soundgoodizerfan.feedback.Feedback.id(advancement));
                if (holder != null)
                    server.getAdvancements().award(holder, "done");
            }
        }
        if (!together && shooter instanceof net.minecraft.server.level.ServerPlayer server)
            GunStrain.shot(server, gun, hand, figures.kickDeg(), aimed);
        if (cookOff) {
            float hp = GunHazard.cookOffHp(round.charge(), noise);
            if (shooter instanceof Player player)
                io.github.soundgoodizerfan.feedback.net.AimPayloads.cue(player, io.github.soundgoodizerfan.feedback.net.AimPayloads.GUN, net.minecraft.network.chat.Component.translatable("message.feedback.gun.cook_off"), 50);
            shooter.hurt(level.damageSources().explosion(null, null), hp);
        }
        chainFire(level, shooter, gun, design, noise);
        return true;
    }

    /**
     * What a shot books once it has left a gun that is not a firearm (an electric gun): the shot's impulse and the traits'
     * hearing, the view kick the shooter's own client has not already made, and the strain on the hand. A firearm books the same
     * inside {@link #shoot}.
     */
    public static void bookShot(ServerLevel level, LivingEntity shooter, ItemStack gun, GunFigures figures, float heat,
                                InteractionHand hand, boolean aimed) {
        figures = GunFu.rolled(shooter, figures);
        GunFu.shotFired(level, shooter, gun, figures, heat);
        if (shooter instanceof net.minecraft.server.level.ServerPlayer server) {
            ModularTraits.gunFired(server, gun, figures);
            if (!clientShot && !cameraTurned)
                io.github.soundgoodizerfan.feedback.net.GunKickPayload.send(server, hand == InteractionHand.OFF_HAND, figures);
            GunStrain.shot(server, gun, hand, figures.kickDeg(), aimed);
        }
    }

    /**
     * A lever's tube stacks rounds nose to primer, and the shot's recoil drives each bullet back onto the primer ahead of
     * it: behind a pointed nose the round in front goes off inside the tube. It hurts the holder and the gun, and the
     * round is gone. Flat noses and round noses are what such a tube is made for. Misuse only: a tube of safe rounds never does it.
     */
    private static void chainFire(ServerLevel level, LivingEntity shooter, net.minecraft.world.item.ItemStack gun, GunSpec design, float noise) {
        if (design.layout().action() != GunLayout.Action.LEVER || design.layout().feed() != GunLayout.Feed.TUBE)
            return;
        boolean chambered = gun.has(FWeapons.CHAMBER.get());
        var rounds = GunAmmo.magazine(gun).rounds();
        int n = rounds.size(), total = n + (chambered ? 1 : 0);
        for (int j = 1; j < total; j++) {
            Cartridge behind = rounds.get(chambered ? n - j : n - 1 - j);
            if (behind.shape() != BulletShape.POINTED)
                continue;
            Cartridge ahead = j == 1 && chambered ? gun.get(FWeapons.CHAMBER.get()) : rounds.get(chambered ? n - (j - 1) : n - j);
            if (j == 1 && chambered) {
                gun.remove(FWeapons.CHAMBER.get());
                GunAmmo.feed(gun);
            } else {
                var left = new java.util.ArrayList<>(rounds);
                left.remove(chambered ? n - (j - 1) : n - j);
                gun.set(FWeapons.MAGAZINE.get(), new Magazine(left));
            }
            gun.set(FWeapons.GUN_DAMAGE.get(), GunHazard.damage(gun) + GunTuning.GUN_CHAIN_FIRE_GUN_DAMAGE);
            if (shooter instanceof Player player)
                io.github.soundgoodizerfan.feedback.net.AimPayloads.cue(player, io.github.soundgoodizerfan.feedback.net.AimPayloads.GUN, net.minecraft.network.chat.Component.translatable("message.feedback.gun.chain_fire"), 50);
            level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                    io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_COOK_OFF.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
            shooter.hurt(level.damageSources().explosion(null, null), GunHazard.cookOffHp(ahead.charge(), noise));
            return;
        }
    }

    /**
     * Cook-off: the chambered round, in a barrel hotter than its propellant ignites at, goes off
     * unasked, in the direction the holder faces, and hurts them. It is gated by the gun's own
     * cycle, so a barrel that stays hot runs away at the rate the gun cycles until the rounds
     * are out or the player takes the round out of the chamber. Nothing here is a feature; it is
     * the round's ignition temperature meeting the barrel's (spec 3.4).
     *
     * @return whether a round cooked off.
     */
    public static boolean cookOff(ServerLevel level, Player holder, net.minecraft.world.item.ItemStack gun, long nowMs) {
        GunSpec design = GunSpec.of(gun).orElse(null);
        Cartridge round = gun.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.CHAMBER.get());
        if (design == null || round == null || GunHazard.isSpent(gun) || !ModularTraits.cooksOff(gun))
            return false;
        var load = round.load(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials());
        if (load.isEmpty()
                || GunCondition.temperature(gun, level).value() < load.get().ignitionTu())
            return false;
        // The server made this shot, so the holder's client played no sound of its own: let them hear it.
        hearSelf = true;
        try {
            return shoot(level, holder, gun, design, round, holder.getYRot(), holder.getXRot(), nowMs, true, false, false, InteractionHand.MAIN_HAND);
        } finally {
            hearSelf = false;
        }
    }

    /** A reload in progress: which gun, in which slot, and when its next beat is due. */
    private record Reload(ItemStack gun, InteractionHand hand, int slot, boolean byRound, long dueMs, long stepMs) {
    }

    private static final Map<UUID, Reload> RELOADS = new HashMap<>();

    /** A cylinder's loading stroke: the gun cannot fire until it ends, however many rounds are already in. */
    private record Stroke(ItemStack gun, long untilMs) {
    }

    private static final Map<UUID, Stroke> STROKES = new HashMap<>();

    /** Whether {@code gun} is mid loading stroke in {@code player}'s hands (a revolver with its cylinder open and the free hand at work). */
    private static boolean inStroke(Player player, ItemStack gun, long nowMs) {
        Stroke s = STROKES.get(player.getUUID());
        if (s == null)
            return false;
        if (nowMs >= s.untilMs()) {
            STROKES.remove(player.getUUID());
            return false;
        }
        return s.gun() == gun;
    }

    /**
     * R went down on the held gun: a reload starts, and it lasts only while R is held (the
     * author's ruling, 2026-10-14). A gun with its own feed (barrels, cylinder, tube) takes a loose
     * round of its bore a beat at a time, so letting go keeps what is in; one fed from a magazine
     * item swaps for the fullest one of its bore and feed only once R has been held the swap's
     * whole time, so letting go early keeps the old one. Firing, or putting the gun away, ends it.
     */
    /**
     * A gun in each hand is held out of the way of the other: a reload takes {@link GunTuning#GUN_DUAL_RELOAD_MULT} times
     * as long, less the share of the extra that {@link Figure#DUAL_RELOAD} (Twin Reload) takes off.
     */
    public static float reloadScale(Player player) {
        float ready = (1f - Mth.clamp(Skills.modifier(player, Figure.PRACTISED_RELOAD), 0f, 0.5f));
        if (!(player.getMainHandItem().getItem() instanceof GunItem && player.getOffhandItem().getItem() instanceof GunItem))
            return ready;
        float relief = Mth.clamp(Skills.modifier(player, Figure.DUAL_RELOAD), 0f, 1f);
        return ready * (1f + (GunTuning.GUN_DUAL_RELOAD_MULT - 1f) * (1f - relief));
    }

    /** Whether a reload would put anything into {@code gun}: a round of its bore with room, or a fuller magazine than it has. */
    private static boolean loadable(Player player, ItemStack gun) {
        GunSpec design = gun.getItem() instanceof GunItem ? GunSpec.of(gun).orElse(null) : null;
        if (design == null)
            return false;
        if (design.internalCapacity().isPresent())
            return !GunAmmo.loadable(player.getInventory(), gun, design).isEmpty();
        int best = bestMagazine(player, design);
        return best >= 0 && (!gun.has(FWeapons.MAGAZINE_SIZE.get())
                || MagazineItem.rounds(player.getInventory().getItem(best)).rounds().size() > GunAmmo.magazine(gun).rounds().size());
    }

    /** The hand whose gun is emptiest of those a reload could still fill, or null: so 0 of 11 comes before 10 of 11. */
    private static InteractionHand neediest(Player player) {
        InteractionHand pick = null;
        float least = Float.MAX_VALUE;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack gun = player.getItemInHand(hand);
            if (!loadable(player, gun))
                continue;
            float fill = GunAmmo.count(gun) / (float) GunAmmo.capacity(gun);
            if (fill < least) {
                least = fill;
                pick = hand;
            }
        }
        return pick;
    }

    /** {@code baseMs} of reload as this player does it: scaled by their skills and Haste, and cut to what a roll has left (Spent-Case Draw). */
    private static long scaled(Player player, long baseMs) {
        long ms = io.github.soundgoodizerfan.feedback.core.Haste.time(Math.round(baseMs * reloadScale(player)), player);
        // Spent-Case Draw: reloading mid-roll, the roll's own remaining time is the beat if that is shorter.
        var phase = io.github.soundgoodizerfan.feedback.combat.state.CombatState.of(player);
        if ((phase.phase() == io.github.soundgoodizerfan.feedback.combat.state.CombatPhase.ROLL
                || phase.phase() == io.github.soundgoodizerfan.feedback.combat.state.CombatPhase.ROLL_RECOVERY)
                && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(player, "gun_fu_spent_case_draw"))
            ms = Math.min(ms, (phase.ticksLeft() + (phase.phase() == io.github.soundgoodizerfan.feedback.combat.state.CombatPhase.ROLL ? phase.then() : 0)) * 50L);
        return ms;
    }

    /** Whether {@code gun}'s rounds go in one at a time, each as the free hand seats it (a tube, a cylinder). */
    private static boolean oneAtATime(GunSpec spec) {
        return spec != null && (spec.layout().feed() == GunLayout.Feed.TUBE || spec.layout().feed() == GunLayout.Feed.CYLINDER);
    }

    /**
     * A cylinder is loaded in the stroke the client draws: the cylinder opens, the hand takes every round, and each goes in at the middle of its
     * slot of the loading window (0.22 to 0.8 of the reload, the first fifth of it spent fetching, the last tenth leaving).
     * Returns {first round's delay, gap between rounds, the whole stroke}; null for a gun that does not load this way.
     */
    private static long[] cylinderBeats(Player player, ItemStack gun) {
        GunSpec spec = GunSpec.of(gun).orElse(null);
        if (spec == null || spec.layout().feed() != GunLayout.Feed.CYLINDER)
            return null;
        int n = GunAmmo.loadable(player.getInventory(), gun, spec).size();
        if (n <= 0)
            return null;
        long total = scaled(player, GunAmmo.roundMs(spec) * n);
        return new long[]{Math.round(total * (0.22 + 0.58 * (0.2 + 0.7 * 0.5 / n))), Math.max(1, Math.round(total * 0.58 * 0.7 / n)), total};
    }

    /** How long one beat of {@code gun}'s reload takes now: a round, a magazine swap, or (barrels, a single chamber) the whole stroke. */
    private static long beatMs(Player player, ItemStack gun) {
        GunSpec spec = GunSpec.of(gun).orElse(null);
        // A tube is thumbed a round a beat; barrels or a single chamber are loaded and shut in one stroke.
        long base = spec == null ? GunTuning.GUN_RELOAD_ROUND_MS : spec.internalCapacity().isPresent()
                ? GunAmmo.roundMs(spec) * (spec.layout().feed() == GunLayout.Feed.TUBE ? 1 : Math.max(1, GunAmmo.loadable(player.getInventory(), gun, spec).size()))
                : GunAmmo.reloadMs(gun);
        return scaled(player, base);
    }

    /** Start a reload of whichever held gun needs it most; it carries on to the other while R is held. */
    public static void reloadStart(Player player, long nowMs) {
        if (GunFu.hotClear(player))
            return;
        InteractionHand hand = player.isSpectator() || RELOADS.containsKey(player.getUUID()) ? null : neediest(player);
        if (hand == null)
            return;
        ItemStack gun = player.getItemInHand(hand);
        boolean byRound = GunSpec.of(gun).map(d -> d.internalCapacity().isPresent()).orElse(true);
        // A gun still cycling from its last shot cannot be reloaded until it could fire again.
        long ready = Math.max(nowMs, NEXT_OK.getOrDefault(new HandKey(player.getUUID(), hand == InteractionHand.OFF_HAND), 0L));
        long[] cyl = cylinderBeats(player, gun);
        if (cyl != null)
            STROKES.put(player.getUUID(), new Stroke(gun, nowMs + cyl[2]));
        RELOADS.put(player.getUUID(), new Reload(gun, hand, player.getInventory().selected, byRound, ready + (cyl != null ? cyl[0] : beatMs(player, gun)),
                cyl != null ? cyl[1] : 0L));
        player.level().playSound(player, player.getX(), player.getEyeY(), player.getZ(),
                io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_RELOAD_START.get(), SoundSource.PLAYERS, 0.6f, 1f);
    }

    /** R let go: a round-by-round reload stops with what is in, a magazine swap does not happen. */
    public static void reloadStop(Player player) {
        RELOADS.remove(player.getUUID());
        STROKES.remove(player.getUUID());
    }

    /** Whether {@code player} is reloading now. */
    public static boolean reloading(Player player) {
        return RELOADS.containsKey(player.getUUID());
    }

    /** Advance a held reload: a round per beat, or the swap once its time is up, then on to the neediest gun. */
    public static void reloadTick(Player player, long nowMs) {
        Reload r = RELOADS.get(player.getUUID());
        if (r == null)
            return;
        // Put away, or another thing in hand: the reload ends where it is.
        if (player.getInventory().selected != r.slot() || player.getItemInHand(r.hand()) != r.gun()) {
            RELOADS.remove(player.getUUID());
            STROKES.remove(player.getUUID());
            return;
        }
        long due = r.dueMs();
        ItemStack gun = r.gun();
        InteractionHand hand = r.hand();
        boolean byRound = r.byRound();
        long step = r.stepMs();
        while (nowMs >= due) {
            GunSpec design = GunSpec.of(gun).orElse(null);
            if (design == null)
                break;
            if (!byRound) {
                swapMagazine(player, gun, design);
            } else if (GunAmmo.loadLoose(player.getInventory(), gun, design, oneAtATime(design) ? 1 : Integer.MAX_VALUE) == 0) {
                RELOADS.remove(player.getUUID());
                STROKES.remove(player.getUUID());
                return;
            } else {
                player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                        io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_RELOAD_ROUND.get(), SoundSource.PLAYERS, 0.6f, 0.95f + player.getRandom().nextFloat() * 0.1f);
            }
            ModularTraits.gunReloaded(player, gun);
            // Whichever gun is now emptiest gets the next beat; nothing left to fill ends the reload.
            hand = neediest(player);
            if (hand == null) {
                RELOADS.remove(player.getUUID());
                return;
            }
            gun = player.getItemInHand(hand);
            byRound = GunSpec.of(gun).map(d -> d.internalCapacity().isPresent()).orElse(true);
            long[] cyl = cylinderBeats(player, gun);
            step = cyl != null ? cyl[1] : 0L;
            due += cyl != null ? cyl[1] : beatMs(player, gun);
        }
        RELOADS.put(player.getUUID(), new Reload(gun, hand, r.slot(), byRound, due, step));
    }

    /** A gun knocked or dropped from a hand is baked afresh as it is picked up, so it never comes back stale or unseen. */
    @SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        ItemStack picked = event.getItemEntity().getItem();
        if (!event.getPlayer().level().isClientSide && picked.getItem() instanceof io.github.soundgoodizerfan.feedback.weapon.GunItem)
            rebakeGun(picked);
    }

    private static void rebakeGun(ItemStack gun) {
        io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.rebake(gun);
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide && !RELOADS.isEmpty())
            reloadTick(event.getEntity(), System.currentTimeMillis());
    }

    /** The inventory slot of the fullest magazine of this gun's bore and feed, or -1. */
    private static int bestMagazine(Player player, GunSpec design) {
        var inventory = player.getInventory();
        int best = -1, most = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var s = inventory.getItem(i);
            if (!s.is(FWeapons.MAGAZINE_ITEM.get()) || GunAmmo.size(s).feed() != design.layout().feed())
                continue;
            Magazine m = MagazineItem.rounds(s);
            if (!m.isEmpty() && m.boreMm() != design.boreMm())
                continue;
            if (best < 0 || m.rounds().size() > most) {
                best = i;
                most = m.rounds().size();
            }
        }
        return best;
    }

    /**
     * Swap the gun's magazine for the fullest one of its bore and feed (any box for a box gun, a
     * belt for a belt gun); the old one comes back as the magazine it was. No magazine in, none out.
     */
    private static void swapMagazine(Player player, ItemStack gun, GunSpec design) {
        var inventory = player.getInventory();
        int best = bestMagazine(player, design);
        if (best < 0)
            return;
        var incoming = inventory.getItem(best);
        MagazineSize was = gun.get(FWeapons.MAGAZINE_SIZE.get());
        var outgoing = was == null ? ItemStack.EMPTY : new ItemStack(FWeapons.MAGAZINE_ITEM.get());
        if (was != null) {
            outgoing.set(FWeapons.MAGAZINE_SIZE.get(), was);
            if (!GunAmmo.magazine(gun).isEmpty())
                outgoing.set(FWeapons.MAGAZINE.get(), GunAmmo.magazine(gun));
        } else // rounds with no magazine round them (a gun from before sizes) come back loose
            GunAmmo.magazine(gun).rounds().forEach(r -> GunAmmo.give(player, GunAmmo.loose(r)));
        gun.set(FWeapons.MAGAZINE.get(), MagazineItem.rounds(incoming));
        gun.set(FWeapons.MAGAZINE_SIZE.get(), GunAmmo.size(incoming));
        GunAmmo.feed(gun); // a magazine put in under an empty chamber chambers its top round
        inventory.setItem(best, outgoing);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_RELOAD_MAGAZINE.get(), SoundSource.PLAYERS, 0.7f, 1f);
    }

    /**
     * Launch a shot along {@code yaw}/{@code pitch} (degrees, the shooter's own view), widened by
     * the gun's spread. The aim comes from the client so the shot leaves where the player was
     * looking when they pressed, not where the server last heard they were.
     */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch) {
        fire(level, shooter, f, yaw, pitch, false, false);
    }

    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, 1);
    }

    /** As above, as {@code pellets} pellets of shot sharing the projectile, each on its own line through the cone. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, GunTuning.GUN_DEFAULT_PROJECTILE_HARDNESS);
    }

    /** As above, of a projectile of this hardness: what it glances off and what armour it goes through. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, hardness, ItemStack.EMPTY);
    }

    /** As above, from {@code gun}: its traits hear where each round lands. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness, ItemStack gun) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, hardness, gun, 0f, false);
    }

    /** As above, with an explosion of {@code blast} power where each pellet stops. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness, ItemStack gun, float blast) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, hardness, gun, blast, false);
    }

    /** As above, and {@code rubber} marks each pellet a rubber round. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness, ItemStack gun, float blast,
                            boolean rubber) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, hardness, gun, blast, rubber, Flight.PLAIN);
    }

    /** As above, with the round's {@link Flight}: what its shape and payload change. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness, ItemStack gun, float blast,
                            boolean rubber, Flight flight) {
        GunFigures each = pellets > 1 ? f.asPellet(pellets) : f;
        Vec3 muzzle = shooter.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(0.4));
        // Dead Eye: the shot is exact, and its round is harder than it was (it goes through more armour).
        boolean exact = shooter instanceof net.minecraft.server.level.ServerPlayer sp
                && io.github.soundgoodizerfan.feedback.combat.cast.DeadEye.consume(sp);
        int bend = io.github.soundgoodizerfan.feedback.modular.attunement.LodeKinds.bend(gun, level);
        boolean wet = !gun.isEmpty() && io.github.soundgoodizerfan.feedback.modular.attunement.TideKinds.level(gun, "wet_round") > 0;
        for (int k = 0; k < pellets; k++) {
            float r = exact ? 0f : each.spreadDeg() * Mth.sqrt(level.random.nextFloat());
            float theta = level.random.nextFloat() * Mth.TWO_PI;
            float aimPitch = Mth.clamp(pitch + r * Mth.sin(theta), -90f, 90f);
            float aimYaw = yaw + r * Mth.cos(theta);
            Vec3 dir = Vec3.directionFromRotation(aimPitch, aimYaw);
            Shot shot = new Shot(shooter, shooter.getEyePosition().add(dir.scale(0.4)), dir.scale(each.muzzleSpeed()), each);
            shot.tracer = tracer;
            shot.incendiary = incendiary;
            shot.blast = blast;
            shot.rubber = rubber;
            shot.hardness = exact ? hardness * 2f : hardness;
            shot.gun = gun.copyWithCount(1);
            shot.flight = flight;
            shot.bend = bend;
            shot.keepsSpeedWet = wet;
            shot.argb = incendiary ? 0xFFFF8A2A : tracer ? 0xFFFFD27A : 0x70FFF0C8;
            shot.dragMult = flight.drag();
            io.github.soundgoodizerfan.feedback.net.GunTracerPayload.send(level, shot.id, false, shot.pos, shot.velocity, shot.argb, shot.dragMult);
            LIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(shot);
        }

        report(level, shooter, f, gun, muzzle);
    }

    /** What a shot sounds and shows at the muzzle: its sound (the shooter's own client already played it), the vibration, the flash or smoke. */
    public static void report(ServerLevel level, LivingEntity shooter, GunFigures f, ItemStack gun, Vec3 muzzle) {
        GunMechanism mechanism = GunItem.mechanismOf(gun);
        for (GunSound.Layer layer : mechanism != null ? mechanism.sound(gun, f, level.random) : GunSound.layers(f, level.random))
            level.playSound(shooter instanceof Player player && !hearSelf ? player : null, muzzle.x, muzzle.y, muzzle.z, layer.event(), SoundSource.PLAYERS,
                    layer.volume(), layer.pitch());
        level.gameEvent(shooter, f.report() < GunTuning.GUN_QUIET_REPORT ? FWeapons.GUNSHOT_QUIET
                : f.report() >= GunTuning.GUN_THUNDEROUS_REPORT ? FWeapons.GUNSHOT_THUNDEROUS : FWeapons.GUNSHOT, muzzle);
        // The burning gas shows as smoke at the muzzle.
        level.sendParticles(mechanism != null ? mechanism.muzzleParticle(gun, f) : f.muzzleParticle(), muzzle.x, muzzle.y, muzzle.z,
                3, 0.02, 0.02, 0.02, 0.01);
        if (mechanism instanceof FirearmMechanism)
            GunImpacts.muzzle(level, muzzle, shooter.getLookAngle(), Math.min(1f, f.report()), ejectsCase(gun));
    }

    /** A gun that works its action by the shot throws its case out: not a break action or a flintlock. */
    private static boolean ejectsCase(ItemStack gun) {
        return GunSpec.of(gun).map(g -> {
            GunLayout.Action a = g.layout().action();
            return a != GunLayout.Action.BREAK && a != GunLayout.Action.FLINTLOCK;
        }).orElse(false);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        List<Shot> shots = LIVE.get(level);
        if (shots == null)
            return;
        for (Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
            Shot s = it.next();
            if (!step(level, s)) {
                io.github.soundgoodizerfan.feedback.net.GunTracerPayload.send(level, s.id, true, s.stopAt != null ? s.stopAt : s.pos, s.velocity, s.argb, s.dragMult);
                it.remove();
            }
        }
        if (shots.isEmpty())
            LIVE.remove(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LIVE.clear();
        NEXT_OK.clear();
        LAST_SHOT.clear();
        RELOADS.clear();
        STROKES.clear();
    }

    private static final Map<net.minecraft.world.level.block.Block, Float> TOUGHNESS = new HashMap<>();
    private static final Map<net.minecraft.world.level.block.Block, Float> HARDNESS = new HashMap<>();
    private static final Map<net.minecraft.world.item.Item, Float> ARMOUR = new HashMap<>();
    private static int toughnessGeneration = -1;

    /** Flush the per-block and per-item caches when the material table has been reloaded. */
    private static void checkGeneration() {
        if (toughnessGeneration != io.github.soundgoodizerfan.feedback.process.MaterialTable.generation()) {
            TOUGHNESS.clear();
            HARDNESS.clear();
            ARMOUR.clear();
            toughnessGeneration = io.github.soundgoodizerfan.feedback.process.MaterialTable.generation();
        }
    }

    /** The hardness of the material an item is, or NaN if no material claims it. */
    private static float hardnessOf(net.minecraft.world.item.Item item) {
        return io.github.soundgoodizerfan.feedback.process.Material
                .find(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), item)
                .map(l -> l.material().hardness().value()).orElse(Float.NaN);
    }

    /**
     * The hardness of the material a block is; a block no material claims reads its vanilla
     * destroy time instead (the fallback Create Big Cannons takes, spec 3.6), so a pack's stone
     * still turns a bullet.
     */
    public static float hardnessOf(net.minecraft.world.level.block.Block block) {
        checkGeneration();
        return HARDNESS.computeIfAbsent(block, b -> {
            float h = hardnessOf(b.asItem());
            return Float.isNaN(h) ? b.defaultDestroyTime() : h;
        });
    }

    /**
     * The hardest armour a living thing wears, by the materials of the pieces (iron, diamond,
     * leather...), or zero if it wears none a material claims. Unknown armour is never pierced.
     */
    public static float armourHardness(net.minecraft.world.entity.LivingEntity target) {
        checkGeneration();
        float hardest = 0;
        for (ItemStack piece : target.getArmorSlots()) {
            if (piece.isEmpty())
                continue;
            // A modular piece is rated by its plate, which is on the stack; no item names a material.
            io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures modular = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.figures(piece);
            float h = modular != null ? modular.get(io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.HARDNESS)
                    : ARMOUR.computeIfAbsent(piece.getItem(), GunShots::hardnessOf);
            hardest = Math.max(hardest, Float.isNaN(h) ? Float.MAX_VALUE : h);
        }
        return hardest;
    }

    /**
     * The share of a hit that goes through armour: none from a bullet no harder than the armour,
     * and the rest by how much harder it is (spec 3.5, the author's first cut for entities).
     * Pure, so a test and the step agree.
     */
    public static float pierce(float bulletHardness, float armourHardness) {
        return armourHardness <= 0 || bulletHardness <= armourHardness ? 0f : 1f - armourHardness / bulletHardness;
    }

    /** Whether a blow of this momentum breaks a brittle block of this toughness. Pure. */
    public static boolean breaks(float toughness, float momentum) {
        return !Float.isNaN(breakCost(toughness)) && momentum >= toughness * GunTuning.GUN_BREAK_MOMENTUM_PER_TOUGHNESS;
    }

    /** Whether a bullet glances off: softer than the block, coming in shallow. Pure. */
    public static boolean glances(float bulletHardness, float blockHardness, float cosToNormal) {
        return bulletHardness < blockHardness && cosToNormal < GunTuning.GUN_RICOCHET_MAX_COS;
    }

    /**
     * The toughness of the material a block is, or NaN if no material claims it (and so nothing
     * breaks it). Cached per block and flushed on a reload, because finding a material by item is
     * a walk over the table.
     */
    public static float toughnessOf(net.minecraft.world.level.block.Block block) {
        checkGeneration();
        return TOUGHNESS.computeIfAbsent(block, b -> {
            var item = b.asItem();
            if (item == net.minecraft.world.item.Items.AIR)
                return Float.NaN;
            return io.github.soundgoodizerfan.feedback.process.Material
                    .find(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), item)
                    .flatMap(l -> l.material().mechanical().toughness())
                    .map(t -> t.value()).orElse(Float.NaN);
        });
    }

    /**
     * Work it costs a bullet to break a block of this toughness, or NaN if it will not break
     * (spec 3.5: only things like glass). Pure, so a test and the step agree.
     */
    public static float breakCost(float toughness) {
        return Float.isNaN(toughness) || toughness > GunTuning.GUN_BREAK_TOUGHNESS_MAX
                ? Float.NaN : toughness * GunTuning.GUN_BREAK_WORK_PER_TOUGHNESS;
    }

    /** A round has hit: the gun's traits hear where, on what, and with how much energy left. */
    private static void landed(ServerLevel level, Shot s, Entity target, Vec3 at) {
        if (!s.gun.isEmpty() && s.shooter instanceof Player player)
            ModularTraits.gunRoundLanded(level, player, s.gun, at, target, s.energy());
    }

    /** A hit from {@code shooter}: a player's attack, or a mob's. */
    public static net.minecraft.world.damagesource.DamageSource attack(ServerLevel level, LivingEntity shooter) {
        return shooter instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(shooter);
    }

    /** Whether {@code shooter}'s bullet may break or burn at {@code at}: a player by build rights, a mob by the mob-griefing rule. */
    private static boolean mayChange(ServerLevel level, LivingEntity shooter, BlockPos at) {
        return shooter instanceof Player player ? player.mayBuild() && level.mayInteract(player, at)
                : level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
    }

    /** Whether a bullet's hurt call is on the stack now: a damage listener reads this to tell a bullet from a blow (not by item or damage type). Server thread only. */
    private static boolean inBullet;

    public static boolean inBullet() {
        return inBullet;
    }

    /** Runs {@code hurt} as a bullet's hit: damage listeners see {@link #inBullet} true while it does (a pulse's hit is one). */
    public static void asBullet(Runnable hurt) {
        inBullet = true;
        try {
            hurt.run();
        } finally {
            inBullet = false;
        }
    }

    /**
     * A return shot (the Feedbacker punching a bullet back): a swept segment from {@code from} along the line to
     * {@code toward}, a little past it, hurting the first thing it meets for {@code damage}, attributed to
     * {@code owner}. It never goes through {@link #step}'s parry check, so a returned shot is not parried again.
     */
    public static void strike(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 toward, float damage) {
        Vec3 end = from.add(toward.subtract(from).normalize().scale(toward.distanceTo(from) + 1.0));
        BlockHitResult block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, owner, from, stop,
                new AABB(from, stop).inflate(1.0), e -> e.isPickable() && !e.isSpectator() && e != owner);
        if (hit == null)
            return;
        Entity target = hit.getEntity();
        target.invulnerableTime = 0;
        inBullet = true;
        try {
            target.hurt(attack(level, owner), damage);
        } finally {
            inBullet = false;
        }
    }

    /**
     * An explosive round going up where it stopped, once it is armed -- point blank it is a dud, which
     * is the safety that lets a gunner shoot at a wall beside them. It breaks blocks only where the
     * world lets its shooter change them.
     *
     * @return whether it went off.
     */
    private static boolean detonate(ServerLevel level, Shot s, Vec3 at) {
        if (s.blast < FTuning.DEMO_POWER_MIN || s.age < GunTuning.DEMO_ROUND_ARM_TICKS)
            return false;
        float power = s.blast;
        s.blast = 0;
        level.explode(s.shooter, at.x, at.y, at.z, power, false,
                mayChange(level, s.shooter, BlockPos.containing(at)) ? net.minecraft.world.level.Level.ExplosionInteraction.BLOCK
                        : net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        return true;
    }

    /**
     * What a payload does where the round lands, read from the payload material's own perks: a toxic one lets its fume cloud
     * go once (a gas round), a perk that works on a living thing is applied to it (poison, chill, cling, radiant, bounce, fire,
     * the echo's outline), an ender-like one hops it a few blocks, a sculk one pays a little experience for a kill. Hard
     * on the undead and on sea things is a damage bonus, added where the damage is worked out.
     *
     * @param target what was struck, or null for a block; {@code blockPos} and {@code face} are the block struck.
     */
    private static void payloadHit(ServerLevel level, Shot s, Vec3 at, Entity target, BlockPos blockPos, net.minecraft.core.Direction face) {
        var perks = s.flight.perks();
        // A vial breaks on the first thing the round meets, and what it held is let go there as a thrown flask lets it go.
        if (s.flight.vial() != null && !s.vialSpent) {
            s.vialSpent = true;
            io.github.soundgoodizerfan.feedback.potion.PotionEffects.impact(level, s.shooter, s.shooter, at, s.flight.vial(),
                    GunTuning.GUN_VIAL_REACH_SCALE * io.github.soundgoodizerfan.feedback.potion.PotionTuning.throwReach());
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 8, 0.2, 0.15, 0.2, 0.1);
        }
        var traits = s.flight.traits();
        // A strand trailed behind the round reels: the body it hit is drawn to the shooter, or the shooter to the block.
        if (traits.contains(io.github.soundgoodizerfan.feedback.process.Material.Trait.FIBROUS)) {
            Vec3 eye = s.shooter.getEyePosition();
            if (target instanceof LivingEntity struck)
                reel(struck, eye);
            else
                reel(s.shooter, at);
            for (int i = 1; i <= 12; i++) {
                Vec3 q = eye.add(at.subtract(eye).scale(i / 12.0));
                level.sendParticles(ParticleTypes.CRIT, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
        }
        // A sticky lump glues what it hits where it stands.
        if (traits.contains(io.github.soundgoodizerfan.feedback.process.Material.Trait.STICKY) && target instanceof LivingEntity struck)
            struck.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    GunTuning.GUN_ANCHOR_TICKS, GunTuning.GUN_ANCHOR_AMPLIFIER));
        // A seed that lands on a block it can grow in is planted there.
        if (s.flight.seed() != null && blockPos != null && face == net.minecraft.core.Direction.UP)
            plant(level, s, blockPos.above());
        if (blockPos != null && !s.gun.isEmpty())
            io.github.soundgoodizerfan.feedback.modular.attunement.BloomKinds.landed(level, s.shooter, s.gun, blockPos,
                    mayChange(level, s.shooter, blockPos));
        if (perks.isEmpty())
            return;
        int toxic = perks.getOrDefault(io.github.soundgoodizerfan.feedback.process.Material.Perk.TOXIC, 0);
        if (toxic > 0 && !s.gassed) {
            s.gassed = true;
            io.github.soundgoodizerfan.feedback.core.hazard.FumeCloud.release(level, at, toxic);
        }
        if (!(target instanceof LivingEntity struck))
            return;
        io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks.affect(perks, struck);
        Integer hop = perks.get(io.github.soundgoodizerfan.feedback.process.Material.Perk.BLINK);
        if (hop != null)
            for (int i = 0; i < 8; i++) {
                double reach = GunTuning.GUN_HOP_BLOCKS_PER_LEVEL * hop;
                if (struck.randomTeleport(struck.getX() + (level.random.nextDouble() - 0.5) * 2 * reach, struck.getY(),
                        struck.getZ() + (level.random.nextDouble() - 0.5) * 2 * reach, true))
                    break;
            }
        Integer siphon = perks.get(io.github.soundgoodizerfan.feedback.process.Material.Perk.SIPHON);
        if (siphon != null && !struck.isAlive())
            net.minecraft.world.entity.ExperienceOrb.award(level, struck.position(),
                    siphon * io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.PERK_SIPHON_XP_PER_LEVEL);
    }

    /**
     * A bouncing payload fired at a surface close by throws the shooter back along the line of aim, on the pull itself and not when the
     * round lands: the rocket jump. Aim down and the shooter goes up; aim ahead and they go back. Nothing close in line, nothing to push off.
     *
     * @param bounce the payload's bounce perk level, or null for a round without one.
     */
    private static void recoilCancel(ServerLevel level, LivingEntity shooter, Integer bounce, float yaw, float pitch) {
        if (bounce == null)
            return;
        Vec3 eye = shooter.getEyePosition(), look = Vec3.directionFromRotation(pitch, yaw);
        var wall = level.clip(new ClipContext(eye, eye.add(look.scale(GunTuning.GUN_BOUNCE_REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (wall.getType() == HitResult.Type.MISS)
            return;
        shooter.setDeltaMovement(shooter.getDeltaMovement().add(look.scale(-GunTuning.GUN_BOUNCE_LAUNCH * bounce)));
        shooter.hurtMarked = true;
        shooter.fallDistance = 0f;
    }

    /** Draws {@code body} toward {@code toward}: a push that is stronger the further it has to go, capped, with a little lift off the ground. */
    private static void reel(LivingEntity body, Vec3 toward) {
        Vec3 d = toward.subtract(body.position().add(0, body.getBbHeight() / 2, 0));
        double dist = d.length();
        if (dist < 1.5)
            return;
        body.setDeltaMovement(body.getDeltaMovement().add(d.normalize().scale(Math.min(GunTuning.GUN_TETHER_PULL, dist * 0.15))).add(0, 0.2, 0));
        body.hurtMarked = true;
        body.fallDistance = 0f;
    }

    /** Plants the seed a round carries at {@code pos} if that is empty and the crop can stand there; the round is the seed spent. */
    private static void plant(ServerLevel level, Shot s, BlockPos pos) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(s.flight.seed());
        if (item.isEmpty() || !(item.get() instanceof net.minecraft.world.item.BlockItem seed) || !level.isEmptyBlock(pos) || !mayChange(level, s.shooter, pos))
            return;
        var crop = seed.getBlock().defaultBlockState();
        if (!crop.canSurvive(level, pos))
            return;
        level.setBlockAndUpdate(pos, crop);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0);
    }

    /**
     * What a burning payload sets off where it lands: a lit charge's neighbours catch in a chain. Dynamite goes up, candles and
     * campfires light, where the world lets the shooter change things.
     */
    private static void fuse(ServerLevel level, Shot s, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
            if (!mayChange(level, s.shooter, p))
                continue;
            var state = level.getBlockState(p);
            if (state.getBlock() instanceof net.minecraft.world.level.block.TntBlock) {
                net.minecraft.world.level.block.TntBlock.explode(level, p);
                level.removeBlock(p, false);
            } else if (net.minecraft.world.level.block.CandleBlock.canLight(state) || net.minecraft.world.level.block.CampfireBlock.canLight(state))
                level.setBlock(p.immutable(), state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true), 11);
        }
    }

    /** One tick's travel. @return whether the shot is still flying. */
    private static boolean step(ServerLevel level, Shot s) {
        if (s.shooter.isRemoved() || ++s.age > GunTuning.GUN_MAX_LIFE_TICKS)
            return false;
        if (s.hold > 0) {
            if (--s.hold == GunFuTuning.COIN_HOLD_TICKS / 2)
                CoinShot.ring(level, s.pos);
            return true;
        }
        if (s.bend != 0)
            s.velocity = io.github.soundgoodizerfan.feedback.modular.attunement.LodeKinds.bend(level, s.shooter, s.pos, s.velocity, s.bend);
        Vec3 from = s.pos, to = from.add(s.velocity);

        // A coin the shooter flipped, struck by this round: the metal decides what it does (CoinShot).
        CoinShot.Result coin = CoinShot.resolve(level, s.shooter, from, to, s.massMu, s.hardness);
        if (coin != null) {
            s.pos = coin.point();
            s.hold = GunFuTuning.COIN_HOLD_TICKS;
            CoinShot.ring(level, s.pos);
            if (coin.direction() != null)
                s.velocity = coin.direction().scale(s.velocity.length());
            return s.spend(s.energy() * (1 - coin.keep() * coin.keep()));
        }

        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        io.github.soundgoodizerfan.feedback.combat.cast.PinDown.flyby(level, s.shooter, from, end);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, s.shooter, from, end,
                new AABB(from, end).inflate(1.0),
                e -> e.isPickable() && !e.isSpectator() && e != s.shooter && !s.pierced.contains(e.getId()));
        if (hit != null) {
            Entity target = hit.getEntity();
            float damage = s.energy() * GunTuning.GUN_DAMAGE_PER_WORK * s.flight.damage();
            // A payload that is hard on the undead or the sea's things hits them harder.
            if (target instanceof LivingEntity victim && !s.flight.perks().isEmpty())
                damage += io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks.bonus(s.flight.perks(), victim);
            // A Feedbacker in its parry window punches the round back instead of taking it.
            if (target instanceof LivingEntity catcher
                    && Parrying.catchBullet(level, s.shooter, catcher, s.massMu, (float) s.velocity.length(), damage))
                return false;
            // How much of the hit the target takes: its health. The bullet's surplus goes on
            // through ("surplus carries through rather than stopping politely", spec 3.5), so a
            // heavy round that kills a zombie is still flying behind it.
            float health = target instanceof net.minecraft.world.entity.LivingEntity living
                    ? living.getHealth() + living.getAbsorptionAmount() : damage;
            // Rapid fire must not be eaten by invulnerability frames; a gun is not a sword.
            target.invulnerableTime = 0;
            inBullet = true;
            try {
                // A bullet harder than the armour goes through a share of it, as its own damage that
                // armour does not reduce; the rest meets the armour as any hit does.
                float through = target instanceof net.minecraft.world.entity.LivingEntity living
                        ? pierce(s.hardness, armourHardness(living)) : 0f;
                if (through > 0) {
                    target.hurt(io.github.soundgoodizerfan.feedback.core.hazard.Hazards.source(level, BULLET_PIERCE, s.shooter), damage * through);
                    target.invulnerableTime = 0;
                }
                target.hurt(attack(level, s.shooter), damage * (1 - through));
            } finally {
                inBullet = false;
            }
            if (target instanceof net.minecraft.world.entity.monster.EnderMan && s.shooter instanceof net.minecraft.world.entity.player.Player shotBy)
                io.github.soundgoodizerfan.feedback.core.Awards.give(shotBy, "combat/dodge_this");
            if (s.rubber && target instanceof LivingEntity struck)
                RubberRounds.landed(s.shooter, struck);
            if (s.incendiary)
                target.igniteForSeconds(GunTuning.GUN_INCENDIARY_SECONDS);
            payloadHit(level, s, hit.getLocation(), target, null, null);
            if (s.incendiary && target instanceof net.minecraft.world.entity.monster.Creeper creeper)
                creeper.ignite();
            GunFu.bulletHit(level, s.shooter, target, s.impulse);
            landed(level, s, target, hit.getLocation());
            s.stopAt = hit.getLocation();
            GunImpacts.body(level, s.shooter, target, hit.getLocation(), s.velocity, damage,
                    target instanceof LivingEntity dead && dead.isDeadOrDying());
            if (detonate(level, s, hit.getLocation()))
                return false;
            if (damage <= health || !s.spend(health / (GunTuning.GUN_DAMAGE_PER_WORK * s.flight.damage())))
                return false;
            s.pierced.add(target.getId());
            s.pos = hit.getLocation();
            return true;
        }
        if (block.getType() != HitResult.Type.MISS) {
            BlockPos at = block.getBlockPos();
            Vec3 p = block.getLocation();
            var hitBlock = level.getBlockState(at).getBlock();
            float toughness = toughnessOf(hitBlock);
            float cost = breakCost(toughness);
            boolean allowed = !Float.isNaN(cost) && mayChange(level, s.shooter, at);
            // What breaks a brittle block is the blow, momentum; what it costs the bullet is work.
            if (allowed && breaks(toughness, s.momentum() * s.flight.ease()) && s.energy() > cost && s.spend(cost)) {
                // Glass breaks and the bullet goes on, a little slower.
                level.destroyBlock(at, false, s.shooter);
                s.pos = p.add(s.velocity.normalize().scale(0.05));
                return true;
            }
            // A soft bullet meeting a harder face at a shallow angle glances off it, slower.
            Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
            float cos = (float) Math.abs(s.velocity.normalize().dot(normal));
            if (s.bounces < GunTuning.GUN_RICOCHET_MAX && glances(s.hardness, hardnessOf(hitBlock), cos / s.flight.glance())
                    && s.spend(s.energy() * (1 - GunTuning.GUN_RICOCHET_KEEP))) {
                s.velocity = s.velocity.subtract(normal.scale(2 * s.velocity.dot(normal)));
                s.pos = p.add(normal.scale(0.05));
                s.bounces++;
                level.playSound(null, p.x, p.y, p.z, io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_RICOCHET.get(), SoundSource.PLAYERS, 0.8f, 0.85f + level.random.nextFloat() * 0.3f);
                GunImpacts.ricochet(level, p, s.velocity);
                io.github.soundgoodizerfan.feedback.net.GunTracerPayload.send(level, s.id, false, s.pos, s.velocity, s.argb, s.dragMult);
                return true;
            }
            s.stopAt = p;
            GunImpacts.block(level, at, block.getDirection(), p, (float) Math.sqrt(Math.min(1f, s.energy() / s.muzzleEnergy)));
            // The surface answers in its own voice: stone, wood, glass, dirt.
            level.playSound(null, p.x, p.y, p.z, level.getBlockState(at).getSoundType().getHitSound(), SoundSource.BLOCKS, 0.7f, 0.7f);
            landed(level, s, null, p);
            payloadHit(level, s, p, null, at, block.getDirection());
            if (s.incendiary)
                fuse(level, s, at);
            if (detonate(level, s, p.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(0.1))))
                return false;
            if (s.incendiary) {
                // Fire where it lands, if fire may be there and the world is allowed to burn.
                BlockPos firePos = at.relative(block.getDirection());
                if (level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)
                        && mayChange(level, s.shooter, firePos)
                        && net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, firePos, block.getDirection()))
                    level.setBlockAndUpdate(firePos, net.minecraft.world.level.block.BaseFireBlock.getState(level, firePos));
            }
            return false;
        }

        // The streak is drawn by the clients from the shot's tracer message; no particles here.
        // Still flying: drag in the medium it ends the tick in, then gravity.
        boolean inFluid = !level.getFluidState(BlockPos.containing(to)).isEmpty();
        float drag = inFluid && !s.keepsSpeedWet ? GunTuning.GUN_FLUID_DRAG_PER_TICK : GunTuning.GUN_DRAG_PER_TICK * s.flight.drag();
        s.velocity = s.velocity.scale(1 - drag).add(0, -GunTuning.GUN_GRAVITY_PER_TICK, 0);
        whiz(level, s, from, to);
        s.pos = to;
        return true;
    }

    /** A bullet passing within a few blocks of a player who did not fire it is heard once, as it goes by. */
    private static void whiz(ServerLevel level, Shot s, Vec3 from, Vec3 to) {
        Vec3 seg = to.subtract(from);
        double len2 = seg.lengthSqr();
        if (len2 < 1e-6)
            return;
        for (Player pl : level.players()) {
            if (pl == s.shooter || s.whizzed.contains(pl.getUUID()))
                continue;
            double t = Mth.clamp(pl.getEyePosition().subtract(from).dot(seg) / len2, 0, 1);
            Vec3 near = from.add(seg.scale(t));
            if (near.distanceToSqr(pl.getEyePosition()) > 9)
                continue;
            s.whizzed.add(pl.getUUID());
            level.playSound(null, near.x, near.y, near.z, io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_WHIZ.get(), SoundSource.PLAYERS, 1f, 0.9f + level.random.nextFloat() * 0.3f);
        }
    }
}
