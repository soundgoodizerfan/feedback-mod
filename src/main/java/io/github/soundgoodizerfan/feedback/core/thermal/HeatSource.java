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
package io.github.soundgoodizerfan.feedback.core.thermal;

import net.minecraft.tags.FluidTags;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A fire. Something that holds a flame temperature, which is the ceiling on anything it heats.
 *
 * <h2>The source decides where its heat goes</h2>
 * A source has a {@link #heatFacing() facing}. A heat-accepting block touching that face gets the
 * source's whole temperature; one touching a side next to it gets {@link #sideShare()} of the way
 * there from ambient (heat lost on the way round), and one behind it gets {@link #backShare()}
 * (none). Fire sources always face up and keep the old fire-under-a-vessel layout; a coil, an arc
 * head or a creative source can be rotated. A source whose heat is very concentrated overrides
 * the shares. The receiver only sums what the sources around it give; it holds no opinion on
 * geometry, which is why {@link #at} is the one reader.
 *
 * <h2>Why a source is not a {@link ThermalBody}</h2>
 * A body has a mass and a temperature that other things change. A fire has a temperature and no
 * mass worth modelling -- nothing the player does cools a fire, they either feed it or they do
 * not. Keeping them apart is what stops the crucible needing to know whether it is over charcoal,
 * over lava, or over something a later slice invents: it asks the block below how hot it burns and
 * has no further opinion.
 */
public interface HeatSource {

    /** How hot this burns right now. Ambient when it is not lit. */
    Tu getFireTu();

    /**
     * What this did across {@code gap} ticks that nobody was there to tick, and have it spend the
     * fuel it burned. Called by whatever sits on top of it as it comes back from an unloaded
     * chunk -- see {@link Catchup}. Nothing by default: a source with no fuel (an element, a
     * flame that is only ever there) has nothing to have run out of.
     */
    default FireSpan catchUp(long gap) {
        return FireSpan.NONE;
    }

    /** The face this source radiates from. Fire sources are always up. */
    default Direction heatFacing() {
        return Direction.UP;
    }

    /** Reach on the four sides next to the facing. A concentrated source (arc, plasma) returns 1. */
    default float sideShare() {
        return FTuning.HEAT_SIDE_SHARE;
    }

    /** Reach on the side behind the facing. Nothing by default. */
    default float backShare() {
        return 0f;
    }

    /** The facing a rotatable source block holds, up when its block has none. */
    static Direction facingOf(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.UP;
    }

    /** How much of this source's heat reaches the block {@code toward} it (the direction from the source to that block). */
    default float shareToward(Direction toward) {
        Direction face = heatFacing();
        if (toward == face)
            return 1f;
        if (toward == face.getOpposite())
            return backShare();
        return sideShare();
    }

    /** {@link #at} across a gap: the fire the sources round {@code pos} gave over {@code gap} ticks. */
    static FireSpan spanAt(Level level, BlockPos pos, long gap) {
        FireSpan best = FireSpan.NONE;
        Tu ambient = null;
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (!(be instanceof HeatSource source))
                continue;
            float share = source.shareToward(dir.getOpposite());
            if (share <= 0f)
                continue;
            FireSpan span = source.catchUp(gap).clampedTo(gap);
            if (span.litTicks() <= 0L)
                continue;
            if (ambient == null)
                ambient = AmbientTable.at(level);
            float tu = ambient.value() + share * (span.tu() - ambient.value());
            if (tu > best.tu())
                best = new FireSpan(span.litTicks(), tu);
        }
        if (level.getFluidState(pos.below()).is(FluidTags.LAVA) && FTuning.FIRE_TU_LAVA.value() > best.tu())
            return new FireSpan(gap, FTuning.FIRE_TU_LAVA.value());
        return best;
    }

    /**
     * How hot the heat reaching {@code pos} is: the hottest of the sources round it, each at the
     * share its own facing gives this side.
     *
     * <h3>Lava is handled here and nowhere else</h3>
     * It is a plain vanilla block with no block entity, so it cannot push. The block under a
     * position being lava is the one special case, as it always was, and by tag so any mod's lava
     * counts.
     */
    static Tu at(Level level, BlockPos pos) {
        Tu ambient = AmbientTable.at(level);
        float best = ambient.value();
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (!(be instanceof HeatSource source))
                continue;
            float share = source.shareToward(dir.getOpposite());
            if (share <= 0f)
                continue;
            float fire = source.getFireTu().value();
            if (fire <= ambient.value())
                continue;
            best = Math.max(best, ambient.value() + share * (fire - ambient.value()));
        }
        if (level.getFluidState(pos.below()).is(FluidTags.LAVA))
            best = Math.max(best, FTuning.FIRE_TU_LAVA.value());
        return new Tu(best);
    }
}
