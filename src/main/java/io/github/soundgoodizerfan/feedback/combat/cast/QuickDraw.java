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
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Quick Draw: the hotbar's first gun comes up and fires on the same tick, and stays in hand. A gun that cannot fire
 * (empty, unchambered, cycling, none in the hotbar) makes the cast fail for free. The snap shot spreads wider the
 * heavier the gun is against {@link BodyMass#of}; a hot barrel already blooms the spread in {@code GunShots}.
 */
public final class QuickDraw {

    private QuickDraw() {
    }

    /** [OPEN] snap-shot spread added per unit of gun Mu over the player's body Mu, and its ceiling (a multiplier of the spread). */
    private static final float SNAP_PER_RATIO = 1.5f, SNAP_MAX = 2f;

    private static final Map<UUID, Float> SNAP = new HashMap<>();

    /** The spread multiplier a Quick Draw shot carries (spent here), 1 for any other shot. Read where the shot's stance is made. */
    public static float consume(ServerPlayer p) {
        Float m = SNAP.remove(p.getUUID());
        return m == null ? 1f : m;
    }

    static boolean act(ServerPlayer p) {
        var inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 9 && slot < 0; i++)
            if (inv.items.get(i).getItem() instanceof GunItem)
                slot = i;
        if (slot < 0)
            return false;
        ItemStack gun = inv.items.get(slot);
        if (GunAmmo.count(gun) < 1)
            return false;
        int was = inv.selected;
        GunFigures f = GunItem.figures(gun);
        float ratio = f == null ? 0f : f.gunMass().value() / Math.max(1f, BodyMass.of(p));
        SNAP.put(p.getUUID(), 1f + Mth.clamp(SNAP_PER_RATIO * ratio, 0f, SNAP_MAX - 1f));
        inv.selected = slot;
        boolean fired = GunShots.triggerHeard(p.serverLevel(), p, gun, p.getYRot(), p.getXRot(), System.currentTimeMillis(), false, InteractionHand.MAIN_HAND);
        SNAP.remove(p.getUUID());
        if (!fired) {
            inv.selected = was; // nothing was drawn: the hand stays where it was
            return false;
        }
        p.connection.send(new ClientboundSetCarriedItemPacket(slot));
        ServerLevel lv = p.serverLevel();
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.5)).add(0, -0.3, 0);
        CastFx.sound(lv, hand, FSounds.ROLL_WHOOSH.get(), 0.5f, 1.9f);
        lv.sendParticles(CastFx.dust(CastFx.STEEL, 0.8f), hand.x, hand.y, hand.z, 6, 0.1, 0.1, 0.1, 0.02);
        lv.sendParticles(ParticleTypes.CRIT, hand.x, hand.y, hand.z, 3, 0.1, 0.1, 0.1, 0.1);
        return true;
    }
}
