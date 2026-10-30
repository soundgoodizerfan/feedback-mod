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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeXpBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The runtime kind of the Reservoir effect: {@code reserve}, a part that
 * holds experience points as polarization, {@link #CAPACITY_PER_LEVEL} a level, summed over the parts of a tool. It
 * stores and never makes. Charged by sneak-use in the air (a share of the holder's own points) or sneak-use on a
 * creative XP source (the operator-tab source every new energy type gets first); drawn on by Mend before the
 * holder is, and filled by Tithe before orbs drop ({@link MendKinds}). Not built: heat past the attuning point
 * dumping what is stored, and an exact reading of the figure (the tooltip says only how full it is).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ReservoirKinds {

    /** Experience points a level of reserve holds. [OPEN] until played */
    private static final int CAPACITY_PER_LEVEL = 60;
    /** Points one sneak-use in the air moves from the holder into the part. [OPEN] until played */
    private static final int TRANSFER = 20;

    private ReservoirKinds() {
    }

    public static void register() {
    }

    static int capacity(ItemStack stack) {
        return TideKinds.level(stack, "reserve") * CAPACITY_PER_LEVEL;
    }

    /** How full a Reservoir tool is, 0..1; negative when it holds none at all. For the HUD bar. */
    public static float fill(ItemStack stack) {
        int cap = capacity(stack);
        return cap == 0 ? -1f : stored(stack) / (float) cap;
    }

    static int stored(ItemStack stack) {
        return Math.min(capacity(stack), stack.getOrDefault(FDataComponents.RESERVOIR_XP.get(), 0));
    }

    /** Puts up to {@code points} in; the points that went. */
    static int add(ItemStack stack, int points) {
        int taken = Math.max(0, Math.min(points, capacity(stack) - stored(stack)));
        if (taken > 0)
            stack.set(FDataComponents.RESERVOIR_XP.get(), stored(stack) + taken);
        return taken;
    }

    /** Takes up to {@code points} out; the points that came. */
    static int take(ItemStack stack, int points) {
        int taken = Math.min(points, stored(stack));
        if (taken > 0)
            stack.set(FDataComponents.RESERVOIR_XP.get(), stored(stack) - taken);
        return taken;
    }

    @SubscribeEvent
    public static void onUseOnSource(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player.level().isClientSide || !player.isShiftKeyDown() || capacity(stack) == 0
                || !(player.level().getBlockEntity(event.getPos()) instanceof CreativeXpBlockEntity source) || !source.on())
            return;
        int moved = add(stack, source.points());
        player.displayClientMessage(Component.translatable(moved > 0 ? "feedback.reservoir.filled" : "feedback.reservoir.full"), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player.level().isClientSide || !player.isShiftKeyDown() || capacity(stack) == 0 || player.totalExperience <= 0)
            return;
        int moved = add(stack, Math.min(TRANSFER, player.totalExperience));
        if (moved > 0) {
            player.giveExperiencePoints(-moved);
            player.displayClientMessage(Component.translatable("feedback.reservoir.filled"), true);
        } else
            player.displayClientMessage(Component.translatable("feedback.reservoir.full"), true);
    }
}
