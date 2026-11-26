// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Acid rain on the acid-cloud world eats a hull that is not made to stand it. Every {@link SpaceTuning#ACID_SAMPLE_TICKS} ticks, each body in that
 * dimension has {@link SpaceTuning#ACID_SAMPLES} random blocks of its plot looked at: a block of a material whose {@code corrosion_resistance} is
 * under {@link HazardTuning#CORROSIVE_BITE} and that has air on a face (the rain reaches it) is eaten with a chance that grows with how far under
 * it falls, as {@link io.github.soundgoodizerfan.feedback.core.hazard.Corrosion} does for a vessel. A hull of a resistant metal (stainless,
 * titanium, noble metals) is untouched; the damage is a lost block, not a hidden condition, so it can be seen and patched.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class AcidRain {

    private AcidRain() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(PlanetRoute.ACID_CLOUD.dimension())
                || level.getGameTime() % SpaceTuning.ACID_SAMPLE_TICKS != 0)
            return;
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        var materials = MaterialTable.get().materials();
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            var box = body.getPlot().getBoundingBox();
            for (int i = 0; i < SpaceTuning.ACID_SAMPLES; i++) {
                BlockPos at = new BlockPos(
                        box.minX() + level.random.nextInt(box.maxX() - box.minX() + 1),
                        box.minY() + level.random.nextInt(box.maxY() - box.minY() + 1),
                        box.minZ() + level.random.nextInt(box.maxZ() - box.minZ() + 1));
                if (!(level.getBlockState(at).getBlock() instanceof FormBlock block))
                    continue;
                var material = materials.get(block.material());
                float resistance = material == null ? 1f : material.corrosionResistance().orElse(1f);
                if (resistance >= HazardTuning.CORROSIVE_BITE || !exposed(level, at))
                    continue;
                float share = (HazardTuning.CORROSIVE_BITE - resistance) / HazardTuning.CORROSIVE_BITE;
                if (level.random.nextFloat() < share * SpaceTuning.ACID_ERODE_CHANCE)
                    level.destroyBlock(at, false);
            }
        }
    }

    private static boolean exposed(ServerLevel level, BlockPos at) {
        for (Direction d : Direction.values())
            if (level.getBlockState(at.relative(d)).isAir())
                return true;
        return false;
    }
}
