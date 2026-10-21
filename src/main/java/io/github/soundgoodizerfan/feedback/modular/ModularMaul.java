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

import io.github.soundgoodizerfan.feedback.modular.trait.SmashAttack;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A maul: a heavy head on a handle that splits wood and staggers what it hits. The stagger is the
 * blow itself -- a slow for as long as the impact is worth, so a heavier head holds its target
 * longer -- rather than a flat effect (spec 5).
 *
 * <p>It is vanilla's {@link MaceItem} so a head shaped as a mace ({@link SmashAttack}) lands the
 * mace's smash on a fall, its damage scaled by the head's mass, and takes the mace's enchantments
 * (item-level for now). A maul head of any other shape swings as a maul: no smash, and the stagger.
 */
public class ModularMaul extends MaceItem implements Modular {

    /** Ticks of slow per joule of impact. */
    private static final float STAGGER_TICKS_PER_JOULE = 2f;

    public ModularMaul(Properties properties) {
        super(properties);
    }

    private static boolean smashes(ItemStack stack) {
        return ModularTraits.of(stack).filter(a -> a.trait() instanceof SmashAttack).isPresent();
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        ItemStack weapon = source.getWeaponItem() != null ? source.getWeaponItem() : ItemStack.EMPTY;
        if (!smashes(weapon))
            return 0f;
        ModularFigures f = ModularStacks.figures(weapon);
        float scale = f == null ? 1f : Math.min(1.5f, Math.max(0.15f, f.mass().value() / ModularTuning.MACE_REFERENCE_MASS));
        return super.getAttackDamageBonus(target, damage, source) * scale;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return !smashes(stack) || super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (smashes(stack)) {
            super.postHurtEnemy(stack, target, attacker);
            return;
        }
        ModularFigures f = ModularStacks.figures(stack);
        if (f != null && !ModularStacks.broken(stack))
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    Math.round(f.get(ModularFigures.Fig.IMPACT) * STAGGER_TICKS_PER_JOULE), 1), attacker);
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> traited = ModularTraits.use(level, player, hand, player.getItemInHand(hand));
        if (traited.getResult() != InteractionResult.PASS) {
            if (ModularTraits.useDuration(player.getItemInHand(hand)) > 0)
                player.startUsingItem(hand);
            return traited;
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        int duration = ModularTraits.useDuration(stack);
        return duration > 0 ? duration : super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        UseAnim anim = ModularTraits.useAnimation(stack);
        return anim != UseAnim.NONE ? anim : super.getUseAnimation(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        ModularTraits.onUseTick(level, user, stack, remaining);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        ModularTraits.releaseUsing(stack, level, user, remaining);
    }
}
