// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.List;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.resources.ResourceLocation;

/** What the GUI shot tool ({@code client/GuiShot}) asks of JEI: the recipe categories there are, and a category's screen opened. Client only. */
public final class JeiShots {

    private JeiShots() {
    }

    /** The ids of every recipe category whose id is in {@code namespace}, or every category when {@code namespace} is empty. */
    public static List<String> categories(String namespace) {
        IJeiRuntime runtime = FeedbackJeiPlugin.runtime();
        if (runtime == null)
            return List.of();
        return runtime.getRecipeManager().createRecipeCategoryLookup().get()
                .map(c -> c.getRecipeType().getUid().toString())
                .filter(id -> namespace.isEmpty() || id.startsWith(namespace + ":"))
                .sorted().toList();
    }

    /** Opens JEI's recipes screen on the category {@code uid}; false when JEI is not up or has no such category. */
    public static boolean open(String uid) {
        IJeiRuntime runtime = FeedbackJeiPlugin.runtime();
        ResourceLocation id = ResourceLocation.tryParse(uid);
        if (runtime == null || id == null)
            return false;
        var type = runtime.getRecipeManager().getRecipeType(id);
        if (type.isEmpty())
            return false;
        runtime.getRecipesGui().showTypes(List.<RecipeType<?>>of(type.get()));
        return true;
    }
}
