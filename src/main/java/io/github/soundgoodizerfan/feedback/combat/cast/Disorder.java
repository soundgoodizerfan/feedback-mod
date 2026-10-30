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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * What the Chaos tree leaves a target with. {@link #note} pays the player a point for a kind of disorder (each
 * kind decays per target in the skill pool, so one trick on one mob pays little); {@link #wearArmour} and
 * {@link #selfWear} are the hardness-against-hardness wear every breaking cast shares; a body set burning by the
 * tree passes the fire to those standing against it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Disorder {

    /** The kinds of disorder a target can be left in; Chaos pays once per kind per target (decaying). */
    public enum Kind { BURNING, WORN, STRIPPED, COLLIDED, BLINDED, TRIPPED, PINNED, POWDERED, PANICKED }

    /** STAND-IN: wear points a strike of equal hardness puts on a piece, and the ratio limits. */
    static final int BASE_WEAR = 6;
    /** STAND-IN: the hardness of a bare heel, a hardened boot. */
    static final float IRON_HEEL = ModularTuning.IRON_CLASS_HARDNESS;
    static final float MIN_RATIO = 0.25f, MAX_RATIO = 4f;
    /** STAND-INS: fire passes to bodies within this many blocks every this many ticks, for this many generations and seconds. */
    private static final double SPREAD_REACH = 1.6;
    private static final int SPREAD_EVERY = 10, SPREAD_GENERATIONS = 3, SPREAD_SECONDS = 4;

    private Disorder() {
    }

    /** {@code p} left {@code target} in a {@code kind} of disorder. */
    public static void note(ServerPlayer p, LivingEntity target, Kind kind) {
        SkillEarning.disorder(p, target, kind.name());
    }

    /** A stack's material hardness: the baked figure, else iron class (unbaked gear is read as iron). */
    static float hardness(ItemStack s) {
        var f = ModularStacks.figures(s);
        return f == null ? ModularTuning.IRON_CLASS_HARDNESS : Math.max(0.1f, f.get(Fig.HARDNESS));
    }

    /** Wear points when {@code striker} hardness meets {@code struck}: harder wears more, doubled by {@code mult}. */
    static int wear(float striker, float struck, float mult) {
        return Math.max(1, Math.round(BASE_WEAR * mult * Mth.clamp(striker / struck, MIN_RATIO, MAX_RATIO)));
    }

    /** Wears every damageable armour piece {@code t} has on by {@code mine} against its hardness; returns the hardest piece's hardness, 0 if nothing was worn. */
    static float wearArmour(LivingEntity t, float mine) {
        float hardest = 0f;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = t.getItemBySlot(slot);
            if (!worn.isDamageableItem())
                continue;
            float h = hardness(worn);
            worn.hurtAndBreak(wear(mine, h, 1f), t, slot);
            hardest = Math.max(hardest, h);
        }
        return hardest;
    }

    /** What the blow costs the striker's own weapon: the struck thing's hardness against it, half as much, never the last point. */
    static void selfWear(ServerPlayer p, float struck) {
        ItemStack w = p.getMainHandItem();
        if (w.isDamageableItem())
            w.hurtAndBreak(ModularStacks.cap(w, wear(struck, hardness(w), 0.5f)), p, EquipmentSlot.MAINHAND);
    }

    private record Burn(LivingEntity body, UUID owner, int generation) {
    }

    private static final Map<UUID, Burn> BURNING = new HashMap<>();

    /** Sets {@code t} burning for {@code seconds} on {@code p}'s account; the fire then passes to those beside it. */
    static void ignite(ServerPlayer p, LivingEntity t, float seconds) {
        t.igniteForSeconds(seconds);
        BURNING.put(t.getUUID(), new Burn(t, p.getUUID(), 0));
        note(p, t, Kind.BURNING);
    }

    /** Adds {@code t}, already burning from a Chaos blow, to the spreading fire. */
    public static void kindled(ServerPlayer p, LivingEntity t) {
        if (t.isOnFire())
            BURNING.putIfAbsent(t.getUUID(), new Burn(t, p.getUUID(), 0));
        note(p, t, Kind.BURNING);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (BURNING.isEmpty() || event.getServer().getTickCount() % SPREAD_EVERY != 0)
            return;
        Map<UUID, Burn> caught = new HashMap<>();
        BURNING.values().removeIf(b -> {
            if (!b.body().isAlive() || !b.body().isOnFire())
                return true;
            if (b.generation() >= SPREAD_GENERATIONS)
                return false;
            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(b.owner());
            for (LivingEntity n : b.body().level().getEntitiesOfClass(LivingEntity.class, b.body().getBoundingBox().inflate(SPREAD_REACH))) {
                if (n == b.body() || n == owner || n.isOnFire() || !n.isAlive() || owner != null && owner.isAlliedTo(n))
                    continue;
                n.igniteForSeconds(SPREAD_SECONDS);
                caught.put(n.getUUID(), new Burn(n, b.owner(), b.generation() + 1));
                if (owner != null)
                    note(owner, n, Kind.BURNING);
                if (b.body().level() instanceof net.minecraft.server.level.ServerLevel lv)
                    lv.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, n.getX(), n.getY() + n.getBbHeight() * 0.5, n.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
            }
            return false;
        });
        BURNING.putAll(caught);
    }
}
