// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What an attuned machine's last draw on a reservoir found, in words: it drew, the run was empty, the run stood too low. */
public class AttuneServerData implements IServerDataProvider<BlockAccessor> {

    public static final AttuneServerData INSTANCE = new AttuneServerData();

    static final String DRAW = "AttuneDraw";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        var be = accessor.getBlockEntity();
        if (be == null || be.getLevel() == null)
            return;
        int last = MachineKinds.lastDraw(be, be.getLevel().getGameTime());
        if (last >= 0)
            data.putInt(DRAW, last);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("attune");
    }
}
