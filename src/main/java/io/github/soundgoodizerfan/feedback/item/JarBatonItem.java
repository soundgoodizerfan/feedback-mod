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
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A Leyden jar on a handle: a capacitor discharged into whatever it hits.
 *
 * <h2>One hit, the whole charge</h2>
 * Use it on a charged jar to draw that jar's voltage; the next melee hit spends all of it. The
 * target takes {@link Shock} for the voltage's band, scaled by its {@code Exposure} as a fence
 * shock is, is knocked back and staggered for as long as the charge was big. Nothing is left to
 * reuse: a capacitor empties.
 *
 * <h2>Misuse shocks the holder</h2>
 * Charge above {@link FTuning#BATON_RATING_VOLTS}, or swinging while standing in water, shocks
 * the holder by the same path; insulating gloves (checked by {@code Shock}) protect a player. A
 * baton used within its rating, dry, wears nothing.
 */
public class JarBatonItem extends Item {

    public JarBatonItem(Properties properties) {
        super(properties);
    }

    private static float volts(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.BATON_VOLTS.get(), 0f);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        if (volts(stack) > 0f || !(level.getBlockEntity(context.getClickedPos()) instanceof LeydenJarBlockEntity jar)
                || jar.getStoredVolts() <= 0f)
            return InteractionResult.PASS;
        if (!level.isClientSide)
            stack.set(FDataComponents.BATON_VOLTS.get(), jar.drain());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float volts = volts(stack);
        if (volts <= 0f || attacker.level().isClientSide)
            return false;
        stack.remove(FDataComponents.BATON_VOLTS.get());
        if (volts > FTuning.BATON_RATING_VOLTS || attacker.isInWater())
            Shock.apply(attacker, volts);
        target.invulnerableTime = 0;   // the swing's own hit has just set it; the discharge is a second blow
        Shock.apply(target, volts);
        target.knockback(FTuning.FENCE_KNOCKBACK, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        target.hurtMarked = true;
        CombatTicker.stagger(target, Math.min(FTuning.BATON_STAGGER_MAX_TICKS,
                Math.round(volts * FTuning.BATON_STAGGER_TICKS_PER_VOLT)), FTuning.BATON_STAGGER_SLOW);
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 10, 0.2, 0.3, 0.2, 0.1);
            server.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3f, 2f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        float v = volts(stack);
        if (v <= 0f)
            lines.add(Component.translatable("tooltip.feedback.jar_baton.empty"));
        else if (io.github.soundgoodizerfan.feedback.client.Readout.instrumented(io.github.soundgoodizerfan.feedback.instrument.Quantity.VOLTAGE))
            lines.add(Component.translatable("tooltip.feedback.jar_baton.charge", io.github.soundgoodizerfan.feedback.client.Readout.figure(
                    io.github.soundgoodizerfan.feedback.instrument.Quantity.VOLTAGE, v)));
    }
}
