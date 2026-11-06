package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.GearKind;
import io.github.soundgoodizerfan.feedback.combat.gear.GearRecipe;
import io.github.soundgoodizerfan.feedback.item.Tongs;
import io.github.soundgoodizerfan.feedback.machine.cable.CableItem;
import io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe;
import io.github.soundgoodizerfan.feedback.machine.xp.XpWireItem;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Crafting-tab displays for the code-matched recipes that name no item (tongs, combat gear, fence, XP wire, torch),
 * built from the loaded materials the way {@link ElectronicsCrafting} builds its examples. The recipes whose inputs
 * are a stateful item (magazine loading, engine and transmission blueprints, food salting, merging and jarring, hand
 * deformation) are registered as unhandled so JEI draws no empty card; each has an info page or its own category.
 */
final class CodeRecipeExamples {

    private CodeRecipeExamples() {
    }

    static void register(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        var crafting = registration.getCraftingCategory();
        var ext = new ElectronicsCrafting.Extension();
        crafting.addExtension(io.github.soundgoodizerfan.feedback.item.TongsRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(GearRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.machine.cable.FenceRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.machine.breaker.BreakerRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.machine.xp.XpWireRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.process.TorchRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.process.FoodSaltingRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.process.FoodMergeRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.process.JarFillRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
        crafting.addExtension(io.github.soundgoodizerfan.feedback.process.HandDeformationRecipe.class, new ElectronicsCrafting.ExtensionOf<>(ext));
    }

    private static RecipeHolder<CraftingRecipe> holder(String path, List<List<ItemStack>> inputs, int w, int h, List<ItemStack> outputs) {
        return new RecipeHolder<>(Feedback.id("jei/" + path), new ElectronicsCrafting.Example(inputs, w, h, outputs));
    }

    /** Stacks of {@code form} for every material that has one, with the ids alongside. */
    private static List<Map.Entry<ResourceLocation, ItemStack>> stocks(Map<ResourceLocation, Material> materials, String form) {
        List<Map.Entry<ResourceLocation, ItemStack>> out = new ArrayList<>();
        materials.forEach((id, m) -> m.stack(id, form).ifPresent(s -> out.add(Map.entry(id, s))));
        return out;
    }

    static List<RecipeHolder<CraftingRecipe>> examples(Map<ResourceLocation, Material> materials) {
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>();
        ItemStack hammer = new ItemStack(FModular.HAND_HAMMER.get());

        // Tongs: two rods, a grip and the hammer; the rods cycle, the grip is the first one loaded.
        var rods = stocks(materials, "rod");
        var grips = stocks(materials, "grip");
        if (!rods.isEmpty() && !grips.isEmpty()) {
            List<ItemStack> rodStacks = rods.stream().map(Map.Entry::getValue).toList();
            List<ItemStack> tongs = rods.stream().map(e -> {
                ItemStack t = new ItemStack(FItems.TONGS.get());
                t.set(FDataComponents.TONGS.get(), new Tongs(e.getKey(), grips.get(0).getKey()));
                return t;
            }).toList();
            out.add(holder("tongs", List.of(List.of(hammer), rodStacks, rodStacks, List.of(grips.get(0).getValue())), 3, 2, tongs));
        }

        // Combat gear: plates of one metal, straps of one leather; one card per piece.
        var plates = stocks(materials, "plate");
        var straps = stocks(materials, "leather");
        if (!plates.isEmpty() && !straps.isEmpty()) {
            var strap = straps.get(0);
            List<ItemStack> plateStacks = plates.stream().map(Map.Entry::getValue).toList();
            for (GearKind kind : GearKind.values()) {
                List<List<ItemStack>> in = new ArrayList<>();
                for (int i = 0; i < kind.plates(); i++)
                    in.add(plateStacks);
                for (int i = 0; i < kind.straps(); i++)
                    in.add(List.of(strap.getValue()));
                List<ItemStack> made = plates.stream().map(p -> GearRecipe.make(kind, p.getKey(), materials.get(p.getKey()),
                        strap.getKey(), materials.get(strap.getKey()))).toList();
                out.add(holder("gear_" + kind.name().toLowerCase(), in, 3, (in.size() + 2) / 3, made));
            }
        }

        // Fence: a cable over two rods in a column.
        List<ItemStack> cables = new ArrayList<>(), fences = new ArrayList<>();
        for (ItemStack wire : CableRecipe.WIRE.getItems().toList())
            Material.find(MaterialIngredient.materialsNow(), wire).ifPresent(found -> {
                ItemStack cable = CableItem.of(found.id(), 1);
                cables.add(cable);
                ItemStack fence = new ItemStack(FItems.FENCE.get(), 2);
                fence.set(FDataComponents.MADE_OF.get(), cable.get(FDataComponents.MADE_OF.get()));
                fences.add(fence);
            });
        if (!cables.isEmpty() && !rods.isEmpty()) {
            List<ItemStack> rodStacks = rods.stream().map(Map.Entry::getValue).toList();
            out.add(holder("fence", List.of(cables, rodStacks, rodStacks), 1, 3, fences));
        }

        // Cable gauges other than the standard one (the real recipe's card draws that): one card each, the conductor cycling.
        for (CableRecipe.Layout layout : CableRecipe.LAYOUTS) {
            if (layout.gauge() == io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.STANDARD)
                continue;
            List<ItemStack> conductorStacks = new ArrayList<>(), made = new ArrayList<>();
            for (ItemStack item : layout.conductor().getItems().toList())
                Material.find(MaterialIngredient.materialsNow(), item).ifPresent(found -> {
                    conductorStacks.add(item);
                    made.add(CableItem.of(found.id(), layout.gauge().yield(), null, layout.gauge()));
                });
            if (conductorStacks.isEmpty())
                continue;
            List<List<ItemStack>> in = new ArrayList<>();
            for (int i = 0; i < layout.width() * layout.height(); i++)
                in.add(conductorStacks);
            out.add(holder("cable_" + layout.gauge().getSerializedName(), in, layout.width(), layout.height(), made));
        }

        // Breaker from cable: the cable's gauge cycles, and the breaker takes it.
        var ironPlate = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Feedback.id("wrought_iron_plate"));
        var copper = Feedback.id("copper");
        if (materials.containsKey(copper)) {
            List<ItemStack> gaugedCables = new ArrayList<>(), breakers = new ArrayList<>();
            for (io.github.soundgoodizerfan.feedback.machine.cable.CableGauge gauge : io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.values()) {
                gaugedCables.add(CableItem.of(copper, 1, null, gauge));
                ItemStack breaker = new ItemStack(FItems.BREAKER.get());
                if (gauge != io.github.soundgoodizerfan.feedback.machine.cable.CableGauge.STANDARD)
                    breaker.set(FDataComponents.CABLE_GAUGE.get(), gauge);
                breakers.add(breaker);
            }
            List<ItemStack> ceramic = List.of(new ItemStack(FItems.CERAMIC.get()));
            out.add(holder("breaker_gauged", List.of(ceramic, List.of(new ItemStack(net.minecraft.world.item.Items.LEVER)), ceramic,
                    gaugedCables, List.of(new ItemStack(ironPlate)), gaugedCables, ceramic, ceramic, ceramic), 3, 3, breakers));
        }

        // XP wire: three of one XP conductor, in a row. Any form counts; the first form a material has stands for it.
        List<ItemStack> conductors = new ArrayList<>(), wires = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (m.xpStore().filter(c -> c.conductance() > 0f).isEmpty())
                return;
            for (String form : List.of("ingot", "plate", "gem", "dust", "rod"))
                if (m.stack(id, form).isPresent()) {
                    conductors.add(m.stack(id, form).get());
                    wires.add(XpWireItem.of(id));
                    wires.get(wires.size() - 1).setCount(3);
                    return;
                }
        });
        if (!conductors.isEmpty())
            out.add(holder("xp_wire", List.of(conductors, conductors, conductors), 3, 1, wires));

