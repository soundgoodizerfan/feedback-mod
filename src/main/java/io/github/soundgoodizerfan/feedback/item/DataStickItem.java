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
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A recipe data stick: blank until a player makes a cut at a hand bench with it in the off hand, then it holds that one
 * operation ({@link RecordedCut}) and plays it back in a powered machine. The machine knows nothing; the stick does.
 */
public class DataStickItem extends Item {

    public DataStickItem(Properties properties) {
        super(properties);
    }

    /**
     * A cut was just made at {@code station}: write it onto the stick in the player's off hand, if there is one (blank or recorded).
     * Called from the bench's completion path on the server; {@code in} is the stock before it was spent.
     */
    public static void record(Player player, Level level, String station, ItemStack in, ItemStack result) {
        ItemStack stick = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!(stick.getItem() instanceof DataStickItem) || in.isEmpty() || result.isEmpty())
            return;
        var materials = ClientMaterials.on(level);
        Optional<Material.Located> input = Material.find(materials, in);
        if (input.isEmpty())
            return;
        String kind = input.get().material().kind().map(Material.Kind::getSerializedName).orElse("");
        String resultForm = Material.find(materials, result).map(Material.Located::form).orElse("");
        stick.set(FDataComponents.RECORDED_CUT.get(), new RecordedCut(station, kind, input.get().form(),
                BuiltInRegistries.ITEM.getKey(result.getItem()).toString(), resultForm));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RecordedCut cut = stack.get(FDataComponents.RECORDED_CUT.get());
        if (cut == null) {
            tooltip.add(Component.translatable("tooltip.feedback.data_stick.blank"));
            return;
        }
        Item result = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(cut.result()));
        tooltip.add(Component.translatable("tooltip.feedback.data_stick.cut", result.getDescription(),
                Component.translatable("feedback.jei.kind." + cut.kind()), cut.form()));
    }
}
