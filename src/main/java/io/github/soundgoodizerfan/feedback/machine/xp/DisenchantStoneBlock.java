// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.modular.Attunements;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The grindstone's cousin for attunements: held to it, a part loses one attunement against the stone and gives back a
 * share of what the session cost ({@link FTuning#UNATTUNE_REFUND}) as XP, into an XP run beside the stone if it has room
 * and as orbs if not. The part keeps its flaws. Not a free lunch: the refund is half the price, and the stone takes the
 * lowest-named attunement first, so choosing which one goes is not offered.
 */
public class DisenchantStoneBlock extends Block {

    public DisenchantStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        String key = made == null ? null : made.measures().attuned().keySet().stream()
                .min(Comparator.naturalOrder()).orElse(null);
        if (key == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        Map<String, Float> kept = new HashMap<>(made.measures().attuned());
        kept.remove(key);
        ItemStack one = stack.copyWithCount(1);
        one.set(FDataComponents.MADE_OF.get(), new MadeOf(made.material(), made.stock(), made.cells(), made.variant(), made.flaw(), made.measures().withAttuned(kept)));
        stack.shrink(1);
        if (stack.isEmpty())
            player.setItemInHand(hand, one);
        else if (!player.getInventory().add(one))
            player.drop(one, false);
        int refund = Math.max(1, Math.round(ModularTuning.ENCHANT_XP_COST.value() * FTuning.UNATTUNE_REFUND));
        int left = refund;
        for (Direction d : Direction.values())
            if (left > 0 && level.getBlockEntity(pos.relative(d)) instanceof XpNode node && node.network() != null)
                left -= node.network().inject(left);
        if (left > 0 && level instanceof ServerLevel server)
            ExperienceOrb.award(server, Vec3.atCenterOf(pos).add(0, 0.6, 0), left);
        player.displayClientMessage(Component.translatable("message.feedback.disenchant_stone"), true);
        return ItemInteractionResult.CONSUME;
    }
}
