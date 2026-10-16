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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a modular item was put together as: a type and the parts in it. Nothing derived -- the
 * figures are {@link ModularFigures}, baked from this and the material table. Like the gun's
 * design, it names materials by id (data), never an item.
 *
 * @param type  the {@link ModularType} id, which is also the item's registry path.
 * @param parts one per role the pattern filled, in the type's role order.
 * @param edge  how the edge was last ground.
 */
public record ModularDesign(ResourceLocation type, List<Part> parts, Edge edge) {

    /**
     * One role filled.
     *
     * @param role     the role's letter in the type's patterns.
     * @param material the material the part is made of.
     * @param form     the form it came in -- an ingot, a plank, a length of chain -- which a role
     *                 may weigh differently (mail covers less than plate).
     * @param count    how many items went into it: the pattern's cells for that role, which is
     *                 its size.
     */
    public record Part(String role, ResourceLocation material, String form, int count) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("role").forGetter(Part::role),
                ResourceLocation.CODEC.fieldOf("material").forGetter(Part::material),
                Codec.STRING.fieldOf("form").forGetter(Part::form),
                Codec.INT.fieldOf("count").forGetter(Part::count)
        ).apply(instance, Part::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Part::role,
                ResourceLocation.STREAM_CODEC, Part::material,
                ByteBufCodecs.STRING_UTF8, Part::form,
                ByteBufCodecs.VAR_INT, Part::count,
                Part::new);
    }

    public static final Codec<ModularDesign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(ModularDesign::type),
            Part.CODEC.listOf().fieldOf("parts").forGetter(ModularDesign::parts),
            Edge.CODEC.optionalFieldOf("edge", Edge.STANDARD).forGetter(ModularDesign::edge)
    ).apply(instance, ModularDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularDesign> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ModularDesign::type,
            Part.STREAM_CODEC.apply(ByteBufCodecs.list()), ModularDesign::parts,
            ByteBufCodecs.idMapper(i -> Edge.values()[i], Edge::ordinal), ModularDesign::edge,
            ModularDesign::new);

    /** The part in {@code role}, or null. */
    public Part part(String role) {
        for (Part p : parts)
            if (p.role().equals(role))
                return p;
        return null;
    }

    public ModularDesign withEdge(Edge edge) {
        return new ModularDesign(type, parts, edge);
    }

    public ModularDesign withPart(Part replacement) {
        return new ModularDesign(type, parts.stream()
                .map(p -> p.role().equals(replacement.role()) ? replacement : p).toList(), edge);
    }
}
