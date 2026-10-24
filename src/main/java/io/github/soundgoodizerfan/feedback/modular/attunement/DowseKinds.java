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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The runtime kinds of the Dowse effects ({@code docs/feedback_enchant_roster.md} 14): a needle that leans toward
 * the nearest block with a property, one effect per property (the choice is which effect is attuned, so each
 * costs a pole of its own): magnetic, dense, conductive, flammable. {@code point_<property>} points at it,
 * {@code away_<property>} (the mirror) points the other way. It shows in the action bar as an arrow relative to
 * where the holder faces, and reads and does nothing else: no distance, no unlock. It reads the <em>nearest</em>
 * such block, so a vein behind a wall of the same property reads as the wall. A magnetic needle in a part whose
 * material has a Curie point stops pointing once the stack is heated past it. Not built: neighbouring metal in
 * the same kit pulling it off true. Armour face ({@code worn_point_*}/{@code worn_away_*}, roster 6b): the same
 * needle read from a worn helmet or chestplate part when nothing held reads.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class DowseKinds {

    /** Reach in blocks and ticks between readings. [OPEN] until played */
    private static final int RANGE = 12, INTERVAL_TICKS = 20;
    /** Density (Mu/mB) above which a block is dense: over iron's, so the heavy seams, not the common metal. [OPEN] until played */
    private static final float DENSE_MIN = 8f;
    /** Prefix of the armour face of a needle kind: read only from a helmet or chestplate part, a worn instrument. */
    private static final String WORN = "worn_";
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static final Map<String, Predicate<Material>> PROPERTIES = Map.of(
            "magnetic", m -> m.separation().magnetic(),
            "dense", m -> m.density().muPerMb() >= DENSE_MIN,
            "conductive", m -> m.electrical().conductivity().isPresent(),
            "flammable", m -> m.traits().contains(Material.Trait.FLAMMABLE));

    /** What each property found per block, so a reading does not look every block up in the material table; dropped when the table reloads. */
    private static final Map<String, Map<Block, Boolean>> CACHE = new HashMap<>();
    private static Object cachedFor;

    private DowseKinds() {
    }

    /** The property of a needle kind, or null: {@code point_magnetic} and {@code worn_point_magnetic} are {@code magnetic}. */
    private static String property(String kind) {
        if (kind.startsWith(WORN))
            kind = kind.substring(WORN.length());
        for (String prefix : new String[]{"point_", "away_"})
            if (kind.startsWith(prefix) && PROPERTIES.containsKey(kind.substring(prefix.length())))
                return kind.substring(prefix.length());
        return null;
    }

    private static boolean has(String property, Block block) {
        var materials = MaterialTable.get().materials();
        if (cachedFor != materials) {
            CACHE.clear();
            cachedFor = materials;
        }
        return CACHE.computeIfAbsent(property, p -> new HashMap<>()).computeIfAbsent(block, b -> {
            var found = Material.find(materials, b.asItem());
            return found.isPresent() && PROPERTIES.get(property).test(found.get().material());
        });
    }

    public static void register() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % INTERVAL_TICKS != 0)
            return;
        // Held needle first, then a helmet, then a chestplate; the first that reads owns the action bar.
        if (!read(player, player.getMainHandItem(), false) && !read(player, player.getItemBySlot(EquipmentSlot.HEAD), true))
            read(player, player.getItemBySlot(EquipmentSlot.CHEST), true);
    }

    /** Shows {@code stack}'s needle, if it has one of the right face (worn kinds only on worn stacks); true if it did. */
    private static boolean read(Player player, ItemStack stack, boolean worn) {
        if (stack.isEmpty())
            return false;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack)) {
            String property = property(a.kind());
            if (property == null || a.kind().startsWith(WORN) != worn)
                continue;
            if (property.equals("magnetic") && a.material().separation().curieTemperature()
                    .map(c -> ItemHeat.get(stack, player.level()).value() > c.value()).orElse(false)) {
                player.displayClientMessage(Component.translatable("feedback.dowse.dead"), true);
                return true;
            }
            BlockPos near = nearest(player, property);
            if (near == null) {
                player.displayClientMessage(Component.translatable("feedback.dowse.none"), true);
                return true;
            }
            double dx = near.getX() + 0.5 - player.getX(), dz = near.getZ() + 0.5 - player.getZ();
            // Bearing from the way the holder faces: yaw 0 is south (+z), and the arrow's up is straight ahead.
            double bearing = Math.toDegrees(Math.atan2(-dx, dz)) - player.getYRot();
            if (a.kind().contains("away_"))
                bearing += 180;
            int arrow = Math.floorMod(Math.round(Mth.wrapDegrees((float) -bearing) / 45f), 8);
            // Directly over or under it, there is nothing to lean toward.
            boolean here = dx * dx + dz * dz < 1.5;
            player.displayClientMessage(Component.translatable("feedback.dowse.needle", here ? "·" : ARROWS[arrow]), true);
            return true;
        }
        return false;
    }

    private static BlockPos nearest(Player player, String property) {
        Level level = player.level();
        BlockPos centre = player.blockPosition(), best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RANGE, -RANGE, -RANGE), centre.offset(RANGE, RANGE, RANGE))) {
            double d = pos.distSqr(centre);
            if (d >= bestDist || d > (double) RANGE * RANGE || !level.isLoaded(pos))
                continue;
            if (has(property, level.getBlockState(pos).getBlock())) {
                best = pos.immutable();
                bestDist = d;
            }
        }
        return best;
    }
}
