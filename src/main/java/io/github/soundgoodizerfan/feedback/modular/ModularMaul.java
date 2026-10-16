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
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A maul: a heavy head on a handle that splits wood and staggers what it hits. The stagger is the
 * blow itself -- a slow for as long as the impact is worth, so a heavier head holds its target
 * longer -- rather than a flat effect (spec 5).
 */
public class ModularMaul extends ModularItem {

    /** Ticks of slow per joule of impact. */
    private static final float STAGGER_TICKS_PER_JOULE = 2f;

    public ModularMaul(Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ModularFigures f = ModularStacks.figures(stack);
        if (f != null && !ModularStacks.broken(stack))
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    Math.round(f.get(ModularFigures.Fig.IMPACT) * STAGGER_TICKS_PER_JOULE), 1), attacker);
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
    }
}
