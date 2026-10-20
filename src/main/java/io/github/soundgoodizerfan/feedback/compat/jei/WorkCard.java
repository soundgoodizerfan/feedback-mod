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
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.Operation;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The card {@link DeformationCategory} and {@link RemovalCategory} share: input and output slots
 * down the left, a headed list of figures to the right. The two records differ only in what their
 * hardness means, so the caller names that row's label.
 */
final class WorkCard {

    static final int WIDTH = 192;
    static final int HEIGHT = 102;

    /** Where the labels start, clear of the slot column, and where their values line up. */
    private static final int LABEL_X = 24;
    private static final int VALUE_X = 72;

    /**
     * Colours for JEI's light grey recipe background.
     * <p>
     * Worth stating because it was got wrong once: these were first written as near-white on the
     * assumption of a dark panel, and the whole card came out washed out and hard to read. The
     * background belongs to JEI, not to us, and it is light.
     */
    private static final int TITLE_COLOUR = 0xFF3F3F3F;
    private static final int LABEL_COLOUR = 0xFF7A7A7A;
    private static final int VALUE_COLOUR = 0xFF262626;
    private static final int RULE_COLOUR = 0x40000000;

    private WorkCard() {
    }

    static void slots(IRecipeLayoutBuilder builder, Ingredient input, ItemStack result) {
        builder.addInputSlot(1, 13).setStandardSlotBackground().addIngredients(input);
        builder.addOutputSlot(1, 57).setOutputSlotBackground().addItemStack(result);
    }

    /**
     * @param lastKey   a sixth row's label, or null for none: the hot-working window on a
     *                  deformation, the swarf shed on a removal. Both are things the card used to
     *                  leave out -- steel's plate read as if any heat would do.
     * @param lastValue that row's value.
     */
    static void draw(GuiGraphics graphics, Operation operation, Ingredient input, int work,
                     String hardnessKey, St hardness, ItemStack result, String lastKey, String lastValue) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, result.getHoverName().getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        // A hairline under the title so the card reads as a headed entry rather than loose lines.
        graphics.fill(1, 11, WIDTH - 1, 12, RULE_COLOUR);

        ItemStack[] accepted = input.getItems();
        row(graphics, font, 18, "feedback.jei.operation", Component.translatable(operation.translationKey()).getString());
        row(graphics, font, 30, "feedback.jei.input", quantity(accepted.length == 0 ? ItemStack.EMPTY : accepted[0]));
        row(graphics, font, 42, "feedback.jei.work", work + " Fu");
        row(graphics, font, 54, hardnessKey, Readout.number(hardness.value()));
        row(graphics, font, 66, "feedback.jei.output", quantity(result));
        if (lastKey != null)
            row(graphics, font, 84, lastKey, lastValue);
    }

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        graphics.drawString(font, Component.translatable(labelKey), LABEL_X, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, VALUE_X, y, VALUE_COLOUR, false);
    }

    private static String quantity(ItemStack stack) {
        return stack.isEmpty() ? "-" : stack.getCount() + " × " + stack.getHoverName().getString();
    }
}
