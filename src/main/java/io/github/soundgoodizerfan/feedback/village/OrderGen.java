// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.village.CommissionProfile.Axis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Works out a villager's orders for a day from nothing but who they are, what they do, how far
 * they have risen and the day number, so nothing is stored and nothing ticks. The same inputs give
 * the same orders on either side and across a restart.
 */
public final class OrderGen {

    private OrderGen() {
    }

    public static int count(int level) {
        return VillageTuning.ORDERS_BASE + level;
    }

    /** Half-width of a band for a villager of this level: wide for a novice, narrowing with trade level. */
    public static float tolerance(int level) {
        return Math.max(VillageTuning.TOLERANCE_FLOOR, VillageTuning.TOLERANCE_NOVICE - VillageTuning.TOLERANCE_STEP * (level - 1));
    }

    public static List<Order> orders(UUID villager, ResourceLocation profession, String villagerType, int level, long day) {
        List<CommissionProfile.Entry> pool = CommissionTable.get().forProfession(profession).stream()
                .filter(e -> e.level() <= level && ModularTypes.known().containsKey(e.type()) && (!e.axes().isEmpty() || !dish(e.type()))).toList();
        List<Order> out = new ArrayList<>();
        if (pool.isEmpty())
            return out;
        for (int i = 0; i < count(level); i++) {
            RandomSource rnd = RandomSource.create(mix(villager.getMostSignificantBits(), villager.getLeastSignificantBits(), day, i));
            out.add(build(villager, pool.get(rnd.nextInt(pool.size())), villagerType, level, rnd));
        }
        return out;
    }

    private static boolean dish(ResourceLocation type) {
        ModularType t = ModularTypes.known(type);
        return t != null && t.family() == ModularType.Family.DISH;
    }

    /**
     * A tool order names parts, not figures: one part at a novice, more with level, each a material (or, now and then, a class of
     * material). The primary part is always asked first. What is asked is always something the part can be made of, and soft
     * materials come before hard ones as the villager rises.
     */
    private static Order tool(CommissionProfile.Entry e, int level, RandomSource rnd) {
        ModularType type = ModularTypes.known(e.type());
        List<String> roles = new ArrayList<>(type.roles().keySet());
        roles.removeIf(r -> choices(type, r, level).isEmpty());
        boolean primary = roles.remove(type.primary());
        java.util.Collections.shuffle(roles, new java.util.Random(rnd.nextLong()));
        if (primary)
            roles.add(0, type.primary());
        List<Order.Ask> asks = new ArrayList<>();
        for (String role : roles.subList(0, Math.min(roles.size(), 1 + level / 2))) {
            List<Material.Located> choices = choices(type, role, level);
            Material.Located pick = choices.get(rnd.nextInt(choices.size()));
            boolean asClass = rnd.nextFloat() < VillageTuning.CLASS_ASK_SHARE && pick.material().kind().isPresent();
            asks.add(new Order.Ask(role, asClass ? null : pick.id(), asClass ? pick.material().kind().get() : null));
        }
        return new Order(e.type(), e.pay(), List.of(), List.of(), List.of(), List.copyOf(asks));
    }

    /** What a villager of this level may ask of a part: the materials it can be made of, no harder than the level's cap, in id order. */
    private static List<Material.Located> choices(ModularType type, String role, int level) {
        float cap = VillageTuning.ASK_HARDNESS_CAP[Math.min(level, VillageTuning.ASK_HARDNESS_CAP.length) - 1];
        List<Material.Located> out = new ArrayList<>(ToolDesigns.materials(type, role, ToolDesigns.standard(type.roles().get(role), ModularTypes.known()),
                MaterialTable.get().materials()));
        out.removeIf(l -> l.material().hardness().value() > cap);
        out.sort(Comparator.comparing(l -> l.id().toString()));
        return out;
    }

    private static Order build(UUID villager, CommissionProfile.Entry e, String villagerType, int level, RandomSource rnd) {
        if (!dish(e.type()))
            return tool(e, level, rnd);
        List<Axis> axes = new ArrayList<>(e.axes());
        for (int i = axes.size() - 1; i > 0; i--)
            java.util.Collections.swap(axes, i, rnd.nextInt(i + 1));
        int stated = Math.min(axes.size(), 2 + (level >= 3 ? 1 : 0));
        float tol = tolerance(level);
        List<Order.Term> terms = new ArrayList<>();
        for (int i = 0; i < stated; i++) {
            Axis a = axes.get(i);
            if (i == 0) {
                boolean above = rnd.nextBoolean();
                float frac = above ? 0.25f + rnd.nextFloat() * 0.3f : 0.45f + rnd.nextFloat() * 0.3f;
                terms.add(new Order.Term(a.fig(), a.lo(), a.hi(), a.lo() + frac * a.range(), 0f, true, above));
            } else {
                float frac = 0.2f + rnd.nextFloat() * 0.6f;
                terms.add(new Order.Term(a.fig(), a.lo(), a.hi(), a.lo() + frac * a.range(), tol, false, false));
            }
        }
        List<Order.Term> quirks = new ArrayList<>();
        long h = mix(villager.getMostSignificantBits(), villager.getLeastSignificantBits(), e.type().hashCode(), 7);
        int all = e.axes().size();
        for (int step = 0; step < all; step++) {
            Axis a = e.axes().get((int) Math.floorMod(h + step, (long) all));
            if (terms.stream().anyMatch(t -> t.fig() == a.fig()))
                continue;
            float frac = 0.2f + 0.6f * (((h >>> 8) & 1023) / 1023f);
            quirks.add(new Order.Term(a.fig(), a.lo(), a.hi(), a.lo() + frac * a.range(), tol, false, false));
            break;
        }
        // The biome's lean on axes the stated terms leave alone: what the villager's kind of place likes.
        List<Order.Term> leans = new ArrayList<>();
        for (Axis a : e.leanFor(villagerType))
            if (terms.stream().noneMatch(t -> t.fig() == a.fig()))
                leans.add(new Order.Term(a.fig(), a.lo(), a.hi(), (a.lo() + a.hi()) * 0.5f, tol, false, false));
        return new Order(e.type(), e.pay(), List.copyOf(terms), List.copyOf(quirks), List.copyOf(leans), List.of());
    }

    private static long mix(long... parts) {
        long h = 0x9E3779B97F4A7C15L;
        for (long p : parts) {
            h ^= p + 0x9E3779B97F4A7C15L + (h << 6) + (h >>> 2);
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 31;
        }
        return h;
    }
}
