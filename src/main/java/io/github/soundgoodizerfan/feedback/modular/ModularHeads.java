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