        // Torch: anything that burns (a material with a flame and a burn time, no chamber) over a stick.
        List<ItemStack> fuels = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (m.combustion().filter(c -> c.chamber().isEmpty() && c.flameTemperature().isPresent() && c.burnTicks().isPresent()).isEmpty())
                return;
            for (ResourceLocation item : m.forms().values()) {
                ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(item));
                if (!s.isEmpty() && s.getCraftingRemainingItem().isEmpty()) {
                    fuels.add(s);
                    return;
                }
            }
        });
        if (!fuels.isEmpty())
            out.add(holder("torch", List.of(fuels, List.of(new ItemStack(Items.STICK))), 1, 2, List.of(new ItemStack(Items.TORCH, 4))));

        // Bootstrap tools: vanilla's layouts, a leather binding, a modular tool out.
        for (var layout : io.github.soundgoodizerfan.feedback.modular.BootstrapToolRecipe.tools()) {
            ItemStack made = io.github.soundgoodizerfan.feedback.modular.BootstrapToolRecipe.tool(layout);
            if (made.isEmpty() || made.is(layout.getResultItem(null).getItem()))
                continue;
            List<List<ItemStack>> in = layout.getIngredients().stream()
                    .map(i -> i.isEmpty() ? (List<ItemStack>) null : List.of(i.getItems())).toList();
            out.add(holder("bootstrap_" + BuiltInRegistries.ITEM.getKey(layout.getResultItem(null).getItem()).getPath(),
                    in, layout.getWidth(), layout.getHeight(), List.of(made)));
        }
        return out;
    }
}
