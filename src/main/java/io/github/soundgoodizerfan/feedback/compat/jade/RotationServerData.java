// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Sends the truth about a rotation network to the client so the HUD can describe it.
 *
 * <h2>Why this goes through Jade rather than the block's own sync</h2>
 * A {@link RotationNode} already syncs enough for rendering, but not enough to be honest with. The
 * Su ledger it carries is a snapshot from the last time the network was recalculated, and target
 * speed and inertia are network-wide and never sent at all. Adjectives could be derived from the
 * stale figures and would usually be right; the debug helmet could not, and philosophy 8 is
 * explicit that an instrument may become wrong but may never overstate its certainty.
 * <p>
 * So the numbers are fetched live, on demand, for the one block the player is looking at -- which
 * is what Jade's server data is for, and costs nothing when nobody is looking.
 * <p>
 * The consequence worth knowing: on a server without Jade installed, no data arrives and the HUD
 * says nothing rather than guessing.
 */
public class RotationServerData implements IServerDataProvider<BlockAccessor> {

    public static final RotationServerData INSTANCE = new RotationServerData();

    /** Present whenever the server answered at all, so the client can tell silence from zero. */
    static final String NETWORKED = "Networked";
    static final String RPM = "Rpm";
    static final String CAPACITY_SU = "CapacitySu";
    static final String LOAD_SU = "LoadSu";
    static final String OVERSTRESSED = "Overstressed";
    static final String TARGET_RPM = "TargetRpm";
    static final String INERTIA = "Inertia";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof RotationNode node))
            return;

        RotationNetwork network = node.getNetwork();
        data.putBoolean(NETWORKED, network != null);
        data.putFloat(RPM, node.getRpm().value());
        if (network == null)
            return;

        data.putFloat(CAPACITY_SU, network.getCapacitySu().value());
        data.putFloat(LOAD_SU, network.getLoadSu().value());
        data.putBoolean(OVERSTRESSED, network.isOverstressed());
        // Scaled by the node's own ratio, because target speed is a network figure and what the
        // player is looking at is one block on it.
        data.putFloat(TARGET_RPM, network.getTargetRpm().value() * node.getRatio());
        data.putFloat(INERTIA, network.getInertia().suTicksPerRpm());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("rotation");
    }
}
