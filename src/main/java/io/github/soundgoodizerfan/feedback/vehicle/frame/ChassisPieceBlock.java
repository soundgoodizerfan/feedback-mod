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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A 1/16-shaped piece of a frame's collision, cut on assembly: a plate cell ({@link ChassisCellBlock})
 * or a tube cell ({@link ChassisTubeBlock}). Invisible -- the frame renders itself -- and it mines,
 * sounds and resists explosions as the hardest material passing through it. Breaking one breaks
 * every plate and tube through it, whole.
 */
public abstract class ChassisPieceBlock extends Block implements BlockSubLevelCollisionShape {

    protected ChassisPieceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return material(level, pos).getDestroyProgress(player, level, pos);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return material(level, pos).getSoundType(level, pos, entity);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return material(level, pos).getExplosionResistance(level, pos, explosion);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server)
            breakPlates(server, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel server)
            breakPlates(server, pos, null);
        super.onBlockExploded(state, level, pos, explosion);
    }

    /**
     * Takes every plate and tube through {@code pos} out of its frame, drops the material a tool
     * could have harvested (none for an explosion or in creative), and re-cuts the cells next tick
     * -- after this block is gone, so a neighbouring cell is not put back and then deleted.
     */
    private static void breakPlates(ServerLevel level, BlockPos pos, @Nullable Player player) {
        FrameBlockEntity frame = owner(level, pos);
        if (frame == null)
            return;
        BlockPos rel = pos.subtract(frame.getBlockPos());
        for (Frame.Plate p : FrameAssembly.platesThrough(frame.frame(), rel)) {
            frame.frame().removePlate(p.a(), p.b(), p.c());
            BlockState material = p.materialState();
            if (player != null && !player.getAbilities().instabuild
                    && (!material.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(material)))
                Block.popResource(level, pos, new ItemStack(material.getBlock()));
        }
        for (long key : FrameAssembly.tubesThrough(frame.frame(), rel)) {
            BlockState material = frame.frame().tubes().get(key).materialState();
            frame.frame().removeTube(Frame.edgeA(key), Frame.edgeB(key));
            if (player != null && !player.getAbilities().instabuild
                    && (!material.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(material)))
                Block.popResource(level, pos, new ItemStack(material.getBlock()));
        }
        frame.cells().remove(rel);
        level.getServer().tell(new TickTask(level.getServer().getTickCount(), frame::changed));
    }

    /** The material this cell mines as: the hardest plate or tube through it, iron if it has lost its frame. */
    private static BlockState material(BlockGetter level, BlockPos pos) {
        FrameBlockEntity frame = owner(level, pos);
        BlockState best = Blocks.IRON_BLOCK.defaultBlockState();
        if (frame == null)
            return best;
        float hardest = -1;
        BlockPos rel = pos.subtract(frame.getBlockPos());
        List<BlockState> through = new ArrayList<>();
        for (Frame.Plate p : FrameAssembly.platesThrough(frame.frame(), rel))
            through.add(p.materialState());
        for (long key : FrameAssembly.tubesThrough(frame.frame(), rel))
            through.add(frame.frame().tubes().get(key).materialState());
        for (BlockState m : through) {
            float h = m.getDestroySpeed(level, pos);
            if (h > hardest) {
                hardest = h;
                best = m;
            }
        }
        return best;
    }

    /**
     * The frame that cut this cell. Frames sit within eight blocks of their cells, so the chunk and
     * its neighbours hold it; walking their block entities is far cheaper than scanning a cube.
     */
    @Nullable
    static FrameBlockEntity owner(BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof Level level))
            return null;
        ChunkPos c = new ChunkPos(pos);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                if (chunk == null)
                    continue;
                for (BlockEntity be : chunk.getBlockEntities().values())
                    if (be instanceof FrameBlockEntity f && f.cells().contains(pos.subtract(f.getBlockPos())))
                        return f;
            }
        return null;
    }

}
