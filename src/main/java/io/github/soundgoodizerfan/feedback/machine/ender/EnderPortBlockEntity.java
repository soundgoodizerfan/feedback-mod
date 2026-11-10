// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.ender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.tube.CarrierItem;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * One end of a bonded pair. An item put into the port crosses to the partner's output slot when the
 * pair's per-minute mass allowance covers it, the tank holds enough liquid teleportant for its mass
 * and distance, both ends are loaded and neither is shielded. Allowance refills lazily from the game
 * clock (no per-tick transfer). Items that hold fluid, and anything tagged {@code shields_pairing} (the
 * shulker boxes), never cross; a shulker box against the port seals it.
 * <p>
 * Only an anchor upgrade keeps the port's chunk forced; otherwise a pair pauses while an end is unloaded,
 * because the live-ports registry holds only ports whose chunk is loaded.
 */
public class EnderPortBlockEntity extends BlockEntity {

    public static final String PAIR_KEY = "Pair";
    public static final TagKey<Fluid> TELEPORTANT = TagKey.create(Registries.FLUID, Feedback.id("teleportant"));
    public static final TagKey<Item> SHIELDS_PAIRING = TagKey.create(Registries.ITEM, Feedback.id("shields_pairing"));

    /** Ports whose chunk is loaded, by pair id. */
    private static final Map<UUID, Set<EnderPortBlockEntity>> LIVE = new HashMap<>();

    @Nullable
    private UUID pair;
    private ItemStack in = ItemStack.EMPTY, out = ItemStack.EMPTY;
    private boolean anchored;
    private float allowance = FTuning.PAIR_CAP_MU_PER_MIN;
    private long allowanceAt;
    private boolean unloaded;
    /** Game time until which the link counts as running, because it just carried something: running dry inside this window destabilises it. */
    private long liveUntil;

