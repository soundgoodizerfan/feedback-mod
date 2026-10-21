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
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;

/** A crossbow whose bolt speed is its prod and string. Its charge time is still vanilla (spec 11). */
public class ModularCrossbow extends net.minecraft.world.item.CrossbowItem implements Modular {

    public ModularCrossbow(Item.Properties properties) {
        super(properties);
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
    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> ammo,
                         float velocity, float inaccuracy, boolean crit, @Nullable LivingEntity target) {
        super.shoot(level, shooter, hand, weapon, ammo, RangedDispatch.launch(weapon, ModularStacks.launch(weapon, velocity)),
                RangedDispatch.spread(weapon, inaccuracy), crit, target);
        ModularStacks.strain(weapon, shooter, hand);
    }

    /** The charge is the player's time held, divided by the archetype's load time: a slow crossbow needs longer held, a fast one less. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        int used = getUseDuration(stack, shooter) - timeLeft;
        boolean was = isCharged(stack);
        super.releaseUsing(stack, level, shooter, getUseDuration(stack, shooter) - Math.round(used / RangedDispatch.drawScale(stack)));
        if (!was && isCharged(stack) && shooter instanceof ServerPlayer player)
            RangedDispatch.loaded(player, stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        super.onUseTick(level, user, stack, remaining);
        ModularTraits.onUseTick(level, user, stack, remaining);
    }

    @Override
    public net.minecraft.world.entity.projectile.AbstractArrow customArrow(net.minecraft.world.entity.projectile.AbstractArrow arrow,
                                                                           ItemStack ammo, ItemStack weapon) {
        return RangedDispatch.arrow(super.customArrow(arrow, ammo, weapon), weapon);
    }

    /** Draws {@code extra} more rounds out of the shooter's quiver into the loaded crossbow (a repeater's magazine). */
    public static void chamber(ItemStack weapon, ServerPlayer shooter, int extra) {
        List<ItemStack> loaded = new java.util.ArrayList<>(weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).getItems());
        for (int i = 0; i < extra; i++)
            loaded.addAll(draw(weapon, shooter.getProjectile(weapon), shooter));
        weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(loaded));
    }
}
