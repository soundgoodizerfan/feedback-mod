// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * "Any item of some form, whose material's figure is in range": the recipe system's way to ask a
 * question about stuff (philosophy 11) instead of naming it. A cable recipe says wire with an
 * {@code ampacity} of at least so much and every metal that qualifies is accepted, including one a
 * pack adds tomorrow.
 *
 * <pre>
 * { "type": "feedback:material", "form": "wire", "property": "ampacity", "min": 0.5 }
 * </pre>
 *
 * Either {@code form} (the item is that form of some material) or {@code tag} (a tag of items, each
 * of which must be some material's) names the stuff; {@code property} with {@code min} and/or
 * {@code max} bounds it. With no property it is "any material's wire".
 *
 * <h2>Form-agnostic: {@code material} and {@code forms}</h2>
 * A thermal row asks "this material, in any of these forms" with {@code material} (an id; absent is any material of the tag) and
 * {@code forms} (a list; absent or empty is every form the material has). A row with such an input is <b>form-agnostic</b>
 * ({@link #agnostic}): its authored hold is for the reference form (grain 1, the ingot) and the form actually held scales it
 * ({@code ThermalProcess#requiredFor}). Dust is never listed as an ingredient of its own; it is one of the forms that satisfies the row.
 * <pre>
 * { "type": "feedback:material", "material": "feedback:cast_iron", "forms": ["ingot", "scrap", "swarf", "dust"] }
 * </pre>
 *
 * <h2>Which table it reads</h2>
 * The server's material table where one is loaded, else the client's copy ({@link ClientMaterials}).
 * Both exist on an integrated server and hold the same data. {@link #getItems} is cached by the
 * {@code Ingredient} that wraps this, so a client that asked before the table arrived would see it
 * empty for good. It cannot: the material sync is sent on {@code OnDatapackSyncEvent}, which
 * {@code PlayerList} posts just ahead of the recipe packet, on a login and on a {@code /reload}.
 */
public record MaterialIngredient(Optional<String> form, Optional<TagKey<Item>> tag,
                                 Optional<MaterialProperty> property, float min, float max,
                                 Optional<ResourceLocation> material, List<String> forms) implements ICustomIngredient {

    public MaterialIngredient(Optional<String> form, Optional<TagKey<Item>> tag, Optional<MaterialProperty> property, float min, float max) {
        this(form, tag, property, min, max, Optional.empty(), List.of());
    }

    public static final MapCodec<MaterialIngredient> CODEC = RecordCodecBuilder.<MaterialIngredient>mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("form").forGetter(MaterialIngredient::form),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(MaterialIngredient::tag),
            MaterialProperty.CODEC.optionalFieldOf("property").forGetter(MaterialIngredient::property),
            Codec.FLOAT.optionalFieldOf("min", Float.NEGATIVE_INFINITY).forGetter(MaterialIngredient::min),
            Codec.FLOAT.optionalFieldOf("max", Float.POSITIVE_INFINITY).forGetter(MaterialIngredient::max),
            ResourceLocation.CODEC.optionalFieldOf("material").forGetter(MaterialIngredient::material),
            Codec.STRING.listOf().optionalFieldOf("forms", List.of()).forGetter(MaterialIngredient::forms)
    ).apply(i, MaterialIngredient::new)).flatXmap(MaterialIngredient::checked, MaterialIngredient::checked);

    public static final IngredientType<MaterialIngredient> TYPE = new IngredientType<>(CODEC);

    private static DataResult<MaterialIngredient> checked(MaterialIngredient in) {
        if (in.form.isEmpty() && in.tag.isEmpty() && in.material.isEmpty())
            return DataResult.error(() -> "a material ingredient names a form, a tag or a material");
        if ((in.min != Float.NEGATIVE_INFINITY || in.max != Float.POSITIVE_INFINITY) && in.property.isEmpty())
            return DataResult.error(() -> "min and max need a property");
        return DataResult.success(in);
    }

    /** Any item of {@code form} whose material's {@code property} is at least {@code min}. */
    public static MaterialIngredient atLeast(String form, MaterialProperty property, float min) {
        return new MaterialIngredient(Optional.of(form), Optional.empty(), Optional.of(property), min, Float.POSITIVE_INFINITY);
    }

    private static final Map<Item, Optional<Material.Located>> LOCATED = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile Map<ResourceLocation, Material> locatedFor = Map.of();

    /**
     * What {@link Material#find} says of an item, memoised: a vessel matches its contents against every row every tick, and the
     * search walks every material. Dropped when the material table is replaced (a reload or a sync).
     */
    public static Optional<Material.Located> locate(Item item) {
        Map<ResourceLocation, Material> now = materialsNow();
        if (now != locatedFor) {
            LOCATED.clear();
            locatedFor = now;
        }
        return LOCATED.computeIfAbsent(item, i -> Material.find(now, i));
    }

    /** Whether this asks for a material in a list of forms rather than one form: a row with such an input is form-agnostic. */
    public boolean agnostic() {
        return material.isPresent() || !forms.isEmpty();
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (tag.isPresent() && !stack.is(tag.get()))
            return false;
        // A shaped part is one item for every material and is told by its MadeOf, so it is located from the stack.
        if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem)
            return Material.find(materialsNow(), stack).filter(this::accepts).isPresent();
        return locate(stack.getItem()).filter(this::accepts).isPresent();
    }

    /** Whether a located item is what this asks for: the form, and the figure in range. */
    public boolean accepts(Material.Located located) {
        if (form.isPresent() && !form.get().equals(located.form()))
            return false;
        if (!forms.isEmpty() && !forms.contains(located.form()))
            return false;
        if (material.isPresent() && !material.get().equals(located.id()))
            return false;
        if (property.isEmpty())
            return true;
        Optional<Float> figure = property.get().of(located.material());
        return figure.isPresent() && figure.get() >= min && figure.get() <= max;
    }

    @Override
    public Stream<ItemStack> getItems() {
        Map<ResourceLocation, Material> materials = materialsNow();
        Stream<ItemStack> stacks;
        if (form.isEmpty() && material.isPresent()) {
            ResourceLocation id = material.get();
            Material own = materials.get(id);
            stacks = own == null ? Stream.empty() : own.items(id).keySet().stream()
                    .filter(f -> accepts(new Material.Located(id, own, f)))
                    .flatMap(f -> own.stack(id, f).stream());
        } else if (form.isPresent()) {
            stacks = materials.entrySet().stream().flatMap(e -> e.getValue().stack(e.getKey(), form.get()).stream()
                    .filter(s -> accepts(new Material.Located(e.getKey(), e.getValue(), form.get()))));
        } else {
            stacks = BuiltInRegistries.ITEM.getTag(tag.get()).stream().flatMap(set -> set.stream())
                    .map(h -> new ItemStack(h.value()))
                    .filter(s -> Material.find(materials, s).filter(this::accepts).isPresent());
        }
        if (tag.isPresent() && form.isPresent())
            stacks = stacks.filter(s -> s.is(tag.get()));
        return stacks;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return FRecipes.MATERIAL_INGREDIENT.get();
    }

    /** What a recipe browser says about it, from the same fields the test reads. */
    public List<Component> describe() {
        String what = form.map(f -> "any " + f.replace('_', ' ')).orElse("any item of the tag");
        if (property.isEmpty())
            return List.of(Component.literal(what));
        String label = property.get().label();
        String range = min != Float.NEGATIVE_INFINITY && max != Float.POSITIVE_INFINITY
                ? label + " from " + num(min) + " to " + num(max)
                : min != Float.NEGATIVE_INFINITY ? label + " at least " + num(min) : label + " at most " + num(max);
        return List.of(Component.literal(what + ", " + range));
    }

    private static String num(float v) {
        return v == Math.rint(v) ? Integer.toString((int) v) : Float.toString(v);
    }

    /** The material table as this side knows it: the server's where one is loaded, else the client's copy. */
    public static Map<ResourceLocation, Material> materialsNow() {
        return MaterialTable.generation() > 0 ? MaterialTable.get().materials() : ClientMaterials.get();
    }
}
