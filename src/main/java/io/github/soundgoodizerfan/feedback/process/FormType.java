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
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A form that is generated: {@code data/feedback/form_type/<form>.json}. Any other form name a
 * material lists ({@code horse_armor}, {@code sensor}) is an association only -- it names an item
 * that already exists and generates nothing.
 *
 * @param kinds which {@link Material.Kind}s get this form generated. A plate is a metal thing; a
 *              stone plate would need its own form type, not a wider one.
 * @param block whether the form is a placed block (a storage block) rather than an item.
 * @param part  whether the form is a shaped part ({@link io.github.soundgoodizerfan.feedback.modular.ModularPartItem}):
 *              one shared item, the material on the stack ({@link MadeOf}), instead of an item per material.
 */
public record FormType(List<Material.Kind> kinds, boolean block, boolean part) {

    public static final Codec<FormType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Material.Kind.CODEC.listOf().fieldOf("kinds").forGetter(FormType::kinds),
            Codec.BOOL.optionalFieldOf("block", false).forGetter(FormType::block),
            Codec.BOOL.optionalFieldOf("part", false).forGetter(FormType::part)
    ).apply(instance, FormType::new));

    public boolean appliesTo(Material material) {
        return material.kind().map(kinds::contains).orElse(false);
    }
}
