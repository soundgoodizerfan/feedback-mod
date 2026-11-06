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
package io.github.soundgoodizerfan.feedback.data;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * A flat model for every generated form item: one greyscale sprite per form
 * ({@code textures/item/form/<form>.png}), tinted per material at runtime by {@code client/FormTint}.
 */
public class FormItemModels extends ItemModelProvider {

    public FormItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Feedback.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (FormItems.Generated generated : FormItems.all())
            if (generated.block() == null) // block forms get their item model with the block state
                withExistingParent(generated.item().getId().getPath(), mcLoc("item/generated"))
                        .texture("layer0", Feedback.id("item/form/" + sprite(generated)));
    }

    /** The form's sprite, or a per-kind one when the form has it: polymer string, meat rod (animal and seafood). */
    private static String sprite(FormItems.Generated generated) {
        var material = FormItems.materials().get(generated.material());
        var kind = material == null ? null : material.kind().orElse(null);
        if (kind == Material.Kind.POLYMER && generated.form().equals("string"))
            return "string_polymer";
        if ((kind == Material.Kind.ANIMAL || kind == Material.Kind.SEAFOOD) && generated.form().equals("rod"))
            return "rod_meat";
        return generated.form();
    }
}
