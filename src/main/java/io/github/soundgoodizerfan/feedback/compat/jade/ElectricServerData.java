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
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Sends an electrical node's reading to the client for the HUD, on demand and only for the block
 * being looked at -- the same reason {@link RotationServerData} does. The reading is not synced
 * to clients by the block entity, so this is the only way it arrives, and a server without Jade
 * sends nothing and the HUD says nothing rather than guessing.
 */
public class ElectricServerData implements IServerDataProvider<BlockAccessor> {

    public static final ElectricServerData INSTANCE = new ElectricServerData();

    static final String ROLE = "ElectricRole";
    static final String VOLTS = "ElectricVolts";
    static final String AMPS = "ElectricAmps";
    static final String WANTED = "ElectricWanted";
    static final String BLOCKED = "ElectricBlocked";
    static final String INSULATION = "WireInsulation";
    static final String JAR = "JarState", JAR_VOLTS = "JarVolts";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ElectricNode node))
            return;
        data.putInt(ROLE, node.role().ordinal());
        data.putFloat(VOLTS, node.getVolts());
        data.putFloat(AMPS, node.getAmps());
        data.putFloat(WANTED, node.getWantedAmps());
        data.putBoolean(BLOCKED, node.isBlocked());
        if (node instanceof CableBlockEntity wire && wire.getInsulation() != null)
            data.putByte(INSULATION, (byte) wire.getInsulation().ordinal());
        if (node instanceof LeydenJarBlockEntity jar) {
            data.putFloat(JAR_VOLTS, jar.getStoredVolts());
            data.putByte(JAR, (byte) (jar.isSupplying() ? 2 : jar.getAmps() > 0f ? 1 : 0));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("electric");
    }
}
