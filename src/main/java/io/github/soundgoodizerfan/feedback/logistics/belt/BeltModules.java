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
package io.github.soundgoodizerfan.feedback.logistics.belt;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.logistics.sort.Band;
import io.github.soundgoodizerfan.feedback.process.OreLadder;
import io.github.soundgoodizerfan.feedback.logistics.sort.Reading;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortReader;
import io.github.soundgoodizerfan.feedback.logistics.sort.Sorter;
import io.github.soundgoodizerfan.feedback.logistics.sort.Verdict;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What each module does with a unit reaching its cell's centre. A module picks a fate (carry on, take
 * the unit off, hold it) from the unit's physical reading; none reads an item id.
 */
final class BeltModules {

    private BeltModules() {
    }

    static BeltRun.Action act(BeltRun run, ModuleHost be, Cargo c, int idx, boolean last) {
        Level level = run.level;
        BeltModule m = be.module();
        BlockPos pos = be.getBlockPos();
        Direction face = be.face();
        boolean live = be.live();
        return switch (m) {
            case FUNNEL_IN -> BeltRun.Action.CONTINUE;
            case FUNNEL_OUT -> {
                ItemStack rest = BeltTargets.deliver(level, pos.relative(face), face.getOpposite(), c.stack, false, false);
                yield settle(c, rest, last);
            }
            case CHUTE -> chute(level, be, c, last);
            case DIVERTER -> be.engaged() ? sweep(level, pos, face, c) : BeltRun.Action.CONTINUE;
            case SPLITTER -> {
                Direction a = be.flip() ? run.facing.getCounterClockWise() : run.facing.getClockWise();
                if (sweep(level, pos, a, c) == BeltRun.Action.TAKEN)
                    yield BeltRun.Action.TAKEN;
                yield sweep(level, pos, a.getOpposite(), c);
            }
            case MAGNET, ELECTROMAGNET, OPTICAL, WEIGHER -> {
                if (!be.engaged() || !live)
                    yield BeltRun.Action.CONTINUE;
                Band band = m == BeltModule.MAGNET || m == BeltModule.ELECTROMAGNET ? Band.PULLED : be.band();
                if (m == BeltModule.OPTICAL) {
                    be.soil();
                    BeltRun.Action ore = sortOre(level, pos, face, be, c);
                    if (ore != null)
                        yield ore;
                }
                Verdict v = Sorter.decide(band, c.stack, level, m.detector, be.dirty(), c.seed ^ (level.getGameTime() * 0x9E3779B97F4A7C15L));
                yield switch (v) {
                    case MATCH -> sweep(level, pos, face, c);
                    case MISS -> BeltRun.Action.CONTINUE;
                    case UNKNOWN -> switch (m.gear.unexpected()) {
                        case REJECT -> sweep(level, pos, face.getOpposite(), c);
                        default -> BeltRun.Action.CONTINUE;
                    };
                };
            }
            case AIR_JET -> {
                if (!be.engaged())
                    yield BeltRun.Action.CONTINUE;
                float pu = be.jetPressure();
                Reading r = SortReader.read(c.stack, level);
                boolean wet = level.isRainingAt(pos.above());
                if (pu >= LogisticsTuning.JET_MIN_PU && !r.unknown() && !r.sticky() && !wet && r.density() < pu * LogisticsTuning.JET_DENSITY_PER_PU
                        && SortReader.bandOf(r, SortProperty.DENSITY) != Band.HEAVY)
                    yield sweep(level, pos, face, c);
                yield BeltRun.Action.CONTINUE;
            }
            case SCREEN -> {
                if (be.fouled())
                    yield BeltRun.Action.CONTINUE;
                float aperture = LogisticsTuning.SCREEN_APERTURE_MU[Math.max(0, Math.min(2, be.aperture()))];
                if (SortReader.unitMass(c.stack) >= aperture)
                    yield BeltRun.Action.CONTINUE;
                boolean[] broke = {false};
                ItemStack rest = BeltTargets.fall(level, pos.below(), c.stack, false, broke);
                if (rest.isEmpty())
                    be.foul();
                yield settle(c, rest, false);
            }
            case COUNTER -> {
                be.pass(level.getGameTime());
                yield BeltRun.Action.CONTINUE;
            }
            case COOLER, FAN -> {
                if (!live)
                    yield BeltRun.Action.CONTINUE;
                float t = ItemHeat.get(c.stack, level).value();
                float floor = m == BeltModule.COOLER ? LogisticsTuning.COOLER_FLOOR_TU : AmbientTable.at(level).value();
                if (t > floor) {
                    float drop = m == BeltModule.COOLER ? LogisticsTuning.COOLER_COOL_TU : LogisticsTuning.FAN_COOL_TU;
                    ItemHeat.set(c.stack, new Tu(Math.max(floor, t - drop)), level);
                }
                yield BeltRun.Action.CONTINUE;
            }
            case HOOD, LINEAR -> BeltRun.Action.CONTINUE;
            case GATE -> be.engaged() ? BeltRun.Action.HOLD : BeltRun.Action.CONTINUE;
        };
    }

