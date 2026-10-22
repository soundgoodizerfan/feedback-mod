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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * What the bow, crossbow and shield classes hand to a trait (spec 5.4, ranged and defensive gear),
 * and the events those traits listen to. Four numbers are read from any ranged trait's params
 * whatever its class, so a handling archetype is data: {@code draw_mult} (draw or load time),
 * {@code velocity_mult}, {@code spread_mult} (the shot's inaccuracy) and {@code quiet}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RangedDispatch {

    private RangedDispatch() {
    }

    /** True for the length of a hushed bow's release, on the server thread only. */
    private static boolean hush;

    private static float param(ItemStack weapon, String name, float fallback) {
        return ModularTraits.of(weapon).map(a -> a.params().get(name, fallback)).orElse(fallback);
    }

    /** The multiple of the baked draw (or load) time this weapon's archetype takes. */
    public static float drawScale(ItemStack weapon) {
        return param(weapon, "draw_mult", 1f);
    }

    /** {@code velocity}, as this weapon's archetype changes it. */
    public static float launch(ItemStack weapon, float velocity) {
        return velocity * param(weapon, "velocity_mult", 1f);
    }

    /** {@code inaccuracy}, as this weapon's archetype changes it. */
    public static float spread(ItemStack weapon, float inaccuracy) {
        return inaccuracy * param(weapon, "spread_mult", 1f);
    }

    /** Runs {@code release} with the arrow-shoot sound off if this weapon's archetype is quiet. */
    public static void release(ItemStack weapon, Runnable release) {
        hush = param(weapon, "quiet", 0f) > 0;
        try {
            release.run();
        } finally {
            hush = false;
        }
    }

    /**
     * Lets {@code arrow} pass through at least {@code level} mobs. {@code setPierceLevel} is private
     * and there is no access transformer here, so the arrow is written out and read back with the
     * field changed, which is what loading a saved arrow does anyway.
     */
    public static void pierce(AbstractArrow arrow, float level) {
        if (level <= arrow.getPierceLevel())
            return;
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        arrow.addAdditionalSaveData(tag);
        tag.putByte("PierceLevel", (byte) level);
        arrow.readAdditionalSaveData(tag);
    }

    /** The projectile a weapon made, with its archetype's say in it; the projectile itself. */
    public static AbstractArrow arrow(AbstractArrow arrow, ItemStack weapon) {
        ModularTraits.of(weapon).ifPresent(a -> a.trait().onArrow(arrow, weapon, a.params()));
        return arrow;
    }

    /** A crossbow was loaded: a trait may load more. */
    public static void loaded(ServerPlayer player, ItemStack weapon) {
        ModularTraits.of(weapon).ifPresent(a -> a.trait().onLoaded(player, weapon, a.params()));
    }

    /** What a shield's face can take of one blow now, before any archetype's multiple: the baked figure, softened by its heat. */
    public static float capacity(LivingEntity holder, ItemStack shield) {
        ModularFigures f = ModularStacks.figures(shield);
        return f == null ? Float.MAX_VALUE : f.get(ModularFigures.Fig.CAPACITY) * ShieldHeat.hold(holder.level(), shield);
    }

    @SubscribeEvent
    public static void onArrowHit(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && !arrow.level().isClientSide) {
            ItemStack weapon = arrow.getWeaponItem();
            if (weapon != null)
                ModularTraits.of(weapon).ifPresent(a -> a.trait().onArrowHit(event, arrow, weapon, a.params()));
        }
    }

    /** After the shield's own capacity (normal priority, {@code ModularEvents}) so an archetype's figure stands. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onShield(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        if (blocker.level().isClientSide)
            return;
        ItemStack shield = blocker.getUseItem();
        ModularTraits.of(shield).ifPresent(a -> a.trait().onShieldBlock(event, blocker, shield, a.params()));
    }

    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent event) {
        if (hush && !event.getLevel().isClientSide && event.getSound().value() == SoundEvents.ARROW_SHOOT)
            event.setCanceled(true);
    }
}
