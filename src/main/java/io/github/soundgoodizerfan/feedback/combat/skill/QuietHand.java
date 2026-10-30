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
 * does not is what pays Veil's trickle. Server side, no client state.
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

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (!(event.getCause() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level))
            return;
        SkillTree.Figure figure = figureOf(event.getVanillaEvent());
        if (figure == null)
            return;
        float hush = Math.min(0.75f, Skills.modifier(player, figure));
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
