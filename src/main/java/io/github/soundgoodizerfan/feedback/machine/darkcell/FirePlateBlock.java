// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A plate that burns what stands on it, as hot as the heat pointed at it. A mob that does not burn is untouched; the drops come out cooked, or charred if they wait. */
public class FirePlateBlock extends Block implements KillMethodBlock {

    /** Invented. Below this a plate is only warm. */
    private static final float WORKING_TU = 350f;

    public FirePlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public KillMethod method(Level level, BlockPos pos, BlockState state) {
        float tu = HeatSource.at(level, pos).value();
        return new KillMethod() {
            @Override
            public KillKind kind() {
                return KillKind.HEAT;
            }

            @Override
            public boolean ready() {
                return tu >= WORKING_TU;
            }

            @Override
            public float damage(MobFacts facts) {
                return facts.fireImmune() ? 0f : (tu - 250f) / 20f;
            }
        };
    }
}
