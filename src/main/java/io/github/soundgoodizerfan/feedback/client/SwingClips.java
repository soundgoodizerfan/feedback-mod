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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryStats;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Plays cleannrooster's clips on a player's actions through playerAnimator (`TODO.md`, animation
 * library item), for every player the client can see. The clip is never the timing: a swing's clip
 * is stretched to the weapon's own swing rate (attack speed), a roll's to the roll's phase, so a
 * heavy blade's long swing is a slow clip and a dagger's a quick one. Nothing here touches the
 * server: the swing is the vanilla flag every client already sees, the phase is synced
 * {@link CombatState}, and the blow itself is {@code combat/hit}.
 *
 * <ul>
 * <li>Blade swing: {@code slice}, or Better Combat's one-handed slashes in turn.</li>
 * <li>Mace swing: {@code 2h_slam}, or {@code leapslamstaff2} when it comes down from a fall; both capped short.</li>
 * <li>Spear held in use: {@code spearthrow_charge}, released: {@code spearthrow_throw}.</li>
 * <li>Roll: {@code fist_tumble_right}, over the whole roll; the procedural tuck is gone, this is the roll's only pose.</li>
 * <li>Feedbacker parry (bare hand, gauntlet worn): {@code fist_jab_right}, paced to the active window.</li>
 * <li>Spear swing: Better Combat's two-handed stabs in turn, nothing without it.</li>
 * <li>Empty hand: {@code fist_jab_right} and {@code fist_jab_left} in turn, or Better Combat's punch and uppercut.</li>
 * </ul>
 * A swing during a parry, roll, kick or stagger is that move's own, drawn by {@link CombatPose}, and gets no
 * clip; a gun's swing (fired or swung) gets none either, since the two are not told apart on the client.
 * Tried and cut by the author after play: {@code cleave}, {@code flourish}, {@code whirl}, {@code stabend},
 * {@code fist_flurry_right}. Never mapped: the other
 * clips in {@code player_animations/}, for moves the mod does not have yet.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SwingClips {

    private static final ResourceLocation LAYER = Feedback.id("swing");
    /** Above the library's own default layers, below nothing of ours yet. */
    private static final int PRIORITY = 42;
    /** A swing never plays faster than this many ticks, however light the blade. */
    private static final float MIN_TICKS = 4f;

    /**
     * A clip by registry name, and how many ticks of it are the action (its last keyframe, not its stop tick).
     * {@code better} is the same move in Better Combat's own set, when that mod is installed: its clips are
     * registered with playerAnimator under {@code bettercombat:}, so they are found by name and never copied
     * or shipped (its licence is all rights reserved; `THIRD-PARTY-LICENSES.md`). Preferred over ours when
     * present; a clip with no {@code name} plays only when Better Combat has its own.
     */
    private record Clip(String name, float ticks, float maxTicks, String better, float betterTicks) {
        Clip(String name, float ticks) {
            this(name, ticks, Float.MAX_VALUE, null, 0f);
        }

        Clip(String name, float ticks, float maxTicks) {
            this(name, ticks, maxTicks, null, 0f);
        }

        Clip withBetter(String better, float betterTicks) {
            return new Clip(name, ticks, maxTicks, better, betterTicks);
        }

        /** The clip to play and its action length: Better Combat's if it is installed and has it, else ours, else none. */
        Resolved resolve() {
            if (better != null) {
                var id = ResourceLocation.fromNamespaceAndPath("bettercombat", better);
                if (PlayerAnimationRegistry.getAnimationOptional(id).isPresent())
                    return new Resolved(id, betterTicks, maxTicks);
            }
            return name == null ? null : new Resolved(Feedback.id(name), ticks, maxTicks);
        }
    }

    private record Resolved(ResourceLocation id, float ticks, float maxTicks) {
    }

    // Better Combat's slashes, slams, stabs and punches run 20 ticks of action after a short windup.
    private static final Clip SLICE_R = new Clip("slice", 10).withBetter("one_handed_slash_horizontal_right", 20),
            SLICE_L = new Clip("slice", 10).withBetter("one_handed_slash_horizontal_left", 20),
            // The slams hold their last pose to the stop tick, 20 and 10; capped, they would otherwise take a mace's whole cooldown.
            SLAM = new Clip("2h_slam", 20, 10).withBetter("two_handed_slam", 20),
            AIR_SLAM = new Clip("leapslamstaff2", 10, 7).withBetter("two_handed_slam_heavy", 20),
            // Nothing of ours for a spear's thrust (stabend was cut), so it is Better Combat's or nothing.
            STAB_R = new Clip(null, 0).withBetter("two_handed_stab_right", 20),
            STAB_L = new Clip(null, 0).withBetter("two_handed_stab_left", 20),
            JAB_R = new Clip("fist_jab_right", 20).withBetter("one_handed_punch", 20),
            JAB_L = new Clip("fist_jab_left", 20).withBetter("one_handed_uppercut_right", 20),
            TUMBLE = new Clip("fist_tumble_right", 15),
            THROW_CHARGE = new Clip("spearthrow_charge", 30), THROW = new Clip("spearthrow_throw", 26);
    private static final Clip[] BLADE = {SLICE_R, SLICE_L}, STAB = {STAB_R, STAB_L}, FIST = {JAB_R, JAB_L};

    /** What each visible player did last tick, to find the tick it changed. */
    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    private record Seen(boolean swinging, CombatPhase phase, boolean using, int combo) {
    }

    /** The layer and its pace, together: a player's layer is made inside the library's constructor hook, where a map keyed by player is not safe. */
    private static final class Paced extends ModifierLayer<IAnimation> {
        final SpeedModifier pace = new SpeedModifier(1f);
        /** Whether the clip now playing is the spear's held charge, which ends when the hand lets go. */
        boolean charging;
    }

    private SwingClips() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, PRIORITY, player -> {
            Paced layer = new Paced();
            layer.addModifierLast(layer.pace);
            // First person is off unless a clip asks: draw the third-person arms there, so a swing is seen.
            FirstPersonModifier firstPerson = new FirstPersonModifier();
            firstPerson.setCurrentFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            layer.addModifierLast(firstPerson);
            return layer;
        });
        NeoForge.EVENT_BUS.addListener(SwingClips::tick);
        if (ModList.get().isLoaded("bettercombat")) {
            Feedback.LOGGER.warn("Feedback is using Better Combat's animations. Do NOT report problems with Feedback to Better Combat's authors; report them to Feedback's tracker.");
            NeoForge.EVENT_BUS.addListener(SwingClips::warnOnJoin);
        }
    }

    /** Once a session, in chat: bug reports go to us, never to the author of the mod whose clips we borrow. */
    private static boolean warned;

    private static void warnOnJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (warned)
            return;
        warned = true;
        event.getPlayer().displayClientMessage(Component.translatable("feedback.better_combat_notice"), false);
    }

    private static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            SEEN.clear();
            return;
        }
        for (Player p : level.players()) {
            if (!(p instanceof AbstractClientPlayer player))
                continue;
            CombatPhase phase = player.hasData(FCombat.STATE) ? CombatState.of(player).phase() : CombatPhase.IDLE;
            Seen was = SEEN.getOrDefault(player.getUUID(), new Seen(false, CombatPhase.IDLE, false, 0));
            ItemStack held = player.getMainHandItem();
            boolean swinging = player.swinging && player.swingingArm == InteractionHand.MAIN_HAND;
            boolean using = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    && family(held) == ModularType.Family.SPEAR;
            int combo = was.combo();

            if (phase == CombatPhase.ROLL && was.phase() != CombatPhase.ROLL) {
                // The whole roll, i-frames and recovery, is the clip's span; nothing else draws a roll.
                CombatState s = CombatState.of(player);
                play(player, TUMBLE, Math.max(1, s.ticksLeft() + s.then()), false);
            } else if (phase == CombatPhase.PARRY_ACTIVE && was.phase() != CombatPhase.PARRY_ACTIVE
                    && ParryStats.of(player).isEmpty()
                    && !CombatGear.granting(player, ParryTuning.PUNCH_PROJECTILES).isEmpty()) {
                // A Feedbacker parry is a punch with nothing in hand: the right jab, landing inside the active window.
                play(player, JAB_R, Math.max(1, CombatState.of(player).ticksLeft()), false);
            } else if (using && !was.using()) {
                play(player, THROW_CHARGE, THROW_CHARGE.ticks(), true);
            } else if (!using && was.using() && layer(player) != null && layer(player).charging) {
                play(player, THROW, THROW.ticks(), false);
            } else if (swinging && !was.swinging() && phase == CombatPhase.IDLE) {
                Clip clip = swingClip(player, held, combo);
                if (clip != null) {
                    combo++;
                    play(player, clip, swingTicks(player), false);
                }
            }
            SEEN.put(player.getUUID(), new Seen(swinging, phase, using, combo));
        }
    }

    private static Clip swingClip(AbstractClientPlayer player, ItemStack held, int combo) {
        if (held.isEmpty()) {
            // An empty hand also swings to open a door or place a block; for ourselves, only a held attack key is a punch.
            boolean me = player == Minecraft.getInstance().player;
            return me && !Minecraft.getInstance().options.keyAttack.isDown() ? null : FIST[combo % FIST.length];
        }
        ModularType.Family family = family(held);
        if (family == null)
            return null;
        return switch (family) {
            case BLADE -> BLADE[combo % BLADE.length];
            case MACE -> !player.onGround() && player.fallDistance > 1.5f ? AIR_SLAM : SLAM;
            case SPEAR -> STAB[combo % STAB.length];
            default -> null;
        };
    }

    /** The family of an unbroken modular weapon, else null. */
    private static ModularType.Family family(ItemStack stack) {
        var design = ModularStacks.design(stack);
        if (design == null || ModularStacks.broken(stack))
            return null;
        ModularType type = ModularTypes.known().get(design.type());
        return type == null ? null : type.family();
    }

    /** Ticks a swing should take: 70% of the weapon's cooldown, so the next can start. */
    private static float swingTicks(AbstractClientPlayer player) {
        double perSecond = Math.max(0.1, player.getAttributeValue(Attributes.ATTACK_SPEED));
        return (float) Math.max(MIN_TICKS, 0.7 * 20.0 / perSecond);
    }

    private static Paced layer(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof Paced layer ? layer : null;
    }

    /** Plays {@code clip} stretched so its action takes {@code ticks}; a held clip plays at its own pace. Nothing if there is no clip to play. */
    private static void play(AbstractClientPlayer player, Clip clip, float ticks, boolean held) {
        Paced layer = layer(player);
        Resolved r = clip.resolve();
        if (r == null || layer == null)
            return;
        var anim = PlayerAnimationRegistry.getAnimationOptional(r.id());
        if (anim.isEmpty()) {
            Feedback.LOGGER.info("SwingClips: no clip {}, known={}", r.id(), PlayerAnimationRegistry.getAnimations().keySet());
            return;
        }
        layer.pace.speed = held ? 1f : r.ticks() / Math.min(ticks, r.maxTicks());
        layer.charging = held;
        layer.setAnimation(anim.get().playAnimation());
    }
}
