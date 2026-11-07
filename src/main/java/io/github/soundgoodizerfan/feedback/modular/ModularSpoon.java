// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlockEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The pot tool. Held use aimed at a stockpot is stirring (every tick, a switch: full pace or none); a use released inside
 * {@link FTuning#SPOON_TAP_TICKS} is a skim. The server times everything; no payloads. The bowl's size scales stir and skim;
 * the handle's material decides how hot a pot it can be held over before the hand burns. A broken spoon does nothing.
 */
public class ModularSpoon extends ModularItem {

    public ModularSpoon(Properties properties) {
        super(properties);
    }

    private static StockpotBlockEntity pot(Player player, Level level) {
        HitResult hit = player.pick(FTuning.SKILLET_LIFT_REACH, 0f, false);
        return hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(block.getBlockPos()) instanceof StockpotBlockEntity pot ? pot : null;
    }

    /** The bowl against the reference spoon: 1 is the reference; a bigger bowl does proportionally more. */
    public static float bowl(ItemStack stack) {
        float mb = ModularStacks.volumeMb(stack);
        return mb <= 0f ? 1f : mb / FTuning.SPOON_REF_BOWL_MB;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModularStacks.broken(stack) || pot(player, level) == null)
            return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof Player player) || ModularStacks.broken(stack))
            return;
        int held = getUseDuration(stack, user) - remaining;
        if (held <= FTuning.SPOON_TAP_TICKS)
            return;
        StockpotBlockEntity pot = pot(player, level);
        if (pot == null)
            return;
        pot.stir(bowl(stack));
        if (held % 20 == 0) {
            ModularDesign design = ModularStacks.design(stack);
            var handle = design == null ? null : design.part("S");
            float stands = handle == null ? FTuning.CONTACT_PAIN_TU.value() : Grip.safeUpTo(level, handle.material(), FTuning.SPOON_EXPOSURE);
            if (pot.temperature() > stands)
                HotContact.burn(player, new Tu(pot.temperature()));
        }
        if (held % FTuning.POT_STIR_WEAR_TICKS == 0)
            stack.hurtAndBreak(1, player, user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (level.isClientSide || !(user instanceof Player player) || ModularStacks.broken(stack))
            return;
        if (getUseDuration(stack, user) - remaining > FTuning.SPOON_TAP_TICKS)
            return;
        StockpotBlockEntity pot = pot(player, level);
        if (pot != null && pot.skim(bowl(stack)))
            stack.hurtAndBreak(1, player, user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }
}
