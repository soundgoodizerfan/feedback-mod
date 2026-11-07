// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
