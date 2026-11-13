// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Hobnails (spec 5.3, armour addon on boots): iron nails bite the ground. On a slippery block (ice: friction above
 * {@code slip_friction}) the wearer's horizontal speed beyond a walk is damped by {@code grip_mult} each tick, so
 * ice does not carry them; but the nails ring on stone, and mobs judge the wearer {@code visibility_mult} as
 * visible. Worn: counts once.
 */
public final class Hobnails implements ModularTrait {

    /** Horizontal blocks per tick of a plain walk on firm ground; speed beyond it is what the ice gave. */
    private static final double WALK = 0.15;

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        if (!wearer.onGround())
            return;
        BlockPos below = wearer.getBlockPosBelowThatAffectsMyMovement();
        if (wearer.level().getBlockState(below).getFriction(wearer.level(), below, wearer) <= params.get("slip_friction", 0.9f))
            return;
        Vec3 v = wearer.getDeltaMovement();
        double speed = Math.hypot(v.x, v.z);
        if (speed <= WALK)
            return;
        double keep = WALK + (speed - WALK) * (1f - params.get("grip_mult", 0.3f));
        wearer.setDeltaMovement(v.x / speed * keep, v.y, v.z / speed * keep);
        wearer.hurtMarked = true;
    }

    @Override
    public void onWornVisibility(LivingEvent.LivingVisibilityEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        event.modifyVisibility(params.get("visibility_mult", 1.15f));
    }
}
