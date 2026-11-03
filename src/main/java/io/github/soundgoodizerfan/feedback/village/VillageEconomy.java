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
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
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
        return m instanceof Villager v ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession()) : WANDERER;
    }

    public static int level(AbstractVillager m) {
        return m instanceof Villager v ? v.getVillagerData().getLevel() : 1;
    }

    private static long today(AbstractVillager m) {
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
            for (Order o : OrderGen.orders(m.getUUID(), profession(m), level, day)) {
                List<TermView> terms = new ArrayList<>();
                for (Order.Term t : o.terms())
                    terms.add(new TermView(t.fig(), t.band(), t.must(), t.above()));
                orders.add(new OrderView(o.type(), o.pay(), List.copyOf(terms)));
            }
        }
        List<GoodView> goods = new ArrayList<>();
        for (int idx : shop(m)) {
            Good g = GoodsTable.get().entries().get(idx);
            goods.add(new GoodView(stackFor(m, g, idx, day), price(g, rep), Math.max(0, g.stock() - ledger.sold(idx)), rep < g.standing(), idx));
        }
        List<VillagerView.WantView> wants = new ArrayList<>();
        for (int idx : wants(m, day)) {
            Want w = BuysTable.get().entries().get(idx);
            wants.add(new VillagerView.WantView(w.icon(), w.food(), w.price(), Math.max(0, w.max() - ledger.bought(idx)), idx));
        }
        int xp = 0, xpMax = 0;
        if (m instanceof Villager v && VillagerData.canLevelUp(level)) {
            xp = v.getVillagerXp() - VillagerData.getMinXpPerLevel(level);
            xpMax = VillagerData.getMaxXpPerLevel(level) - VillagerData.getMinXpPerLevel(level);
        }
        return new VillagerView(wanderer, level, xp, xpMax, rep, List.copyOf(orders), ledger.doneMask(), List.copyOf(goods), List.copyOf(wants),
                VillagerView.NONE, -1, false, 0);
    }

    /** A blueprint of the named design: a gun pool entry, else the starter design of a modular type; empty if neither exists. */
    private static ItemStack blueprint(ResourceLocation id) {
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
        for (int i = 0; i < all.size(); i++)
            if (all.get(i).profession().equals(prof) && all.get(i).level() <= level
                    && all.get(i).blueprint().map(b -> !blueprint(b).isEmpty()).orElse(true))
                out.add(i);
        return out;
    }

    /** Indices into the buys table of what this merchant wants today: four to six lines of the shared and profession pools. */
    private static List<Integer> wants(AbstractVillager m, long day) {
        ResourceLocation prof = profession(m);
        List<Want> all = BuysTable.get().entries();
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < all.size(); i++)
            if (all.get(i).profession().equals(Want.ANY) || all.get(i).profession().equals(prof))
                pool.add(i);
        RandomSource rnd = RandomSource.create(m.getUUID().getMostSignificantBits() * 31L + m.getUUID().getLeastSignificantBits() + day * 0x9E3779B97F4A7C15L);
        java.util.Collections.shuffle(pool, new java.util.Random(rnd.nextLong()));
        List<Integer> picked = new ArrayList<>(pool.subList(0, Math.min(pool.size(), VillageTuning.WANTS_MIN + rnd.nextInt(VillageTuning.WANTS_MAX - VillageTuning.WANTS_MIN + 1))));
        java.util.Collections.sort(picked);
        return picked;
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

    // --- a delivery ---

    public static void deliver(ServerPlayer p, VillagerMenu menu, int index, int key) {
        AbstractVillager m = menu.merchant();
        if (!(m instanceof Villager v) || !menu.stillValid(p))
            return;
        long day = today(v);
        Ledger ledger = v.getData(FVillage.LEDGER).on(day);
        List<Order> orders = OrderGen.orders(v.getUUID(), profession(v), level(v), day);
        if (index < 0 || index >= orders.size() || ledger.done(index))
            return;
        Order order = orders.get(index);
        if (order.type().hashCode() != key) { // the list moved under the click (a level-up): show the new one
            send(p, menu, view(p, v));
            return;
        }
        ItemStack item = menu.delivered();
        ModularDesign design = ModularStacks.design(item);
        ModularFigures figures = ModularStacks.figures(item);
        if (design == null || figures == null || !design.type().equals(order.type())) {
            send(p, menu, view(p, v).withResult(VillagerView.WRONG, -1, false, 0));
            return;
        }
        CommissionJudge.Result r = CommissionJudge.judge(order, figures);
        if (r.grade() == CommissionJudge.Grade.REJECTED) {
            v.getGossips().add(p.getUUID(), GossipType.MINOR_NEGATIVE, VillageTuning.GOSSIP_REJECTED);
            v.playSound(SoundEvents.VILLAGER_NO, 1f, v.getVoicePitch());
            send(p, menu, view(p, v).withResult(r.grade().ordinal(), r.worst() == null ? -1 : r.worst().ordinal(), r.worstHigh(), 0));
            return;
        }
        String bin = binKey(order.type(), design);
        int paid = Math.round(order.pay() * r.grade().payFactor() * ledger.payKept(bin, day));
        menu.consume();
        if (paid > 0)
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, paid));
        v.setData(FVillage.LEDGER, ledger.delivered(index, bin, day));
        if (r.grade() == CommissionJudge.Grade.EXCEPTIONAL)
            v.getGossips().add(p.getUUID(), GossipType.MAJOR_POSITIVE, VillageTuning.GOSSIP_EXCEPTIONAL);
        else if (r.grade() == CommissionJudge.Grade.GOOD)
            v.getGossips().add(p.getUUID(), GossipType.MAJOR_POSITIVE, VillageTuning.GOSSIP_GOOD);
        addXp(v, r.grade().xp());
        v.playSound(SoundEvents.VILLAGER_YES, 1f, v.getVoicePitch());
        send(p, menu, view(p, v).withResult(r.grade().ordinal(), r.worst() == null ? -1 : r.worst().ordinal(), r.worstHigh(), paid));
    }

    /** The saturation bin of a delivery: the type and the kind of its primary part's material. */
    private static String binKey(ResourceLocation type, ModularDesign design) {
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

    private static void addXp(Villager v, int xp) {
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
        if (rep < g.standing() || ledger.sold(idx) >= g.stock()) {
            send(p, menu, view(p, m).withResult(VillagerView.SOLD, -1, false, 0));
            return;
        }
        int cost = price(g, rep);
        if (!takeEmeralds(p, cost)) {
            send(p, menu, view(p, m).withResult(VillagerView.POOR_PURSE, -1, false, 0));
            return;
        }
        p.getInventory().placeItemBackInInventory(stackFor(m, g, idx, day));
        m.setData(FVillage.LEDGER, ledger.sold(idx, day));
        if (m instanceof Villager v)
            addXp(v, VillageTuning.BUY_XP);
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
        int n = Math.min(held.getCount(), w.max() - ledger.bought(key));
        if (n <= 0) {
            send(p, menu, view(p, m).withResult(VillagerView.FULL, -1, false, 0));
            return;
        }
        held.shrink(n);
        p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, n * w.price()));
        m.setData(FVillage.LEDGER, ledger.boughtMore(key, n, day));
        if (m instanceof Villager v)
            addXp(v, VillageTuning.BUY_XP);
        p.level().playSound(null, m.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
        send(p, menu, view(p, m).withResult(VillagerView.SALE, -1, false, n * w.price()));
    }

    /** Takes {@code n} emeralds from the inventory, or nothing if it holds fewer. */
    private static boolean takeEmeralds(ServerPlayer p, int n) {
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
