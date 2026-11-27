// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Whatever throws sparks drowns a radio near it: a block tagged {@code feedback:radio_noise} (the arc heads) within {@link RadioTuning#NOISE_BLOCKS}
 * of a set cuts that end's link by {@link RadioTuning#NOISE_CUT}, unless the person stands boxed in by four or more blocks tagged
 * {@code feedback:radio_shields} (lead-lined chests). The same rule the robot mast uses for its own ends.
 */
public final class RadioNoise {

    private RadioNoise() {
    }

    /** Whether four or more of {@code pos}'s neighbours are shield blocks. */
    public static boolean shielded(Level level, BlockPos pos) {
        int shields = 0;
        for (Direction d : Direction.values())
            if (level.getBlockState(pos.relative(d)).is(FTags.RADIO_SHIELDS))
                shields++;
        return shields >= 4;
    }

    /** Whether a noise block stands within {@code range} of {@code at}; {@code scan} is the caller's scratch position. */
    public static boolean noisy(Level level, BlockPos at, int range, BlockPos.MutableBlockPos scan) {
        for (int dx = -range; dx <= range; dx++)
            for (int dy = -range; dy <= range; dy++)
                for (int dz = -range; dz <= range; dz++) {
                    scan.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (level.isLoaded(scan) && level.getBlockState(scan).is(FTags.RADIO_NOISE))
                        return true;
                }
        return false;
    }

    /** What noise takes off the link quality at {@code person}'s end: {@link RadioTuning#NOISE_CUT} beside a noise block outside a shield, else nothing. */
    public static float cut(ServerPlayer person) {
        BlockPos at = person.blockPosition();
        Level level = person.level();
        return !shielded(level, at) && noisy(level, at, RadioTuning.NOISE_BLOCKS, new BlockPos.MutableBlockPos()) ? RadioTuning.NOISE_CUT : 0f;
    }
}
