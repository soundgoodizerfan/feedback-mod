// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.item.DataStickItem;
import io.github.soundgoodizerfan.feedback.item.RecordedAssembly;
import io.github.soundgoodizerfan.feedback.modular.Treatises;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Writes a hand assembly onto a recipe data stick: the Crafting tab calls {@link #snapshot} before a craft is taken and {@link #record}
 * after, so the tools that wore and the fasteners that were spent are read off what the craft did rather than guessed from the recipe.
 * Also the one definition of a stock class, shared with the Auto Engineer that matches the stick against what it is fed.
 */
public final class AssemblyRecording {

    /** The grid, tool damage and fastener count before a craft. */
    public record Snapshot(List<ItemStack> grid, List<ItemStack> tools, int[] damage, int fasteners, int school) {
    }

    private AssemblyRecording() {
    }

    /** The class of stock {@code stack} is: {@code kind/form} for a material's item, {@code item:<id>} for anything else, empty for nothing. */
    public static String stockClass(Map<ResourceLocation, Material> materials, ItemStack stack) {
        if (stack.isEmpty())
            return "";
        Optional<Material.Located> found = Material.find(materials, stack);
        if (found.isPresent())
            return found.get().material().kind().map(Material.Kind::getSerializedName).orElse("") + "/" + found.get().form();
        return "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static Snapshot snapshot(EngineeringInput input) {
        List<ItemStack> grid = new ArrayList<>();
        for (ItemStack s : input.grid())
            grid.add(s.copy());
        List<ItemStack> tools = new ArrayList<>(input.tools());
        int[] damage = new int[tools.size()];
        for (int i = 0; i < damage.length; i++)
            damage[i] = tools.get(i).getDamageValue();
        int fasteners = 0;
        for (ItemStack f : input.fasteners())
            fasteners += f.getCount();
        return new Snapshot(grid, tools, damage, fasteners, input.known() | Treatises.mask(input.binder()));
    }

    /**
     * Records the craft just taken if the player holds a data stick in the off hand. {@code after} is the same input once the craft ran:
     * a tool whose damage rose is a tool the work used, and the fasteners missing are the ones it spent.
     */
    public static void record(Player player, Level level, Snapshot before, EngineeringInput after, ItemStack result) {
        if (level.isClientSide || result.isEmpty() || DataStickItem.offHandStick(player).isEmpty())
            return;
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        List<String> grid = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (ItemStack s : before.grid()) {
            grid.add(stockClass(materials, s));
            counts.add(s.getCount());
        }
        List<String> tools = new ArrayList<>();
        for (int i = 0; i < before.tools().size(); i++) {
            ItemStack t = before.tools().get(i);
            if (t.isEmpty() || t.getDamageValue() <= before.damage()[i])
                continue;
            for (ItemAbility a : List.of(ToolAbilities.HAMMER, ToolAbilities.SAW, ToolAbilities.FILE, ToolAbilities.DRIVER))
                if (ToolAbilities.can(t, a) && !tools.contains(a.name())) {
                    tools.add(a.name());
                    break;
                }
        }
        int left = 0;
        for (ItemStack f : after.fasteners())
            left += f.getCount();
        DataStickItem.recordAssembly(player, new RecordedAssembly(RecordedAssembly.STATION,
                BuiltInRegistries.ITEM.getKey(result.getItem()).toString(), grid, counts, tools, Math.max(0, before.fasteners() - left), before.school()));
    }
}
