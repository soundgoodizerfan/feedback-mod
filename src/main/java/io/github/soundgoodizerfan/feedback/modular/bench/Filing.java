// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Filing a made part down at the Part Bench: a file takes one grain off one of the part's axes, and the metal that came off
 * is swarf (the swarf form of the part's own material, if it has one). A part can only be filed down, never up: one already at
 * its axis's minimum is remelted instead. Pure; the bench decides who holds the file and what it wears.
 *
 * <p>Volume is conserved: what the axes lose is exactly the swarf that falls (in whole items of
 * {@link io.github.soundgoodizerfan.feedback.registry.FormItems#volume}, the remainder kept on the part as {@code filed_mb}), and the
 * crucible takes its loss when the swarf is melted ({@link ModularTuning#SWARF_REMELT_LOSS}).
 */
public final class Filing {

    private Filing() {
    }

    /** State key: swarf volume (mB) filed off but not yet a whole swarf item. */
    public static final String FILED_MB = "filed_mb";
    /** State key: strokes the part has had, so a file wears one point per {@link ModularTuning#FILE_STROKES_PER_WEAR}. */
    public static final String STROKES = "strokes";

    /** What one stroke did: the filed part, the swarf it shed, whether the file wears a point, or why nothing happened. */
    public record Stroke(ItemStack part, ItemStack swarf, boolean wear, String refusal) {
        public boolean ok() {
            return refusal.isEmpty();
        }
    }

    /** The role a made part is the part of, in any type. */
    public static Optional<ModularType.Role> roleOf(ItemStack part, Map<ResourceLocation, ModularType> types) {
        if (!(part.getItem() instanceof ModularPartItem) || !part.has(FDataComponents.MADE_OF.get()))
            return Optional.empty();
        ResourceLocation item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(part.getItem());
        for (ModularType type : types.values())
            for (ModularType.Role role : type.roles().values())
                if (!role.axes().isEmpty() && role.part().filter(s -> s.item().equals(item)).isPresent())
                    return Optional.of(role);
        return Optional.empty();
    }

    /** The axes of {@code part} a file can work, in the order the bench indexes them; empty for a part that declares none. */
    public static List<Map.Entry<String, ModularType.Axis>> axes(ItemStack part, Map<ResourceLocation, ModularType> types) {
        MadeOf made = part.get(FDataComponents.MADE_OF.get());
        if (made == null || made.measures().axes().isEmpty())
            return List.of();
        return roleOf(part, types).map(r -> r.sortedAxes(made.variant())).orElse(List.of());
    }

    /** The as-built value of an axis of {@code part}. */
    public static float value(ItemStack part, Map.Entry<String, ModularType.Axis> axis) {
        MadeOf made = part.get(FDataComponents.MADE_OF.get());
        return made == null ? axis.getValue().def() : made.measures().axis(axis.getKey(), axis.getValue().def());
    }

    /** The volume of stock {@code part} holds, in mB: its cells of stock scaled by its volume-scaling axes. */
    public static float volumeMb(ItemStack part, Map<ResourceLocation, ModularType> types) {
        MadeOf made = part.get(FDataComponents.MADE_OF.get());
        if (made == null)
            return 0f;
        float scale = roleOf(part, types).map(r -> r.volumeScaleOf(made.measures().axes(), made.variant())).orElse(1f);
        return made.cells() * ModularTuning.CELL_MB * scale;
    }

    /** State key: tool marks a coarse file left on the part, 0 smooth to 1 gouged; a smooth file burnishes them away, and enough of them is a flaw ({@link ModularTuning#FILE_MARKS_FLAW_AT}). */
    public static final String MARKS = "marks";
    /** State key: a grain of cut a slow file has built up but not yet taken. */
    public static final String CUT = "cut";

    /**
     * One stroke of {@code file} on axis number {@code index} of the single part in {@code part}. The file's cut sets how many grains a stroke
     * takes (a bastard two, a smooth file half a grain a stroke) and what it leaves behind or polishes off ({@link #MARKS}); a longer file takes
     * more per stroke and wanders more.
     */
    public static Stroke stroke(ItemStack part, int index, Map<ResourceLocation, ModularType> types,
                                Map<ResourceLocation, Material> materials, ItemStack file) {
        List<Map.Entry<String, ModularType.Axis>> axes = axes(part, types);
        MadeOf made = part.get(FDataComponents.MADE_OF.get());
        if (made == null || index < 0 || index >= axes.size())
            return refuse(part, "nothing");
        if (part.getCount() != 1)
            return refuse(part, "one_part");
        Map.Entry<String, ModularType.Axis> axis = axes.get(index);
        float now = value(part, axis);
        if (now <= axis.getValue().min() + 1e-4f)
            return refuse(part, "at_minimum");
        ModularType.Role role = roleOf(part, types).orElseThrow();
        io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities.Face cut =
                io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities.face(file, "file_head");
        float reach = cut == null ? 1f : cut.ratio("length");
        float grains = (cut == null ? 1f : cut.param("grains", 1f)) * reach;
        float marksNow = made.measures().state(MARKS, 0f);
        float marks = Math.max(0f, Math.min(1f, marksNow + (cut == null ? 0.15f : cut.param("marks", 0.15f) * (cut != null && cut.param("marks", 0f) > 0f ? reach : 1f))));
        float owed = made.measures().state(CUT, 0f) + grains;
        int whole = (int) owed;
        float next = Math.max(axis.getValue().min(), now - whole * axis.getValue().grain());
        Map<String, Float> was = new HashMap<>(made.measures().axes());
        Map<String, Float> to = new HashMap<>(was);
        to.put(axis.getKey(), next);
        float removed = made.cells() * ModularTuning.CELL_MB * Math.max(0f, role.volumeScaleOf(was, made.variant()) - role.volumeScaleOf(to, made.variant()));
        Optional<net.minecraft.world.item.Item> swarf = made.cells() > 0 ? materials.get(made.material()) == null ? Optional.empty()
                : materials.get(made.material()).item(made.material(), "swarf") : Optional.empty();
        float held = made.measures().state(FILED_MB, 0f) + (swarf.isPresent() ? removed : 0f);
        int each = io.github.soundgoodizerfan.feedback.registry.FormItems.volume("swarf");
        int items = each > 0 && swarf.isPresent() ? (int) (held / each) : 0;
        if (items > 0)
            held -= items * each;
        float strokes = made.measures().state(STROKES, 0f) + 1f;
        boolean wear = strokes % ModularTuning.FILE_STROKES_PER_WEAR == 0f;
        io.github.soundgoodizerfan.feedback.process.Measures m = made.measures().withAxes(to)
                .withState(FILED_MB, swarf.isPresent() ? held : 0f).withState(STROKES, strokes)
                .withState(MARKS, marks).withState(CUT, owed - whole);
        ItemStack out = part.copy();
        out.set(FDataComponents.MADE_OF.get(), made.withMeasures(m));
        return new Stroke(out, items > 0 ? new ItemStack(swarf.get(), items) : ItemStack.EMPTY, wear, "");
    }

    private static Stroke refuse(ItemStack part, String why) {
        return new Stroke(part, ItemStack.EMPTY, false, why);
    }
}
