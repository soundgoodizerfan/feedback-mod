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

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * What a substance <em>is</em>, as a table of figures. Everything about how it can be worked is
 * computed from these on reload by {@link FormTable}; no recipe names a material.
 *
 * <h2>A bag of values, checked only by whoever needs one</h2>
 * Hardness is the only value every material has, because {@code Fu per blow = St / hardness}
 * needs it. Anything else (a hot-working window today; melting point, thermal mass, cooling rate
 * later) is optional and costs nothing until a system asks. This is the shape GregTech's
 * {@code Material} uses -- a map of optional typed properties, null-checked by the consumer --
 * minus its flag layer, which a form's existence in {@link #forms} makes redundant.
 *
 * <h2>Hardness belongs to the state, so hardened steel is its own material</h2>
 * Quenched steel is harder than steel, and removal reads hardness from the material. Rather than
 * letting a form override it, a quench maps one material's item to another's, which is what
 * actually happened to the metal.
 *
 * @param hardness       see {@link Deformation} -- the floor and the divisor.
 * @param minTemperature how hot a workpiece must be to be deformed, in Tu. Zero for cold-workable.
 *                       Removal ignores it.
 * @param maxTemperature how hot it may be and still be deformed. Unbounded unless stated.
 * @param forms          form name to the item that is this material in that form. This is the
 *                       only place an item id appears; a form step names forms, never items.
 */
public record Material(St hardness, Tu minTemperature, Tu maxTemperature, Map<String, Item> forms) {

    /** Hardness divides, so zero or less is refused at load rather than turning into Infinity Fu. */
    static final Codec<St> HARDNESS_CODEC = Units.codec(St::new).validate(st -> st.value() > 0
            ? DataResult.success(st) : DataResult.error(() -> "hardness must be positive: " + st.value()));

    public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HARDNESS_CODEC.fieldOf("hardness").forGetter(Material::hardness),
            Units.codec(Tu::new).optionalFieldOf("min_temperature", new Tu(0f)).forGetter(Material::minTemperature),
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(Material::maxTemperature),
            Codec.unboundedMap(Codec.STRING, BuiltInRegistries.ITEM.byNameCodec()).fieldOf("forms").forGetter(Material::forms)
    ).apply(instance, Material::new));
}
