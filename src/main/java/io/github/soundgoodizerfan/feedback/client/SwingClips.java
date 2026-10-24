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

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
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
 * Plays cleannrooster's clips on a player's swing through playerAnimator (`TODO.md`, animation
 * library item). Spike: one clip, {@code slice}, on a modular blade's main-hand swing, for every
 * player the client can see. The clip's pace follows the blade's own swing rate, so a heavy blade's
 * long swing stretches it and a light one's short swing hurries it; the clip is never the timing,
 * the baked weapon is. Nothing here touches the server: the swing is the vanilla flag every client
 * already sees, and the blow itself is {@code combat/hit}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SwingClips {

    private static final ResourceLocation LAYER = Feedback.id("swing");
    private static final ResourceLocation SLICE = Feedback.id("slice");
    /** Above the library's own default layers, below nothing of ours yet. */
    private static final int PRIORITY = 42;
    /** The clip's length in ticks (its {@code endTick}), the pace's numerator. */
    private static final float SLICE_TICKS = 10f;
    /** A swing never plays faster than this many ticks, however light the blade. */
    private static final float MIN_TICKS = 4f;

    private static final Map<UUID, Boolean> WAS_SWINGING = new HashMap<>();

    /** The layer and its pace, together: a player's layer is made inside the library's constructor hook, where a map keyed by player is not safe. */
    private static final class Paced extends ModifierLayer<IAnimation> {
        final SpeedModifier pace = new SpeedModifier(1f);
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
            WAS_SWINGING.clear();
            return;
        }
        for (Player p : level.players()) {
            if (!(p instanceof AbstractClientPlayer player))
                continue;
            boolean now = player.swinging && player.swingingArm == InteractionHand.MAIN_HAND;
            boolean was = WAS_SWINGING.put(player.getUUID(), now) == Boolean.TRUE;
            if (now && !was) {
                boolean blade = isBlade(player.getMainHandItem());
                Feedback.LOGGER.info("SwingClips: {} swung, blade={}, design={}", player.getName().getString(), blade,
                        ModularStacks.design(player.getMainHandItem()) != null);
                if (blade)
                    play(player);
            }
        }
    }

    private static boolean isBlade(ItemStack stack) {
        var design = ModularStacks.design(stack);
        if (design == null || ModularStacks.broken(stack))
            return false;
        ModularType type = ModularTypes.known().get(design.type());
        return type != null && type.family() == ModularType.Family.BLADE;
    }

    private static void play(AbstractClientPlayer player) {
        var clip = PlayerAnimationRegistry.getAnimationOptional(SLICE);
        if (!(PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof Paced layer) || clip.isEmpty())
            return;
        SpeedModifier pace = layer.pace;
        // Attack speed is swings per second; a swing gets 70% of its cooldown, so the next can start.
        double perSecond = Math.max(0.1, player.getAttributeValue(Attributes.ATTACK_SPEED));
        pace.speed = SLICE_TICKS / (float) Math.max(MIN_TICKS, 0.7 * 20.0 / perSecond);
        layer.setAnimation(clip.get().playAnimation());
    }
}
