// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The crystal world's geodes ring, and a ringing that reaches a brittle piece cracks it. Every {@link SpaceTuning#RESONANCE_SAMPLE_TICKS}
 * ticks each body in that dimension has {@link SpaceTuning#RESONANCE_SAMPLES} random blocks of its plot looked at: a block as brittle as
 * {@link SpaceTuning#IMPACT_BRITTLE_HARDNESS} or less (glass, panes, thin crystal) breaks with {@link SpaceTuning#RESONANCE_CHANCE}, with a
 * chime. A hull of tougher parts is untouched; the damage is a lost block, so it is seen and patched.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class CrystalResonance {

    private CrystalResonance() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(PlanetRoute.CRYSTAL.dimension())
                || level.getGameTime() % SpaceTuning.RESONANCE_SAMPLE_TICKS != 0)
            return;
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            var box = body.getPlot().getBoundingBox();
            for (int i = 0; i < SpaceTuning.RESONANCE_SAMPLES; i++) {
                BlockPos at = new BlockPos(
                        box.minX() + level.random.nextInt(box.maxX() - box.minX() + 1),
                        box.minY() + level.random.nextInt(box.maxY() - box.minY() + 1),
                        box.minZ() + level.random.nextInt(box.maxZ() - box.minZ() + 1));
                var state = level.getBlockState(at);
                if (state.isAir())
                    continue;
                float hardness = state.getDestroySpeed(level, at);
                if (hardness >= 0f && hardness <= SpaceTuning.IMPACT_BRITTLE_HARDNESS && level.random.nextFloat() < SpaceTuning.RESONANCE_CHANCE) {
                    level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1f);
                    level.destroyBlock(at, false);
                }
            }
        }
    }
}
