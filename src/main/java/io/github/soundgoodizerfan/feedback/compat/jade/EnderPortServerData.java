// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.ender.EnderPortBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** An ender port's pairing state, distance to its partner, teleportant held and whether a chunk anchor holds it. */
public class EnderPortServerData implements IServerDataProvider<BlockAccessor> {

    public static final EnderPortServerData INSTANCE = new EnderPortServerData();

    static final String STATE = "PortState", DISTANCE = "PortDistance", TELEPORTANT = "PortTeleportant", ANCHORED = "PortAnchored";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EnderPortBlockEntity port) {
            data.putString(STATE, port.partnerState().getString());
            data.putFloat(DISTANCE, port.partnerDistance());
            data.putInt(TELEPORTANT, port.teleportantMb());
            data.putBoolean(ANCHORED, port.anchored());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("ender_port");
    }
}
