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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Brace: plant and hold, a blow taken goes back as stagger. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Brace {

    private Brace() {
    }

    /** The game tick Brace is live until. */
    private static final java.util.Map<java.util.UUID, Long> BRACED = new java.util.HashMap<>();
    private static final net.minecraft.resources.ResourceLocation BRACE_SLOW = Feedback.id("brace_slow"), BRACE_ROOT = Feedback.id("brace_root");

    /**
     * Brace: plant for four seconds. Knockback can't move you, you can barely walk, and every blow taken is held by your mass
     * (armour counts) against the attacker's: the held share is not taken, it goes into the attacker as stagger, and the
     * weapon pays for it in wear.
     */
    static boolean act(ServerPlayer p) {
        BRACED.put(p.getUUID(), p.level().getGameTime() + 80);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(BRACE_SLOW);
        speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BRACE_SLOW, -0.75,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var root = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        root.removeModifier(BRACE_ROOT);
        root.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BRACE_ROOT, 1.0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.7f);
        ServerLevel lv = p.serverLevel();
        Vec3 feet = p.position().add(0, 0.1, 0);
        lv.sendParticles(ParticleTypes.CLOUD, feet.x, feet.y, feet.z, 12, 0.5, 0.02, 0.5, 0.02);
        CastFx.ring(lv, CastFx.dust(CastFx.STEEL, 1.4f), feet, 0.9, 16, 0.2);
        CastFx.spokes(lv, ParticleTypes.CRIT, feet, 0.5, 1.8, 8, 0.25);
        CastFx.debris(lv, feet, 30, 0.8);
        CastFx.sound(lv, feet, SoundEvents.ANVIL_PLACE, 0.8f, 0.6f);
        CastFx.sound(lv, feet, SoundEvents.IRON_GOLEM_REPAIR, 0.8f, 0.7f);
        return true;
    }

    private static void unbrace(ServerPlayer p) {
        CastFx.ring(p.serverLevel(), ParticleTypes.CLOUD, p.position().add(0, 0.1, 0), 0.7, 10, 0.12);
        BRACED.remove(p.getUUID());
        p.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(BRACE_SLOW);
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(BRACE_ROOT);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onBraced(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !BRACED.containsKey(p.getUUID())
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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Long braced = BRACED.get(p.getUUID());
        if (braced != null && p.level().getGameTime() >= braced)
            unbrace(p);
        else if (braced != null && p.level().getGameTime() % 5 == 0) // planted: a ring of dust turning at the feet
            CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.STEEL, 0.9f), p.position().add(0, 0.05, 0), 0.9, 8, 0);
    }
}
