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
import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** The truth about a node on an XP network: the store it holds and the run it sits in, fetched for the one block looked at. */
public class XpServerData implements IServerDataProvider<BlockAccessor> {

    public static final XpServerData INSTANCE = new XpServerData();

    static final String XP = "Xp";
    static final String POINTS = "XpPoints";
    static final String PER_LEVEL = "XpPerLevel";
    static final String MAX = "XpMax";
    static final String RUN_LEVEL = "XpRunLevel";
    static final String RUN_POINTS = "XpRunPoints";
    static final String FLOW = "XpFlow";
    static final String LEAK = "XpLeak";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof XpNode node))
            return;
        data.putBoolean(XP, true);
        XpStore s = node.xpStore();
        if (s != null) {
            data.putInt(POINTS, s.points());
            data.putFloat(PER_LEVEL, s.perLevel());
            data.putInt(MAX, s.maxPoints());
        }
        XpNetwork net = node.network();
        if (net != null) {
            data.putFloat(RUN_LEVEL, net.level());
            data.putInt(RUN_POINTS, net.points());
            data.putInt(FLOW, net.flow());
            data.putInt(LEAK, net.lastLeak());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("xp");
    }
}
