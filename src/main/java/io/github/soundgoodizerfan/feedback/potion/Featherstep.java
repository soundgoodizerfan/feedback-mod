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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Featherstep: vanilla slow falling, carried alongside, plus walking on powder snow like leather boots. Pressure plates are not handled: vanilla
 * counts every non-{@code isIgnoringBlockTriggers} entity on a plate and NeoForge has no event for it, so a clean hook does not exist.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Featherstep {

    private Featherstep() {}

    /** Carry the vanilla slow falling for the same length, no icon or particles of its own. */
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e.is(FEffects.FEATHERSTEP) && !event.getEntity().level().isClientSide)
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, e.getDuration(), 0, e.isAmbient(), false, false));
    }

    /**
     * Powder snow's collision is fixed by the boots (NeoForge hooks only item stacks), so an effect lifts the player to the top of the layer
     * instead. Runs on both sides: the client owns a player's movement. Sneaking sinks, as in vanilla.
     */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.hasEffect(FEffects.FEATHERSTEP) || p.isShiftKeyDown() || p.getAbilities().flying)
            return;
        BlockPos pos = p.blockPosition();
        if (!p.level().getBlockState(pos).is(Blocks.POWDER_SNOW) || p.level().getBlockState(pos.above()).is(Blocks.POWDER_SNOW))
            return;
        if (p.getY() < pos.getY() + 1) {
            p.setPos(p.getX(), pos.getY() + 1, p.getZ());
            p.setDeltaMovement(p.getDeltaMovement().x, Math.max(0, p.getDeltaMovement().y), p.getDeltaMovement().z);
            p.setOnGround(true);
            p.fallDistance = 0;
        }
    }
}
