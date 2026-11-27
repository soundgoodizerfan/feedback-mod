// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/** Where a powered sight's line from the eye ends: the first block or entity within {@code max} blocks. */
final class SightRay {

    /** The point hit and its distance from the eye; {@code at} is null when the line reached {@code max} with nothing in it. */
    record Hit(Vec3 at, double distance) {
    }

    private SightRay() {
    }

    static Hit cast(Player player, double max) {
        Vec3 eye = player.getEyePosition(1f), look = player.getViewVector(1f);
        BlockHitResult block = player.level().clip(new ClipContext(eye, eye.add(look.scale(max)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? max : eye.distanceTo(block.getLocation());
        Vec3 end = eye.add(look.scale(reach));
        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box, e -> !e.isSpectator() && e.isPickable());
        if (entity != null)
            return new Hit(entity.getLocation(), eye.distanceTo(entity.getLocation()));
        return block.getType() == HitResult.Type.MISS ? new Hit(null, max) : new Hit(block.getLocation(), reach);
    }
}
