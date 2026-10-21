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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import io.github.soundgoodizerfan.feedback.modular.trait.SpearCharge;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The spear, and every variant of its head. The standard head charges ({@link SpearCharge}: the use
 * key is a held key, not item use, so the item has no use of its own); a trident, javelin, harpoon or
 * pitchfork head is thrown as a {@link ThrownWeapon} rather than vanilla's trident, so its thrown
 * blow is its baked damage and its variant's params (a harpoon's rope) apply; Riptide still spins
 * the thrower as in vanilla. It is a {@link net.minecraft.world.item.TridentItem} so a thrown head
 * takes the trident's enchantments; item-level for now (enchanting a part is later).
 */
public class ModularSpear extends net.minecraft.world.item.TridentItem implements Modular {

    public ModularSpear(Item.Properties properties) {
        super(properties);
    }

    /** Whether the head on this spear is thrown rather than charged: any variant but the standard's. */
    private static boolean thrown(ItemStack stack) {
        return ModularTraits.of(stack).map(a -> !(a.trait() instanceof SpearCharge)).orElse(true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!thrown(stack))
            return InteractionResultHolder.pass(stack);
        InteractionResultHolder<ItemStack> traited = ModularTraits.use(level, player, hand, stack);
        if (traited.getResult() != InteractionResult.PASS) {
            if (ModularTraits.useDuration(stack) > 0)
                player.startUsingItem(hand);
            return traited;
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (!thrown(stack))
            return 0;
        int duration = ModularTraits.useDuration(stack);
        return duration > 0 ? duration : super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (!thrown(stack))
            return UseAnim.NONE;
        UseAnim anim = ModularTraits.useAnimation(stack);
        return anim != UseAnim.NONE ? anim : super.getUseAnimation(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        ModularTraits.onUseTick(level, user, stack, remaining);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || !thrown(stack))
            return;
        if (ModularTraits.useDuration(stack) > 0) {
            ModularTraits.releaseUsing(stack, level, user, timeLeft);
            return;
        }
        if (EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0f) {
            super.releaseUsing(stack, level, user, timeLeft);
            return;
        }
        Throwing.release(stack, level, player, getUseDuration(stack, user) - timeLeft,
                ModularTraits.of(stack).map(ModularTraits.Active::params).orElse(new ModularTrait.Params(java.util.Map.of())), 0f, 1f, 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ModularStacks.enchantability(stack, 1);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ModularStacks.cap(stack, amount);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        ModularStacks.mined(stack, level, state, pos, miner);
        return result;
    }
}
