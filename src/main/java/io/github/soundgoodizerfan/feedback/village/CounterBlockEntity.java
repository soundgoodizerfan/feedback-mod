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
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

/**
 * A shop counter. Two jobs. <b>Hand-over:</b> what arrives at the intake goes to the nearest villager within {@link #REACH} as a delivery
 * would, and the answer is pushed out (emeralds into the till, refused goods into the returns); a want sold this way pays
 * {@link VillageTuning#PUSH_RATE} of its line, because the villager did not choose to come. <b>Shelf:</b> nine slots of goods a villager
 * walks to buy. A slow scan finds villagers in the catchment (straight radius plus the road, {@link RoadReach}) that are idle or meeting,
 * have an unmet want the shelf serves, and have a path to the front face; it sets their vanilla {@code WALK_TARGET}. Nothing runs on a
 * villager. When one stands at the front it buys at the keeper's price and the emeralds go to the till. No path, no pull: a villager sealed
 * in a cell is simply never drawn.
 *
 * <p>Slot layout of the side handler: 0 intake (insert only), 1..9 till, 10..18 returns (extract only). The shelf is its own handler.
 */
public class CounterBlockEntity extends BlockEntity {

    /** Blocks within which a villager is found for a hand-over; the same reach a villager's own screen keeps. */
    public static final int REACH = 8;
    public static final int SHELF = 9;
    private static final int CYCLE_TICKS = 20;

