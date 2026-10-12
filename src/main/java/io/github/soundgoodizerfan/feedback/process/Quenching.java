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
package io.github.soundgoodizerfan.feedback.process;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Throwing hot metal in water.
 *
 * <h2>Why this is an event on a dropped item and not a machine</h2>
 * Because that is what quenching is. There was a version of this with a quench tank block in it,
 * and the tank added nothing except a place to stand -- the interesting decision is entirely in
 * <em>whether the ingot is still hot enough when it lands</em>, and that decision is the walk
 * from the crucible, which already exists. A block would have been a container for a mechanic
 * that does not need one, and philosophy 3 cuts mechanics that are real, well-precedented and
 * have no choice inside them.
 * <p>
 * The consequence is better than the block would have been: the player quenches by throwing the
 * ingot, anywhere, into any water. It is the most physical action in the mod and it needed no
 * machinery at all.
 *
 * <h2>What it teaches</h2>
 * The same ingot at the same final temperature is a different item depending on how it got there.
 * Nothing about the end state distinguishes the two. That is philosophy 7's history-sensitivity
 * and the first requirement in the slice that cannot be satisfied by getting a number right --
 * and it is real metallurgy, which is the point of §2: the mod did not invent this rule, it
 * noticed it.
 * <p>
 * Missing the window costs nothing but the trip. A cold ingot in water is a cold ingot.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class Quenching {

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item))
            return;

        Level level = item.level();
        if (level.isClientSide || !item.isInWater())
            return;

        ItemStack stack = item.getItem();
        // Cheapest check first: the overwhelming majority of dropped items were never heated, and
        // this listener sees every ticking entity in the world.
        if (!stack.has(FDataComponents.TEMPERATURE.get()))
            return;

        float tu = ItemHeat.get(stack, level).value();

        Optional<Quench> maybe = QuenchTable.get().find(stack);
        if (maybe.isPresent() && tu >= maybe.get().minTemperature().value()) {
            ItemStack hardened = maybe.get().result().copyWithCount(stack.getCount());
            item.setItem(hardened);
        } else {
            // Not quenchable, or it cooled on the way. The heat is gone either way -- water does
            // not care whether anything useful happened. A copy set back, not a mutation in place,
            // so the entity's synced data notices and clients see the change.
            ItemStack cooled = stack.copy();
            ItemHeat.clear(cooled);
            item.setItem(cooled);
        }

        // A stamp that has long since cooled to the room makes no steam.
        if (tu > AmbientTable.at(level).value())
            hiss(level, item, tu);
    }

    private static void hiss(Level level, ItemEntity item, float tu) {
        level.playSound(null, item.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 2.0f);
        if (level instanceof ServerLevel server)
            server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    item.getX(), item.getY() + 0.2, item.getZ(),
                    (int) Math.min(24, tu / 60f), 0.2, 0.1, 0.2, 0.01);
    }
}
