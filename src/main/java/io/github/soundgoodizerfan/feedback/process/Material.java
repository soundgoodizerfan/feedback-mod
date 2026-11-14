// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
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
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.LatentHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.SpecificHeat;
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
 * <h2>Every figure for its phase, in every file</h2>
 * The world resolves on truth, not measurement, so a file that lacks a figure its phase has is unfinished
 * (author 2026-11-13); {@code scripts/check_material_complete.py} lists such files. The codec stays tolerant
 * (every field but hardness is optional) so a datapack material loads while it is being written, and a shared
 * kind template ({@code "template": true}, named as {@code parent}) supplies the figures a family has in common.
 * A behaviour block (combustion, neutron, catalyst, potion) is omitted only when the thing truly has none of it.
 * This is the shape GregTech's {@code Material} uses, minus its flag layer, which a form's existence in
 * {@link #forms} makes redundant.
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
 * @param neutron        how it meets neutrons in a reactor core (absent: inert).
 * @param radioactivity  the dose rate one ingot's volume of it gives at point blank, in Ru (absent: none).
 *                       Read by {@code Radiation} for every item, block and modular part made of it.
 * @param combustion     see {@link Combustion}: what burning it does, in a fire and in a chamber.
 * @param perks          what a part of it does to a blow, by perk and level -- see {@link Perk}. Empty
 *                       for most. Qualitative only: a figure (lead's density) is a stat, never a perk.
 * @param color          RGB it is drawn in, written {@code "#rrggbb"}. Absent means nobody chose one:
 *                       the client then averages the texture of its ingot or block item, and
 *                       failing that takes its kind's grey. Only the client ever draws it.
 *
 * <h2>Units come from their definitions</h2>
 * {@code Tu} is degrees Celsius, density is kg/L, heat figures are kJ-based ({@code FTuning.JOULES_PER_WORK}),
 * thermal conductivity is real W/(m K) over 4010 (copper 0.1), hardness is 0.1 GPa Vickers per {@code St}, yield
 * 100 MPa per {@code St}, toughness 1 MPa sqrt(m) per {@code St}. {@code docs/material_figures.md} gives every figure's
 * real definition and what reads it.
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
                       Optional<Integer> enchantability, Optional<Integer> color,
                       Map<Perk, Integer> perks, Optional<Attuning> attunement, Optional<Food> food,
                       Map<ResourceLocation, Float> bears, Optional<Ru> radioactivity, Optional<Catalyst> catalyst,
                       Optional<Potion> potion, Optional<XpCapacity> xpStore,
                       Optional<Integer> luminance, boolean phosphor, Optional<Neutron> neutron, Optional<Integer> fastener,
                       Optional<Float> toxicity) {

    /**
     * How a material meets neutrons in a reactor core (fission spec 2.5): {@code fissile} is what a rod of it gives per neutron
     * (1 for natural uranium fuel), {@code fertile} what it breeds toward fissile while it sits in a running core, {@code moderation}
     * how much it slows neutrons so neighbouring fuel runs hotter, {@code absorption} how much it soaks them up. Read by the core
     * from the item's material, never from an id; a pack adds a fuel, moderator or absorber by writing this block.
     */
    public record Neutron(float fissile, float fertile, float moderation, float absorption, boolean fast) {
        public static final Codec<Neutron> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("fissile", 0f).forGetter(Neutron::fissile),
                Codec.FLOAT.optionalFieldOf("fertile", 0f).forGetter(Neutron::fertile),
                Codec.FLOAT.optionalFieldOf("moderation", 0f).forGetter(Neutron::moderation),
                Codec.FLOAT.optionalFieldOf("absorption", 0f).forGetter(Neutron::absorption),
                Codec.BOOL.optionalFieldOf("fast", false).forGetter(Neutron::fast)
        ).apply(i, Neutron::new));
    }

    /**
     * How a material holds and carries experience (progression roadmap L2, 6.8). {@code topLevel} is the level a
     * container of it reaches when full, whatever its size; {@code capacitance} is its points per unit of container
     * size, so the ceiling is size times capacitance. {@code conductance} is how many points per tick a wire of it moves
     * per level of difference (0 is no wire at all) and {@code leak} the share of what moves that escapes as orbs.
     * The figures in the material files are invented and unplayed.
     */
    public record XpCapacity(float topLevel, float capacitance, float conductance, float leak) {
        static final Codec<XpCapacity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(0.001f, Float.MAX_VALUE).fieldOf("top_level").forGetter(XpCapacity::topLevel),
                Codec.floatRange(0.001f, Float.MAX_VALUE).fieldOf("capacitance").forGetter(XpCapacity::capacitance),
                Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("conductance", 0f).forGetter(XpCapacity::conductance),
                Codec.floatRange(0f, 1f).optionalFieldOf("leak", 0f).forGetter(XpCapacity::leak)
        ).apply(instance, XpCapacity::new));
    }

    /**
     * What a plant does on the brewing stand's map (potions spec 15): a segment of the path, found by the material, never by item.
     *
     * @param family  a compound class id: its colour and taste, the dud draught's hint and the recipe browser's label. No angle.
     * @param heading degrees, 0 is +x and 90 is +y: where the segment's net displacement goes.
     * @param reach   the net displacement length at full cut and heat, in map units.
     * @param shape   the curve it walks; absent in data is a line.
     * @param pull    a stabiliser: tightens the marker toward the nearest effect node along one axis, never rescues a wild miss.
     */
    public record Potion(String family, float heading, float reach, io.github.soundgoodizerfan.feedback.potion.PotionShape shape, Optional<Pull> pull) {
        static final Codec<Potion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("family").forGetter(Potion::family),
                Codec.FLOAT.optionalFieldOf("heading", 0f).forGetter(Potion::heading),
                Codec.floatRange(0f, 4f).fieldOf("reach").forGetter(Potion::reach),
                io.github.soundgoodizerfan.feedback.potion.PotionShape.CODEC.optionalFieldOf("shape", io.github.soundgoodizerfan.feedback.potion.PotionShape.LINE).forGetter(Potion::shape),
                Pull.CODEC.optionalFieldOf("pull").forGetter(Potion::pull)
        ).apply(instance, Potion::new));
    }

    /** @param axisX true for the x axis (strength), false for y (duration) */
    public record Pull(boolean axisX, float amount) {
        static final Codec<Pull> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(a -> a.equals("x"), x -> x ? "x" : "y").fieldOf("axis").forGetter(Pull::axisX),
                Codec.floatRange(0f, 1f).fieldOf("amount").forGetter(Pull::amount)
        ).apply(instance, Pull::new));
    }

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

    /** The broad class of stuff. One per material, and every material has one -- a Minecraft-native stuff is classed by its physical behaviour like any other, never as an "exotic" exception; the finer observations are {@link Trait}s. */
    public enum Kind implements StringRepresentable {
        METAL, MINERAL, CRYSTAL, CERAMIC, GLASS, GRANULAR, ORGANIC,
        /** Silicon: an element that is neither metal nor salt, shaped and cast like a metal but brittle and a poor conductor. */
        METALLOID,
        /** Petroleum and its cuts (petrochemistry spec 4.1): a fluid whose properties ride on its material, keyed by the fluid's id. */
        FLUID,
        /** Plastics and rubbers (spec 11): shaped by the form, mould and casting systems like a metal. */
        POLYMER,
        /** Foodstuffs are classed as finely as stone and metal are, by what they are: */
        GRAIN, VEGETABLE, FRUIT, FUNGUS, ANIMAL, SEAFOOD, DOUGH, DISH,
        /** Plants that carry a path for the brewing stand (potions spec 15): a flower, and a herb or fern. */
        FLOWER, HERB;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A yes/no a person could tell by handling it. Never a stand-in for identity (§11). */
    public enum Trait implements StringRepresentable {
        /** Rolls into plate whatever its kind (sculk: a gun's sculk jacket); a {@link FormType}'s {@code traits} list names the forms such a trait opens. */
        ROLLABLE,
        ALLOY, FLAMMABLE, FIREPROOF, TRANSPARENT, LUMINOUS,
        /** Gives off light when hot enough to glow (every metal does; other materials opt in). */
        INCANDESCENT, FIBROUS, POROUS, STICKY,
        /** An acid or a base: eats a vessel whose corrosion resistance is below {@code HazardTuning.CORROSIVE_BITE} (core/hazard/Corrosion). */
        CORROSIVE,
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
        /** Worked into a dough to make it rich (egg, sugar, honey): what a bread's enrichment roles take, and what names a loaf enriched, not lean (foods spec 10). */
        ENRICHING,
        /** A fat (butter, lard): what lets a dough be laminated. No fat item exists yet. */
        FAT,
        /** A pot serving (broth, stew) a soup is poured from: what a soup's base role takes. */
        BROTH,
        /** A tunnelling-active fluid not yet damped (ender spec 2): loose or over its boiling point it undergoes premature spatial transitions that hop whatever stands near (core/hazard/SpatialHop). */
        SPATIAL,
        /** A made assembly (a clad fuel rod), not bulk stock: no modular part is built from it. */
        ASSEMBLY,
        /** A drawn filament whose texture grows with each draw-then-anneal cycle (process/WorkHardening): the superconductor's. Opts in to work hardening at any ductility. */
        TEXTURED;

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
     * The temperature at which stuff made of it fails in a fire: what burns (organic, polymer, plant and food kinds, anything with
     * a {@code combustion} block) burns up at its oxidation temperature; metal, ceramic, mineral and the rest hold until they melt or
     * decompose, so iron scales in lava rather than burning away (author 2026-11-13).
     */
    public Optional<Tu> failureTemperature() {
        boolean burns = combustion.isPresent() || kind.map(k -> switch (k) {
            case ORGANIC, POLYMER, FLOWER, HERB, GRAIN, VEGETABLE, FRUIT, FUNGUS, ANIMAL, SEAFOOD, DOUGH, DISH -> true;
            default -> false;
        }).orElse(false);
        return burns ? thermal.oxidationTemperature().or(thermal::meltingPoint) : thermal.meltingPoint().or(thermal::oxidationTemperature);
    }

    /** Whether stuff made of it survives lava: its failure temperature is above lava's fire. Such an item is {@code fire_resistant}, as vanilla netherite is. */
    public boolean survivesLava() {
        return failureTemperature().map(t -> t.value() > ThermalTuning.FIRE_TU_LAVA.value()).orElse(false);
    }

    /** How fast a loose workpiece of it sheds heat: its figure, else the stock rate for anything that states none. */
    public TuRate looseCooling() {
        return thermal.coolingRate().orElse(ThermalTuning.ITEM_COOLING_TU_PER_TICK);
    }

    /**
     * The hottest an electrode of this material can run, or empty if it cannot be one: graphite sublimes
     * ({@code sublimation_temperature}); a conducting metal (one with an ampacity) boils.
     */
    public Optional<Tu> electrodeLimit() {
        return thermal().sublimationTemperature().or(() -> ampacity().isPresent() ? thermal().boilingPoint() : Optional.empty());
    }

    /**
     * How it takes, holds and survives heat.
     *
     * @param meltingPoint         where it turns liquid. Should agree with any {@code thermal_process}
     *                             melt of it.
     * @param boilingPoint         where the liquid boils off (distillation).
     * @param coolingRate          how fast a loose workpiece of it cools, replacing
     *                             {@code ThermalTuning.ITEM_COOLING_TU_PER_TICK} per material once read
     *                             (mechanics §2.2's open item).
     * @param conductivity         conductance of a one-block wall of it, on the scale where copper's
     *                             is {@code FTuning.HEAT_EXCHANGER_CONDUCTANCE}.
     * @param oxidationTemperature above this, held in air, it scales (metal) or burns (organic).
     * @param annealTemperature    heating past this undoes work hardening.
     * @param serviceTemperature   the hottest a machine body of it may run before it softens and wears.
     * @param specificHeat         Work to warm one Mu by one Tu (real kJ/(kg K)); a body's thermal mass is this times its mass.
     * @param latentHeatFusion     Work per Mu a melt or freeze absorbs or gives up at the melting point, the temperature holding.
     * @param latentHeatVaporisation Work per Mu a boil or condensation absorbs or gives up at the boiling point.
     */
    public record Thermal(Optional<Tu> meltingPoint, Optional<Tu> boilingPoint, Optional<TuRate> coolingRate,
                          Optional<Conductance> conductivity, Optional<Tu> oxidationTemperature,
                          Optional<Tu> annealTemperature, Optional<Tu> serviceTemperature,
                          Optional<Tu> sublimationTemperature, Optional<SpecificHeat> specificHeat,
                          Optional<LatentHeat> latentHeatFusion, Optional<LatentHeat> latentHeatVaporisation) {
        static final Thermal NONE = new Thermal(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        static final Codec<Thermal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Tu::new).optionalFieldOf("melting_point").forGetter(Thermal::meltingPoint),
                Units.codec(Tu::new).optionalFieldOf("boiling_point").forGetter(Thermal::boilingPoint),
                Units.codec(TuRate::new).optionalFieldOf("cooling_rate").forGetter(Thermal::coolingRate),
                Units.codec(Conductance::new).optionalFieldOf("conductivity").forGetter(Thermal::conductivity),
                Units.codec(Tu::new).optionalFieldOf("oxidation_temperature").forGetter(Thermal::oxidationTemperature),
                Units.codec(Tu::new).optionalFieldOf("anneal_temperature").forGetter(Thermal::annealTemperature),
                Units.codec(Tu::new).optionalFieldOf("service_temperature").forGetter(Thermal::serviceTemperature),
                Units.codec(Tu::new).optionalFieldOf("sublimation_temperature").forGetter(Thermal::sublimationTemperature),
                Units.codec(SpecificHeat::new).optionalFieldOf("specific_heat").forGetter(Thermal::specificHeat),
                Units.codec(LatentHeat::new).optionalFieldOf("latent_heat_fusion").forGetter(Thermal::latentHeatFusion),
                Units.codec(LatentHeat::new).optionalFieldOf("latent_heat_vaporisation").forGetter(Thermal::latentHeatVaporisation)
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
     * @param permeability  mB of gas a full 1000 mB cell wrapped in a skin of this passes each tick at rest, within its
     *                      stretch (absent or 0 is gas-tight). Read by {@code vehicle/air/Skin}.
     */
    public record Mechanical(Optional<St> toughness, Optional<Integer> ductility, Optional<St> workHardening,
                             Optional<St> yieldStrength, Optional<Float> friction, Optional<Float> abrasiveness,
                             boolean hardenable, Optional<Damping> damping, Optional<Float> permeability) {
        /**
         * How a material stores a barrel's vibration when shrunk on as its jacket: cold, the group tightens to {@code coldSpread}; once the barrel
         * has heated past {@code spoilAt} (a share of the way from the room to the round's ignition) the stored vibration goes bad and the group
         * opens to {@code spoiledSpread}.
         */
        public record Damping(float coldSpread, float spoilAt, float spoiledSpread) {
            static final Codec<Damping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("cold_spread").forGetter(Damping::coldSpread),
                    Codec.floatRange(0f, 1f).fieldOf("spoil_at").forGetter(Damping::spoilAt),
                    Codec.FLOAT.fieldOf("spoiled_spread").forGetter(Damping::spoiledSpread)
            ).apply(instance, Damping::new));
        }

        static final Mechanical NONE = new Mechanical(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.empty(), Optional.empty());

        static final Codec<Mechanical> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(St::new).optionalFieldOf("toughness").forGetter(Mechanical::toughness),
                Codec.INT.optionalFieldOf("ductility").forGetter(Mechanical::ductility),
                Units.codec(St::new).optionalFieldOf("work_hardening").forGetter(Mechanical::workHardening),
                Units.codec(St::new).optionalFieldOf("yield_strength").forGetter(Mechanical::yieldStrength),
                Codec.FLOAT.optionalFieldOf("friction").forGetter(Mechanical::friction),
                Codec.FLOAT.optionalFieldOf("abrasiveness").forGetter(Mechanical::abrasiveness),
                Codec.BOOL.optionalFieldOf("hardenable", false).forGetter(Mechanical::hardenable),
                Damping.CODEC.optionalFieldOf("damping").forGetter(Mechanical::damping),
                Codec.FLOAT.optionalFieldOf("permeability").forGetter(Mechanical::permeability)
        ).apply(instance, Mechanical::new));
    }

    /**
     * The liquid, for whatever melts.
     *
     * @param viscosity relative to water (1). §3 names viscosity as not yet earning its place; it is
     *                  recorded for a misrun in a mold, and stays unread until that is a decision.
     * @param density   liquid density -- slag floats off a heavier melt.
     * @param shearThickening how much its viscosity multiplies by at the reference flow ({@code FTuning.SHEAR_REFERENCE_MB_PER_TICK}):
     *                  0 is Newtonian (water), above 0 it stiffens the faster it is pushed (cornstarch in water; soul oil).
     */
    public record Molten(Optional<Float> viscosity, Optional<Density> density, Optional<Float> shearThickening) {
        static final Molten NONE = new Molten(Optional.empty(), Optional.empty(), Optional.empty());

        static final Codec<Molten> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("viscosity").forGetter(Molten::viscosity),
                Units.codec(Density::new).optionalFieldOf("density").forGetter(Molten::density),
                Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("shear_thickening").forGetter(Molten::shearThickening)
        ).apply(instance, Molten::new));
    }

    /**
     * What §11's separators can tell apart. Density is the material's own top-level figure.
     *
     * @param magnetic         an electromagnetic drum pulls it.
     * @param curieTemperature above this it stops being magnetic -- hot iron passes the drum.
     * @param reflectivity     0 to 1, what an optical sorter sees. Two materials that look alike
     *                         should read alike, so the sorter is genuinely fooled.
     * @param gangue           for a bearer (an ore): the host rock its grains are locked in. Ore separation reads the
     *                         contrast between this material's density, magnetism and reflectivity and the bearer's
     *                         ({@code OreLadder}); absent means {@code feedback:stone}.
     */
    public record Separation(boolean magnetic, Optional<Tu> curieTemperature, Optional<Float> reflectivity,
                             Optional<ResourceLocation> gangue) {
        static final Separation NONE = new Separation(false, Optional.empty(), Optional.empty(), Optional.empty());

        static final Codec<Separation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("magnetic", false).forGetter(Separation::magnetic),
                Units.codec(Tu::new).optionalFieldOf("curie_temperature").forGetter(Separation::curieTemperature),
                Codec.FLOAT.optionalFieldOf("reflectivity").forGetter(Separation::reflectivity),
                ResourceLocation.CODEC.optionalFieldOf("gangue").forGetter(Separation::gangue)
        ).apply(instance, Separation::new));
    }

    /**
     * What a material does as a gun's charge: not stored, read off {@link Combustion}'s chamber by
     * {@link #propellant()}, so every gun system reads one view and none of them knows which block it came from.
     *
     * @param ignitionTemperature a round left chambered in a barrel this hot goes off by itself
     *                     (cook-off, {@code docs/specs/feedback_guns_spec.md} 3.4): the material's oxidation
     *                     temperature.
     */
    public record Propellant(Pu peakPressure, float burnRate, Tu ignitionTemperature, float oxygenBalance) {
    }

    /** This material as a gun's charge, if it is one: its combustion's chamber. */
    public Optional<Propellant> propellant() {
        return combustion.flatMap(c -> c.chamber().map(ch -> new Propellant(ch.peakPressure(), ch.burnRate(),
                thermal.oxidationTemperature().orElse(new Tu(FTuning.GUN_PROPELLANT_IGNITION_TU)),
                c.oxygenBalance())));
    }

    /**
     * What burning it does: one set of figures for every fire it is in, open or confined
     * A firebox reads the open half through {@link Fuel}, a
     * gun's chamber the confined half through {@link #propellant}. Its ignition point is not here:
     * the material already states it as {@code thermal.oxidation_temperature}, above which, held
     * in air, it burns. Absent means it does not burn.
     *
     * @param flameTemperature how hot an open fire of it burns. A hard ceiling on anything it
     *                         heats -- a vessel approaches its fire and never passes it, so a fuel
     *                         too cool for a process is a real impossibility (philosophy 8). With
     *                         {@code burnTicks}, it makes the stuff a fuel.
     * @param burnTicks        how long one piece of it burns; other forms by how much stuff they
     *                         are ({@link Fuel#pieces}). Independent of the temperature on purpose:
     *                         charcoal hot and brief, wood cool and brief, coal hot and long.
     * @param spread           how much the flame varies, as a fraction, rolled once per piece and
     *                         held for its burn: philosophy 10's noise floor, entering as something
     *                         true (charcoal is not all the same).
     * @param oxygenBalance    oxygen it brings beyond what its own fuel needs, per item: positive
     *                         an oxidizer (gunpowder's nitrate), negative a fuel that needs one
     *                         (blaze powder's carbon and sulfur). A blend in a case burns
     *                         completely only when the sum is not negative.
     * @param oxygenFraction   the share of this gas that is breathable oxygen, 0 to 1: what a lung (a sculk culture)
     *                         takes from it. Air 0.21, oxygen 1. Independent of {@code oxygenBalance}, which is
     *                         about burning.
     * @param chamber          what it does confined in a case; present makes it a propellant.
     * @param flashPoint       a liquid's flash point (petrochemistry spec 4.2): above this temperature it
     *                         gives off enough vapour to ignite. Absent means it gives off none.
     */
    public record Combustion(Optional<Tu> flameTemperature, Optional<Integer> burnTicks, float spread,
                             float oxygenBalance, float oxygenFraction, Optional<Chamber> chamber, Optional<Tu> flashPoint) {
        static final Codec<Combustion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Units.codec(Tu::new).optionalFieldOf("flame_temperature").forGetter(Combustion::flameTemperature),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("burn_ticks").forGetter(Combustion::burnTicks),
                Codec.FLOAT.optionalFieldOf("spread", 0.06f).forGetter(Combustion::spread),
                Codec.FLOAT.optionalFieldOf("oxygen_balance", 0f).forGetter(Combustion::oxygenBalance),
                Codec.floatRange(0f, 1f).optionalFieldOf("oxygen_fraction", 0f).forGetter(Combustion::oxygenFraction),
                Chamber.CODEC.optionalFieldOf("chamber").forGetter(Combustion::chamber),
                Units.codec(Tu::new).optionalFieldOf("flash_point").forGetter(Combustion::flashPoint)
        ).apply(instance, Combustion::new));
    }

    /**
     * A burn confined in a case ({@code docs/specs/feedback_guns_spec.md} 3.6): two independent numbers, as
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
     * How it carries and makes electricity. A conductor's current rating is the
     * top-level {@code ampacity}; this holds the other figures. Resistance is not modelled, so
     * {@code drop} is stated directly as the thing it would have been derived to be.
     *
     * @param drop               Eu lost from each packet per block of a wire of it, before the
     *                           gauge and {@code ElectricMachineTuning#CABLE_LOSS_EU_PER_DROP} (6.4). A
     *                           path loses the sum over its blocks, whatever the current.
     * @param electrodePotential how noble a cell electrode of it is against a common reference (never
     *                           shown as volts). A galvanic cell's band follows the difference between
     *                           its two electrodes' figures, and an electrolytic bath plates the noblest
     *                           first, so no cell names a voltage.
     * @param conductivity       how well a body of it passes current, copper 1. Read by {@code Exposure}
     *                           (what a worn plate or a floor does to a shock); absent, the kind decides
     *                           there: a metal conducts, an organic insulates.
     * @param insulation         the highest voltage band a coat of it holds off when put on a wire
     *                           (6.4): above it the coat burns off. Absent, it is no insulator.
     * @param piezoCoupling      how strongly a plate of it rings when squeezed and squeezes when rung, 0-1
     *                           (6.17). A mass probe's crystal must have some; absent, it does not ring.
     * @param rectifying         how good a spot a cat's whisker can find on it, 0-1 (6.18). Absent, it has none.
     * @param secondaryCell      the plate figures of a rechargeable cell made with this metal as its plates (the lead-acid
     *                           battery's lead). Absent, it is no battery plate.
     * @param coreLoss           the share, 0 to 1, of the power through a machine's core of it that is lost as heat in
     *                           the core (hysteresis and eddy currents). Absent, {@code FTuning.CORE_LOSS_DEFAULT}.
     * @param quenchDrop         a superconductor's drop once it quenches (current past its ampacity). Absent, the
     *                           conductor never quenches: it only heats when overloaded.
     */
    public record Electrical(Optional<Float> drop, Optional<Float> electrodePotential, Optional<Float> conductivity,
                             Optional<VoltageBand> insulation,
                             Optional<Float> piezoCoupling, Optional<Float> rectifying,
                             Optional<SecondaryCell> secondaryCell, Optional<Float> coreLoss, Optional<Float> quenchDrop) {
        static final Electrical NONE = new Electrical(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        static final Codec<Electrical> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("drop").forGetter(Electrical::drop),
                Codec.FLOAT.optionalFieldOf("electrode_potential").forGetter(Electrical::electrodePotential),
                Codec.FLOAT.optionalFieldOf("conductivity").forGetter(Electrical::conductivity),
                VoltageBand.CODEC.optionalFieldOf("insulation").forGetter(Electrical::insulation),
                Codec.FLOAT.optionalFieldOf("piezo_coupling").forGetter(Electrical::piezoCoupling),
                Codec.FLOAT.optionalFieldOf("rectifying").forGetter(Electrical::rectifying),
                SecondaryCell.CODEC.optionalFieldOf("secondary_cell").forGetter(Electrical::secondaryCell),
                Codec.FLOAT.optionalFieldOf("core_loss").forGetter(Electrical::coreLoss),
                Codec.FLOAT.optionalFieldOf("quench_drop").forGetter(Electrical::quenchDrop)
        ).apply(instance, Electrical::new));
    }

    /**
     * Plate figures of a secondary (rechargeable) cell, per cell. A chemistry is one more material file with this block.
     *
     * @param emptyShare       the share of its rated current a cell still gives when empty: it holds full to the knee, ten per cent charge, then falls to this (the cliff).
     * @param euPerIngot       charge one ingot of plate holds per cell, in Eu.
     * @param coldFactor       the share of capacity left at the coldest ambient; 1 at room temperature.
     * @param electrolyte      the fluid tag (id) the cell is filled with: sulfuric acid for lead, lye for an alkaline cell.
     * @param chargeEfficiency the share of charged work kept; the rest heats the case.
     * @param gassing          whether overcharge electrolyses water into hydrogen; false for a cell that shrugs it off.
     */
    public record SecondaryCell(float emptyShare, float euPerIngot, float coldFactor,
                                ResourceLocation electrolyte, float chargeEfficiency, boolean gassing) {
        static final Codec<SecondaryCell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("empty_share", 1f).forGetter(SecondaryCell::emptyShare),
                Codec.FLOAT.fieldOf("eu_per_ingot").forGetter(SecondaryCell::euPerIngot),
                Codec.FLOAT.optionalFieldOf("cold_factor", 1f).forGetter(SecondaryCell::coldFactor),
                ResourceLocation.CODEC.optionalFieldOf("electrolyte", ResourceLocation.fromNamespaceAndPath("feedback", "battery_acid")).forGetter(SecondaryCell::electrolyte),
                Codec.FLOAT.optionalFieldOf("charge_efficiency", FTuning.BATTERY_CHARGE_EFFICIENCY).forGetter(SecondaryCell::chargeEfficiency),
                Codec.BOOL.optionalFieldOf("gassing", true).forGetter(SecondaryCell::gassing)
        ).apply(instance, SecondaryCell::new));
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
            Tail.CODEC.forGetter(m -> new Tail(m.electrical(), m.composition(), m.enchantability(), m.color(), m.perks(), m.attunement(), m.food(), m.bears(), m.radioactivity(), m.catalyst(), m.potion(), m.xpStore(), m.neutron(), new Extras(m.luminance(), m.phosphor(), m.fastener(), m.toxicity())))
    ).apply(instance, (hardness, min, max, forms, exclude, density, kind, traits, thermal, mechanical,
                       molten, separation, ampacity, corrosion, combustion, tail) -> new Material(
            hardness, min, max, forms, exclude, density, kind, traits, thermal, mechanical, molten,
            separation, ampacity, corrosion, combustion, tail.electrical(), tail.composition(),
            tail.enchantability(), tail.color(), tail.perks(), tail.attunement(), tail.food().map(f -> f.heated(thermal.specificHeat())), tail.bears(), tail.radioactivity(), tail.catalyst(), tail.potion(), tail.xpStore(), tail.luminance(), tail.phosphor(), tail.neutron(), tail.fastener(), tail.toxicity())));

    /**
     * The keys that did not fit the codec's sixteen.
     *
     * @param luminance block light (0 to 15) a lit lamp panel of this phosphor gives at full; absent, none.
     * @param phosphor  whether it glows under an electric current (an electroluminescent lamp panel can be made of it).
     * @param fastener coarse quality, 1 to 5, of a screw, bolt or rivet made of it (the Engineering Table's fasteners): strength, corrosion
     *                 and fit in one figure. Absent for a material that makes no fastener.
     * @param enchantability how well it takes an enchantment, on vanilla's scale (gold 22, stone 5):
     *                       the one figure a modular item's enchanting reads, from its main part
     *                      . Absent reads as poor.
     */
    private record Tail(Electrical electrical, Map<ResourceLocation, Float> composition, Optional<Integer> enchantability,
                        Optional<Integer> color,
                        Map<Perk, Integer> perks, Optional<Attuning> attunement, Optional<Food> food,
                        Map<ResourceLocation, Float> bears, Optional<Ru> radioactivity, Optional<Catalyst> catalyst, Optional<Potion> potion, Optional<XpCapacity> xpStore,
                        Optional<Neutron> neutron, Extras extras) {
        Optional<Integer> luminance() { return extras.luminance(); }
        boolean phosphor() { return extras.phosphor(); }
        Optional<Integer> fastener() { return extras.fastener(); }
        Optional<Float> toxicity() { return extras.toxicity(); }

        static final com.mojang.serialization.MapCodec<Tail> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Electrical.CODEC.optionalFieldOf("electrical", Electrical.NONE).forGetter(Tail::electrical),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).optionalFieldOf("composition", Map.of()).forGetter(Tail::composition),
                Codec.INT.optionalFieldOf("enchantability").forGetter(Tail::enchantability),
                HEX.optionalFieldOf("color").forGetter(Tail::color),
                Codec.unboundedMap(Perk.CODEC, Codec.intRange(1, 5)).optionalFieldOf("perks", Map.of()).forGetter(Tail::perks),
                Attuning.CODEC.optionalFieldOf("attuning").forGetter(Tail::attunement),
                Food.CODEC.optionalFieldOf("food").forGetter(Tail::food),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).optionalFieldOf("bears", Map.of()).forGetter(Tail::bears),
                Units.codec(Ru::new).optionalFieldOf("radioactivity").forGetter(Tail::radioactivity),
                Catalyst.CODEC.optionalFieldOf("catalyst").forGetter(Tail::catalyst),
                Potion.CODEC.optionalFieldOf("potion").forGetter(Tail::potion),
                XpCapacity.CODEC.optionalFieldOf("xp_store").forGetter(Tail::xpStore),
                Neutron.CODEC.optionalFieldOf("neutron").forGetter(Tail::neutron),
                Extras.CODEC.forGetter(Tail::extras)
        ).apply(instance, Tail::new));
    }

    /**
     * The last small keys, grouped only because a record codec stops at sixteen; the JSON keeps them top level.
     *
     * @param toxicity how harmful it is swallowed: a log-compressed index of the real oral LD50 in mg/kg (author 2026-11-13);
     *                 0 is harmless to eat, higher is deadlier. The index and its constants are in {@code docs/material_figures.md}.
     */
    private record Extras(Optional<Integer> luminance, boolean phosphor, Optional<Integer> fastener, Optional<Float> toxicity) {
        static final com.mojang.serialization.MapCodec<Extras> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(0, 15).optionalFieldOf("luminance").forGetter(Extras::luminance),
                Codec.BOOL.optionalFieldOf("phosphor", false).forGetter(Extras::phosphor),
                Codec.intRange(1, 5).optionalFieldOf("fastener").forGetter(Extras::fastener),
                Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("toxicity").forGetter(Extras::toxicity)
        ).apply(instance, Extras::new));
    }

    /**
     * What it is as food ({@code docs/specs/feedback_food_spec.md} 3). Hunger and saturation are not
     * here: they are the vanilla figures of the {@code cooked} form's item, read off it, so a
     * modded food joins by naming its forms.
     *
     * @param moisture   fraction of the item that is water. Cooking drives it off; a food that has
     *                   lost much of it is dried.
     * @param pathogen   live-organism load a raw item carries, as a strength (the data is in Qu, up to
     *                   {@link ProcessTuning#FOOD_QU_PER_STRENGTH}). Heat kills a fraction of it each
     *                   tick, scaled by cooking speed ({@code Cooking.tick}).
     * @param toxin      stable-toxin load, a strength as {@code pathogen}. Heat does not touch it.
     * @param fat        0 to 1: slow absorption. With {@code fibre}, delays and stretches what its compounds do.
     * @param fibre      0 to 1: as {@code fat}.
     * @param compounds  named actives and a strength each (the data is Qu per item); what each does is a {@link Compound}.
     * @param perks      traits specific to this food, a potency each (Qu per item); what each does is a {@link FoodPerk}.
     * @param shelf      1 or more: how many times longer than a wet food it takes to rot (dry grain keeps).
     * @param mass       one item's heft against a middling piece (1): a thick cut takes {@code mass} times as long to cook and to dry
     *                   through, and its core lags a pan {@code mass} times as long. One temperature for the item; mass sets the rate.
     * @param charTemperature where it burns; absent, the material's {@code oxidation_temperature},
     *                   else {@link FTuning#FOOD_CHAR_TU}.
     */
    public record Food(float moisture, float pathogen, float toxin, Optional<Tu> charTemperature,
                       float fat, float fibre, Map<String, Float> compounds, Map<String, Float> perks, float shelf, float mass) {
        /**
         * An amount of an active in Qu on disk, a strength in memory: the one read site of
         * {@link ProcessTuning#FOOD_QU_PER_STRENGTH}. Over a full strength's worth is refused.
         */
        private static final Codec<Float> QU_AS_STRENGTH = Units.codec(Qu::new)
                .validate(q -> q.value() >= 0f && q.value() <= ProcessTuning.FOOD_QU_PER_STRENGTH + 1e-6f
                        ? DataResult.success(q) : DataResult.error(() -> "amount " + q.value() + " Qu is not within 0 to " + ProcessTuning.FOOD_QU_PER_STRENGTH))
                .xmap(q -> q.value() / ProcessTuning.FOOD_QU_PER_STRENGTH, strength -> new Qu(strength * ProcessTuning.FOOD_QU_PER_STRENGTH));

        /**
         * This food with its heft scaled by its specific heat against a wet food's ({@code FTuning.FOOD_REFERENCE_SPECIFIC_HEAT}): the
         * file states how big a piece is, the material states how much heat that piece takes, and a dry grain warms and dries faster
         * than a wet cut of the same size.
         */
        public Food heated(Optional<SpecificHeat> cp) {
            if (cp.isEmpty())
                return this;
            float heft = Math.max(0.2f, Math.min(10f, mass * cp.get().workPerMuPerTu() / FTuning.FOOD_REFERENCE_SPECIFIC_HEAT));
            return new Food(moisture, pathogen, toxin, charTemperature, fat, fibre, compounds, perks, shelf, heft);
        }

        /** A strength (the held figure) as the Qu it stands for, for display. */
        public static Qu qu(float strength) {
            return new Qu(strength * ProcessTuning.FOOD_QU_PER_STRENGTH);
        }

        static final Codec<Food> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(0f, 1f).optionalFieldOf("moisture", 0.7f).forGetter(Food::moisture),
                QU_AS_STRENGTH.optionalFieldOf("pathogen", 0f).forGetter(Food::pathogen),
                QU_AS_STRENGTH.optionalFieldOf("toxin", 0f).forGetter(Food::toxin),
                Units.codec(Tu::new).optionalFieldOf("char_tu").forGetter(Food::charTemperature),
                Codec.floatRange(0f, 1f).optionalFieldOf("fat", 0f).forGetter(Food::fat),
                Codec.floatRange(0f, 1f).optionalFieldOf("fibre", 0f).forGetter(Food::fibre),
                Codec.unboundedMap(Codec.STRING, QU_AS_STRENGTH).optionalFieldOf("compounds", Map.of()).forGetter(Food::compounds),
                Codec.unboundedMap(Codec.STRING, QU_AS_STRENGTH).optionalFieldOf("perks", Map.of()).forGetter(Food::perks),
                Codec.floatRange(1f, 1000f).optionalFieldOf("shelf", 1f).forGetter(Food::shelf),
                Codec.floatRange(0.2f, 10f).optionalFieldOf("mass", 1f).forGetter(Food::mass)
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
     * Where and how it takes an attunement (enchanting, {@code docs/specs/feedback_enchanting_spec.md} 3-4): the field acts only
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
                    : io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.ENCHANT_COLD_POINT_TU;
            return new Attuning(new Tu(top), new Tu(top - io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.ENCHANT_WINDOW_TU),
                    io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.ENCHANT_COERCIVE,
                    io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.ENCHANT_LOOP_WIDTH,
                    io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning.ENCHANT_LAG_TICKS,
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
                if (form.getValue() == item && !(item instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem))
                    return Optional.of(new Located(e.getKey(), e.getValue(), form.getKey()));
        return Optional.empty();
    }

    /**
     * What {@code stack} is: a shaped part by its {@link MadeOf} (in the form named by the part's
     * item, {@code pickaxe_head}), anything else by its item.
     */
    public static Optional<Located> find(Map<ResourceLocation, Material> materials, net.minecraft.world.item.ItemStack stack) {
        // A finished modular item that can be held in another (a suit module) is found by its nest form and wearing material.
        if (stack.has(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get())
                || stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get())) {
            Optional<Located> held = io.github.soundgoodizerfan.feedback.modular.type.ModularNest.locate(materials, stack);
            if (held.isPresent())
                return held;
        }
        MadeOf made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        // Raw stock the enchanting table stamped (a rod, a grip) is still found by its item: the stamp only carries the attunement.
        if (made == null || !(stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem))
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
        // A part form's item is shared by every material, so the registry alone says yes for air's cylinder: ask the form type too.
        if (exclude.contains(form) || !generated(FormItems.types()).contains(form))
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
            if (i instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem)
                stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), new MadeOf(id, PART_STOCK, 1));
            return stack;
        });
    }

    /** What a form step takes: the item, and for a shaped part also its material, so copper's cylinder is not steel's. */
    public java.util.Optional<net.minecraft.world.item.crafting.Ingredient> ingredient(ResourceLocation id, String form) {
        return item(id, form).map(i -> i instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem
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
