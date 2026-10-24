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

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Bloom effect ({@code docs/feedback_enchant_roster.md} 23), on a hoe or shovel head. A swing
 * (use on a block) works the ground in a small area, then the tool cools down. {@code grow}: each growable crop or
 * grass block in the area takes one bone-meal step. {@code reap} (the mirror): each ripe crop is harvested and
 * replanted young. No upkeep and no wear; the cooldown is the cost.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BloomKinds {

    /** Blocks out from the struck block, per level, in x and z (capped by {@link #MAX_RADIUS}). [OPEN] until played */
    private static final int RADIUS_PER_LEVEL = 1;
    private static final int MAX_RADIUS = 3;
    /** Ticks before the next swing works the ground again. [OPEN] until played */
    private static final int COOLDOWN = 40;

    /** Worn boots: blocks walked per step of work, less one per level (floor {@link #MIN_STEP}), and the work's radius. [OPEN] until played */
    private static final float STEP_BLOCKS = 6f, MIN_STEP = 2f;
    private static final int WORN_RADIUS = 1;
    private static final String WALKED = "feedback_bloom_walked";

    private BloomKinds() {
    }

    public static void register() {
    }

    /**
     * Boots {@code plate}/{@code lining} (kinds {@code worn_grow}, {@code worn_reap}): every few blocks walked on the
     * ground, the 3x3 underfoot takes the same work a swing does. Checked every 10 ticks; the cooldown is the walking.
     */
    @SubscribeEvent
    public static void onWalk(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 10 != 0 || !player.onGround())
            return;
        int grow = BaitKinds.wornAt(player, EquipmentSlot.FEET, "worn_grow"), reap = BaitKinds.wornAt(player, EquipmentSlot.FEET, "worn_reap");
        if (grow + reap == 0)
            return;
        var data = player.getPersistentData();
        float walked = data.getFloat(WALKED);
        if (walked > player.walkDist)
            walked = player.walkDist; // walkDist restarts on respawn
        if (player.walkDist - walked < Math.max(MIN_STEP, STEP_BLOCKS - (grow + reap)))
            return;
        data.putFloat(WALKED, player.walkDist);
        Level level = player.level();
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-WORN_RADIUS, -1, -WORN_RADIUS), at.offset(WORN_RADIUS, 0, WORN_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (grow > 0 && state.getBlock() instanceof BonemealableBlock b
                    && b.isValidBonemealTarget(level, pos, state) && b.isBonemealSuccess(level, level.random, pos, state))
                b.performBonemeal((net.minecraft.server.level.ServerLevel) level, level.random, pos, state);
            else if (reap > 0 && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                Block.dropResources(state, level, pos, null, player, ItemStack.EMPTY);
                level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
            }
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack tool = event.getItemStack();
        if (level.isClientSide || player.getCooldowns().isOnCooldown(tool.getItem()))
            return;
        int grow = TideKinds.level(tool, "grow"), reap = TideKinds.level(tool, "reap");
        if (grow + reap == 0)
            return;
        int r = Math.min(MAX_RADIUS, RADIUS_PER_LEVEL * (grow + reap));
        BlockPos at = event.getPos();
        boolean did = false;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, 0, -r), at.offset(r, 1, r))) {
            BlockState state = level.getBlockState(pos);
            if (grow > 0 && state.getBlock() instanceof BonemealableBlock b
                    && b.isValidBonemealTarget(level, pos, state) && b.isBonemealSuccess(level, level.random, pos, state)) {
                b.performBonemeal((net.minecraft.server.level.ServerLevel) level, level.random, pos, state);
                did = true;
            } else if (reap > 0 && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                Block.dropResources(state, level, pos, null, player, tool);
                level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
                did = true;
            }
        }
        if (did)
            player.getCooldowns().addCooldown(tool.getItem(), COOLDOWN);
    }
}
