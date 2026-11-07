// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.level.block.state.BlockState;

/**
 * What a kind of head does, read off the single-head types: the part {@code pickaxe_head} is what
 * the pickaxe's wearing role is made of, so it digs what the pickaxe digs and strikes as the
 * pickaxe strikes. A twin-headed tool carries two such parts and so has no dig table of its own;
 * any two heads make a tool, and nothing is authored per pair (spec 5.3).
 *
 * @param mines the block tags the head digs, each at a share of its full dig speed.
 * @param strike how well the head's shape turns a blow into damage ({@link ModularType#strike()}).
 * @param cells how many cells of stock the part is made of: its size.
 * @param shaping how the part is made, with the variants it can be forged or laid in.
 */
public record ModularHeads(List<ModularType.Mine> mines, float strike, int cells, ModularType.Shaping shaping) {

    private static int generation = -1;
    private static Map<String, ModularHeads> cache = Map.of();

    /** The head kind {@code kind} names ({@code pickaxe_head}), if some single-head type's wearing role is made of it. */
    public static Optional<ModularHeads> of(String kind) {
        if (generation != ModularTypes.generation()) {
            Map<String, ModularHeads> built = new HashMap<>();
            for (ModularType type : ModularTypes.known().values()) {
                ModularType.Role wear = type.roles().get(type.wearRole());
                if (wear == null || wear.striker() || wear.part().isEmpty() || type.mines().isEmpty())
                    continue;
                ModularType.Shaping shaping = wear.part().get();
                built.put(shaping.item().getPath(), new ModularHeads(type.mines(), type.strike(), shaping.cells(), shaping));
            }
            cache = built;
            generation = ModularTypes.generation();
        }
        return Optional.ofNullable(cache.get(kind));
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
