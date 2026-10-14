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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
 * The server's side of a shot ({@code feedback_guns_spec.md} sections 7 and 7.1).
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
        final Player shooter;
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
        /** The projectile's material hardness: what decides a glance off a block and a way through armour. */
        float hardness = FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS;
        final float massMu;
        int bounces;

        Shot(Player shooter, Vec3 pos, Vec3 velocity, GunFigures figures) {
            this.shooter = shooter;
            this.pos = pos;
            this.velocity = velocity;
            this.muzzleSpeed = figures.muzzleSpeed();
            this.muzzleEnergy = figures.muzzleEnergy();
            this.massMu = figures.projectileMass().value();
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
    private static final Map<UUID, Long> NEXT_OK = new HashMap<>();

    private GunShots() {
    }

    /**
     * Whether {@code shooter} may fire now, and if so book the next slot. The cycle is wall-clock
     * (spec 7.1); the tolerance is for network jitter until timestamp sync replaces it.
     */
    public static boolean accept(Player shooter, GunFigures figures, long nowMs) {
        // Early by at most a quarter of a cycle, or the jitter allowance if that is less: a
        // client hacked to fire a little early gains only that, and an honest one never trips it.
        long tolerance = Math.min(FTuning.GUN_FIRE_TOLERANCE_MS, figures.cycleMs() / 4);
        long earliest = NEXT_OK.getOrDefault(shooter.getUUID(), 0L);
        if (nowMs + tolerance < earliest)
            return false;
        NEXT_OK.put(shooter.getUUID(), Math.max(earliest, nowMs) + figures.cycleMs());
        return true;
    }

    /**
     * A trigger pull on the server: the round the gun's own record says is next, in the figures of
     * this gun, the cycle validated, the round spent, overpressure booked, and the shot launched.
     * The client names only the aim; it never says what is chambered.
     *
     * @return whether a shot left the muzzle.
     */
    public static boolean trigger(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs) {
        return trigger(level, shooter, gun, yaw, pitch, nowMs, false);
    }

    public static boolean trigger(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs, boolean aimed) {
        GunDesign design = gun.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_DESIGN.get());
        if (design == null || GunHazard.isSpent(gun))
            return false;
        reloadStop(shooter); // a pull ends a reload where it is
        var next = GunAmmo.next(gun);
        if (next.isEmpty())
            return false;
        return shoot(level, shooter, gun, design, next.get(), yaw, pitch, nowMs, false, aimed);
    }

    /**
     * The common path of a pull and a cook-off: figures for this round in this gun as it is now,
     * cycle checked, round spent, overpressure and heat and wear booked, shot launched.
     */
    private static boolean shoot(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                 GunDesign design, Cartridge round, float yaw, float pitch, long nowMs, boolean cookOff,
                                 boolean aimed) {
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        round = round.at(level.getGameTime());
        var base = design.firing(GunAmmo.count(gun)).bake(materials, round);
        var load = round.load(materials);
        if (base.isEmpty() || load.isEmpty())
            return false;
        // A winding above its quench temperature carries nothing: a hard gate with a cause.
        if (GunCondition.quenched(gun, level, load.get())) {
            shooter.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.feedback.gun.quenched"), true);
            return false;
        }
        float heat = GunCondition.heatFraction(gun, level, load.get().ignitionTu());
        GunFigures figures = base.get().aged(GunCondition.condition(gun), heat);
        if (!accept(shooter, figures, nowMs))
            return false;
        GunAmmo.spend(gun);
        float caseRating = round.caseRating(materials).map(p -> p.value()).orElse(Float.MAX_VALUE);
        float noise = 1 + (level.random.nextFloat() * 2 - 1) * FTuning.GUN_HAZARD_NOISE;
        GunHazard.apply(level, shooter, gun, GunHazard.assess(figures.peakPressure().value(),
                figures.chamberRating().value(), caseRating, noise));
        GunCondition.afterShot(gun, figures, level, heat);
        var projectile = materials.get(round.projectile());
        fire(level, shooter, figures.withSpread(GunStance.of(shooter, aimed)), yaw, pitch,
                round.tracer(materials), round.incendiary(materials), round.pellets(),
                projectile == null ? FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS : projectile.hardness().value());
        if (cookOff) {
            float hp = GunHazard.cookOffHp(round.charge(), noise);
            shooter.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.feedback.gun.cook_off"), true);
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
        GunDesign design = gun.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_DESIGN.get());
        Cartridge round = gun.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.CHAMBER.get());
        if (design == null || round == null || GunHazard.isSpent(gun))
            return false;
        var load = round.load(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials());
        if (load.isEmpty() || load.get().coil()
                || GunCondition.temperature(gun, level).value() < load.get().ignitionTu())
            return false;
        return shoot(level, holder, gun, design, round, holder.getYRot(), holder.getXRot(), nowMs, true, false);
    }

    /** A reload in progress: which gun, in which slot, and when its next beat is due. */
    private record Reload(ItemStack gun, int slot, boolean byRound, long dueMs) {
    }

    private static final Map<UUID, Reload> RELOADS = new HashMap<>();

    /**
     * R went down on the held gun: a reload starts, and it lasts only while R is held (the
     * author's ruling, 2026-10-14). A gun with its own feed (barrels, cylinder, tube) takes a loose
     * round of its bore a beat at a time, so letting go keeps what is in; one fed from a magazine
     * item swaps for the fullest one of its bore and feed only once R has been held the swap's
     * whole time, so letting go early keeps the old one. Firing, or putting the gun away, ends it.
     */
    public static void reloadStart(Player player, long nowMs) {
        var gun = player.getMainHandItem();
        GunDesign design = gun.getItem() instanceof GunItem && !player.isSpectator() ? gun.get(FWeapons.GUN_DESIGN.get()) : null;
        if (design == null || RELOADS.containsKey(player.getUUID()))
            return;
        boolean byRound = design.internalCapacity().isPresent();
        if (byRound ? GunAmmo.loadable(player.getInventory(), gun, design).isEmpty() : bestMagazine(player, design) < 0)
            return;
        RELOADS.put(player.getUUID(), new Reload(gun, player.getInventory().selected, byRound,
                nowMs + (byRound ? FTuning.GUN_RELOAD_ROUND_MS : FTuning.GUN_RELOAD_MS)));
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

    /** Advance a held reload: a round per beat, or the swap once its time is up. */
    public static void reloadTick(Player player, long nowMs) {
        Reload r = RELOADS.get(player.getUUID());
        if (r == null)
            return;
        // Put away, or another thing in hand: the reload ends where it is.
        if (player.getInventory().selected != r.slot() || player.getMainHandItem() != r.gun()) {
            RELOADS.remove(player.getUUID());
            return;
        }
        GunDesign design = r.gun().get(FWeapons.GUN_DESIGN.get());
        long due = r.dueMs();
        while (design != null && nowMs >= due) {
            if (!r.byRound()) {
                swapMagazine(player, r.gun(), design);
                RELOADS.remove(player.getUUID());
                return;
            }
            if (GunAmmo.loadLoose(player.getInventory(), r.gun(), design, 1) == 0) {
                RELOADS.remove(player.getUUID());
                return;
            }
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.5f, 1.6f);
            due += FTuning.GUN_RELOAD_ROUND_MS;
        }
        RELOADS.put(player.getUUID(), new Reload(r.gun(), r.slot(), r.byRound(), due));
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide && !RELOADS.isEmpty())
            reloadTick(event.getEntity(), System.currentTimeMillis());
    }

    /** The inventory slot of the fullest magazine of this gun's bore and feed, or -1. */
    private static int bestMagazine(Player player, GunDesign design) {
        var inventory = player.getInventory();
        int best = -1, most = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var s = inventory.getItem(i);
            if (!s.is(FWeapons.MAGAZINE_ITEM.get()) || GunAmmo.size(s).feed() != design.layout().feed())
                continue;
            Magazine m = MagazineItem.rounds(s);
            if ((m.isEmpty() || m.boreMm() == design.boreMm()) && m.rounds().size() > most) {
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
    private static void swapMagazine(Player player, ItemStack gun, GunDesign design) {
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
    public static void fire(ServerLevel level, Player shooter, GunFigures f, float yaw, float pitch) {
        fire(level, shooter, f, yaw, pitch, false, false);
    }

    public static void fire(ServerLevel level, Player shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, 1);
    }

    /** As above, as {@code pellets} pellets of shot sharing the projectile, each on its own line through the cone. */
    public static void fire(ServerLevel level, Player shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets) {
        fire(level, shooter, f, yaw, pitch, tracer, incendiary, pellets, FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS);
    }

    /** As above, of a projectile of this hardness: what it glances off and what armour it goes through. */
    public static void fire(ServerLevel level, Player shooter, GunFigures f, float yaw, float pitch,
                            boolean tracer, boolean incendiary, int pellets, float hardness) {
        GunFigures each = pellets > 1 ? f.asPellet(pellets) : f;
        Vec3 muzzle = shooter.getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(0.4));
        for (int k = 0; k < pellets; k++) {
            float r = each.spreadDeg() * Mth.sqrt(level.random.nextFloat());
            float theta = level.random.nextFloat() * Mth.TWO_PI;
            float aimPitch = Mth.clamp(pitch + r * Mth.sin(theta), -90f, 90f);
            float aimYaw = yaw + r * Mth.cos(theta);
            Vec3 dir = Vec3.directionFromRotation(aimPitch, aimYaw);
            Shot shot = new Shot(shooter, shooter.getEyePosition().add(dir.scale(0.4)), dir.scale(each.muzzleSpeed()), each);
            shot.tracer = tracer;
            shot.incendiary = incendiary;
            shot.hardness = hardness;
            LIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(shot);
        }

        for (GunSound.Layer layer : GunSound.layers(f, level.random))
            level.playSound(shooter, muzzle.x, muzzle.y, muzzle.z, layer.event(), SoundSource.PLAYERS,
                    layer.volume(), layer.pitch());
        level.gameEvent(shooter, f.report() < FTuning.GUN_QUIET_REPORT ? FWeapons.GUNSHOT_QUIET : FWeapons.GUNSHOT, muzzle);
        // A coil has no burning gas to show: a crackle of field, not smoke.
        level.sendParticles(f.coil() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
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
            float h = ARMOUR.computeIfAbsent(piece.getItem(), GunShots::hardnessOf);
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

    /** One tick's travel. @return whether the shot is still flying. */
    private static boolean step(ServerLevel level, Shot s) {
        if (s.shooter.isRemoved() || ++s.age > FTuning.GUN_MAX_LIFE_TICKS)
            return false;
        Vec3 from = s.pos, to = from.add(s.velocity);

        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, s.shooter, from, end,
                new AABB(from, end).inflate(1.0),
                e -> e.isPickable() && !e.isSpectator() && e != s.shooter && !s.pierced.contains(e.getId()));
        if (hit != null) {
            Entity target = hit.getEntity();
            float damage = s.energy() * FTuning.GUN_DAMAGE_PER_WORK;
            // How much of the hit the target takes: its health. The bullet's surplus goes on
            // through ("surplus carries through rather than stopping politely", spec 3.5), so a
            // heavy round that kills a zombie is still flying behind it.
            float health = target instanceof net.minecraft.world.entity.LivingEntity living
                    ? living.getHealth() + living.getAbsorptionAmount() : damage;
            // Rapid fire must not be eaten by invulnerability frames; a gun is not a sword.
            target.invulnerableTime = 0;
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
            target.hurt(level.damageSources().playerAttack(s.shooter), damage * (1 - through));
            if (s.incendiary)
                target.igniteForSeconds(FTuning.GUN_INCENDIARY_SECONDS);
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
            boolean allowed = !Float.isNaN(cost) && s.shooter.mayBuild() && level.mayInteract(s.shooter, at);
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
            if (s.incendiary) {
                // Fire where it lands, if fire may be there and the world is allowed to burn.
                BlockPos firePos = at.relative(block.getDirection());
                if (level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)
                        && s.shooter.mayBuild() && level.mayInteract(s.shooter, firePos)
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
