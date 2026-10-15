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
package io.github.soundgoodizerfan.feedback.item;

import java.util.Map;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

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
        if (!(held.getItem() instanceof HandToolItem tool))
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

    /** The one hand tool in the grid, or empty. A craft using two of them is not a thing. */
    private static ItemStack toolIn(net.minecraft.world.Container grid) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.getItem() instanceof HandToolItem) {
                if (!found.isEmpty())
                    return ItemStack.EMPTY;
                found = stack;
            }
        }
        return found;
    }
}
