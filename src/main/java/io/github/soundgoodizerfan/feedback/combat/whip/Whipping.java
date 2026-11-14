// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.whip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.combat.cast.Stamina;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.gear.GearStats;
import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.punch.Punching;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The Whiplash, server side: a worn arm granting {@link WhipTuning#WHIPLASH}. Its own key throws a barbed {@link GrappleHook} from the arm
 * (the hands stay free for a sword or a gun); while the key is held the hook flies and, once it bites, the line holds. Releasing the key
 * reels: a creature lighter than the wearer is dragged in, a heavier one or a block pulls the wearer to it, an item is fetched, a projectile
 * is carried back and sent along the look, and a coin the spear touches is knocked back up. Pressing again with a line out lets it go, and
 * releasing before the hook has bitten draws it back. The line is the arm's strap material, so its rating is that metal's, as any grapple
 * line's. Throws spend stamina and wear the arm. The client only says the key went down or came up.
 */
public final class Whipping {

    /** Game tick each player last threw: the arm's own cooldown, apart from the fists'. */
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private Whipping() {
    }

    private static ItemStack arm(ServerPlayer player) {
        return CombatGear.granting(player, WhipTuning.WHIPLASH);
    }

    /** The hooks {@code player} has out from the arm. */
    private static List<GrappleHook> hooks(ServerPlayer player) {
        return GrappleHook.allOf(player.level(), player).stream().filter(h -> h.has(GrappleHook.WHIP)).toList();
    }

    private static void denied(ServerPlayer player) {
        player.playNotifySound(FSounds.PRESS_DENIED.get(), SoundSource.PLAYERS, FeelTuning.DENY_VOLUME, FeelTuning.DENY_PITCH);
    }

    /** The key went down: lets go of a line that is out, else throws the hook. */
    public static void press(ServerPlayer player) {
        ItemStack arm = arm(player);
        if (arm.isEmpty() || player.isSpectator() || player.isPassenger() || player.isSleeping() || Grabbing.holding(player)) {
            denied(player);
            return;
        }
        List<GrappleHook> out = hooks(player);
        if (!out.isEmpty()) {
            out.forEach(GrappleHook::discard);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < WhipTuning.COOLDOWN || !Stamina.spend(player, WhipTuning.STAMINA)) {
            denied(player);
            return;
        }
        LAST.put(player.getUUID(), now);
        GearStats gear = arm.get(FDataComponents.GEAR.get());
        GrappleHook hook = new GrappleHook(level, player);
        hook.setFittings(GrappleHook.WHIP | GrappleHook.BARB | GrappleHook.WINCH);
        if (gear != null)
            hook.setLine(gear.strap());
        hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, ModularTuning.GRAPPLE_SPEED, 0f);
        level.addFreshEntity(hook);
        player.swing(Punching.hand(player), true);
        level.playSound(null, player, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.8f, 1.3f);
        arm.hurtAndBreak(WhipTuning.WEAR_THROW, player, EquipmentSlot.MAINHAND);
    }

    /** The key came up: a hook that has bitten reels; one still in the air is drawn back. */
    public static void release(ServerPlayer player) {
        for (GrappleHook hook : hooks(player)) {
            if (hook.anchored())
                hook.pull();
            else
                hook.discard();
        }
    }

    /** A punch cancels the reel, keeping the wearer's momentum: the lines let go. */
    public static void cancel(ServerPlayer player) {
        hooks(player).forEach(GrappleHook::discard);
    }
}
