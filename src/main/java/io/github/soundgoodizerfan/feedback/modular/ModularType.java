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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * One kind of modular item as data ({@code data/feedback/modular_type/<id>.json}): which roles it
 * has, what each accepts, the patterns that assemble it, and the numbers of its geometry. A new
 * type is a file and a lang line plus one registry line -- an item has to exist before any
 * datapack does. Tinkers' tool definitions and Silent Gear's gear types are the precedent; a
 * {@code parent} lets a type inherit another's file (see {@link ModularTypes}).
 *
 * <h2>The pattern is the type</h2>
 * Types are told apart by which roles are filled and how many of each, as the Tool Table reads them
 * ({@link #assemblies}): a pickaxe is a head over two handles, an axe is not. The count of a raw
 * role is its size -- two sticks are a longer handle than one -- so a longer handle is a different
 * pattern, not a slider. Where the letters sit in a pattern is only how the Blueprint Table draws it.
 *
 * @param family      which bake the type uses.
 * @param slot        where it is worn or held.
 * @param mines       block tags it digs, each at a share of the full dig speed.
 * @param strike      how well the head's shape turns a blow into damage on a body: an axe's
 *                    cleaving edge against a pick's point. Geometry, not a stat.
 * @param wearLitres  volume of the edge zone that wears, which sets durability. The edge, not the
 *                    whole head: a shovel and a pickaxe last the same.
 * @param wearRole    the role whose material wears (and sets the bite).
 * @param primary     the role a vanilla item's own material fills when it is converted.
 * @param roles       by the letter patterns use.
 * @param patterns    rows of letters, a space for nothing; mirrored freely.
 * @param defaults    the material each other role takes in a converted vanilla item.
 * @param vanilla     material form names that are this type in vanilla ({@code pickaxe},
 *                    {@code chainmail_helmet}) to the form the part is recorded as.
 * @param armourMax   armour points the piece gives when its plate is as good as plate gets.
 * @param combos      for a twin-headed type: the names of the pairs of heads that have one, keyed by
 *                    the two head kinds sorted and joined with {@code +} ({@code axe_head+pickaxe_head}
 *                    is a mattock). A pair with no name is called by its two heads. Naming only: what
 *                    a pair does comes from the heads.
 * @param trait       the signature behaviour of the whole type ({@link ModularTraits}), by id: the
 *                    spear's charge. A variant's own trait replaces it. Empty for none.
 * @param traitParams numbers the type's trait reads.
 * @param blueprint   whether making one takes a blueprint of it in the Tool Table's plan slot (spec 1: complicated
 *                    items are designed on paper first; simple ones are their parts).
 * @param ranges      for a raw-stock role whose size is the player's to choose, the fewest and most items it takes
 *                    ({@code [min, max]}; a minimum of 0 makes the role optional): a pattern then names the role once
 *                    and the type makes one form of every size, instead of a pattern for each. A charge's filler is
 *                    one to eight powders; writing seventy-two patterns for it would be the mistake.
 * @param bakes       for a dish: it is formed raw and cooked whole in a heated vessel ({@code Cooking#findCookable}), instead of
 *                    being served cooked through (a pie).
 * @param shapes      things the type's bench shapes that fill no role as a part: a food laid from dough at the Prep
 *                    Table's Parts tab (flatbread dough, noodles), the food item itself and no {@code MadeOf}.
 */
public record ModularType(Family family, Slot slot, List<Mine> mines, float strike, float wearLitres,
                          String wearRole, String primary, Map<String, Role> roles,
                          List<List<String>> patterns, Map<String, ResourceLocation> defaults,
                          Map<String, String> vanilla, float armourMax, boolean blueprint,
                          Naming names, String trait, Map<String, Float> traitParams, Map<String, List<Integer>> ranges,
                          List<Shaping> shapes, boolean bakes) {

    /**
     * The names a build gets. Naming only; what a build does comes from its parts.
     *
     * @param combos       for a twin-headed type: the names of the pairs of heads that have one, keyed by the two
     *                     head kinds sorted and joined with {@code +} ({@code axe_head+pickaxe_head} is a mattock).
     *                     A pair with no name is called by its two heads.
     * @param designations for a gun: real-world designations, in order, the first that matches naming the item
     *                     as the tooltip's class line ({@link Designation}).
     */
    public record Naming(Map<String, String> combos, List<Designation> designations) {
        static final Naming NONE = new Naming(Map.of(), List.of());
        static final Codec<Naming> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("combos", Map.of()).forGetter(Naming::combos),
                Designation.CODEC.listOf().optionalFieldOf("designations", List.of()).forGetter(Naming::designations)
        ).apply(instance, Naming::new));
    }

    /**
     * A real-world name for a build that a rule reads off its variant, its traits and a few named facts
     * (spec 8.4): a revolver is a handgun with a cylinder, a sawn-off a short double shotgun. Nothing in play
     * depends on it. Every condition that is written must hold; a value may list alternatives with {@code |}.
     *
     * @param name   lang key tail ({@code gun.feedback.designation.<name>}).
     * @param is     facts that must equal one alternative: {@code variant}, {@code action}, {@code feed},
     *               {@code sight}, {@code barrel} (short, medium, long) and {@code calibre} (the barrel's class).
     * @param traits trait ids the build must have.
     * @param without trait ids it must not have.
     */
    public record Designation(String name, Map<String, String> is, List<String> traits, List<String> without) {
        static final Codec<Designation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Designation::name),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("is", Map.of()).forGetter(Designation::is),
                Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(Designation::traits),
                Codec.STRING.listOf().optionalFieldOf("without", List.of()).forGetter(Designation::without)
        ).apply(instance, Designation::new));
    }

    /** For a twin-headed type: the names of the pairs of heads that have one. */
    public Map<String, String> combos() {
        return names.combos();
    }

    /** Which physics bakes it. */
    public enum Family implements StringRepresentable {
        DIG, BLADE, MACE, SPEAR, BOW, CROSSBOW, SHIELD, ARMOUR, SHEARS,
        /** A gun: its roles and blueprint gate are the core's, its bake is {@code GunDesign.bake} (spec 8). It has no patterns. */
        GUN,
        /** A placed explosive: its bake is {@code Blast} on its filler and casing. It has no wear and no tool figures. */
        CHARGE,
        /** A machine's striking head ({@code machine/HeadSlot}): its bake is the St a blow of it can carry. No wear bar of its own: its condition rides the stack. */
        HEAD,
        /** A dish: its roles are told apart by the food traits of their materials, its bake is {@code DishBake} (dish spec 2). It has no wear and no tool figures. */
        DISH,
        /** A tool worked by hand in a crafting grid or at a table (hand hammer, hand knife): its bake is the St of an ordinary blow. Wears like a tool, digs and fights as nothing. */
        HAND,
        /** A hand covering worn in the hands slot (gauntlet, glove): its bake is the damage it adds to a barehanded swing, and its plate decides what it keeps off the hand. */
        GAUNTLET;

        static final Codec<Family> CODEC = StringRepresentable.fromEnum(Family::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Slot implements StringRepresentable {
        MAINHAND(EquipmentSlot.MAINHAND), OFFHAND(EquipmentSlot.OFFHAND), HEAD(EquipmentSlot.HEAD),
        CHEST(EquipmentSlot.CHEST), LEGS(EquipmentSlot.LEGS), FEET(EquipmentSlot.FEET);

        static final Codec<Slot> CODEC = StringRepresentable.fromEnum(Slot::values);

        private final EquipmentSlot slot;

        Slot(EquipmentSlot slot) {
            this.slot = slot;
        }

        public EquipmentSlot equipment() {
            return slot;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A block tag a type digs and how fast, as a share of the full figure. */
    public record Mine(ResourceLocation tag, float factor) {
        static final Codec<Mine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("tag").forGetter(Mine::tag),
                Codec.FLOAT.optionalFieldOf("factor", 1f).forGetter(Mine::factor)
        ).apply(instance, Mine::new));
    }

    /**
     * One place in the item a part goes.
     *
     * @param name   lang key tail, and what a tooltip calls it.
     * @param forms  forms of a material that fit, {@code *_planks} meaning any form ending so.
     * @param traits observations the material must show (a lining is fibrous).
     * @param litres volume of one cell of it.
     * @param length for a rod (handle, haft, shaft): how long one cell is. A rod's cells run from
     *               the hand outward and add up.
     * @param at     for anything else: how far from the hand it sits, in metres.
     * @param rides  whether {@code at} is measured from the far end of the rod instead (a head
     *               sits on top of its handle).
     * @param factor per form, how much of the volume does the work (mail covers less than plate).
     * @param layer  which sprite layer of the item model it tints. A role with no layer in the
     *               model (a binding) is not drawn. Also the order of the roles, which a synced
     *               map cannot be trusted to keep.
     * @param part   if the role takes a shaped part rather than raw stock: how that part is made.
     *               {@code forms} then names the part's item ({@code pickaxe_head}), and
     *               {@code litres} is still per cell of the stock that went into it.
     * @param stock  for a striking role that takes any of several head kinds ({@code forms} names
     *               them, the first is the default): the raw stock a head of any of them is made
     *               from, which is what mends it. A twin-headed tool's two heads are such roles.
     * @param set    a hard piece set into the wearing part rather than a part in its own right: a
     *               tip in a head or blade, studs in plate. It lends the wearing part its hardness
     *               and takes its toughness from the body ({@link ModularBake}).
     * @param dims   the as-built measures a part of this role carries, each with its range and unit
     *               ({@link Dim}); empty for a role whose part is only a material and a shape. A machine writes
     *               them ({@code Machining}), the bake reads them.
     * @param alt    for a second part of the same kind as another role (a double gun's second barrel): that
     *               role's letter. The two take the same parts by design, so the audit does not count their
     *               overlap, and which is which is settled by sorting, as a twin's heads are.
     * @param bulk   for a role no pattern lays (an optional part): how many items it takes when it is on.
     * @param after  a role not placed at the table's assembly: a minigame adds it to a finished design (a dish's finish).
     *               The type keeps it so the bake and the design can hold it; inference and labels skip it.
     * @param anyTraits observations of which the material must show at least one, besides all of {@code traits}; empty for none
     *               (a pie's filling is vegetal or protein).
     * @param seal   for a dish's container: 0 open, 1 covered (slows drying, migration and spill), 2 sealed (also stops rot).
     */
    public record Role(String name, List<String> forms, List<Material.Trait> traits, float litres, float length,
                       float at, boolean rides, Map<String, Float> factor, int layer, Optional<Shaping> part,
                       List<String> stock, boolean set, Map<String, Dim> dims, String alt, int bulk, boolean after, int seal,
                       List<Material.Trait> anyTraits) {
        static final Codec<Role> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Role::name),
                Codec.STRING.listOf().fieldOf("forms").forGetter(Role::forms),
                Material.Trait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(Role::traits),
                Codec.FLOAT.fieldOf("litres").forGetter(Role::litres),
                Codec.FLOAT.optionalFieldOf("length", 0f).forGetter(Role::length),
                Codec.FLOAT.optionalFieldOf("at", 0f).forGetter(Role::at),
                Codec.BOOL.optionalFieldOf("rides", false).forGetter(Role::rides),
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("factor", Map.of()).forGetter(Role::factor),
                Codec.INT.optionalFieldOf("layer", 9).forGetter(Role::layer),
                Shaping.CODEC.optionalFieldOf("part").forGetter(Role::part),
                Codec.STRING.listOf().optionalFieldOf("stock", List.of()).forGetter(Role::stock),
                Codec.BOOL.optionalFieldOf("set", false).forGetter(Role::set),
                Codec.unboundedMap(Codec.STRING, Dim.CODEC).optionalFieldOf("dims", Map.of()).forGetter(Role::dims),
                Codec.mapPair(Codec.STRING.optionalFieldOf("alt", ""), Material.Trait.CODEC.listOf().optionalFieldOf("any_traits", List.of()))
                        .forGetter(r -> com.mojang.datafixers.util.Pair.of(r.alt(), r.anyTraits())),
                Codec.INT.optionalFieldOf("bulk", 1).forGetter(Role::bulk),
                // A group holds sixteen fields, so these two share one.
                Codec.mapPair(Codec.BOOL.optionalFieldOf("after", false), Codec.INT.optionalFieldOf("seal", 0))
                        .forGetter(r -> com.mojang.datafixers.util.Pair.of(r.after(), r.seal()))
        ).apply(instance, (name, forms, traits, litres, length, at, rides, factor, layer, part, stock, set, dims, alts, bulk, flags) ->
                new Role(name, forms, traits, litres, length, at, rides, factor, layer, part, stock, set, dims, alts.getFirst(), bulk,
                        flags.getFirst(), flags.getSecond(), alts.getSecond())));

        public boolean rod() {
            return length > 0;
        }

        /**
         * The measure {@code name} of {@code made}: what the machine left on it, else the shape's own nominal
         * (a barrel variant names its bore), else the role's default. Clamped to the role's range.
         */
        public float dim(ModularDesign.Part made, String name) {
            Dim d = dims.get(name);
            if (d == null)
                return made.dim(name, 0f);
            float nominal = variant(made).map(v -> v.params().get(name)).orElse(d.def());
            return d.clamp(made.dim(name, nominal));
        }

        public boolean accepts(Material.Located l) {
            return fits(forms, l.form()) && traits.stream().allMatch(l.material().traits()::contains) && anyFits(l);
        }

        private boolean anyFits(Material.Located l) {
            return anyTraits.isEmpty() || anyTraits.stream().anyMatch(l.material().traits()::contains);
        }

        /** Every trait that decides whether a material fits, all-of or any-of: two food roles with none in common never take one item. */
        public List<Material.Trait> seen() {
            if (anyTraits.isEmpty())
                return traits;
            List<Material.Trait> out = new ArrayList<>(traits);
            out.addAll(anyTraits);
            return out;
        }

        /** The raw stock the role is made of: the part's, if it takes a part, else what it takes. */
        public List<String> stockForms() {
            return part.map(Shaping::stock).orElse(stock.isEmpty() ? forms : stock);
        }

        /** Whether this is a head of whatever kind: one of the heads of a twin-headed tool ({@link ModularHeads}). */
        public boolean striker() {
            return !stock.isEmpty();
        }

        /** Whether {@code l} is the stock this role is made of -- what mends it. */
        public boolean acceptsStock(Material.Located l) {
            return fits(stockForms(), l.form()) && traits.stream().allMatch(l.material().traits()::contains) && anyFits(l);
        }

        public float factor(String form) {
            return factor.getOrDefault(form, 1f);
        }

        /** The variant {@code made} was shaped in, if it has one. */
        private Optional<Variant> variantOf(ModularDesign.Part made) {
            Optional<Shaping> shaping = striker() ? ModularHeads.of(ModularHeads.kindOf(made, this)).map(ModularHeads::shaping) : part;
            if (made.variant().isEmpty())
                return shaping.filter(s -> !s.base().isEmpty()).flatMap(s -> s.variant(s.base()));
            return shaping.flatMap(s -> s.variant(made.variant()));
        }

        /** Thickness of {@code made} against the standard shape, and as hammered ({@link ModularDesign.Part#built}). */
        public float section(ModularDesign.Part made) {
            return variantOf(made).map(Variant::section).orElse(1f) * made.built();
        }

        /** How well the shape of {@code made} turns a blow into damage, against the standard. */
        public float strike(ModularDesign.Part made) {
            return variantOf(made).map(Variant::strike).orElse(1f);
        }

        /** Metres the shape of {@code made} moves its centre of mass from the hand. */
        public float centre(ModularDesign.Part made) {
            return variantOf(made).map(Variant::centre).orElse(0f);
        }

        /** The variant {@code made} was shaped in, if it has one. */
        public Optional<Variant> variant(ModularDesign.Part made) {
            return variantOf(made);
        }

        /** The share of the stock {@code made}'s shape keeps, and as hammered ({@link ModularDesign.Part#built}). */
        public float kept(ModularDesign.Part made) {
            return variantOf(made).map(Variant::mass).orElse(1f) * made.built();
        }

        /** Metres the shape of {@code made} moves it from the hand. */
        public float reach(ModularDesign.Part made) {
            return variantOf(made).map(Variant::reach).orElse(0f);
        }
    }

    private static final Codec<List<String>> ROW = Codec.STRING.listOf();

    public static final Codec<ModularType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Family.CODEC.fieldOf("family").forGetter(ModularType::family),
            Slot.CODEC.optionalFieldOf("slot", Slot.MAINHAND).forGetter(ModularType::slot),
            Mine.CODEC.listOf().optionalFieldOf("mines", List.of()).forGetter(ModularType::mines),
            Codec.FLOAT.optionalFieldOf("strike", 1f).forGetter(ModularType::strike),
            Codec.FLOAT.optionalFieldOf("wear_litres", 0.375f).forGetter(ModularType::wearLitres),
            Codec.STRING.fieldOf("wear_role").forGetter(ModularType::wearRole),
            Codec.STRING.fieldOf("primary").forGetter(ModularType::primary),
            Codec.unboundedMap(Codec.STRING, Role.CODEC).fieldOf("roles").forGetter(ModularType::roles),
            ROW.listOf().fieldOf("patterns").forGetter(ModularType::patterns),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("defaults", Map.of()).forGetter(ModularType::defaults),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("vanilla", Map.of()).forGetter(ModularType::vanilla),
            Codec.FLOAT.optionalFieldOf("armour_max", 0f).forGetter(ModularType::armourMax),
            // Two fields of the one object, paired only because a record codec group stops at sixteen.
            Codec.mapPair(Codec.BOOL.optionalFieldOf("blueprint", false),
                    Codec.unboundedMap(Codec.STRING, Codec.INT.listOf()).optionalFieldOf("ranges", Map.of()))
                    .forGetter(t -> com.mojang.datafixers.util.Pair.of(t.blueprint(), t.ranges())),
            Naming.CODEC.optionalFieldOf("names", Naming.NONE).forGetter(ModularType::names),
            Codec.mapPair(Codec.STRING.optionalFieldOf("trait", ""), Codec.BOOL.optionalFieldOf("bakes", false))
                    .forGetter(t -> com.mojang.datafixers.util.Pair.of(t.trait(), t.bakes())),
            Codec.mapPair(Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("trait_params", Map.of()),
                    Shaping.CODEC.listOf().optionalFieldOf("shapes", List.of()))
                    .forGetter(t -> com.mojang.datafixers.util.Pair.of(t.traitParams(), t.shapes()))
    ).apply(instance, (family, slot, mines, strike, wearLitres, wearRole, primary, roles, patterns, defaults, vanilla, armourMax,
                       gate, names, traitBakes, tail) -> new ModularType(family, slot, mines, strike, wearLitres, wearRole, primary,
            roles, patterns, defaults, vanilla, armourMax, gate.getFirst(), names, traitBakes.getFirst(), tail.getFirst(), gate.getSecond(), tail.getSecond(), traitBakes.getSecond())));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ModularType> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /**
     * One as-built measure a part may carry ({@link ModularDesign.Part#dims}): the range a machine can leave it
     * in, the unit it is quoted in, and what a part nothing has cut reads as. A continuous figure has a place in
     * the core, so a type that wants one declares it per role instead of growing a field on the design.
     *
     * @param min  the least it can be.
     * @param max  the most.
     * @param unit lang key tail for the unit ({@code modular.feedback.unit.<unit>}), e.g. {@code mm}.
     * @param def  its value on a part no machine has touched.
     */
    public record Dim(float min, float max, String unit, float def) {
        static final Codec<Dim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("min").forGetter(Dim::min),
                Codec.FLOAT.fieldOf("max").forGetter(Dim::max),
                Codec.STRING.optionalFieldOf("unit", "").forGetter(Dim::unit),
                Codec.FLOAT.fieldOf("default").forGetter(Dim::def)
        ).apply(instance, Dim::new));

        public float clamp(float v) {
            return Math.min(max, Math.max(min, v));
        }
    }

    /**
     * How a part is shaped from stock (step 1 of the parts pass): the stock laid in {@code shape}
     * at the Tool Table with a hand hammer ({@link ToolTableShaping}). One registered item
     * per part, the material on the stack ({@link io.github.soundgoodizerfan.feedback.process.MadeOf}).
     *
     * @param item  the part's item.
     * @param shape rows of the role's letter, a space for nothing; mirrored freely. Its cells are
     *              the part's size, as a pattern's were.
     * @param stock forms of a material it can be shaped from, as a role's {@code forms}.
     * @param traits traits the stock's material must have, as a role's {@code traits}: a food shape takes
     *               only dough ({@code kneadable}), where any material in the form would otherwise do.
     * @param base  the name of the standard shape, when it has one (a receiver's standard shape is a
     *              {@code handgun}; a barrel's, the medium calibre): a part made in it records no variant, and
     *              the variant of that name in {@code variants} (which has no shape of its own) holds its
     *              traits and numbers. Empty when the standard shape is only the plain part.
     */
    public record Shaping(ResourceLocation item, List<String> shape, List<String> stock, List<Variant> variants, String base,
                          List<Material.Trait> traits) {
        static final Codec<Shaping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Shaping::item),
                Codec.STRING.listOf().fieldOf("shape").forGetter(Shaping::shape),
                Codec.STRING.listOf().fieldOf("stock").forGetter(Shaping::stock),
                Variant.CODEC.listOf().optionalFieldOf("variants", List.of()).forGetter(Shaping::variants),
                Codec.STRING.optionalFieldOf("base", "").forGetter(Shaping::base),
                Material.Trait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(Shaping::traits)
        ).apply(instance, Shaping::new));

        /** Every shape the part is laid in (a cell diagram and a cell count): the standard one first, then the variants that have a shape. */
        public List<Variant> laid() {
            List<Variant> out = new ArrayList<>();
            out.add(new Variant("", shape, 0f, 1f, "", 1f, 0f, 1f, "", Map.of()));
            variants.stream().filter(v -> !v.shape().isEmpty()).forEach(out::add);
            return out;
        }

        /** The variants a standard part is forged into by rhythm ({@code ForgeRhythm}): those with a pattern and no shape of their own. */
        public List<Variant> forged() {
            return variants.stream().filter(v -> v.shape().isEmpty() && !v.rhythm().isEmpty()).toList();
        }

        /** The variant called {@code name}; empty for the standard shape or a name the type does not have. */
        public Optional<Variant> variant(String name) {
            return variants.stream().filter(v -> v.name().equals(name)).findFirst();
        }

        public int cells() {
            int n = 0;
            for (String row : shape)
                for (char c : row.toCharArray())
                    if (c != ' ')
                        n++;
            return n;
        }
    }

    /**
     * Another shape a part is made in: the same item, a different layout of cells, and the geometry
     * that layout stands for (spec 3). The cells are the size, as everywhere (more cells, more mass);
     * the rest is what the shape does to the figures. Each variant has to be a trade, never a
     * strict upgrade -- a thin blade is lighter, reaches further and bites, and chips.
     * <p>The layout must be unique across every part of every type: the part recipe knows only a
     * shape, so two parts laid alike would be one recipe.
     *
     * @param name    lang key tail ({@code modular.feedback.variant.<name>}); empty is the standard shape.
     * @param shape   rows of the role's letter, as {@link Shaping#shape}.
     * @param reach   metres added to the part's distance from the hand.
     * @param section thickness against the standard: a thin edge bites more (square root) and a thick
     *                one resists chipping (linearly). Mass does not move with it: the cells hold the
     *                volume, and a drawn-out part is longer and thinner, not lighter.
     * @param strike  against the standard: how well this shape turns a blow into damage on a body (a
     *                rapier is a point and does little per swing; a machete hits hard). Geometry, as
     *                {@link ModularType#strike()} is; it does not touch what is dug.
     * @param centre  metres the part's centre of mass moves from the hand, as {@code reach} moves its
     *                tip. They differ because a blade can reach far with its mass kept near the
     *                hand (a rapier tapers to a point), and the swing slows with where the mass is,
     *                not where the tip is.
     * @param mass    the share of the stock kept: a blade ground thin loses the rest as swarf (not
     *                handed back yet), so a rapier is lighter than a machete from the same ingots.
     * @param trait   the signature behaviour of this archetype ({@link ModularTraits}), by id: how it
     *                fights, not what it measures. Empty for none.
     * @param params  numbers the trait reads, by name, so tuning it is data.
     * @param rhythm  for a variant forged rather than laid: the pattern played on the anvil, one
     *                character per step ({@code t} a tap, {@code h} a heavy blow, {@code .} a rest).
     *                A forged variant has no shape, since it is made from a standard part.
     */
    public record Variant(String name, List<String> shape, float reach, float section, String rhythm, float strike,
                          float centre, float mass, String trait, Map<String, Float> params) {
        static final Codec<Variant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Variant::name),
                Codec.STRING.listOf().optionalFieldOf("shape", List.of()).forGetter(Variant::shape),
                Codec.FLOAT.optionalFieldOf("reach", 0f).forGetter(Variant::reach),
                Codec.FLOAT.optionalFieldOf("section", 1f).forGetter(Variant::section),
                Codec.STRING.optionalFieldOf("rhythm", "").forGetter(Variant::rhythm),
                Codec.FLOAT.optionalFieldOf("strike", 1f).forGetter(Variant::strike),
                Codec.FLOAT.optionalFieldOf("centre", 0f).forGetter(Variant::centre),
                Codec.FLOAT.optionalFieldOf("mass", 1f).forGetter(Variant::mass),
                Codec.STRING.optionalFieldOf("trait", "").forGetter(Variant::trait),
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of()).forGetter(Variant::params)
        ).apply(instance, Variant::new));

        /** How many cells of stock its shape lays; a forged variant has none of its own. */
        public int cells() {
            int n = 0;
            for (String row : shape)
                for (char c : row.toCharArray())
                    if (c != ' ')
                        n++;
            return n;
        }
    }

    /** Whether {@code form} is one of {@code forms}, {@code *_planks} meaning any form ending so. */
    public static boolean fits(List<String> forms, String form) {
        for (String f : forms)
            if (f.startsWith("*") ? form.endsWith(f.substring(1)) : f.equals(form))
                return true;
        return false;
    }

    /** The letters of the striking roles in role order, or empty for a type with one fixed head. */
    public List<String> strikers() {
        return order().stream().filter(k -> roles.get(k).striker()).toList();
    }

    /** Whether it has an edge to grind: a melee head or a blade, as opposed to a bow, a shield or armour. */
    public boolean edged() {
        return switch (family) {
            case DIG, BLADE, MACE, SPEAR, SHEARS -> true;
            default -> false;
        };
    }

    /**
     * How many cells of stock each letter has in the first (canonical) pattern: a part's shape's
     * cells for a role that takes a part, since a part fills one cell of the pattern.
     */
    public Map<String, Integer> canonicalCounts() {
        return counts(0);
    }

    /** How many cells each letter has in pattern {@code index}: which parts it has, and how big. */
    private Map<String, Integer> counts(int index) {
        Map<String, Integer> out = letters(index);
        out.replaceAll((k, n) -> !roles.containsKey(k) ? n
                : roles.get(k).striker() ? ModularHeads.of(roles.get(k).forms().get(0)).map(ModularHeads::cells).orElse(n)
                : roles.get(k).part().map(Shaping::cells).orElse(n));
        return out;
    }

    /** How many times each letter is written in pattern {@code index}: for a shaped part, one cell of the pattern is one part. */
    public Map<String, Integer> letters(int index) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String row : patterns.get(index))
            for (char c : row.toCharArray())
                if (c != ' ')
                    out.merge(String.valueOf(c), 1, Integer::sum);
        return out;
    }

    /**
     * One thing the Tool Table accepts: which pattern it extends and how many items each role has --
     * a shaped part counts one, raw stock as many as the stack holds.
     */
    public record Assembly(int pattern, Map<String, Integer> counts) {}

    /**
     * The roles no pattern lays -- a tip, a binding: they are put on, not laid -- so each is offered
     * with and without. In the order of {@link #order}.
     */
    public List<String> extras() {
        java.util.Set<String> laid = new java.util.HashSet<>();
        for (int i = 0; i < patterns.size(); i++)
            laid.addAll(letters(i).keySet());
        return order().stream().filter(k -> !laid.contains(k) && !roles.get(k).after()).toList();
    }

    /** The letters assembly places, in order: {@link #order} without the {@code after} roles. */
    public List<String> placed() {
        return order().stream().filter(k -> !roles.get(k).after()).toList();
    }

    /** The letter of the role a minigame adds to a finished design, or null. */
    public String afterRole() {
        return order().stream().filter(k -> roles.get(k).after()).findFirst().orElse(null);
    }

    /**
     * The ways to make this type before any {@link #extras} are added, one per pattern: how many
     * items each role has. A shaped part is one item however many cells of the pattern it spans (the
     * crossbow's prod); raw stock is as many as the stack holds. A pattern is how many of each role
     * there are, never where they sit.
     */
    public List<Map<String, Integer>> forms() {
        return FORMS.computeIfAbsent(this, t -> t.makeForms());
    }

    /** Forms by type instance, by identity (a record's hash walks every field); a datapack reload leaves the old entries behind, a few kilobytes each. */
    private static final Map<ModularType, List<Map<String, Integer>>> FORMS =
            java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());
    private static final Map<ModularType, List<Map<String, Integer>>> BASES =
            java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    /**
     * One entry per pattern, with a ranged role at the least it takes (left out when that is none): the
     * sheet draws these and keeps each ranged role's size beside them, so a part with a count is one part,
     * not a form for every size. {@link #forms} is the same list with every size written out, for the
     * Tool Table's matching.
     */
    public List<Map<String, Integer>> baseForms() {
        return BASES.computeIfAbsent(this, t -> {
            List<Map<String, Integer>> out = new ArrayList<>();
            for (int i = 0; i < t.patterns.size(); i++) {
                Map<String, Integer> counts = t.letters(i);
                counts.replaceAll((k, n) -> t.roles.get(k) != null && (t.roles.get(k).part().isPresent() || t.roles.get(k).striker()) ? 1 : n);
                t.ranges.forEach((k, r) -> {
                    if (counts.containsKey(k) && r.get(0) == 0)
                        counts.remove(k);
                    else if (counts.containsKey(k))
                        counts.put(k, r.get(0));
                });
                out.add(counts);
            }
            return out;
        });
    }

    /** Whether the role is raw stock whose size the player chooses. */
    public boolean ranged(String role) {
        return ranges.containsKey(role);
    }

    /** The parts the sheet can add or take off: the {@link #extras}, and each ranged role that may be none. */
    public List<String> optionals() {
        List<String> extras = extras();
        return order().stream().filter(k -> extras.contains(k) || (ranges.containsKey(k) && ranges.get(k).get(0) == 0)).toList();
    }

    private List<Map<String, Integer>> makeForms() {
        List<Map<String, Integer>> out = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            Map<String, Integer> counts = letters(i);
            counts.replaceAll((k, n) -> roles.get(k) != null && (roles.get(k).part().isPresent() || roles.get(k).striker()) ? 1 : n);
            sized(counts, counts.keySet().stream().filter(ranges::containsKey).toList(), 0, out);
        }
        return out;
    }

    /** {@code base} once for every combination of sizes the ranged roles {@code ranged} can take, from index {@code k}. */
    private void sized(Map<String, Integer> base, List<String> ranged, int k, List<Map<String, Integer>> out) {
        if (k == ranged.size()) {
            out.add(base);
            return;
        }
        List<Integer> range = ranges.get(ranged.get(k));
        for (int n = range.get(0); n <= range.get(1); n++) {
            Map<String, Integer> copy = new LinkedHashMap<>(base);
            if (n == 0)
                copy.remove(ranged.get(k));
            else
                copy.put(ranged.get(k), n);
            sized(copy, ranged, k + 1, out);
        }
    }

    /**
     * The role-to-count maps the Tool Table makes this type from that have exactly {@code roles} roles: each form
     * with every way of choosing the {@link #extras} that bring it to that size, one item of each. Built on demand
     * for one size, never all together: a type with many optional parts (a gun's fittings) has two to the power of
     * its extras' worth of combinations.
     */
    public List<Assembly> assemblies(int roles) {
        List<Assembly> out = new ArrayList<>();
        List<Map<String, Integer>> forms = forms();
        for (int i = 0; i < forms.size(); i++)
            withExtras(i, forms.get(i), roles - forms.get(i).size(), out);
        return out;
    }

    /** The assemblies with at most {@code maxExtras} optional parts on, none up to that many. */
    public List<Assembly> assembliesUpTo(int maxExtras) {
        List<Assembly> out = new ArrayList<>();
        List<Map<String, Integer>> forms = forms();
        for (int i = 0; i < forms.size(); i++)
            for (int k = 0; k <= maxExtras; k++)
                withExtras(i, forms.get(i), k, out);
        return out;
    }

    /** Every way of putting exactly {@code k} of the extras on {@code form}. */
    private void withExtras(int pattern, Map<String, Integer> form, int k, List<Assembly> out) {
        List<String> extras = extras();
        if (k < 0 || k > extras.size())
            return;
        choose(extras, k, 0, new ArrayList<>(), chosen -> {
            Map<String, Integer> counts = new LinkedHashMap<>(form);
            chosen.forEach(r -> counts.put(r, roles.get(r).bulk()));
            out.add(new Assembly(pattern, counts));
        });
    }

    private static void choose(List<String> from, int k, int at, List<String> chosen, java.util.function.Consumer<List<String>> done) {
        if (chosen.size() == k) {
            done.accept(chosen);
            return;
        }
        for (int i = at; i <= from.size() - (k - chosen.size()); i++) {
            chosen.add(from.get(i));
            choose(from, k, i + 1, chosen, done);
            chosen.remove(chosen.size() - 1);
        }
    }

    /** The role letters by sprite layer, then letter: the same on both sides of the wire. */
    public List<String> order() {
        List<String> out = new ArrayList<>(roles.keySet());
        out.sort(java.util.Comparator.<String>comparingInt(k -> roles.get(k).layer()).thenComparing(k -> k));
        return out;
    }
}
