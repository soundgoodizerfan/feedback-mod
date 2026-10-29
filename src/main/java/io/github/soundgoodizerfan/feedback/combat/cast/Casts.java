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
package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Rearm;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.weapon.CoinShot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Cast techniques: the moves a fighter slots to a cast key and pops on purpose (the skill kit's set pieces, as
 * against the stem payoffs that ride an action already happening). A cast needs the unlock, a rearmed technique
 * and its stamina, and each one reads a figure the world already carries: the gun's cycle and heat, the body's mass
 * and fall, the weapon's damage, a held item's temperature. The cast's {@code rearm} is the node's, so a clock can be
 * time, a landed blow, a stagger or a parry.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Casts {

    /** A cast: its unlock id, stamina cost, display order, the act (true if it happened), and its hold, if it is held. */
    public record Cast(String id, float cost, boolean rearms, Predicate<ServerPlayer> act, Hold hold) {
        Cast(String id, float cost, boolean rearms, Predicate<ServerPlayer> act) {
            this(id, cost, rearms, act, null);
        }

        Cast(String id, float cost, Predicate<ServerPlayer> act) {
            this(id, cost, true, act, null);
        }

        Cast(String id, float cost, Predicate<ServerPlayer> act, Hold hold) {
            this(id, cost, true, act, hold);
        }
    }

    /**
     * A held cast: once the act has started it, each tick {@code tick} runs (false once the cast has ended itself) and
     * {@code drain} stamina is spent; letting go of the key, or an empty bar, calls {@code letGo}.
     */
    public record Hold(float drain, Predicate<ServerPlayer> tick, java.util.function.Consumer<ServerPlayer> letGo) {
    }

    /** A hold under way: the slot whose key is down, and its cast. */
    private record Holding(int slot, Cast cast) {
    }

    private static final java.util.Map<UUID, Holding> HOLDING = new java.util.HashMap<>();

    private record Later(UUID who, long at, Runnable run) {
    }

    private static final List<Later> LATER = new ArrayList<>();

    /** STAND-IN: stamina Wind Back returns. */
    static final float WIND_BACK = 15f;

    /** The wait code (see {@link Rearms#wait}) for a skill whose node needs a trait the held weapon lacks. */
    public static final int WRONG_GEAR = 4;

    public static final List<Cast> ALL = List.of(
            new Cast("kick", 10, false, Casts::kick),
            new Cast("field_dressing", 0, FieldDressing::act, FieldDressing.HOLD),
            new Cast("second_breath", 0, SecondBreath::act),
            new Cast("cauterize", 15, Cauterize::act),
            new Cast("shake_off", 20, ShakeOff::act),
            new Cast("grab", 0, false, Casts::grab),
            new Cast("coin_flip", 15, CoinShot::flip),
            new Cast("quake_leap", 25, QuakeLeap::act),
            new Cast("whirl", 30, Whirl::act),
            new Cast("forge_slam", 30, ForgeSlam::act),
            new Cast("flurry", 30, Flurry::act),
            new Cast("shadow_step", 25, ShadowStep::act),
            new Cast("walking_fire", 30, WalkingFire::act),
            new Cast("muzzle_flash", 30, MuzzleFlash::act),
            new Cast("hamstring", 25, Hamstring::act),
            new Cast("mark_and_fire", 10, MarkAndFire::act, MarkAndFire.HOLD),
            new Cast("dropkick", 25, Dropkick::act),
            new Cast("shield_wall", 25, ShieldWall::act),
            new Cast("reckoning", 30, Reckoning::act),
            new Cast("wall_run", 20, WallRun::act),
            new Cast("vault", 25, Vault::act),
            new Cast("slide", 15, Slide::act),
            new Cast("smoke_step", 25, SmokeStep::act),
            new Cast("vanish", 20, Vanish::act),
            new Cast("pocket_sand", 15, PocketSand::act),
            new Cast("challenge", 15, Challenge::act),
            new Cast("advance", 25, Advance::act),
            new Cast("punish", 25, Punish::act),
            new Cast("haymaker", 30, Haymaker::act),
            new Cast("uppercut", 25, Uppercut::act),
            new Cast("frenzy", 30, Frenzy::act),
            new Cast("stomp", 20, Stomp::act),
            new Cast("axe_kick", 25, AxeKick::act),
            new Cast("reel_in", 20, ReelIn::act),
            new Cast("bowling", 25, Bowling::act),
            new Cast("swing", 20, Swing::act),
            new Cast("conduct", 25, Conduct::act),
            new Cast("hitch", 20, Hitch::act),
            new Cast("cleave", 30, Cleave::act),
            new Cast("overheat", 30, Overheat::act),
            new Cast("called_shot", 25, CalledShot::act),
            new Cast("point_blank", 25, PointBlank::act),
            new Cast("snap_volley", 30, SnapVolley::act),
            new Cast("human_shield", 20, HumanShield::act),
            new Cast("disarm_shot", 25, DisarmShot::act));

    private Casts() {
    }

    public static Cast find(String id) {
        for (Cast c : ALL)
            if (c.id().equals(id))
                return c;
        return null;
    }

    /** Slots {@code tech} (or clears the slot, for ""): only a technique the player has and that is castable. */
    public static void slot(ServerPlayer p, int slot, String tech) {
        if (slot < 0 || slot >= CastState.SLOTS || !tech.isEmpty() && (find(tech) == null || !Skills.owned(p, tech)))
            return;
        CastState.set(p, CastState.of(p).slotted(slot, tech));
    }

    /** A cast key press: the slotted technique fires if it is owned, rearmed and paid for. */
    public static void fire(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= CastState.SLOTS || p.isSpectator() || p.isPassenger())
            return;
        Cast c = find(CastState.of(p).slots().get(slot));
        if (c == null || !Skills.unlocked(p, c.id()))
            return;
        Holding h = HOLDING.get(p.getUUID());
        if (h != null && h.cast() == c) {
            letGo(p); // a press while it is still held means its release was lost: this press lets go instead
            return;
        }
        if (Stamina.get(p) < c.cost())
            return;
        // Second Mark's second coin costs stamina but no rearm.
        boolean followUp = c.id().equals("coin_flip") && CoinShot.followUp(p);
        if (c.rearms() && !followUp && !Rearms.ready(p, c.id()))
            return;
        if (!c.act().test(p))
            return;
        Stamina.spend(p, c.cost());
        if (c.rearms() && !followUp)
            Rearms.spend(p, c.id()); // after the act, so a stagger it dealt does not rearm it at once
        if (c.hold() != null) {
            if (h != null)
                letGo(p); // one hold at a time
            HOLDING.put(p.getUUID(), new Holding(slot, c));
        }
    }

    /** A cast key let go: a hold on that slot ends. */
    public static void release(ServerPlayer p, int slot) {
        Holding h = HOLDING.get(p.getUUID());
        if (h != null && h.slot() == slot)
            letGo(p);
    }

    private static void letGo(ServerPlayer p) {
        Holding h = HOLDING.remove(p.getUUID());
        if (h != null)
            h.cast().hold().letGo().accept(p);
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p)
            letGo(p);
    }

    static void later(ServerPlayer p, int ticks, Runnable run) {
        LATER.add(new Later(p.getUUID(), p.level().getGameTime() + ticks, run));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (LATER.isEmpty())
            return;
        long now = event.getServer().overworld().getGameTime();
        // Collect first: a beat may schedule more beats (a shockwave does), which must not touch the list mid-loop.
        List<Later> due = new ArrayList<>();
        for (Iterator<Later> it = LATER.iterator(); it.hasNext(); ) {
            Later l = it.next();
            if (now >= l.at()) {
                it.remove();
                due.add(l);
            }
        }
        due.forEach(l -> l.run().run());
    }

    static List<LivingEntity> near(ServerPlayer p, Vec3 at, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                e -> e != p && e.isAlive() && !e.isSpectator() && !p.isAlliedTo(e));
    }

    /** A stagger from {@code joules} of impact on {@code target}, by the same ratio a kick uses; true if it landed. */
    static boolean stagger(ServerPlayer p, LivingEntity target, float joules) {
        int ticks = ParryTuning.staggerTicks(joules, BodyMass.of(target) + BodyMass.armour(target));
        if (ticks <= 0)
            return false;
        CombatTicker.stagger(target, ticks, ParryTuning.STAGGER_SLOW);
        Rearms.on(p, Rearm.STAGGER);
        return true;
    }

    /** The kick, as a cast: its own cooldown and swing are Kicking's, so it has no rearm and a small stamina cost. */
    private static boolean kick(ServerPlayer p) {
        if (!io.github.soundgoodizerfan.feedback.combat.kick.Kicking.canKick(p))
            return false;
        io.github.soundgoodizerfan.feedback.combat.kick.Kicking.kick(p);
        p.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    /** The grab, as a cast: a press takes hold, or lets go (and throws). Free, so a hold can always be released. */
    private static boolean grab(ServerPlayer p) {
        io.github.soundgoodizerfan.feedback.combat.grab.Grabbing.press(p);
        return true;
    }

    /** The nearest living thing in front within {@code range}, in a cone, or null. */
    static LivingEntity ahead(ServerPlayer p, double range, double minDot) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : near(p, eye.add(look.scale(range / 2)), range / 2 + 1)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.lengthSqr();
            if (d < bestD && to.normalize().dot(look) > minDot) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Holding h = HOLDING.get(p.getUUID());
        if (h != null) {
            if (!h.cast().hold().tick().test(p))
                HOLDING.remove(p.getUUID());
            else if (!Stamina.spend(p, h.cast().hold().drain()))
                letGo(p);
        }
        CastState st = CastState.of(p);
        List<Integer> waits = new ArrayList<>();
        for (String tech : st.slots()) {
            Cast c = find(tech);
            waits.add(c == null ? 0 : !Skills.gearAllows(p, c.id()) ? WRONG_GEAR
                    : !c.rearms() || c.id().equals("coin_flip") && CoinShot.followUp(p) ? 0 : Rearms.wait(p, c.id()));
        }
        if (!waits.equals(st.waits()))
            CastState.set(p, st.withWaits(waits));
    }

    /** A parry has just landed on {@code defender} for {@code amount} of damage (Parrying calls this): Reckoning banks it, Reprisal answers it. */
    public static void parried(LivingEntity defender, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(defender instanceof ServerPlayer p))
            return;
        Reckoning.bank(p, amount);
        Reprisal.answer(p, source);
        // Wind back: a parry made on a low bar gives breath back.
        if (Stamina.get(p) < Stamina.LOW && Skills.unlocked(p, "wind_back") && Rearms.ready(p, "wind_back")) {
            Stamina.gain(p, WIND_BACK);
            Rearms.spend(p, "wind_back");
            CastFx.sound(p.serverLevel(), p.position(), net.minecraft.sounds.SoundEvents.PLAYER_BREATH, 0.8f, 1.3f);
        }
    }
}
