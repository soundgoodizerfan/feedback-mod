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

import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A substrate with traces laid on it (mechanics 6.20). Everything about it is the {@link Board}
 * component; the item only says what the component records. A board with no component (one from a
 * creative tab) reads as unfinished. It is the Basic Electronic Circuit's raw input in phase 4.
 */
public class BoardItem extends Item {

    public BoardItem(Properties properties) {
        super(properties);
    }

    /** The name of a material as the lang file spells it. */
    public static Component name(ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }

    /** What a board is, one line each, for the tooltip and the component tester. */
    public static void describe(Board board, java.util.function.Consumer<Component> out) {
        out.accept(Component.translatable("feedback.board.trace." + board.trace().getSerializedName(), name(board.traceMaterial())));
        out.accept(Component.translatable("feedback.board.substrate", name(board.substrate())));
        if (!board.parts().isEmpty())
            out.accept(Component.translatable("feedback.board.parts", board.parts().size()));
        out.accept(board.solder().isEmpty()
                ? Component.translatable("feedback.board.unpasted")
                : Component.translatable("feedback.board.solder", name(board.solder().get()),
                        Component.translatable(board.joint().translationKey())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Board board = stack.get(FDataComponents.BOARD.get());
        if (board != null)
            describe(board, tooltip::add);
    }
}
