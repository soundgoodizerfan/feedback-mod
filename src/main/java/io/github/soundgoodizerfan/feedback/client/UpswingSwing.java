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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.OffhandStrike;
import io.github.soundgoodizerfan.feedback.combat.hit.Wielding;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.net.OffhandStrikePayload;
import io.github.soundgoodizerfan.feedback.net.SwingPlayPayload;
import io.github.soundgoodizerfan.feedback.net.SwingStartPayload;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The melee swing's upswing, Better Combat's mechanic on our own backend. Better Combat's rendering is
 * kept whole -- its attack animation player (speed gearing, mirroring for the left hand, first-person
 * arms, fades), its clips and its sounds, reached through {@link PlayerAttackAnimatable} and by id -- and
 * everything it would read from a weapon-attributes file is decided here, from the weapon's own baked
 * figures, on the fly.
 *
 * <p>A click on an unbroken modular blade, mace or spear, or on an empty hand, does not attack at once. Dual wielding
 * ({@link Wielding}) gives each hand its own button, never alternating: left click is the main hand, right click the off hand
 * (its strike being {@code OffhandStrike} on the server); a two-handed
 * weapon takes the {@code two_handed_*} clips and shuts the off hand.
 * It starts the clip, and the blow (vanilla's attack on whatever the crosshair then holds, or the swept
 * volume for a swing the pick missed, exactly as before) lands when the clip's strike does: {@code length}
 * ticks in all, the strike at {@code length * upswing}. {@code length} is the weapon's own attack delay, so
 * attack speed and Haste still set the pace; the upswing grows with how heavy the weapon is
 * ({@link ParryTuning#heaviness}), so a dagger is quick to land and a maul is not. The server hears
 * nothing of the windup: the attack packet simply arrives later, and {@code MeleeSweep} rides it as it
 * always has. What it relays is the animation ({@link SwingStartPayload}), so everyone sees the windup
 * begin when it does.
 *
 * <p>Holding the click never repeats a swing: each swing needs the button let go and pressed again (Better Combat's
 * hold-to-keep-attacking is deliberately not carried over; ruled by the author 2026-10-24).
 *
 * <p>A stagger, or any combat move (parry, roll, kick), in the windup cancels the swing: the blow is lost
 * and the clip stops. A click during a windup is swallowed, and one while the cooldown is under
 * {@link #READY} is too, so a swing never starts on a half-charged weapon.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class UpswingSwing {

    /** Cooldown charge below which a click is not a swing: a swing then lands, after its upswing, near full charge. */
    private static final float READY = 0.5f;
    /** A swing is never shorter than this many ticks, whatever the attack speed (a fist's delay is five). */
    private static final float MIN_LENGTH = 10f;

    private record Swing(String clip, AnimatedHand hand, InteractionHand arm, float length, float upswing, String sound) {
    }

    private record Pending(long strikeAt, Swing swing) {
    }

    /** Another player's strike sound, due at a game time. */
    private record RemoteStrike(int entity, long at, String sound) {
    }

    private static Pending pending;
    private static final List<RemoteStrike> REMOTE = new ArrayList<>();
    /** Which of a weapon's two clips comes next, so a swing and its follow-up differ. */
    private static int combo;
    /** A swing started from a press: the next needs the button let go first, so holding the click never repeats a swing. */
    private static boolean needRelease;
    /** Game time of the last off-hand strike, for its own cooldown gate (the server keeps the real one). */
    private static long lastOffhand = -1000;

    private UpswingSwing() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || event.isCanceled())
            return;
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.gameMode == null || mc.screen != null || !player.isAlive() || player.isSpectator()
                || player.isUsingItem() || (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK))
            return;
        if (player.hasData(FCombat.STATE) && CombatState.of(player).phase() != CombatPhase.IDLE)
            return;
        Swing swing = choose(player, InteractionHand.MAIN_HAND);
        if (swing == null)
            return;
        // Ours from here: nothing of vanilla's attack runs, now or on a click that cannot start a swing.
        event.setCanceled(true);
        event.setSwingHand(false);
        if (needRelease)
            return;
        if (begin(mc, player, swing))
            needRelease = true;
    }

    /**
     * The off hand asked to strike (right click or its key): the same windup as a left click, off-hand clip
     * and all. Does nothing while a windup runs, the off hand is shut by a two-handed weapon, or it cannot strike.
     */
    public static void requestOffhand() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.gameMode == null || !player.isAlive() || player.isSpectator() || player.isUsingItem()
                || (player.hasData(FCombat.STATE) && CombatState.of(player).phase() != CombatPhase.IDLE)
                || Wielding.offHandBlocked(player) || !OffhandStrike.strikes(player.getOffhandItem()))
            return;
        Swing swing = choose(player, InteractionHand.OFF_HAND);
        if (swing != null)
            begin(mc, player, swing);
    }

    /** Starts the windup if none runs and the hand is charged enough; true if it started. */
    private static boolean begin(Minecraft mc, LocalPlayer player, Swing swing) {
        long now = mc.level.getGameTime();
        boolean ready = swing.arm() == InteractionHand.OFF_HAND
                ? now - lastOffhand >= OffhandStrike.cooldownTicks(player, player.getOffhandItem()) * READY
                : player.getAttackStrengthScale(0f) >= READY;
        if (pending != null || !ready)
            return false;
        pending = new Pending(now + Math.round(swing.length() * swing.upswing()), swing);
        play(player, swing.clip(), swing.hand(), swing.length(), swing.upswing());
        PacketDistributor.sendToServer(new SwingStartPayload(swing.clip(), swing.hand().ordinal(), swing.length(),
                swing.upswing(), swing.sound()));
        return true;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) {
            pending = null;
            REMOTE.clear();
            return;
        }
        long now = level.getGameTime();
        if (!mc.options.keyAttack.isDown())
            needRelease = false;
        if (pending != null) {
            boolean broken = !player.isAlive() || (player.hasData(FCombat.STATE) && CombatState.of(player).phase() != CombatPhase.IDLE);
            if (broken) {
                stop(player, 4f);
                pending = null;
            } else if (now >= pending.strikeAt()) {
                strike(mc, player, pending.swing());
                pending = null;
            }
        }
        for (Iterator<RemoteStrike> it = REMOTE.iterator(); it.hasNext(); ) {
            RemoteStrike r = it.next();
            if (now >= r.at()) {
                if (level.getEntity(r.entity()) instanceof Player p)
                    sound(p, r.sound());
                it.remove();
            }
        }
    }

    /** The moment the clip strikes: the blow vanilla's click would have made (the server's, for the off hand), and the swing's sound. */
    private static void strike(Minecraft mc, Player player, Swing swing) {
        if (swing.arm() == InteractionHand.OFF_HAND) {
            lastOffhand = mc.level.getGameTime();
            PacketDistributor.sendToServer(new OffhandStrikePayload());
            player.swing(InteractionHand.OFF_HAND);
        } else {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity().isAlive())
                mc.gameMode.attack(player, hit.getEntity());
            else {
                MeleeSweepClient.sweepIfExpected(player);
                player.resetAttackStrengthTicker();
            }
            player.swing(InteractionHand.MAIN_HAND);
        }
        sound(player, swing.sound());
    }

    /** What to play for {@code player}'s {@code arm}, or null if this is not a swing we handle. */
    private static Swing choose(Player player, InteractionHand arm) {
        boolean off = arm == InteractionHand.OFF_HAND;
        ItemStack held = off ? player.getOffhandItem() : player.getMainHandItem();
        float delay = Math.max(MIN_LENGTH, off ? (float) OffhandStrike.cooldownTicks(player, held) : player.getCurrentItemAttackStrengthDelay());
        int next = combo++;
        // A dual wielder's two hands swing the same clip, the off hand mirrored; one hand alternates its two.
        boolean dual = Wielding.dualWielding(player);
        boolean right = dual || off ? true : next % 2 == 0;
        AnimatedHand hand = off ? AnimatedHand.OFF_HAND : AnimatedHand.MAIN_HAND;
        if (held.isEmpty())
            return off ? null : new Swing("bettercombat:" + (right ? "one_handed_punch" : "one_handed_uppercut_right"),
                    hand, arm, delay, 0.3f, "bettercombat:fist_punch");
        var design = ModularStacks.design(held);
        if (design == null || ModularStacks.broken(held))
            return null;
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null)
            return null;
        float h = Wielding.heaviness(held);
        boolean two = Wielding.isTwoHanded(held);
        if (two && off)
            return null;
        if (two)
            hand = AnimatedHand.TWO_HANDED;
        String side = right ? "right" : "left";
        return switch (type.family()) {
            case BLADE -> two
                    ? new Swing("bettercombat:two_handed_slash_horizontal_" + side, hand, arm, delay,
                            Mth.lerp(h, 0.35f, 0.6f), "bettercombat:claymore_swing")
                    : new Swing("bettercombat:one_handed_slash_horizontal_" + side, hand, arm, delay,
                            Mth.lerp(h, 0.35f, 0.6f), h < 0.25f ? "bettercombat:dagger_slash" : "bettercombat:sword_slash");
            case MACE -> !off && !player.onGround() && player.fallDistance > 1.5f
                    ? new Swing("bettercombat:two_handed_slam_heavy", AnimatedHand.TWO_HANDED, arm, delay, 0.6f, "bettercombat:hammer_slam")
                    : two
                    ? new Swing("bettercombat:two_handed_slam", hand, arm, delay, Mth.lerp(h, 0.45f, 0.7f), "bettercombat:hammer_slam")
                    : new Swing("bettercombat:one_handed_slam", hand, arm, delay, Mth.lerp(h, 0.45f, 0.7f), "bettercombat:mace_slash");
            case SPEAR -> off ? null : new Swing("bettercombat:two_handed_stab_" + side, AnimatedHand.TWO_HANDED, arm, delay, 0.4f,
                    "bettercombat:spear_stab");
            default -> null;
        };
    }

    /** Plays a Better Combat clip on {@code player}: its own gearing puts the strike {@code upswing} of {@code length} ticks in. */
    private static void play(Player player, String clip, AnimatedHand hand, float length, float upswing) {
        if (player instanceof PlayerAttackAnimatable animatable)
            animatable.playAttackAnimation(clip, hand, length, upswing);
    }

    private static void stop(Player player, float fade) {
        if (player instanceof PlayerAttackAnimatable animatable)
            animatable.stopAttackAnimation(fade);
    }

    private static void sound(Player player, String id) {
        if (id.isEmpty())
            return;
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id)), SoundSource.PLAYERS, 1f, 1f, false);
    }

    /** Another player's swing, relayed: play the clip now and the strike sound when it lands. */
    public static void playRemote(SwingPlayPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entity()) instanceof AbstractClientPlayer who) || who == mc.player)
            return;
        AnimatedHand[] hands = AnimatedHand.values();
        play(who, p.clip(), hands[Mth.clamp(p.hand(), 0, hands.length - 1)], p.length(), p.upswing());
        REMOTE.add(new RemoteStrike(p.entity(), mc.level.getGameTime() + Math.round(p.length() * p.upswing()), p.sound()));
    }

    /**
     * The Feedbacker's parry, drawn as Better Combat's punch: the clip spans the window and the recovery
     * after it, with the strike {@code 0.3} in, which lands inside the window.
     */
    public static void punchParry(AbstractClientPlayer player, float window, float recovery) {
        float length = Math.max(MIN_LENGTH, window + recovery);
        play(player, "bettercombat:one_handed_punch", AnimatedHand.MAIN_HAND, length, Mth.clamp(0.6f * window / length, 0.05f, 0.5f));
    }
}
