// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mode A: a controller's pattern checked against the world. A cell matches a block tag or a part
 * role, never a block id. All four horizontal turns are tried and the closest is reported, so a
 * near miss names the wrong blocks rather than just "unformed".
 */
public final class PatternAssembler {

    private static final int MAX_FLAWS = 16;

    /** Exactly one of tag / role is set. */
    public record Cell(@Nullable TagKey<Block> tag, @Nullable String role) {
        boolean matches(Level level, BlockPos pos, BlockState state) {
            if (tag != null)
                return state.is(tag);
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof Part p && p.role().equals(role);
        }

        String describe() {
            return tag != null ? "#" + tag.location() : "role:" + role;
        }
    }

    /** Cell at an offset from the controller, in the pattern's unturned frame. */
    public record Entry(int dx, int dy, int dz, Cell cell) {
    }

    public record Pattern(List<Entry> entries) {
    }

    public record Result(boolean formed, List<BlockPos> blocks, List<Part> parts, List<Structure.Flaw> flaws) {
    }

    private PatternAssembler() {
    }

    public static Result assemble(Level level, BlockPos origin, @Nullable Pattern pattern) {
        if (pattern == null)
            return new Result(false, List.of(), List.of(), List.of(new Structure.Flaw(null, "unknown pattern")));
        Result best = null;
        int bestWrong = Integer.MAX_VALUE;
        for (int turn = 0; turn < 4; turn++) {
            List<BlockPos> blocks = new ArrayList<>();
            Set<Part> parts = new LinkedHashSet<>();
            List<Structure.Flaw> flaws = new ArrayList<>();
            int wrong = 0;
            for (Entry e : pattern.entries()) {
                int dx = e.dx(), dz = e.dz();
                for (int k = 0; k < turn; k++) {
                    int t = dx;
                    dx = -dz;
                    dz = t;
                }
                BlockPos pos = origin.offset(dx, e.dy(), dz);
                if (!level.isLoaded(pos) || !e.cell().matches(level, pos, level.getBlockState(pos))) {
                    wrong++;
                    if (flaws.size() < MAX_FLAWS)
                        flaws.add(new Structure.Flaw(pos, "expected " + e.cell().describe()));
                    continue;
                }
                blocks.add(pos);
                if (level.getBlockEntity(pos) instanceof Part p)
                    parts.add(p);
            }
            if (wrong < bestWrong) {
                bestWrong = wrong;
                best = new Result(wrong == 0, blocks, new ArrayList<>(parts), flaws);
            }
        }
        return best;
    }
}
