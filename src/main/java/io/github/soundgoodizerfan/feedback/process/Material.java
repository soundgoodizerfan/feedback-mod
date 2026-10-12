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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Density;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
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
 * @param kind           what sort of stuff it is, qualitatively -- the adjective a player would
 *                       use before owning any instrument (§8).
 * @param traits         further qualitative observations, each one a yes/no a person could make
 *                       by handling it.
 * @param thermal        see {@link Thermal}.
 * @param mechanical     see {@link Mechanical}.
 * @param molten         see {@link Molten}.
 * @param separation     see {@link Separation}.
 * @param ampacity       current a conductor of it carries before it melts, as a fraction of
 *                       copper's. A ratio because resistance is not modelled (§17) and {@code Eu}
 *                       has no type yet; retype it when {@code Eu} gets one.
 * @param corrosionResistance 0 to 1: how well it survives water and acid. Corrosion may only ever
 *                       follow misuse, never time -- wear is caused by mistakes.
 *
 * <h2>Most of these figures have no reader yet</h2>
 * Everything from {@code kind} down was entered ahead of any system that reads it, so the table is
 * ready when one arrives (see {@code feedback_mechanics.md} §5.6). Absent means nobody has measured
 * it, never zero. The numbers are placeholders (§19): real-world values where the real world has
 * one, rounded, with Tu read as degrees Celsius -- tin, lead and zinc already melt at their real
 * figures -- except where an existing table already said otherwise, which wins.
 *
 * <h2>Generated items</h2>
 * Items are registered before a datapack exists, so {@link FormItems} reads the mod's own
 * material and form-type files from the jar at registration. A {@link FormType} whose kinds
 * include this material's, and that it neither lists nor excludes, is the generated item
 * {@code <namespace>:<material>_<form>}, resolved at compile by {@link #items}. A datapack
 * material can retune figures and name existing items, but cannot mint new ones.
 *
 * <h2>Any other form name is an association</h2>
 * {@code forms} may name anything made of the stuff -- iron's {@code horse_armor}, sculk's
 * {@code sensor} -- under a name no form type declares. Those generate nothing and are only there
 * so {@link #find} can say what an item is made of.
 */
public record Material(St hardness, Tu minTemperature, Tu maxTemperature,
                       Map<String, ResourceLocation> forms, List<String> exclude, Density density,
                       Optional<Kind> kind, List<Trait> traits, Thermal thermal, Mechanical mechanical,
                       Molten molten, Separation separation, Optional<Float> ampacity,
                       Optional<Float> corrosionResistance) {

    /** The broad class of stuff. One per material; the finer observations are {@link Trait}s. */
    public enum Kind implements StringRepresentable {
        METAL, MINERAL, CRYSTAL, CERAMIC, GLASS, GRANULAR, ORGANIC,
        /** §12's flagged exceptions: Minecraft-only stuff with its own defined properties. */
        EXOTIC;

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A yes/no a person could tell by handling it. Never a stand-in for identity (§11). */
    public enum Trait implements StringRepresentable {
        ALLOY, FLAMMABLE, FIREPROOF, TRANSPARENT, LUMINOUS, FIBROUS, POROUS, STICKY;

        static final Codec<Trait> CODEC = StringRepresentable.fromEnum(Trait::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * How it takes, holds and survives heat.
     *
     * @param meltingPoint         where it turns liquid. Should agree with any {@code thermal_process}
     *                             melt of it.
     * @param boilingPoint         where the liquid boils off (distillation).
     * @param coolingRate          how fast a loose workpiece of it cools, replacing
     *                             {@code FTuning.ITEM_COOLING_TU_PER_TICK} per material once read
     *                             (mechanics §2.2's open item).
     * @param conductivity         conductance of a one-block wall of it, on the scale where copper's
     *                             is {@code FTuning.HEAT_EXCHANGER_CONDUCTANCE}.
     * @param oxidationTemperature above this, held in air, it scales (metal) or burns (organic).
     * @param annealTemperature    heating past this undoes work hardening.
     * @param serviceTemperature   the hottest a machine body of it may run before it softens and wears.
     */
    public record Thermal(Optional<Tu> meltingPoint, Optional<Tu> boilingPoint, Optional<TuRate> coolingRate,
                          Optional<Conductance> conductivity, Optional<Tu> oxidationTemperature,
                          Optional<Tu> annealTemperature, Optional<Tu> serviceTemperature) {
        static final Thermal NONE = new Thermal(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        static final Codec<Thermal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Tu::new).optionalFieldOf("melting_point").forGetter(Thermal::meltingPoint),
                Units.codec(Tu::new).optionalFieldOf("boiling_point").forGetter(Thermal::boilingPoint),
                Units.codec(TuRate::new).optionalFieldOf("cooling_rate").forGetter(Thermal::coolingRate),
                Units.codec(Conductance::new).optionalFieldOf("conductivity").forGetter(Thermal::conductivity),
                Units.codec(Tu::new).optionalFieldOf("oxidation_temperature").forGetter(Thermal::oxidationTemperature),
                Units.codec(Tu::new).optionalFieldOf("anneal_temperature").forGetter(Thermal::annealTemperature),
                Units.codec(Tu::new).optionalFieldOf("service_temperature").forGetter(Thermal::serviceTemperature)
        ).apply(instance, Thermal::new));
    }

    /**
     * How it answers force. All strengths are on the hammer's {@code St} scale, so a figure reads
     * directly against a crank's two numbers.
     *
     * @param toughness     one blow above this cracks it. Below the hardness floor means any blow
     *                      that could move it breaks it instead -- brittle.
     * @param ductility     cold deformation steps it survives before cracking.
     * @param workHardening hardness gained per cold step, until annealed.
     * @param yieldStrength load a part of it takes before it bends (frames, shafts, engines).
     * @param friction      sliding coefficient, for brakes, clutches, bearings and tires.
     * @param abrasiveness  0 to 1: how fast cutting it spends a grinding wheel's grit.
     * @param hardenable    whether a quench hardens it rather than merely cooling it.
     */
    public record Mechanical(Optional<St> toughness, Optional<Integer> ductility, Optional<St> workHardening,
                             Optional<St> yieldStrength, Optional<Float> friction, Optional<Float> abrasiveness,
                             boolean hardenable) {
        static final Mechanical NONE = new Mechanical(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), false);

        static final Codec<Mechanical> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(St::new).optionalFieldOf("toughness").forGetter(Mechanical::toughness),
                Codec.INT.optionalFieldOf("ductility").forGetter(Mechanical::ductility),
                Units.codec(St::new).optionalFieldOf("work_hardening").forGetter(Mechanical::workHardening),
                Units.codec(St::new).optionalFieldOf("yield_strength").forGetter(Mechanical::yieldStrength),
                Codec.FLOAT.optionalFieldOf("friction").forGetter(Mechanical::friction),
                Codec.FLOAT.optionalFieldOf("abrasiveness").forGetter(Mechanical::abrasiveness),
                Codec.BOOL.optionalFieldOf("hardenable", false).forGetter(Mechanical::hardenable)
        ).apply(instance, Mechanical::new));
    }

    /**
     * The liquid, for whatever melts.
     *
     * @param viscosity relative to water (1). §3 names viscosity as not yet earning its place; it is
     *                  recorded for a misrun in a mold, and stays unread until that is a decision.
     * @param density   liquid density -- slag floats off a heavier melt.
     */
    public record Molten(Optional<Float> viscosity, Optional<Density> density) {
        static final Molten NONE = new Molten(Optional.empty(), Optional.empty());

        static final Codec<Molten> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("viscosity").forGetter(Molten::viscosity),
                Units.codec(Density::new).optionalFieldOf("density").forGetter(Molten::density)
        ).apply(instance, Molten::new));
    }

    /**
     * What §11's separators can tell apart. Density is the material's own top-level figure.
     *
     * @param magnetic         an electromagnetic drum pulls it.
     * @param curieTemperature above this it stops being magnetic -- hot iron passes the drum.
     * @param reflectivity     0 to 1, what an optical sorter sees. Two materials that look alike
     *                         should read alike, so the sorter is genuinely fooled.
     */
    public record Separation(boolean magnetic, Optional<Tu> curieTemperature, Optional<Float> reflectivity) {
        static final Separation NONE = new Separation(false, Optional.empty(), Optional.empty());

        static final Codec<Separation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("magnetic", false).forGetter(Separation::magnetic),
                Units.codec(Tu::new).optionalFieldOf("curie_temperature").forGetter(Separation::curieTemperature),
                Codec.FLOAT.optionalFieldOf("reflectivity").forGetter(Separation::reflectivity)
        ).apply(instance, Separation::new));
    }

    /** Hardness divides, so zero or less is refused at load rather than turning into Infinity Fu. */
    static final Codec<St> HARDNESS_CODEC = Units.codec(St::new).validate(st -> st.value() > 0
            ? DataResult.success(st) : DataResult.error(() -> "hardness must be positive: " + st.value()));

    public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HARDNESS_CODEC.fieldOf("hardness").forGetter(Material::hardness),
            Units.codec(Tu::new).optionalFieldOf("min_temperature", new Tu(0f)).forGetter(Material::minTemperature),
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(Material::maxTemperature),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("forms", Map.of()).forGetter(Material::forms),
            Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(Material::exclude),
            Units.codec(Density::new).optionalFieldOf("density", new Density(0f)).forGetter(Material::density),
            Kind.CODEC.optionalFieldOf("kind").forGetter(Material::kind),
            Trait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(Material::traits),
            Thermal.CODEC.optionalFieldOf("thermal", Thermal.NONE).forGetter(Material::thermal),
            Mechanical.CODEC.optionalFieldOf("mechanical", Mechanical.NONE).forGetter(Material::mechanical),
            Molten.CODEC.optionalFieldOf("molten", Molten.NONE).forGetter(Material::molten),
            Separation.CODEC.optionalFieldOf("separation", Separation.NONE).forGetter(Material::separation),
            Codec.FLOAT.optionalFieldOf("ampacity").forGetter(Material::ampacity),
            Codec.FLOAT.optionalFieldOf("corrosion_resistance").forGetter(Material::corrosionResistance)
    ).apply(instance, Material::new));

    /**
     * The form types that apply to this material's kind and that it has no explicit item for and
     * does not exclude.
     */
    public Set<String> generated(Map<String, FormType> types) {
        Set<String> out = new TreeSet<>();
        types.forEach((form, type) -> {
            if (type.appliesTo(this) && !forms.containsKey(form) && !exclude.contains(form))
                out.add(form);
        });
        return out;
    }

    /** Where an item sits in the material table: which material, in which form. */
    public record Located(ResourceLocation id, Material material, String form) {}

    /** The material and form {@code item} is, if any material lists or generated it. */
    public static Optional<Located> find(Map<ResourceLocation, Material> materials, Item item) {
        for (Map.Entry<ResourceLocation, Material> e : materials.entrySet())
            for (Map.Entry<String, Item> form : e.getValue().items(e.getKey()).entrySet())
                if (form.getValue() == item)
                    return Optional.of(new Located(e.getKey(), e.getValue(), form.getKey()));
        return Optional.empty();
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
    public Map<String, Item> items(ResourceLocation id) {
        Map<String, Item> out = new LinkedHashMap<>();
        forms.forEach((form, item) -> BuiltInRegistries.ITEM.getOptional(item).ifPresent(i -> out.put(form, i)));
        for (String form : generated(FormItems.types()))
            BuiltInRegistries.ITEM.getOptional(FormItems.itemId(id, form)).ifPresent(i -> out.put(form, i));
        return out;
    }
}
