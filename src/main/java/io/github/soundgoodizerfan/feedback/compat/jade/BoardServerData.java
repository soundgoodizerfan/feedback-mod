// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.robot.RobotBoardBlockEntity;
import io.github.soundgoodizerfan.feedback.robot.RobotTuning;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

/** A job board's slots (free, claimed by a robot's short tag, done), and how many of the robots in range it hears. */
public class BoardServerData implements IServerDataProvider<BlockAccessor> {

    public static final BoardServerData INSTANCE = new BoardServerData();

    static final String STATES = "BoardStates", CLAIMERS = "BoardClaimers", HEARD = "BoardHeard", RANGE = "BoardRange";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof RobotBoardBlockEntity board))
            return;
        int[] states = new int[RobotTuning.BOARD_SLOTS];
        ListTag claimers = new ListTag();
        for (int i = 0; i < states.length; i++) {
            states[i] = board.slotState(i);
            claimers.add(StringTag.valueOf(board.claimerTag(i)));
        }
        data.putIntArray(STATES, states);
        data.put(CLAIMERS, claimers);
        data.putInt(HEARD, board.heardCount());
        data.putInt(RANGE, board.inRangeCount());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("robot_board");
    }
}
