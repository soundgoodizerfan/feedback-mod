// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Pavise: crouched behind a shield for four seconds, every arrow, bolt and bullet that comes at you from the front is stopped,
 * not turned. The shield wears for the energy it stopped; you cannot move off the spot.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Pavise {

    private Pavise() {
    }

    /** STAND-INS ([OPEN]): ticks held, how far the planted guard may be left, damage a point of shield wear is worth. */
    private static final int TICKS = 80;
    private static final double DRIFT = 0.6;
    private static final float DAMAGE_PER_WEAR = 2f;

    private record Plant(long until, Vec3 spot) {
    }

    private static final Map<UUID, Plant> PLANTS = new HashMap<>();

    /** Pavise: crouch behind a shield and plant it for four seconds. */
    static boolean act(ServerPlayer p) {
        if (!p.isShiftKeyDown() || shield(p).isEmpty())
            return false;
        PLANTS.put(p.getUUID(), new Plant(p.level().getGameTime() + TICKS, p.position()));
        CastFx.pulse(p.serverLevel(), p.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.ICE, 1.6f, 9);
        CastFx.sound(p.serverLevel(), p.position(), FSounds.CAST_GUARD.get(), 0.8f, 0.7f);
        return true;
    }

    private static ItemStack shield(ServerPlayer p) {
        return p.getMainHandItem().getUseAnimation() == UseAnim.BLOCK ? p.getMainHandItem()
                : p.getOffhandItem().getUseAnimation() == UseAnim.BLOCK ? p.getOffhandItem() : ItemStack.EMPTY;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !PLANTS.containsKey(p.getUUID()))
            return;
        Plant plant = PLANTS.get(p.getUUID());
        var source = event.getSource();
        if (!source.is(DamageTypeTags.IS_PROJECTILE) && !GunShots.inBullet())
            return;
        Vec3 from = source.getSourcePosition();
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (from == null || look.lengthSqr() < 1e-6 || from.subtract(p.position()).multiply(1, 0, 1).normalize().dot(look.normalize()) < 0.2)
            return;
        ItemStack shield = shield(p);
        if (shield.isEmpty()) {
            PLANTS.remove(p.getUUID());
            return;
        }
        if (shield.isDamageableItem())
            shield.hurtAndBreak(Math.max(1, Math.round(event.getAmount() / DAMAGE_PER_WEAR)), p,
                    shield == p.getMainHandItem() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        event.setCanceled(true);
        Vec3 at = p.position().add(0, 1.1, 0).add(look.normalize().scale(0.7));
        CastFx.shards(p.serverLevel(), at, CastFx.ICE, 0.8f, 5);
        CastFx.sound(p.serverLevel(), at, FSounds.CAST_GUARD.get(), 1f, 1.1f);
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !PLANTS.containsKey(p.getUUID()))
            return;
        Plant plant = PLANTS.get(p.getUUID());
        if (p.level().getGameTime() >= plant.until() || !p.isShiftKeyDown() || shield(p).isEmpty()
                || p.position().distanceTo(plant.spot()) > DRIFT)
            PLANTS.remove(p.getUUID());
    }
}
