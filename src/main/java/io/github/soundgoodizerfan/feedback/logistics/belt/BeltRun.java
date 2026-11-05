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
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortReader;
import io.github.soundgoodizerfan.feedback.net.BeltRunPayload;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One straight run of belt cells and its cargo ledger: the {@code RotationNetwork}/{@code FluidNetwork}
 * idea for items. Cargo is never an item entity and never ticked one by one.
 *
 * <h2>The model</h2>
 * The run holds one scalar {@link #phase}, advanced once a tick by the belt speed. A free unit stores
 * {@code base} and sits at {@code base + phase}, so it costs nothing while it moves. Work happens at
 * <em>events</em>: a unit reaching a module's coordinate, a slope's foot, the exit, or the pinned unit
 * ahead. {@link #nextEvent} holds the phase of the earliest, so a tick with no event is O(1) however
 * much cargo rides. A blocked leader and the units queued behind it are <em>pinned</em> (a train) and
 * are released when the leader moves or is taken; pinned units whose hold is outside the run (a full
 * handler, a closed gate) are retried every {@link LogisticsTuning#BELT_RETRY_TICKS}.
 * <p>
 * Coordinates run from 0 (tail edge of the first cell) to {@code n} (exit edge of the last). Cargo is
 * ordered front (nearest the exit) first.
 */
public final class BeltRun {

    private static final double EPS = 1e-6, EXIT_MARGIN = 0.1;

    enum Action { CONTINUE, TAKEN, HOLD }

    final Level level;
    final BlockPos[] blocks;
    final BeltBlock.Slope[] slopes;
    final Direction facing;
    final int n;
    private final double[] evCoord;
    private final int[] evBlock;
    private final boolean[] evSlope;
    private final int pulls;
    final long id;

    double phase;
    float speed;
    private double nextEvent = Double.NEGATIVE_INFINITY;
    private boolean wake;
    private final ArrayList<Cargo> cargo = new ArrayList<>();
    private float massCache;
    private long counter;
    private boolean dirty = true, shapeDirty = true;
    private long lastSync = -1000;
    private float syncedSpeed = -1f;
    private float syncedLoad;
    private float slip = 1f;
    private boolean slipStale = true;

    BeltRun(Level level, List<BlockPos> blockList, Direction facing) {
        this.level = level;
        this.n = blockList.size();
        this.blocks = blockList.toArray(new BlockPos[0]);
        this.facing = facing;
        this.id = blocks[0].asLong();
        this.slopes = new BeltBlock.Slope[n];
        List<double[]> evs = new ArrayList<>();
        int pull = 0;
        for (int i = 0; i < n; i++) {
            BeltPart part = BeltPart.of(level.getBlockState(blocks[i]));
            slopes[i] = part == null ? BeltBlock.Slope.FLAT : part.beltSlope(level.getBlockState(blocks[i]));
            if (slopes[i] == BeltBlock.Slope.UP)
                evs.add(new double[]{i, 1, i});
            if (level.getBlockEntity(blocks[i]) instanceof BeltBlockEntity be && be.module() != null) {
                evs.add(new double[]{i + 0.5, 0, i});
                if (be.module() == BeltModule.FUNNEL_IN)
                    pull++;
            }
        }
        evs.sort((a, b) -> Double.compare(a[0], b[0]) != 0 ? Double.compare(a[0], b[0]) : Double.compare(b[1], a[1]));
        evCoord = new double[evs.size()];
        evBlock = new int[evs.size()];
        evSlope = new boolean[evs.size()];
        for (int i = 0; i < evs.size(); i++) {
            evCoord[i] = evs.get(i)[0];
            evSlope[i] = evs.get(i)[1] > 0;
            evBlock[i] = (int) evs.get(i)[2];
        }
        this.pulls = pull;
    }

    // --- queries --------------------------------------------------------------------------------

    public int length() {
        return n;
    }

    public BlockPos block(int i) {
        return blocks[i];
    }

    public float speed() {
        return speed;
    }

    public boolean isEmpty() {
        return cargo.isEmpty();
    }

    int indexOf(BlockPos pos) {
        for (int i = 0; i < n; i++)
            if (blocks[i].equals(pos))
                return i;
        return -1;
    }

    /** Mu of everything on the run. */
    public float mass() {
        return massCache;
    }

    private void remass() {
        float m = 0f;
        for (Cargo c : cargo)
            m += SortReader.unitMass(c.stack) * c.stack.getCount();
        massCache = m;
    }

    @Nullable
    private BeltHeadBlockEntity head() {
        return level.getBlockEntity(blocks[n - 1]) instanceof BeltHeadBlockEntity h ? h : null;
    }

    @Nullable
    BeltBlockEntity belt(int i) {
        return level.isLoaded(blocks[i]) && level.getBlockEntity(blocks[i]) instanceof BeltBlockEntity be ? be : null;
    }

    /** The gap two consecutive units keep, by their mass: a heavy load takes more belt. */
    double gap(Cargo lead, Cargo follow) {
        float m = (SortReader.unitMass(lead.stack) * lead.stack.getCount() + SortReader.unitMass(follow.stack) * follow.stack.getCount()) / 2f;
        return Math.min(LogisticsTuning.BELT_MAX_GAP, LogisticsTuning.BELT_MIN_GAP + LogisticsTuning.BELT_GAP_PER_MU * m);
    }

    void wake() {
        wake = true;
    }

    void markSlipStale() {
        slipStale = true;
    }

    // --- ledger ---------------------------------------------------------------------------------

    private int firstEventAfter(double at) {
        int i = 0;
        while (i < evCoord.length && evCoord[i] <= at)
            i++;
        return i;
    }

    /**
     * Puts {@code in} on the belt at coordinate {@code at}, as many items as mass, count and space allow.
     * Returns what was left over (the whole stack if nothing fits); with {@code simulate} nothing changes.
     */
    public ItemStack insert(ItemStack in, double at, boolean simulate) {
        if (in.isEmpty() || level.isClientSide)
            return in;
        at = Math.max(0.0, Math.min(at, n - EXIT_MARGIN));
        float unit = Math.max(0.01f, SortReader.unitMass(in));
        int take = Math.min(in.getCount(), Math.max(1, (int) (LogisticsTuning.BELT_UNIT_MASS_CAP / unit)));
        if (cargo.size() >= n * LogisticsTuning.BELT_UNITS_PER_BLOCK)
            return in;
        float room = n * LogisticsTuning.BELT_MASS_PER_BLOCK - massCache;
        if (room < unit)
            return in;
        take = Math.max(1, Math.min(take, (int) (room / unit)));
        ItemStack unitStack = in.copyWithCount(take);
        Cargo probe = new Cargo(unitStack, 0L);
        int k = 0;
        while (k < cargo.size() && cargo.get(k).pos(phase) >= at)
            k++;
        if (k > 0 && cargo.get(k - 1).pos(phase) - at < gap(cargo.get(k - 1), probe))
            return in;
        if (k < cargo.size() && at - cargo.get(k).pos(phase) < gap(probe, cargo.get(k)))
            return in;
        if (!simulate) {
            probe.seed = level.getGameTime() * 6364136223846793005L + counter++;
            probe.base = at - phase;
            probe.ev = firstEventAfter(at);
            cargo.add(k, probe);
            changed();
        }
        return in.copyWithCount(in.getCount() - take);
    }

    /** Seats a unit at a saved coordinate with no spacing check (rebuilds and reloads). */
    void seat(ItemStack stack, double at) {
        if (stack.isEmpty())
            return;
        at = Math.max(0.0, Math.min(at, n - EXIT_MARGIN));
        Cargo c = new Cargo(stack, level.getGameTime() * 6364136223846793005L + counter++);
        c.base = at - phase;
        c.ev = firstEventAfter(at);
        int k = 0;
        while (k < cargo.size() && cargo.get(k).pos(phase) >= at)
            k++;
        cargo.add(k, c);
        changed();
    }

    private void changed() {
        nextEvent = Double.NEGATIVE_INFINITY;
        dirty = true;
        remass();
    }

    /** Takes in a dropped item that came to rest on cell {@code pos}. */
    void adopt(BlockPos pos, ItemEntity item) {
        int i = indexOf(pos);
        if (i < 0 || item.isRemoved())
            return;
        Vec3 p = item.position();
        double along = (p.x - pos.getX()) * facing.getStepX() + (p.z - pos.getZ()) * facing.getStepZ();
        double frac = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? along : 1.0 + along;
        ItemStack rest = insert(item.getItem(), i + Math.max(0.0, Math.min(0.99, frac)), false);
        if (rest.isEmpty())
            item.discard();
        else
            item.setItem(rest);
    }

    /** The unit nearest {@code at} (within a block), removed and returned: the hand-clear of a jam. */
    ItemStack takeNear(Vec3 at) {
        Cargo best = null;
        double bestD = 1.0;
        for (Cargo c : cargo) {
            double d = worldPos(c.pos(phase)).distanceTo(at);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        if (best == null)
            return ItemStack.EMPTY;
        cargo.remove(best);
        changed();
        return best.stack;
    }

    /** The cargo over cell {@code i}: live stacks (a heat source may warm them in place) and their offsets. */
    List<ItemStack> stacksOver(int i) {
        List<ItemStack> out = new ArrayList<>();
        for (Cargo c : cargo) {
            double p = c.pos(phase);
            if (p >= i && p < i + 1)
                out.add(c.stack);
        }
        return out;
    }

    /** Offsets within cell {@code i} of the cargo over it, for saving. */
    List<Object[]> savedOver(int i) {
        List<Object[]> out = new ArrayList<>();
        for (Cargo c : cargo) {
            double p = c.pos(phase);
            if (p >= i && p < i + 1)
                out.add(new Object[]{c.stack, (float) (p - i)});
        }
        return out;
    }

    /** Every unit with the cell it is over and its offset, for a rebuild. */
    List<Object[]> drain() {
        List<Object[]> out = new ArrayList<>();
        for (Cargo c : cargo) {
            double p = c.pos(phase);
            int i = (int) Math.max(0, Math.min(n - 1, Math.floor(p)));
            out.add(new Object[]{blocks[i], (float) (p - i), c.stack});
        }
        cargo.clear();
        return out;
    }

    /** World position of coordinate {@code s}: along the run, climbing or falling with the slope. */
    Vec3 worldPos(double s) {
        int i = (int) Math.max(0, Math.min(n - 1, Math.floor(s)));
        double frac = s - i;
        double dy = switch (slopes[i]) {
            case UP -> frac;
            case DOWN -> -frac;
            default -> 0.0;
        };
        return new Vec3(blocks[i].getX() + 0.5 + facing.getStepX() * (frac - 0.5), blocks[i].getY() + 0.25 + dy,
                blocks[i].getZ() + 0.5 + facing.getStepZ() * (frac - 0.5));
    }

    // --- tick -----------------------------------------------------------------------------------

    void tick(long time) {
        BeltHeadBlockEntity head = head();
        if (slipStale)
            refreshSlip();
        speed = head == null ? 0f : Math.abs(head.getRpm().value()) * LogisticsTuning.BELT_BLOCKS_PER_TICK_PER_RPM * slip;
        if (cargo.isEmpty() && pulls == 0) {
            maybeSync(time, head);
            return;
        }
        phase += speed;
        boolean woken = wake;
        if (woken || phase >= nextEvent) {
            wake = false;
            settle();
        }
        if (woken || time % LogisticsTuning.BELT_RETRY_TICKS == 0)
            retry();
        if (pulls > 0 && speed > 0f && time % LogisticsTuning.BELT_PULL_TICKS == 0)
            pull();
        if (time % 20 == 0 && !cargo.isEmpty())
            wear();
        maybeSync(time, head);
    }

    private void refreshSlip() {
        slipStale = false;
        float worst = 1f;
        for (int i = 0; i < n; i++) {
            BeltBlockEntity be = belt(i);
            if (be != null)
                worst = Math.min(worst, be.getCondition());
        }
        float t = (worst - LogisticsTuning.BELT_CONDITION_FLOOR) / (1f - LogisticsTuning.BELT_CONDITION_FLOOR);
        slip = LogisticsTuning.BELT_SLIP_SPEED_FLOOR + (1f - LogisticsTuning.BELT_SLIP_SPEED_FLOOR) * Math.max(0f, Math.min(1f, t));
    }

    /** Hot cargo wears the band it lies on: misuse, never uptime. */
    private void wear() {
        boolean worn = false;
        for (Cargo c : cargo) {
            double p = c.pos(phase);
            int i = (int) Math.max(0, Math.min(n - 1, Math.floor(p)));
            BeltBlockEntity be = belt(i);
            if (be == null)
                continue;
            float over = ItemHeat.get(c.stack, level).value() - be.spec().maxTu();
            if (over > 0f) {
                be.wear(over * LogisticsTuning.BELT_HOT_WEAR_PER_TU);
                worn = true;
            }
        }
        if (worn)
            slipStale = true;
    }

    // --- events ---------------------------------------------------------------------------------

    private enum Kind { QUEUE, EVENT, EXIT }

    private double limitValue;
    private Kind limitKind;

    private void limit(Cargo c, @Nullable Cargo ahead) {
        limitValue = n - EXIT_MARGIN;
        limitKind = Kind.EXIT;
        if (c.ev < evCoord.length && evCoord[c.ev] < limitValue) {
            limitValue = evCoord[c.ev];
            limitKind = Kind.EVENT;
        }
        if (ahead != null && ahead.pinned()) {
            double q = ahead.pin - gap(ahead, c);
            if (q <= limitValue) {
                limitValue = q;
                limitKind = Kind.QUEUE;
            }
        }
    }

    private void settle() {
        for (int guard = 0; guard < 256; guard++) {
            boolean again = false;
            Cargo ahead = null;
            for (int i = 0; i < cargo.size(); i++) {
                Cargo c = cargo.get(i);
                if (c.reason == Cargo.Reason.QUEUE) {
                    if (ahead == null || !ahead.pinned()) {
                        c.free(c.pin, phase);
                        again = true;
                    }
                } else if (!c.pinned()) {
                    double p = c.base + phase;
                    limit(c, ahead);
                    if (p >= limitValue - EPS) {
                        double at = limitValue;
                        switch (limitKind) {
                            case QUEUE -> c.pinAt(at, Cargo.Reason.QUEUE);
                            case EVENT -> {
                                int e = c.ev;
                                Action a = act(c, e);
                                if (a == Action.TAKEN) {
                                    cargo.remove(i--);
                                    again = true;
                                    changed();
                                    continue;
                                } else if (a == Action.HOLD) {
                                    c.pinAt(evCoord[e], Cargo.Reason.EVENT);
                                } else {
                                    c.ev++;
                                    again = true;
                                }
                            }
                            case EXIT -> {
                                if (exit(c)) {
                                    cargo.remove(i--);
                                    again = true;
                                    changed();
                                    continue;
                                }
                                c.pinAt(n - EXIT_MARGIN, Cargo.Reason.EXIT);
                            }
                        }
                        dirty = true;
                        again = true;
                    }
                }
                ahead = c;
            }
            if (!again)
                break;
        }
        double best = Double.POSITIVE_INFINITY;
        Cargo ahead = null;
        for (Cargo c : cargo) {
            if (!c.pinned()) {
                limit(c, ahead);
                best = Math.min(best, limitValue - c.base);
            }
            ahead = c;
        }
        nextEvent = best;
        remass();
    }

    private Action act(Cargo c, int e) {
        BeltBlockEntity be = belt(evBlock[e]);
        if (evSlope[e]) {
            if (be == null)
                return Action.CONTINUE;
            float itemFriction = SortReader.read(c.stack, level).friction();
            boolean wet = level.isRainingAt(blocks[evBlock[e]].above()) || !level.getFluidState(blocks[evBlock[e]].above()).isEmpty();
            float grip = be.spec().friction() * itemFriction * (wet ? LogisticsTuning.WET_GRIP : 1f);
            return grip >= LogisticsTuning.INCLINE_TAN ? Action.CONTINUE : Action.HOLD;
        }
        if (be == null || be.module() == null)
            return Action.CONTINUE;
        return BeltModules.act(this, be, c, evBlock[e], evBlock[e] >= (head() != null ? n - 2 : n - 1));
    }

    private boolean exit(Cargo c) {
        BlockPos t = BeltTargets.nextOf(level, blocks[n - 1]);
        if (t == null || !level.isLoaded(t))
            return false;
        c.stack = BeltTargets.intoBelt(level, t, facing, c.stack, false);
        return c.stack.isEmpty();
    }

    private void retry() {
        boolean any = false;
        for (int i = 0; i < cargo.size(); i++) {
            Cargo c = cargo.get(i);
            if (c.reason == Cargo.Reason.EXIT) {
                if (exit(c)) {
                    cargo.remove(i--);
                    any = true;
                } else if (c.reason == Cargo.Reason.EXIT) {
                    c.pin = n - EXIT_MARGIN;
                }
            } else if (c.reason == Cargo.Reason.EVENT) {
                int e = c.ev;
                Action a = act(c, e);
                if (a == Action.TAKEN) {
                    cargo.remove(i--);
                    any = true;
                } else if (a == Action.CONTINUE) {
                    c.ev++;
                    c.free(c.pin, phase);
                    any = true;
                }
            }
        }
        if (any)
            changed();
    }

    /** Funnels facing a handler pull one unit each, if there is room at their cell. */
    private void pull() {
        for (int i = 0; i < n; i++) {
            BeltBlockEntity be = belt(i);
            if (be == null || be.module() != BeltModule.FUNNEL_IN)
                continue;
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, blocks[i].relative(be.face()), be.face().getOpposite());
            if (h == null)
                continue;
            for (int s = 0; s < h.getSlots(); s++) {
                ItemStack look = h.extractItem(s, 64, true);
                if (look.isEmpty())
                    continue;
                ItemStack rest = insert(look, i + 0.5, true);
                int took = look.getCount() - rest.getCount();
                if (took <= 0)
                    continue;
                ItemStack real = h.extractItem(s, took, false);
                if (!real.isEmpty())
                    insert(real, i + 0.5, false);
                break;
            }
        }
    }

    // --- sync -----------------------------------------------------------------------------------

    private void maybeSync(long time, @Nullable BeltHeadBlockEntity head) {
        float load = massCache;
        if (head != null && Math.abs(load - syncedLoad) > 1f + syncedLoad * 0.1f) {
            syncedLoad = load;
            if (head.getNetwork() != null)
                head.getNetwork().recalculate();
        }
        boolean speedMoved = Math.abs(speed - syncedSpeed) > 0.0002f;
        boolean periodic = !cargo.isEmpty() && time - lastSync >= 100;
        if (!(dirty && time - lastSync >= LogisticsTuning.BELT_SYNC_TICKS) && !(speedMoved && time - lastSync >= LogisticsTuning.BELT_SYNC_TICKS) && !periodic)
            return;
        if (!(level instanceof ServerLevel server))
            return;
        boolean withShape = shapeDirty || periodic;
        long[] shape = null;
        byte[] kinds = null;
        if (withShape) {
            shape = new long[n];
            kinds = new byte[n];
            for (int i = 0; i < n; i++) {
                shape[i] = blocks[i].asLong();
                kinds[i] = (byte) slopes[i].ordinal();
            }
        }
        List<BeltRunPayload.Entry> entries = new ArrayList<>(cargo.size());
        for (Cargo c : cargo)
            entries.add(new BeltRunPayload.Entry(c.stack.copy(), (float) c.pos(phase), c.pinned()));
        Vec3 mid = worldPos(n / 2.0);
        PacketDistributor.sendToPlayersNear(server, null, mid.x, mid.y, mid.z, 64 + n,
                new BeltRunPayload(id, facing.get3DDataValue(), shape, kinds, speed, server.getGameTime(), entries));
        lastSync = time;
        dirty = false;
        shapeDirty = false;
        syncedSpeed = speed;
    }

    /** Tells nearby clients the run is gone. */
    void retire() {
        if (level instanceof ServerLevel server) {
            Vec3 mid = worldPos(n / 2.0);
            PacketDistributor.sendToPlayersNear(server, null, mid.x, mid.y, mid.z, 64 + n,
                    new BeltRunPayload(id, facing.get3DDataValue(), new long[0], new byte[0], 0f, server.getGameTime(), List.of()));
        }
    }
}
