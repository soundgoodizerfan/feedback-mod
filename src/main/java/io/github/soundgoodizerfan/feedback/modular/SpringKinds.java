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

    private SpringKinds() {
    }

    static void register() {
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
