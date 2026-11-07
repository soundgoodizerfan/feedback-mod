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
package io.github.soundgoodizerfan.feedback.village;

import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Market Cross. Holds nothing saved: it is <em>active</em> when the paved ground around it holds a square (enough open road tiles under the
 * sky, with a heart that is more than a street's width), found by the same flood that gives a Counter its road reach and rechecked on a slow
 * cadence when asked. The player earns the market by paving a plaza, not by crafting a bigger block.
 */
public class MarketCrossBlockEntity extends BlockEntity {

    public static final BlockIndex<MarketCrossBlockEntity> INDEX = new BlockIndex<>();

    private boolean active;
    private long checkedAt = Long.MIN_VALUE;

    public MarketCrossBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.CROSS_ENTITY.get(), pos, state);
    }

    /** Whether the plaza around the cross stands; recomputed at most every {@link VillageTuning#MARKET_RECHECK_TICKS}. */
    public boolean active() {
        Level l = level;
        if (l == null || l.isClientSide)
            return false;
        long t = l.getGameTime();
        if (t - checkedAt >= VillageTuning.MARKET_RECHECK_TICKS) {
            checkedAt = t;
            active = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                RoadReach r = RoadReach.flood(l, worldPosition.relative(d));
                if (r.openToSky(l) >= VillageTuning.MARKET_PLAZA_MIN && r.interior() >= VillageTuning.MARKET_INTERIOR_MIN) {
                    active = true;
                    break;
                }
            }
        }
        return active;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        INDEX.add(this);
    }

    @Override
    public void setRemoved() {
        INDEX.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        INDEX.remove(this);
        super.onChunkUnloaded();
    }
}
