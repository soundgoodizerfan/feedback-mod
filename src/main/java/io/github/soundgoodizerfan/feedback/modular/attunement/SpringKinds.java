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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.ModularBow;
import io.github.soundgoodizerfan.feedback.modular.ModularCrossbow;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * The runtime kinds of the Spring effect ({@code docs/feedback_enchant_roster.md}). {@code store} (limbs, prod): the
 * same draw leaves the bow faster. {@code release} (string, mirror): the draw or load takes less time.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SpringKinds {

    /** Extra launch speed per level of store (arrow damage follows speed). Flat: the arrow's mass is not read. [OPEN] until played. */
    private static final float STORE_PER_LEVEL = 0.1f;
    /** Share of the draw time cut per level of release. [OPEN] until played. */
    private static final float RELEASE_PER_LEVEL = 0.12f;

    /** Ticks of sneak that count: under {@link #COIL_MIN} the jump is a plain jump, over {@link #COIL_MAX} no more is stored. [OPEN] until played. */
    private static final int COIL_MIN = 10;
    private static final int COIL_MAX = 40;
    /** Extra upward speed at a full hold, per level of coil, and its cap (vanilla jump is about 0.42). [OPEN] until played. */
    private static final double COIL_LIFT_PER_LEVEL = 0.6;
    private static final double COIL_LIFT_CAP = 1.4;
    /** Wear per level on each coiled part per release. [OPEN] until played. */
    private static final int COIL_WEAR_PER_LEVEL = 2;
    private static final String COIL_START = "feedback_coil_start";

    private SpringKinds() {
    }

    /** Coil levels on legs and boots only (plate and lining are the armour roles; the slot picks the part). */
    private static int coil(net.minecraft.world.entity.player.Player player, boolean wear) {
        int total = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET }) {
            ItemStack stack = player.getItemBySlot(slot);
            int level = stack.isEmpty() ? 0 : level(stack, "coil");
            total += level;
            if (wear && level > 0)
                stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + level * COIL_WEAR_PER_LEVEL));
        }
        return total;
    }

    /** Marks when a coiled wearer started a grounded sneak (-1: sneaking without a coil, not looked up again). */
    @SubscribeEvent
    public static void onSneak(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide)
            return;
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (!player.isShiftKeyDown()) {
            if (data.contains(COIL_START))
                data.remove(COIL_START);
        } else if (player.onGround() && !data.contains(COIL_START))
            data.putLong(COIL_START, coil(player, false) > 0 ? player.level().getGameTime() : -1L);
    }

    @SubscribeEvent
    public static void onJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) || player.level().isClientSide
                || !player.getPersistentData().contains(COIL_START))
            return;
        long start = player.getPersistentData().getLong(COIL_START);
        player.getPersistentData().remove(COIL_START);
        long held = Math.min(COIL_MAX, player.level().getGameTime() - start);
        if (start < 0 || held < COIL_MIN)
            return;
        int level = coil(player, true);
        double lift = Math.min(COIL_LIFT_CAP, COIL_LIFT_PER_LEVEL * level * held / COIL_MAX);
        player.setDeltaMovement(player.getDeltaMovement().add(0, lift, 0));
        player.hurtMarked = true;
    }

    public static void register() {
    }

    private static int level(ItemStack stack, String kind) {
        int level = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals(kind))
                level += a.level();
        return level;
    }

    @SubscribeEvent
    public static void onShot(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractArrow arrow) || arrow.tickCount > 0)
            return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof ModularBow || weapon.getItem() instanceof ModularCrossbow))
            return;
        int level = level(weapon, "store");
        if (level > 0)
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1f + STORE_PER_LEVEL * level));
    }

    /** Takes an extra tick off the remaining draw now and then, so a drawn bow reaches full charge sooner by that share. */
    @SubscribeEvent
    public static void onDraw(LivingEntityUseItemEvent.Tick event) {
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof ModularBow || stack.getItem() instanceof ModularCrossbow))
            return;
        int level = level(stack, "release");
        if (level <= 0)
            return;
        float share = Math.min(0.6f, RELEASE_PER_LEVEL * level);
        int used = stack.getUseDuration(event.getEntity()) - event.getDuration();
        if ((int) ((used + 1) * share) > (int) (used * share))
            event.setDuration(event.getDuration() - 1);
    }
}
