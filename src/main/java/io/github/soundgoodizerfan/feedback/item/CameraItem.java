// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.modular.item.ModularTelescope;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A hand-held camera. Hold use to expose and let go to take the picture: the client captures what it sees, shrinks it to a small photograph, and
 * the server hands over a {@link PhotographItem}. The lens is a telescope in the other hand when there is one (its magnification crops the picture, its
 * blur, as {@code TelescopeFocus.blur} gives it, blurs it); with none the camera is a pinhole, soft and unmagnified. Exposing for under
 * {@link #MIN_EXPOSURE_TICKS} takes nothing.
 */
public class CameraItem extends Item {

    public static final int MIN_EXPOSURE_TICKS = 10;
    /** The blur of a pinhole with no lens, on the telescope's scale. */
    public static final float PINHOLE_BLUR = 0.5f;

    public CameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1200;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide && entity instanceof Player player && player.isLocalPlayer() && getUseDuration(stack, entity) - timeLeft >= MIN_EXPOSURE_TICKS)
            io.github.soundgoodizerfan.feedback.client.camera.CameraShot.take(player);
    }

    /** The lens in the player's other hand, or empty (a pinhole). */
    public static ItemStack lens(Player player, ItemStack camera) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s != camera && s.getItem() instanceof ModularTelescope)
                return s;
        }
        return ItemStack.EMPTY;
    }
}
