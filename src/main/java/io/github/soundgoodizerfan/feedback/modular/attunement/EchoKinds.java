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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The runtime kinds of the Echo effect ({@code docs/feedback_enchant_roster.md} 17). Echo+ : {@code echo_blow} on a
 * head, blade or tip repeats a landed blow on the same target a beat later at a share of its damage; {@code echo_wear}
 * on a plate or studs makes the plate take extra durability damage on every hit the wearer takes (the damage itself is
 * unchanged). Echo- (Lag): {@code lag_blow} on a head, blade or tip makes the swing land only after a delay;
 * {@code lag_hit} on a plate or studs spreads a share of each hit the wearer takes over the next few seconds.
 * Delayed work lives in a small tick queue here. Nothing in it wears a part: wear comes only from the hit taken
 * ({@code echo_wear}), capped one short of breaking, and a delayed swing or delayed damage costs nothing of its own.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class EchoKinds {

    /** Share of the blow's damage one level of echo repeats, and the most that can be. [OPEN] until played. */
    private static final float ECHO_SHARE_PER_LEVEL = 0.4f, ECHO_SHARE_CAP = 1f;
    /** Ticks before the echo lands; over the target's 10-tick hurt immunity, or it would be swallowed. [OPEN] until played. */
    private static final int ECHO_DELAY = 12;
    /** Extra durability an echoing plate loses per level on each hit its wearer takes. [OPEN] until played. */
    private static final int ECHO_WEAR_PER_LEVEL = 1;
    /** Ticks a lagging swing waits before it lands. [OPEN] until played. */
    private static final int LAG_SWING_DELAY = 60;
    /** Share of a hit one level of lag puts off, and the most that can be. [OPEN] until played. */
    private static final float LAG_SHARE_PER_LEVEL = 0.35f, LAG_SHARE_CAP = 0.9f;
    /** The deferred damage arrives in this many equal parts, this many ticks apart. [OPEN] until played. */
    private static final int LAG_PARTS = 4, LAG_PART_GAP = 20;

    private record Task(long due, Runnable run) {
    }

    private static final List<Task> QUEUE = new ArrayList<>();
    /** True while a delayed hit is being delivered, so it is not echoed, delayed or deferred again. */
    private static boolean replaying;

    private EchoKinds() {
    }

    private static void later(LivingEntity at, int ticks, Runnable run) {
        MinecraftServer server = at.level().getServer();
        if (server != null)
            QUEUE.add(new Task(server.getTickCount() + ticks, run));
    }

    /** Land {@code amount} on {@code victim} from {@code source}, flagged so no echo kind reacts to it. */
    private static void deliver(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!victim.isAlive())
            return;
        replaying = true;
        try {
            victim.hurt(source, amount);
        } finally {
            replaying = false;
        }
    }

    public static void register() {
        AttunementKinds.onStrike("echo_blow", (event, a) -> {
            if (replaying)
                return;
            LivingEntity victim = event.getEntity();
            var source = event.getSource();
            float share = Math.min(ECHO_SHARE_CAP, a.level() * ECHO_SHARE_PER_LEVEL);
            float amount = event.getAmount() * share;
            later(victim, ECHO_DELAY, () -> deliver(victim, source, amount));
        });
        AttunementKinds.onStrike("lag_blow", (event, a) -> {
            if (replaying)
                return;
            LivingEntity victim = event.getEntity();
            var source = event.getSource();
            float amount = event.getAmount();
            event.setCanceled(true);
            later(victim, LAG_SWING_DELAY, () -> deliver(victim, source, amount));
        });
    }

    private static int worn(LivingEntity wearer, String kind) {
        int n = 0;
        for (ItemStack stack : wearer.getArmorSlots())
            n += TideKinds.level(stack, kind);
        return n;
    }

    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (replaying || wearer.level().isClientSide || event.getAmount() <= 0f)
            return;
        for (ItemStack stack : wearer.getArmorSlots()) {
            if (stack.isEmpty())
                continue;
            int level = 0;
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
                if (a.kind().equals("echo_wear"))
                    level += a.level();
            if (level > 0)
                stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + level * ECHO_WEAR_PER_LEVEL));
        }
        int lag = worn(wearer, "lag_hit");
        if (lag == 0)
            return;
        float later = event.getAmount() * Math.min(LAG_SHARE_CAP, lag * LAG_SHARE_PER_LEVEL);
        event.setAmount(event.getAmount() - later);
        float part = later / LAG_PARTS;
        for (int i = 1; i <= LAG_PARTS; i++)
            later(wearer, i * LAG_PART_GAP, () -> deliver(wearer, wearer.damageSources().generic(), part));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty())
            return;
        long now = event.getServer().getTickCount();
        List<Task> due = new ArrayList<>();
        QUEUE.removeIf(t -> t.due <= now && due.add(t));
        for (Task t : due)
            t.run.run();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        QUEUE.clear();
    }
}
