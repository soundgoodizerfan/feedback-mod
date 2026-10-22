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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.hit;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The volume a melee swing covers: a thick line from the eye along the look, as long as the wielder's
 * interaction range (which the bake already sets from the weapon's reach) and as wide as its head is
 * heavy. Pure geometry over the world, no state, so the client runs the same function to predict which
 * targets the server will find and the two agree up to entity position lag.
 *
 * <p>Width has no baked figure of its own yet (there is no head-shape field, see the integration map
 * section 9), so it is read off {@link Fig#BALANCE}: a spear's head is a point on a long haft (near 0), a
 * cleaver or maul is nearly all head (near 1). A real {@code width} on the head variant replaces this.
 * The line is not an arc; a wide head covers the same angle as a half-width over its length, which is
 * the cheap version of a sweep. Blocks stop it.
 */
public final class SweptVolume {

    private SweptVolume() {
    }

    /** The half-width of {@code player}'s main-hand melee weapon's volume, or empty if it has none or the sweep is off. */
    public static Optional<Float> halfWidth(Player player) {
        if (!ModularTuning.MELEE_SWEEP)
            return Optional.empty();
        ItemStack weapon = player.getMainHandItem();
        var design = ModularStacks.design(weapon);
        var figures = ModularStacks.figures(weapon);
        if (design == null || figures == null || ModularStacks.broken(weapon))
            return Optional.empty();
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null || type.family() != ModularType.Family.BLADE && type.family() != ModularType.Family.MACE
                && type.family() != ModularType.Family.SPEAR)
            return Optional.empty();
        return Optional.of(ModularTuning.SWEEP_HALF_WIDTH_MIN
                + ModularTuning.SWEEP_HALF_WIDTH_BALANCE * Math.max(0f, Math.min(1f, figures.get(Fig.BALANCE))));
    }

    /** The living things the volume covers, nearest first, never the wielder, its mount or an ally. */
    public static List<LivingEntity> targets(Player player, float halfWidth) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1f).scale(player.entityInteractionRange()));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS)
            end = block.getLocation();
        Vec3 stop = end;
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, stop).inflate(halfWidth + 1),
                        e -> e != player && e != player.getVehicle() && e.isAlive() && !e.isSpectator() && e.isPickable()
                                && !player.isAlliedTo(e)
                                && e.getBoundingBox().inflate(halfWidth).clip(eye, stop).isPresent())
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(ModularTuning.SWEEP_MAX_TARGETS)
                .toList();
    }
}
