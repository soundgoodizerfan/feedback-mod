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
package io.github.soundgoodizerfan.feedback.combat.parry;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The parry, server side (implementation plan 2.2). A press opens a short active window nested inside a
 * longer committed one: the window turns a blow aside, and whatever the window did not catch, the
 * recovery behind it leaves the fighter open for. Block is a different verb and is untouched.
 *
 * <p>Resolution runs at HIGHEST in {@code LivingIncomingDamageEvent}, before the traits and perks see a
 * blow, so a parried hit never reaches them. A parry cancels the event and then pays out ({@link #result}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Parrying {

    private Parrying() {
    }

    /** A press. Does nothing unless the fighter is free and holds something that parries (or wears a Feedbacker). */
    public static void begin(ServerPlayer player) {
        CombatState s = CombatState.of(player);
        if (!s.ready() || player.isSpectator())
            return;
        // The ping pad lengthens the active window only, never the lockout, and is capped so a laggy
        // press is forgiven a little and a very laggy one is not forgiven a parry.
        int pad = Mth.clamp(player.connection.latency() / 50, 0, ParryTuning.PING_CAP);
        Optional<ParryStats> stats = ParryStats.of(player);
        int window, recovery;
        net.minecraft.world.InteractionHand hand;
        if (stats.isPresent()) {
            window = stats.get().window();
            recovery = stats.get().recovery();
            hand = stats.get().hand();
        } else if (!CombatGear.granting(player, ParryTuning.PUNCH_PROJECTILES).isEmpty()) {
            window = ParryTuning.FEEDBACKER_WINDOW + CombatGear.of(player).windowTicks();
            recovery = ParryTuning.FEEDBACKER_RECOVERY;
            hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        } else
            return;
        CombatState.set(player, new CombatState(CombatPhase.PARRY_ACTIVE, window + pad, recovery, s.iFrames()));
        player.swing(hand, true);
        CombatCues.everyone(player, FSounds.PARRY_RAISE.get(), FeelTuning.RAISE_VOLUME, FeelTuning.RAISE_PITCH);
    }

    /**
     * The Feedbacker (plan 0): a wearer in the active window punches any projectile about to land, through the
     * impact event rather than the damage event, because a fireball explodes on impact whatever the damage does.
     * A weapon in hand still parries melee the plain way. The punch is gated by the
     * {@code feedback:punch_projectiles} grant, never by what the projectile is.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity defender)
                || defender.level().isClientSide || projectile.getOwner() == defender || !defender.hasData(FCombat.STATE)
                || CombatState.of(defender).phase() != CombatPhase.PARRY_ACTIVE
                || !inCone(defender, projectile.position(), ParryTuning.FEEDBACKER_CONE_DEG))
            return;
        ItemStack gauntlet = CombatGear.granting(defender, ParryTuning.PUNCH_PROJECTILES);
        if (gauntlet.isEmpty())
            return;
        event.setCanceled(true);
        CombatState.set(defender, new CombatState(CombatPhase.IDLE, 0, 0, CombatState.of(defender).iFrames()));

        float speedIn = Math.max(0.5f, (float) projectile.getDeltaMovement().length());
        wear(defender, gauntlet, projectile.getBbWidth() * projectile.getBbWidth() * projectile.getBbHeight() * ParryTuning.BBOX_DENSITY, speedIn);

        turn(defender, projectile, speedIn * ParryTuning.FEEDBACKER_SPEED);
    }

    /**
     * A bullet about to hit {@code defender}: a wearer of the gauntlet in the active window inside its cone
     * punches it back (negated, then {@link GunShots#strike} along the line to the shooter at
     * {@link ParryTuning#FEEDBACKER_SPEED} times the damage, attributed to the wearer). Bullets are swept
     * segments with no entity, so this is called by {@code GunShots} rather than heard as an event.
     * Only the gauntlet turns a bullet: a weapon parry ignores it (see {@link #onIncoming}).
     * @return whether the bullet was caught and must not land.
     */
    public static boolean catchBullet(ServerLevel level, LivingEntity shooter, LivingEntity defender, float massMu, float speed, float damage) {
        if (!defender.hasData(FCombat.STATE) || CombatState.of(defender).phase() != CombatPhase.PARRY_ACTIVE
                || !inCone(defender, shooter.position(), ParryTuning.FEEDBACKER_CONE_DEG))
            return false;
        ItemStack gauntlet = CombatGear.granting(defender, ParryTuning.PUNCH_PROJECTILES);
        if (gauntlet.isEmpty())
            return false;
        CombatState.set(defender, new CombatState(CombatPhase.IDLE, 0, 0, CombatState.of(defender).iFrames()));
        io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.parried(defender, GunShots.attack(level, shooter));
        wear(defender, gauntlet, massMu, speed);
        GunShots.strike(level, defender, defender.getEyePosition(), shooter.getBoundingBox().getCenter(),
                damage * ParryTuning.FEEDBACKER_SPEED);
        return true;
    }

    /**
     * Impulse against the gauntlet's joint rating (philosophy 6): force that cannot go into the punch goes
     * into the gauntlet. Over the rating it is still punched, and the gauntlet pays for it, down to its last
     * point and never past it. Also fires the feel.
     */
    private static void wear(LivingEntity defender, ItemStack gauntlet, float mass, float speed) {
        float ratio = mass * speed / (ParryTuning.FEEDBACKER_RATING * (1f + CombatGear.of(defender).joint()));
        int cost = ParryTuning.WEAR_CLEAN + Math.round(ratio * ParryTuning.FEEDBACKER_WEAR_PER_RATING)
                + (ratio > 1f ? ParryTuning.FEEDBACKER_WRECK_WEAR : 0);
        gauntlet.hurtAndBreak(ModularStacks.cap(gauntlet, cost), defender, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        if (defender instanceof ServerPlayer player) {
            CombatCues.everyone(player, FSounds.FEEDBACKER_PARRY.get(), 1f, 1f);
            (ratio > 0.5f ? FeelTuning.HEAVY : FeelTuning.LIGHT).fire(player, player.getEyePosition().add(player.getLookAngle()));
        }
    }

    /** Hand {@code projectile} to {@code defender} and send it along their look at {@code speed}, at the end of the tick. */
    private static void turn(LivingEntity defender, Projectile projectile, float speed) {
        Vec3 look = defender.getLookAngle();
        projectile.setOwner(defender);
        // The projectile's own tick runs on after the hit and would overwrite a velocity set now.
        defender.getServer().execute(() -> {
            if (projectile.isRemoved())
                return;
            if (projectile instanceof AbstractHurtingProjectile hurting) {
                // A fireball is driven by its acceleration as well as its velocity: the shot below points the velocity along the look.
                hurting.accelerationPower *= ParryTuning.FEEDBACKER_SPEED;
            }
            projectile.shoot(look.x, look.y, look.z, speed, Math.max(0f, ParryTuning.REDIRECT_INACCURACY
                    + io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(defender,
                            io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.REDIRECT_INACCURACY)));
            projectile.hurtMarked = true;
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity defender = event.getEntity();
        if (defender.level().isClientSide || !defender.hasData(FCombat.STATE)
                || CombatState.of(defender).phase() != CombatPhase.PARRY_ACTIVE)
            return;
        DamageSource source = event.getSource();
        // A bullet arrives as a plain attack by the shooter; read as melee it would stagger and shove the
        // shooter. Only the gauntlet turns a bullet (catchBullet); a weapon parry lets it through.
        if (GunShots.inBullet() || !parryable(source))
            return;
        Optional<ParryStats> stats = ParryStats.of(defender);
        Entity direct = source.getDirectEntity();
        Vec3 from = direct != null ? direct.position() : source.getSourcePosition();
        if (stats.isEmpty() || from == null || !inCone(defender, from, stats.get().coneHalfDeg())
                || stats.get().projectilesOnly() && !(direct instanceof Projectile))
            return;
        event.setCanceled(true);
        // A parry that lands is spent, not held: the fighter is free at once (the reward for the timing).
        CombatState.set(defender, new CombatState(CombatPhase.IDLE, 0, 0, CombatState.of(defender).iFrames()));
        result(defender, source, direct, event.getAmount(), stats.get());
    }

    /** Game tick each fighter's riposte window closes. Server only; a stale entry is harmless (compared to the clock). */
    private static final java.util.Map<java.util.UUID, Long> RIPOSTE_UNTIL = new java.util.HashMap<>();

    /**
     * Riposte (skill unlock): the first strike a parrier lands inside the window a landed parry opened is
     * boosted by {@link ParryTuning#RIPOSTE_MULT} and spends the window. Runs after the parry's own cancel
     * (a cancelled event is not heard), before armour and enchantments reduce the amount.
     */
    @SubscribeEvent
    public static void onRiposte(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || GunShots.inBullet())
            return;
        Long until = RIPOSTE_UNTIL.remove(player.getUUID());
        if (until == null || player.level().getGameTime() > until)
            return;
        event.setAmount(event.getAmount() * ParryTuning.RIPOSTE_MULT);
        CombatCues.everyone(player, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 1.5f);
    }

    /** Melee, projectile and thrown. Not fire, magic, explosion or a fall: those are not struck at the guard. */
    static boolean parryable(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION))
            return false;
        return source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    /** Whether {@code from} lies within {@code halfDeg} of where {@code who} looks, measured flat. */
    static boolean inCone(LivingEntity who, Vec3 from, float halfDeg) {
        Vec3 to = from.subtract(who.position()).multiply(1, 0, 1);
        Vec3 look = who.getViewVector(1f).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-6 || look.lengthSqr() < 1.0E-6)
            return true;
        return to.normalize().dot(look.normalize()) >= Math.cos(Math.toRadians(halfDeg));
    }

    /** The parry's payout: the attacker staggered, the weapon worn by the excess, the feel, a projectile turned. */
    private static void result(LivingEntity defender, DamageSource source, Entity direct, float amount, ParryStats stats) {
        Entity attacker = source.getEntity();
        io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.parried(defender, source);
        float impact = amount * ParryTuning.JOULES_PER_DAMAGE;
        if (defender instanceof ServerPlayer p && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "riposte"))
            RIPOSTE_UNTIL.put(p.getUUID(), p.level().getGameTime() + ParryTuning.RIPOSTE_TICKS);
        if (attacker instanceof LivingEntity living) {
            ModularFigures held = ModularStacks.figures(living.getMainHandItem());
            if (direct == attacker && held != null && held.get(ModularFigures.Fig.IMPACT) > 0)
                impact = held.get(ModularFigures.Fig.IMPACT);
            if (direct == attacker) {
                // STAND-IN mass: bounding-box volume times a density, for want of a mass on mobs (plan 5.18).
                int ticks = Math.round(ParryTuning.staggerTicks(impact, ParryTuning.standInMass(living))
                        * Math.max(0f, 1 + io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(defender,
                                io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.STAGGER_POWER)));
                CombatTicker.stagger(living, ticks, ParryTuning.STAGGER_SLOW);
                if (stats.knock() > 0)
                    living.knockback(stats.knock(), defender.getX() - living.getX(), defender.getZ() - living.getZ());
            }
        }

        // Force beyond what the weapon was built to take goes into the weapon (philosophy 6): a blow
        // within its rating is a clean parry and costs a point, one far over it costs a lot.
        float over = impact / (stats.mass() * ParryTuning.RATING_PER_MU);
        int cost = ParryTuning.WEAR_CLEAN + (over > 1 ? (int) Math.ceil((over - 1) * ParryTuning.WEAR_PER_OVER) : 0);
        stats.stack().hurtAndBreak(ModularStacks.cap(stats.stack(), cost), defender, LivingEntity.getSlotForHand(stats.hand()));

        if (defender instanceof ServerPlayer player)
            (over > 1 ? FeelTuning.HEAVY : FeelTuning.LIGHT)
                    .fire(player, player.getEyePosition().add(player.getLookAngle()));

        if (direct instanceof Projectile projectile) {
            turn(defender, projectile, Math.max(0.5f, (float) projectile.getDeltaMovement().length()) * stats.redirect());
        }
    }
}
