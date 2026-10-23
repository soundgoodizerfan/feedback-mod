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

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The runtime kinds of the Yield effect ({@code docs/feedback_enchant_roster.md}). {@code fortune} (Looting and Fortune): a
 * head gets more out of whatever it breaks or kills. {@code intact} (Silk Touch, the mirror): the block comes out whole.
 * Both subscribe to the drop events themselves and read a part's attunements through {@link MaterialPerks#attunements}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class YieldKinds {

    /** Chance per fortune level that each drop is doubled, capped at 1. Per-level, not a table. [OPEN] until played. */
    private static final float CHANCE_PER_LEVEL = 0.25f;

    private YieldKinds() {
    }

    static void register() {
    }

    private static int level(ItemStack stack, String kind) {
        int sum = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals(kind))
                sum += a.level();
        return sum;
    }

    /** One extra copy of each drop, each with chance {@code level * CHANCE_PER_LEVEL}. */
    private static List<ItemEntity> extras(Level world, List<ItemEntity> drops, int level) {
        List<ItemEntity> out = new ArrayList<>();
        for (ItemEntity drop : drops)
            if (world.random.nextFloat() < level * CHANCE_PER_LEVEL)
                out.add(new ItemEntity(world, drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        return out;
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        List<ItemEntity> drops = event.getDrops();
        if (!(event.getBreaker() instanceof Player) || drops.isEmpty())
            return;
        net.minecraft.world.item.Item own = event.getState().getBlock().asItem();
        boolean selfDrop = drops.stream().anyMatch(d -> d.getItem().is(own));
        int intact = level(tool, "intact");
        // Whole block only where the loot table gives something else; block entities hold state a bare item would lose.
        if (intact > 0 && !selfDrop && event.getBlockEntity() == null && own != net.minecraft.world.item.Items.AIR) {
            ItemEntity first = drops.get(0);
            ItemEntity whole = new ItemEntity(event.getLevel(), first.getX(), first.getY(), first.getZ(), new ItemStack(own));
            drops.clear();
            drops.add(whole);
            return;
        }
        // Doubling is for blocks that give something other than themselves (ores, crops); no block ids are named.
        int fortune = level(tool, "fortune");
        if (fortune > 0 && !selfDrop)
            drops.addAll(extras(event.getLevel(), drops, fortune));
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof LivingEntity attacker) || event.getEntity() instanceof Player)
            return;
        int fortune = level(StrikeSource.weapon(attacker, event.getSource()), "fortune");
        if (fortune > 0)
            event.getDrops().addAll(extras(event.getEntity().level(), new ArrayList<>(event.getDrops()), fortune));
    }
}
