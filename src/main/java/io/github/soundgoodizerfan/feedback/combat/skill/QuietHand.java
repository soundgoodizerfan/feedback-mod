// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;

/**
 * Veil's quiet hand: the vibrations a player's footsteps and hands give off carry a fraction nearer. Vanilla
 * gives each listener (sculk sensor 8 blocks, warden 16) its own reach, and an event is heard by whoever is
 * inside it; here the event is taken from vanilla and delivered by the same walk of the listener registry,
 * but a listener is reached only inside {@code reach * (1 - hush)}. A listener that would have heard and
 * does not is what pays Veil's trickle. A blow's own vibration is quieted the same way by Dull edge and Hush the kill. Server side, no client state.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class QuietHand {

    private QuietHand() {
    }

    /** Which figure quiets an event, or null for one that is not a footstep or a hand. */
    private static SkillTree.Figure figureOf(Holder<GameEvent> e) {
        if (e.is(GameEvent.STEP))
            return SkillTree.Figure.STEP_HUSH;
        if (e.is(GameEvent.BLOCK_DESTROY) || e.is(GameEvent.BLOCK_PLACE) || e.is(GameEvent.BLOCK_ACTIVATE)
                || e.is(GameEvent.BLOCK_DEACTIVATE) || e.is(GameEvent.CONTAINER_OPEN) || e.is(GameEvent.CONTAINER_CLOSE))
            return SkillTree.Figure.HAND_HUSH;
        return null;
    }

    /** Per player: the game tick of a blow that is meant to kill an unaware creature quietly (Hush the kill). */
    private static final java.util.Map<java.util.UUID, Long> HUSHED_BLOW = new java.util.HashMap<>();

    /** Stealth share a weapon needs for Hush the kill, the hush a dull edge gives a blow, and the hardness under which an edge is dull ([OPEN]). */
    static final float HUSH_KILL_STEALTH = 0.5f, DULL_HUSH = 0.2f, DULL_HARDNESS = 2.5f;

    /** The vibration a blow gives off (the damage and death events): a dull edge clanks less, and a quiet kill makes none. */
    private static float blowHush(ServerPlayer player, Holder<GameEvent> e) {
        if (!e.is(GameEvent.ENTITY_DAMAGE) && !e.is(GameEvent.ENTITY_DIE))
            return 0f;
        if (HUSHED_BLOW.getOrDefault(player.getUUID(), -1L) == player.level().getGameTime())
            return 1f;
        if (Skills.unlocked(player, "dull_edge")) {
            var f = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.figures(player.getMainHandItem());
            if (f != null && f.get(io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.HARDNESS) < DULL_HARDNESS)
                return DULL_HUSH;
        }
        return 0f;
    }

    /** Hush the kill: a blow that will kill an unaware creature, from a stealthy weapon, makes no vibration. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onBlow(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p
                || !Skills.unlocked(p, "hush_the_kill"))
            return;
        var target = event.getEntity();
        if (event.getAmount() >= target.getHealth() && Weakness.unaware(target)
                && io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile.meleeOf(p).stealth() >= HUSH_KILL_STEALTH)
            HUSHED_BLOW.put(p.getUUID(), p.level().getGameTime());
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (!(event.getCause() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level))
            return;
        SkillTree.Figure figure = figureOf(event.getVanillaEvent());
        float hush;
        if (figure != null)
            hush = Math.min(0.75f, Skills.modifier(player, figure));
        else
            hush = blowHush(player, event.getVanillaEvent());
        if (hush <= 0f)
            return;
        event.setCanceled(true);
        Holder<GameEvent> gameEvent = event.getVanillaEvent();
        Vec3 pos = event.getEventPosition();
        GameEvent.Context context = event.getContext();
        List<GameEvent.ListenerInfo> heard = new ArrayList<>();
        GameEventListenerRegistry.ListenerVisitor visitor = (listener, at) -> {
            double reach = listener.getListenerRadius() * (1f - hush);
            if (at.distanceToSqr(pos) > reach * reach) {
                SkillEarning.hushed(player, at);
                return;
            }
            if (listener.getDeliveryMode() == GameEventListener.DeliveryMode.BY_DISTANCE)
                heard.add(new GameEvent.ListenerInfo(gameEvent, pos, context, listener, at));
            else
                listener.handleGameEvent(level, gameEvent, context, pos);
        };
        int r = gameEvent.value().notificationRadius();
        int x0 = SectionPos.blockToSectionCoord((int) Math.floor(pos.x) - r), x1 = SectionPos.blockToSectionCoord((int) Math.floor(pos.x) + r);
        int y0 = SectionPos.blockToSectionCoord((int) Math.floor(pos.y) - r), y1 = SectionPos.blockToSectionCoord((int) Math.floor(pos.y) + r);
        int z0 = SectionPos.blockToSectionCoord((int) Math.floor(pos.z) - r), z1 = SectionPos.blockToSectionCoord((int) Math.floor(pos.z) + r);
        for (int cx = x0; cx <= x1; cx++)
            for (int cz = z0; cz <= z1; cz++) {
                ChunkAccess chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null)
                    for (int cy = y0; cy <= y1; cy++)
                        chunk.getListenerRegistry(cy).visitInRangeListeners(gameEvent, pos, context, visitor);
            }
        java.util.Collections.sort(heard);
        for (GameEvent.ListenerInfo info : heard)
            info.recipient().handleGameEvent(level, info.gameEvent(), info.context(), info.source());
    }
}
