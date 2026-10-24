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
 */
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Momentum effect ({@code docs/feedback_enchant_roster.md} 19). {@code momentum}: a blow
 * after a straight sprint hits harder and knocks further. {@code brace}: the mirror, a blow after standing still.
 *
 * <p>The run is tracked here from per-tick positions, not read from the spear: a spear jab after a sprint counts, and
 * so does {@code SpearCharge}'s charge, because both arrive as an ordinary blow while the wielder is still running.
 * Cost is one map lookup and a few floats per player per tick; nothing is allocated.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MomentumKinds {

    /** Extra damage share per block run per level, and the most that can be. [OPEN] until played. */
    private static final float DAMAGE_PER_BLOCK = 0.03f, DAMAGE_CAP = 0.75f;
    /** Knockback strength per block run per level, and the most that can be. [OPEN] until played. */
    private static final float KNOCKBACK_PER_BLOCK = 0.03f, KNOCKBACK_CAP = 1.0f;
    /** Longest run that counts, in blocks. [OPEN] until played. */
    private static final float RUN_CAP = 16f;
    /** Ticks off the sprint (or standing) before a run is forgotten; a swing drops the sprint. [OPEN] until played. */
    private static final int RUN_GRACE = 6;
    /** Cosine of the sharpest per-tick turn that keeps a run straight. [OPEN] until played. */
    private static final double STRAIGHT_COS = 0.9;
    /** Horizontal blocks a tick under which the wielder is standing still. [OPEN] until played. */
    private static final double STILL_STEP = 0.01;
    /** Brace: extra damage share per second stood still per level, the most that can be, and the longest wait counted. [OPEN] until played. */
    private static final float BRACE_PER_SECOND = 0.05f, BRACE_CAP = 0.6f;
    private static final int STILL_CAP_TICKS = 400;

    /** Worn legs/boots: speed share per block run per level, and the most that can be. [OPEN] until played. */
    private static final float SPRINT_PER_BLOCK = 0.01f, SPRINT_CAP = 0.4f;
    /** Worn stance: damage cut per second stood still per level, and the most that can be. [OPEN] until played. */
    private static final float STANCE_PER_SECOND = 0.03f, STANCE_CAP = 0.5f;
    /** Ticks between re-reading the armour, and between speed updates. */
    private static final int WORN_RECHECK = 10, SPEED_PERIOD = 5;
    private static final ResourceLocation SPRINT_ID = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "momentum_worn_sprint");

    private static final class Track {
        int wornSprint;
        boolean seen;
        double x, z, hx, hz;
        float run;
        int gap, still;
    }

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private MomentumKinds() {
    }

    public static void register() {
        onStrike("momentum", (event, a) -> {
            Track t = track(event);
            if (t == null || t.run <= 0f)
                return;
            event.setAmount(event.getAmount() * (1f + Math.min(DAMAGE_CAP, t.run * DAMAGE_PER_BLOCK * a.level())));
            float kb = Math.min(KNOCKBACK_CAP, t.run * KNOCKBACK_PER_BLOCK * a.level());
            LivingEntity victim = event.getEntity();
            // knockback() pushes away from the point it is given, so give it the attacker's side.
            var attacker = event.getSource().getEntity();
            victim.knockback(kb, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
        });
        onStrike("brace", (event, a) -> {
            Track t = track(event);
            if (t == null || t.still <= 0)
                return;
            event.setAmount(event.getAmount()
                    * (1f + Math.min(BRACE_CAP, t.still / 20f * BRACE_PER_SECOND * a.level())));
        });
    }

    private static Track track(LivingIncomingDamageEvent event) {
        return event.getSource().getEntity() instanceof ServerPlayer p ? TRACKS.get(p.getUUID()) : null;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Track t = TRACKS.computeIfAbsent(p.getUUID(), k -> new Track());
        wornSpeed(p, t);
        double dx = p.getX() - t.x, dz = p.getZ() - t.z;
        t.x = p.getX();
        t.z = p.getZ();
        if (!t.seen) {
            t.seen = true;
            return;
        }
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < STILL_STEP) {
            t.still = Math.min(STILL_CAP_TICKS, t.still + 1);
            if (++t.gap > RUN_GRACE)
                t.run = 0f;
            return;
        }
        t.still = 0;
        if (!p.isSprinting()) {
            if (++t.gap > RUN_GRACE)
                t.run = 0f;
            return;
        }
        double hx = dx / d, hz = dz / d;
        // shortcut: only a sharp turn within one tick breaks a run, so a wide arc still counts; tighten if it plays wrong.
        if (t.run > 0f && hx * t.hx + hz * t.hz < STRAIGHT_COS)
            t.run = 0f;
        t.hx = hx;
        t.hz = hz;
        t.gap = 0;
        t.run = (float) Math.min(RUN_CAP, t.run + d);
    }

    /**
     * Legs or boots part with {@code worn_sprint}: the longer the straight run (the same run a blow reads), the faster,
     * to a cap; the run resets on stopping or a sharp turn, so the speed does too. A transient multiplier, synced
     * every few ticks, taken off when the part comes off.
     */
    private static void wornSpeed(ServerPlayer p, Track t) {
        if (p.tickCount % WORN_RECHECK == 0)
            t.wornSprint = BaitKinds.wornAt(p, EquipmentSlot.LEGS, "worn_sprint") + BaitKinds.wornAt(p, EquipmentSlot.FEET, "worn_sprint");
        if (p.tickCount % SPEED_PERIOD != 0)
            return;
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
            return;
        if (t.wornSprint > 0 && t.run > 0f)
            speed.addOrUpdateTransientModifier(new AttributeModifier(SPRINT_ID,
                    Math.min(SPRINT_CAP, t.run * SPRINT_PER_BLOCK * t.wornSprint), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (speed.hasModifier(SPRINT_ID))
            speed.removeModifier(SPRINT_ID);
    }

    /** Armour with {@code worn_stance} (any piece): standing still builds a cut in damage taken; moving resets it. */
    @SubscribeEvent
    public static void onWearerHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Track t = TRACKS.get(p.getUUID());
        if (t == null || t.still <= 0)
            return;
        int n = BaitKinds.worn(p, "worn_stance");
        if (n > 0)
            event.setAmount(event.getAmount() * (1f - Math.min(STANCE_CAP, t.still / 20f * STANCE_PER_SECOND * n)));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }
}
