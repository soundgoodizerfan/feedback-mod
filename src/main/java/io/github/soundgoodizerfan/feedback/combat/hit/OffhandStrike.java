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
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.modular.trait.WielderState;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The off-hand swing (plan 0, dual wield). Vanilla's attack is main-hand: its cooldown, damage and
 * reach are the player's attributes, which carry only the main-hand weapon's modifiers. So the
 * off hand strikes here by its own figures: damage, attack speed and reach read off the off-hand
 * stack's attribute modifiers, a cooldown of its own (game time of the last off-hand swing), a
 * single target picked along the look (no sweep; a trait such as Overhead brings its own), and the
 * blow dealt through a {@link StrikeSource} so traits, perks, drops and wear resolve against the
 * off-hand stack. Server-side; the client key only asks.
 */
public final class OffhandStrike {

    private static final WielderState<Long> LAST = new WielderState<>();

    private OffhandStrike() {
    }

    /** Whether {@code stack} is an unbroken modular blade, mace or spear: what a hand may strike with here. */
    public static boolean strikes(ItemStack stack) {
        var design = ModularStacks.design(stack);
        if (design == null || ModularStacks.broken(stack))
            return false;
        ModularType type = ModularTypes.known().get(design.type());
        return type != null && (type.family() == ModularType.Family.BLADE || type.family() == ModularType.Family.MACE
                || type.family() == ModularType.Family.SPEAR);
    }

    public static void strike(ServerPlayer player) {
        ItemStack weapon = player.getOffhandItem();
        if (!strikes(weapon) || player.isSpectator() || player.isUsingItem())
            return;
        long now = player.level().getGameTime();
        double cooldown = 20 / Math.max(0.1, stat(weapon, Attributes.ATTACK_SPEED, 4));
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < cooldown * 0.5)
            return;
        float charge = last == null ? 1f : Mth.clamp((float) ((now - last) / cooldown), 0f, 1f);
        LAST.put(player.getUUID(), now);
        player.swing(InteractionHand.OFF_HAND, true);

        double reach = stat(weapon, Attributes.ENTITY_INTERACTION_RANGE, 3);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1f).scale(reach));
        var block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS)
            end = block.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, new AABB(eye, end).inflate(1),
                e -> e != player.getVehicle() && !e.isSpectator() && e.isPickable() && e.isAlive() && !player.isAlliedTo(e));
        if (hit == null)
            return;
        Entity target = hit.getEntity();
        if (!target.isAttackable() || target.skipAttackInteraction(player))
            return;

        ServerLevel level = player.serverLevel();
        DamageSource source = StrikeSource.of(player.damageSources().playerAttack(player), InteractionHand.OFF_HAND);
        float base = (float) stat(weapon, Attributes.ATTACK_DAMAGE, 1);
        float bonus = EnchantmentHelper.modifyDamage(level, weapon, target, source, base) - base;
        if (target.hurt(source, base * (0.2f + charge * charge * 0.8f) + bonus * charge)) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            if (target instanceof LivingEntity living)
                living.knockback(ModularTuning.SWEEP_KNOCKBACK * charge, Mth.sin(yaw), -Mth.cos(yaw));
            weapon.hurtAndBreak(ModularStacks.cap(weapon, 1), player, EquipmentSlot.OFFHAND);
            SoundEvent sound = charge > 0.9f ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1f, 1f);
        }
    }

    /** {@code base} plus the added values of {@code attribute} in {@code stack}'s own modifiers (the player's attributes hold only the main hand's). */
    private static double stat(ItemStack stack, Holder<Attribute> attribute, double base) {
        double v = base;
        for (ItemAttributeModifiers.Entry e : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers())
            if (e.attribute().equals(attribute))
                v += e.modifier().amount();
        return v;
    }
}
