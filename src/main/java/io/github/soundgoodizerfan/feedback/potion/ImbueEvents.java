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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.process.Dip;
import io.github.soundgoodizerfan.feedback.process.DipTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Imbue (potions spec 12.5). Sneak-using a tool that a {@code dip} imbue entry accepts, with a brewed potion in the other hand, soaks the
 * tool: it takes the potion's rider as an {@link Imbue} for the entry's hits. Only an effect node flagged {@code imbue} qualifies. Each
 * landed melee blow lays the rider on the body struck and spends a hit; at none the soak is gone. The rider is a fraction of the potion's
 * own level and length, so it is well under an enchant.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ImbueEvents {

    private ImbueEvents() {}

    @SubscribeEvent
    public static void onSoak(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getLevel().isClientSide || !player.isShiftKeyDown())
            return;
        InteractionHand other = event.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack tool = event.getItemStack(), bottle = player.getItemInHand(other);
        PotionState state = bottle.get(FDataComponents.POTION_STATE.get());
        if (state == null || state.dud())
            return;
        Dip soak = DipTable.get().findImbue(tool).orElse(null);
        PotionFeature node = soak == null ? null : PotionMap.get().byEffect(state.effect().get());
        if (node == null || !node.effect().get().imbue())
            return;
        int amplifier = state.amplifierNow(event.getLevel());
        if (amplifier < 0)
            return;
        float potency = state.potencyNow(event.getLevel());
        tool.set(FDataComponents.IMBUE.get(), new Imbue(state.effect().get(), amplifier / PotionTuning.IMBUE_LEVEL_DIVISOR,
                Math.min(PotionTuning.IMBUE_MAX_TICKS, Math.round(state.duration() * PotionTuning.IMBUE_DURATION_FRACTION)),
                Math.max(1, Math.round(soak.imbue().get().hits() * node.effect().get().imbueHits() * potency))));
        if (!player.getAbilities().instabuild)
            player.setItemInHand(other, new ItemStack(Items.GLASS_BOTTLE));
        event.getLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.6f, 1.2f);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onStrike(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide
                || event.getSource().getDirectEntity() != attacker)
            return;
        ItemStack weapon = StrikeSource.weapon(attacker, event.getSource());
        Imbue imbue = weapon.get(FDataComponents.IMBUE.get());
        if (imbue == null)
            return;
        PotionEffects.apply(imbue.effect(), event.getEntity(), imbue.amplifier(), imbue.duration());
        if (imbue.hits() <= 1)
            weapon.remove(FDataComponents.IMBUE.get());
        else
            weapon.set(FDataComponents.IMBUE.get(), new Imbue(imbue.effect(), imbue.amplifier(), imbue.duration(), imbue.hits() - 1));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Imbue imbue = event.getItemStack().get(FDataComponents.IMBUE.get());
        if (imbue != null)
            event.getToolTip().add(Component.translatable("tooltip.feedback.imbue", PotionEffects.name(imbue.effect()), imbue.hits())
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
