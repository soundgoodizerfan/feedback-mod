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
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.combat.hit.SweptVolume;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * A gun swung as a bludgeon (gun-fu, the melee stance). Every figure is the gun's baked mass
 * ({@link GunFigures#gunMass()}); no gun has a row of its own. Server side it resolves one swing: a short
 * line from the eye ({@link SweptVolume#targets}), the nearest living thing on it takes a normal
 * player-attack {@code hurt}, so the damage event and every trait and perk resolve as for any hit
 * (traits read the main hand, so an off-hand swing carries the main hand's traits until the per-hand
 * work lands). A plain swing wears nothing: misuse wears, a swing is not misuse.
 */
public final class GunBludgeon {

    /** Earliest game tick each player's hand may swing again; [main, off]. */
    private static final Map<UUID, long[]> NEXT_TICK = new HashMap<>();

    private GunBludgeon() {
    }

    /** Damage of a swing of a gun of this baked mass. */
    public static float damage(GunFigures f) {
        return Math.min(FTuning.GUN_BLUDGEON_DAMAGE_MAX,
                FTuning.GUN_BLUDGEON_DAMAGE_BASE + FTuning.GUN_BLUDGEON_DAMAGE_PER_MU * f.gunMass().value());
    }

    /** Ticks before the same hand can swing again: a heavy gun is slow to bring round. */
    public static int cooldownTicks(GunFigures f) {
        return Mth.clamp(Math.round(FTuning.GUN_BLUDGEON_TICKS_PER_MU * f.gunMass().value()),
                FTuning.GUN_BLUDGEON_TICKS_MIN, FTuning.GUN_BLUDGEON_TICKS_MAX);
    }

    public static void swing(ServerPlayer player, boolean offHand) {
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack gun = player.getItemInHand(hand);
        GunFigures f = gun.getItem() instanceof GunItem ? GunItem.figures(gun) : null;
        if (f == null || (offHand && !GunItem.offHandFires(player)))
            return;
        long now = player.level().getGameTime();
        long[] next = NEXT_TICK.computeIfAbsent(player.getUUID(), u -> new long[2]);
        int i = offHand ? 1 : 0;
        // One tick of grace for network jitter.
        if (now + 1 < next[i])
            return;
        next[i] = now + cooldownTicks(f);

        player.swing(hand, true);
        var target = SweptVolume.targets(player, FTuning.GUN_BLUDGEON_HALF_WIDTH).stream().findFirst().orElse(null);
        var sound = SoundEvents.PLAYER_ATTACK_NODAMAGE;
        if (target != null && target.hurt(player.damageSources().playerAttack(player), damage(f))) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            target.knockback(FTuning.GUN_BLUDGEON_KNOCKBACK_BASE + FTuning.GUN_BLUDGEON_KNOCKBACK_PER_MU * f.gunMass().value(),
                    Mth.sin(yaw), -Mth.cos(yaw));
            sound = f.gunMass().value() >= FTuning.GUN_BLUDGEON_HEAVY_MU ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK;
            // HOOK (gun-fu, not built): a staggering hit chambers a free shot at the target here. Stagger does
            // not exist yet; when it does, fire GunShots at `target` from this branch. See the combat plan.
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1f, 1f);
    }
}
