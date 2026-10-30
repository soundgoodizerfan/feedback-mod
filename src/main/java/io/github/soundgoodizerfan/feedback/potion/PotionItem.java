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
package io.github.soundgoodizerfan.feedback.potion;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A bottle of potion or a dud draught (potions spec 4), which replace vanilla's potion items. Drinking applies the effect the brew
 * landed on at the level and length baked in, the level scaled by what potency is left; a dud gives a short nausea. Everything it needs
 * is in {@link PotionState}.
 */
public class PotionItem extends Item {

    public PotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown())
            return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand);
        // Sneaking throws (spec 5); the node's throw flag is checked on the server, which owns the map.
        if (level.isClientSide)
            return InteractionResultHolder.success(stack);
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        PotionFeature node = state == null || state.dud() ? null : PotionMap.get().byEffect(state.effect().get());
        if (node == null || !node.effect().get().throwable())
            return InteractionResultHolder.pass(stack);
        ThrownPotionBottle bottle = new ThrownPotionBottle(level, player);
        bottle.setItem(stack.copyWithCount(1));
        bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20f, 0.5f, 1f);
        level.addFreshEntity(bottle);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.4f);
        if (!player.getAbilities().instabuild)
            stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        if (!level.isClientSide) {
            drink(state, level, entity);
        }
        Player player = entity instanceof Player p ? p : null;
        if (player != null && player.getAbilities().instabuild)
            return stack;
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty())
            return bottle;
        if (player != null && !player.getInventory().add(bottle))
            player.drop(bottle, false);
        return stack;
    }

    private static void drink(PotionState state, Level level, LivingEntity entity) {
        if (state == null || state.dud()) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, PotionTuning.DUD_NAUSEA_TICKS));
            return;
        }
        int amplifier = state.amplifierNow(level);
        if (amplifier < 0)
            return;
        PotionEffects.consume(state.effect().get(), entity, amplifier, state.duration());
    }

    @Override
    public Component getName(ItemStack stack) {
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        if (state == null || state.dud())
            return Component.translatable("item.feedback.dud_draught");
        return Component.translatable("item.feedback.potion.of", PotionEffects.name(state.effect().get()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        if (state == null || state.dud() || context.level() == null)
            return;
        float p = state.potencyNow(context.level());
        lines.add(Component.translatable(p >= 0.75f ? "tooltip.feedback.potion.fresh" : p >= 0.4f ? "tooltip.feedback.potion.fading"
                : p >= PotionTuning.MIN_POTENCY ? "tooltip.feedback.potion.stale" : "tooltip.feedback.potion.flat")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        var id = state.effect().get();
        if (PotionEffects.IGNITE.equals(id) || PotionEffects.EXPLOSION.equals(id))
            lines.add(Component.translatable(PotionEffects.EXPLOSION.equals(id) ? "tooltip.feedback.potion.drink_explosion" : "tooltip.feedback.potion.drink_ignite")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.feedback.potion.throw").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