    /**
     * The optical sorter on ground ore: a unit of a bearer's crushed or dust whose grains the head tells from their rock (reflectivity against
     * the gangue's) is split, the concentrate swept off the side and the tailings dropped below; a unit it cannot tell goes on to be sorted
     * again. Null when the unit is not ore feed, so the ordinary band verdict decides. A dirty head reads less, like any optical reading.
     */
    private static BeltRun.Action sortOre(Level level, BlockPos pos, Direction face, ModuleHost be, Cargo c) {
        if (OreLadder.feed(c.stack).isEmpty())
            return null;
        if (be.dirty())
            return BeltRun.Action.CONTINUE;
        var split = OreLadder.attempt(c.stack, OreLadder.Mode.OPTICAL, level.random);
        if (split.isEmpty())
            return BeltRun.Action.CONTINUE;
        ItemStack rest = BeltTargets.deliver(level, pos.relative(face), face.getOpposite(), split.get().concentrate().copy(), false, true);
        if (!rest.isEmpty())
            return BeltRun.Action.CONTINUE; // nowhere for the concentrate to go: leave the unit be
        c.stack.shrink(1);
        BeltTargets.fall(level, pos.below(), split.get().tailings().copy(), false, new boolean[1]);
        return c.stack.isEmpty() ? BeltRun.Action.TAKEN : BeltRun.Action.CONTINUE;
    }

    /** A unit that was offered somewhere: gone, shortened (and held if it was the end), or untouched. */
    private static BeltRun.Action settle(Cargo c, ItemStack rest, boolean holdIfLeft) {
        if (rest.isEmpty())
            return BeltRun.Action.TAKEN;
        c.stack = rest;
        return holdIfLeft ? BeltRun.Action.HOLD : BeltRun.Action.CONTINUE;
    }

    /** Sweeps the whole unit off to the side; a side that will not take it jams the line. */
    private static BeltRun.Action sweep(Level level, BlockPos pos, Direction dir, Cargo c) {
        ItemStack rest = BeltTargets.deliver(level, pos.relative(dir), dir.getOpposite(), c.stack, false, true);
        return settle(c, rest, true);
    }

    /** Drops the unit off the belt: into the column below the face, or the cell beside it. Brittle cargo breaks on a long drop. */
    private static BeltRun.Action chute(Level level, ModuleHost be, Cargo c, boolean last) {
        Direction face = be.face();
        BlockPos from = face == Direction.DOWN ? be.getBlockPos().below() : face == Direction.UP ? null : be.getBlockPos().relative(face);
        if (from == null)
            return BeltRun.Action.CONTINUE;
        Reading r = SortReader.read(c.stack, level);
        boolean[] broke = {false};
        ItemStack rest = BeltTargets.fall(level, from, c.stack, r.brittle(), broke);
        if (broke[0]) {
            level.playSound(null, from, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6f, 1.2f);
            return BeltRun.Action.TAKEN;
        }
        return settle(c, rest, last);
    }
}
