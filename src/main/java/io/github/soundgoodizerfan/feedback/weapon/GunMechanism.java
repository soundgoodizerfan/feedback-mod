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
 * <p>The three families are the firearm (a powder charge in a case, {@code FirearmMechanism}), the pulse gun (a cell and a pulse
 * seat, {@code ElectricMechanism}) and the gas gun (a spring or a compressed gas; not built as a gun item yet).
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

    /** Whether the gun reads how long the trigger is held (a flywheel's spin, a hold charge): the client then tells the server when it goes down and comes up. */
    default boolean tracksTrigger(ItemStack gun) {
        return false;
    }

    /** Whether a pull is made on the release of the trigger (a hold charge: the shot is what was built while it was down). */
    default boolean fireOnRelease(ItemStack gun) {
        return false;
    }

    /** Whether the reload key works the gun (a pump charge) instead of reloading it. */
    default boolean reloadKeyWorks(ItemStack gun) {
        return false;
    }

    /** Whether the reload key would do anything now on a gun that {@link #reloadKeyWorks} (the client plays its stroke only then). */
    default boolean reloadKeyReady(ItemStack gun) {
        return true;
    }

    /** The reload key pressed on a gun that {@link #reloadKeyWorks}. */
    default void reloadKey(ServerLevel level, Player player, ItemStack gun) {
    }

    /** Whether a pump key press would work the gun's pump charge now (the client plays the stroke only then). */
    default boolean canPump(ItemStack gun) {
        return false;
    }

    /** The pump key pressed with the gun in hand (a pump charge's stroke). */
    default void pumpKey(ServerLevel level, Player player, ItemStack gun) {
    }

    /** The sight went up or came down on the gun (a scope zoom charge builds while it is up). */
    default void aimChanged(ItemStack gun, boolean aiming, long gameTime) {
    }
}
