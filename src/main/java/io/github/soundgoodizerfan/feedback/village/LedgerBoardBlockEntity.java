// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.DataResult;

import io.github.soundgoodizerfan.feedback.modular.prep.DishProfile;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;
import io.github.soundgoodizerfan.feedback.village.Contracts.Contract;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The village's notice board. It holds what contracts need to be remembered (pieces delivered and the grades so far, keyed by the contract's
 * id, and the village's own saturation bins) and nothing else: the orders it lists, the contracts it posts and the standing it shows are all
 * derived when the board opens. A Counter within {@link VillageTuning#LINK_REACH} fills contracts by proximity through {@link #deliver}.
 */
public class LedgerBoardBlockEntity extends BlockEntity {

    public static final BlockIndex<LedgerBoardBlockEntity> INDEX = new BlockIndex<>();

    /** Pieces delivered and the sum of their grade ordinals, per contract id. */
    private final Map<Long, int[]> progress = new HashMap<>();
    /** The village's saturation bins, shared across its contracts per (type, kind). */
    private Ledger bins = Ledger.EMPTY;
    /** Banner bounties owed for won raids. */
    private int bounties;
    /** The raid being watched; runtime only, lost on a reload (a raid that ends across one pays no bounty). */
    private net.minecraft.world.entity.raid.Raid watched;

    public LedgerBoardBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.BOARD_ENTITY.get(), pos, state);
    }

    /** What a delivery came to. */
    public record Delivered(Fate fate, int paid, int grade, ItemStack reward) {
        public enum Fate { TAKEN, REJECTED, UNWANTED }

        static final Delivered UNWANTED = new Delivered(Fate.UNWANTED, 0, -1, ItemStack.EMPTY);
    }

    // --- contracts ---

    private int[] state(long id) {
        return progress.computeIfAbsent(id, k -> new int[2]);
    }

    /** The nearest loaded board within {@link VillageTuning#LINK_REACH} of {@code from}, or null. */
    @Nullable
    public static LedgerBoardBlockEntity near(ServerLevel level, BlockPos from) {
        LedgerBoardBlockEntity best = null;
        double bd = (double) VillageTuning.LINK_REACH * VillageTuning.LINK_REACH;
        for (LedgerBoardBlockEntity b : INDEX.in(level)) {
            double d = b.worldPosition.distSqr(from);
            if (!b.isRemoved() && d <= bd) {
                best = b;
                bd = d;
            }
        }
        return best;
    }

    /**
     * Offers {@code item} to the open contracts as a delivery would: the first contract of its type that the piece reaches the class for takes it
     * and pays the line (class factor, the village's own bin). Finishing a contract adds a bonus, praise from the village toward {@code who}, and
     * with honoured standing or an Exceptional mean a blueprint. {@code item} is shrunk by one when taken.
     */
    public Delivered deliver(ServerLevel level, ItemStack item, @Nullable UUID who) {
        long today = level.getGameTime() / VillageTuning.DAY_TICKS;
        ModularDesign design = ModularStacks.design(item);
        ModularFigures figures = DishProfile.read(item, level);
        if (design == null || figures == null)
            return Delivered.UNWANTED;
        int tier = who == null ? 0 : VillageStanding.read(level, worldPosition, who).tier();
        boolean asked = false;
        int worst = -1;
        for (Contract c : Contracts.open(worldPosition, today, tier)) {
            int[] st = state(c.id());
            ModularDesign ordered = io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.forOrder(design, c.order().type());
            if (st[0] >= c.count() || ordered == null)
                continue;
            asked = true;
            CommissionJudge.Result r = CommissionJudge.judge(c.order(), figures, ordered, MaterialTable.get().materials());
            worst = Math.max(worst, r.grade().ordinal());
            if (r.grade().ordinal() < c.min().ordinal())
                continue;
            String bin = DishProfile.is(item) ? c.order().type() + "|" + DishProfile.bin(figures) : VillageEconomy.binKey(c.order().type(), ordered);
            Ledger b = bins.on(today);
            int paid = Math.round(c.order().pay() * r.grade().payFactor() * b.payKept(bin, today));
            bins = b.delivered(0, bin, today);
            item.shrink(1);
            st[0]++;
            st[1] += r.grade().ordinal();
            ItemStack reward = ItemStack.EMPTY;
            if (st[0] >= c.count()) {
                paid += VillageTuning.CONTRACT_BONUS * (1 + tier);
                if (who != null)
                    VillageStanding.praise(level, worldPosition, who, VillageTuning.CONTRACT_GOSSIP);
                float mean = st[1] / (float) c.count();
                if (mean >= CommissionJudge.Grade.EXCEPTIONAL.ordinal() - 0.5f || tier >= VillageTuning.STANDING_TIERS.length)
                    reward = blueprint(c);
            }
            setChanged();
            return new Delivered(Delivered.Fate.TAKEN, paid, r.grade().ordinal(), reward);
        }
        return asked ? new Delivered(Delivered.Fate.REJECTED, 0, worst, ItemStack.EMPTY) : Delivered.UNWANTED;
    }

    /** A plain reproducible blueprint: a line from the goods table that sells one, else the starter design of the contract's own type. */
    private ItemStack blueprint(Contract c) {
        List<net.minecraft.resources.ResourceLocation> ids = new ArrayList<>();
        for (Good g : GoodsTable.get().entries())
            g.blueprint().filter(b -> !VillageEconomy.blueprint(b).isEmpty()).ifPresent(ids::add);
        ids.sort(Comparator.comparing(net.minecraft.resources.ResourceLocation::toString));
        if (ids.isEmpty())
            return VillageEconomy.blueprint(c.order().type());
        return VillageEconomy.blueprint(ids.get((int) Math.floorMod(c.id() * 31L + worldPosition.asLong(), (long) ids.size())));
    }

    /** Emeralds a banner in {@code item} earns as a raid's bounty; 0 when it is no illager banner or no won raid is owed. Shrinks the stack when paid. */
    public int bounty(ItemStack item) {
        if (bounties <= 0 || item.isEmpty() || !(level instanceof ServerLevel sl))
            return 0;
        ItemStack banner = net.minecraft.world.entity.raid.Raid.getLeaderBannerInstance(
                sl.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN));
        if (!ItemStack.isSameItemSameComponents(item, banner))
            return 0;
        item.shrink(1);
        bounties--;
        setChanged();
        return VillageTuning.BOUNTY_PAY;
    }

    /**
     * Watches for a raid on the village: while one is on the notices warn of it; when it ends in victory every player with the hero's effect is
     * praised by the villagers and one banner bounty is owed. Slow cadence; nothing per villager.
     */
    public void tickServer() {
        if (!(level instanceof ServerLevel sl) || sl.getGameTime() % 100 != 7)
            return;
        net.minecraft.world.entity.raid.Raid r = sl.getRaids().getNearbyRaid(worldPosition,
                VillageTuning.VILLAGE_RADIUS * VillageTuning.VILLAGE_RADIUS);
        if (r != null) {
            watched = r;
            return;
        }
        if (watched != null) {
            if (watched.isVictory()) {
                bounties++;
                for (net.minecraft.server.level.ServerPlayer p : sl.players())
                    if (p.hasEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE)
                            && p.distanceToSqr(worldPosition.getCenter()) < (double) VillageTuning.VILLAGE_RADIUS * VillageTuning.VILLAGE_RADIUS)
                        VillageStanding.praise(sl, worldPosition, p.getUUID(), VillageTuning.RAID_PRAISE);
                setChanged();
            }
            watched = null;
        }
    }

    // --- what the board shows ---

    public BoardView view(ServerLevel level, UUID player) {
        long today = level.getGameTime() / VillageTuning.DAY_TICKS;
        VillageStanding.Reading standing = VillageStanding.read(level, worldPosition, player);
        List<BoardView.OrderLine> orders = new ArrayList<>();
        for (Villager v : VillageStanding.villagers(level, worldPosition, VillageTuning.BOARD_VILLAGERS_MAX)) {
            Ledger ledger = v.getData(io.github.soundgoodizerfan.feedback.registry.FVillage.LEDGER).on(today);
            List<Order> list = Guilds.ordersOf(v, today);
            for (int i = 0; i < list.size(); i++) {
                Order o = list.get(i);
                orders.add(new BoardView.OrderLine(OrderWords.typeName(o.type()),
                        Component.translatable("entity.minecraft.villager." + VillageEconomy.profession(v).getPath()),
                        OrderWords.oneLine(o), o.pay(), ledger.done(i)));
            }
        }
        orders.sort(Comparator.comparing((BoardView.OrderLine o) -> o.type().getString()).thenComparing(o -> o.profession().getString()));
        List<BoardView.ContractLine> contracts = new ArrayList<>();
        for (Contract c : Contracts.open(worldPosition, today, standing.tier())) {
            Component what = Component.translatable("village.feedback.board.contract_what", OrderWords.typeName(c.order().type()), OrderWords.oneLine(c.order()));
            contracts.add(new BoardView.ContractLine(what,
                    Component.translatable("village.feedback.grade." + c.min().name().toLowerCase(java.util.Locale.ROOT)),
                    progress.getOrDefault(c.id(), new int[2])[0], c.count(), (int) c.daysLeft(today), c.order().pay()));
        }
        return new BoardView(standing.mean(), standing.tier(), standing.sampled(), List.copyOf(orders), List.copyOf(contracts),
                notices(level, today, standing), -1, 0);
    }

    private List<Component> notices(ServerLevel level, long today, VillageStanding.Reading standing) {
        List<Component> out = new ArrayList<>();
        if (watched != null && !watched.isOver())
            out.add(Component.translatable("village.feedback.board.raid"));
        if (bounties > 0)
            out.add(Component.translatable("village.feedback.board.bounty", VillageTuning.BOUNTY_PAY, bounties));
        out.add(Component.translatable("village.feedback.board.standing." + standing.tier()));
        if (standing.tier() < VillageTuning.STANDING_TIERS.length)
            out.add(Component.translatable("village.feedback.board.next." + standing.tier()));
        int until = MarketDay.daysUntil(today);
        MarketCrossBlockEntity cross = MarketDay.cross(level, worldPosition);
        out.add(cross == null ? Component.translatable("village.feedback.board.market_none")
                : until == 0 ? Component.translatable("village.feedback.board.market_today")
                : Component.translatable("village.feedback.board.market_in", until));
        return out;
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
        ListTag list = new ListTag();
        progress.forEach((id, st) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Id", id);
            t.putInt("N", st[0]);
            t.putInt("G", st[1]);
            list.add(t);
        });
        tag.put("Progress", list);
        tag.putInt("Bounties", bounties);
        DataResult<Tag> enc = Ledger.CODEC.encodeStart(NbtOps.INSTANCE, bins);
        enc.result().ifPresent(t -> tag.put("Bins", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress.clear();
        bounties = tag.getInt("Bounties");
        for (Tag e : tag.getList("Progress", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) e;
            progress.put(t.getLong("Id"), new int[]{t.getInt("N"), t.getInt("G")});
        }
        bins = tag.contains("Bins") ? Ledger.CODEC.parse(NbtOps.INSTANCE, tag.get("Bins")).result().orElse(Ledger.EMPTY) : Ledger.EMPTY;
    }
}
