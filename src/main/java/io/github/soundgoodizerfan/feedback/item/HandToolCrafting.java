// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.Map;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

/**
 * Hand work makes a noise.
 *
 * <h2>Why this is worth a class</h2>
 * Philosophy 8: the player's own senses are free, and the mod leans on that everywhere else -- a
 * labouring rotation network smokes and creaks at its sources, a quench hisses, a hammer clangs on
 * the anvil. Hand work was the one operation in the mod that happened in total silence, which made
 * a crafting grid feel like a menu rather than like the same blow the machine lands. It is the same
 * blow. It should sound like it.
 *
 * <p>It gives away nothing. A noise is an adjective -- it says <em>that happened</em> and names no
 * figure -- so it sits on the free side of §8's line alongside the smoke and the creak.
 *
 * <h2>Why an event and not the item</h2>
 * GregTech plays its tool sound inside {@code getCraftingRemainingItem}, which works but hides a
 * side effect in a getter that both logical sides call. {@code ItemCraftedEvent} is the honest
 * place: it fires once, when a craft actually happens, and it hands over the grid to look at.
 *
 * <h2>One clang per tick, and why that is the right number</h2>
 * Shift-clicking can craft several times in a single tick, and an untroubled implementation would
 * fire every sound at once. Throttling to one per player per tick collapses that into a single
 * report, while ordinary one-at-a-time crafting still clangs every time. (The hand hammer itself
 * takes one piece, so its shift-click is one blow; the throttle is for any tool that does not.)
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HandToolCrafting {

    /**
     * Last tick each player was heard working. Weak so a disconnecting player is not kept alive by
     * their own hammering, and never read across threads -- crafting happens on the server thread.
     */
    private static final Map<Player, Long> LAST_HEARD = new WeakHashMap<>();

    private HandToolCrafting() {
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide)
            return;

        ItemStack held = toolIn(event.getInventory());
        if (!(held.getItem() instanceof HandTool tool))
            return;
        // Every swing costs food, harder swings more. Before the clang throttle: that is per tick,
        // and every craft is a swing.
        player.causeFoodExhaustion(FTuning.HAND_SWING_EXHAUSTION_PER_ST * tool.getStrength(held).value());

        long now = level.getGameTime();
        Long heard = LAST_HEARD.get(player);
        if (heard != null && heard == now)
            return;
        LAST_HEARD.put(player, now);

        // Quiet and high, the same as the Mechanical Hammer's blow -- one arm is not a machine, but
        // it is the same anvil and it should be recognisably the same sound.
        level.playSound(null, player.blockPosition(), tool.getWorkSound(), SoundSource.PLAYERS,
                0.3f, 1.5f + level.random.nextFloat() * 0.2f);
    }

    /**
     * A tool in an open menu (a crafting grid) is not ticked, so a mighty swing would outlast the Strength
     * effect there. Drop it to what the player can manage and re-run the grid, so the result is recomputed.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0)
            return;
        for (var slot : player.containerMenu.slots) {
            ItemStack stack = slot.getItem();
            HandSwing swing = stack.get(FDataComponents.HAND_SWING);
            if (swing != null && swing.within(player) != swing) {
                stack.set(FDataComponents.HAND_SWING, swing.within(player));
                player.containerMenu.slotsChanged(slot.container);
            }
        }
    }

    /** The one hand tool in the grid, or empty. A craft using two of them is not a thing. */
    private static ItemStack toolIn(net.minecraft.world.Container grid) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.getItem() instanceof HandTool) {
                if (!found.isEmpty())
                    return ItemStack.EMPTY;
                found = stack;
            }
        }
        return found;
    }
}
