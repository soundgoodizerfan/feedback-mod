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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Premature spatial transitions (ender spec 2): an undamped tunnelling-active fluid ({@link Material.Trait#SPATIAL}) that is let go
 * or held over its boiling point pushes whatever stands near it into the transition regime, the way chorus fruit does a
 * body, so items and creatures within {@link FTuning#SPATIAL_HOP_RADIUS} hop a short random distance. It gives no address,
 * only a nearby accessible state. Chance rises with the amount lost; a hop needs room (a spot with no collision), so an
 * entity in a sealed cell stays put. Callers are rate-limited (a reactor checks once a second).
 */
public final class SpatialHop {

    private SpatialHop() {
    }

    /** Whether {@code fluid} is spatially unstable by its material's traits. */
    public static boolean spatial(FluidStack fluid) {
        if (fluid.isEmpty())
            return false;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
        return m != null && m.traits().contains(Material.Trait.SPATIAL);
    }

    /** Whether {@code fluid} is spatially unstable and its temperature {@code tu} is at or over its material's boiling point. */
    public static boolean boiling(FluidStack fluid, float tu) {
        if (!spatial(fluid))
            return false;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
        return m.thermal().boilingPoint().map(b -> tu >= b.value()).orElse(false);
    }

    /** A fluid with nowhere to go: if it is spatially unstable, hops what stands near {@code at}. Server side. */
    public static void loose(Level level, Vec3 at, FluidStack lost) {
        if (spatial(lost))
            hop(level, at, lost.getAmount());
    }

    /** Hops items and creatures within the radius of {@code at}, each by a chance that grows with {@code mb}. */
    public static void hop(Level level, Vec3 at, int mb) {
        if (!(level instanceof ServerLevel server))
            return;
        float chance = Math.min(1f, FTuning.SPATIAL_HOP_BASE_CHANCE
                + (1f - FTuning.SPATIAL_HOP_BASE_CHANCE) * mb / FTuning.SPATIAL_HOP_MB_FOR_CERTAIN);
        float r = FTuning.SPATIAL_HOP_RADIUS;
        for (Entity e : server.getEntitiesOfClass(Entity.class, new AABB(at, at).inflate(r),
                x -> x instanceof LivingEntity || x instanceof ItemEntity)) {
            if (e.position().distanceTo(at) > r || level.random.nextFloat() >= chance)
                continue;
            for (int tries = 0; tries < 8; tries++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double d = FTuning.SPATIAL_HOP_MIN + level.random.nextDouble() * (FTuning.SPATIAL_HOP_MAX - FTuning.SPATIAL_HOP_MIN);
                double dx = Math.cos(angle) * d, dy = level.random.nextInt(4) - 1, dz = Math.sin(angle) * d;
                if (!level.noCollision(e, e.getBoundingBox().move(dx, dy, dz)))
                    continue;
                server.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.BLOCKS, 0.6f, 1f);
                e.teleportTo(e.getX() + dx, e.getY() + dy, e.getZ() + dz);
                server.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.BLOCKS, 0.6f, 1f);
                break;
            }
        }
    }
}
