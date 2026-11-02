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
package io.github.soundgoodizerfan.feedback.machine.skillet;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.Handled;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularItem;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;

import net.minecraft.resources.ResourceLocation;
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

import org.jetbrains.annotations.Nullable;

/**
 * The pan tool. A tap (released within {@link FTuning#SKILLET_TAP_TICKS}) flips the piece the cursor is over; a hold
 * lifts the pan off the fire while it is kept on the pan and within reach. The server counts the ticks, so a client
 * can only press and release. A hot pan heats the hand through the handle unless its material insulates
 * ({@link Handled}): the grip is the handle's. Held in use it wears a point every {@link FTuning#SPATULA_WEAR_TICKS} ticks.
 */
public class SpatulaItem extends ModularItem implements Handled {

    private static final int FOREVER = 72000;

    public SpatulaItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return FOREVER;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModularStacks.broken(stack))
            return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static SkilletBlockEntity pan(Player player, Level level, BlockHitResult[] hitOut) {
        HitResult hit = player.pick(FTuning.SKILLET_LIFT_REACH, 0f, false);
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(block.getBlockPos()) instanceof SkilletBlockEntity pan) {
            hitOut[0] = block;
            return pan;
        }
        return null;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof Player player))
            return;
        int held = getUseDuration(stack, user) - remaining;
        if (held <= FTuning.SKILLET_TAP_TICKS)
            return;
        SkilletBlockEntity pan = pan(player, level, new BlockHitResult[1]);
        if (pan == null)
            return;
        pan.lift();
        if (held % FTuning.SPATULA_WEAR_TICKS == 0)
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        if (held % 20 == 0 && pan.panTu() > holdsUpToTu(stack, level))
            HotContact.burn(player, new Tu(pan.panTu()));
        if (ModularStacks.broken(stack))
            player.stopUsingItem();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof Player player))
            return;
        if (getUseDuration(stack, user) - timeLeft > FTuning.SKILLET_TAP_TICKS)
            return;
        BlockHitResult[] hit = new BlockHitResult[1];
        SkilletBlockEntity pan = pan(player, level, hit);
        if (pan != null)
            pan.flip(SkilletBlock.slot(hit[0].getBlockPos(), hit[0]));
    }

    /** The hottest pan the hand can hold through this handle: its material's, at the spatula's exposure. */
    @Override
    public float holdsUpToTu(ItemStack stack, Level level) {
        ModularDesign design = ModularStacks.design(stack);
        ModularDesign.Part handle = design == null ? null : design.part("S");
        ResourceLocation id = handle == null ? null : handle.material();
        return id == null ? FTuning.CONTACT_PAIN_TU.value() : Grip.safeUpTo(level, id, FTuning.SPATULA_EXPOSURE);
    }
}
