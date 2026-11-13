// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import java.util.List;

import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
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
 * Use it on a charged jar to draw that jar's charge, in Eu, and its band; the next melee hit spends all of it. The
 * target takes {@link Shock} for the band and the Eu, scaled by its {@code Exposure} as a fence
 * shock is, is knocked back and staggered for as long as the charge was big. Nothing is left to
 * reuse: a capacitor empties.
 *
 * <h2>Misuse shocks the holder</h2>
 * A jar above {@link ItemTuning#BATON_RATING_BAND}, or swinging while standing in water, shocks
 * the holder by the same path; insulating gloves (checked by {@code Shock}) protect a player. A
 * baton used within its rating, dry, wears nothing.
 */
public class JarBatonItem extends Item {

    public JarBatonItem(Properties properties) {
        super(properties);
    }

    private static float eu(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.BATON_EU.get(), 0f);
    }

    private static VoltageBand band(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.BATON_BAND.get(), VoltageBand.LV);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        if (eu(stack) > 0f || !(level.getBlockEntity(context.getClickedPos()) instanceof LeydenJarBlockEntity jar)
                || jar.getStoredEu() <= 0f)
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            stack.set(FDataComponents.BATON_BAND.get(), jar.band());
            stack.set(FDataComponents.BATON_EU.get(), jar.drain());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float eu = eu(stack);
        VoltageBand band = band(stack);
        if (eu <= 0f || attacker.level().isClientSide)
            return false;
        stack.remove(FDataComponents.BATON_EU.get());
        stack.remove(FDataComponents.BATON_BAND.get());
        if (band.compareTo(ItemTuning.BATON_RATING_BAND) > 0 || attacker.isInWater())
            Shock.apply(attacker, band, eu);
        target.invulnerableTime = 0;   // the swing's own hit has just set it; the discharge is a second blow
        Shock.apply(target, band, eu);
        target.knockback(FTuning.FENCE_KNOCKBACK, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        target.hurtMarked = true;
        CombatTicker.stagger(target, Math.min(ItemTuning.BATON_STAGGER_MAX_TICKS,
                Math.round(eu / ElectricRules.packetEu(band) * ItemTuning.BATON_STAGGER_TICKS_PER_PACKET)), ItemTuning.BATON_STAGGER_SLOW);
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 10, 0.2, 0.3, 0.2, 0.1);
            server.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3f, 2f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (eu(stack) <= 0f)
            lines.add(Component.translatable("tooltip.feedback.jar_baton.empty"));
        else if (io.github.soundgoodizerfan.feedback.client.tooltip.Readout.instrumented(io.github.soundgoodizerfan.feedback.instrument.Quantity.BAND))
            lines.add(Component.translatable("tooltip.feedback.jar_baton.charge",
                    Component.translatable(band(stack).key())));
    }
}
