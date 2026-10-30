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

import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;

/** Rend: a strike that grinds the worn armour of whatever it hits, hardness against hardness. */
final class Rend {

    private Rend() {
    }

    /** Rend: a short blow at what you face; each armour piece it wears loses durability by your weapon's hardness against that piece's, and your weapon is worn by theirs. Nothing worn, nothing happens. */
    static boolean act(ServerPlayer p) {
        ItemStack w = p.getMainHandItem();
        LivingEntity t = ahead(p, 3.2, 0.5);
        if (w.isEmpty() || w.getItem() instanceof GunItem || t == null)
            return false;
        float struck = Disorder.wearArmour(t, Disorder.hardness(w));
        if (struck <= 0f)
            return false;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), 2f);
        Disorder.selfWear(p, struck);
        Disorder.note(p, t, Disorder.Kind.WORN);
        p.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 chest = t.position().add(0, t.getBbHeight() * 0.65, 0);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, chest.x, chest.y, chest.z, 22, 0.25, 0.25, 0.25, 0.4);
        CastFx.line(lv, CastFx.fade(CastFx.STEEL, CastFx.EMBER, 1.1f), chest.add(-0.4, 0.4, 0), chest.add(0.4, -0.4, 0), 0.1);
        CastFx.sound(lv, chest, SoundEvents.GRINDSTONE_USE, 1f, 0.8f);
        CastFx.sound(lv, chest, SoundEvents.ANVIL_LAND, 0.5f, 1.6f);
        return true;
    }
}
