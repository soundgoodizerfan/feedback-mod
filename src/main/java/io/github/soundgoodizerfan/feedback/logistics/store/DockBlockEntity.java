// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackLinkedSet;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
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
public class DockBlockEntity extends BlockEntity {

    /** A retrieval asked for: what, and how many are still to fetch. */
    public static final class Request {
        public final ItemStack item;
        public int remaining;

        public Request(ItemStack item, int remaining) {
            this.item = item.copyWithCount(1);
            this.remaining = remaining;
        }
    }

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

    /** Everything on the aisle's loaded racks, one lot per distinct stack. */
    public List<Lot> stock() {
        Map<ItemStack, Lot> out = new Object2ObjectLinkedOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
        for (Aisle.Cell cell : aisle().racks()) {
            RackBlockEntity rack = rackAt(cell.pos());
            if (rack == null)
                continue;
            for (int i = 0; i < StoreTuning.BAYS_PER_CELL; i++)
                for (Lot lot : rack.bay(i).lots) {
                    Lot sum = out.get(lot.item);
                    if (sum == null)
                        out.put(lot.item, new Lot(lot.item, lot.count));
                    else
                        sum.count += lot.count;
                }
        }
        return new ArrayList<>(out.values());
    }

    public List<Request> requests() {
        return requests;
    }

    /** What holds the dock up, if anything. */
    public DockView.State state() {
        if (aisle().head() == null)
            return DockView.State.NO_HEAD;
        if (speed() <= 0f)
            return DockView.State.STILL;
        if (!requests.isEmpty() && outputRoom(requests.get(0).item) <= 0)
            return DockView.State.OUTPUT_FULL;
        return noRoom && !intake.isEmpty() ? DockView.State.NO_ROOM : DockView.State.READY;
    }

    /**
     * Queues a retrieval of up to {@code count} of {@code item}, never more than the aisle holds beyond what is already
     * asked for. False when the queue has no place left for it.
     */
    public boolean request(ItemStack item, int count) {
        if (level == null || level.isClientSide || count <= 0)
            return true;
        // What is riding home still counts as held: its request has not been marked down yet.
        int held = job != null && job.fetch && job.returning && ItemStack.isSameItemSameComponents(job.item, item) ? job.load : 0;
        for (Lot l : stock())
            if (l.matches(item))
                held += l.count;
        Request same = null;
        for (Request r : requests)
            if (ItemStack.isSameItemSameComponents(r.item, item)) {
                same = r;
                held -= r.remaining;
            }
        int n = Math.min(count, held);
        if (n <= 0)
            return true;
        if (same != null)
            same.remaining += n;
        else if (requests.size() < StoreTuning.MAX_REQUESTS)
            requests.add(new Request(item, n));
        else
            return false;
        fetchWait = 0;
        setChanged();
        return true;
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

    public void cancel(int index) {
        if (index >= 0 && index < requests.size()) {
            requests.remove(index);
            setChanged();
        }
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
                    begin(new Job(true, cell, i, r.item, n));
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
            begin(found);
            return;
        }
        noRoom = true;
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
        for (int pass = 0; pass < 2; pass++)
            for (Aisle.Cell cell : aisle().racks()) {
                RackBlockEntity rack = rackAt(cell.pos());
                if (rack == null)
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
            int n = Math.min(job.load, bay.room(job.item, rack.rating()));
            if (n <= 0)
                return;
            bay.add(job.item, n);
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
            if (ItemStack.isSameItemSameComponents(r.item, job.item)) {
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
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- persistence ------------------------------------------------------------------------

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
            asked.add(one);
        }
        tag.put("Requests", asked);
        if (job != null)
            tag.put("Job", job.save(registries));
    }

    /** Reads both the saved tag and the smaller one a client is sent (carriage and speed only). */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        job = tag.contains("Job") ? Job.load(tag.getCompound("Job"), registries) : null;
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
                requests.add(new Request(item, asked.getCompound(i).getInt("Count")));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        AisleChanges.bump();
    }
}
