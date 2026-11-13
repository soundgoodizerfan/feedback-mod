// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.net.SoundingPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The prospector's pick (spec 5.4): right click sends a pulse that outlines every ore within
 * {@code range} blocks (default 12) through the stone, for {@code seconds} (default 6), to the
 * wielder alone. It is an instrument: it reads and never mines faster. Cost: {@code cooldown_ticks}
 * (default 200) between pulses and {@code wear} (default 4) per pulse. Ore is a block tag, never
 * an id, so modded ore answers too; {@code max_found} (default 64) nearest are sent.
 */
public final class Sounding implements ModularTrait {

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack weapon, Params params) {
        if (!(player instanceof ServerPlayer sp) || player.getCooldowns().isOnCooldown(weapon.getItem()))
            return InteractionResultHolder.pass(weapon);
        int range = (int) params.get("range", 12);
        BlockPos centre = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-range, -range, -range), centre.offset(range, range, range)))
            if (pos.distSqr(centre) <= (double) range * range && level.getBlockState(pos).is(Tags.Blocks.ORES))
                found.add(pos.immutable());
        found.sort(Comparator.comparingDouble(p -> p.distSqr(centre)));
        if (found.size() > (int) params.get("max_found", 64))
            found = found.subList(0, (int) params.get("max_found", 64));

        PacketDistributor.sendToPlayer(sp, new SoundingPayload(found, (int) (params.get("seconds", 6) * 20)));
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 0.6f);
        player.getCooldowns().addCooldown(weapon.getItem(), (int) params.get("cooldown_ticks", 200));
        weapon.hurtAndBreak(ModularStacks.cap(weapon, (int) params.get("wear", 4)), sp, EquipmentSlot.MAINHAND);
        return InteractionResultHolder.sidedSuccess(weapon, level.isClientSide);
    }
}
