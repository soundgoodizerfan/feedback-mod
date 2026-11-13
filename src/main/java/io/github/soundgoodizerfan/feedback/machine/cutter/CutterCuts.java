// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cutter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import io.github.soundgoodizerfan.feedback.item.RecordedCut;
import io.github.soundgoodizerfan.feedback.modular.bench.Station;
import io.github.soundgoodizerfan.feedback.modular.bench.WoodCuts;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.process.Removal;
import io.github.soundgoodizerfan.feedback.process.RemovalTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * What a Power Cutter may legally cut an input into, and which of those it does. The machine holds no recipe: the legal cuts
 * are the physical ones (a saw does what the hand stonecutter's cuts are, a plasma head pierces and blanks conducting plate), the
 * choice is the data stick's if the input fits what it recorded, and otherwise a seeded pseudo-random pick from the world seed and the
 * input's properties (material, form, head), never its item id.
 */
final class CutterCuts {

    /** One cut: what comes out, how many of the input it spends, and spare stock handed back (a log's leftover planks). */
    record Cut(ItemStack result, int inputs, ItemStack spare) { }

    /** The seeded choice per (world seed, material, form, head): an index into the sorted legal list, computed once. */
    private record Key(long seed, String material, String form, CutterHeadItem.Type head) { }

    private static final Map<Key, Integer> CHOICES = new HashMap<>();
    private static int generation = -1;

    private CutterCuts() {
    }

    /** Whether the head may work this stock at all: a saw takes cold stock, a plasma head conducting metal plate. */
    static boolean fits(CutterHeadItem.Type head, Material.Located what) {
        Material m = what.material();
        return switch (head) {
            case SAW -> Station.STONECUTTING.accepts(m);
            case PLASMA -> what.form().equals("plate") && (m.ampacity().isPresent() || m.kind().filter(k -> k == Material.Kind.METAL).isPresent());
        };
    }

    /** Every cut this head could make on {@code in}, sorted by result id so the order does not depend on load order. */
    static List<Cut> legal(ServerLevel level, ItemStack in, Material.Located what, CutterHeadItem.Type head) {
        if (!fits(head, what))
            return List.of();
        List<Cut> out = new ArrayList<>();
        if (head == CutterHeadItem.Type.SAW) {
            for (WoodCuts.Cut c : WoodCuts.of(level, in)) {
                int inputs = c.inputs(in);
                int left = c.leftover(in);
                ItemStack spare = left > 0 ? WoodCuts.planksOf(level, in).map(p -> new ItemStack(p, left)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
                out.add(new Cut(c.result().copy(), inputs, spare));
            }
            for (var r : level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(in), level))
                out.add(new Cut(r.value().getResultItem(level.registryAccess()).copy(), 1, ItemStack.EMPTY));
        } else {
            for (Removal r : RemovalTable.get().entries())
                if ((r.operation() == Operation.TURN || r.operation() == Operation.DRILL) && r.input().test(in) && r.applies(in))
                    out.add(new Cut(r.finish(in), 1, ItemStack.EMPTY));
        }
        out.sort(Comparator.comparing((Cut c) -> BuiltInRegistries.ITEM.getKey(c.result().getItem()).toString())
                .thenComparingInt(c -> c.result().getCount()));
        return out;
    }

    /** The cut the stick says, if the input is of its stock class and one legal cut is the recorded item (else the same form of result). */
    static Optional<Cut> recorded(ServerLevel level, List<Cut> legal, Material.Located what, RecordedCut rec) {
        String kind = what.material().kind().map(Material.Kind::getSerializedName).orElse("");
        if (rec == null || !rec.kind().equals(kind) || !rec.form().equals(what.form()))
            return Optional.empty();
        for (Cut c : legal)
            if (BuiltInRegistries.ITEM.getKey(c.result().getItem()).toString().equals(rec.result()))
                return Optional.of(c);
        if (rec.resultForm().isEmpty())
            return Optional.empty();
        var materials = ClientMaterials.on(level);
        for (Cut c : legal)
            if (Material.find(materials, c.result()).map(l -> l.form().equals(rec.resultForm())).orElse(false))
                return Optional.of(c);
        return Optional.empty();
    }

    /** A pseudo-random legal cut, fixed by the world seed and the input's material, form and the head: the same input always gives the same cut here. */
    static Cut seeded(ServerLevel level, List<Cut> legal, Material.Located what, CutterHeadItem.Type head) {
        synchronized (CHOICES) {
            if (generation != MaterialTable.generation()) {
                CHOICES.clear();
                generation = MaterialTable.generation();
            }
            Key key = new Key(level.getSeed(), what.id().toString(), what.form(), head);
            int pick = CHOICES.computeIfAbsent(key, k -> new Random(k.seed() * 31 + k.material().hashCode() * 17L
                    + k.form().hashCode() * 7L + k.head().ordinal()).nextInt(legal.size()));
            return legal.get(Math.min(pick, legal.size() - 1));
        }
    }
}
