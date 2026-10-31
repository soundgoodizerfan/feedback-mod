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
        return Instruments.signed(self, Component.translatable("feedback.xp.store",
                Instruments.format(level, self.resolution(Quantity.XP_LEVEL)),
                Instruments.format(points, self.resolution(Quantity.EXPERIENCE)),
                Instruments.format(ceiling, self.resolution(Quantity.EXPERIENCE))));
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
            Component line;
            if (node.xpStore() != null) {
                XpStore s = node.xpStore();
                line = store(self, s.level(), s.points(), s.maxPoints());
            } else if (net != null) {
                line = Instruments.signed(self, Component.translatable("feedback.xp.run",
                        Instruments.format(net.level(), self.resolution(Quantity.XP_LEVEL)),
                        Instruments.format(net.points(), self.resolution(Quantity.EXPERIENCE)),
                        net.flow(), net.lastLeak()));
            } else
                line = Component.translatable("feedback.xp.none");
            player.displayClientMessage(line, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            XpStore held = XpItems.store(player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
            player.displayClientMessage(held != null ? store(self, held.level(), held.points(), held.maxPoints())
                    : store(self, player.experienceLevel + player.experienceProgress, player.totalExperience, player.totalExperience), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide)
            return InteractionResult.SUCCESS;
        Instrument self = Instruments.fitted(stack, this);
        if (target instanceof Villager villager) {
            player.displayClientMessage(Instruments.signed(self, Component.translatable("feedback.xp.villager",
                    villager.getVillagerData().getLevel(), villager.getVillagerXp())), true);
            return InteractionResult.CONSUME;
        }
        if (target instanceof Player other) {
            player.displayClientMessage(store(self, other.experienceLevel + other.experienceProgress, other.totalExperience,
                    other.totalExperience), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
