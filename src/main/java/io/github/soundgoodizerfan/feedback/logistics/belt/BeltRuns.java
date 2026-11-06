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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Per-level registry of belt runs, their rebuild and their tick; the {@code FluidNetworks} shape.
 * Runtime only: the belt blocks are the truth, and each saves the cargo over it, so a run rebuilds
 * from them on load. Changed cells are queued and a whole stretch is rebuilt once before the tick;
 * cargo is carried across a rebuild by the cell it is over.
 * <p>
 * A run is the longest straight chain in which each cell is the one before it feeds: same facing,
 * and {@link BeltPart#next} of the one lands on the other. A belt feeding across from the side or
 * round a corner ends its run and hands cargo to the other run's cell.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BeltRuns {

    private static final Map<LevelAccessor, BeltRuns> PER_LEVEL = new HashMap<>();

    /** Belt speed at a cell on the client (set by the client's run store); the server asks its own runs. */
    public static Function<BlockPos, Float> clientSpeed = p -> 0f;

    public static BeltRuns of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new BeltRuns(level));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /** Whether {@code from} is a cell touching {@code pos} (a belt's neighbours reach one block up or down). */
    public static boolean adjacent(BlockPos pos, BlockPos from) {
        return Math.abs(pos.getX() - from.getX()) <= 1 && Math.abs(pos.getY() - from.getY()) <= 1 && Math.abs(pos.getZ() - from.getZ()) <= 1;
    }

    private final Level level;
    private Set<BlockPos> dirty = new LinkedHashSet<>();
    private final Map<BlockPos, BeltRun> byBlock = new HashMap<>();
    private final Set<BeltRun> runs = new HashSet<>();

    private BeltRuns(Level level) {
        this.level = level;
    }

    public void queue(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    @org.jetbrains.annotations.Nullable
    public BeltRun runAt(BlockPos pos) {
        return byBlock.get(pos);
    }

    public float speedAt(BlockPos pos) {
        BeltRun run = byBlock.get(pos);
        return run == null ? 0f : run.speed;
    }

    public void adopt(BlockPos pos, ItemEntity item) {
        BeltRun run = byBlock.get(pos);
        if (run != null)
            run.adopt(pos, item);
    }

    /** Offers {@code stack} to the belt cell at {@code pos}, at {@code offset} into the cell. */
    public ItemStack insertAt(BlockPos pos, ItemStack stack, double offset, boolean simulate) {
        BeltRun run = byBlock.get(pos);
        return run == null ? stack : run.insert(stack, run.indexOf(pos) + offset, simulate);
    }

    /** A unit near {@code at} on the run over {@code pos}, taken off. */
    public ItemStack takeNear(BlockPos pos, Vec3 at) {
        BeltRun run = byBlock.get(pos);
        return run == null ? ItemStack.EMPTY : run.takeNear(at);
    }

    /** Something a held unit waited on changed (a gate opened, a handler emptied): look again next tick. */
    public void wake(BlockPos pos) {
        BeltRun run = byBlock.get(pos);
        if (run != null)
            run.wake();
    }

    public void slipChanged(BlockPos pos) {
        BeltRun run = byBlock.get(pos);
        if (run != null)
            run.markSlipStale();
    }

    List<ItemStack> stacksOver(BlockPos pos) {
        BeltRun run = byBlock.get(pos);
        return run == null ? List.of() : run.stacksOver(run.indexOf(pos));
    }

    List<Object[]> savedOver(BlockPos pos) {
        BeltRun run = byBlock.get(pos);
        return run == null ? List.of() : run.savedOver(run.indexOf(pos));
    }

    // --- rebuild --------------------------------------------------------------------------------

    private static BeltPart part(Level level, BlockPos pos) {
        return level.isLoaded(pos) ? BeltPart.of(level.getBlockState(pos)) : null;
    }

    private void flush() {
        if (dirty.isEmpty())
            return;
        Set<BlockPos> batch = dirty;
        dirty = new LinkedHashSet<>();

        Set<BlockPos> region = new HashSet<>();
        for (BlockPos p : batch)
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++)
                        region.add(p.offset(dx, dy, dz));

        Set<BeltRun> old = new HashSet<>();
        for (BlockPos p : region)
            if (byBlock.containsKey(p))
                old.add(byBlock.get(p));

        // Everything the old runs carried, remembered by the cell it was over.
        List<Object[]> carried = new ArrayList<>();
        Set<BlockPos> members = new LinkedHashSet<>();
        for (BeltRun run : old) {
            carried.addAll(run.drain());
            for (BlockPos b : run.blocks)
                members.add(b);
            runs.remove(run);
            run.retire();
        }
        for (BlockPos b : members)
            byBlock.remove(b);
        for (BlockPos p : region)
            members.add(p);
        members.removeIf(p -> part(level, p) == null);

        // Who feeds whom: straight before the rise, before the fall.
        Map<BlockPos, BlockPos> prev = new HashMap<>();
        for (BlockPos x : members) {
            BlockState xs = level.getBlockState(x);
            Direction f = BeltPart.of(xs).beltFacing(xs);
            BlockPos[] candidates = {x.relative(f.getOpposite()), x.relative(f.getOpposite()).below(), x.relative(f.getOpposite()).above()};
            for (BlockPos c : candidates) {
                if (!members.contains(c))
                    continue;
                BlockState cs = level.getBlockState(c);
                BeltPart cp = BeltPart.of(cs);
                if (cp.beltFacing(cs) == f && cp.next(c, cs).equals(x)) {
                    prev.put(x, c);
                    break;
                }
            }
        }
        Map<BlockPos, BlockPos> next = new HashMap<>();
        prev.forEach((x, p) -> next.put(p, x));

        List<BeltRun> built = new ArrayList<>();
        for (BlockPos x : members) {
            if (prev.containsKey(x))
                continue;
            List<BlockPos> chain = new ArrayList<>();
            for (BlockPos at = x; at != null && chain.size() < 4096; at = next.get(at))
                chain.add(at);
            BlockState xs = level.getBlockState(x);
            BeltRun run = new BeltRun(level, chain, BeltPart.of(xs).beltFacing(xs));
            built.add(run);
            runs.add(run);
            for (BlockPos b : chain)
                byBlock.put(b, run);
        }

        // Saved cargo of freshly loaded cells, then what the old runs carried.
        for (BeltRun run : built)
            for (int i = 0; i < run.n; i++)
                for (Object[] saved : CargoSave.take(level.getBlockEntity(run.blocks[i]))) {
                    run.seat((ItemStack) saved[0], i + (Float) saved[1]);
                    if (saved.length > 2 && (Long) saved[2] > 0L)
                        run.sat(level.getGameTime() - (Long) saved[2]);
                }
        for (Object[] c : carried) {
            BlockPos at = (BlockPos) c[0];
            BeltRun run = byBlock.get(at);
            if (run != null)
                run.seat((ItemStack) c[2], run.indexOf(at) + (Float) c[1]);
            else if (level.isLoaded(at))
                BeltTargets.spill(level, Vec3.atCenterOf(at), (ItemStack) c[2]);
        }
        for (BeltRun run : built)
            run.markSlipStale();
    }

    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).flush();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide)
            return;
        BeltRuns reg = of(event.getLevel());
        long time = event.getLevel().getGameTime();
        for (BeltRun run : new ArrayList<>(reg.runs))
            run.tick(time);
    }
}
