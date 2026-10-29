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
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.WorkpieceHolder;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The condition of a worn block that holds no workpiece (a thruster, a gearbox), which
 * {@link WorkpieceComponent} does not cover; the client does not hold the figure, so it is sent.
 */
public class WearingServerData implements IServerDataProvider<BlockAccessor> {

    public static final WearingServerData INSTANCE = new WearingServerData();

    static final String CONDITION = "WornCondition";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof Wearing wearing && !(accessor.getBlockEntity() instanceof WorkpieceHolder))
            data.putFloat(CONDITION, wearing.getCondition());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("wearing");
    }
}