    private final FluidTank tank = new FluidTank(FTuning.PAIR_TANK_MB, fs -> fs.is(TELEPORTANT)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? in : out;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || !in.isEmpty() || !crosses(stack))
                return stack;
            if (!simulate) {
                in = stack.copy();
                setChanged();
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1 || out.isEmpty())
                return ItemStack.EMPTY;
            ItemStack got = out.copyWithCount(Math.min(amount, out.getCount()));
            if (!simulate) {
                out.shrink(got.getCount());
                if (out.isEmpty())
                    out = ItemStack.EMPTY;
                setChanged();
            }
            return got;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && crosses(stack);
        }
    };

    public EnderPortBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ENDER_PORT.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return handler;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    public boolean anchored() {
        return anchored;
    }

    public int teleportantMb() {
        return tank.getFluidAmount();
    }

    /** Fluid-holding items and shielded items (shulker boxes) do not cross. */
    static boolean crosses(ItemStack stack) {
        return !stack.is(SHIELDS_PAIRING) && stack.getCapability(Capabilities.FluidHandler.ITEM) == null
                && !(stack.getItem() instanceof CarrierItem c && c.kind() == CarrierItem.Kind.POD);
    }

    private boolean sealed() {
        for (Direction d : Direction.values())
            if (level.getBlockState(worldPosition.relative(d)).getBlock() instanceof ShulkerBoxBlock)
                return true;
        return false;
    }

    @Nullable
    private EnderPortBlockEntity partner() {
        if (pair == null)
            return null;
        for (EnderPortBlockEntity p : LIVE.getOrDefault(pair, Set.of()))
            if (p != this && !p.isRemoved())
                return p;
        return null;
    }

    public Component partnerState() {
        EnderPortBlockEntity p = partner();
        return Component.translatable(pair == null ? "message.feedback.ender_port.unbonded"
                : p == null ? "message.feedback.ender_port.away" : "message.feedback.ender_port.linked");
    }

    /** Blocks to the other end of the pair (the cross-dimension figure when it is in another dimension), or -1 with no partner here. */
    public float partnerDistance() {
        EnderPortBlockEntity p = partner();
        return p == null ? -1f : distanceTo(p);
    }

    private float distanceTo(EnderPortBlockEntity p) {
        if (p.level != level)
            return FTuning.PAIR_CROSS_DIMENSION_BLOCKS;
        return (float) Math.sqrt(worldPosition.distSqr(p.worldPosition));
    }

    public void anchor() {
        anchored = true;
        force(true);
        setChanged();
    }

    private void force(boolean on) {
        if (level instanceof ServerLevel sl)
            sl.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, on);
    }

    public void tickServer() {
        if (level == null)
            return;
        long now = level.getGameTime();
        if (now % 20 != 0)
            return;
        if (!out.isEmpty())
            pushOut();
        if (in.isEmpty() || sealed())
            return;
        EnderPortBlockEntity other = partner();
        if (other == null || !other.out.isEmpty() || other.sealed())
            return;
        float elapsed = now - allowanceAt;
        allowance = Math.min(FTuning.PAIR_CAP_MU_PER_MIN, allowance + FTuning.PAIR_CAP_MU_PER_MIN * elapsed / 1200f);
        allowanceAt = now;
        float mass = CarrierItem.stackMass(in);
        if (mass > FTuning.PAIR_CAP_MU_PER_MIN) {
            // Over the cap for ever: handed back rather than held for ever.
            dropStack(in);
            in = ItemStack.EMPTY;
            setChanged();
            return;
        }
        int cost = (int) Math.ceil(mass * FTuning.PAIR_TELEPORTANT_MB_PER_MU * (1f + distanceTo(other) / FTuning.PAIR_DISTANCE_SCALE));
        if (allowance < mass)
            return;
        if (tank.getFluidAmount() < cost) {
            // A link that was carrying and runs dry has nowhere clean to put what is mid-transit (ender spec 2.1); one that was never
            // started simply waits.
            if (now < liveUntil)
                destabilise();
            return;
        }
        liveUntil = now + FTuning.PAIR_LIVE_TICKS;
        allowance -= mass;
        tank.drain(cost, IFluidHandler.FluidAction.EXECUTE);
        other.out = in;
        in = ItemStack.EMPTY;
        other.setChanged();
        setChanged();
    }

    /** The feed failed while the link ran: the stack in transit is lost and what stands near either end may hop, as with chorus fruit. */
    private void destabilise() {
        liveUntil = 0L;
        in = ItemStack.EMPTY;
        setChanged();
        io.github.soundgoodizerfan.feedback.core.hazard.SpatialHop.hop(level, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition), FTuning.PAIR_DRY_HOP_MB);
    }

    /** The id this port is bonded under, or null. */
    @Nullable
    public UUID pairId() {
        return pair;
    }

    private void pushOut() {
        for (Direction d : Direction.values()) {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (h == null)
                continue;
            for (int s = 0; s < h.getSlots() && !out.isEmpty(); s++)
                out = h.insertItem(s, out, false);
            if (out.isEmpty()) {
                setChanged();
                return;
            }
        }
    }

    private void dropStack(ItemStack s) {
        level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, s));
    }

    public void dropAll() {
        if (level == null)
            return;
        if (!in.isEmpty())
            dropStack(in);
        if (!out.isEmpty())
            dropStack(out);
        in = out = ItemStack.EMPTY;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && pair != null) {
            LIVE.computeIfAbsent(pair, k -> new HashSet<>()).add(this);
            PairBook.get(level.getServer()).put(pair, level.dimension(), worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
        forget();
    }

    private void forget() {
        if (pair != null && LIVE.containsKey(pair)) {
            LIVE.get(pair).remove(this);
            if (LIVE.get(pair).isEmpty())
                LIVE.remove(pair);
        }
    }

    @Override
    public void setRemoved() {
        forget();
        if (!unloaded && level != null && !level.isClientSide && pair != null)
            PairBook.get(level.getServer()).remove(pair, level.dimension(), worldPosition);
        if (!unloaded && anchored && level != null && !level.isClientSide)
            force(false);
        super.setRemoved();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(PAIR_KEY)) {
            pair = data.copyTag().getUUID(PAIR_KEY);
            if (level != null && !level.isClientSide)
                LIVE.computeIfAbsent(pair, k -> new HashSet<>()).add(this);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (pair != null) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(PAIR_KEY, pair);
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (pair != null)
            tag.putUUID(PAIR_KEY, pair);
        if (!in.isEmpty())
            tag.put("In", in.save(registries));
        if (!out.isEmpty())
            tag.put("Out", out.save(registries));
        tag.putBoolean("Anchored", anchored);
        tag.putFloat("Allowance", allowance);
        tag.putLong("At", allowanceAt);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pair = tag.hasUUID(PAIR_KEY) ? tag.getUUID(PAIR_KEY) : null;
        in = tag.contains("In") ? ItemStack.parse(registries, tag.getCompound("In")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        out = tag.contains("Out") ? ItemStack.parse(registries, tag.getCompound("Out")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        anchored = tag.getBoolean("Anchored");
        allowance = tag.contains("Allowance") ? tag.getFloat("Allowance") : FTuning.PAIR_CAP_MU_PER_MIN;
        allowanceAt = tag.getLong("At");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
