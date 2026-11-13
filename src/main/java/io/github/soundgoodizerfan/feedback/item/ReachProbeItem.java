// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.Reachable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A probe that links to a machine: right-click one that states its reach ({@link Reachable}) and the probe keeps its name and
 * figures. While carried, the recipe browser sets that machine's reach beside what a recipe asks for (philosophy 6). The
 * figures are an equipment spec, printed exactly; reading a requirement still goes through the best instrument carried.
 * Sneak-use in the air forgets the link. It reads nothing and controls nothing.
 */
public class ReachProbeItem extends Item {

    public ReachProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof Reachable machine))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            var reach = machine.reach();
            stack.set(FDataComponents.REACH.get(), new Link(level.getBlockState(context.getClickedPos()).getBlock().getDescriptionId(),
                    reach.tempTu(), reach.pressurePu(), reach.rpm()));
            player.displayClientMessage(Component.translatable("feedback.reach_probe.linked",
                    Component.translatable(level.getBlockState(context.getClickedPos()).getBlock().getDescriptionId())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.has(FDataComponents.REACH.get()))
            return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.remove(FDataComponents.REACH.get());
            player.displayClientMessage(Component.translatable("feedback.reach_probe.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var link = stack.get(FDataComponents.REACH.get());
        if (link == null) {
            tooltip.add(Component.translatable("tooltip.feedback.reach_probe.empty").withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.feedback.reach_probe.linked", Component.translatable(link.nameKey())).withStyle(net.minecraft.ChatFormatting.AQUA));
        if (link.tempTu() < Reachable.Reach.UNSTATED)
            tooltip.add(Component.translatable("feedback.jei.reach.tu", io.github.soundgoodizerfan.feedback.instrument.Instruments.sig(link.tempTu(), 6)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (link.pressurePu() < Reachable.Reach.UNSTATED)
            tooltip.add(Component.translatable("feedback.jei.reach.pu", io.github.soundgoodizerfan.feedback.instrument.Instruments.sig(link.pressurePu(), 6)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (link.rpm() < Reachable.Reach.UNSTATED)
            tooltip.add(Component.translatable("feedback.jei.reach.rpm", io.github.soundgoodizerfan.feedback.instrument.Instruments.sig(link.rpm(), 6)).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** What the probe keeps of a machine: its block's name key and the reach it stated. Stored on the stack, so no client class is needed to read it. */
    public record Link(String nameKey, float tempTu, float pressurePu, float rpm) {

        public static final com.mojang.serialization.Codec<Link> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.STRING.fieldOf("name").forGetter(Link::nameKey),
                com.mojang.serialization.Codec.FLOAT.fieldOf("temp").forGetter(Link::tempTu),
                com.mojang.serialization.Codec.FLOAT.fieldOf("pressure").forGetter(Link::pressurePu),
                com.mojang.serialization.Codec.FLOAT.fieldOf("rpm").forGetter(Link::rpm)).apply(i, Link::new));
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Link> STREAM = net.minecraft.network.codec.StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, Link::nameKey,
                net.minecraft.network.codec.ByteBufCodecs.FLOAT, Link::tempTu,
                net.minecraft.network.codec.ByteBufCodecs.FLOAT, Link::pressurePu,
                net.minecraft.network.codec.ByteBufCodecs.FLOAT, Link::rpm, Link::new);
    }
}
