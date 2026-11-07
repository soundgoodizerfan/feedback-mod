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
package io.github.soundgoodizerfan.feedback.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A link card: used on a Depot it records that depot's position; used on a second depot it links the two (both ways) and is wiped. The link is a
 * recorded fact the player carried from one place to the other, never found by a search.
 */
public class DepotLinkCardItem extends Item {

    private static final String POS = "DepotPos";

    public DepotLinkCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(level.getBlockEntity(context.getClickedPos()) instanceof DepotBlockEntity depot))
            return InteractionResult.PASS;
        Player player = context.getPlayer();
        ItemStack card = context.getItemInHand();
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var recorded = tag.contains(POS) ? NbtUtils.readBlockPos(tag, POS) : java.util.Optional.<BlockPos>empty();
        if (recorded.isEmpty()) {
            tag.put(POS, NbtUtils.writeBlockPos(depot.getBlockPos()));
            card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            say(player, "feedback.depot.recorded");
            return InteractionResult.CONSUME;
        }
        BlockPos other = recorded.get();
        if (other.equals(depot.getBlockPos())) {
            say(player, "feedback.depot.same");
        } else if (level.getBlockEntity(other) instanceof DepotBlockEntity first) {
            first.link(depot.self());
            depot.link(first.self());
            card.remove(DataComponents.CUSTOM_DATA);
            say(player, "feedback.depot.linked");
        } else {
            // The first depot is gone or its chunk is not loaded: the card keeps what it recorded until it can be used again.
            say(player, "feedback.depot.unreachable");
        }
        return InteractionResult.CONSUME;
    }

    private static void say(Player player, String key) {
        if (player != null)
            player.displayClientMessage(Component.translatable(key), true);
    }
}
