// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.tube;

import io.github.soundgoodizerfan.feedback.core.tube.TubeTuning;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.core.tube.Capsule;
import io.github.soundgoodizerfan.feedback.logistics.sort.Band;
import io.github.soundgoodizerfan.feedback.logistics.sort.Detector;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty;
import io.github.soundgoodizerfan.feedback.logistics.sort.Sorter;
import io.github.soundgoodizerfan.feedback.logistics.sort.Verdict;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The door of a tube run: loads items into capsules and unpacks capsules that arrive. Items go in
 * through the handler (a hopper, a belt end), are packed into one capsule when the buffer is full or
 * has stood idle, stamped with the station's address, and launched into the tube it faces. Arriving
 * capsules are emptied into an output buffer that is pushed on into neighbouring inventories; the
 * empty capsule goes to the carrier stock. A pod stocked here carries a player (empty hand to board).
 * <p>
 * The address is the one thing a station decides, once, at the door; the station knows nothing of
 * where it leads. It is a named colour set with a dye.
 */
public class TubeStationBlockEntity extends BlockEntity {

    /** 0-3 load buffer, 4-7 unloaded output, 8 empty capsules, 9 pods. */
    static final int IN = 0, OUT = 4, CARRIERS = 8, PODS = 9, SLOTS = 10;

