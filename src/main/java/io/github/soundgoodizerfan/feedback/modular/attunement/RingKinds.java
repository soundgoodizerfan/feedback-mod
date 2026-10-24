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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.modular.attunement;

import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Ring effect ({@code docs/feedback_enchant_roster.md} 27, sound). Weapon: {@code ring}, a
 * landed blow rings (a loud vibration for sculk, and hostile mobs without a target come to look); {@code hush} (the
 * mirror), the blow and the victim's hurt make no vibration. Armour: {@code worn_ring}, steps and hits taken ring;
 * {@code worn_hush}, steps and hits taken make no vibration. Nothing wears.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RingKinds {

    /** Blocks hostile mobs hear a ring from, per level. [OPEN] until played */
    private static final double HEAR_PER_LEVEL = 8;
    /** Ticks between a ringing wearer's steps. [OPEN] until played */
    private static final int STEP_PERIOD = 20;
    private static final String HUSH = "feedback_hush";

    private RingKinds() {
    }

    public static void register() {
        onStrike("ring", (event, a) -> {
            if (event.getSource().getEntity() instanceof LivingEntity who)
                ring(who, a.level());
        });
        onStrike("hush", (event, a) -> {
            // Marks both ends for this tick; onGameEvent drops the vibrations they cause.
            long now = event.getEntity().level().getGameTime();
            event.getEntity().getPersistentData().putLong(HUSH, now);
            event.getSource().getEntity().getPersistentData().putLong(HUSH, now);
        });
    }

    private static void ring(Entity who, int level) {
        who.level().gameEvent(who, GameEvent.EXPLODE, who.position());
        for (Mob mob : who.level().getEntitiesOfClass(Mob.class, who.getBoundingBox().inflate(HEAR_PER_LEVEL * level),
                m -> m instanceof Enemy && m.getTarget() == null))
            mob.getNavigation().moveTo(who, 1.0);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % STEP_PERIOD != 0 || !p.onGround() || p.isCrouching()
                || p.getDeltaMovement().horizontalDistanceSqr() < 0.0025)
            return;
        int n = BaitKinds.worn(p, "worn_ring");
        if (n > 0)
            ring(p, n);
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(victim instanceof Player))
            return;
        int n = BaitKinds.worn(victim, "worn_ring");
        if (n > 0)
            ring(victim, n);
        if (BaitKinds.worn(victim, "worn_hush") > 0)
            victim.getPersistentData().putLong(HUSH, victim.level().getGameTime());
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        Entity cause = event.getCause();
        if (cause == null || event.getLevel().isClientSide)
            return;
        // A marked entity is silent for the tick of the blow or hit; a hushed wearer is silent in step too.
        if (cause.getPersistentData().getLong(HUSH) == event.getLevel().getGameTime()
                && (event.getVanillaEvent().is(GameEvent.ENTITY_DAMAGE.key()) || event.getVanillaEvent().is(GameEvent.ENTITY_ACTION.key())))
            event.setCanceled(true);
        else if (cause instanceof Player p && event.getVanillaEvent().is(GameEvent.STEP.key())
                && BaitKinds.worn(p, "worn_hush") > 0)
            event.setCanceled(true);
    }
}
