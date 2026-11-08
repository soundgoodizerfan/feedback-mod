// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.DishProfile;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.net.VillagerStatePayload;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVillage;
import io.github.soundgoodizerfan.feedback.village.VillagerView.GoodView;
import io.github.soundgoodizerfan.feedback.village.VillagerView.OrderView;
import io.github.soundgoodizerfan.feedback.village.VillagerView.TermView;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The server half of the villager screen: opens it, works out what it shows, and settles a
 * delivery or a purchase. Everything is derived from the villager and the day when asked, so
 * nothing ticks; the only stored state is the villager's {@link Ledger} and vanilla's gossip.
 */
public final class VillageEconomy {

    private VillageEconomy() {
    }

    /** A wandering trader's stand-in profession in the goods table. */
    public static final ResourceLocation WANDERER = Feedback.id("wanderer");

    public static ResourceLocation profession(AbstractVillager m) {
        return m instanceof Villager v ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession())
                : m.getPersistentData().getBoolean(MarketDay.REGIONAL) ? MarketDay.REGIONAL_PROFESSION : WANDERER;
    }

    /** The villager's kind by home biome (taiga, desert, plains ...): what a dish order's lean is keyed on. */
    public static String villagerType(AbstractVillager m) {
        return m instanceof Villager v ? BuiltInRegistries.VILLAGER_TYPE.getKey(v.getVillagerData().getType()).getPath()
                : m.getPersistentData().getString(MarketDay.TYPE);
    }

    public static int level(AbstractVillager m) {
        return m instanceof Villager v ? v.getVillagerData().getLevel() : 1;
    }

    static long today(AbstractVillager m) {
        return m.level().getGameTime() / VillageTuning.DAY_TICKS;
    }

    private static int standing(AbstractVillager m, ServerPlayer p) {
        return m instanceof Villager v ? v.getPlayerReputation(p) : 0;
    }

    /** Opens the screen on {@code m} for {@code p} and sends the first view. */
    public static void open(ServerPlayer p, AbstractVillager m) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new VillagerMenu(id, inv, m.getId()), m.getDisplayName()),
                buf -> buf.writeVarInt(m.getId()));
        m.setTradingPlayer(p); // vanilla's own hold: the villager stops, faces the player and waits
        if (p.containerMenu instanceof VillagerMenu menu)
            send(p, menu, view(p, m).withResult(VillagerView.NONE, -1, false, 0));
    }

    private static void send(ServerPlayer p, VillagerMenu menu, VillagerView view) {
        PacketDistributor.sendToPlayer(p, new VillagerStatePayload(menu.containerId, view));
    }

    // --- what the screen shows ---

    public static VillagerView view(ServerPlayer p, AbstractVillager m) {
        long day = today(m);
        Ledger ledger = m.getData(FVillage.LEDGER).on(day);
        int level = level(m);
        int rep = standing(m, p);
        boolean wanderer = !(m instanceof Villager);
        List<OrderView> orders = new ArrayList<>();
        if (!wanderer) {
            for (Order o : Guilds.ordersOf((Villager) m, day)) {
                List<TermView> terms = new ArrayList<>();
                for (Order.Term t : o.terms())
                    terms.add(new TermView(t.fig(), t.band(), t.must(), t.above()));
                orders.add(new OrderView(o.type(), o.pay(), List.copyOf(terms), o.asks()));
            }
        }
        List<GoodView> goods = new ArrayList<>();
        for (int idx : shop(m)) {
            Good g = GoodsTable.get().entries().get(idx);
            goods.add(new GoodView(stackFor(m, g, idx, day), price(g, rep), g.weekly() && !weeklyReady(ledger, idx, day) ? 0 : Math.max(0, stock(m, g) - ledger.sold(idx)), rep < g.standing(), idx));
        }
        List<VillagerView.WantView> wants = new ArrayList<>();
        for (int idx : wants(m, day)) {
            Want w = BuysTable.get().entries().get(idx);
            wants.add(new VillagerView.WantView(w.icon(), w.food(), w.priceFor(villagerType(m)), Math.max(0, w.max() - ledger.bought(idx)), idx));
        }
        int xp = 0, xpMax = 0;
        if (m instanceof Villager v && VillagerData.canLevelUp(level)) {
            xp = v.getVillagerXp() - VillagerData.getMinXpPerLevel(level);
            xpMax = VillagerData.getMaxXpPerLevel(level) - VillagerData.getMinXpPerLevel(level);
        }
        return new VillagerView(wanderer, level, xp, xpMax, rep, List.copyOf(orders), ledger.doneMask(), List.copyOf(goods), List.copyOf(wants),
                VillagerView.NONE, -1, false, 0, List.of());
    }

    /** A blueprint of the named design: a gun pool entry, else the starter design of a modular type; empty if neither exists. */
    static ItemStack blueprint(ResourceLocation id) {
        return io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.get().design(id)
                .or(() -> io.github.soundgoodizerfan.feedback.modular.ToolDesigns.starter(id,
                        io.github.soundgoodizerfan.feedback.modular.ModularTypes.known(), MaterialTable.get().materials()))
                .map(io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool::blueprint).orElse(ItemStack.EMPTY);
    }

    /** Indices into the goods table of what this merchant sells at its level, in file order. */
    private static List<Integer> shop(AbstractVillager m) {
        ResourceLocation prof = profession(m);
        int level = level(m);
        List<Good> all = GoodsTable.get().entries();
        List<Integer> out = new ArrayList<>();
        Guilds.Guild guild = m instanceof Villager v ? Guilds.of(v) : null;
        int guildTier = guild == null ? 0 : guild.tier();
        String type = villagerType(m);
        for (int i = 0; i < all.size(); i++) {
            Good g = all.get(i);
            if (g.profession().equals(prof) && g.level() <= level && g.guildTier() <= guildTier
                    && (g.types().isEmpty() || g.types().contains(type))
                    && g.blueprint().map(b -> !blueprint(b).isEmpty()).orElse(true))
                out.add(i);
        }
        return out;
    }

    /** Indices into the buys table of what this merchant wants today: four to six lines of the shared and profession pools. */
    static List<Integer> wants(AbstractVillager m, long day) {
        ResourceLocation prof = profession(m);
        List<Want> all = BuysTable.get().entries();
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < all.size(); i++)
            if (all.get(i).profession().equals(Want.ANY) || all.get(i).profession().equals(prof))
                pool.add(i);
        RandomSource rnd = RandomSource.create(m.getUUID().getMostSignificantBits() * 31L + m.getUUID().getLeastSignificantBits() + day * 0x9E3779B97F4A7C15L);
        java.util.Collections.shuffle(pool, new java.util.Random(rnd.nextLong()));
        List<Integer> picked = new ArrayList<>(pool.subList(0, Math.min(pool.size(), VillageTuning.WANTS_MIN + rnd.nextInt(VillageTuning.WANTS_MAX - VillageTuning.WANTS_MIN + 1))));
        if (MarketDay.feels(m))
            for (int extra : MarketDay.extras(rnd.nextLong()))
                if (!picked.contains(extra))
                    picked.add(extra);
        java.util.Collections.sort(picked);
        return picked;
    }

    /** Whether a weekly line has gone a week since this villager last sold it (the bin outlives the day's ledger reset). */
    private static boolean weeklyReady(Ledger ledger, int idx, long day) {
        Ledger.Bin b = ledger.bins().get("wk" + idx);
        return b == null || day - b.day() >= VillageTuning.WEEK_DAYS;
    }

    /** A line's stock today: its own, doubled for a villager at market (shop stock restocks on market day). */
    private static int stock(AbstractVillager m, Good g) {
        return g.stock() * (MarketDay.feels(m) ? VillageTuning.MARKET_STOCK_MULT : 1);
    }

    private static int price(Good g, int standing) {
        float shift = Math.max(-VillageTuning.PRICE_MARKUP_CAP, Math.min(VillageTuning.PRICE_DISCOUNT_CAP, standing * VillageTuning.PRICE_PER_STANDING));
        return Math.max(1, Math.round(g.price() * (1f - shift)));
    }

    /** The stack a good puts in hand; an enchanted book carries the one enchantment the villager picked for the day. */
    private static ItemStack stackFor(AbstractVillager m, Good g, int idx, long day) {
        if (g.blueprint().isPresent())
            return blueprint(g.blueprint().get());
        if (!g.enchantedBook())
            return g.stack();
        RandomSource rnd = RandomSource.create(m.getUUID().getLeastSignificantBits() ^ (day * 31L + idx));
        Optional<HolderSet.Named<Enchantment>> tag = m.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.TRADEABLE);
        Optional<Holder<Enchantment>> pick = tag.flatMap(t -> t.getRandomElement(rnd));
        if (pick.isEmpty())
            return new ItemStack(Items.BOOK);
        Holder<Enchantment> e = pick.get();
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e, 1 + rnd.nextInt(e.value().getMaxLevel())));
    }

    // --- a hint ---

    /** The hint button: worded hints for the order at {@code index}, sent back with the view. Free; nothing is stored. */
    public static void hint(ServerPlayer p, VillagerMenu menu, int index, int key) {
        AbstractVillager m = menu.merchant();
        if (!(m instanceof Villager v) || !menu.stillValid(p))
            return;
        List<Order> orders = Guilds.ordersOf(v, today(v));
        if (index < 0 || index >= orders.size() || orders.get(index).type().hashCode() != key) {
            send(p, menu, view(p, v));
            return;
        }
        send(p, menu, view(p, v).withHint(VillageHints.of(orders.get(index))));
    }

    // --- a delivery ---

    public static void deliver(ServerPlayer p, VillagerMenu menu, int index, int key) {
        AbstractVillager m = menu.merchant();
        if (!(m instanceof Villager v) || !menu.stillValid(p))
            return;
        long day = today(v);
        Ledger ledger = v.getData(FVillage.LEDGER).on(day);
        List<Order> orders = Guilds.ordersOf(v, day);
        if (index < 0 || index >= orders.size() || ledger.done(index))
            return;
        Order order = orders.get(index);
        if (order.type().hashCode() != key) { // the list moved under the click (a level-up): show the new one
            send(p, menu, view(p, v));
            return;
        }
        ItemStack item = menu.delivered();
        ModularDesign design = ModularStacks.design(item);
        ModularFigures figures = DishProfile.read(item, v.level());
        if (design == null || figures == null || !design.type().equals(order.type())) {
            send(p, menu, view(p, v).withResult(VillagerView.WRONG, -1, false, 0));
            return;
        }
        CommissionJudge.Result r = CommissionJudge.judge(order, figures, design, MaterialTable.get().materials());
        boolean guildOrder = Guilds.isGuildOrder(v, index);
        if (r.grade() == CommissionJudge.Grade.REJECTED || (guildOrder && r.grade().ordinal() < CommissionJudge.Grade.GOOD.ordinal())) {
            v.getGossips().add(p.getUUID(), GossipType.MINOR_NEGATIVE, VillageTuning.GOSSIP_REJECTED);
            v.playSound(SoundEvents.VILLAGER_NO, 1f, v.getVoicePitch());
            send(p, menu, view(p, v).withResult(CommissionJudge.Grade.REJECTED.ordinal(), complaint(r), r.worstHigh(), 0));
            return;
        }
        String bin = DishProfile.is(item) ? order.type() + "|" + DishProfile.bin(figures) : binKey(order.type(), design);
        int paid = Math.round(order.pay() * r.grade().payFactor() * ledger.payKept(bin, day));
        menu.consume();
        if (paid > 0)
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, paid));
        v.setData(FVillage.LEDGER, ledger.delivered(index, bin, day));
        if (guildOrder)
            Guilds.markDone(v, day);
        if (r.grade() == CommissionJudge.Grade.EXCEPTIONAL)
            v.getGossips().add(p.getUUID(), GossipType.MAJOR_POSITIVE, VillageTuning.GOSSIP_EXCEPTIONAL);
        else if (r.grade() == CommissionJudge.Grade.GOOD)
            v.getGossips().add(p.getUUID(), GossipType.MAJOR_POSITIVE, VillageTuning.GOSSIP_GOOD);
        earn(v, r.grade().xp(), day);
        v.playSound(SoundEvents.VILLAGER_YES, 1f, v.getVoicePitch());
        send(p, menu, view(p, v).withResult(r.grade().ordinal(), complaint(r), r.worstHigh(), paid));
    }

    /** The view's {@code fig}: a figure's ordinal, or past the last figure the index of the part ask that missed most, or -1. */
    private static int complaint(CommissionJudge.Result r) {
        return r.worstAsk() >= 0 ? ModularFigures.Fig.values().length + r.worstAsk() : r.worst() == null ? -1 : r.worst().ordinal();
    }

    /** The saturation bin of a delivery: the type and the kind of its primary part's material. */
    static String binKey(ResourceLocation type, ModularDesign design) {
        String kind = "";
        if (design != null) {
            ModularType t = ModularTypes.known().get(type);
            ModularDesign.Part part = t == null ? null : design.part(t.primary());
            Material m = part == null ? null : MaterialTable.get().materials().get(part.material());
            if (m != null)
                kind = m.kind().map(Enum::name).orElse("");
        }
        return type + "|" + kind;
    }

    /** Delivery experience: the villager's own, and a share to each other member of its guild. */
    static void earn(Villager v, int xp, long day) {
        sale(v, xp);
        Guilds.mentor(v, xp, day);
    }

    /**
     * Villager experience for a completed Counter transaction, and the same trade paid as player XP into a Trade Tap
     * within reach, if one has room. Nothing drops as orbs: a village with no tap near it pays no player XP.
     */
    static void sale(Villager v, int xp) {
        addXp(v, xp);
        if (xp > 0 && v.level() instanceof net.minecraft.server.level.ServerLevel level)
            io.github.soundgoodizerfan.feedback.machine.xp.XpTaps.pay(level, v.position(), xp * VillageTuning.TAP_XP_PER_VILLAGER_XP);
    }

    static void addXp(Villager v, int xp) {
        if (xp <= 0)
            return;
        v.setVillagerXp(v.getVillagerXp() + xp);
        int level = v.getVillagerData().getLevel();
        boolean rose = false;
        while (VillagerData.canLevelUp(level) && v.getVillagerXp() >= VillagerData.getMaxXpPerLevel(level)) {
            v.setVillagerData(v.getVillagerData().setLevel(++level));
            rose = true;
        }
        if (rose) {
            v.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            v.level().broadcastEntityEvent(v, (byte) 14);
        }
    }

    // --- a hand-over without a player (the Counter) ---

    /** What a Counter's hand-over came to: taken (and what it paid in emeralds), judged below the line, or not wanted at all. */
    public enum Fate { TAKEN, REJECTED, UNWANTED }

    public record Handed(Fate fate, int emeralds) {
    }

    /**
     * Hands {@code item} to a villager as a delivery would, with nobody standing at the screen. A modular piece is judged against today's
     * undone orders of its type and pays by the same grade and saturation; the first order it satisfies takes it. Anything else is offered to
     * the day's wants and sold up to their cap. {@code item} is shrunk by what was taken. No gossip changes either way: that is a player's
     * standing, and nobody is here.
     */
    public static Handed handOver(Villager v, ItemStack item) {
        long day = today(v);
        Ledger ledger = v.getData(FVillage.LEDGER).on(day);
        ModularDesign design = ModularStacks.design(item);
        ModularFigures figures = DishProfile.read(item, v.level());
        if (design != null && figures != null) {
            List<Order> orders = Guilds.ordersOf(v, day);
            boolean asked = false;
            for (int i = 0; i < orders.size(); i++) {
                Order order = orders.get(i);
                if (ledger.done(i) || !design.type().equals(order.type()))
                    continue;
                asked = true;
                CommissionJudge.Result r = CommissionJudge.judge(order, figures, design, MaterialTable.get().materials());
                if (r.grade() == CommissionJudge.Grade.REJECTED
                        || (Guilds.isGuildOrder(v, i) && r.grade().ordinal() < CommissionJudge.Grade.GOOD.ordinal()))
                    continue;
                String bin = DishProfile.is(item) ? order.type() + "|" + DishProfile.bin(figures) : binKey(order.type(), design);
                int paid = Math.round(order.pay() * r.grade().payFactor() * ledger.payKept(bin, day));
                item.shrink(1);
                v.setData(FVillage.LEDGER, ledger.delivered(i, bin, day));
                if (Guilds.isGuildOrder(v, i))
                    Guilds.markDone(v, day);
                earn(v, r.grade().xp(), day);
                v.playSound(SoundEvents.VILLAGER_YES, 1f, v.getVoicePitch());
                return new Handed(Fate.TAKEN, paid);
            }
            return new Handed(asked ? Fate.REJECTED : Fate.UNWANTED, 0);
        }
        for (int key : wants(v, day)) {
            Want w = BuysTable.get().entries().get(key);
            if (!w.matches(item))
                continue;
            int n = Math.min(item.getCount(), w.max() - ledger.bought(key));
            float fresh = freshness(item, v.level());
            if (n <= 0 || fresh <= 0f)
                continue;
            item.shrink(n);
            v.setData(FVillage.LEDGER, ledger.boughtMore(key, n, day));
            sale(v, VillageTuning.BUY_XP);
            v.playSound(SoundEvents.VILLAGER_YES, 1f, v.getVoicePitch());
            // The villager did not choose to come: a pushed want pays PUSH_RATE of the line, and a stale food half of that.
            return new Handed(Fate.TAKEN, Math.max(1, Math.round(n * w.priceFor(villagerType(v)) * fresh * VillageTuning.PUSH_RATE)));
        }
        return new Handed(Fate.UNWANTED, 0);
    }

    // --- walk-in sales and the market price ---

    /**
     * What a food is worth for how it has kept: 1 fresh, {@link VillageTuning#STALE_FACTOR} going off, 0 rotten (refused). Read from the stack's own
     * spoilage clock at this moment, so what sat on a warm shelf or rode a slow belt prices as it arrives. Anything that does not spoil is 1.
     */
    public static float freshness(ItemStack stack, net.minecraft.world.level.Level level) {
        var entry = io.github.soundgoodizerfan.feedback.process.Cooking.findPerishable(level, stack);
        if (entry.isEmpty())
            return 1f;
        var state = io.github.soundgoodizerfan.feedback.process.Cooking.current(stack, entry.get(), level,
                io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value());
        return switch (io.github.soundgoodizerfan.feedback.process.Cooking.spoilage(state)) {
            case 0 -> 1f;
            case 1 -> VillageTuning.STALE_FACTOR;
            default -> 0f;
        };
    }

    /** The market price of {@code stack}: the highest price any buys line offers for it, 0 when no villager pool wants it. */
    public static int marketPrice(ItemStack stack) {
        int best = 0;
        if (stack.isEmpty())
            return 0;
        for (Want w : BuysTable.get().entries())
            if (w.matches(stack))
                best = Math.max(best, w.price());
        return best;
    }

    /** The most a villager pays for something with this market price: {@link VillageTuning#GOUGE_CEILING} times, never under +1. */
    public static int ceiling(int market) {
        return Math.max(market + 1, Math.round(market * VillageTuning.GOUGE_CEILING));
    }

    /** Why a walk-in did not buy: nothing it wants stands on the shelf, or what it wants is priced past the ceiling. */
    public enum WalkIn { SOLD, NOTHING, TOO_DEAR }

    /** One shelf slot's offer to a villager: the day's want line it matches, or -1. */
    public static int wantFor(Villager v, ItemStack stack, long day) {
        Ledger ledger = v.getData(FVillage.LEDGER).on(day);
        for (int key : wants(v, day)) {
            Want w = BuysTable.get().entries().get(key);
            if (w.matches(stack) && w.max() - ledger.bought(key) > 0)
                return key;
        }
        return -1;
    }

    /**
     * The result of a walk-in sale: what was sold from which slot and for how much. Built by {@link #walkIn}; the Counter applies it.
     *
     * @param fate   whether anything sold
     * @param slot   shelf slot sold from
     * @param count  items sold
     * @param each   emeralds paid each
     * @param market the market price of what sold, to tell a gouge from a fair price
     */
    public record Sale(WalkIn fate, int slot, int count, int each, int market) {
        static final Sale NOTHING = new Sale(WalkIn.NOTHING, -1, 0, 0, 0);
    }

    /**
     * A villager at the front face buys from the shelf: the first slot whose stack matches one of the day's wants it has cap left on,
     * at the keeper's price ({@code prices[slot]}, 0 meaning the line's own price). Priced past the ceiling it will not buy. Writes the
     * ledger (the sale and the errand) and XP; the caller moves the goods and pays the till.
     */
    public static Sale walkIn(Villager v, List<ItemStack> shelf, int[] prices) {
        long day = today(v);
        Ledger ledger = v.getData(FVillage.LEDGER).on(day);
        boolean dear = false;
        for (int slot = 0; slot < shelf.size(); slot++) {
            ItemStack stack = shelf.get(slot);
            if (stack.isEmpty())
                continue;
            for (int key : wants(v, day)) {
                Want w = BuysTable.get().entries().get(key);
                if (!w.matches(stack))
                    continue;
                int cap = w.max() - ledger.bought(key);
                float fresh = freshness(stack, v.level());
                if (cap <= 0 || fresh <= 0f)
                    continue;
                int market = marketPrice(stack);
                int each = prices[slot] > 0 ? prices[slot] : w.priceFor(villagerType(v));
                if (each > ceiling(market)) {
                    dear = true;
                    continue;
                }
                int n = Math.min(stack.getCount(), cap);
                v.setData(FVillage.LEDGER, ledger.boughtMore(key, n, day).errand(day));
                sale(v, VillageTuning.BUY_XP);
                v.playSound(SoundEvents.VILLAGER_YES, 1f, v.getVoicePitch());
                return new Sale(WalkIn.SOLD, slot, n, Math.max(1, Math.round(each * fresh)), market);
            }
        }
        return dear ? new Sale(WalkIn.TOO_DEAR, -1, 0, 0, 0) : Sale.NOTHING;
    }

    /** True when {@code v} still has an unmet want that something on the shelf serves. */
    public static boolean wantsFrom(Villager v, List<ItemStack> shelf) {
        long day = today(v);
        for (ItemStack s : shelf)
            if (!s.isEmpty() && wantFor(v, s, day) >= 0)
                return true;
        return false;
    }

    /** Errands this villager has left today. */
    public static int errandsLeft(Villager v) {
        return VillageTuning.errandsPerDay(level(v)) + (MarketDay.feels(v) ? 1 : 0) - v.getData(FVillage.LEDGER).on(today(v)).errands();
    }

    // --- a purchase ---

    public static void buy(ServerPlayer p, VillagerMenu menu, int slot, int key) {
        AbstractVillager m = menu.merchant();
        if (m == null || !menu.stillValid(p))
            return;
        List<Integer> shop = shop(m);
        if (slot < 0 || slot >= shop.size())
            return;
        int idx = shop.get(slot);
        if (idx != key) { // the shop moved under the click (a level-up): show the new one
            send(p, menu, view(p, m));
            return;
        }
        Good g = GoodsTable.get().entries().get(idx);
        long day = today(m);
        Ledger ledger = m.getData(FVillage.LEDGER).on(day);
        int rep = standing(m, p);
        if (rep < g.standing() || ledger.sold(idx) >= stock(m, g) || (g.weekly() && !weeklyReady(ledger, idx, day))) {
            send(p, menu, view(p, m).withResult(VillagerView.SOLD, -1, false, 0));
            return;
        }
        int cost = price(g, rep);
        if (!takeEmeralds(p, cost)) {
            send(p, menu, view(p, m).withResult(VillagerView.POOR_PURSE, -1, false, 0));
            return;
        }
        p.getInventory().placeItemBackInInventory(stackFor(m, g, idx, day));
        m.setData(FVillage.LEDGER, g.weekly() ? ledger.sold(idx, day).weeklyBought(idx, day) : ledger.sold(idx, day));
        if (m instanceof Villager v)
            sale(v, VillageTuning.BUY_XP);
        p.level().playSound(null, m.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
        send(p, menu, view(p, m));
    }

    // --- a sale ---

    public static void sell(ServerPlayer p, VillagerMenu menu, int slot, int key) {
        AbstractVillager m = menu.merchant();
        if (m == null || !menu.stillValid(p))
            return;
        long day = today(m);
        List<Integer> wanted = wants(m, day);
        if (slot < 0 || slot >= wanted.size() || wanted.get(slot) != key) { // the list moved under the click
            send(p, menu, view(p, m));
            return;
        }
        Want w = BuysTable.get().entries().get(key);
        ItemStack held = menu.delivered();
        if (!w.matches(held)) {
            send(p, menu, view(p, m).withResult(VillagerView.NOT_WANTED, -1, false, 0));
            return;
        }
        Ledger ledger = m.getData(FVillage.LEDGER).on(day);
        float fresh = freshness(held, m.level());
        int n = Math.min(held.getCount(), w.max() - ledger.bought(key));
        if (n <= 0 || fresh <= 0f) {
            send(p, menu, view(p, m).withResult(fresh <= 0f ? VillagerView.NOT_WANTED : VillagerView.FULL, -1, false, 0));
            return;
        }
        int pay = Math.max(1, Math.round(n * w.priceFor(villagerType(m)) * fresh));
        held.shrink(n);
        p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, pay));
        m.setData(FVillage.LEDGER, ledger.boughtMore(key, n, day));
        if (m instanceof Villager v)
            sale(v, VillageTuning.BUY_XP);
        p.level().playSound(null, m.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
        send(p, menu, view(p, m).withResult(VillagerView.SALE, -1, false, pay));
    }

    /** Takes {@code n} emeralds from the inventory, or nothing if it holds fewer. */
    static boolean takeEmeralds(net.minecraft.world.entity.player.Player p, int n) {
        int have = 0;
        for (ItemStack s : p.getInventory().items)
            if (s.is(Items.EMERALD))
                have += s.getCount();
        if (have < n)
            return false;
        int left = n;
        for (ItemStack s : p.getInventory().items) {
            if (left == 0)
                break;
            if (s.is(Items.EMERALD)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }
}