    private final ItemStack[] slots = new ItemStack[SLOTS];
    /** Where a unit goes when it is not in the sort bands (or when no band is set). */
    @Nullable
    private DyeColor address;
    /** Where a unit that is in every band goes. */
    @Nullable
    private DyeColor matchAddress;
    /** Where a multi-stage station sends cargo it cannot place; a plain station sends that to {@link #address}. */
    @Nullable
    private DyeColor rejectAddress;
    /** Sort bands, all of which a unit must be in to match; empty means no sorting. */
    private final List<Band> rules = new ArrayList<>();
    private long lastInsert, nextLoadAt;

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slots[slot];
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack))
                return stack;
            ItemStack rest = merge(slot, stack, simulate);
            if (!simulate && rest.getCount() != stack.getCount() && slot < OUT)
                lastInsert = level == null ? 0 : level.getGameTime();
            return rest;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < OUT || slots[slot].isEmpty())
                return ItemStack.EMPTY;
            ItemStack out = slots[slot].copyWithCount(Math.min(amount, slots[slot].getCount()));
            if (!simulate) {
                slots[slot].shrink(out.getCount());
                if (slots[slot].isEmpty())
                    slots[slot] = ItemStack.EMPTY;
                setChanged();
            }
            return out;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == PODS ? 1 : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            boolean carrier = stack.getItem() instanceof CarrierItem;
            if (slot < OUT)
                return !carrier;
            if (slot == CARRIERS)
                return carrier && ((CarrierItem) stack.getItem()).kind() != CarrierItem.Kind.POD
                        && CarrierItem.address(stack).isEmpty() && CarrierItem.contents(stack).stream().allMatch(ItemStack::isEmpty);
            if (slot == PODS)
                return carrier && ((CarrierItem) stack.getItem()).kind() == CarrierItem.Kind.POD;
            return false;
        }
    };

    public TubeStationBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.TUBE_STATION.get(), pos, state);
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public IItemHandler getHandler() {
        return handler;
    }

    public Direction faces() {
        return getBlockState().getValue(TubeStationBlock.FACING);
    }

    public Optional<DyeColor> address() {
        return Optional.ofNullable(address);
    }

    public void setAddress(@Nullable DyeColor colour) {
        address = colour;
        setChanged();
    }

    public int stock(int slot) {
        return slots[slot].getCount();
    }

    ItemStack merge(int slot, ItemStack stack, boolean simulate) {
        ItemStack cur = slots[slot];
        int limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
        if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, stack))
            return stack;
        int moved = Math.min(limit - cur.getCount(), stack.getCount());
        if (moved <= 0)
            return stack;
        if (!simulate) {
            if (cur.isEmpty())
                slots[slot] = stack.copyWithCount(moved);
            else
                cur.grow(moved);
            setChanged();
        }
        return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    public boolean multiStage() {
        return getBlockState().getBlock() instanceof TubeStationBlock b && b.multiStage();
    }

    public int maxStages() {
        return multiStage() ? TubeTuning.SORT_STATION_STAGES : 1;
    }

    public List<Band> rules() {
        return rules;
    }

    public Optional<DyeColor> matchAddress() {
        return Optional.ofNullable(matchAddress);
    }

    public void setMatchAddress(@Nullable DyeColor colour) {
        matchAddress = colour;
        setChanged();
    }

    public Optional<DyeColor> rejectAddress() {
        return Optional.ofNullable(rejectAddress);
    }

    public void setRejectAddress(@Nullable DyeColor colour) {
        rejectAddress = colour;
        setChanged();
    }

    /** The gauge: sneaking adds a stage (or clears all when full); otherwise steps the last stage through the bands and then its property. */
    public void cycleRule(boolean add) {
        if (add) {
            if (rules.size() < maxStages())
                rules.add(Band.first(SortProperty.MAGNETIC));
            else
                rules.clear();
        } else if (rules.isEmpty())
            rules.add(Band.first(SortProperty.MAGNETIC));
        else {
            int idx = rules.size() - 1;
            Band last = rules.get(idx), n = last.next();
            if (n.ordinal() > last.ordinal())
                rules.set(idx, n);
            else {
                int pi = last.property().ordinal() + 1;
                if (pi >= SortProperty.values().length)
                    rules.remove(idx);
                else
                    rules.set(idx, Band.first(SortProperty.values()[pi]));
            }
        }
        setChanged();
    }

    private Detector detector(SortProperty p) {
        return switch (p) {
            case MAGNETIC -> multiStage() ? Detector.ELECTROMAGNET : Detector.MAGNET;
            case REFLECTIVITY -> Detector.OPTICAL;
            case DENSITY, SIZE -> Detector.WEIGHER;
            default -> Detector.PLAIN;
        };
    }

    /** The load time of a stage: a weigher is slowest, so a line is as slow as its slowest stage, not as long as its count. */
    private int stageTicks(Band b) {
        return switch (detector(b.property())) {
            case WEIGHER -> TubeTuning.SORT_STAGE_TICKS_WEIGHER;
            case OPTICAL -> TubeTuning.SORT_STAGE_TICKS_OPTICAL;
            default -> TubeTuning.SORT_STAGE_TICKS_MAGNETIC;
        };
    }

    /**
     * The address one unit is stamped with, once, at the door. Every stage must read it in its band
     * (each a {@link Sorter} verdict with its head's noise); a miss goes to the default address, and cargo
     * no material places goes to the reject address of a multi-stage station (gear that rejects cleanly)
     * or to the default address of a plain one.
     */
    private Optional<DyeColor> addressFor(ItemStack unit, int slot, long now) {
        if (rules.isEmpty())
            return address();
        for (Band b : rules) {
            long seed = worldPosition.asLong() * 31L + slot * 7919L + now + b.ordinal();
            Verdict v = Sorter.decide(b, unit, level, detector(b.property()), false, seed);
            if (v == Verdict.UNKNOWN)
                return multiStage() ? rejectAddress() : address();
            if (v == Verdict.MISS)
                return address();
        }
        return matchAddress();
    }

    public void tickServer() {
        if (level == null)
            return;
        long now = level.getGameTime();
        if (now % 8 == 0)
            pushOut();
        if (now % 4 == 0)
            tryLoad(now);
    }

    private void pushOut() {
        for (int i = OUT; i < CARRIERS; i++) {
            if (slots[i].isEmpty())
                continue;
            for (Direction d : Direction.values()) {
                if (d == faces())
                    continue;
                IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
                if (h == null)
                    continue;
                ItemStack rest = slots[i];
                for (int s = 0; s < h.getSlots() && !rest.isEmpty(); s++)
                    rest = h.insertItem(s, rest, false);
                slots[i] = rest;
                setChanged();
                if (rest.isEmpty())
                    break;
            }
        }
    }

    private void tryLoad(long now) {
        if (now < nextLoadAt || slots[CARRIERS].isEmpty()
                || !(level.getBlockEntity(worldPosition.relative(faces())) instanceof TubeBlockEntity tube)
                || tube.capsule() != null)
            return;
        boolean any = false, full = true;
        for (int i = IN; i < OUT; i++) {
            any |= !slots[i].isEmpty();
            full &= !slots[i].isEmpty();
        }
        if (!any || !(full || now - lastInsert >= TubeTuning.STATION_LOAD_IDLE_TICKS))
            return;
        // Group the first unit's address: every buffered unit that reads the same goes in one capsule.
        Optional<DyeColor> to = Optional.empty();
        boolean first = true;
        List<ItemStack> load = new ArrayList<>(TubeTuning.CARRIER_SLOTS);
        for (int i = IN; i < OUT; i++) {
            if (slots[i].isEmpty())
                continue;
            Optional<DyeColor> a = addressFor(slots[i], i, now);
            if (first) {
                to = a;
                first = false;
            }
            if (a.equals(to))
                load.add(slots[i]);
        }
        ItemStack carrier = slots[CARRIERS].copyWithCount(1);
        CarrierItem.setContents(carrier, load);
        CarrierItem.setAddress(carrier, to);
        if (tube.launch(carrier, faces().getOpposite(), null, now)) {
            slots[CARRIERS].shrink(1);
            if (slots[CARRIERS].isEmpty())
                slots[CARRIERS] = ItemStack.EMPTY;
            for (int i = IN; i < OUT; i++)
                if (load.contains(slots[i]))
                    slots[i] = ItemStack.EMPTY;
            int slowest = TubeTuning.SORT_STAGE_TICKS_MAGNETIC;
            for (Band b : rules)
                slowest = Math.max(slowest, stageTicks(b));
            nextLoadAt = now + slowest;
            setChanged();
        }
    }

    /** Boards {@code player} in the stocked pod and sends it into the tube. */
    public boolean board(ServerPlayer player) {
        if (slots[PODS].isEmpty() || !(level.getBlockEntity(worldPosition.relative(faces())) instanceof TubeBlockEntity tube)
                || tube.capsule() != null)
            return false;
        ItemStack pod = slots[PODS].copyWithCount(1);
        CarrierItem.setAddress(pod, address());
        if (!tube.launch(pod, faces().getOpposite(), player.getUUID(), level.getGameTime()))
            return false;
        slots[PODS] = ItemStack.EMPTY;
        setChanged();
        return true;
    }

    /** A capsule arrives from the tube. False (it waits in the tube) when the buffers cannot take it. */
    public boolean receive(Capsule c, long now) {
        ItemStack carrier = c.stack.copyWithCount(1);
        boolean pod = carrier.getItem() instanceof CarrierItem i && i.kind() == CarrierItem.Kind.POD;
        List<ItemStack> load = CarrierItem.contents(carrier);
        boolean loaded = load.stream().anyMatch(s -> !s.isEmpty());
        if (loaded && Arrays.stream(slots, OUT, CARRIERS).anyMatch(s -> !s.isEmpty()))
            return false;
        CarrierItem.setContents(carrier, List.of());
        CarrierItem.setAddress(carrier, Optional.empty());
        int slot = pod ? PODS : CARRIERS;
        if (!merge(slot, carrier, true).isEmpty())
            return false;
        merge(slot, carrier, false);
        for (int i = 0; i < TubeTuning.CARRIER_SLOTS; i++)
            slots[OUT + i] = load.get(i);
        if (c.rider != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(c.rider);
            if (p != null)
                p.connection.teleport(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                        p.getYRot(), p.getXRot());
        }
        setChanged();
        return true;
    }

    public void dropAll() {
        if (level == null)
            return;
        for (ItemStack s : slots)
            if (!s.isEmpty())
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, s));
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++)
            list.add(slots[i].isEmpty() ? new CompoundTag() : slots[i].save(registries));
        tag.put("Slots", list);
        if (address != null)
            tag.putInt("Address", address.getId());
        if (matchAddress != null)
            tag.putInt("Match", matchAddress.getId());
        if (rejectAddress != null)
            tag.putInt("Reject", rejectAddress.getId());
        tag.putIntArray("Rules", rules.stream().mapToInt(Band::ordinal).toArray());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag list = tag.getList("Slots", 10);
        for (int i = 0; i < SLOTS; i++)
            slots[i] = i < list.size() ? ItemStack.parse(registries, list.getCompound(i)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        address = tag.contains("Address") ? DyeColor.byId(tag.getInt("Address")) : null;
        matchAddress = tag.contains("Match") ? DyeColor.byId(tag.getInt("Match")) : null;
        rejectAddress = tag.contains("Reject") ? DyeColor.byId(tag.getInt("Reject")) : null;
        rules.clear();
        for (int o : tag.getIntArray("Rules"))
            rules.add(Band.byOrdinal(o));
    }
}
