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
public record DataNodeRef(BlockPos pos, @Nullable Direction side, @Nullable BlockPos frame) implements Comparable<DataNodeRef> {

    private static final String POS = "Pos";
    private static final String SIDE = "Side";
    private static final String FRAME = "Frame";

    public DataNodeRef(BlockPos pos, @Nullable Direction side) {
        this(pos, side, null);
    }

    /**
     * A part of a vehicle is named as an offset from the vehicle's frame block, since the two are
     * lifted together (vehicles spec §5): then {@link #pos} is that offset and {@code frame} is where
     * the frame stood when the link was made -- only a hint for the Programmer's checks, never
     * trusted at run time, when the controller finds its own frame. Null for everything else.
     */
    public boolean framed() {
        return frame != null;
    }

    /** Where the target stood when linked: {@link #pos} itself unless {@link #framed}. */
    public BlockPos linkedAt() {
        return frame == null ? pos : frame.offset(pos);
    }

    /** For a printed Punch Card's stored program -- persisted the same way an NBT tag would be. */
    public static final Codec<DataNodeRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(DataNodeRef::pos),
            Direction.CODEC.optionalFieldOf("side").forGetter(ref -> Optional.ofNullable(ref.side())),
            BlockPos.CODEC.optionalFieldOf("frame").forGetter(ref -> Optional.ofNullable(ref.frame()))
    ).apply(instance, (pos, side, frame) -> new DataNodeRef(pos, side.orElse(null), frame.orElse(null))));

    public static final StreamCodec<io.netty.buffer.ByteBuf, DataNodeRef> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DataNodeRef::pos,
            ByteBufCodecs.optional(Direction.STREAM_CODEC), ref -> Optional.ofNullable(ref.side()),
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ref -> Optional.ofNullable(ref.frame()),
            (pos, side, frame) -> new DataNodeRef(pos, side.orElse(null), frame.orElse(null)));

    /**
     * An arbitrary but consistent order -- not meaning, just enough that a link between A and B
     * can be drawn from whichever end sorts first and never twice. See the renderer.
     */
    @Override
    public int compareTo(DataNodeRef other) {
        int byPos = Long.compare(pos.asLong(), other.pos.asLong());
        if (byPos != 0)
            return byPos;
        int mine = side == null ? -1 : side.get3DDataValue();
        int theirs = other.side == null ? -1 : other.side.get3DDataValue();
        return Integer.compare(mine, theirs);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(POS, pos.asLong());
        tag.putInt(SIDE, side == null ? -1 : side.get3DDataValue());
        if (frame != null)
            tag.putLong(FRAME, frame.asLong());
        return tag;
    }

    public static DataNodeRef fromTag(CompoundTag tag) {
        int side = tag.getInt(SIDE);
        return new DataNodeRef(BlockPos.of(tag.getLong(POS)), side < 0 ? null : Direction.from3DDataValue(side),
                tag.contains(FRAME) ? BlockPos.of(tag.getLong(FRAME)) : null);
    }
}
