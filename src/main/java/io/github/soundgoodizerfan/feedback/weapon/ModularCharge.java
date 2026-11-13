// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.item.ModularItem;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A charge made at the Tool Table from a casing and its filler, or designed first at the Blueprint
 * Table: a modular item whose parts are what is packed ({@code modular_type/charge.json}). Placed, it
 * becomes the demolition charge block carrying exactly that filler and casing, unlit.
 */
public class ModularCharge extends ModularItem {

    public ModularCharge(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos at = place.getClickedPos();
        BlockState state = FWeapons.DEMOLITION_CHARGE.get().defaultBlockState();
        if (!place.canPlace() || !state.canSurvive(level, at))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(at, state, 3);
            if (level.getBlockEntity(at) instanceof DemolitionChargeBlockEntity charge)
                charge.pack(context.getItemInHand());
            level.playSound(null, at, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Thrown from the air. Its fuse part decides what happens: an impact fuse goes off on the first thing it hits, a
     * delay fuse bounces and goes off when its burn time has run.
     */
    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var packed = Blast.packed(held);
            GrenadeEntity grenade = new GrenadeEntity(level, player, packed.map(Blast.Packed::impact).orElse(false),
                    packed.map(Blast.Packed::fuseTicks).orElse(io.github.soundgoodizerfan.feedback.weapon.GunTuning.DEMO_FUSE_TICKS));
            grenade.setItem(held.copyWithCount(1));
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, io.github.soundgoodizerfan.feedback.weapon.GunTuning.GRENADE_THROW_SPEED, 1f);
            level.addFreshEntity(grenade);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.PLAYERS, 1f, 1f);
            if (!player.getAbilities().instabuild)
                held.shrink(1);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
