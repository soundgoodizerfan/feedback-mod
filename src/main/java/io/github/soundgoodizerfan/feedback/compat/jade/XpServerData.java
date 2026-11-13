// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
    static final String STAGE = "CultureStage";
    static final String ORGAN = "CultureOrgan";
    static final String MASS = "CultureMass";
    static final String AIR = "CultureAir";

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
        if (accessor.getBlockEntity() instanceof io.github.soundgoodizerfan.feedback.machine.sculk.CultureDishBlockEntity dish) {
            data.putInt(STAGE, dish.culture().stage().ordinal());
            data.putFloat(MASS, dish.culture().mass());
            data.putInt(ORGAN, dish.culture().organState());
            data.putInt(AIR, dish.airMb());
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
