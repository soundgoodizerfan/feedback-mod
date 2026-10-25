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
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.OffhandStrike;
import io.github.soundgoodizerfan.feedback.combat.hit.Wielding;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * The off-hand attack: right click when nothing else wants it ({@link #takesUseClick}), and a key,
 * unbound by default, as a rebindable alternative that always asks. The server decides whether the off-hand stack can strike
 * and what it hits ({@code OffhandStrike}); the client only asks.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class OffhandStrikeClient {

    public static final KeyMapping KEY = new KeyMapping("key.feedback.offhand_strike", InputConstants.UNKNOWN.getValue(),
            "key.categories.feedback");

    public static final KeyMapping BARE_TOGGLE = new KeyMapping("key.feedback.barehanded", InputConstants.KEY_B,
            "key.categories.feedback");

    /** Barehanded combat mode: with both hands empty, left click punches with the main fist and right click with the off fist. */
    private static boolean bare;

    public static boolean barehanded() {
        return bare;
    }

    /** Whether the use key was already down last tick: a held key repeats, and a swing needs a fresh press (no hold-to-attack). */
    private static boolean useHeld;

    private OffhandStrikeClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        useHeld = mc.options.keyUse.isDown();
        while (BARE_TOGGLE.consumeClick())
            if (mc.player != null && mc.screen == null) {
                bare = !bare;
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        bare ? "feedback.barehanded.on" : "feedback.barehanded.off"), true);
            }
        while (KEY.consumeClick())
            if (mc.player != null && mc.screen == null && !mc.player.getOffhandItem().isEmpty())
                UpswingSwing.requestOffhand();
    }

    /** Right click strikes with the off hand when {@link #takesUseClick}; vanilla's use is cancelled, the server swings. */
    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND && takesUseClick(mc)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            if (!useHeld)
                UpswingSwing.requestOffhand();
        }
    }

    /**
     * The one eligibility rule. Right click is an off-hand strike only when the off hand holds a modular
     * strike weapon, the main hand is empty or another such weapon with no use animation (so no bow, food,
     * block, bucket, shield, gun or tool: those all have a use of their own), the player is not mid-use,
     * and the block aimed at has no use of its own (a class overriding {@code useWithoutItem}: chest, door,
     * lever, button...). A held key does not repeat the strike: each swing is a fresh press.
     */
    private static boolean takesUseClick(Minecraft mc) {
        var player = mc.player;
        if (player == null || mc.screen != null || player.isUsingItem() || Wielding.offHandBlocked(player))
            return false;
        boolean fists = bare && OffhandStrike.barehanded(player);
        if (!fists && !OffhandStrike.strikes(player.getOffhandItem()))
            return false;
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && !(OffhandStrike.strikes(main) && main.getUseAnimation() == UseAnim.NONE))
            return false;
        return !(mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hasUse(mc.level.getBlockState(hit.getBlockPos()).getBlock().getClass()));
    }

    private static final ClassValue<Boolean> HAS_USE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != net.minecraft.world.level.block.Block.class
                    && c != net.minecraft.world.level.block.state.BlockBehaviour.class; c = c.getSuperclass())
                try {
                    c.getDeclaredMethod("useWithoutItem", net.minecraft.world.level.block.state.BlockState.class,
                            net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class,
                            net.minecraft.world.entity.player.Player.class, BlockHitResult.class);
                    return true;
                } catch (NoSuchMethodException e) {
                    // not this class; try its parent
                }
            return false;
        }
    };

    private static boolean hasUse(Class<?> block) {
        return HAS_USE.get(block);
    }
}
