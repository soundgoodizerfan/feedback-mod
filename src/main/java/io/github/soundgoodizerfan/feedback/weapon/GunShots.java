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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
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
 * The server's side of a shot ({@code docs/feedback_guns_spec.md} sections 7 and 7.1).
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

    private static final class Shot {
        final LivingEntity shooter;
        Vec3 pos;
        Vec3 velocity;
        final float muzzleSpeed;
        final float muzzleEnergy;
        int age;
        /** Entities this shot has already gone through, so it is not counted on one twice. */
        final java.util.Set<Integer> pierced = new java.util.HashSet<>();
        /** A luminous payload: the path is drawn bright. */
        boolean tracer;
        /** A flammable payload: what it hits is set alight. */
        boolean incendiary;
        /** Power of the explosion where it stops, or 0 (explosive filler). */
        float blast;
        /** The projectile's material hardness: what decides a glance off a block and a way through armour. */
        float hardness = FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS;
        final float massMu;
        /** Momentum of the round and gas at the muzzle, kg m/s: what Muzzle Stagger reads. */
        final float impulse;
        int bounces;
        /** The gun it came from, so its traits hear where the round lands. */
        ItemStack gun = ItemStack.EMPTY;

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
            if (left <= muzzleEnergy * FTuning.GUN_MIN_ENERGY_FRACTION)
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
        long tolerance = Math.min(FTuning.GUN_FIRE_TOLERANCE_MS, cycleMs / 4);
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
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null || GunHazard.isSpent(gun))
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
        round = round.at(level.getGameTime());
        var base = design.firing(GunAmmo.count(gun)).bake(materials, round, GunSteam.stored(gun));
        var load = design.loadOf(materials, round);
        if (base.isEmpty() || load.isEmpty())
            return false;
        // A steam gun with less than a shot's steam in the chest only hisses.
        if (design.launcher() == GunSpec.Launcher.STEAM && !GunSteam.canFire(gun)) {
            if (shooter instanceof Player player)
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.feedback.gun.no_steam"), true);
            return false;
        }
        // A winding above its quench temperature carries nothing: a hard gate with a cause.
        if (GunCondition.quenched(gun, level, load.get())) {
            if (shooter instanceof Player player)
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.feedback.gun.quenched"), true);
            return false;
        }
        float heat = GunCondition.heatFraction(gun, level, load.get().ignitionTu());
        GunFigures figures = base.get().aged(GunCondition.condition(gun), GunFu.bloomHeat(shooter, heat));
        // Traits that read the shooter's hands and the dual-wield rule are a player's; a mob fires the bake as it stands.
        if (shooter instanceof Player player) {
            figures = ModularTraits.gunAimed(player, gun, figures);
            if (GunItem.dualWielding(player))
                figures = figures.oneHanded();
        }
        if (!together && !accept(shooter, figures, nowMs, hand))
            return false;
        LAST_SHOT.put(shooter.getUUID(), System.currentTimeMillis());
        GunAmmo.spend(gun);
        if (design.launcher() == GunSpec.Launcher.STEAM)
            GunSteam.spend(gun);
        // Only a chamber's case holds the pressure; a spring or a steam gun's round is a dry slug in a shell.
        float caseRating = design.launcher().chargeless() ? Float.MAX_VALUE
                : round.caseRating(materials).map(p -> p.value()).orElse(Float.MAX_VALUE);
        float noise = 1 + (level.random.nextFloat() * 2 - 1) * FTuning.GUN_HAZARD_NOISE;
        GunHazard.apply(level, shooter, gun, GunHazard.assess(figures.peakPressure().value(),
                figures.chamberRating().value(), caseRating, noise));
        GunCondition.afterShot(gun, figures, level, heat);
        var projectile = materials.get(round.projectile());
        fire(level, shooter, figures.withSpread(GunStance.of(shooter, aimed)), yaw, pitch,
                round.tracer(materials), round.incendiary(materials), round.pellets(),
                projectile == null ? FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS : projectile.hardness().value(), gun,
                round.blast(materials));
        GunFu.shotFired(level, shooter, gun, figures, heat);
        if (shooter instanceof net.minecraft.server.level.ServerPlayer server)
            ModularTraits.gunFired(server, gun, figures);
        if (cookOff) {
            float hp = GunHazard.cookOffHp(round.charge(), noise);
            if (shooter instanceof Player player)
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.feedback.gun.cook_off"), true);
            shooter.hurt(level.damageSources().explosion(null, null), hp);
        }
        return true;
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
        if (load.isEmpty() || load.get().coil()
                || GunCondition.temperature(gun, level).value() < load.get().ignitionTu())
            return false;
        return shoot(level, holder, gun, design, round, holder.getYRot(), holder.getXRot(), nowMs, true, false, false, InteractionHand.MAIN_HAND);
    }

    /** A reload in progress: which gun, in which slot, and when its next beat is due. */
    private record Reload(ItemStack gun, InteractionHand hand, int slot, boolean byRound, long dueMs) {
    }

    private static final Map<UUID, Reload> RELOADS = new HashMap<>();

    /**
     * R went down on the held gun: a reload starts, and it lasts only while R is held (the
     * author's ruling, 2026-10-14). A gun with its own feed (barrels, cylinder, tube) takes a loose
     * round of its bore a beat at a time, so letting go keeps what is in; one fed from a magazine
     * item swaps for the fullest one of its bore and feed only once R has been held the swap's
     * whole time, so letting go early keeps the old one. Firing, or putting the gun away, ends it.
     */
    /**
     * A gun in each hand is held out of the way of the other: a reload takes {@link FTuning#GUN_DUAL_RELOAD_MULT} times
     * as long, less the share of the extra that {@link Figure#DUAL_RELOAD} (Twin Reload) takes off.
     */
    public static float reloadScale(Player player) {
        if (!(player.getMainHandItem().getItem() instanceof GunItem && player.getOffhandItem().getItem() instanceof GunItem))
            return 1f;
        float relief = Mth.clamp(Skills.modifier(player, Figure.DUAL_RELOAD), 0f, 1f);
        return 1f + (FTuning.GUN_DUAL_RELOAD_MULT - 1f) * (1f - relief);
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

    /** How long one beat of {@code gun}'s reload takes now: a round, or a magazine swap, scaled for two guns. */
    private static long beatMs(Player player, ItemStack gun) {
        boolean byRound = GunSpec.of(gun).map(d -> d.internalCapacity().isPresent()).orElse(true);
        long ms = io.github.soundgoodizerfan.feedback.core.Haste.time(
                Math.round((byRound ? FTuning.GUN_RELOAD_ROUND_MS : GunAmmo.reloadMs(gun)) * reloadScale(player)), player);
        // Spent-Case Draw: reloading mid-roll, the roll's own remaining time is the beat if that is shorter.
        var phase = io.github.soundgoodizerfan.feedback.combat.state.CombatState.of(player);
        if ((phase.phase() == io.github.soundgoodizerfan.feedback.combat.state.CombatPhase.ROLL
                || phase.phase() == io.github.soundgoodizerfan.feedback.combat.state.CombatPhase.ROLL_RECOVERY)
                && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(player, "gun_fu_spent_case_draw"))
            ms = Math.min(ms, (phase.ticksLeft() + (phase.phase() == io.github.soundgoodizerfan.feedback.combat.state.CombatPhase.ROLL ? phase.then() : 0)) * 50L);
        return ms;
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
        RELOADS.put(player.getUUID(), new Reload(gun, hand, player.getInventory().selected, byRound, nowMs + beatMs(player, gun)));
        player.level().playSound(player, player.getX(), player.getEyeY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** R let go: a round-by-round reload stops with what is in, a magazine swap does not happen. */
    public static void reloadStop(Player player) {
        RELOADS.remove(player.getUUID());
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
            return;
        }
        long due = r.dueMs();
        ItemStack gun = r.gun();
        InteractionHand hand = r.hand();
        boolean byRound = r.byRound();
        while (nowMs >= due) {
            GunSpec design = GunSpec.of(gun).orElse(null);
            if (design == null)
                break;
            if (!byRound) {
                swapMagazine(player, gun, design);
            } else if (GunAmmo.loadLoose(player.getInventory(), gun, design, 1) == 0) {
                RELOADS.remove(player.getUUID());
                return;
            } else {
                player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.5f, 1.6f);
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
            due += beatMs(player, gun);
        }
        RELOADS.put(player.getUUID(), new Reload(gun, hand, r.slot(), byRound, due));
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
        boolean bestPreferred = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var s = inventory.getItem(i);
            if (!s.is(FWeapons.MAGAZINE_ITEM.get()) || GunAmmo.size(s).feed() != design.layout().feed())
                continue;
            Magazine m = MagazineItem.rounds(s);
            if (!m.isEmpty() && m.boreMm() != design.boreMm())
                continue;
            // A magazine of the rounds this launcher drives beats a fuller one of the other kind.
            boolean preferred = m.isEmpty() || GunAmmo.prefers(design, m.top());
            if (best < 0 || (preferred && !bestPreferred) || (preferred == bestPreferred && m.rounds().size() > most)) {
                best = i;
                most = m.rounds().size();
                bestPreferred = preferred;
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
                net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6f, 1.4f);
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
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS);
    }

    /** As above, of a projectile of this hardness: what it glances off and what armour it goes through. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, hardness, ItemStack.EMPTY);
    }

    /** As above, from {@code gun}: its traits hear where each round lands. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness, ItemStack gun) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, hardness, gun, 0f);
    }

    /** As above, with an explosion of {@code blast} power where each pellet stops. */
    public static void fire(ServerLevel level, LivingEntity shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness, ItemStack gun, float blast) {
        GunFigures each = pellets > 1 ? f.asPellet(pellets) : f;
        Vec3 muzzle = shooter.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(0.4));
        // Dead Eye: the shot is exact, and its round is harder than it was (it goes through more armour).
        boolean exact = shooter instanceof net.minecraft.server.level.ServerPlayer sp
                && io.github.soundgoodizerfan.feedback.combat.cast.DeadEye.consume(sp);
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
            shot.hardness = exact ? hardness * 2f : hardness;
            shot.gun = gun.copyWithCount(1);
            LIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(shot);
        }

        for (GunSound.Layer layer : GunSound.layers(f, level.random))
            level.playSound(shooter instanceof Player player && !hearSelf ? player : null, muzzle.x, muzzle.y, muzzle.z, layer.event(), SoundSource.PLAYERS,
                    layer.volume(), layer.pitch());
        level.gameEvent(shooter, f.report() < FTuning.GUN_QUIET_REPORT ? FWeapons.GUNSHOT_QUIET
                : f.report() >= FTuning.GUN_THUNDEROUS_REPORT ? FWeapons.GUNSHOT_THUNDEROUS : FWeapons.GUNSHOT, muzzle);
        // Only a chamber has burning gas to show; a coil crackles and a spring flicks dust.
        level.sendParticles(f.muzzleParticle(), muzzle.x, muzzle.y, muzzle.z,
                3, 0.02, 0.02, 0.02, 0.01);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        List<Shot> shots = LIVE.get(level);
        if (shots == null)
            return;
        for (Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
            if (!step(level, it.next()))
                it.remove();
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
            io.github.soundgoodizerfan.feedback.modular.ModularFigures modular = io.github.soundgoodizerfan.feedback.modular.ModularStacks.figures(piece);
            float h = modular != null ? modular.get(io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig.HARDNESS)
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
        return !Float.isNaN(breakCost(toughness)) && momentum >= toughness * FTuning.GUN_BREAK_MOMENTUM_PER_TOUGHNESS;
    }

    /** Whether a bullet glances off: softer than the block, coming in shallow. Pure. */
    public static boolean glances(float bulletHardness, float blockHardness, float cosToNormal) {
        return bulletHardness < blockHardness && cosToNormal < FTuning.GUN_RICOCHET_MAX_COS;
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
        return Float.isNaN(toughness) || toughness > FTuning.GUN_BREAK_TOUGHNESS_MAX
                ? Float.NaN : toughness * FTuning.GUN_BREAK_WORK_PER_TOUGHNESS;
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
        if (s.blast < FTuning.DEMO_POWER_MIN || s.age < FTuning.DEMO_ROUND_ARM_TICKS)
            return false;
        float power = s.blast;
        s.blast = 0;
        level.explode(s.shooter, at.x, at.y, at.z, power, false,
                mayChange(level, s.shooter, BlockPos.containing(at)) ? net.minecraft.world.level.Level.ExplosionInteraction.BLOCK
                        : net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        return true;
    }

    /** One tick's travel. @return whether the shot is still flying. */
    private static boolean step(ServerLevel level, Shot s) {
        if (s.shooter.isRemoved() || ++s.age > FTuning.GUN_MAX_LIFE_TICKS)
            return false;
        Vec3 from = s.pos, to = from.add(s.velocity);

        // A coin the shooter flipped, struck by this round: the metal decides what it does (CoinShot).
        CoinShot.Result coin = CoinShot.resolve(level, s.shooter, from, to, s.massMu, s.hardness);
        if (coin != null) {
            s.pos = coin.point();
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
            float damage = s.energy() * FTuning.GUN_DAMAGE_PER_WORK;
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
                    target.hurt(new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(BULLET_PIERCE), s.shooter), damage * through);
                    target.invulnerableTime = 0;
                }
                target.hurt(attack(level, s.shooter), damage * (1 - through));
            } finally {
                inBullet = false;
            }
            if (s.incendiary)
                target.igniteForSeconds(FTuning.GUN_INCENDIARY_SECONDS);
            GunFu.bulletHit(level, s.shooter, target, s.impulse);
            landed(level, s, target, hit.getLocation());
            if (detonate(level, s, hit.getLocation()))
                return false;
            if (damage <= health || !s.spend(health / FTuning.GUN_DAMAGE_PER_WORK))
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
            if (allowed && breaks(toughness, s.momentum()) && s.energy() > cost && s.spend(cost)) {
                // Glass breaks and the bullet goes on, a little slower.
                level.destroyBlock(at, false, s.shooter);
                s.pos = p.add(s.velocity.normalize().scale(0.05));
                return true;
            }
            // A soft bullet meeting a harder face at a shallow angle glances off it, slower.
            Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
            float cos = (float) Math.abs(s.velocity.normalize().dot(normal));
            if (s.bounces < FTuning.GUN_RICOCHET_MAX && glances(s.hardness, hardnessOf(hitBlock), cos)
                    && s.spend(s.energy() * (1 - FTuning.GUN_RICOCHET_KEEP))) {
                s.velocity = s.velocity.subtract(normal.scale(2 * s.velocity.dot(normal)));
                s.pos = p.add(normal.scale(0.05));
                s.bounces++;
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 4, 0.05, 0.05, 0.05, 0.2);
                return true;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(at)),
                    p.x, p.y, p.z, 6, 0.05, 0.05, 0.05, 0.1);
            landed(level, s, null, p);
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

        // A tracer: a few points along the tick's travel, so a bullet can be seen going.
        // An ordinary round leaves a faint glint; a luminous payload draws a bright line.
        Vec3 d = to.subtract(from);
        int samples = s.tracer ? 10 : 4;
        for (int k = 1; k <= samples; k++) {
            Vec3 q = from.add(d.scale(k / (double) samples));
            level.sendParticles(s.tracer ? ParticleTypes.END_ROD : ParticleTypes.CRIT, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }

        // Still flying: drag in the medium it ends the tick in, then gravity.
        boolean inFluid = !level.getFluidState(BlockPos.containing(to)).isEmpty();
        float drag = inFluid ? FTuning.GUN_FLUID_DRAG_PER_TICK : FTuning.GUN_DRAG_PER_TICK;
        s.velocity = s.velocity.scale(1 - drag).add(0, -FTuning.GUN_GRAVITY_PER_TICK, 0);
        s.pos = to;
        return true;
    }
}
