// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electromagnet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where a powered electromagnet's field is, for the things that ride on it from above (the levitation plate and
 * sleeve). A magnet writes its field here when its field changes and clears it when it is removed or unloaded, so a
 * plate in a physics step looks up at most a few dozen cells and never searches blocks or loads a chunk.
 * Server thread only; the maps are concurrent only so a reload cannot corrupt them.
 */
public final class MagnetField {

    /** Centre-to-centre reach in blocks: past this a magnet's field is nothing to a plate above it. */
    public static final double REACH = 3.0;
    /** Centre-to-centre distance at which the field is at full strength: the plate resting on the magnet. */
    public static final double TOUCH = 1.0;

    private static final Map<ResourceKey<Level>, Map<Long, Float>> FIELDS = new ConcurrentHashMap<>();

    private MagnetField() {
    }

    /** Records a magnet's field (0 to 1); zero or less clears it. */
    public static void set(Level level, BlockPos pos, float field) {
        if (field > 0f)
            FIELDS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(pos.asLong(), field);
        else
            clear(level, pos);
    }

    public static void clear(Level level, BlockPos pos) {
        Map<Long, Float> m = FIELDS.get(level.dimension());
        if (m != null)
            m.remove(pos.asLong());
    }

    /** The strongest field of a powered electromagnet on any of the six faces of {@code pos}: what a fitted sleeve rides on. */
    public static float touching(Level level, BlockPos pos) {
        Map<Long, Float> m = FIELDS.get(level.dimension());
        if (m == null || m.isEmpty())
            return 0f;
        float best = 0f;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            Float field = m.get(p.setWithOffset(pos, d).asLong());
            if (field != null && field > best)
                best = field;
        }
        return best;
    }

    /**
     * The field a thing at {@code (x, y, z)} feels from powered magnets below it, 0 to 1: the strongest magnet's field times how
     * near it is (1 touching, falling linearly to 0 at {@link #REACH}) times how nearly it is straight above (1 within half a
     * block sideways, 0 at a block and a half).
     */
    public static float below(Level level, double x, double y, double z) {
        Map<Long, Float> m = FIELDS.get(level.dimension());
        if (m == null || m.isEmpty())
            return 0f;
        float best = 0f;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int bx = (int) Math.floor(x - 1.5); bx <= (int) Math.floor(x + 1.5); bx++)
            for (int bz = (int) Math.floor(z - 1.5); bz <= (int) Math.floor(z + 1.5); bz++)
                for (int by = (int) Math.floor(y - REACH); by <= (int) Math.floor(y - 0.5); by++) {
                    Float field = m.get(p.set(bx, by, bz).asLong());
                    if (field == null)
                        continue;
                    double d = y - (by + 0.5);
                    double side = Math.hypot(x - (bx + 0.5), z - (bz + 0.5));
                    double near = Math.min(1.0, Math.max(0.0, (REACH - Math.max(d, TOUCH)) / (REACH - TOUCH)));
                    double over = Math.min(1.0, Math.max(0.0, 1.5 - side));
                    best = Math.max(best, (float) (field * near * Math.min(1.0, over)));
                }
        return best;
    }
}
