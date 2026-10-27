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
package io.github.soundgoodizerfan.feedback.core.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Per-level registry, rebuild and tick of structures (the {@code FluidNetworks} pattern). Parts
 * report load and removal; changed positions are rebuilt together before anything ticks, and each
 * structure ticks once per level tick. Pattern structures also re-walk their pattern every 20
 * ticks, because a plain tagged block placed or broken never reports itself.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class Multiblocks {

    private static final int RECHECK_TICKS = 20;
    private static final Map<LevelAccessor, Multiblocks> PER_LEVEL = new HashMap<>();

    public static Multiblocks of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new Multiblocks());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /** Call from a part's {@code onLoad}. Server side only. */
    public static void load(Part part) {
        Level level = ((BlockEntity) part).getLevel();
        if (level != null && !level.isClientSide)
            of(level).queue(part.pos());
    }

    /** Call from a part's {@code setRemoved} (load or break alike; a reloaded chunk requeues its parts). */
    public static void remove(Part part) {
        Level level = ((BlockEntity) part).getLevel();
        if (level == null || level.isClientSide)
            return;
        Multiblocks m = of(level);
        if (part.structure() != null)
            m.stale.add(part.structure());
        m.queue(part.pos());
    }

    private Set<BlockPos> dirty = new LinkedHashSet<>();
    private final Set<Structure> stale = new LinkedHashSet<>();
    private final Set<Structure> structures = new LinkedHashSet<>();

    public void queue(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    @Nullable
    private static Part partAt(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof Part p
                && !((BlockEntity) p).isRemoved() ? p : null;
    }

    private void flush(Level level) {
        if (dirty.isEmpty() && stale.isEmpty())
            return;
        Set<BlockPos> batch = dirty;
        dirty = new LinkedHashSet<>();

        Set<Part> seeds = new LinkedHashSet<>();
        for (BlockPos pos : batch) {
            add(seeds, partAt(level, pos));
            for (Direction d : Direction.values())
                add(seeds, partAt(level, pos.relative(d)));
        }
        Set<Structure> old = new LinkedHashSet<>(stale);
        stale.clear();
        for (Part p : seeds)
            if (p.structure() != null)
                old.add(p.structure());
        for (Structure s : old) {
            structures.remove(s);
            for (Part p : s.members) {
                p.setStructure(null);
                if (partAt(level, p.pos()) == p)
                    seeds.add(p);
            }
        }

        List<Structure> built = new ArrayList<>();
        Set<Part> seen = new HashSet<>();
        for (Part seed : seeds)
            if (!(seed instanceof Controller) && seed.rule() != null && !seen.contains(seed)) {
                Structure s = StackAssembler.assemble(level, seed, pos -> partAt(level, pos), seen);
                for (Part p : s.members)
                    p.setStructure(s);
                built.add(s);
            }
        for (Part seed : seeds)
            if (seed instanceof Controller c) {
                Structure s = patternStructure(level, c);
                built.add(s);
            }
        // shortcut: every rebuilt structure merges the whole old set, right for the usual one-in-one-out; a split stack should divide state by size.
        for (Structure s : built) {
            s.merge(old);
            structures.add(s);
        }
    }

    private Structure patternStructure(Level level, Controller c) {
        PatternAssembler.Result r = PatternAssembler.assemble(level, c.pos(),
                MultiblockPatterns.get().find(c.pattern()));
        Structure s = c.createStructure();
        s.controller = c;
        s.formed = r.formed();
        s.flaws = r.flaws();
        s.members.add(c);
        c.setStructure(s);
        for (Part p : r.parts())
            if (p != c && p.structure() == null && p.rule() == null) {
                s.members.add(p);
                p.setStructure(s);
            }
        return s;
    }

    private static void add(Set<Part> set, @Nullable Part p) {
        if (p != null)
            set.add(p);
    }

    private void tickAll(Level level) {
        long time = level.getGameTime();
        for (Structure s : new ArrayList<>(structures)) {
            s.tick(level);
            Controller c = s.controller;
            if (c != null && (time + c.pos().hashCode()) % RECHECK_TICKS == 0 && changed(level, s, c)) {
                stale.add(s);
                queue(c.pos());
            }
        }
    }

    private static boolean changed(Level level, Structure s, Controller c) {
        PatternAssembler.Result r = PatternAssembler.assemble(level, c.pos(), MultiblockPatterns.get().find(c.pattern()));
        return r.formed() != s.formed || !r.flaws().equals(s.flaws);
    }

    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).flush(event.getLevel());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).tickAll(event.getLevel());
    }
}
