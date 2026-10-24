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

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.Wielding;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * How a player holds a weapon at rest: a two-handed blade or mace, a spear, a bow, a crossbow. Better
 * Combat's idle poses, kept; the choosing is ours. Its own pose code reads a weapon-attributes file per
 * item, which our modular weapons do not have, so it never poses them. Here every client reads the held
 * stack ({@link Wielding}: a blade or mace is two-handed by its own heaviness) and lays the matching
 * {@code bettercombat:pose_*} clip on two layers of its own, in the same two channels Better Combat uses:
 * the body (arms, torso, legs) and the held item. Nothing is synced, because the stack already is.
 *
 * <p>No pose while the player swings, swims, climbs, glides, uses an item, or is in a combat move; the
 * attack layer (Better Combat's, above these) takes over for a swing anyway. A left-handed player is mirrored.
 * Poses are never drawn in first person.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponPose {

    private static final ResourceLocation BODY = Feedback.id("pose_body"), ITEM = Feedback.id("pose_item");
    /** Below our own clip layer (42) and Better Combat's attack layer (2000). */
    private static final int BODY_PRIORITY = 20, ITEM_PRIORITY = 21;

    private WeaponPose() {
    }

    /** A pose layer: what it shows now, so an unchanged pose is not restarted each tick. */
    private static final class PoseLayer extends ModifierLayer<IAnimation> {
        final MirrorModifier mirror = new MirrorModifier();
        final boolean body;
        ResourceLocation shown;
        boolean mirrored;

        PoseLayer(boolean body) {
            this.body = body;
            mirror.setEnabled(false);
            addModifierLast(mirror);
        }

        void show(ResourceLocation id, boolean mirrorIt) {
            if (java.util.Objects.equals(id, shown) && (id == null || mirrorIt == mirrored))
                return;
            shown = id;
            mirrored = mirrorIt;
            var clip = id == null ? java.util.Optional.<dev.kosmx.playerAnim.api.IPlayable>empty()
                    : PlayerAnimationRegistry.getAnimationOptional(id);
            if (clip.isEmpty() || !(clip.get() instanceof KeyframeAnimation source)) {
                replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE), null);
                return;
            }
            var copy = source.mutableCopy();
            if (body) {
                off(copy.rightItem);
                off(copy.leftItem);
            } else {
                off(copy.head);
                off(copy.torso);
                off(copy.body);
                off(copy.rightArm);
                off(copy.leftArm);
                off(copy.rightLeg);
                off(copy.leftLeg);
            }
            mirror.setEnabled(mirrorIt);
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                    new KeyframeAnimationPlayer(copy.build(), 0));
        }

        private static void off(KeyframeAnimation.StateCollection part) {
            part.pitch.setEnabled(false);
            part.roll.setEnabled(false);
            part.yaw.setEnabled(false);
            part.x.setEnabled(false);
            part.y.setEnabled(false);
            part.z.setEnabled(false);
        }
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(BODY, BODY_PRIORITY, player -> new PoseLayer(true));
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ITEM, ITEM_PRIORITY, player -> new PoseLayer(false));
        NeoForge.EVENT_BUS.addListener(WeaponPose::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null)
            return;
        for (Player p : level.players()) {
            if (!(p instanceof AbstractClientPlayer player))
                continue;
            ResourceLocation pose = poseFor(player);
            boolean left = player.getMainArm() == HumanoidArm.LEFT;
            var data = PlayerAnimationAccess.getPlayerAssociatedData(player);
            if (data.get(BODY) instanceof PoseLayer body)
                body.show(pose, left);
            if (data.get(ITEM) instanceof PoseLayer item)
                item.show(pose, left);
        }
    }

    /** The {@code bettercombat:pose_*} clip for what {@code player} holds and does, or null for none. */
    private static ResourceLocation poseFor(Player player) {
        if (player.swinging || player.isSwimming() || player.isUsingItem() || player.onClimbable() || player.isFallFlying()
                || (player.hasData(FCombat.STATE) && CombatState.of(player).phase() != CombatPhase.IDLE))
            return null;
        ItemStack held = player.getMainHandItem();
        var design = ModularStacks.design(held);
        if (design == null || ModularStacks.broken(held))
            return null;
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null)
            return null;
        String pose = switch (type.family()) {
            case BLADE -> Wielding.isTwoHanded(held) ? "pose_two_handed_sword" : null;
            case MACE -> Wielding.isTwoHanded(held) ? "pose_two_handed_heavy" : null;
            case SPEAR -> "pose_two_handed_polearm";
            case BOW -> "pose_two_handed_bow";
            case CROSSBOW -> "pose_two_handed_crossbow";
            default -> null;
        };
        return pose == null ? null : ResourceLocation.fromNamespaceAndPath("bettercombat", pose);
    }
}
