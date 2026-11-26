// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackLinkedSet;

import java.util.EnumMap;

import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltModule;
import io.github.soundgoodizerfan.feedback.logistics.sort.Band;
import io.github.soundgoodizerfan.feedback.logistics.sort.Detector;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty;
import io.github.soundgoodizerfan.feedback.logistics.sort.Sorter;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltTargets;
import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * An aisle's end: cargo goes into the intake buffer through any face, retrievals collect in the output buffer (pushed out
 * of the front, or pulled), and the carriage's jobs are run here, one at a time. A retrieval starts only when the output
 * has room, so the carriage is never left holding one; storing goes on while retrievals wait. A dock with nothing in
 * its buffers, queue or carriage does no work.
 * <p>
 * Designed after reading Create's logistics network (MIT) and Applied Energistics 2 (LGPL-3.0); nothing copied. Taken:
 * stock listed as a union over separate stores keyed on item plus components, and stock kept on a broken store's item.
 * Rejected: any container joining the list, an instant distance-free grid, channels, capacity tiers and autocrafting.
 */
public class DockBlockEntity extends ElectricNode {

    /** Reader heads fitted on the sides and top. */
    private final Map<Direction, Head> heads = new EnumMap<>(Direction.class);
    /** Storing looks made: seeds the heads, so each load and each retry is read afresh. */
    private int reads;

    /** A retrieval asked for: what, how many are still to fetch, and the desk order it is a share of (0 for the dock's own). */
    public static final class Request {
        public final ItemStack item;
        public final int order;
        /** Drawn at random, so it names this request among every dock's. */
        public final int id;
        public int remaining;

        public Request(ItemStack item, int remaining, int order, int id) {
            this.item = item.copyWithCount(1);
            this.remaining = remaining;
            this.order = order;
            this.id = id;
        }
    }

    /** The name the dock was given before it was placed, or null. */
    private Component name;
    private List<Lot> counted;
    private int countedAisle, countedStock;

    private final List<ItemStack> intake = new ArrayList<>();
    private final List<ItemStack> output = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();
    private Job job;
    /** Set while the intake holds something no bay will take. */
    private boolean noRoom;
    private int storeWait, fetchWait;
    private float sentSpeed;
    private long aisleReadAt;
    /** Client: game time the carriage state arrived, and the speed it is run at. */
    private long clientSince;
    private float clientSpeed;

