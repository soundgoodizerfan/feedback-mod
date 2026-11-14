// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * What share of a pulse a struck block keeps when a coherence lens's beam bounces off it. A block made of one material (a copper block, a
 * gold block) reads that material's own {@code separation.reflectivity}, the optical sorter's figure; a mixed block (stone, deepslate, dirt,
 * wood) has no one material, so it takes a defined figure by what it is made of, read from tags and never from an id. Dirt keeps almost
 * nothing, so a wall of it ends the beam; tinted glass is a poor window and a fair mirror.
 */
public final class BlockReflectivity {

    private BlockReflectivity() {
    }

    /** The share, 0 to 1, of a pulse the block at {@code pos} keeps. */
    public static float of(BlockGetter level, BlockPos pos, BlockState state) {
        var found = Material.find(MaterialTable.get().materials(), state.getBlock().asItem());
        if (found.isPresent() && found.get().material().separation().reflectivity().isPresent())
            return found.get().material().separation().reflectivity().get();
        if (state.is(Tags.Blocks.GLASS_BLOCKS_TINTED))
            return ModularTuning.BLOCK_REFLECT_TINTED;
        if (state.is(BlockTags.ICE))
            return ModularTuning.BLOCK_REFLECT_ICE;
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS))
            return ModularTuning.BLOCK_REFLECT_WOOL;
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.SNOW) || state.is(Tags.Blocks.GRAVELS))
            return ModularTuning.BLOCK_REFLECT_DIRT;
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS))
            return ModularTuning.BLOCK_REFLECT_WOOD;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Tags.Blocks.STONES)
                || state.is(Tags.Blocks.COBBLESTONES) || state.is(Tags.Blocks.ORES))
            return ModularTuning.BLOCK_REFLECT_STONE;
        return ModularTuning.BLOCK_REFLECT_OTHER;
    }
}
