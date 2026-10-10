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
package io.github.soundgoodizerfan.feedback.vehicle;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Lifts everything touching it out of the world into one Sable physics body.
 *
 * <p>Ported from SummerWorks (the author's own mod) as the smoke test that a Sable body works
 * inside Feedback -- {@code feedback_vehicles_spec.md} section 6, step 2. It is <em>not</em> the
 * chassis system: vehicles are built from welded vertices and plates (section 4), not from a
 * floodfill of placed blocks. When the vertex assembly exists, this is what hands the finished
 * frame to Sable, and the floodfill goes.
 */
public class VehicleCoreBlock extends Block {
    /** Biggest body one core will assemble. */
    public static final int MAX_BLOCKS = 4096;

    public VehicleCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (SableCompanion.INSTANCE.getContaining(level, pos) != null) {
            player.displayClientMessage(Component.translatable("message.feedback.vehicle.already_assembled"), true);
            return InteractionResult.CONSUME;
        }
        ServerSubLevel body = assemble(server, pos);
        player.displayClientMessage(body != null
                ? Component.translatable("message.feedback.vehicle.assembled")
                : Component.translatable("message.feedback.vehicle.too_big", MAX_BLOCKS), true);
        return InteractionResult.CONSUME;
    }

    /** Assembles the body around a core, or returns null if it is too big to gather. */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, BlockPos core) {
        SubLevelAssemblyHelper.GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(core, level, MAX_BLOCKS, null);
        if (result.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS || result.blocks() == null) return null;
        return SubLevelAssemblyHelper.assembleBlocks(level, core, result.blocks(), result.boundingBox());
    }
}
