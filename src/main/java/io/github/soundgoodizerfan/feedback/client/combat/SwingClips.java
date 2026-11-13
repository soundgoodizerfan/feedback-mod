// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.util.Ease;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The clips that are not a melee swing's: they play through playerAnimator on a layer of our own.
 * Melee swings, with their upswing and their Better Combat clips and sounds, are {@link UpswingSwing}.
 *
 * <ul>
 * <li>Roll: cleannrooster's {@code fist_tumble_right}, stretched over the whole roll; the roll's only pose.</li>
 * <li>Spear held in use: {@code spearthrow_charge}, released: {@code spearthrow_throw}.</li>
 * <li>Feedbacker parry (bare hand, gauntlet worn): Better Combat's punch, by {@link UpswingSwing#punchParry}.</li>
 * </ul>
 * Tried and cut by the author after play: {@code cleave}, {@code flourish}, {@code whirl}, {@code stabend},
 * {@code fist_flurry_right}; our own slice, slam and jab clips, which Better Combat's replace.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SwingClips {

    private static final ResourceLocation LAYER = Feedback.id("swing");
    /** Above the library's own default layers, below Better Combat's attack layer (2000). */
    private static final int PRIORITY = 42;

    /** A clip, by registry name, and how many ticks of it are the action (its last keyframe, not its stop tick). */
    private record Clip(String name, float ticks) {
        ResourceLocation id() {
            return Feedback.id(name);
        }
    }

    private static final Clip TUMBLE = new Clip("fist_tumble_right", 15), THROW_CHARGE = new Clip("spearthrow_charge", 30),
            THROW = new Clip("spearthrow_throw", 26);

    /** What each visible player did last tick, to find the tick it changed. */
    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    private record Seen(CombatPhase phase, boolean using) {
    }

    /** The layer and its pace, together: a player's layer is made inside the library's constructor hook, where a map keyed by player is not safe. */
    private static final class Paced extends ModifierLayer<IAnimation> {
        final SpeedModifier pace = new SpeedModifier(1f);
        /** Whether the clip now playing is the spear's held charge, which ends when the hand lets go. */
        boolean charging;
        /** Ticks until the throw clip is faded out; 0 when nothing is pending. */
        int clearIn;
    }

    private SwingClips() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, PRIORITY, player -> {
            Paced layer = new Paced();
            layer.addModifierLast(layer.pace);
            // First person is off unless a clip asks: draw the third-person arms there, so a clip is seen.
            FirstPersonModifier firstPerson = new FirstPersonModifier();
            firstPerson.setCurrentFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            layer.addModifierLast(firstPerson);
            return layer;
        });
        NeoForge.EVENT_BUS.addListener(SwingClips::tick);
        Feedback.LOGGER.warn("Feedback is using Better Combat's animation player, clips and sounds. Do NOT report problems with Feedback to Better Combat's authors; report them to Feedback's tracker.");
        NeoForge.EVENT_BUS.addListener(SwingClips::warnOnJoin);
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
            Seen was = SEEN.getOrDefault(player.getUUID(), new Seen(CombatPhase.IDLE, false));
            boolean using = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND && isSpear(player.getMainHandItem());

            if (phase == CombatPhase.ROLL && was.phase() != CombatPhase.ROLL) {
                // The tumble is squeezed into the i-frames; recovery holds its end pose. Nothing else draws a roll.
                play(player, TUMBLE, Math.max(1, CombatState.of(player).ticksLeft()), false);
            } else if (phase == CombatPhase.PARRY_ACTIVE && was.phase() != CombatPhase.PARRY_ACTIVE
                    && ParryStats.of(player).map(ParryStats::bare)
                            .orElseGet(() -> !CombatGear.granting(player, ParryTuning.PUNCH_PROJECTILES).isEmpty())) {
                // A Feedbacker parry is a punch with nothing in hand, landing inside the active window.
                UpswingSwing.punchParry(player);
            } else if (using && !was.using()) {
                play(player, THROW_CHARGE, THROW_CHARGE.ticks(), true);
            } else if (!using && was.using() && layer(player) != null && layer(player).charging) {
                play(player, THROW, THROW.ticks(), false);
                layer(player).clearIn = (int) THROW.ticks() + 4;
            }
            // The clip holds its last pose to its stop tick; fade out as soon as the action is over.
            Paced layer = layer(player);
            if (layer != null && layer.clearIn > 0 && --layer.clearIn == 0)
                layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE), null);
            SEEN.put(player.getUUID(), new Seen(phase, using));
        }
    }

    private static boolean isSpear(ItemStack stack) {
        var design = ModularStacks.design(stack);
        if (design == null || ModularStacks.broken(stack))
            return false;
        return io.github.soundgoodizerfan.feedback.modular.ModularFaces.combat(stack) == ModularType.Family.SPEAR;
    }

    private static Paced layer(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof Paced layer ? layer : null;
    }

    /** Plays {@code clip} stretched so its action takes {@code ticks}; a held clip plays at its own pace. */
    private static void play(AbstractClientPlayer player, Clip clip, float ticks, boolean held) {
        Paced layer = layer(player);
        var anim = PlayerAnimationRegistry.getAnimationOptional(clip.id());
        if (layer == null || anim.isEmpty())
            return;
        layer.pace.speed = held ? 1f : clip.ticks() / ticks;
        layer.charging = held;
        layer.setAnimation(anim.get().playAnimation());
    }
}
