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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.Density;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * @param forms          form name to the item that is this material in that form, for forms
 *                       whose item already exists (vanilla's copper ingot, or a name that was
 *                       registered before forms were generated). A form step names forms, never
 *                       items.
 * @param density        how heavy the stuff is, whatever shape it takes (an engine's metal, later
 *                       plates and tubes). Optional, like everything but hardness: zero means a
 *                       material nobody has weighed yet, and a consumer that needs it refuses it.
 * @param exclude        forms this material does not have. Every form a step names is generated
 *                       for every material unless it is listed in {@link #forms} or here --
 *                       absence is the thing that is stated, so a new form reaches every
 *                       material without editing any of them.
 *
 * <h2>Generated items</h2>
 * Items are registered before a datapack exists, so {@link FormItems} reads the mod's own
 * material and form files from the jar at registration. A form this material has but does not
 * list is the item {@code <namespace>:<material>_<form>}, resolved at compile by {@link #items}.
 * A datapack material can retune figures and name existing items, but cannot mint new ones.
 */
public record Material(St hardness, Tu minTemperature, Tu maxTemperature,
                       Map<String, ResourceLocation> forms, List<String> exclude, Density density) {

    /** Hardness divides, so zero or less is refused at load rather than turning into Infinity Fu. */
    static final Codec<St> HARDNESS_CODEC = Units.codec(St::new).validate(st -> st.value() > 0
            ? DataResult.success(st) : DataResult.error(() -> "hardness must be positive: " + st.value()));

    public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HARDNESS_CODEC.fieldOf("hardness").forGetter(Material::hardness),
            Units.codec(Tu::new).optionalFieldOf("min_temperature", new Tu(0f)).forGetter(Material::minTemperature),
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(Material::maxTemperature),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("forms", Map.of()).forGetter(Material::forms),
            Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(Material::exclude),
            Units.codec(Density::new).optionalFieldOf("density", new Density(0f)).forGetter(Material::density)
    ).apply(instance, Material::new));

    /** The forms of {@code universe} this material has no explicit item for and does not exclude. */
    public Set<String> generated(Collection<String> universe) {
        Set<String> out = new TreeSet<>(universe);
        out.removeAll(forms.keySet());
        out.removeAll(exclude);
        return out;
    }

    /**
     * The item that is this material in {@code form} -- its ingot, its plate -- if it has one: the
     * explicit entry, else the generated item unless the form is excluded. For a consumer that
     * needs one form (an engine's cost in ingots) rather than the whole map.
     */
    public java.util.Optional<Item> item(ResourceLocation id, String form) {
        if (forms.containsKey(form))
            return BuiltInRegistries.ITEM.getOptional(forms.get(form));
        if (exclude.contains(form))
            return java.util.Optional.empty();
        return BuiltInRegistries.ITEM.getOptional(FormItems.itemId(id, form));
    }

    /**
     * Form name to item, explicit entries and generated ones together. A form whose item is not
     * registered is left out rather than failing: a step then simply does not apply.
     */
    public Map<String, Item> items(ResourceLocation id, Collection<String> universe) {
        Map<String, Item> out = new LinkedHashMap<>();
        forms.forEach((form, item) -> BuiltInRegistries.ITEM.getOptional(item).ifPresent(i -> out.put(form, i)));
        for (String form : generated(universe))
            BuiltInRegistries.ITEM.getOptional(FormItems.itemId(id, form)).ifPresent(i -> out.put(form, i));
        return out;
    }
}
