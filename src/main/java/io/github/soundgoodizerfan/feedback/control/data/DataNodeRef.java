// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import org.jetbrains.annotations.Nullable;

/**
 * Where the other end of a link is, not what it is -- resolved back to a live {@link DataNode}
 * on demand rather than cached, the same trade {@code ThermalBody} makes for a workpiece's
 * temperature. A node's identity is a block position plus an optional side: {@code null} for an
 * unsided node (a controller, occupying the whole block), a real {@link Direction} for a sided
 * one (a sensor fitting sitting on one face).
 * <p>
 * MrCrayfish's Furniture Mod: Refurbished (MIT) keys its electricity nodes by {@code BlockPos}
 * alone -- one node per block entity. Feedback widens that by one field because a fitting is
 * sided and a holder can carry several at once (up to six sensor fittings on one crucible); see
 * {@code TODO.md} for what else was and wasn't taken from that read.
 */
public record DataNodeRef(BlockPos pos, @Nullable Direction side, int voxel) implements Comparable<DataNodeRef> {

    private static final String POS = "Pos";
    private static final String SIDE = "Side";
    private static final String VOXEL = "Voxel";

    public DataNodeRef(BlockPos pos, @Nullable Direction side) {
        this(pos, side, -1);
    }

    /**
     * A micro-block voxel is named by its cell's position and its index in the cell ({@code voxel}, 0
     * to 63); -1 for every other node. The position is where the block stands now, in the world or
     * in a vehicle's plot; the Lift rewrites the references it moves ({@code ControllerCore#moved}).
     */
    public boolean isVoxel() {
        return voxel >= 0;
    }

    /** For a printed Punch Card's stored program -- persisted the same way an NBT tag would be. */
    public static final Codec<DataNodeRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(DataNodeRef::pos),
            Direction.CODEC.optionalFieldOf("side").forGetter(ref -> Optional.ofNullable(ref.side())),
            Codec.INT.optionalFieldOf("voxel", -1).forGetter(DataNodeRef::voxel)
    ).apply(instance, (pos, side, voxel) -> new DataNodeRef(pos, side.orElse(null), voxel)));

    public static final StreamCodec<io.netty.buffer.ByteBuf, DataNodeRef> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DataNodeRef::pos,
            ByteBufCodecs.optional(Direction.STREAM_CODEC), ref -> Optional.ofNullable(ref.side()),
            ByteBufCodecs.VAR_INT, ref -> ref.voxel() + 1,
            (pos, side, voxel) -> new DataNodeRef(pos, side.orElse(null), voxel - 1));

    /**
     * An arbitrary but consistent order -- not meaning, just enough that a link between A and B
     * can be drawn from whichever end sorts first and never twice. See the renderer.
     */
    @Override
    public int compareTo(DataNodeRef other) {
        int byPos = Long.compare(pos.asLong(), other.pos.asLong());
        if (byPos != 0)
            return byPos;
        if (voxel != other.voxel)
            return Integer.compare(voxel, other.voxel);
        int mine = side == null ? -1 : side.get3DDataValue();
        int theirs = other.side == null ? -1 : other.side.get3DDataValue();
        return Integer.compare(mine, theirs);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(POS, pos.asLong());
        tag.putInt(SIDE, side == null ? -1 : side.get3DDataValue());
        if (voxel >= 0)
            tag.putInt(VOXEL, voxel);
        return tag;
    }

    public static DataNodeRef fromTag(CompoundTag tag) {
        int side = tag.getInt(SIDE);
        return new DataNodeRef(BlockPos.of(tag.getLong(POS)), side < 0 ? null : Direction.from3DDataValue(side),
                tag.contains(VOXEL) ? tag.getInt(VOXEL) : -1);
    }
}
