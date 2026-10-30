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
 */
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Green thumb: crops and saplings near the drinker grow faster, a ranged and weaker bonemeal. Nothing is grown outright: each growable plant in
 * reach is given a few extra {@code randomTick}s, the same call the world makes when it picks the block at random, so the plant's own growth rule
 * (light, moisture, stage chance) decides whether it grows. The trade is the drinker's body: every pass costs food exhaustion, so hunger drains
 * faster while it lasts.
 * <p>
 * {@link #grow} takes a place and a radius, never a player, so a sprinkler or a planter can pour the same potion over its own patch.
 * <p>
 * Cost: one pass per drinker every {@link PotionTuning#GREEN_THUMB_EVERY} ticks over at most (2r+1)^3 positions (r capped at
 * {@link PotionTuning#GREEN_THUMB_MAX_RADIUS}, 1331 at the default 5), skipping any position not in a loaded, ticking chunk (nothing is
 * force-loaded) and any plant already fully grown. Plants are found by block tag or class ({@code BlockTags.CROPS}, {@code SAPLINGS},
 * {@code CropBlock}, {@code StemBlock}), so modded and Feedback crops (all {@code CropBlock}s) count with no id list.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GreenThumb {

    private GreenThumb() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || p.tickCount % PotionTuning.GREEN_THUMB_EVERY != 0)
            return;
        MobEffectInstance e = p.getEffect(FEffects.GREEN_THUMB);
        if (e == null)
            return;
        float strength = e.getAmplifier() + 1;
        grow(level, p.blockPosition(), PotionTuning.GREEN_THUMB_RADIUS, strength);
        p.causeFoodExhaustion(PotionTuning.GREEN_THUMB_EXHAUSTION * strength); // the growth is paid for in the drinker's own food
    }

    /**
     * One pass: gives each growable crop or sapling within {@code radius} blocks of {@code centre} an expected {@code strength} times
     * {@link PotionTuning#GREEN_THUMB_TICKS} extra random ticks. Strength 1 is a level-I potion.
     */
    public static void grow(ServerLevel level, BlockPos centre, int radius, float strength) {
        int r = Math.min(radius, PotionTuning.GREEN_THUMB_MAX_RADIUS);
        float expected = strength * PotionTuning.GREEN_THUMB_TICKS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                for (int dy = -r; dy <= r; dy++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!level.shouldTickBlocksAt(pos)) // loaded and ticking only; never loads a chunk
                        continue;
                    BlockState state = level.getBlockState(pos);
                    if (!growable(state))
                        continue;
                    int n = (int) expected + (level.random.nextFloat() < expected - (int) expected ? 1 : 0);
                    for (int i = 0; i < n; i++) {
                        state.randomTick(level, pos.immutable(), level.random);
                        state = level.getBlockState(pos); // a tree replaces its sapling; a ripe crop stops
                        if (!growable(state))
                            break;
                    }
                }
    }

    /** A crop, stem or sapling that has not finished growing. By tag or class, never by id. */
    private static boolean growable(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop)
            return !crop.isMaxAge(state);
        if (state.getBlock() instanceof StemBlock)
            return true;
        return state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS);
    }
}
