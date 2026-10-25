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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Brace, always on once owned (a stance, not a cast): while crouched, knockback cannot move you and every blow taken is held by your mass (armour counts) against the attacker's; the held share goes into the attacker as stagger and the held weapon wears for it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Brace {

    private Brace() {
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onBraced(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !braced(p)
                || !(event.getSource().getEntity() instanceof LivingEntity foe) || foe.distanceToSqr(p) > 36)
            return;
        float mine = BodyMass.of(p) + BodyMass.armour(p), theirs = BodyMass.of(foe) + BodyMass.armour(foe);
        float held = event.getAmount() * Mth.clamp(mine / (mine + theirs), 0.3f, 0.8f);        event.setAmount(event.getAmount() - held);
        stagger(p, foe, held * ParryTuning.JOULES_PER_DAMAGE);
        ItemStack weapon = p.getMainHandItem();
        if (weapon.isDamageableItem())
            weapon.hurtAndBreak(Math.max(1, (int) (held / 4)), p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        ServerLevel lv = p.serverLevel();
        Vec3 clash = p.position().add(0, 1.2, 0).add(foe.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(0.8));
        lv.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(), 5, 0.3, 0.3, 0.3, 0.2);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, clash.x, clash.y, clash.z, 14, 0.25, 0.25, 0.25, 0.5);
        lv.sendParticles(ParticleTypes.FLASH, clash.x, clash.y, clash.z, 1, 0, 0, 0, 0);
        CastFx.sound(lv, clash, SoundEvents.SHIELD_BLOCK, 1f, 0.6f);
        CastFx.sound(lv, clash, SoundEvents.ANVIL_LAND, 0.4f, 1.6f);
    }

    /** Braced is crouching with the skill: the player's own stance, so there is no window or key. */
    private static boolean braced(ServerPlayer p) {
        return p.isShiftKeyDown() && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "brace");
    }

    /** A braced fighter cannot be shoved. */
    @SubscribeEvent
    public static void onKnockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && braced(p))
            event.setCanceled(true);
    }
}
