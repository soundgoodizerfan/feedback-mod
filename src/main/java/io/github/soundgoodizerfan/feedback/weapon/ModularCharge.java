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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.ModularItem;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A charge made at the Tool Table from a casing and its filler, or designed first at the Blueprint
 * Table: a modular item whose parts are what is packed ({@code modular_type/charge.json}). Placed, it
 * becomes the demolition charge block carrying exactly that filler and casing, unlit.
 */
public class ModularCharge extends ModularItem {

    public ModularCharge(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos at = place.getClickedPos();
        BlockState state = FWeapons.DEMOLITION_CHARGE.get().defaultBlockState();
        if (!place.canPlace() || !state.canSurvive(level, at))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(at, state, 3);
            if (level.getBlockEntity(at) instanceof DemolitionChargeBlockEntity charge)
                charge.pack(context.getItemInHand());
            level.playSound(null, at, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
