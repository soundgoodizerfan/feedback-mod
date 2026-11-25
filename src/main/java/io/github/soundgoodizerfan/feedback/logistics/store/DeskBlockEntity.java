// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The catalogue desk: one index over every warehouse dock linked to it with the data connector. It needs the circuit its
 * recipe built in, which sets how many docks it reads, and a trickle of LV current. It does nothing until a screen asks:
 * it has no tick. A request is shared among the docks holding the stack, the soonest to deliver first, and each share
 * comes out of its own dock.
 */
public class DeskBlockEntity extends ElectricNode {

    /** A linked dock: where it is, how far, and the block entity, or null while its chunk is away. */
    public record Linked(BlockPos pos, int distance, @Nullable DockBlockEntity dock) {
    }

    private ItemStack circuit = ItemStack.EMPTY;
    private int orders;

    public DeskBlockEntity(BlockPos pos, BlockState state) {
        super(FStore.DESK_ENTITY.get(), pos, state);
    }

    /** Docks this desk reads: set by its circuit's speed. */
    public int dockCap() {
        float speed = Math.max(0f, Math.min(1f, Circuit.of(circuit).speed()));
        return StoreTuning.DESK_MIN_DOCKS + Math.round(speed * (StoreTuning.DESK_MAX_DOCKS - StoreTuning.DESK_MIN_DOCKS));
    }

    /** One link spare, so a desk linked up to its cap does not count as an overloaded end of its data wires. */
    @Override
    public int getMaxNodeLinks() {
        return dockCap() + 1;
    }

    /** Why the desk is dark, or null while it works. */
    @Nullable
    public StoreView.State down() {
        if (!Circuit.of(circuit).live())
            return StoreView.State.NO_CIRCUIT;
        // At least half its wanted work must arrive. The index takes no harm from a higher band: its draw is a trickle.
        return isLive() && ElectricRules.rate(getPacketEu(), getAmps(), getWantedAmps(), VoltageBand.LV) >= StoreTuning.DESK_MIN_FEED
                ? null : StoreView.State.NO_POWER;
    }

    /** Refuses a link past the cap; the spare in {@link #getMaxNodeLinks} is only for the wires' figure. */
    @Override
    public boolean canLinkTo(DataNode other) {
        return super.canLinkTo(other) && getNodeLinks().size() < dockCap();
    }

    /** Requests not yet delivered for {@code item}, over every dock listed. */
    private static int pending(List<DockBlockEntity> docks, ItemStack item) {
        int n = 0;
        for (DockBlockEntity d : docks)
            n += d.pending(item);
        return n;
    }

    /**
     * Each dock's stock with every rack counted once, by the first dock listed that reaches it: a rack row between two
     * rails, or a rail with a dock at each end, is one lot of goods however many carriages can get to it. Null for a dock
     * that is away.
     */
    static List<List<Lot>> stocks(List<Linked> docks) {
        Set<BlockPos> counted = new HashSet<>();
        List<List<Lot>> out = new ArrayList<>(docks.size());
        for (Linked l : docks)
            out.add(l.dock() == null ? null : l.dock().stockExcept(counted));
        return out;
    }

    /** The linked docks, nearest first, no more than the cap. A link to a loaded block that is no dock is passed over. */
    public List<Linked> docks() {
        List<Linked> out = new ArrayList<>();
        if (level == null)
            return out;
        pruneNodeLinks();
        for (DataNodeRef ref : getNodeLinks()) {
            BlockPos at = ref.pos();
            DockBlockEntity dock = null;
            if (level.isLoaded(at)) {
                if (!(level.getBlockEntity(at) instanceof DockBlockEntity d))
                    continue;
                dock = d;
            }
            out.add(new Linked(at, (int) Math.round(Math.sqrt(at.distSqr(worldPosition))), dock));
        }
        out.sort(Comparator.comparingInt(Linked::distance).thenComparingLong(l -> l.pos().asLong()));
        return out.size() > dockCap() ? new ArrayList<>(out.subList(0, dockCap())) : out;
    }

    /**
     * Shares {@code count} of {@code item} among the docks holding it, the soonest first. False when a dock that held some
     * had no place in its queue and the order is short for it.
     */
    public boolean order(ItemStack item, int count) {
        if (level == null || level.isClientSide || down() != null || count <= 0)
            return true;
        List<Linked> linked = docks();
        List<List<Lot>> stocks = stocks(linked);
        List<DockBlockEntity> all = new ArrayList<>();
        int held = 0;
        for (int i = 0; i < linked.size(); i++) {
            if (stocks.get(i) == null)
                continue;
            all.add(linked.get(i).dock());
            for (Lot lot : stocks.get(i))
                if (lot.matches(item))
                    held += lot.count;
        }
        // Never more than is there and unasked for, so a shared rack is not promised twice.
        count = Math.min(count, held - pending(all, item));
        if (count <= 0)
            return true;
        // Each dock's figure once: reading it walks the aisle.
        Map<DockBlockEntity, Float> soonest = new IdentityHashMap<>();
        for (DockBlockEntity d : all)
            soonest.put(d, d.estimate(item));
        List<DockBlockEntity> from = new ArrayList<>(all);
        from.sort(Comparator.comparing(soonest::get));
        int order = ++orders;
        setChanged();
        boolean full = false;
        for (DockBlockEntity dock : from) {
            if (count <= 0)
                break;
            int taken = dock.request(item, count, order);
            if (taken < 0)
                full = true;
            else
                count -= taken;
        }
        return !full || count <= 0;
    }

    // --- electric ---------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return Circuit.of(circuit).live() ? StoreTuning.DESK_AMPS : 0f;
    }

    // --- persistence ------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemStack built = input.get(FDataComponents.CIRCUIT_STACK.get());
        circuit = built == null ? ItemStack.EMPTY : built.copy();
        if (level != null && !level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!circuit.isEmpty())
            components.set(FDataComponents.CIRCUIT_STACK.get(), circuit.copy());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Circuit", circuit.saveOptional(registries));
        tag.putInt("Orders", orders);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        circuit = ItemStack.parseOptional(registries, tag.getCompound("Circuit"));
        orders = tag.getInt("Orders");
    }
}
