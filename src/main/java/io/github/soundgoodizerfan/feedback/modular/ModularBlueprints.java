// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

/**
 * Plans as items. Knowing how to make a complex thing is an object: a plan sheet ({@link ModularBlueprintItem}) holding a design, or a
 * {@link BlueprintBookItem} holding sheets like a bundle. A bench that wants a plan reads every sheet in what lies in its plan slot, so
 * a book is only a tidier way to carry sheets; a plan in a book is the same sheet. A plan is never a flag on the player.
 */
public final class ModularBlueprints {

    private ModularBlueprints() {
    }

    /** The plan sheets in {@code stack}: itself if it is a sheet, its sheets if it is a book. */
    public static List<ItemStack> sheets(ItemStack stack) {
        List<ItemStack> out = new ArrayList<>();
        if (stack.isEmpty())
            return out;
        if (stack.getItem() instanceof ModularBlueprintItem) {
            if (ModularStacks.design(stack) != null)
                out.add(stack);
        } else if (stack.getItem() instanceof BlueprintBookItem) {
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null)
                for (ItemStack s : contents.items())
                    if (s.getItem() instanceof ModularBlueprintItem && ModularStacks.design(s) != null)
                        out.add(s);
        }
        return out;
    }

    /** The designs on the sheets in {@code stack}. */
    public static List<ModularDesign> designs(ItemStack stack) {
        return sheets(stack).stream().map(ModularStacks::design).toList();
    }

    /** Whether {@code stack} holds a plan of {@code type}: the reference a table needs to draw a type that must be learned. */
    public static boolean references(ItemStack stack, ResourceLocation type) {
        return designs(stack).stream().anyMatch(d -> d.type().equals(type));
    }

    /** A plan sheet of {@code design}. */
    public static ItemStack sheet(ModularDesign design, ModularFigures figures) {
        ItemStack sheet = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FModular.BLUEPRINT.get());
        sheet.set(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get(), design);
        if (figures != null)
            sheet.set(io.github.soundgoodizerfan.feedback.registry.FModular.FIGURES.get(), figures);
        return sheet;
    }

    /**
     * A plan sheet of a plain suit module with one random upgrade, drawn in common metals: what a chest holds. It is a sheet and nothing
     * else; the parts, and any circuit or cell the upgrade needs, are still to be made. Empty if the tables cannot draw it.
     */
    public static Optional<ItemStack> randomModulePlan(RandomSource random) {
        Map<ResourceLocation, ModularType> types = ModularTypes.get().types();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        ResourceLocation typeId = Feedback.id("suit_module");
        ModularType type = types.get(typeId);
        if (type == null)
            return Optional.empty();
        List<String> attachments = type.roles().entrySet().stream().filter(e -> e.getValue().name().startsWith("module_")).map(Map.Entry::getKey).toList();
        if (attachments.isEmpty())
            return Optional.empty();
        String pickedRole = attachments.get(random.nextInt(attachments.size()));
        List<PartBenchShaping.Option> options = ToolDesigns.shapes(type.roles().get(pickedRole), types).stream().filter(o -> !o.variant().name().isEmpty()).toList();
        if (options.isEmpty())
            return Optional.empty();
        PartBenchShaping.Option upgrade = options.get(random.nextInt(options.size()));
        List<ModularDesign.Part> parts = new ArrayList<>();
        for (String role : type.order()) {
            ModularType.Role r = type.roles().get(role);
            PartBenchShaping.Option shape = role.equals(pickedRole) ? upgrade : ToolDesigns.standard(r, types);
            if (!(role.equals(pickedRole) || role.equals("H") || role.equals("W")) || shape == null)
                continue;
            ResourceLocation metal = Feedback.id(role.equals("W") ? "copper" : "wrought_iron");
            Optional<Material.Located> made = ToolDesigns.located(r, shape, metal, materials);
            if (made.isEmpty())
                return Optional.empty();
            parts.add(ToolDesigns.part(role, made.get(), shape, 1));
        }
        ModularDesign design = new ModularDesign(typeId, parts, Edge.STANDARD);
        return ToolDesigns.predict(design, types, materials).map(f -> sheet(design, f));
    }

    /** Adds a module plan to a chest's loot; the chest tables and the chance are the modifier's conditions. */
    public static class Loot extends LootModifier {

        public static final MapCodec<Loot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, Loot::new));

        public Loot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            randomModulePlan(context.getRandom()).ifPresent(loot::add);
            return loot;
        }

        @Override
        public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
