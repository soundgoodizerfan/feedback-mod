// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Rubber rounds between players: a bullet whose projectile is a polymer is a rubber round, by kind,
 * never by item. Hitting a player with one grants "Restoring Order"; killing a player who once hit you
 * with one grants "Rage Against the Machine". The record of who hit whom lives for the session only.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RubberRounds {

    /** Victim to the players who have hit them with a rubber round. */
    private static final Map<UUID, Set<UUID>> HIT_BY = new HashMap<>();

    private RubberRounds() {
    }

    /** Whether a round with this projectile material is rubber: a polymer. */
    public static boolean isRubber(Material projectile) {
        return projectile != null && projectile.kind().filter(k -> k == Material.Kind.POLYMER).isPresent();
    }

    /** A rubber round from {@code shooter} landed on {@code target}. */
    static void landed(LivingEntity shooter, LivingEntity target) {
        if (!(shooter instanceof ServerPlayer from) || !(target instanceof Player to) || from == to)
            return;
        HIT_BY.computeIfAbsent(to.getUUID(), k -> new HashSet<>()).add(from.getUUID());
        award(from, "restoring_order");
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player dead) || !(event.getSource().getEntity() instanceof ServerPlayer killer))
            return;
        Set<UUID> hitters = HIT_BY.get(killer.getUUID());
        if (hitters != null && hitters.contains(dead.getUUID()))
            award(killer, "rage_against_the_machine");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HIT_BY.clear();
    }

    private static void award(ServerPlayer player, String name) {
        var holder = player.server.getAdvancements().get(Feedback.id("combat/" + name));
        if (holder != null)
            player.getAdvancements().award(holder, "done");
    }
}
