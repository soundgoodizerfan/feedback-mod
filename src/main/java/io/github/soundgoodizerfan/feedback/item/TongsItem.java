// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.Insulator;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import java.util.List;

/**
 * A pair of tongs: hold one in a hand and the other hand can carry a hot piece, and the hand
 * with the tongs can reach into hot equipment.
 *
 * <h2>Two materials, two limits, and a longer reach than a glove</h2>
 * A glove sits on the palm; tongs put a forearm of arm and a grip between the jaws and the hand,
 * so only a tenth of a piece's excess reaches the grip ({@link FTuning#TONGS_EXPOSURE}). The
 * limit is therefore the lower of what the grip stands at that exposure and the temperature the
 * arms melt at -- a wooden grip and iron arms hold a yellow-hot piece, a bone grip holds whatever
 * the iron will, and copper arms give out before steel's window opens. Nothing is stamped on the
 * item: pair a better grip or a better metal and the limit moves.
 *
 * <p>They do nothing against a bare wire. Holding a piece away from the hand is not insulating
 * the hand from a conductor, and {@link Insulator#shockProofBand} stays at its default null.
 *
 * <p>They never wear: the three accepted exceptions are not a list that grows.
 */
public class TongsItem extends Item implements Insulator {

    public TongsItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The skillet the player is looking at within reach, or null. */
    private static SkilletBlockEntity pan(Player player, Level level) {
        HitResult hit = player.pick(FTuning.SKILLET_LIFT_REACH, 0f, false);
        return hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(block.getBlockPos()) instanceof SkilletBlockEntity pan ? pan : null;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Held on a skillet the tongs take the pan by its handle and lift it off the fire; release sets it back. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (pan(player, level) == null)
            return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof Player player))
            return;
        SkilletBlockEntity pan = pan(player, level);
        if (pan == null)
            return;
        pan.lift();
        if (pan.panTu() > ThermalTuning.CONTACT_PAIN_TU.value() && pan.panTu() <= handlesUpToTu(stack, level))
            io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(player, "progress/heat/tongs");
        if ((getUseDuration(stack, user) - remaining) % 20 == 0 && pan.panTu() > handlesUpToTu(stack, level))
            HotContact.burn(player, new Tu(pan.panTu()));
    }

    @Override
    public float handlesUpToTu(ItemStack stack, Level level) {
        Tongs tongs = stack.get(FDataComponents.TONGS.get());
        if (tongs == null)
            return ThermalTuning.CONTACT_PAIN_TU.value();
        float grip = Grip.safeUpTo(level, tongs.grip(), FTuning.TONGS_EXPOSURE);
        float arms = Grip.material(level, tongs.arms())
                .flatMap(m -> m.thermal().meltingPoint())
                .map(tu -> tu.value())
                .orElse(Float.MAX_VALUE);
        return Math.min(grip, arms);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tongs tongs = stack.get(FDataComponents.TONGS.get());
        if (tongs == null)
            return;
        tooltip.add(Component.translatable("item.feedback.tongs.arms", name(tongs.arms())));
        tooltip.add(Component.translatable("item.feedback.tongs.grip", name(tongs.grip())));
    }

    private static Component name(ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }
}
