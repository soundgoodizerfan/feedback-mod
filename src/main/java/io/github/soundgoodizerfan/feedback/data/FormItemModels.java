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

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * A flat model for every generated form item. Textures are vanilla placeholders chosen by form,
 * like every other texture in the mod until there is art; the point of the table is that a form
 * looks the same across materials until it is given its own.
 */
public class FormItemModels extends ItemModelProvider {

    private static final String DEFAULT_TEXTURE = "item/iron_nugget";
    private static final Map<String, String> TEXTURES = Map.ofEntries(
            Map.entry("plate", "item/iron_ingot"),
            Map.entry("drilled_plate", "item/iron_ingot"),
            Map.entry("ground_ingot", "item/iron_ingot"),
            Map.entry("foil", "item/paper"),
            Map.entry("rod", "item/stick"),
            Map.entry("turned_rod", "item/stick"),
            Map.entry("wire", "item/string"),
            Map.entry("fine_wire", "item/string"),
            Map.entry("swarf", "item/gunpowder"),
            Map.entry("gun_barrel", "item/stick"),
            Map.entry("magazine_tube", "item/stick"),
            Map.entry("bolt", "item/stick"),
            Map.entry("receiver", "item/iron_ingot"),
            Map.entry("slide", "item/iron_ingot"),
            Map.entry("grip", "item/iron_ingot"));

    public FormItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Feedback.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (FormItems.Generated generated : FormItems.all())
            if (generated.block() == null) // block forms get their item model with the block state
                withExistingParent(generated.item().getId().getPath(), mcLoc("item/generated"))
                        .texture("layer0", ResourceLocation.withDefaultNamespace(
                                TEXTURES.getOrDefault(generated.form(), DEFAULT_TEXTURE)));
    }
}
