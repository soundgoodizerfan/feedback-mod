// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.level.block.state.BlockState;

/**
 * What a kind of head (a face of the hafted plan) is, read off the face definitions: the types with no patterns whose wearing part is the
 * head's own shaped part ({@code pickaxe_head}, {@code sword_blade}, {@code hammer} head...). A hafted tool carries one to three such
 * parts and has no dig table, strike figure or combat class of its own: each comes from the faces it has ({@link ModularFaces}), and
 * nothing is authored per combination (spec 2).
 *
 * @param mines    the block tags the head digs, each at a share of its full dig speed.
 * @param strike   how well the head's shape turns a blow into damage ({@link ModularType#strike()}).
 * @param cells    how many cells of stock the part is made of: its size.
 * @param shaping  how the part is made, with the variants it can be forged or laid in.
 * @param combat   the family a swing of this face is judged as: blade, mace, spear, dig or hand (a bench tool).
 * @param wearLitres litres of the face's own wearing stock, which sets how long it lasts.
 * @param actions  what it does to a block it is used on ({@link ModularType#actions()}).
 * @param school   the treatise a face of this kind needs to be assembled ({@code weapons:1}), or empty; any build holding one is complex.
 * @param trait    the signature behaviour a face of this kind gives when no part variant gives one ({@code spear_charge} for a spear's head), with its numbers.
 * @param role     the face definition's own wearing role: the litres, place on the haft and layer a face of this kind has.
 */
public record ModularHeads(List<ModularType.Mine> mines, float strike, int cells, ModularType.Shaping shaping,
                           ModularType.Family combat, float wearLitres, List<String> actions, ModularType.Role role, String school,
                           String trait, Map<String, Float> traitParams) {

    private static int generation = -1;
    private static Map<String, ModularHeads> cache = Map.of();
    private static Map<String, Map<String, ModularType.Axis>> axesCache = Map.of();

    /** The head kind {@code kind} names ({@code pickaxe_head}), if some face definition's wearing role is made of it. */
    public static Optional<ModularHeads> of(String kind) {
        if (generation != ModularTypes.generation()) {
            Map<String, ModularHeads> built = new HashMap<>();
            Map<String, Map<String, ModularType.Axis>> axes = new HashMap<>();
            for (ModularType type : ModularTypes.known().values()) {
                // A face definition ({@code "face": true}): never assembled or an item, a head kind as its wearing part.
                if (!type.face())
                    continue;
                ModularType.Role wear = type.roles().get(type.wearRole());
                if (wear == null || wear.striker() || wear.part().isEmpty())
                    continue;
                ModularType.Shaping shaping = wear.part().get();
                built.put(shaping.item().getPath(), new ModularHeads(type.mines(), type.strike(), (int) Math.ceil(shaping.cells()), shaping,
                        type.family(), type.wearLitres(), type.actions(), wear, type.school(), type.trait(), type.traitParams()));
                axes.put(shaping.item().getPath(), wear.axes());
            }
            cache = built;
            axesCache = axes;
            generation = ModularTypes.generation();
        }
        return Optional.ofNullable(cache.get(kind));
    }

    /** Every head kind there is, by the item path of its part. */
    public static Map<String, ModularHeads> all() {
        of("");
        return cache;
    }

    /** The axes a head of kind {@code kind} carries (the single-head type's own: head size, edge length); empty for a kind with none. */
    public static Map<String, ModularType.Axis> axesOf(String kind) {
        of(kind);
        return axesCache.getOrDefault(kind, Map.of());
    }

    /** The kind a part of a striking role is: what it records, else the role's first kind. */
    public static String kindOf(ModularDesign.Part part, ModularType.Role role) {
        return part.kind().isEmpty() ? role.forms().get(0) : part.kind();
    }

    /** Whether the head digs the block. */
    public boolean digs(BlockState state) {
        for (ModularType.Mine m : mines)
            if (state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, m.tag())))
                return true;
        return false;
    }
}
