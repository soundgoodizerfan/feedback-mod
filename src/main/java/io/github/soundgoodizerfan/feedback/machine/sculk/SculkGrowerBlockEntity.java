// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Sculk Grower (roadmap X7): a living machine that works a culture without a hand. Next to a ripe Culture Dish it harvests the dish once the run
 * it sits on pays {@link ElectricMachineTuning#GROWER_XP} points at {@link ElectricMachineTuning#GROWER_LEVEL} or higher, in the dark and inside the
 * ambient band a relay organ needs. The products pop above the dish as a hoe's would; the dish's own growth is the rate limit, and its neglect
 * (clotting, a shrieker) is the failure. It holds no store of its own: a node that only draws.
 */
public class SculkGrowerBlockEntity extends SculkNodeBlockEntity {

    public SculkGrowerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SCULK_GROWER.get(), pos, state);
    }

    void tickServer() {
        if (level == null || (level.getGameTime() + worldPosition.hashCode()) % ElectricMachineTuning.GROWER_LOOK_TICKS != 0 || network() == null)
            return;
        float tu = AmbientTable.at(level).value();
        if (level.getMaxLocalRawBrightness(worldPosition) > ElectricMachineTuning.CULTURE_ORGAN_MAX_LIGHT
                || tu < ElectricMachineTuning.CULTURE_ORGAN_MIN_TU || tu > ElectricMachineTuning.CULTURE_ORGAN_MAX_TU)
            return;
        for (Direction d : Direction.values()) {
            BlockPos at = worldPosition.relative(d);
            if (level.getBlockEntity(at) instanceof CultureDishBlockEntity dish && dish.culture().isRipe()
                    && network().canPay(ElectricMachineTuning.GROWER_XP, ElectricMachineTuning.GROWER_LEVEL)
                    && network().draw(ElectricMachineTuning.GROWER_XP) >= ElectricMachineTuning.GROWER_XP) {
                CultureDishBlock.harvest(level, at, dish);
                return;
            }
        }
    }
}
