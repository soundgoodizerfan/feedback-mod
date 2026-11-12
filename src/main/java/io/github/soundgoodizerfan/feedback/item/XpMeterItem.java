// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.xp.XpItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Reads experience, both of its numbers: the level a store stands at (the potential) and the points it holds (the
 * amount). Use it on a holder, wire or creative source for the container or the whole run; in the air for the container
 * in the other hand, else your own; on a player or villager for theirs. Read-only, like every instrument. A level is
 * read to whole levels and points to fives until a circuit narrows the step.
 */
public class XpMeterItem extends Item implements Instrument {

    public XpMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.XP_LEVEL || quantity == Quantity.EXPERIENCE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return quantity == Quantity.XP_LEVEL ? FTuning.XP_METER_RESOLUTION_LEVEL : FTuning.XP_METER_RESOLUTION_POINTS;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.xp_meter");
    }

    private Component store(Instrument self, float level, float points, float ceiling) {
        return Component.translatable("feedback.xp.store",
                Instruments.format(level, self.resolution(Quantity.XP_LEVEL)),
                Instruments.format(points, self.resolution(Quantity.EXPERIENCE)),
                Instruments.format(ceiling, self.resolution(Quantity.EXPERIENCE)));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof XpNode node))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            XpNetwork net = node.network();
            if (node.xpStore() != null) {
                XpStore st = node.xpStore();
                io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, store(self, st.level(), st.points(), st.maxPoints()), Quantity.XP_LEVEL,
                        Instruments.quantise(st.level(), self.resolution(Quantity.XP_LEVEL)));
            } else if (net != null) {
                io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, Component.translatable("feedback.xp.run",
                        Instruments.format(net.level(), self.resolution(Quantity.XP_LEVEL)),
                        Instruments.format(net.points(), self.resolution(Quantity.EXPERIENCE)),
                        net.flow(), net.lastLeak()), Quantity.XP_LEVEL, Instruments.quantise(net.level(), self.resolution(Quantity.XP_LEVEL)));
            } else
                io.github.soundgoodizerfan.feedback.net.AimPayloads.read(player, context.getClickedPos(), self, Component.translatable("feedback.xp.none"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            XpStore held = XpItems.store(player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
            float lvl = held != null ? held.level() : player.experienceLevel + player.experienceProgress;
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, self, held != null ? store(self, held.level(), held.points(), held.maxPoints())
                    : store(self, lvl, player.totalExperience, player.totalExperience), Quantity.XP_LEVEL,
                    Instruments.quantise(lvl, self.resolution(Quantity.XP_LEVEL)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide)
            return InteractionResult.SUCCESS;
        Instrument self = Instruments.fitted(stack, this);
        if (target instanceof Villager villager) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.read(player, villager, self, Component.translatable("feedback.xp.villager",
                    villager.getVillagerData().getLevel(), villager.getVillagerXp()));
            return InteractionResult.CONSUME;
        }
        if (target instanceof Player other) {
            float lvl = other.experienceLevel + other.experienceProgress;
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, other, self, store(self, lvl, other.totalExperience, other.totalExperience), Quantity.XP_LEVEL,
                    Instruments.quantise(lvl, self.resolution(Quantity.XP_LEVEL)));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