    private final ItemStackHandler intake = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler till = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler returns = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Goods on display. Only what some villager pool buys may stand here. */
    private final ItemStackHandler shelf = new ItemStackHandler(SHELF) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return VillageEconomy.marketPrice(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The keeper's price per item for each shelf slot; 0 means the villager's own line price (and the market price for a player). */
    private final int[] prices = new int[SHELF];
    /** The last player to open the back menu: used only to aim gossip, never to allow or refuse anything. */
    @Nullable
    private UUID keeper;

    // runtime only
    private final Map<UUID, Long> pulled = new HashMap<>();
    private RoadReach road = RoadReach.NONE;
    private long roadAt = Long.MIN_VALUE;
    private long statsDay = -1;
    private int inReach, wanting, errandsPending, sealed, tooDear, walkInSales, handSales, stalled, fairSales, walkInCount;
    private boolean near;

    public CounterBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.COUNTER_ENTITY.get(), pos, state);
    }

    // --- ports ---

    /** What a hopper, tube or belt sees from {@code side}: nothing at the front (the shop face), the shelf on the back and top, the intake and till elsewhere. */
    @Nullable
    public IItemHandler port(@Nullable Direction side) {
        Direction front = facing();
        if (side == front)
            return null;
        if (side == front.getOpposite() || side == Direction.UP)
            return shelfPort();
        return sidePort();
    }

    private IItemHandler sidePort() {
        return new CombinedInvWrapper(intake, till, returns) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 0 ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
            }
        };
    }

    private IItemHandler shelfPort() {
        return new net.neoforged.neoforge.items.wrapper.RangedWrapper(shelf, 0, SHELF) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        };
    }

    public ItemStackHandler shelf() {
        return shelf;
    }

    public ItemStackHandler tillSlots() {
        return till;
    }

    public ItemStackHandler returnSlots() {
        return returns;
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(CounterBlock.FACING) ? s.getValue(CounterBlock.FACING) : Direction.NORTH;
    }

    public void keeper(@Nullable UUID who) {
        if (who != null && !who.equals(keeper)) {
            keeper = who;
            setChanged();
        }
    }

    public int price(int slot) {
        return prices[slot];
    }

    public void price(int slot, int each) {
        prices[slot] = Math.max(0, Math.min(64, each));
        setChanged();
    }

    /** Menu data: the nine keeper prices, then the nine market prices of what stands on the shelf. */
    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int i) {
                return i < SHELF ? prices[i] : VillageEconomy.marketPrice(shelf.getStackInSlot(i - SHELF));
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return SHELF * 2;
            }
        };
    }

    // --- the player's side of the counter ---

    /** What a player pays for one item of shelf slot {@code slot}: the keeper's price, or the market price when none is set. */
    public int playerPrice(int slot) {
        int each = prices[slot];
        return each > 0 ? each : Math.max(1, VillageEconomy.marketPrice(shelf.getStackInSlot(slot)));
    }

    /** Puts {@code n} emeralds in the till; false (nothing moved) when it cannot hold them all. */
    public boolean payTill(int n) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int left = n; left > 0; left -= 64)
            stacks.add(new ItemStack(Items.EMERALD, Math.min(64, left)));
        // Simulate against a copy so a partial deposit never happens.
        ItemStackHandler copy = new ItemStackHandler(till.getSlots());
        for (int i = 0; i < till.getSlots(); i++)
            copy.setStackInSlot(i, till.getStackInSlot(i).copy());
        for (ItemStack s : stacks)
            if (!ItemHandlerHelper.insertItem(copy, s, false).isEmpty())
                return false;
        for (ItemStack s : stacks)
            ItemHandlerHelper.insertItem(till, s, false);
        return true;
    }

    // --- ticking ---

    public void tickServer() {
        if (!(level instanceof ServerLevel sl))
            return;
        long t = sl.getGameTime();
        if (t % CYCLE_TICKS == 0)
            handOverCycle();
        if (!pulled.isEmpty() && t % VillageTuning.ARRIVAL_TICKS == 0)
            arrivals(sl, t);
        if (Math.floorMod(t + worldPosition.asLong(), (long) VillageTuning.PULL_SCAN_TICKS) == 0)
            scan(sl, t);
    }

    private void rollDay(long day) {
        if (day != statsDay) {
            statsDay = day;
            tooDear = walkInSales = handSales = stalled = 0;
        }
    }

    private void handOverCycle() {
        Villager v = level.getEntitiesOfClass(Villager.class, new AABB(worldPosition).inflate(REACH), x -> !x.isBaby()).stream()
                .min(java.util.Comparator.comparingDouble(x -> x.distanceToSqr(worldPosition.getCenter()))).orElse(null);
        near = v != null;
        ItemStack work = intake.getStackInSlot(0);
        if (work.isEmpty() || full(till) || full(returns))
            return;
        rollDay(level.getGameTime() / VillageTuning.DAY_TICKS);
        // A board within reach takes what its open contracts ask for before any villager does; what it refuses goes on to the villager.
        if (level instanceof ServerLevel sl && LedgerBoardBlockEntity.near(sl, worldPosition) instanceof LedgerBoardBlockEntity board) {
            LedgerBoardBlockEntity.Delivered d = board.deliver(sl, work, keeper);
            if (d.fate() == LedgerBoardBlockEntity.Delivered.Fate.TAKEN) {
                if (d.paid() > 0)
                    ItemHandlerHelper.insertItem(till, new ItemStack(Items.EMERALD, d.paid()), false);
                if (!d.reward().isEmpty()) {
                    ItemStack left = ItemHandlerHelper.insertItem(returns, d.reward(), false);
                    if (!left.isEmpty())
                        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), left);
                }
                handSales++;
                setChanged();
                return;
            }
        }
        if (v == null)
            return;
        VillageEconomy.Handed done = VillageEconomy.handOver(v, work);
        if (done.fate() == VillageEconomy.Fate.TAKEN) {
            handSales++;
            if (done.emeralds() > 0)
                ItemHandlerHelper.insertItem(till, new ItemStack(Items.EMERALD, done.emeralds()), false);
            setChanged();
        } else {
            // Not taken: the whole stack goes back out the return slots.
            ItemStack back = work.copy();
            intake.setStackInSlot(0, ItemStack.EMPTY);
            ItemStack left = ItemHandlerHelper.insertItem(returns, back, false);
            if (!left.isEmpty())
                intake.setStackInSlot(0, left);
        }
    }

    private List<ItemStack> stock() {
        List<ItemStack> out = new ArrayList<>(SHELF);
        for (int i = 0; i < SHELF; i++)
            out.add(shelf.getStackInSlot(i));
        return out;
    }

    /**
     * The air the shelf sits in: the world's ambient, pulled toward any thermal body (a stove, a cold box) touching the Counter by a share of its
     * excess. A shelf by a fire ruins bread and one by cold keeps it, with no rule: the stack's own spoilage clock reads this when it is settled.
     */
    private float shelfTu() {
        float amb = io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value();
        float env = amb;
        for (Direction d : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody tb) {
                float pulled = amb + (tb.getTemperature().value() - amb) * VillageTuning.SHELF_HEAT_SHARE;
                if (Math.abs(pulled - amb) > Math.abs(env - amb))
                    env = pulled;
            }
        return env;
    }

    /** Brings every perishable on the shelf up to now in the shelf's air. */
    private void settleShelf() {
        float env = shelfTu();
        for (int i = 0; i < SHELF; i++)
            if (!shelf.getStackInSlot(i).isEmpty())
                io.github.soundgoodizerfan.feedback.process.Cooking.settle(shelf.getStackInSlot(i), level, env);
    }

    /** The market value of everything on the shelf: what a raider would think it worth the trip. */
    public int shelfValue() {
        int v = 0;
        for (int i = 0; i < SHELF; i++) {
            ItemStack s = shelf.getStackInSlot(i);
            v += VillageEconomy.marketPrice(s) * s.getCount();
        }
        return v;
    }

    /** A raider takes the most valuable stack off the shelf and gets away with it. */
    public ItemStack plunder() {
        int best = -1, bv = 0;
        for (int i = 0; i < SHELF; i++) {
            int v = VillageEconomy.marketPrice(shelf.getStackInSlot(i)) * shelf.getStackInSlot(i).getCount();
            if (v > bv) {
                bv = v;
                best = i;
            }
        }
        if (best < 0)
            return ItemStack.EMPTY;
        ItemStack out = shelf.extractItem(best, shelf.getStackInSlot(best).getCount(), false);
        setChanged();
        return out;
    }

    public boolean stocked() {
        for (int i = 0; i < SHELF; i++)
            if (!shelf.getStackInSlot(i).isEmpty())
                return true;
        return false;
    }

    /** The tile in front of the shop face, where a villager stands. */
    public BlockPos front() {
        return worldPosition.relative(facing());
    }

    private Vec3 frontCentre() {
        return Vec3.atBottomCenterOf(front());
    }

    private void scan(ServerLevel sl, long t) {
        rollDay(t / VillageTuning.DAY_TICKS);
        inReach = wanting = errandsPending = sealed = 0;
        if (!stocked()) {
            road = RoadReach.NONE;
            roadAt = Long.MIN_VALUE;
            return;
        }
        if (t - roadAt >= VillageTuning.ROAD_RESCAN_TICKS) {
            road = RoadReach.flood(sl, front());
            roadAt = t;
        }
        AABB box = new AABB(worldPosition).inflate(VillageTuning.PULL_RADIUS);
        if (road.size() > 0)
            box = box.minmax(road.bounds());
        Vec3 centre = worldPosition.getCenter();
        List<Villager> found = sl.getEntitiesOfClass(Villager.class, box, v -> !v.isBaby() && v.isAlive());
        found.sort(java.util.Comparator.comparingDouble(v -> v.distanceToSqr(centre)));
        settleShelf();
        List<ItemStack> stock = stock();
        double r2 = (double) VillageTuning.PULL_RADIUS * VillageTuning.PULL_RADIUS;
        int paths = 0, seen = 0;
        for (Villager v : found) {
            if (seen >= VillageTuning.PULL_MAX_PER_SCAN)
                break;
            if (v.distanceToSqr(centre) > r2 && !road.near(v.blockPosition()))
                continue;
            seen++;
            inReach++;
            if (!VillageEconomy.wantsFrom(v, stock))
                continue;
            wanting++;
            if (v.position().distanceTo(frontCentre()) <= VillageTuning.ARRIVE_DIST) {
                sell(v);
                continue;
            }
            if (VillageEconomy.errandsLeft(v) <= 0)
                continue;
            errandsPending++;
            if (pulled.containsKey(v.getUUID()) || !free(v) || nearerCounter(sl, v, stock))
                continue;
            if (paths >= VillageTuning.PULL_PATHS_PER_SCAN)
                continue;
            paths++;
            Path p = v.getNavigation().createPath(front(), 1);
            if (p == null || !p.canReach()) {
                sealed++;
                continue;
            }
            v.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET, new WalkTarget(front(), VillageTuning.PULL_SPEED, 1),
                    VillageTuning.PULL_EXPIRY);
            pulled.put(v.getUUID(), t + VillageTuning.PULL_EXPIRY);
        }
    }

    /** Idle or meeting (the windows the vanilla schedule leaves free) with nowhere to go. */
    private static boolean free(Villager v) {
        return v.getBrain().getActiveNonCoreActivity().map(a -> a == Activity.IDLE || a == Activity.MEET).orElse(false)
                && !v.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }

    /** Whether another loaded, stocked Counter that serves this villager stands nearer to it: the nearest wins, so competition needs no rule. */
    private boolean nearerCounter(ServerLevel sl, Villager v, List<ItemStack> mine) {
        double mineD = v.distanceToSqr(frontCentre());
        for (CounterBlockEntity c : CounterIndex.in(sl)) {
            if (c == this || c.isRemoved() || !c.stocked())
                continue;
            if (v.distanceToSqr(c.frontCentre()) < mineD && VillageEconomy.wantsFrom(v, c.stock()))
                return true;
        }
        return false;
    }

    private void arrivals(ServerLevel sl, long t) {
        var it = pulled.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (!(sl.getEntity(e.getKey()) instanceof Villager v) || !v.isAlive() || e.getValue() < t) {
                it.remove();
                continue;
            }
            if (v.position().distanceTo(frontCentre()) <= VillageTuning.ARRIVE_DIST) {
                it.remove();
                sell(v);
            }
        }
    }

    /** A villager at the front face buys from the shelf. */
    private void sell(Villager v) {
        if (full(till)) {
            stalled++;
            return;
        }
        settleShelf();
        List<ItemStack> stock = stock();
        int[] copy = prices.clone();
        VillageEconomy.Sale s = VillageEconomy.walkIn(v, stock, copy);
        if (s.fate() == VillageEconomy.WalkIn.TOO_DEAR) {
            tooDear++;
            return;
        }
        if (s.fate() != VillageEconomy.WalkIn.SOLD)
            return;
        shelf.extractItem(s.slot(), s.count(), false);
        ItemStack pay = new ItemStack(Items.EMERALD, s.each() * s.count());
        for (int left = pay.getCount(); left > 0; left -= 64) {
            ItemStack part = new ItemStack(Items.EMERALD, Math.min(64, left));
            ItemStack rest = ItemHandlerHelper.insertItem(till, part, false);
            if (!rest.isEmpty())
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), rest);
        }
        walkInSales++;
        walkInCount++;
        gossip(v, s);
        setChanged();
    }

    private void gossip(Villager v, VillageEconomy.Sale s) {
        if (keeper == null)
            return;
        if (walkInCount % VillageTuning.WALKIN_GOSSIP_EVERY == 0)
            v.getGossips().add(keeper, GossipType.MINOR_POSITIVE, 1);
        int over = s.each() - s.market();
        if (over > 0) {
            int hits = 0;
            for (int i = 0; i < over * s.count(); i++)
                if (level.random.nextFloat() < VillageTuning.GOUGE_REP_CHANCE)
                    hits++;
            if (hits > 0)
                v.getGossips().add(keeper, GossipType.MINOR_NEGATIVE, hits);
        } else if (over < 0 && ++fairSales % VillageTuning.FAIR_REP_EVERY == 0) {
            // Under market: a small positive word, now and then.
            v.getGossips().add(keeper, GossipType.MINOR_POSITIVE, 1);
        }
    }

    private static boolean full(ItemStackHandler h) {
        for (int i = 0; i < h.getSlots(); i++)
            if (h.getStackInSlot(i).isEmpty())
                return false;
        return true;
    }

    // --- reading it ---

    /** The catchment readout figures, for the report and Jade. */
    public CompoundTag readout() {
        CompoundTag t = new CompoundTag();
        t.putInt("InReach", inReach);
        t.putInt("Wanting", wanting);
        t.putInt("Errands", errandsPending);
        t.putInt("Sealed", sealed);
        t.putInt("TooDear", tooDear);
        t.putInt("WalkIn", walkInSales);
        t.putInt("Hand", handSales);
        t.putInt("Stalled", stalled);
        t.putInt("Road", road.size());
        t.putBoolean("Stocked", stocked());
        t.putBoolean("Near", near);
        return t;
    }

    public Component report() {
        if (!stocked())
            return Component.translatable(near ? "feedback.counter.near" : "feedback.counter.alone");
        return Component.translatable("feedback.counter.catchment", inReach, wanting, errandsPending, sealed, walkInSales, handSales);
    }

    public void dropAll() {
        if (level == null)
            return;
        for (ItemStackHandler h : new ItemStackHandler[]{intake, till, returns, shelf})
            for (int i = 0; i < h.getSlots(); i++)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), h.getStackInSlot(i));
    }

    // --- lifecycle and saving ---

    @Override
    public void onLoad() {
        super.onLoad();
        CounterIndex.add(this);
    }

    @Override
    public void setRemoved() {
        CounterIndex.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        CounterIndex.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Intake", intake.serializeNBT(registries));
        tag.put("Payout", till.serializeNBT(registries));
        tag.put("Returns", returns.serializeNBT(registries));
        tag.put("Shelf", shelf.serializeNBT(registries));
        tag.putIntArray("Prices", prices);
        if (keeper != null)
            tag.putUUID("Keeper", keeper);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        intake.deserializeNBT(registries, tag.getCompound("Intake"));
        till.deserializeNBT(registries, tag.getCompound("Payout"));
        returns.deserializeNBT(registries, tag.getCompound("Returns"));
        shelf.deserializeNBT(registries, tag.getCompound("Shelf"));
        int[] p = tag.getIntArray("Prices");
        for (int i = 0; i < SHELF; i++)
            prices[i] = i < p.length ? p[i] : 0;
        keeper = tag.hasUUID("Keeper") ? tag.getUUID("Keeper") : null;
    }
}
