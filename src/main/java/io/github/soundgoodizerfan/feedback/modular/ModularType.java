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
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * One kind of modular item as data ({@code data/feedback/modular_type/<id>.json}): which roles it
 * has, what each accepts, the patterns that assemble it, and the numbers of its geometry. A new
 * type is a file and a lang line plus one registry line -- an item has to exist before any
 * datapack does. Tinkers' tool definitions and Silent Gear's gear types are the precedent; a
 * {@code parent} lets a type inherit another's file (see {@link ModularTypes}).
 *
 * <h2>The pattern is the type</h2>
 * Types are told apart by the shape of the recipe, as vanilla's are: a pickaxe is three heads over
 * two handles, an axe is not. A role's cells are its size -- three ingots make a bigger head than
 * one -- and the count of handle cells is the handle's length, so a longer handle is a different
 * pattern, not a slider.
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
 * @param blueprint   whether assembling one takes a blueprint of it in the grid (spec 1: complicated
 *                    items are designed on paper first; simple ones are their parts).
 */
public record ModularType(Family family, Slot slot, List<Mine> mines, float strike, float wearLitres,
                          String wearRole, String primary, Map<String, Role> roles,
                          List<List<String>> patterns, Map<String, ResourceLocation> defaults,
                          Map<String, String> vanilla, float armourMax, boolean blueprint) {

    /** Which physics bakes it. */
    public enum Family implements StringRepresentable {
        DIG, BLADE, MACE, SPEAR, BOW, CROSSBOW, SHIELD, ARMOUR, SHEARS;

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
     */
    public record Role(String name, List<String> forms, List<Material.Trait> traits, float litres, float length,
                       float at, boolean rides, Map<String, Float> factor, int layer, Optional<Shaping> part) {
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
                Shaping.CODEC.optionalFieldOf("part").forGetter(Role::part)
        ).apply(instance, Role::new));

        public boolean rod() {
            return length > 0;
        }

        public boolean accepts(Material.Located l) {
            return fits(forms, l.form()) && traits.stream().allMatch(l.material().traits()::contains);
        }

        /** The raw stock the role is made of: the part's, if it takes a part, else what it takes. */
        public List<String> stockForms() {
            return part.map(Shaping::stock).orElse(forms);
        }

        /** Whether {@code l} is the stock this role is made of -- what mends it. */
        public boolean acceptsStock(Material.Located l) {
            return fits(stockForms(), l.form()) && traits.stream().allMatch(l.material().traits()::contains);
        }

        public float factor(String form) {
            return factor.getOrDefault(form, 1f);
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
            Codec.BOOL.optionalFieldOf("blueprint", false).forGetter(ModularType::blueprint)
    ).apply(instance, ModularType::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ModularType> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /**
     * How a part is shaped from stock (step 1 of the parts pass): the stock laid in {@code shape}
     * with a hand tool anywhere else in the grid ({@link ModularPartRecipe}). One registered item
     * per part, the material on the stack ({@link io.github.soundgoodizerfan.feedback.process.MadeOf}).
     *
     * @param item  the part's item.
     * @param shape rows of the role's letter, a space for nothing; mirrored freely. Its cells are
     *              the part's size, as a pattern's were.
     * @param stock forms of a material it can be shaped from, as a role's {@code forms}.
     */
    public record Shaping(ResourceLocation item, List<String> shape, List<String> stock) {
        static final Codec<Shaping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Shaping::item),
                Codec.STRING.listOf().fieldOf("shape").forGetter(Shaping::shape),
                Codec.STRING.listOf().fieldOf("stock").forGetter(Shaping::stock)
        ).apply(instance, Shaping::new));

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
    static boolean fits(List<String> forms, String form) {
        for (String f : forms)
            if (f.startsWith("*") ? form.endsWith(f.substring(1)) : f.equals(form))
                return true;
        return false;
    }

    /** A part found in a grid: what it is made of, how many cells it fills, and the first stack. */
    public record Found(Material.Located what, int count, ItemStack stack) {}

    /**
     * Which letters a grid fills, if it is exactly one of this type's patterns: every cell of a
     * letter the same material and form, a form the role takes, and nothing else in the grid.
     */
    public Optional<Map<String, Found>> match(CraftingInput input, Function<ItemStack, Optional<Material.Located>> locate) {
        for (List<String> pattern : patterns)
            for (boolean mirrored : new boolean[] {false, true}) {
                List<String> rows = mirrored ? pattern.stream().map(r -> new StringBuilder(r).reverse().toString()).toList() : pattern;
                int h = rows.size();
                int w = rows.stream().mapToInt(String::length).max().orElse(0);
                for (int oy = 0; oy + h <= input.height(); oy++)
                    for (int ox = 0; ox + w <= input.width(); ox++) {
                        Optional<Map<String, Found>> hit = place(input, rows, ox, oy, locate);
                        if (hit.isPresent())
                            return hit;
                    }
            }
        return Optional.empty();
    }

    private Optional<Map<String, Found>> place(CraftingInput input, List<String> rows, int ox, int oy,
                                               Function<ItemStack, Optional<Material.Located>> locate) {
        Map<String, Found> found = new LinkedHashMap<>();
        for (int y = 0; y < input.height(); y++)
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                int py = y - oy;
                int px = x - ox;
                char c = py >= 0 && py < rows.size() && px >= 0 && px < rows.get(py).length() ? rows.get(py).charAt(px) : ' ';
                if (c == ' ') {
                    if (!stack.isEmpty())
                        return Optional.empty();
                    continue;
                }
                Role role = roles.get(String.valueOf(c));
                Optional<Material.Located> what = stack.isEmpty() ? Optional.empty() : locate.apply(stack);
                if (role == null || what.isEmpty() || !role.accepts(what.get()))
                    return Optional.empty();
                Found earlier = found.get(String.valueOf(c));
                if (earlier != null && (!earlier.what().id().equals(what.get().id()) || !earlier.what().form().equals(what.get().form())))
                    return Optional.empty();
                found.put(String.valueOf(c), new Found(what.get(), earlier == null ? 1 : earlier.count() + 1,
                        earlier == null ? stack : earlier.stack()));
            }
        return found.isEmpty() ? Optional.empty() : Optional.of(found);
    }

    /**
     * How many cells of stock each letter has in the first (canonical) pattern: a part's shape's
     * cells for a role that takes a part, since a part fills one cell of the pattern.
     */
    public Map<String, Integer> canonicalCounts() {
        return counts(0);
    }

    /** How many cells each letter has in pattern {@code index}: which parts it has, and how big. */
    public Map<String, Integer> counts(int index) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String row : patterns.get(index))
            for (char c : row.toCharArray())
                if (c != ' ')
                    out.merge(String.valueOf(c), 1, Integer::sum);
        out.replaceAll((k, n) -> roles.containsKey(k) ? roles.get(k).part().map(Shaping::cells).orElse(n) : n);
        return out;
    }

    /** The role letters by sprite layer, then letter: the same on both sides of the wire. */
    public List<String> order() {
        List<String> out = new ArrayList<>(roles.keySet());
        out.sort(java.util.Comparator.<String>comparingInt(k -> roles.get(k).layer()).thenComparing(k -> k));
        return out;
    }
}
