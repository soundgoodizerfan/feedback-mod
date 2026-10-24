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
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
 * <li>Blade swing: {@code slice}.</li>
 * <li>Mace swing: {@code 2h_slam}, or {@code leapslamstaff2} when it comes down from a fall; both capped short.</li>
 * <li>Spear held in use: {@code spearthrow_charge}, released: {@code spearthrow_throw}.</li>
 * <li>Empty hand: {@code fist_jab_right} and {@code fist_jab_left} in turn.</li>
 * </ul>
 * A swing during a parry, roll, kick or stagger is that move's own, drawn by {@link CombatPose}, and gets no
 * clip; a gun's swing (fired or swung) gets none either, since the two are not told apart on the client.
 * Tried and cut by the author after play: {@code cleave}, {@code flourish}, {@code whirl}, {@code stabend},
 * {@code fist_flurry_right}, {@code fist_tumble_right} (fought the procedural roll). Never mapped: the other
 * clips in {@code player_animations/}, for moves the mod does not have yet.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SwingClips {

    private static final ResourceLocation LAYER = Feedback.id("swing");
    /** Above the library's own default layers, below nothing of ours yet. */
    private static final int PRIORITY = 42;
    /** A swing never plays faster than this many ticks, however light the blade. */
    private static final float MIN_TICKS = 4f;

    /** A clip, by registry name, and how many ticks of it are the action (its last keyframe, not its stop tick). */
    private record Clip(String name, float ticks, float maxTicks) {
        Clip(String name, float ticks) {
            this(name, ticks, Float.MAX_VALUE);
        }

        ResourceLocation id() {
            return Feedback.id(name);
        }
    }

    private static final Clip SLICE = new Clip("slice", 10),
            // The slams hold their last pose to the stop tick, 20 and 10; capped, they would otherwise take a mace's whole cooldown.
            SLAM = new Clip("2h_slam", 20, 10), AIR_SLAM = new Clip("leapslamstaff2", 10, 7),
            JAB_R = new Clip("fist_jab_right", 20), JAB_L = new Clip("fist_jab_left", 20),
            THROW_CHARGE = new Clip("spearthrow_charge", 30), THROW = new Clip("spearthrow_throw", 26);
    private static final Clip[] FIST = {JAB_R, JAB_L};

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

            if (using && !was.using()) {
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
            case BLADE -> SLICE;
            case MACE -> !player.onGround() && player.fallDistance > 1.5f ? AIR_SLAM : SLAM;
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

    /** Plays {@code clip} stretched so its action takes {@code ticks}; a held clip plays at its own pace. */
    private static void play(AbstractClientPlayer player, Clip clip, float ticks, boolean held) {
        Paced layer = layer(player);
        var anim = PlayerAnimationRegistry.getAnimationOptional(clip.id());
        if (layer == null || anim.isEmpty()) {
            Feedback.LOGGER.info("SwingClips: no clip {}, layer={}, known={}", clip.name(), layer != null,
                    PlayerAnimationRegistry.getAnimations().keySet());
            return;
        }
        layer.pace.speed = held ? 1f : clip.ticks() / Math.min(ticks, clip.maxTicks());
        layer.charging = held;
        layer.setAnimation(anim.get().playAnimation());
    }
}
