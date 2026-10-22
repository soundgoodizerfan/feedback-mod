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
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Butcher's cleaver (spec 5.4): {@code animal_mult} against livestock and wildlife, and each food
 * drop of an animal it kills has {@code extra_chance} of dropping a second helping. Against
 * monsters it is a poor weapon, {@code monster_mult}.
 */
public final class Butcher implements ModularTrait {

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        LivingEntity target = event.getEntity();
        if (target instanceof Animal)
            event.setAmount(event.getAmount() * params.get("animal_mult", 1.5f));
        else if (target.getType().getCategory() == MobCategory.MONSTER)
            event.setAmount(event.getAmount() * params.get("monster_mult", 0.85f));
    }

    @EventBusSubscriber(modid = Feedback.MOD_ID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onDrops(LivingDropsEvent event) {
            if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide
                    || !(event.getSource().getEntity() instanceof LivingEntity killer))
                return;
            ItemStack weapon = io.github.soundgoodizerfan.feedback.modular.StrikeSource.weapon(killer, event.getSource());
            var active = ModularTraits.of(weapon).filter(a -> a.trait() instanceof Butcher);
            if (active.isEmpty())
                return;
            float chance = active.get().params().get("extra_chance", 0.5f);
            List<ItemEntity> extra = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                if (drop.getItem().has(DataComponents.FOOD) && animal.getRandom().nextFloat() < chance)
                    extra.add(new ItemEntity(animal.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
            event.getDrops().addAll(extra);
        }
    }
}
