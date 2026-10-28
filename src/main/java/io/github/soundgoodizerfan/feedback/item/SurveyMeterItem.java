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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * The survey meter (mechanics §9.5): use it and it reports the ore around you within {@link
 * FTuning#SURVEY_RADIUS} blocks, by kind, with a bearing to where the most of each lies. Read-only,
 * powered by nothing, and no key: it unlocks no ore, it only saves digging blind.
 *
 * <p>It reads the count of ore blocks to a resolution of {@link FTuning#SURVEY_RESOLUTION}, with a
 * wobble of up to half a step on each reading (the noise floor, philosophy 8): a lean vein under one
 * step reads as nothing some of the time, a fat one never reads the same twice. A finer meter would
 * only narrow both.
 */
public class SurveyMeterItem extends Item implements Instrument {

    public SurveyMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.ORE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.SURVEY_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.survey_meter");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            player.getCooldowns().addCooldown(this, FTuning.SURVEY_COOLDOWN_TICKS);
            report(level, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void report(Level level, Player player) {
        int r = FTuning.SURVEY_RADIUS;
        BlockPos centre = player.blockPosition();
        // Per kind of ore (deepslate and stone are one kind): how many, and where their centre lies.
        Map<String, long[]> found = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    var state = level.getBlockState(pos);
                    if (!state.is(Tags.Blocks.ORES))
                        continue;
                    String key = state.getBlock().getDescriptionId().replace("deepslate_", "");
                    long[] sum = found.computeIfAbsent(key, k -> new long[4]);
                    sum[0]++;
                    sum[1] += dx;
                    sum[2] += dy;
                    sum[3] += dz;
                }
        // An oil basin is not blocks (petrochemistry spec 2.5): it reads from how near the nearest one's
        // circle lies, to the same resolution and wobble, with a bearing to its centre.
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            long[] best = null;
            for (var site : io.github.soundgoodizerfan.feedback.geology.BasinFeature.sitesNear(server.getSeed(),
                    level.dimension().location(), centre.getX(), centre.getZ())) {
                int dx = site.centre().getX() - centre.getX(), dz = site.centre().getZ() - centre.getZ();
                long n = Math.round((site.radius() + r - Math.hypot(dx, dz)) * FTuning.SURVEY_OIL_PER_BLOCK);
                if (n > 0 && (best == null || n > best[0]))
                    best = new long[]{n, dx * n, (site.centre().getY() - centre.getY()) * n, dz * n};
            }
            if (best != null)
                found.put("feedback.survey.oil", best);
        }
        var lines = found.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong((Map.Entry<String, long[]> e) -> -e.getValue()[0]))
                .limit(4)
                .map(e -> {
                    long[] s = e.getValue();
                    float wobble = (level.random.nextFloat() - 0.5f) * FTuning.SURVEY_RESOLUTION;
                    float reading = Instruments.quantise(s[0] + wobble, FTuning.SURVEY_RESOLUTION);
                    return reading <= 0 ? null : Component.translatable("feedback.survey.line",
                            Component.translatable(e.getKey()),
                            Instruments.format(reading, FTuning.SURVEY_RESOLUTION), bearing(s));
                })
                .filter(java.util.Objects::nonNull).toList();
        if (lines.isEmpty()) {
            player.displayClientMessage(Instruments.signed(this, Component.translatable("feedback.survey.none")), true);
            return;
        }
        for (var line : lines)
            player.sendSystemMessage(Instruments.signed(this, line));
    }

    /** Where the mean of the ore lies from the holder: nearest of eight horizontal points, and above or below. */
    private static Component bearing(long[] s) {
        double x = s[1] / (double) s[0], y = s[2] / (double) s[0], z = s[3] / (double) s[0];
        Direction side = Direction.fromYRot(Math.toDegrees(Math.atan2(-x, z)));
        boolean near = Math.hypot(x, z) < 3;
        Component horizontal = near ? Component.translatable("feedback.survey.here")
                : Component.translatable("feedback.survey.dir." + side.getName());
        if (Math.abs(y) < 3)
            return horizontal;
        return Component.translatable("feedback.survey.vertical", horizontal,
                Component.translatable(y > 0 ? "feedback.survey.above" : "feedback.survey.below"));
    }
}