    private Aisle aisle;
    private int aisleRead;

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(FStore.DOCK_ENTITY.get(), pos, state);
    }

    // --- the aisle --------------------------------------------------------------------------

    public Aisle aisle() {
        if (aisle == null || aisleRead != AisleChanges.generation()
                || aisle.partial() && level.getGameTime() - aisleReadAt >= StoreTuning.PARTIAL_READ_TICKS) {
            aisleReadAt = level.getGameTime();
            aisle = Aisle.read(level, worldPosition, getBlockState().getValue(DockBlock.FACING).getOpposite());
            aisleRead = AisleChanges.generation();
        }
        return aisle;
    }

    @Nullable
    private ShuttleHeadBlockEntity head() {
        BlockPos at = aisle().head();
        return at != null && level.isLoaded(at) && level.getBlockEntity(at) instanceof ShuttleHeadBlockEntity h ? h : null;
    }

    /** Cells a tick the carriage runs at now; 0 with no head or a still shaft. */
    public float speed() {
        ShuttleHeadBlockEntity h = head();
        return h == null ? 0f : Math.abs(h.getRpm().value()) * StoreTuning.CELLS_PER_TICK_PER_RPM;
    }

    @Nullable
    private RackBlockEntity rackAt(BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RackBlockEntity r ? r : null;
    }

    /**
     * Everything on the aisle's loaded racks, one lot per distinct stack and set of recorded bands; counted again only
     * after something moved. Do not change it.
     */
    public List<Lot> stock() {
        aisle();
        if (counted == null || countedAisle != aisleRead || countedStock != AisleChanges.stockGeneration()) {
            countedAisle = aisleRead;
            countedStock = AisleChanges.stockGeneration();
            counted = count(Set.of());
        }
        return counted;
    }

    /** The stock of the racks not yet in {@code counted}, adding this aisle's to it. */
    List<Lot> stockExcept(Set<BlockPos> counted) {
        boolean shared = false;
        for (Aisle.Cell cell : aisle().racks())
            shared |= counted.contains(cell.pos());
        List<Lot> out = shared ? count(counted) : stock();
        for (Aisle.Cell cell : aisle().racks())
            counted.add(cell.pos());
        return out;
    }

    /** How many of {@code item} are asked for and not yet delivered. */
    int pending(ItemStack item) {
        int n = 0;
        for (Request r : requests)
            if (ItemStack.isSameItemSameComponents(r.item, item))
                n += r.remaining;
        return n;
    }

    @Override
    public boolean canLinkTo(DataNode other) {
        // A desk's own cap holds whichever end the link is started from.
        return super.canLinkTo(other) && (!(other instanceof DeskBlockEntity desk) || desk.canLinkTo(this));
    }

    // --- reader heads -----------------------------------------------------------------------

    public Map<Direction, Head> heads() {
        return heads;
    }

    /** Whether a head may sit on {@code face}: not the front (output) nor the back (the shuttle head's). */
    public boolean mounts(Direction face) {
        Direction front = getBlockState().getValue(DockBlock.FACING);
        return face != Direction.DOWN && face.getAxis() != front.getAxis();
    }

    void fit(Direction face, BeltModule module) {
        heads.put(face, new Head(module));
        headsChanged(true);
    }

    /** Wipes a fouled head, or with {@code pull} takes the head off and returns its module; null when nothing was done. */
    @Nullable
    Head tend(Direction face, boolean pull) {
        Head head = heads.get(face);
        if (head == null || !pull && !head.wipeable())
            return null;
        if (pull)
            heads.remove(face);
        else
            head.used = 0;
        headsChanged(pull);
        return head;
    }

    private void headsChanged(boolean wiring) {
        storeWait = 0;
        sync(speed());
        if (wiring && level != null && !level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
    }

    /** What the heads record of {@code stack} as it is taken up. */
    private int read(ItemStack stack) {
        if (heads.isEmpty())
            return 0;
        long seed = (reads * 0x9E3779B97F4A7C15L) ^ ItemStack.hashItemAndComponents(stack);
        // As a belt cell's module: at least half the wanted work has to arrive.
        boolean powered = isLive() && ElectricRules.rate(getPacketEu(), getAmps(), getWantedAmps(), ratedBand()) >= StoreTuning.DESK_MIN_FEED;
        // A head reads one piece, however many wait with it.
        ItemStack piece = stack.copyWithCount(1);
        int marks = 0;
        for (SortProperty p : SortProperty.values()) {
            // Of several heads on one property the quietest reads, and each of the others cuts its chance of being wrong.
            Head best = null;
            int checking = 0;
            for (Head head : heads.values()) {
                if (head.module.dockProperty() != p || head.fouled() || head.module.powered && !powered)
                    continue;
                checking++;
                if (best == null || best.dirty() && !head.dirty()
                        || best.dirty() == head.dirty() && detector(head).noise < detector(best).noise)
                    best = head;
            }
            if (best == null)
                continue;
            Band band = Sorter.perceive(p, piece, level, detector(best), best.dirty(), seed + p.ordinal(), 1f / checking);
            if (band != null)
                marks |= Marks.bit(band);
        }
        return marks;
    }

    private static Detector detector(Head head) {
        return head.module.detector == null ? Detector.PLAIN : head.module.detector;
    }

    // --- electric ---------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        float amps = 0f;
        for (Head head : heads.values())
            amps += head.wantedAmps();
        return amps;
    }

    @Override
    public boolean conducting() {
        return wantedAmps() > 0f;
    }

    private List<Lot> count(Set<BlockPos> skip) {
        // Goods recorded differently stay apart, so a property search never mixes one bay's readings with another's.
        Map<ItemStack, List<Lot>> out = new Object2ObjectLinkedOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
        for (Aisle.Cell cell : aisle().racks()) {
            RackBlockEntity rack = rackAt(cell.pos());
            if (rack == null || skip.contains(cell.pos()))
                continue;
            for (int i = 0; i < StoreTuning.BAYS_PER_CELL; i++)
                for (Lot lot : rack.bay(i).lots) {
                    List<Lot> same = out.computeIfAbsent(lot.item, k -> new ArrayList<>(1));
                    Lot sum = null;
                    for (Lot have : same)
                        if (have.marks == lot.marks)
                            sum = have;
                    if (sum == null) {
                        sum = new Lot(lot.item, 0);
                        sum.marks = lot.marks;
                        same.add(sum);
                    }
                    sum.count += lot.count;
                }
        }
        List<Lot> all = new ArrayList<>();
        out.values().forEach(all::addAll);
        return all;
    }

    public List<Request> requests() {
        return requests;
    }

    /** What holds the dock up, if anything. */
    public StoreView.State state() {
        if (aisle().head() == null)
            return StoreView.State.NO_HEAD;
        if (speed() <= 0f)
            return StoreView.State.STILL;
        if (!requests.isEmpty() && outputRoom(requests.get(0).item) <= 0)
            return StoreView.State.OUTPUT_FULL;
        return noRoom && !intake.isEmpty() ? StoreView.State.NO_ROOM : StoreView.State.READY;
    }

    /** This dock as a screen lists it. */
    StoreView.Source source(Component called) {
        Aisle a = aisle();
        float speed = speed();
        float far = a.racks().isEmpty() ? 0f : a.racks().get(a.racks().size() - 1).distance();
        return new StoreView.Source(called, state(), a.racks().size(), speed <= 0f ? 0 : Math.round(2f * far / speed));
    }

    @Nullable
    public Component customName() {
        return name;
    }

    /** Names the dock; blank takes the name off. */
    void rename(String to) {
        String clean = net.minecraft.util.StringUtil.filterText(to).trim();
        if (clean.length() > StoreTuning.DOCK_NAME_MAX)
            clean = clean.substring(0, StoreTuning.DOCK_NAME_MAX);
        name = clean.isEmpty() ? null : Component.literal(clean);
        setChanged();
    }

    /**
     * Roughly how soon this dock could begin handing out {@code item}: the trips ahead of it and the run to the nearest bay
     * holding it, in ticks. A dock that cannot deliver now ranks after every one that can.
     */
    float estimate(ItemStack item) {
        float speed = speed();
        float nearest = Float.MAX_VALUE;
        for (Aisle.Cell cell : aisle().racks()) {
            RackBlockEntity rack = rackAt(cell.pos());
            if (rack == null)
                continue;
            for (int i = 0; i < StoreTuning.BAYS_PER_CELL && nearest == Float.MAX_VALUE; i++)
                if (rack.bay(i).find(item) != null)
                    nearest = cell.distance();
            if (nearest != Float.MAX_VALUE)
                break;
        }
        if (speed <= 0f || nearest == Float.MAX_VALUE || outputRoom(item) <= 0)
            return Float.MAX_VALUE;
        int ahead = requests.size() + (job == null ? 0 : 1);
        float far = aisle().racks().get(aisle().racks().size() - 1).distance();
        return (ahead * far + nearest) * 2f / speed;
    }

    /**
     * Queues a retrieval of up to {@code count} of {@code item} for desk order {@code order} (0 for the dock's own), never
     * more than the aisle holds beyond what is already asked for. How many were queued, or -1 when there were some to
     * give and the queue had no place left.
     */
    public int request(ItemStack item, int count, int order) {
        if (level == null || level.isClientSide || count <= 0)
            return 0;
        // What is riding home still counts as held: its request has not been marked down yet.
        int held = job != null && job.fetch && job.returning && ItemStack.isSameItemSameComponents(job.item, item) ? job.load : 0;
        for (Lot l : stock())
            if (l.matches(item))
                held += l.count;
        Request same = null;
        for (Request r : requests)
            if (ItemStack.isSameItemSameComponents(r.item, item)) {
                if (r.order == order)
                    same = r;
                held -= r.remaining;
            }
        int n = Math.min(count, held);
        if (n <= 0)
            return 0;
        if (same != null)
            same.remaining += n;
        else if (requests.size() < StoreTuning.MAX_REQUESTS)
            requests.add(new Request(item, n, order, level.random.nextInt(Integer.MAX_VALUE - 1) + 1));
        else
            return -1;
        fetchWait = 0;
        setChanged();
        return n;
    }

    /** Hands everything waiting in the intake back to {@code player}: the way out for cargo no bay takes. */
    boolean returnIntake(Player player) {
        if (intake.isEmpty())
            return false;
        for (ItemStack s : intake)
            if (!player.getInventory().add(s))
                player.drop(s, false);
        intake.clear();
        noRoom = false;
        setChanged();
        return true;
    }

    /** Drops the request of that id, if it is here. */
    public void cancel(int id) {
        if (requests.removeIf(r -> r.id == id))
            setChanged();
    }

    /** Drops requests for goods no rack of a wholly loaded aisle holds (another dock took them from a shared rack, or the rack went). */
    private void forgetGone() {
        if (aisle().partial())
            return;
        for (Aisle.Cell cell : aisle().racks())
            if (rackAt(cell.pos()) == null)
                return;
        List<Lot> held = stock();
        if (requests.removeIf(r -> held.stream().noneMatch(l -> l.matches(r.item))))
            setChanged();
    }

    // --- the tick ---------------------------------------------------------------------------

    public void tickServer() {
        if (level == null || job == null && intake.isEmpty() && requests.isEmpty() && output.isEmpty())
            return;
        if (!output.isEmpty() && level.getGameTime() % StoreTuning.PUSH_TICKS == 0)
            push();
        if (job == null) {
            if (aisle().head() == null || !startFetch())
                startStore();
            return;
        }
        // A head reloaded or replaced mid-trip is told again what it carries; no change costs nothing.
        carried();
        float speed = speed();
        if (Math.abs(speed - sentSpeed) > 0.05f * Math.max(speed, sentSpeed))
            sync(speed);
        if (speed <= 0f)
            return;
        job.progress += speed;
        setChanged();
        if (job.progress < job.cell.distance())
            return;
        if (job.returning) {
            comeHome();
            job = null;
        } else {
            arrive();
            job.returning = true;
            job.progress = 0f;
        }
        carried();
        sync(speed);
    }

    private boolean startFetch() {
        if (requests.isEmpty())
            return false;
        if (fetchWait > 0) {
            fetchWait--;
            return false;
        }
        forgetGone();
        for (Request r : requests) {
            float unit = Goods.unitMass(r.item);
            int room = outputRoom(r.item);
            if (room <= 0)
                continue;
            for (Aisle.Cell cell : aisle().racks()) {
                RackBlockEntity rack = rackAt(cell.pos());
                if (rack == null)
                    continue;
                for (int i = 0; i < StoreTuning.BAYS_PER_CELL; i++) {
                    Lot lot = rack.bay(i).find(r.item);
                    if (lot == null)
                        continue;
                    int n = Math.min(Math.min(r.remaining, lot.count), Math.min(room, fits(StoreTuning.SHUTTLE_MASS_MU, unit)));
                    Job next = new Job(true, cell, i, r.item, n);
                    next.request = r.id;
                    begin(next);
                    return true;
                }
            }
        }
        // Nothing can be brought now (output full, or the stock is in an unloaded rack): look again shortly.
        fetchWait = StoreTuning.RETRY_TICKS;
        return false;
    }

    private void startStore() {
        if (intake.isEmpty() || aisle().head() == null)
            return;
        if (storeWait > 0) {
            storeWait--;
            return;
        }
        for (int i = 0; i < intake.size(); i++) {
            ItemStack stack = intake.get(i);
            if (seenBefore(i))
                continue;
            Job found = findBay(stack);
            if (found == null)
                continue;
            noRoom = false;
            drain(intake, found.item, found.load);
            reads++;
            for (Head head : heads.values())
                head.used++;
            begin(found);
            return;
        }
        noRoom = true;
        // The next look reads the goods afresh, so one misread no rack accepts cannot hold them for good.
        reads++;
        storeWait = StoreTuning.RETRY_TICKS;
    }

    /** Whether an earlier intake stack is like the one at {@code at}, so it has been looked for already. */
    private boolean seenBefore(int at) {
        for (int i = 0; i < at; i++)
            if (ItemStack.isSameItemSameComponents(intake.get(i), intake.get(at)))
                return true;
        return false;
    }

    /** A storing job for as much of {@code stack} as one trip takes: to a bay already holding it, else the nearest with room. */
    @Nullable
    private Job findBay(ItemStack stack) {
        // The trip takes this stack and every other like it in the intake.
        int waiting = 0;
        for (ItemStack s : intake)
            if (ItemStack.isSameItemSameComponents(s, stack))
                waiting += s.getCount();
        int marks = read(stack);
        // A bay already holding it, then the nearest on a rack filtered for it, then the nearest unfiltered.
        for (int pass = 0; pass < 3; pass++)
            for (Aisle.Cell cell : aisle().racks()) {
                RackBlockEntity rack = rackAt(cell.pos());
                if (rack == null || !rack.takes(marks) || pass == 1 && rack.filter() == null || pass == 2 && rack.filter() != null)
                    continue;
                for (int i = 0; i < StoreTuning.BAYS_PER_CELL; i++) {
                    Bay bay = rack.bay(i);
                    if (pass == 0 && bay.find(stack) == null)
                        continue;
                    int n = Math.min(bay.room(stack, rack.rating()),
                            Math.min(waiting, fits(StoreTuning.SHUTTLE_MASS_MU, Goods.unitMass(stack))));
                    if (n <= 0)
                        continue;
                    Job j = new Job(false, cell, i, stack, n);
                    j.load = n;
                    j.marks = marks;
                    return j;
                }
            }
        return null;
    }

    private void begin(Job next) {
        job = next;
        carried();
        level.playSound(null, worldPosition, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.15f, 1.6f);
        setChanged();
        sync(speed());
    }

    /** The carriage is at the bay. A rack that has gone, unloaded or filled meanwhile leaves the load as it was. */
    private void arrive() {
        RackBlockEntity rack = rackAt(job.cell.pos());
        if (rack == null)
            return;
        Bay bay = rack.bay(job.bay);
        if (job.fetch) {
            job.load = bay.take(job.item, job.planned);
            if (job.load > 0)
                rack.changed();
        } else {
            int n = rack.takes(job.marks) ? Math.min(job.load, bay.room(job.item, rack.rating())) : 0;
            if (n <= 0)
                return;
            bay.add(job.item, n, job.marks);
            job.load -= n;
            float over = ItemHeat.get(job.item, level).value() - rack.serviceTu();
            if (over > 0f)
                rack.scorch(over);
            rack.changed();
        }
        level.playSound(null, job.cell.pos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 0.7f);
    }

    private void comeHome() {
        if (job.load <= 0)
            return;
        if (!job.fetch) {
            add(intake, job.item, job.load);
            return;
        }
        add(output, job.item, job.load);
        for (int i = 0; i < requests.size(); i++) {
            Request r = requests.get(i);
            // Charged only to the request it was fetched for; a cancelled one's cargo is charged to nothing.
            if (r.id == job.request) {
                r.remaining -= job.load;
                if (r.remaining <= 0)
                    requests.remove(i);
                break;
            }
        }
        level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.7f, 1.1f);
    }

    private void carried() {
        ShuttleHeadBlockEntity h = head();
        if (h != null)
            h.carry(job == null ? 0f : job.loadMass());
    }

    private void push() {
        Direction front = getBlockState().getValue(DockBlock.FACING);
        ItemStack first = output.get(0);
        ItemStack left = BeltTargets.deliver(level, worldPosition.relative(front), front.getOpposite(), first.copy(), false, false);
        if (left.getCount() == first.getCount())
            return;
        if (left.isEmpty())
            output.remove(0);
        else
            output.set(0, left);
        setChanged();
    }

    // --- buffers ----------------------------------------------------------------------------

    private static int fits(float mass, float unit) {
        return unit <= 0f ? Integer.MAX_VALUE : Math.max(1, (int) (mass / unit + 1e-4f));
    }

    private static float massOf(List<ItemStack> buffer) {
        float m = 0f;
        for (ItemStack s : buffer)
            m += Goods.unitMass(s) * s.getCount();
        return m;
    }

    /** How many of {@code item} a buffer still takes; an empty buffer always takes one, however heavy. */
    private static int roomIn(List<ItemStack> buffer, ItemStack item) {
        float unit = Goods.unitMass(item);
        if (unit <= 0f)
            return Integer.MAX_VALUE;
        int n = Math.max(0, (int) ((StoreTuning.DOCK_BUFFER_MU - massOf(buffer)) / unit + 1e-4f));
        return buffer.isEmpty() ? Math.max(1, n) : n;
    }

    private int outputRoom(ItemStack item) {
        return roomIn(output, item);
    }

    private static void drain(List<ItemStack> buffer, ItemStack item, int count) {
        for (int i = 0; i < buffer.size() && count > 0; i++) {
            ItemStack s = buffer.get(i);
            if (!ItemStack.isSameItemSameComponents(s, item))
                continue;
            int n = Math.min(count, s.getCount());
            s.shrink(n);
            count -= n;
            if (s.isEmpty())
                buffer.remove(i--);
        }
    }

    private static void add(List<ItemStack> buffer, ItemStack item, int count) {
        for (ItemStack s : buffer) {
            if (count <= 0)
                return;
            if (ItemStack.isSameItemSameComponents(s, item)) {
                int n = Math.min(count, s.getMaxStackSize() - s.getCount());
                s.grow(n);
                count -= n;
            }
        }
        while (count > 0) {
            int n = Math.min(count, item.getMaxStackSize());
            buffer.add(item.copyWithCount(n));
            count -= n;
        }
    }

    /** The face every side presents: the output stacks to pull, and one last slot that takes cargo into the intake. */
    public IItemHandler handler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return output.size() + 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot >= 0 && slot < output.size() ? output.get(slot) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                // A single piece heavier than a trip (a loaded rack) can never be carried, so it is not taken in.
                if (stack.isEmpty() || slot != output.size() || Goods.unitMass(stack) > StoreTuning.SHUTTLE_MASS_MU)
                    return stack;
                int n = Math.min(stack.getCount(), roomIn(intake, stack));
                if (n <= 0)
                    return stack;
                if (!simulate) {
                    // A trickle is let gather for a moment so the carriage does not run for each piece.
                    if (intake.isEmpty())
                        storeWait = StoreTuning.SETTLE_TICKS;
                    add(intake, stack, n);
                    if (massOf(intake) >= StoreTuning.DOCK_BUFFER_MU / 2f)
                        storeWait = 0;
                    setChanged();
                }
                return stack.copyWithCount(stack.getCount() - n);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < 0 || slot >= output.size() || amount <= 0)
                    return ItemStack.EMPTY;
                ItemStack in = output.get(slot);
                int n = Math.min(amount, in.getCount());
                ItemStack out = in.copyWithCount(n);
                if (!simulate) {
                    in.shrink(n);
                    if (in.isEmpty())
                        output.remove(slot);
                    setChanged();
                }
                return out;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == output.size();
            }
        };
    }

    /** Everything in the buffers and on the carriage falls out when the dock is broken. */
    void spill() {
        for (ItemStack s : intake)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        for (ItemStack s : output)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        if (job != null && job.load > 0) {
            List<ItemStack> held = new ArrayList<>();
            add(held, job.item, job.load);
            for (ItemStack s : held)
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        }
        intake.clear();
        output.clear();
        job = null;
        carried();
    }

    // --- the carriage on the client ---------------------------------------------------------

    @Nullable
    public Job job() {
        return job;
    }

    /** Cells run on the present leg, carried forward from the last word of the server. */
    public float clientProgress(float partialTick) {
        if (job == null || level == null)
            return 0f;
        float p = job.progress + clientSpeed * (level.getGameTime() - clientSince + partialTick);
        return Math.min(p, job.cell.distance());
    }

    private void sync(float speed) {
        sentSpeed = speed;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (job != null)
            tag.put("Job", job.save(registries));
        tag.putFloat("Speed", level == null ? 0f : speed());
        writeHeads(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- persistence ------------------------------------------------------------------------

    private void writeHeads(CompoundTag tag) {
        CompoundTag fitted = new CompoundTag();
        heads.forEach((face, head) -> fitted.put(face.getName(), head.save()));
        tag.put("Heads", fitted);
    }

    private static ListTag write(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack s : stacks)
            list.add(s.save(registries));
        return list;
    }

    private static void read(List<ItemStack> into, ListTag list, HolderLookup.Provider registries) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!s.isEmpty())
                into.add(s);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Intake", write(intake, registries));
        tag.put("Output", write(output, registries));
        ListTag asked = new ListTag();
        for (Request r : requests) {
            CompoundTag one = new CompoundTag();
            one.put("Item", r.item.save(registries));
            one.putInt("Count", r.remaining);
            one.putInt("Order", r.order);
            one.putInt("Id", r.id);
            asked.add(one);
        }
        tag.put("Requests", asked);
        writeHeads(tag);
        tag.putInt("Reads", reads);
        if (job != null)
            tag.put("Job", job.save(registries));
        if (name != null)
            tag.putString("CustomName", Component.Serializer.toJson(name, registries));
    }

    /** Reads both the saved tag and the smaller one a client is sent (carriage and speed only). */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        job = tag.contains("Job") ? Job.load(tag.getCompound("Job"), registries) : null;
        heads.clear();
        CompoundTag fitted = tag.getCompound("Heads");
        for (Direction d : Direction.values()) {
            Head head = fitted.contains(d.getName()) ? Head.load(fitted.getCompound(d.getName())) : null;
            if (head != null)
                heads.put(d, head);
        }
        reads = tag.getInt("Reads");
        if (tag.contains("Speed")) {
            clientSpeed = tag.getFloat("Speed");
            clientSince = level == null ? 0L : level.getGameTime();
            return;
        }
        read(intake, tag.getList("Intake", Tag.TAG_COMPOUND), registries);
        read(output, tag.getList("Output", Tag.TAG_COMPOUND), registries);
        requests.clear();
        ListTag asked = tag.getList("Requests", Tag.TAG_COMPOUND);
        for (int i = 0; i < asked.size(); i++) {
            ItemStack item = ItemStack.parseOptional(registries, asked.getCompound(i).getCompound("Item"));
            if (!item.isEmpty())
                requests.add(new Request(item, asked.getCompound(i).getInt("Count"), asked.getCompound(i).getInt("Order"),
                        asked.getCompound(i).getInt("Id")));
        }
        name = tag.contains("CustomName") ? Component.Serializer.fromJson(tag.getString("CustomName"), registries) : null;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        name = input.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, name);
    }


    @Override
    public void onLoad() {
        super.onLoad();
        AisleChanges.bump();
    }
}
