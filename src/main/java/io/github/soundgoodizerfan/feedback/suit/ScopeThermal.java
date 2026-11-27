// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A scope's thermal fitting (gun role {@code g}, on the sight's {@code scope_mount}): while the gun is aimed the same temperature scan the suit's
 * thermal sight runs feeds the wearer's thermal view ({@link ThermalScan}), reaching further the more the scope magnifies, so a long scope sees
 * heat far off and a short one close. Server and client both ask {@link #fitted}; the server scans while the player aims.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ScopeThermal {

    private ScopeThermal() {
    }

    private static final Set<UUID> AIMING = new HashSet<>();
    /** The magnification a scope's range is quoted at: the sight's default zoom. */
    private static final float REF_ZOOM = 2.5f;

    /** Whether {@code gun} is a built gun carrying a thermal fitting. */
    public static boolean fitted(ItemStack gun) {
        var design = ModularStacks.design(gun);
        return design != null && design.part("g") != null && design.type().getPath().equals("gun");
    }

    /** How far, in blocks, the fitted scope sees heat: the base range times the sight's zoom against its default. */
    public static float range(ItemStack gun) {
        var design = ModularStacks.design(gun);
        var type = design == null ? null : ModularTypes.known(design.type());
        var sight = design == null ? null : design.part("S");
        var role = type == null ? null : type.roles().get("S");
        float zoom = role == null || sight == null ? REF_ZOOM : role.axis(sight, "zoom");
        return Math.min(SuitTuning.THERMAL_MAX_RANGE * 2f, SuitTuning.THERMAL_BASE_RANGE * Math.max(1f, zoom / REF_ZOOM));
    }

    /** The player started or stopped aiming the gun in hand. */
    public static void aim(ServerPlayer player, ItemStack gun, boolean aiming) {
        if (aiming && fitted(gun))
            AIMING.add(player.getUUID());
        else
            AIMING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !AIMING.contains(p.getUUID()))
            return;
        ItemStack gun = p.getMainHandItem();
        if (!fitted(gun)) {
            AIMING.remove(p.getUUID());
            return;
        }
        if (p.tickCount % SuitTuning.THERMAL_SCAN_TICKS == 0 && io.github.soundgoodizerfan.feedback.weapon.SightCell.charged(gun))
            ThermalScan.scan(p, range(gun));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AIMING.remove(event.getEntity().getUUID());
    }
}
