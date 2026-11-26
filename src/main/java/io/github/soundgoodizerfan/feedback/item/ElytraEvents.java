// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import io.github.soundgoodizerfan.feedback.suit.SuitTuning;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A firework rocket used while gliding does nothing (the boost is refused, the rocket kept); crossbows and displays are untouched. A wing lifts
 * by the air under it: gliding in air thinner than the suit's reference density (the End's) sinks the wearer by the missing share, so the End is
 * a controlled descent, not a free glide. The air is the dimension's density curve ({@link AirDensityTable}), as for a suit's dash and thrust.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ElytraEvents {

    private ElytraEvents() {
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isFallFlying() && event.getItemStack().is(Items.FIREWORK_ROCKET)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** Thin air takes lift from a gliding wing: the missing share of density becomes extra sink each tick. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !p.isFallFlying())
            return;
        float density = Math.min(1f, AirDensityTable.at(p.level(), p.getY()) / SuitTuning.REF_AIR_DENSITY);
        if (density >= 1f)
            return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, v.y - VehicleTuning.ELYTRA_THIN_AIR_SINK * (1f - density), v.z);
        p.hurtMarked = true;
    }
}
