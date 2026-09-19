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
package io.github.soundgoodizerfan.feedback.machine.valve;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.machine.bellows.Blown;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gate on a {@link Blown} line. No new mechanic -- {@link Switchable} and {@code Blown} already
 * exist for the Bellows/Firebox/Pressure Vessel chain, and a Valve is simply a block that sits
 * between two of them and answers both. Engaged state lives on the blockstate, the same choice
 * {@code DamperBlockEntity} already makes -- nothing here needs to be synced by hand.
 *
 * <h2>Air, not the switch, is what it moves</h2>
 * Open, it forwards whatever is blown into it to whatever {@code Blown} sits on its output face.
 * Closed, the air is simply lost -- the same "anything over the buffer is lost to the room" rule
 * {@link Blown#addAir} already states, applied one block earlier. Nothing here decides what the
 * air is for; that is entirely the far end's business, the same separation the Bellows keeps.
 */
public class ValveBlockEntity extends BlockEntity implements Blown, Switchable {

    public ValveBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.VALVE.get(), pos, state);
    }

    @Override
    public void addAir(float air) {
        if (level == null || !isEngaged())
            return;
        if (level.getBlockEntity(worldPosition.relative(getBlockState().getValue(ValveBlock.FACING)))
                instanceof Blown blown)
            blown.addAir(air);
    }

    @Override
    public boolean isEngaged() {
        return getBlockState().getValue(ValveBlock.OPEN);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || isEngaged() == engaged)
            return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ValveBlock.OPEN, engaged));
        level.playSound(null, worldPosition,
                engaged ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.4f, 1.0f);
    }
}
