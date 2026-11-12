// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import io.github.soundgoodizerfan.feedback.process.Measures;

/**
 * What a modular item was put together as: a type and the parts in it. Nothing derived -- the
 * figures are {@link ModularFigures}, baked from this and the material table. Like the gun's
 * design, it names materials by id (data), never an item.
 *
 * @param type  the {@link ModularType} id, which is also the item's registry path.
 * @param parts one per role the pattern filled, in the type's role order.
 * @param edge  how the edge was last ground.
 * @param colour a cosmetic RGB the drawing was given (a pulse gun's beam), carried from the blueprint onto the built item; blue by default.
 */
public record ModularDesign(ResourceLocation type, List<Part> parts, Edge edge, int colour) {

    /** The colour a design has until one is chosen: a blue beam. */
    public static final int DEFAULT_COLOUR = 0x3F7FFF;

    public ModularDesign(ResourceLocation type, List<Part> parts, Edge edge) {
        this(type, parts, edge, DEFAULT_COLOUR);
    }

    /**
     * One role filled.
     *
     * @param role     the role's letter in the type's patterns.
     * @param material the material the part is made of.
     * @param form     the form it came in -- an ingot, a plank, a length of chain -- which a role
     *                 may weigh differently (mail covers less than plate).
     * @param count    how many items went into it: the pattern's cells for that role, which is
     *                 its size.
     * @param kind     for a shaped part, which kind it is ({@code pickaxe_head}); empty for raw stock
     *                 and for a part whose role has only one kind. A twin-headed tool's heads are
     *                 told apart by it ({@link ModularHeads}).
     * @param variant  which shape of the part it was made in ({@link ModularType.Variant}); empty for the
     *                 standard one. Its geometry is read from the type, never stored here.
     * @param flaw     how badly it was forged, 0 to 3 ({@code MadeOf#flaw}).
     * @param measures as-built axes (a barrel's real bore, outside and length, by the names its role declares,
     *                 {@link ModularType.Axis}), forge work, attunements and live food state ({@link Measures});
     *                 {@link Measures#NONE} for a part with none. A role axis left out reads the axis's default.
     * @param sub      for a role that holds a finished modular item (a suit's upgrade module): that item, whole, with its own design,
     *                 figures, wear and attunements, and any items nested in it in turn. Empty for every other part. {@code material}
     *                 then names the held item's wearing material, so the part is found like any other.
     */
    public record Part(String role, ResourceLocation material, String form, int count, String kind, String variant, int flaw,
                       Measures measures, ItemStack sub) {

        public Part(String role, ResourceLocation material, String form, int count, String kind, String variant, int flaw,
                    Measures measures) {
            this(role, material, form, count, kind, variant, flaw, measures, ItemStack.EMPTY);
        }

        /** A part that is a finished modular item held whole. */
        public static Part holding(String role, ResourceLocation material, String form, ItemStack held) {
            return new Part(role, material, form, 1, "", "", 0, Measures.NONE, held.copyWithCount(1));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Part p && role.equals(p.role) && material.equals(p.material) && form.equals(p.form) && count == p.count
                    && kind.equals(p.kind) && variant.equals(p.variant) && flaw == p.flaw && measures.equals(p.measures)
                    && ItemStack.matches(sub, p.sub);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(role, material, form, count, kind, variant, flaw, measures, ItemStack.hashItemAndComponents(sub));
        }

        public Part(String role, ResourceLocation material, String form, int count) {
            this(role, material, form, count, "", "", 0, Measures.NONE);
        }

        public Part(String role, ResourceLocation material, String form, int count, String kind, String variant, int flaw) {
            this(role, material, form, count, kind, variant, flaw, Measures.NONE);
        }

        /** This part with {@code m} in place of its measures. */
        public Part withMeasures(Measures m) {
            return new Part(role, material, form, count, kind, variant, flaw, m, sub);
        }

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("role").forGetter(Part::role),
                ResourceLocation.CODEC.fieldOf("material").forGetter(Part::material),
                Codec.STRING.fieldOf("form").forGetter(Part::form),
                Codec.INT.fieldOf("count").forGetter(Part::count),
                Codec.STRING.optionalFieldOf("kind", "").forGetter(Part::kind),
                Codec.STRING.optionalFieldOf("variant", "").forGetter(Part::variant),
                Codec.INT.optionalFieldOf("flaw", 0).forGetter(Part::flaw),
                Measures.CODEC.optionalFieldOf("measures", Measures.NONE).forGetter(Part::measures),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("sub", ItemStack.EMPTY).forGetter(Part::sub)
        ).apply(instance, Part::new));

        /** Eight fields, two more than {@code StreamCodec.composite} takes. */
        public static final StreamCodec<RegistryFriendlyByteBuf, Part> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.role());
                    ResourceLocation.STREAM_CODEC.encode(buf, p.material());
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.form());
                    ByteBufCodecs.VAR_INT.encode(buf, p.count());
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.kind());
                    ByteBufCodecs.STRING_UTF8.encode(buf, p.variant());
                    ByteBufCodecs.VAR_INT.encode(buf, p.flaw());
                    Measures.STREAM_CODEC.encode(buf, p.measures());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.sub());
                },
                buf -> new Part(ByteBufCodecs.STRING_UTF8.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), Measures.STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));

        /**
         * Thickness and mass of this part against the drawing, from the work its forging did against
         * the work it asked ({@link ForgeRhythm#WORK}): short is thick and heavy, overrun thin and light.
         * 1 for a part that was not forged by hand, which is the drawing.
         */
        public float built() {
            return 1f / measures.work();
        }

        /** The as-built axis called {@code name}, else {@code fallback}. */
        public float axis(String name, float fallback) {
            return measures.axis(name, fallback);
        }

        /** The live state entry called {@code name} (food parts), else {@code fallback}. */
        public float state(String name, float fallback) {
            return measures.state(name, fallback);
        }
    }

    public static final Codec<ModularDesign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(ModularDesign::type),
            Part.CODEC.listOf().fieldOf("parts").forGetter(ModularDesign::parts),
            Edge.CODEC.optionalFieldOf("edge", Edge.STANDARD).forGetter(ModularDesign::edge),
            Codec.INT.optionalFieldOf("colour", DEFAULT_COLOUR).forGetter(ModularDesign::colour)
    ).apply(instance, ModularDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularDesign> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ModularDesign::type,
            Part.STREAM_CODEC.apply(ByteBufCodecs.list()), ModularDesign::parts,
            ByteBufCodecs.idMapper(i -> Edge.values()[i], Edge::ordinal), ModularDesign::edge,
            ByteBufCodecs.VAR_INT, ModularDesign::colour,
            ModularDesign::new);

    /** The part in {@code role}, or null. */
    public Part part(String role) {
        for (Part p : parts)
            if (p.role().equals(role))
                return p;
        return null;
    }

    public ModularDesign withEdge(Edge edge) {
        return new ModularDesign(type, parts, edge, colour);
    }

    /** This design with {@code colour} (an RGB, the high bits ignored). */
    public ModularDesign withColour(int colour) {
        return new ModularDesign(type, parts, edge, colour & 0xFFFFFF);
    }

    /** This design with {@code replacement} parts and everything else kept: its edge and its colour. */
    public ModularDesign withParts(List<Part> replacement) {
        return new ModularDesign(type, replacement, edge, colour);
    }

    public ModularDesign withPart(Part replacement) {
        return new ModularDesign(type, parts.stream()
                .map(p -> p.role().equals(replacement.role()) ? replacement : p).toList(), edge, colour);
    }
}
