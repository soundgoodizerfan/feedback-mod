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

import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    /**
     * A gliding wing sinks by three things: thin air (the missing share of density), load (the glider's mass over the reference of a bare
     * body: armour is carried on the wing, so a heavy suit stalls it) and wear (a worn membrane lifts less, by its worn share). Each becomes
     * extra sink each tick. Past the load the phantom membrane's yield strength rates it for, the wing tears: the elytra takes extra
     * damage each second by how far over it is. Wear comes from misuse only, a bare or lightly armoured glide never wears it past vanilla's.
     */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !p.isFallFlying())
            return;
        float density = Math.min(1f, AirDensityTable.at(p.level(), p.getY()) / SuitTuning.REF_AIR_DENSITY);
        float over = BodyMass.armour(p) / ParryTuning.PLAYER_MASS;
        ItemStack wing = p.getItemBySlot(EquipmentSlot.CHEST);
        float worn = wing.isDamageableItem() ? wing.getDamageValue() / (float) wing.getMaxDamage() : 0f;
        float sink = VehicleTuning.ELYTRA_THIN_AIR_SINK * (1f - density) + VehicleTuning.ELYTRA_LOAD_SINK * over
                + VehicleTuning.ELYTRA_WORN_SINK * worn;
        if (over > membraneRating() && wing.isDamageableItem() && p.tickCount % 20 == 0)
            wing.hurtAndBreak(Math.max(1, Math.round((over - membraneRating()) * VehicleTuning.ELYTRA_TEAR_PER_EXCESS)), p, EquipmentSlot.CHEST);
        if (sink <= 0f)
            return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, v.y - sink, v.z);
        p.hurtMarked = true;
    }

    /** The extra load share (over a bare body) the phantom membrane carries before it tears: its {@code yield_strength} times the share per St. */
    private static float membraneRating() {
        var m = MaterialTable.get().materials().get(Feedback.id("phantom_membrane"));
        float yield = m == null ? 0f : m.mechanical().yieldStrength().map(St::value).orElse(0f);
        return yield * VehicleTuning.ELYTRA_RATING_PER_ST;
    }
}
