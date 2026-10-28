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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Density;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Ru;
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
 * @param electrical     see {@link Electrical}.
 * @param corrosionResistance 0 to 1: how well it survives water and acid. Corrosion may only ever
 *                       follow misuse, never time -- wear is caused by mistakes.
 * @param composition    an alloy's constituent materials and their shares, for what dissolving it
 *                       puts into a solution (see {@link #constituents}). Empty for a pure
 *                       material. The alloy table's windows say what a melt must be to <em>be</em>
 *                       brass; this says what a given piece of brass is.
 * @param bears          the metals this material is an ore of, each with how many ingots one piece yields
 *                       (1 is a full ingot, 0.5 a poor ore). A bearer is its own material, with its own
 *                       hardness, magnetism and colour, but by default it smelts into its metal at that
 *                       metal's melting point (see {@code ThermalProcessTable}) and gets ore, deepslate ore
 *                       and lump forms generated. Only the largest share smelts so far.
 * @param radioactivity  the dose rate one ingot's volume of it gives at point blank, in Ru (absent: none).
 *                       Read by {@code Radiation} for every item, block and modular part made of it.
 * @param combustion     see {@link Combustion}: what burning it does, in a fire and in a chamber.
 * @param coil           see {@link Coil}: what it does as a coil gun's charge. Not combustion.
 * @param perks          what a part of it does to a blow, by perk and level -- see {@link Perk}. Empty
 *                       for most. Qualitative only: a figure (lead's density) is a stat, never a perk.
 * @param color          RGB it is drawn in, written {@code "#rrggbb"}. Absent means nobody chose one:
 *                       the client then averages the texture of its ingot or block item, and
 *                       failing that takes its kind's grey. Only the client ever draws it.
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
                       Optional<Float> corrosionResistance, Optional<Combustion> combustion,
                       Electrical electrical, Map<ResourceLocation, Float> composition,
                       Optional<Integer> enchantability, Optional<Coil> coil, Optional<Integer> color,
                       Map<Perk, Integer> perks, Optional<Attuning> attunement, Optional<Food> food,
                       Map<ResourceLocation, Float> bears, Optional<Ru> radioactivity, Optional<Catalyst> catalyst) {

    /**
     * What a bed of it does as a catalyst (petrochemistry spec 7.4): a material property, the way fuel is.
     *
     * @param activity               how much faster it makes a reaction than a row's baseline: 1 is a plain bed.
     * @param sulfurTolerance        how much sulfur-weighted feed it takes before it loses the same activity: above 1 poisons slower.
     * @param regenerationTemperature the Tu a bed must hold to be regenerated.
     * @param sinterTemperature      held over this it sinters, permanently.
     */
    public record Catalyst(float activity, float sulfurTolerance, Tu regenerationTemperature, Tu sinterTemperature) {
        static final Codec<Catalyst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("activity", 1f).forGetter(Catalyst::activity),
                Codec.FLOAT.optionalFieldOf("sulfur_tolerance", 1f).forGetter(Catalyst::sulfurTolerance),
                Units.codec(Tu::new).fieldOf("regeneration_temperature").forGetter(Catalyst::regenerationTemperature),
                Units.codec(Tu::new).fieldOf("sinter_temperature").forGetter(Catalyst::sinterTemperature)
        ).apply(instance, Catalyst::new));
    }

    /** The broad class of stuff. One per material; the finer observations are {@link Trait}s. */
    public enum Kind implements StringRepresentable {
        METAL, MINERAL, CRYSTAL, CERAMIC, GLASS, GRANULAR, ORGANIC,
        /** §12's flagged exceptions: Minecraft-only stuff with its own defined properties. */
        EXOTIC,
        /** Sculk alone: a living-looking mass that rolls into plates (gun sculk jacket) without every exotic gaining them. */
        SCULK,
        /** Petroleum and its cuts (petrochemistry spec 4.1): a fluid whose properties ride on its material, keyed by the fluid's id. */
        FLUID,
        /** Plastics and rubbers (spec 11): shaped by the form, mould and casting systems like a metal. */
        POLYMER;

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A yes/no a person could tell by handling it. Never a stand-in for identity (§11). */
    public enum Trait implements StringRepresentable {
        ALLOY, FLAMMABLE, FIREPROOF, TRANSPARENT, LUMINOUS, FIBROUS, POROUS, STICKY,
        /** A joining alloy: its melting point is a joint's liquidus (basic electronics phase 3). */
        SOLDER,
        /** A dust that sets as a conductive trace when printed on a substrate (phase 3). */
        PASTE,
        /** What a dish's roles are told apart by (dish spec 2.1): a grain or tuber, meat or fish, a plant, a strong small addition. */
        STARCHY, PROTEIN, VEGETAL, SEASONING,
        /** A root or tuber (modular foods spec 2): what names a cut potato fries and a mashed one mash. */
        TUBER,
        /** Sharp and wet (modular foods spec 2): what lets a minced vegetable in a bowl be a salsa. */
        ACIDIC,
        /** Strands of dough (noodles, pasta): what a noodle dish's base is told by. */
        NOODLE,
        /** What a part votes for when a dish picks its naming pattern (foods spec 7); a tie names no cuisine. */
        ITALIAN, EAST_ASIAN, MEXICAN, EASTERN_EUROPEAN,
        /** A root vegetable (beet, turnip): what a root stew is built on. */
        ROOT,
        /** A mushroom: what makes a forager's stew a hunter's. */
        FUNGAL,
        /** Cures food by drawing its water out (food spec 8): what the salting recipe takes, by this and not by what it is called. */
        SALINE,
        /** Dough that gains gluten under the hand (food spec 6): what the Prep Table's Knead takes. */
        KNEADABLE,
        /** A baked sheet a dish is wrapped in (dish spec 2.1): what a wrap's container role takes. */
        WRAPPER,
        /** Dough that bakes to a pie's shell and lid (modular foods spec, pies): what a pie's container and top roles take. */
        PASTRY,
        /** A pot serving (broth, stew) a soup is poured from: what a soup's base role takes. */
        BROTH,
        /** Sculk: what a gun's sculk jacket role takes. */
        SCULK;

        public static final Codec<Trait> CODEC = StringRepresentable.fromEnum(Trait::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A qualitative thing a part of this material does to a blow (spec 2.4), read by
     * {@code MaterialPerks} from whatever parts a modular weapon is made of. The level is the
     * vanilla enchantment's, applied free; a perk is never a number the material was missing.
     */
    public enum Perk implements StringRepresentable {
        /** Extra damage to the undead: silver's old repute. */
        SMITE,
        /** Sets the target alight; the part is a flame, so a bare hand pays ({@code core/hazard}). */
        FIRE_ASPECT,
        /** Extra damage to any target, not only the wet ones. */
        IMPALING,
        /** A struck target rings back and is outlined for a few seconds. */
        ECHO,
        /** The part counts as lighter in the hand: swing inertia and balance see less of its mass than it has. */
        FEATHERLIGHT,
        /** A struck target is chilled: freezing builds on it as powder snow's does. */
        CHILL,
        /** A struck target is poisoned for a few seconds. */
        TOXIC,
        /** A struck target is thrown upward, as off slime. */
        BOUNCE,
        /** A struck target is slowed, as in honey. */
        CLING,
        /** A struck target is irradiated: it withers for a few seconds and is outlined as it glows. */
        RADIANT,
        /** A kill yields a little extra experience. */
        SIPHON,
        /** A kill carries the wielder into the victim's place. */
        BLINK,
        /** What a part of it holds stays warm: a dish on it is held above the warm point wherever it is. */
        WARMING;

        public static final Codec<Perk> CODEC = StringRepresentable.fromEnum(Perk::values);

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
     * @param freezeTicks how long a bucket of the melt, poured into a fire, holds its melting point
     *                  while it freezes: a heat source that is a body at a fixed temperature, not
     *                  a burn. Lava is stone at its {@code melting_point} (mechanics 5.7).
     */
    public record Molten(Optional<Float> viscosity, Optional<Density> density, Optional<Integer> freezeTicks) {
        static final Molten NONE = new Molten(Optional.empty(), Optional.empty(), Optional.empty());

        static final Codec<Molten> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("viscosity").forGetter(Molten::viscosity),
                Units.codec(Density::new).optionalFieldOf("density").forGetter(Molten::density),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("freeze_ticks").forGetter(Molten::freezeTicks)
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

    /**
     * What a material does as a gun's charge: not stored, read off {@link Combustion}'s chamber or
     * off {@link Coil} by {@link #propellant()}, so every gun system reads one view and none of
     * them knows which block it came from.
     *
     * @param ignitionTemperature a round left chambered in a barrel this hot goes off by itself
     *                     (cook-off, {@code docs/feedback_guns_spec.md} 3.4): the material's oxidation
     *                     temperature. For a coil, the winding's quench temperature.
     * @param drive        a gas pressing a bullet, or a field pulling a slug.
     */
    public record Propellant(Pu peakPressure, float burnRate, Tu ignitionTemperature, float oxygenBalance, Drive drive) {

        /** What the propellant does work through: a gas, or a field. */
        public enum Drive { CHEMICAL, COIL }
    }

    /** This material as a gun's charge, if it is one: its combustion's chamber, or its coil. */
    public Optional<Propellant> propellant() {
        if (coil.isPresent())
            return Optional.of(new Propellant(coil.get().fieldPressure(), 1f, coil.get().quenchTemperature(), 0f,
                    Propellant.Drive.COIL));
        return combustion.flatMap(c -> c.chamber().map(ch -> new Propellant(ch.peakPressure(), ch.burnRate(),
                thermal.oxidationTemperature().orElse(new Tu(FTuning.GUN_PROPELLANT_IGNITION_TU)),
                c.oxygenBalance(), Propellant.Drive.CHEMICAL)));
    }

    /**
     * What burning it does: one set of figures for every fire it is in, open or confined
     * ({@code feedback_mechanics.md} 5.7). A firebox reads the open half through {@link Fuel}, a
     * gun's chamber the confined half through {@link #propellant}. Its ignition point is not here:
     * the material already states it as {@code thermal.oxidation_temperature}, above which, held
     * in air, it burns. Absent means it does not burn.
     *
     * @param flameTemperature how hot an open fire of it burns. A hard ceiling on anything it
     *                         heats -- a vessel approaches its fire and never passes it, so a fuel
     *                         too cool for a process is a real impossibility (philosophy 7). With
     *                         {@code burnTicks}, it makes the stuff a fuel.
     * @param burnTicks        how long one piece of it burns; other forms by how much stuff they
     *                         are ({@link Fuel#pieces}). Independent of the temperature on purpose:
     *                         charcoal hot and brief, wood cool and brief, coal hot and long.
     * @param spread           how much the flame varies, as a fraction, rolled once per piece and
     *                         held for its burn: philosophy 8's noise floor, entering as something
     *                         true (charcoal is not all the same).
     * @param oxygenBalance    oxygen it brings beyond what its own fuel needs, per item: positive
     *                         an oxidizer (gunpowder's nitrate), negative a fuel that needs one
     *                         (blaze powder's carbon and sulfur). A blend in a case burns
     *                         completely only when the sum is not negative.
     * @param chamber          what it does confined in a case; present makes it a propellant.
     * @param flashPoint       a liquid's flash point (petrochemistry spec 4.2): above this temperature it
     *                         gives off enough vapour to ignite. Absent means it gives off none.
     */
    public record Combustion(Optional<Tu> flameTemperature, Optional<Integer> burnTicks, float spread,
                             float oxygenBalance, Optional<Chamber> chamber, Optional<Tu> flashPoint) {
        static final Codec<Combustion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Tu::new).optionalFieldOf("flame_temperature").forGetter(Combustion::flameTemperature),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("burn_ticks").forGetter(Combustion::burnTicks),
                Codec.FLOAT.optionalFieldOf("spread", 0.06f).forGetter(Combustion::spread),
                Codec.FLOAT.optionalFieldOf("oxygen_balance", 0f).forGetter(Combustion::oxygenBalance),
                Chamber.CODEC.optionalFieldOf("chamber").forGetter(Combustion::chamber),
                Units.codec(Tu::new).optionalFieldOf("flash_point").forGetter(Combustion::flashPoint)
        ).apply(instance, Combustion::new));
    }

    /**
     * A burn confined in a case ({@code docs/feedback_guns_spec.md} 3.6): two independent numbers, as
     * Create Big Cannons has them.
     *
     * @param peakPressure the gas pressure a nominal charge makes in a case, before the round's
     *                     own loading and before any barrel is asked about.
     * @param burnRate     how much of a barrel's length it takes to burn, per metre: the fraction
     *                     burned by the muzzle is {@code burnRate x barrel length}, capped at 1.
     */
    public record Chamber(Pu peakPressure, float burnRate) {
        static final Codec<Chamber> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Pu::new).fieldOf("peak_pressure").forGetter(Chamber::peakPressure),
                Codec.floatRange(0.01f, 1000f).fieldOf("burn_rate").forGetter(Chamber::burnRate)
        ).apply(instance, Chamber::new));
    }

    /**
     * A coil gun's charge: a field pulling a slug, not a gas pushing one. Not combustion, so it
     * lives apart from it.
     *
     * @param fieldPressure     the magnetic pressure a nominal charge makes (it is a pressure).
     * @param quenchTemperature past this the superconducting winding reverts and carries nothing
     *                          until it has cooled -- the coil's cook-off figure.
     */
    public record Coil(Pu fieldPressure, Tu quenchTemperature) {
        static final Codec<Coil> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Pu::new).fieldOf("field_pressure").forGetter(Coil::fieldPressure),
                Units.codec(Tu::new).fieldOf("quench_temperature").forGetter(Coil::quenchTemperature)
        ).apply(instance, Coil::new));
    }

    /**
     * How it carries and makes electricity (mechanics 6.4). A conductor's current rating is the
     * top-level {@code ampacity}; this holds the other figures. Resistance is not modelled, so
     * {@code drop} is stated directly as the thing it would have been derived to be.
     *
     * @param drop               volts lost per block of a wire of it, per amp carried (6.4). A
     *                           path's drop is the sum over its blocks of {@code drop x current}.
     * @param electrodePotential volts of a cell electrode of it against a common reference. A
     *                           galvanic cell's voltage is the difference between its two
     *                           electrodes' figures, so no cell names a voltage.
     * @param conductivity       how well a body of it passes current, copper 1. Read by {@code Exposure}
     *                           (what a worn plate or a floor does to a shock); absent, the kind decides
     *                           there: a metal conducts, an organic insulates.
     * @param insulation         the highest voltage band a coat of it holds off when put on a wire
     *                           (6.4): above it the coat burns off. Absent, it is no insulator.
     * @param piezoCoupling      how strongly a plate of it rings when squeezed and squeezes when rung, 0-1
     *                           (6.17). A mass probe's crystal must have some; absent, it does not ring.
     * @param rectifying         how good a spot a cat's whisker can find on it, 0-1 (6.18). Absent, it has none.
     */
    public record Electrical(Optional<Float> drop, Optional<Float> electrodePotential, Optional<Float> conductivity,
                             Optional<VoltageBand> insulation,
                             Optional<Float> piezoCoupling, Optional<Float> rectifying) {
        static final Electrical NONE = new Electrical(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        static final Codec<Electrical> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("drop").forGetter(Electrical::drop),
                Codec.FLOAT.optionalFieldOf("electrode_potential").forGetter(Electrical::electrodePotential),
                Codec.FLOAT.optionalFieldOf("conductivity").forGetter(Electrical::conductivity),
                VoltageBand.CODEC.optionalFieldOf("insulation").forGetter(Electrical::insulation),
                Codec.FLOAT.optionalFieldOf("piezo_coupling").forGetter(Electrical::piezoCoupling),
                Codec.FLOAT.optionalFieldOf("rectifying").forGetter(Electrical::rectifying)
        ).apply(instance, Electrical::new));
    }

    /** {@code "#rrggbb"} (the hash optional) as a 24-bit int. */
    static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(str -> {
        try {
            String h = str.startsWith("#") ? str.substring(1) : str;
            if (h.length() != 6)
                return DataResult.error(() -> "color must be #rrggbb: " + str);
            return DataResult.success(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "color must be #rrggbb: " + str);
        }
    }, rgb -> String.format("#%06x", rgb & 0xFFFFFF));

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
            Codec.FLOAT.optionalFieldOf("corrosion_resistance").forGetter(Material::corrosionResistance),
            Combustion.CODEC.optionalFieldOf("combustion").forGetter(Material::combustion),
            // Sixteen is RecordCodecBuilder's ceiling, so the last keys ride as one group. The
            // JSON is unchanged: they stay plain top-level keys.
            Tail.CODEC.forGetter(m -> new Tail(m.electrical(), m.composition(), m.enchantability(), m.coil(), m.color(), m.perks(), m.attunement(), m.food(), m.bears(), m.radioactivity(), m.catalyst()))
    ).apply(instance, (hardness, min, max, forms, exclude, density, kind, traits, thermal, mechanical,
                       molten, separation, ampacity, corrosion, combustion, tail) -> new Material(
            hardness, min, max, forms, exclude, density, kind, traits, thermal, mechanical, molten,
            separation, ampacity, corrosion, combustion, tail.electrical(), tail.composition(),
            tail.enchantability(), tail.coil(), tail.color(), tail.perks(), tail.attunement(), tail.food(), tail.bears(), tail.radioactivity(), tail.catalyst())));

    /**
     * The keys that did not fit the codec's sixteen.
     *
     * @param enchantability how well it takes an enchantment, on vanilla's scale (gold 22, stone 5):
     *                       the one figure a modular item's enchanting reads, from its main part
     *                       ({@code docs/feedback_modular_items_spec.md} 2.1). Absent reads as poor.
     */
    private record Tail(Electrical electrical, Map<ResourceLocation, Float> composition, Optional<Integer> enchantability,
                        Optional<Coil> coil, Optional<Integer> color,
                        Map<Perk, Integer> perks, Optional<Attuning> attunement, Optional<Food> food,
                        Map<ResourceLocation, Float> bears, Optional<Ru> radioactivity, Optional<Catalyst> catalyst) {
        static final com.mojang.serialization.MapCodec<Tail> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Electrical.CODEC.optionalFieldOf("electrical", Electrical.NONE).forGetter(Tail::electrical),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).optionalFieldOf("composition", Map.of()).forGetter(Tail::composition),
                Codec.INT.optionalFieldOf("enchantability").forGetter(Tail::enchantability),
                Coil.CODEC.optionalFieldOf("coil").forGetter(Tail::coil),
                HEX.optionalFieldOf("color").forGetter(Tail::color),
                Codec.unboundedMap(Perk.CODEC, Codec.intRange(1, 5)).optionalFieldOf("perks", Map.of()).forGetter(Tail::perks),
                Attuning.CODEC.optionalFieldOf("attuning").forGetter(Tail::attunement),
                Food.CODEC.optionalFieldOf("food").forGetter(Tail::food),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).optionalFieldOf("bears", Map.of()).forGetter(Tail::bears),
                Units.codec(Ru::new).optionalFieldOf("radioactivity").forGetter(Tail::radioactivity),
                Catalyst.CODEC.optionalFieldOf("catalyst").forGetter(Tail::catalyst)
        ).apply(instance, Tail::new));
    }

    /**
     * What it is as food ({@code docs/feedback_food_spec.md} 3). Hunger and saturation are not
     * here: they are the vanilla figures of the {@code cooked} form's item, read off it, so a
     * modded food joins by naming its forms.
     *
     * @param moisture   fraction of the item that is water. Cooking drives it off; a food that has
     *                   lost much of it is dried.
     * @param pathogen   0 to 1: live-organism load a raw item carries. Heat kills a fraction of it each
     *                   tick, scaled by cooking speed ({@code Cooking.tick}).
     * @param toxin      0 to 1: stable-toxin load. Heat does not touch it.
     * @param fat        0 to 1: slow absorption. With {@code fibre}, delays and stretches what its compounds do.
     * @param fibre      0 to 1: as {@code fat}.
     * @param compounds  named actives and a strength each (0 to 1); what each does is a {@link Compound}.
     * @param perks      traits specific to this food, a potency each (0 to 1); what each does is a {@link FoodPerk}.
     * @param charTemperature where it burns; absent, the material's {@code oxidation_temperature},
     *                   else {@link FTuning#FOOD_CHAR_TU}.
     */
    public record Food(float moisture, float pathogen, float toxin, Optional<Tu> charTemperature,
                       float fat, float fibre, Map<String, Float> compounds, Map<String, Float> perks) {
        static final Codec<Food> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(0f, 1f).optionalFieldOf("moisture", 0.7f).forGetter(Food::moisture),
                Codec.floatRange(0f, 1f).optionalFieldOf("pathogen", 0f).forGetter(Food::pathogen),
                Codec.floatRange(0f, 1f).optionalFieldOf("toxin", 0f).forGetter(Food::toxin),
                Units.codec(Tu::new).optionalFieldOf("char_tu").forGetter(Food::charTemperature),
                Codec.floatRange(0f, 1f).optionalFieldOf("fat", 0f).forGetter(Food::fat),
                Codec.floatRange(0f, 1f).optionalFieldOf("fibre", 0f).forGetter(Food::fibre),
                Codec.unboundedMap(Codec.STRING, Codec.floatRange(0f, 1f)).optionalFieldOf("compounds", Map.of()).forGetter(Food::compounds),
                Codec.unboundedMap(Codec.STRING, Codec.floatRange(0f, 1f)).optionalFieldOf("perks", Map.of()).forGetter(Food::perks)
        ).apply(instance, Food::new));

        /** Compounds and perks as one map, perks under {@code perk:<id>}: the blends and the dose carry both the same way. */
        public Map<String, Float> actives() {
            if (perks.isEmpty())
                return compounds;
            Map<String, Float> out = new java.util.HashMap<>(compounds);
            perks.forEach((id, potency) -> out.put(FoodPerk.PREFIX + id, potency));
            return out;
        }
    }

    /**
     * Where and how it takes an attunement (enchanting, {@code docs/feedback_enchanting_spec.md} 3-4): the field acts only
     * while the part is under {@code temperature} and over {@code lock}; {@code coercive} is the field below which
     * nothing moves, {@code loopWidth} how wide the swing past it is, {@code lag} the ticks polarization takes to follow.
     * {@code cold}: the material is worked without heat (anything but a metal), so it is attuned without it -- it
     * needs no heat to start and does not cool during the game, sitting at {@code temperature} throughout.
     */
    public record Attuning(Tu temperature, Tu lock, float coercive, float loopWidth, float lag, boolean cold) {
        static final Codec<Attuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Tu::new).fieldOf("temperature").forGetter(Attuning::temperature),
                Units.codec(Tu::new).fieldOf("lock").forGetter(Attuning::lock),
                Codec.floatRange(0.05f, 0.9f).fieldOf("coercive").forGetter(Attuning::coercive),
                Codec.floatRange(0.05f, 1f).fieldOf("loop_width").forGetter(Attuning::loopWidth),
                Codec.floatRange(1f, 40f).fieldOf("lag").forGetter(Attuning::lag),
                Codec.BOOL.optionalFieldOf("cold", false).forGetter(Attuning::cold)
        ).apply(instance, Attuning::new));
    }

    /**
     * Its attuning figures: the file's, else every material is enchantable but none is good at it. shortcut: the
     * fallback borrows the cold end of the working window (a cold-worked material a flat point) and one loop
     * for all; give a material a {@code attuning} block when it should play differently.
     */
    public Attuning attuning() {
        return attunement.orElseGet(() -> {
            float top = minTemperature.value() > 0 ? minTemperature.value()
                    : io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_COLD_POINT_TU;
            return new Attuning(new Tu(top), new Tu(top - io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_WINDOW_TU),
                    io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_COERCIVE,
                    io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_LOOP_WIDTH,
                    io.github.soundgoodizerfan.feedback.modular.ModularTuning.ENCHANT_LAG_TICKS,
                    kind.orElse(null) != Kind.METAL);
        });
    }

    /**
     * What dissolving this material puts into a solution: its constituent materials and their
     * shares. An alloy says so ({@code composition} in its file, e.g. brass is copper and zinc); a
     * pure material has none and dissolves as itself. Shares are normalised here, so a file may
     * write them as fractions or as parts.
     */
    public Map<ResourceLocation, Float> constituents(ResourceLocation id) {
        if (composition.isEmpty())
            return Map.of(id, 1f);
        float total = 0f;
        for (float share : composition.values())
            total += share;
        Map<ResourceLocation, Float> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Float> e : composition.entrySet())
            out.put(e.getKey(), e.getValue() / total);
        return out;
    }

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
                // A shaped part is one item for every material: it is told by its MadeOf, not its item.
                if (form.getValue() == item && !(item instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem))
                    return Optional.of(new Located(e.getKey(), e.getValue(), form.getKey()));
        return Optional.empty();
    }

    /**
     * What {@code stack} is: a shaped part by its {@link MadeOf} (in the form named by the part's
     * item, {@code pickaxe_head}), anything else by its item.
     */
    public static Optional<Located> find(Map<ResourceLocation, Material> materials, net.minecraft.world.item.ItemStack stack) {
        MadeOf made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        // Raw stock the enchanting table stamped (a rod, a grip) is still found by its item: the stamp only carries the attunement.
        if (made == null || !(stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem))
            return find(materials, stack.getItem());
        Material m = materials.get(made.material());
        return m == null ? Optional.empty()
                : Optional.of(new Located(made.material(), m, BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()));
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
     * One piece of {@code form} as a stack: the form's item, and for a shaped part the
     * {@link MadeOf} that makes it this material's. What a form step produces.
     */
    public java.util.Optional<net.minecraft.world.item.ItemStack> stack(ResourceLocation id, String form) {
        return item(id, form).map(i -> {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(i);
            if (i instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem)
                stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), new MadeOf(id, PART_STOCK, 1));
            return stack;
        });
    }

    /** What a form step takes: the item, and for a shaped part also its material, so copper's cylinder is not steel's. */
    public java.util.Optional<net.minecraft.world.item.crafting.Ingredient> ingredient(ResourceLocation id, String form) {
        return item(id, form).map(i -> i instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem
                ? net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false,
                io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), new MadeOf(id, PART_STOCK, 1), i)
                : net.minecraft.world.item.crafting.Ingredient.of(i));
    }

    /** The stock a machined part is recorded as made of: what mends it, and what a form step makes. */
    private static final String PART_STOCK = "ingot";

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
