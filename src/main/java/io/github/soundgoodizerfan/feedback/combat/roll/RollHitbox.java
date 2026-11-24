// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.roll;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * While a roll lasts the body is low: the hitbox takes the crawl pose's height ({@link #LOW_HEIGHT}, as vanilla crawling) and keeps its width, so a
 * rolling person goes under a one-block gap and a shot that would have caught their head goes over. Both sides run it (the server decides what is
 * hit, the client what it collides with and draws) from the synced combat state. When the roll ends the box goes back to standing height only if
 * there is headroom; under a low ceiling it stays low until there is, rather than leaving the person inside the block.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RollHitbox {

    /** The crawl pose's height, blocks, and its eye height. */
    public static final float LOW_HEIGHT = 0.6f, LOW_EYES = 0.4f;
    private static final String KEY = "feedback_roll_low";

    private RollHitbox() {
    }

    /** Whether the roll (its dodge or its recovery) is still on {@code player}. */
    private static boolean rolling(Player player) {
        if (!player.hasData(FCombat.STATE))
            return false;
        CombatPhase phase = CombatState.of(player).phase();
        return phase == CombatPhase.ROLL || phase == CombatPhase.ROLL_RECOVERY;
    }

    /** Whether a standing person fits where {@code player} is. */
    private static boolean fitsStanding(Player player) {
        float w = player.getType().getDimensions().width();
        AABB standing = new AABB(player.getX() - w / 2, player.getY(), player.getZ() - w / 2, player.getX() + w / 2, player.getY() + player.getType().getDimensions().height(), player.getZ() + w / 2);
        return player.level().noCollision(player, standing);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean low = player.getPersistentData().getBoolean(KEY);
        boolean want = rolling(player) || low && !fitsStanding(player);
        if (want != low) {
            player.getPersistentData().putBoolean(KEY, want);
            player.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && player.getPersistentData().getBoolean(KEY))
            event.setNewSize(EntityDimensions.scalable(event.getNewSize().width(), LOW_HEIGHT).withEyeHeight(LOW_EYES));
    }
}
