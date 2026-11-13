// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.cast.Stamina;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.trait.Billhook;
import io.github.soundgoodizerfan.feedback.weapon.GunFu;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The bow and crossbow side of the gun skills: every gun buff has a projectile-weapon twin in the same tree, so a
 * player not tech-tree'd into guns still benefits. A projectile weapon is any item that is a
 * {@link ProjectileWeaponItem} (the arrow remembers the weapon that loosed it), never an item id. Each perk
 * reads the arrow's own speed, which is the draw: a hard-drawn bow carries more than a short one.
 * <ul>
 * <li>Observation, Far sight (twin of Dead eye): a hard-drawn arrow flies flat for its first second.</li>
 * <li>Observation, Quarry mark (twin of Called shot): a hit at range wounds the target, which bleeds for longer under more energy and leaves drops on the ground the shooter can follow.</li>
 * <li>Brute, Point-blank draw (twin of Muzzle stagger): a close hit staggers by the arrow's energy over the target's mass.</li>
 * <li>Chaos, Barbed shaft (twin of Powder burn): a close hit pins the target, slowing it until the pin works loose or the shooter strikes.</li>
 * <li>Anchor, Braced draw (twin of Braced fire): loosed braced against terrain, the arrow leaves exactly down the sight line.</li>
 * <li>Veil, Snap shot (twin of Dodge charge): after a roll that swallowed a blow, the next arrow leaves at full draw.</li>
 * <li>Proficiency, Quarry breath (twin of Hot clear): a kill at range gives stamina back by the arrow's energy.</li>
 * </ul>
 * Constants are STAND-INS ([OPEN]) until played.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BowCraft {

    /** Arrow mass, kg (stand-in), and blocks/tick to m/s. */
    private static final float ARROW_MASS = 0.05f, MPS = 20f;
    /** Full draw leaves at this many blocks per tick. */
    private static final double FULL_DRAW = 3.0;
    private static final double FLAT_SPEED = 2.5;
    private static final int FLAT_TICKS = 20, SNAP_TICKS = 60;
    private static final double CLOSE = 4.0;
    /** Barbed shaft: ticks a pin holds and the slowness level it puts on (STAND-INS). */
    private static final int PIN_TICKS = 100, PIN_SLOW = 2;
    private static final float BREATH_PER_JOULE = 0.2f, BREATH_MAX = 30f;
    /** Quarry mark: bleed seconds per joule (most {@code WOUND_MAX_SECONDS}), damage a second, ticks a drop stays, drops kept per trail, blocks apart (STAND-INS). */
    private static final float WOUND_SECONDS_PER_JOULE = 0.1f, WOUND_MAX_SECONDS = 10f, WOUND_DAMAGE = 1f;
    private static final int DROP_TICKS = 600, DROPS_MAX = 24;
    private static final double DROP_APART = 1.0;

    private record Pin(UUID shooter, long until) {
    }

    private static final Map<UUID, Pin> PINS = new HashMap<>();

    /** A wounded quarry's trail: the shooter who follows it, and where it dropped blood and when. */
    private record Trail(UUID shooter, java.util.ArrayDeque<Vec3> at, java.util.ArrayDeque<Long> when) {
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private static final net.minecraft.core.particles.DustParticleOptions BLOOD =
            new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.65f, 0.04f, 0.06f), 0.9f);
    private static final Map<UUID, Long> SNAP = new HashMap<>();
    private static final Set<AbstractArrow> FLAT = Collections.newSetFromMap(new WeakHashMap<>());

    private BowCraft() {
    }

    /** Joules the arrow carries now: half m v squared. */
    private static float energy(AbstractArrow arrow) {
        float v = (float) arrow.getDeltaMovement().length() * MPS;
        return 0.5f * ARROW_MASS * v * v;
    }

    private static ServerPlayer shooter(AbstractArrow arrow) {
        return arrow.getOwner() instanceof ServerPlayer p && arrow.getWeaponItem() != null
                && arrow.getWeaponItem().getItem() instanceof ProjectileWeaponItem ? p : null;
    }

    /** A roll swallowed a blow: Snap shot arms (called from {@code GunFu.dodged}, which the roll already calls). */
    public static void dodged(LivingEntity who) {
        if (who instanceof ServerPlayer p && Skills.unlocked(p, "snap_shot"))
            SNAP.put(p.getUUID(), p.level().getGameTime() + SNAP_TICKS);
    }

    /** Called draw: the target a player has called, and the game tick the call ends; the next arrow loosed flies at where it will be. */
    private record Called(UUID target, long until) {
    }

    private static final Map<UUID, Called> CALLED = new HashMap<>();
    /** Held draw: ticks a player has held a full draw, counted only while the skill is owned. */
    private static final Map<UUID, Integer> HELD = new HashMap<>();
    /** STAND-INS ([OPEN]): ticks a call lasts, full-draw ticks that steady the arrow, and where the hands start to shake; stamina a held tick costs. */
    private static final int CALL_TICKS = 60, STEADY_TICKS = 20, SHAKE_TICKS = 100;
    private static final float HOLD_COST = 0.2f;

    /** Called draw: calls {@code target} for the next arrow {@code p} looses. */
    public static void call(ServerPlayer p, LivingEntity target) {
        CALLED.put(p.getUUID(), new Called(target.getUUID(), p.level().getGameTime() + CALL_TICKS));
    }

    @SubscribeEvent
    public static void onHold(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Skills.unlocked(p, "held_draw"))
            return;
        var using = p.getUseItem();
        if (!p.isUsingItem() || !(using.getItem() instanceof ProjectileWeaponItem)
                || p.getTicksUsingItem() < io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.drawTicks(using)) {
            if (!p.isUsingItem())
                HELD.remove(p.getUUID());
            return;
        }
        int held = HELD.merge(p.getUUID(), 1, Integer::sum);
        if (held <= SHAKE_TICKS && !Stamina.spend(p, HOLD_COST))
            HELD.put(p.getUUID(), SHAKE_TICKS + 1); // out of breath: the aim goes
    }

    @SubscribeEvent
    public static void onLoosed(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow) || arrow.tickCount > 0)
            return;
        ServerPlayer p = shooter(arrow);
        if (p == null)
            return;
        Long snap = SNAP.remove(p.getUUID());
        Vec3 v = arrow.getDeltaMovement();
        if (snap != null && p.level().getGameTime() <= snap && v.length() < FULL_DRAW) {
            arrow.setDeltaMovement(v.normalize().scale(FULL_DRAW));
            arrow.setCritArrow(true);
        }
        // Held draw: a full draw held a second is steady, aimed exactly down the look; held too long, it shakes.
        Integer held = HELD.remove(p.getUUID());
        if (held != null && held >= STEADY_TICKS && held <= SHAKE_TICKS)
            arrow.setDeltaMovement(p.getLookAngle().scale(arrow.getDeltaMovement().length()));
        // Called draw: the arrow flies at where the called creature will be when it arrives.
        Called call = CALLED.remove(p.getUUID());
        if (call != null && p.level().getGameTime() <= call.until() && p.serverLevel().getEntity(call.target()) instanceof LivingEntity t
                && t.isAlive()) {
            double speed = arrow.getDeltaMovement().length();
            Vec3 c = t.getBoundingBox().getCenter();
            double flight = c.distanceTo(arrow.position()) / Math.max(0.1, speed);
            Vec3 lead = c.add(t.getDeltaMovement().multiply(1, t.onGround() ? 0 : 1, 1).scale(flight)).add(0, 0.5 * 0.05 * flight * flight, 0);
            arrow.setDeltaMovement(lead.subtract(arrow.position()).normalize().scale(speed));
        }
        if (Skills.unlocked(p, "braced_draw") && GunFu.braced(p))
            arrow.setDeltaMovement(p.getLookAngle().scale(arrow.getDeltaMovement().length()));
        if (Skills.unlocked(p, "far_sight") && arrow.getDeltaMovement().length() >= FLAT_SPEED) {
            arrow.setNoGravity(true);
            FLAT.add(arrow);
        }
    }

    /** Far sight ends: the arrow starts to drop after its first second. */
    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (!FLAT.isEmpty() && event.getEntity() instanceof AbstractArrow arrow && arrow.tickCount >= FLAT_TICKS
                && FLAT.remove(arrow))
            arrow.setNoGravity(false);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow) || event.getNewDamage() <= 0f)
            return;
        ServerPlayer p = shooter(arrow);
        if (p == null)
            return;
        LivingEntity target = event.getEntity();
        double dist = p.distanceTo(target);
        float joules = energy(arrow);
        // Observation pays for any projectile weapon's hit at range, the same as a gun's.
        if (dist >= SkillTuning.OBSERVE_RANGE)
            SkillEarning.award(p, SkillEarning.OBSERVATION, target.getStringUUID(), SkillTuning.GUN_HIT);
        if (dist >= SkillTuning.OBSERVE_RANGE && Skills.unlocked(p, "quarry_mark") && !target.isDeadOrDying()) {
            Billhook.open(target, Math.min(WOUND_MAX_SECONDS, joules * WOUND_SECONDS_PER_JOULE), WOUND_DAMAGE);
            TRAILS.putIfAbsent(target.getUUID(), new Trail(p.getUUID(), new java.util.ArrayDeque<>(), new java.util.ArrayDeque<>()));
        }
        if (dist <= CLOSE && !target.isDeadOrDying()) {
            if (Skills.unlocked(p, "point_blank_draw"))
                CombatTicker.stagger(target, ParryTuning.staggerTicks(joules, ParryTuning.standInMass(target)),
                        ParryTuning.STAGGER_SLOW);
            if (Skills.unlocked(p, "barbed_shaft")) {
                long now = p.level().getGameTime();
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PIN_TICKS, PIN_SLOW));
                PINS.put(target.getUUID(), new Pin(p.getUUID(), now + PIN_TICKS));
                io.github.soundgoodizerfan.feedback.combat.cast.Disorder.note(p, target, io.github.soundgoodizerfan.feedback.combat.cast.Disorder.Kind.PINNED);
            }
        }
        if (dist >= SkillTuning.OBSERVE_RANGE && target.isDeadOrDying() && Skills.unlocked(p, "quarry_breath"))
            Stamina.gain(p, Math.min(BREATH_MAX, joules * BREATH_PER_JOULE));
    }

    /** Barbed shaft: the shooter's own blow on a pinned target pulls the shaft out, and the slowness with it. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (PINS.isEmpty() || !(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet())
            return;
        Pin pin = PINS.get(event.getEntity().getUUID());
        if (pin == null || !pin.shooter().equals(p.getUUID()))
            return;
        PINS.remove(event.getEntity().getUUID());
        event.getEntity().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    /** Quarry mark: a bleeding quarry drops blood where it goes; the shooter sees the drops for half a minute. */
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        long tick = event.getServer().getTickCount();
        if (tick % 10 != 0)
            return;
        PINS.values().removeIf(pin -> event.getServer().overworld().getGameTime() > pin.until());
        if (TRAILS.isEmpty())
            return;
        for (var it = TRAILS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Trail trail = e.getValue();
            ServerPlayer shooter = event.getServer().getPlayerList().getPlayer(trail.shooter());
            LivingEntity quarry = null;
            if (shooter != null && shooter.serverLevel().getEntity(e.getKey()) instanceof LivingEntity q && q.isAlive())
                quarry = q;
            long now = shooter == null ? 0 : shooter.level().getGameTime();
            while (!trail.when().isEmpty() && now - trail.when().peekFirst() > DROP_TICKS) {
                trail.when().pollFirst();
                trail.at().pollFirst();
            }
            if (shooter == null || quarry == null && trail.at().isEmpty()) {
                it.remove();
                continue;
            }
            if (quarry != null && Billhook.bleeding(quarry)) {
                Vec3 here = quarry.position().add(0, 0.1, 0);
                if (trail.at().isEmpty() || trail.at().peekLast().distanceTo(here) >= DROP_APART) {
                    trail.at().addLast(here);
                    trail.when().addLast(now);
                    if (trail.at().size() > DROPS_MAX) {
                        trail.at().pollFirst();
                        trail.when().pollFirst();
                    }
                }
            } else if (quarry != null && trail.at().isEmpty())
                it.remove();
            for (Vec3 d : trail.at())
                shooter.serverLevel().sendParticles(shooter, BLOOD, true, d.x, d.y, d.z, 2, 0.12, 0.0, 0.12, 0);
        }
    }
}
