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
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A chassis frame's drivetrain, line by line, from the frame block or any of its cells: whether a
 * tube couples to something turning, how fast the chain turns, and each tire's load and grip.
 */
public class FrameServerData implements IServerDataProvider<BlockAccessor> {

    public static final FrameServerData INSTANCE = new FrameServerData();

    static final String COUPLED = "DriveCoupled", LINES = "DriveLines";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        FrameBlockEntity frame = accessor.getBlockEntity() instanceof FrameBlockEntity be ? be
                : accessor.getBlockEntity() instanceof ChassisCellBlockEntity ? FrameBlockEntity.owning(accessor.getLevel(), accessor.getPosition())
                : null;
        if (frame == null)
            return;
        data.putBoolean(COUPLED, !frame.drivetrain().chainTubes().isEmpty());
        ListTag lines = new ListTag();
        for (String line : frame.drivetrain().status())
            lines.add(StringTag.valueOf(line));
        data.put(LINES, lines);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("frame_drive");
    }
}
