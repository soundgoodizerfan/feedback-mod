// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
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

    static final String COUPLED = "DriveCoupled", LINES = "DriveLines", PADS = "WorstPad";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        FrameBlockEntity frame = accessor.getBlockEntity() instanceof FrameBlockEntity be ? be
                : accessor.getBlockEntity() instanceof ChassisCellBlockEntity cell
                ? (cell.frame() != null ? FrameBlockEntity.loaded(cell.frame()) : FrameBlockEntity.owning(accessor.getLevel(), accessor.getPosition()))
                : null;
        if (frame == null)
            return;
        data.putBoolean(COUPLED, !frame.drivetrain().tubeSpin().isEmpty());
        frame.frame().tires().values().stream().filter(t -> t.brake() != null)
                .mapToDouble(Frame.Tire::padCondition).min().ifPresent(c -> data.putFloat(PADS, (float) c));
        ListTag lines = new ListTag();
        lines.add(StringTag.valueOf(String.format(java.util.Locale.ROOT, "Controls: steer %.3f, brake %.3f",
                frame.steering(), frame.brake())));
        for (String line : frame.drivetrain().status())
            lines.add(StringTag.valueOf(line));
        data.put(LINES, lines);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("frame_drive");
    }
}
