// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

/**
 * One end of a trade road between two villages. Goods set on the outgoing shelf leave with the next market day's convoy and the other depot's
 * outgoing goods arrive on the incoming shelf. The link is recorded by a link card, never found by search. A convoy needs a road leaving each end
 * (the same flood a Counter reaches by), is delayed by the distance, and loses goods to raiders on the way at a chance that watch posts at either end
 * lower. The transfer is kept in {@link DepotPost}, so it arrives whether or not either village is loaded and nothing is ever forced loaded; a
 * courier on a donkey leaves along the road only when a player is there to see it.
 */
public class DepotBlockEntity extends BlockEntity {

    public static final BlockIndex<DepotBlockEntity> INDEX = new BlockIndex<>();
    public static final TagKey<Block> WATCH_POST = TagKey.create(Registries.BLOCK, Feedback.id("watch_post"));
    public static final int SLOTS = 9;

    private final ItemStackHandler outgoing = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler incoming = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    @Nullable
    private GlobalPos link;
    private long lastDeparture = -1;

    public DepotBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.DEPOT_ENTITY.get(), pos, state);
    }

    public ItemStackHandler outgoing() {
        return outgoing;
    }

    public ItemStackHandler incoming() {
        return incoming;
    }

    @Nullable
    public GlobalPos link() {
        return link;
    }

    public void link(@Nullable GlobalPos other) {
        link = other;
        setChanged();
    }

    public GlobalPos self() {
        return GlobalPos.of(level.dimension(), worldPosition);
    }

    /** What a hopper, tube or belt sees: put goods into the outgoing shelf, take what arrived from the incoming one. */
    public IItemHandler port() {
        return new CombinedInvWrapper(outgoing, incoming) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot < SLOTS ? super.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot < SLOTS ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
            }
        };
    }

    // --- the road ---

    /** The tile beside the depot a road leaves from: the first horizontal neighbour with road under it. */
    @Nullable
    private RoadReach roadFlood() {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            RoadReach r = RoadReach.flood(level, worldPosition.relative(d));
            if (r.size() >= VillageTuning.FREIGHT_ROAD_MIN)
                return r;
        }
        return null;
    }

    private int watchPosts(ServerLevel sl, BlockPos at) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-VillageTuning.FREIGHT_POST_RADIUS, -3, -VillageTuning.FREIGHT_POST_RADIUS),
                at.offset(VillageTuning.FREIGHT_POST_RADIUS, 3, VillageTuning.FREIGHT_POST_RADIUS)))
            if (sl.isLoaded(p) && sl.getBlockState(p).is(WATCH_POST))
                n++;
        return Math.min(n, VillageTuning.FREIGHT_POST_MAX);
    }

    /** Sends the outgoing goods down the road, if it is market day, a road leaves, and the other end is in this dimension. */
    public void depart(ServerLevel sl, long day) {
        if (link == null || day == lastDeparture || !MarketDay.isMarketDay(day) || link.dimension() != sl.dimension())
            return;
        List<ItemStack> goods = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++)
            if (!outgoing.getStackInSlot(i).isEmpty())
                goods.add(outgoing.getStackInSlot(i));
        if (goods.isEmpty())
            return;
        RoadReach road = roadFlood();
        if (road == null)
            return;
        lastDeparture = day;
        double dist = Math.sqrt(worldPosition.distSqr(link.pos()));
        long due = sl.getGameTime() + Math.max(VillageTuning.FREIGHT_MIN_TICKS, (long) (dist * VillageTuning.FREIGHT_TICKS_PER_BLOCK));
        // Raiders on the road: each stack is its own roll, and watch posts at either end lower the chance.
        int posts = watchPosts(sl, worldPosition) + watchPosts(sl, link.pos());
        float loss = Math.max(VillageTuning.FREIGHT_LOSS_FLOOR, VillageTuning.FREIGHT_LOSS * (1f - VillageTuning.FREIGHT_POST_SHIELD * posts));
        List<ItemStack> carried = new ArrayList<>();
        for (ItemStack s : goods)
            if (sl.random.nextFloat() >= loss)
                carried.add(s.copy());
        for (int i = 0; i < SLOTS; i++)
            outgoing.setStackInSlot(i, ItemStack.EMPTY);
        DepotPost.get(sl.getServer()).send(link, carried, due);
        courier(sl, road);
        setChanged();
    }

    /** A donkey sets off along the road, but only where a player can see it; it is gone in half a minute. */
    private void courier(ServerLevel sl, RoadReach road) {
        if (sl.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 64, false) == null)
            return;
        Donkey donkey = EntityType.DONKEY.spawn(sl, worldPosition.above(), MobSpawnType.EVENT);
        BlockPos far = road.farthest(worldPosition);
        if (donkey == null || far == null)
            return;
        donkey.getNavigation().moveTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 1.0);
        Freight.courier(donkey, sl.getGameTime() + VillageTuning.COURIER_TICKS);
    }

    /** Takes in what has arrived. */
    private void collect(ServerLevel sl) {
        List<ItemStack> due = DepotPost.get(sl.getServer()).collect(self(), sl.getGameTime());
        List<ItemStack> back = new ArrayList<>();
        for (ItemStack s : due) {
            ItemStack left = ItemHandlerHelper.insertItem(incoming, s, false);
            if (!left.isEmpty())
                back.add(left);
        }
        // What does not fit goes back on the road for the next look.
        if (!back.isEmpty())
            DepotPost.get(sl.getServer()).send(self(), back, sl.getGameTime() + VillageTuning.FREIGHT_RETRY_TICKS);
        if (!due.isEmpty())
            setChanged();
    }

    public void tickServer() {
        if (level instanceof ServerLevel sl && sl.getGameTime() % VillageTuning.FREIGHT_COLLECT_TICKS == 0)
            collect(sl);
    }

    /** Parcels on the road to this depot, for the readout. */
    public int onTheRoad() {
        return level instanceof ServerLevel sl ? DepotPost.get(sl.getServer()).onTheRoad(self()) : 0;
    }

    public void dropAll() {
        if (level == null)
            return;
        for (ItemStackHandler h : new ItemStackHandler[]{outgoing, incoming})
            for (int i = 0; i < h.getSlots(); i++)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), h.getStackInSlot(i));
    }

    // --- lifecycle and saving ---

    @Override
    public void onLoad() {
        super.onLoad();
        INDEX.add(this);
    }

    @Override
    public void setRemoved() {
        INDEX.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        INDEX.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Outgoing", outgoing.serializeNBT(registries));
        tag.put("Incoming", incoming.serializeNBT(registries));
        tag.putLong("Last", lastDeparture);
        if (link != null) {
            tag.putString("LinkDim", link.dimension().location().toString());
            tag.put("LinkPos", NbtUtils.writeBlockPos(link.pos()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        outgoing.deserializeNBT(registries, tag.getCompound("Outgoing"));
        incoming.deserializeNBT(registries, tag.getCompound("Incoming"));
        lastDeparture = tag.contains("Last") ? tag.getLong("Last") : -1;
        link = tag.contains("LinkPos") ? NbtUtils.readBlockPos(tag, "LinkPos").map(p -> GlobalPos.of(
                net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(tag.getString("LinkDim"))), p)).orElse(null) : null;
    }
}
