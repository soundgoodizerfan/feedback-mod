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
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

    /** A press. Does nothing unless the fighter is free and holds something that parries. */
    public static void begin(ServerPlayer player) {
        CombatState s = CombatState.of(player);
        if (!s.ready() || player.isSpectator())
            return;
        ParryStats.of(player).ifPresent(st -> {
            // The ping pad lengthens the active window only, never the lockout, and is capped so a laggy
            // press is forgiven a little and a very laggy one is not forgiven a parry.
            int pad = Mth.clamp(player.connection.latency() / 50, 0, ParryTuning.PING_CAP);
            CombatState.set(player, new CombatState(CombatPhase.PARRY_ACTIVE, st.window() + pad, st.recovery(), s.iFrames()));
            player.swing(st.hand(), true);
            CombatCues.everyone(player, FSounds.PARRY_RAISE.get(), FeelTuning.RAISE_VOLUME, FeelTuning.RAISE_PITCH);
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity defender = event.getEntity();
        if (defender.level().isClientSide || !defender.hasData(FCombat.STATE)
                || CombatState.of(defender).phase() != CombatPhase.PARRY_ACTIVE)
            return;
        DamageSource source = event.getSource();
        if (!parryable(source))
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
            Vec3 look = defender.getLookAngle();
            float speed = Math.max(0.5f, (float) projectile.getDeltaMovement().length()) * stats.redirect();
            projectile.setOwner(defender);
            // The projectile's own bounce-off code runs after this event and would overwrite a velocity set
            // now, so the turn waits for the end of the tick.
            defender.getServer().execute(() -> {
                if (!projectile.isRemoved()) {
                    projectile.shoot(look.x, look.y, look.z, speed, Math.max(0f, ParryTuning.REDIRECT_INACCURACY
                            + io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(defender,
                                    io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.REDIRECT_INACCURACY)));
                    projectile.hurtMarked = true;
                }
            });
        }
        // Bullets are swept segments with no entity (GunShots), so they are cancelled here but not turned;
        // redirecting a bullet is the gun-fu hook, not built.
    }
}
