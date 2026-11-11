// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.List;
import java.util.Map;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * What differs between the families of gun: where a shot's energy and its projectile come from. A {@link GunItem} owns one. Everything
 * else about a gun is shared and lives outside the mechanism: the client's input (fire, hold, mode, aim, reload key), the HUD frame,
 * the cycle gate, the swept bullet and its hits, the kick and strain, the pose and the look.
 *
 * <p>The three families are the firearm (a powder charge in a case, {@code FirearmMechanism}), the electric gun (a cell and a metal
 * slug, {@code ElectricMechanism}) and the gas gun (a spring or a compressed gas; not built as a gun item yet).
 */
public interface GunMechanism {

    /** Whether {@code gun} is a built gun this mechanism can work with (it has a design it understands). */
    boolean built(ItemStack gun);

    /**
     * A player's pull on the server: the cycle gate, what is spent, the mistakes it costs, the shot launched through
     * {@link GunShots#fire}. The client names only the aim; it never says what is loaded.
     *
     * @return whether a shot left the muzzle.
     */
    boolean trigger(ServerLevel level, Player shooter, ItemStack gun, float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand);

    /** A mob's pull; a mechanism with no way for a mob to feed it returns false. */
    boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch);

    /**
     * What the next shot would do, for the client to predict (its cycle, kick, sound), or null if the gun cannot fire. The same
     * record the server fires from, so the two agree.
     *
     * @param hand 0 for the main hand, 1 for the off hand (a per-hand cache key).
     */
    GunFigures predict(Player player, ItemStack gun, Level level, Map<ResourceLocation, Material> materials, int hand);

    /** How far aiming zooms the view (a field-of-view multiplier, 1 for none). */
    float sightZoom(ItemStack gun);

    /** Milliseconds the sight takes to come up. */
    int raiseMs(ItemStack gun);

    /** The fire mode the gun's action allows for the one set. */
    FireMode allow(ItemStack gun, FireMode set);

    /** The fire mode after {@code current} in this gun's cycle of modes. */
    FireMode next(ItemStack gun, FireMode current);

    /** The sound of a shot as layers. */
    List<GunSound.Layer> sound(ItemStack gun, GunFigures figures, RandomSource random);

    /** What shows at the muzzle. */
    SimpleParticleType muzzleParticle(ItemStack gun, GunFigures figures);
}
